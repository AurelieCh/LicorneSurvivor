package org.java.ressources.rendu;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.GradientPaint;
import java.awt.Graphics2D;
import java.awt.RadialGradientPaint;
import java.awt.Stroke;
import java.awt.geom.Path2D;
import java.awt.geom.Point2D;

import static org.java.ressources.rendu.Dessin.SOMBRE;
import static org.java.ressources.rendu.Dessin.etoile;

/**
 * Un petit animal mignon et détaillé par personnage (centré sur x, y comme la licorne).
 * 0 Lumi (licorne) · 1 Flèche (lapin archer) · 2 Boum (ourson aviateur)
 * 3 Lame (chat chevalier) · 4 Ronce (hérisson fleuri) · 5 Prisme (fennec à gemme)
 */
public final class SpritesPersonnages {
    private SpritesPersonnages() {
    }

    public static void dessiner(Graphics2D g, int x, int y0, int id, double anim) {
        int y = y0 + (int) Math.round(Math.sin(anim * 5) * 1.2); // petit flottement
        Stroke ancien = g.getStroke();
        switch (id) {
            case 1:
                fleche(g, x, y, anim);
                break;
            case 2:
                boum(g, x, y, anim);
                break;
            case 3:
                lame(g, x, y, anim);
                break;
            case 4:
                ronce(g, x, y, anim);
                break;
            case 5:
                prisme(g, x, y, anim);
                break;
            default:
                Sprites.licorne(g, x, y0, anim);
                break;
        }
        g.setStroke(ancien);
    }

    // ------------------------------------------------------------------ outils communs

    private static Color alpha(Color c, int a) {
        return new Color(c.getRed(), c.getGreen(), c.getBlue(), a);
    }

    /** Tête ronde : dégradé doux à trois teintes avec un reflet en haut à gauche, comme Lumi. */
    private static void tete(Graphics2D g, int x, int y, Color c0, Color c1, Color c2, Color contour) {
        g.setPaint(new RadialGradientPaint(new Point2D.Float(x - 7, y - 11), 36f, new float[]{0f, 0.6f, 1f},
                new Color[]{c0, c1, c2}));
        g.fillOval(x - 23, y - 20, 46, 41);
        g.setColor(contour);
        g.setStroke(new BasicStroke(2));
        g.drawOval(x - 23, y - 20, 46, 41);
    }

    /** Petites touffes de poils sur les joues (à dessiner AVANT la tête). */
    private static void touffes(Graphics2D g, int x, int y, Color fill, Color bord, int style) {
        g.setStroke(new BasicStroke(1.6f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        switch (style) {
            case 0: // boules de poil douces (lapin)
                boules(g, x, y, fill, bord);
                break;
            case 2: // deux mèches pointues (chat)
            case 4: // longues mèches effilées (fennec)
                for (int c = -1; c <= 1; c += 2) {
                    for (int k = 0; k < 2; k++) {
                        double lg = style == 4 ? 33 - k * 2 : 29 - k;
                        int by = y + 4 + k * 7;
                        Path2D.Double m = new Path2D.Double();
                        m.moveTo(x + c * 19, by);
                        m.quadTo(x + c * 27, by - 1, x + c * lg, by + 7);
                        m.quadTo(x + c * 25, by + 8, x + c * 19, by + 6);
                        m.closePath();
                        g.setColor(fill);
                        g.fill(m);
                        g.setColor(bord);
                        g.draw(m);
                    }
                }
                break;
            case 3: // petite feuille sur chaque joue (hérisson)
                for (int c = -1; c <= 1; c += 2) {
                    Path2D.Double f = new Path2D.Double();
                    f.moveTo(x + c * 19, y + 6);
                    f.quadTo(x + c * 33, y + 3, x + c * 31, y + 16);
                    f.quadTo(x + c * 21, y + 16, x + c * 19, y + 6);
                    g.setColor(fill);
                    g.fill(f);
                    g.setColor(bord);
                    g.draw(f);
                    g.drawLine(x + c * 20, y + 8, x + c * 29, y + 14);
                }
                break;
            default: // 1 : rien (ourson)
                break;
        }
    }

    private static void boules(Graphics2D g, int x, int y, Color fill, Color bord) {
        g.setStroke(new BasicStroke(1.6f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        // trois petites boules de poil par côté : contour d'abord, remplissage ensuite pour fondre les traits
        int[][] boules = {{23, 3, 9}, {25, 8, 9}, {22, 13, 8}};
        for (int passe = 0; passe < 2; passe++) {
            for (int c = -1; c <= 1; c += 2) {
                for (int[] b : boules) {
                    int bx = x + c * b[0] - b[2] / 2, by = y + b[1] - b[2] / 2;
                    if (passe == 0) {
                        g.setColor(bord);
                        g.drawOval(bx, by, b[2], b[2]);
                    } else {
                        g.setColor(fill);
                        g.fillOval(bx, by, b[2], b[2]);
                    }
                }
            }
        }
    }

    /** Grands yeux brillants avec cils (qui clignent), joues roses et sourire. */
    private static void visage(Graphics2D g, int x, int y, double anim, int ecart, Color iris, boolean joues) {
        boolean cligne = (anim % 4.2) < 0.13;
        g.setStroke(new BasicStroke(1.8f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        for (int c = -1; c <= 1; c += 2) {
            int ex = x + c * ecart;
            if (cligne) {
                g.setColor(SOMBRE);
                g.drawArc(ex - 5, y - 3, 10, 7, 180, 180);
            } else {
                g.setColor(SOMBRE);
                g.fillOval(ex - 5, y - 7, 10, 14);
                g.setColor(iris);
                g.fillOval(ex - 4, y + 1, 8, 5);
                g.setColor(SOMBRE);
                g.fillOval(ex - 2, y - 1, 4, 5);
                g.setColor(Color.WHITE);
                g.fillOval(ex - 3, y - 5, 4, 5);
                g.fillOval(ex + 1, y + 2, 2, 2);
                g.setColor(SOMBRE);
                g.drawLine(ex + c * 5, y - 5, ex + c * 8, y - 8);
            }
            if (joues) {
                g.setColor(new Color(255, 140, 175, 150));
                g.fillOval(ex + c * 6 - 5, y + 5, 10, 6);
            }
        }
        g.setColor(SOMBRE);
        g.setStroke(new BasicStroke(1.8f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g.drawArc(x - 5, y + 5, 10, 7, 200, 140);
    }

    private static void brillants(Graphics2D g, double anim, double[][] pts, Color col) {
        for (double[] b : pts) {
            double lueur = Math.max(0, Math.sin(anim * 4 + b[2]));
            if (lueur < 0.05) continue;
            g.setColor(alpha(col, (int) (230 * lueur)));
            etoile(g, b[0], b[1], 2.5 + 3 * lueur, 1.1 + lueur, anim * 2 + b[2]);
        }
    }

    // ------------------------------------------------------------------ 1 · Flèche : lapin archer

    private static void fleche(Graphics2D g, int x, int y, double anim) {
        Color peau = new Color(205, 235, 255), clair = new Color(240, 250, 255);
        Color bord = new Color(100, 165, 220), rose = new Color(255, 185, 205), poil = new Color(150, 200, 245);

        // flèches dans le dos (derrière la tête)
        for (int k = 0; k < 3; k++) {
            int bx = x + 16 + k * 5, hy = y - 34 - k * 3;
            g.setColor(new Color(150, 105, 70));
            g.setStroke(new BasicStroke(2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g.drawLine(bx - 3, y + 2, bx, hy);
            g.setColor(new Color(190, 195, 205));
            g.fillPolygon(new int[]{bx - 3, bx + 3, bx}, new int[]{hy + 1, hy + 1, hy - 6}, 3);
        }

        // arc à gauche
        g.setColor(new Color(150, 100, 60));
        g.setStroke(new BasicStroke(4f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g.drawArc(x - 42, y - 30, 24, 56, 90, 180);
        g.setColor(new Color(240, 240, 250));
        g.setStroke(new BasicStroke(1.2f));
        g.drawLine(x - 30, y - 30, x - 30, y + 26);

        // longues oreilles qui ondulent, avec intérieur rose en dégradé
        for (int c = -1; c <= 1; c += 2) {
            double ang = c * 0.2 + Math.sin(anim * 3 + c) * 0.07;
            int bx = x + c * 12, by = y - 18;
            g.rotate(ang, bx, by);
            g.setPaint(new GradientPaint(bx, by, peau, bx, by - 42, clair));
            g.fillOval(bx - 7, by - 42, 14, 46);
            g.setColor(bord);
            g.setStroke(new BasicStroke(1.8f));
            g.drawOval(bx - 7, by - 42, 14, 46);
            g.setPaint(new GradientPaint(bx, by, rose, bx, by - 36, new Color(255, 228, 238)));
            g.fillOval(bx - 3, by - 36, 6, 32);
            g.rotate(-ang, bx, by);
        }

        touffes(g, x, y, peau, bord, 0);
        tete(g, x, y, Color.WHITE, clair, peau, bord);

        // museau blanc
        g.setColor(new Color(255, 255, 255, 230));
        g.fillOval(x - 9, y + 1, 18, 13);

        // petite mèche qui se balance
        g.setStroke(new BasicStroke(3.5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g.setColor(poil);
        for (int k = -1; k <= 1; k++) {
            double bal = Math.sin(anim * 4 + k) * 1.3;
            Path2D.Double m = new Path2D.Double();
            m.moveTo(x + k * 4, y - 17);
            m.quadTo(x + k * 5 + bal, y - 22, x + k * 7 + bal, y - 27);
            g.draw(m);
        }

        // bandeau bleu + plume rouge
        g.setColor(new Color(70, 130, 200));
        g.setStroke(new BasicStroke(5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g.drawArc(x - 21, y - 19, 42, 16, 0, 180);
        Path2D.Double plume = new Path2D.Double();
        plume.moveTo(x + 19, y - 12);
        plume.quadTo(x + 36, y - 14 + Math.sin(anim * 4), x + 40, y - 28);
        plume.quadTo(x + 28, y - 26, x + 19, y - 12);
        g.setColor(new Color(235, 90, 100));
        g.fill(plume);
        g.setColor(new Color(170, 50, 60));
        g.setStroke(new BasicStroke(1.2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g.drawLine(x + 19, y - 12, x + 36, y - 25);

        // truffe rose, visage, petites dents de lapin
        g.setColor(new Color(255, 150, 175));
        g.fillOval(x - 2, y + 2, 4, 3);
        visage(g, x, y - 4, anim, 9, new Color(90, 150, 230), true);
        g.setColor(Color.WHITE);
        g.fillRoundRect(x - 2, y + 11, 4, 4, 1, 1);
        g.setColor(bord);
        g.setStroke(new BasicStroke(1f));
        g.drawRoundRect(x - 2, y + 11, 4, 4, 1, 1);

        brillants(g, anim, new double[][]{{x + 38, y - 33, 0}, {x - 36, y - 36, 2.1}, {x + 10, y - 56, 4.2}},
                new Color(210, 240, 255));
    }

    // ------------------------------------------------------------------ 2 · Boum : ourson aviateur

    private static void boum(Graphics2D g, int x, int y, double anim) {
        Color peau = new Color(255, 205, 110), bord = new Color(225, 140, 40), clair = new Color(255, 240, 190);

        // oreilles rondes avec intérieur
        for (int c = -1; c <= 1; c += 2) {
            g.setPaint(new RadialGradientPaint(new Point2D.Float(x + c * 15, y - 25), 12f, new float[]{0f, 1f},
                    new Color[]{new Color(255, 230, 160), peau}));
            g.fillOval(x + c * 17 - 8, y - 27, 16, 16);
            g.setColor(bord);
            g.setStroke(new BasicStroke(2f));
            g.drawOval(x + c * 17 - 8, y - 27, 16, 16);
            g.setColor(new Color(255, 170, 120));
            g.fillOval(x + c * 17 - 4, y - 23, 8, 8);
        }

        touffes(g, x, y, peau, bord, 1);
        tete(g, x, y, new Color(255, 252, 232), new Color(255, 232, 170), peau, bord);

        // museau clair + truffe brillante
        g.setPaint(new RadialGradientPaint(new Point2D.Float(x - 2, y + 4), 14f, new float[]{0f, 1f},
                new Color[]{Color.WHITE, clair}));
        g.fillOval(x - 10, y, 20, 15);
        g.setColor(SOMBRE);
        g.fillOval(x - 3, y + 1, 6, 4);
        g.setColor(Color.WHITE);
        g.fillOval(x - 2, y + 1, 2, 1);

        // traînée de suie sur la joue
        g.setColor(new Color(80, 60, 60, 60));
        g.fillOval(x + 11, y + 6, 9, 5);

        // lunettes d'aviateur relevées sur le front
        g.setColor(new Color(130, 85, 55));
        g.setStroke(new BasicStroke(4f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g.drawArc(x - 22, y - 19, 44, 16, 0, 180);
        for (int c = -1; c <= 1; c += 2) {
            g.setColor(new Color(190, 230, 250, 220));
            g.fillOval(x + c * 10 - 6, y - 21, 12, 12);
            g.setColor(new Color(90, 90, 105));
            g.setStroke(new BasicStroke(2f));
            g.drawOval(x + c * 10 - 6, y - 21, 12, 12);
            g.setColor(Color.WHITE);
            g.fillOval(x + c * 10 - 3, y - 19, 4, 3);
        }

        // mèche de bombe avec étincelle et petites fumées
        g.setColor(new Color(70, 60, 80));
        g.fillRoundRect(x - 5, y - 27, 10, 8, 3, 3);
        Path2D.Double meche = new Path2D.Double();
        meche.moveTo(x, y - 27);
        meche.quadTo(x + 9, y - 34, x + 4, y - 42);
        g.setColor(new Color(130, 90, 55));
        g.setStroke(new BasicStroke(2.2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g.draw(meche);
        for (int k = 0; k < 3; k++) {
            double t = (anim * 0.8 + k / 3.0) % 1.0;
            double px = x + 4 + Math.sin(t * 6 + k) * 3, py = y - 46 - t * 14, r = 2 + t * 3;
            g.setColor(new Color(200, 200, 210, (int) (120 * (1 - t))));
            g.fillOval((int) (px - r), (int) (py - r), (int) (2 * r), (int) (2 * r));
        }
        double lueur = 0.6 + 0.4 * Math.sin(anim * 14);
        g.setColor(new Color(255, 150, 40, 230));
        etoile(g, x + 4, y - 42, 4 + 3 * lueur, 1.5, anim * 6);
        g.setColor(new Color(255, 240, 120, 230));
        etoile(g, x + 4, y - 42, 2 + 1.5 * lueur, 0.8, -anim * 6);

        visage(g, x, y - 3, anim, 10, new Color(200, 110, 40), true);
    }

    // ------------------------------------------------------------------ 3 · Lame : chat chevalier

    private static void lame(Graphics2D g, int x, int y, double anim) {
        Color peau = new Color(236, 220, 255), clair = new Color(248, 242, 255), bord = new Color(140, 100, 205);

        // épée dans le dos
        g.setColor(new Color(170, 175, 195));
        g.setStroke(new BasicStroke(5f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_ROUND));
        g.drawLine(x + 22, y + 2, x + 32, y - 44);
        g.setColor(new Color(245, 248, 255));
        g.setStroke(new BasicStroke(1.5f));
        g.drawLine(x + 23, y - 2, x + 31, y - 40);
        g.setColor(new Color(240, 190, 50));
        g.setStroke(new BasicStroke(3f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g.drawLine(x + 17, y - 4, x + 31, y - 1);
        g.fillOval(x + 20, y + 3, 5, 5);

        // oreilles pointues avec petites touffes
        for (int c = -1; c <= 1; c += 2) {
            int[] ox = {x + c * 22, x + c * 20, x + c * 6}, oy = {y - 8, y - 35, y - 18};
            g.setColor(peau);
            g.fillPolygon(ox, oy, 3);
            g.setColor(bord);
            g.setStroke(new BasicStroke(2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g.drawPolygon(ox, oy, 3);
            g.setColor(new Color(255, 165, 195));
            g.fillPolygon(new int[]{x + c * 19, x + c * 18, x + c * 10}, new int[]{y - 12, y - 28, y - 18}, 3);
            g.setColor(Color.WHITE);
            g.setStroke(new BasicStroke(1.4f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g.drawLine(x + c * 17, y - 14, x + c * 14, y - 19);
            g.drawLine(x + c * 18, y - 17, x + c * 15, y - 22);
        }

        touffes(g, x, y, peau, bord, 2);
        tete(g, x, y, Color.WHITE, clair, peau, bord);

        // rayures de chat tigré sur le front
        g.setColor(alpha(bord, 150));
        g.setStroke(new BasicStroke(2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g.drawLine(x, y - 15, x, y - 9);
        g.drawLine(x - 5, y - 15, x - 5, y - 11);
        g.drawLine(x + 5, y - 15, x + 5, y - 11);

        // bandeau argenté + gemme + rubans qui flottent
        g.setColor(new Color(190, 195, 215));
        g.setStroke(new BasicStroke(5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g.drawArc(x - 21, y - 19, 42, 18, 0, 180);
        g.setColor(new Color(255, 90, 130));
        g.fillOval(x - 3, y - 22, 6, 6);
        g.setColor(Color.WHITE);
        g.fillOval(x - 2, y - 21, 2, 2);
        double w = Math.sin(anim * 4) * 2;
        g.setColor(new Color(150, 110, 220));
        g.setStroke(new BasicStroke(3.2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        Path2D.Double r1 = new Path2D.Double();
        r1.moveTo(x - 21, y - 9);
        r1.quadTo(x - 30, y - 9 + w, x - 38, y - 3 + w * 1.5);
        g.draw(r1);
        Path2D.Double r2 = new Path2D.Double();
        r2.moveTo(x - 21, y - 9);
        r2.quadTo(x - 29, y - 4 + w, x - 35, y + 2 + w * 1.5);
        g.draw(r2);

        // truffe, moustaches qui bougent un peu
        g.setColor(new Color(255, 140, 170));
        g.fillPolygon(new int[]{x - 3, x + 3, x}, new int[]{y + 1, y + 1, y + 5}, 3);
        g.setColor(alpha(SOMBRE, 170));
        g.setStroke(new BasicStroke(1.2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        double fr = Math.sin(anim * 3) * 1.2;
        for (int c = -1; c <= 1; c += 2) {
            g.drawLine(x + c * 17, y + 6, (x + c * 29), (int) (y + 3 + fr));
            g.drawLine(x + c * 17, y + 9, (x + c * 29), (int) (y + 10 - fr));
        }
        visage(g, x, y - 4, anim, 9, new Color(150, 105, 205), true);

        double lueur = Math.max(0, Math.sin(anim * 4));
        if (lueur > 0.05) {
            g.setColor(new Color(255, 255, 255, (int) (230 * lueur)));
            etoile(g, x + 32, y - 46, 2.5 + 3 * lueur, 1.1 + lueur, anim * 2);
        }
    }

    // ------------------------------------------------------------------ 4 · Ronce : hérisson fleuri

    private static void ronce(Graphics2D g, int x, int y, double anim) {
        Color peau = new Color(214, 246, 220), bord = new Color(60, 140, 90), creme = new Color(252, 249, 228);

        // piquants en éventail derrière la tête, avec trois petites baies rouges
        for (int i = 0; i < 13; i++) {
            double a = -3.5 + i * (3.85 / 12);
            double r0 = 17, rt = 33 + (i % 2) * 3 + Math.sin(anim * 3 + i);
            double cx = x, cy = y - 2;
            double tx = cx + Math.cos(a) * rt, ty = cy + Math.sin(a) * rt;
            Path2D.Double p = new Path2D.Double();
            p.moveTo(cx + Math.cos(a - 0.2) * r0, cy + Math.sin(a - 0.2) * r0);
            p.lineTo(tx, ty);
            p.lineTo(cx + Math.cos(a + 0.2) * r0, cy + Math.sin(a + 0.2) * r0);
            p.closePath();
            g.setColor(i % 2 == 0 ? new Color(70, 150, 95) : new Color(105, 190, 120));
            g.fill(p);
            g.setColor(new Color(45, 110, 70));
            g.setStroke(new BasicStroke(1.3f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g.draw(p);
            if (i == 2 || i == 7 || i == 11) {
                g.setColor(new Color(235, 70, 90));
                g.fillOval((int) tx - 3, (int) ty - 3, 6, 6);
                g.setColor(Color.WHITE);
                g.fillOval((int) tx - 1, (int) ty - 2, 2, 2);
            }
        }

        // petites oreilles rondes
        for (int c = -1; c <= 1; c += 2) {
            g.setColor(peau);
            g.fillOval(x + c * 18 - 6, y - 21, 12, 12);
            g.setColor(bord);
            g.setStroke(new BasicStroke(2f));
            g.drawOval(x + c * 18 - 6, y - 21, 12, 12);
            g.setColor(new Color(255, 170, 195));
            g.fillOval(x + c * 18 - 3, y - 18, 6, 6);
        }

        touffes(g, x, y, new Color(140, 220, 130), new Color(45, 110, 70), 3);
        tete(g, x, y, new Color(245, 255, 245), new Color(225, 247, 230), new Color(200, 235, 210), bord);

        // masque de visage creme
        g.setPaint(new RadialGradientPaint(new Point2D.Float(x - 3, y - 2), 22f, new float[]{0f, 1f},
                new Color[]{Color.WHITE, creme}));
        g.fillOval(x - 17, y - 9, 34, 27);

        // couronne de feuilles + bouton de rose
        for (int c = -1; c <= 1; c += 2) {
            double s = Math.sin(anim * 3 + c * 1.4) * 2.5;
            Path2D.Double feuille = new Path2D.Double();
            feuille.moveTo(x + c * 2, y - 19);
            feuille.quadTo(x + c * 20, y - 23, x + c * 18 + s, y - 38);
            feuille.quadTo(x + c * 4, y - 31, x + c * 2, y - 19);
            g.setColor(new Color(140, 220, 130));
            g.fill(feuille);
            g.setColor(new Color(45, 110, 70));
            g.setStroke(new BasicStroke(1.3f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g.draw(feuille);
            g.drawLine(x + c * 2, y - 20, (int) (x + c * 16 + s), y - 35);
        }
        g.setColor(new Color(60, 140, 80));
        g.setStroke(new BasicStroke(3f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g.drawLine(x, y - 19, x, y - 28);
        g.setColor(new Color(255, 120, 160));
        g.fillOval(x - 5, y - 37, 10, 11);
        g.setColor(new Color(200, 60, 105));
        g.setStroke(new BasicStroke(1.3f));
        g.drawArc(x - 3, y - 34, 6, 6, 0, 270);

        // petit nez noir brillant
        g.setColor(SOMBRE);
        g.fillOval(x - 3, y - 1, 6, 5);
        g.setColor(Color.WHITE);
        g.fillOval(x - 2, y, 2, 1);
        visage(g, x, y - 4, anim, 9, new Color(70, 170, 100), true);

        brillants(g, anim, new double[][]{{x + 30, y - 30, 0}, {x - 30, y - 28, 2.1}}, new Color(220, 255, 200));
    }

    // ------------------------------------------------------------------ 5 · Prisme : fennec à gemme

    private static void prisme(Graphics2D g, int x, int y, double anim) {
        Color fourrure = new Color(255, 226, 130), clair = new Color(255, 246, 205);
        Color bord = new Color(225, 170, 50), rose = new Color(255, 180, 200);

        // rayons arc-en-ciel autour de la tête
        g.setStroke(new BasicStroke(2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        for (int k = 0; k < 12; k++) {
            float teinte = (float) ((k / 12.0 + anim * 0.1) % 1.0);
            double a = k * Math.PI / 6 + anim * 0.5;
            double r1 = 40, r2 = 46 + 4 * Math.sin(anim * 3 + k);
            g.setColor(alpha(Color.getHSBColor(teinte, 0.45f, 1f), 170));
            g.drawLine((int) (x + Math.cos(a) * r1), (int) (y - 8 + Math.sin(a) * r1),
                    (int) (x + Math.cos(a) * r2), (int) (y - 8 + Math.sin(a) * r2));
        }

        // grandes oreilles de fennec : bout qui change de couleur, intérieur rose et duveteux
        float hue = (float) ((anim * 0.12) % 1.0);
        Color bout = Color.getHSBColor(hue, 0.45f, 1f);
        for (int c = -1; c <= 1; c += 2) {
            double ang = c * 0.04 + Math.sin(anim * 3 + c) * 0.05;
            g.rotate(ang, x + c * 14, y - 18);
            Path2D.Double oreille = new Path2D.Double();
            oreille.moveTo(x + c * 20, y - 8);
            oreille.quadTo(x + c * 34, y - 30, x + c * 24, y - 52);
            oreille.quadTo(x + c * 12, y - 34, x + c * 7, y - 19);
            oreille.closePath();
            g.setPaint(new GradientPaint(x + c * 20, y - 8, fourrure, x + c * 24, y - 52, bout));
            g.fill(oreille);
            g.setColor(bord);
            g.setStroke(new BasicStroke(2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g.draw(oreille);
            Path2D.Double dedans = new Path2D.Double();
            dedans.moveTo(x + c * 18, y - 13);
            dedans.quadTo(x + c * 27, y - 30, x + c * 23, y - 44);
            dedans.quadTo(x + c * 14, y - 32, x + c * 11, y - 20);
            dedans.closePath();
            g.setColor(rose);
            g.fill(dedans);
            g.setColor(Color.WHITE);
            g.setStroke(new BasicStroke(1.4f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g.drawLine(x + c * 17, y - 15, x + c * 14, y - 21);
            g.drawLine(x + c * 19, y - 19, x + c * 16, y - 26);
            g.rotate(-ang, x + c * 14, y - 18);
        }

        touffes(g, x, y, clair, bord, 4);
        tete(g, x, y, new Color(255, 250, 225), fourrure, new Color(250, 205, 100), bord);

        // bas du visage blanc
        g.setPaint(new RadialGradientPaint(new Point2D.Float(x, y + 8), 16f, new float[]{0f, 1f},
                new Color[]{Color.WHITE, new Color(255, 248, 225)}));
        g.fillOval(x - 13, y - 2, 26, 21);

        // petite houppe sur le front
        g.setColor(fourrure);
        g.setStroke(new BasicStroke(3.5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        for (int k = -1; k <= 1; k++) {
            double bal = Math.sin(anim * 4 + k) * 1.2;
            g.drawLine(x + k * 4, y - 19, (int) (x + k * 5 + bal), y - 27);
        }

        // gemme prismatique sur le front
        int[] gx = {x - 4, x + 4, x + 6, x, x - 6}, gy = {y - 18, y - 18, y - 14, y - 8, y - 14};
        g.setColor(Color.getHSBColor((float) ((anim * 0.15) % 1.0), 0.5f, 1f));
        g.fillPolygon(gx, gy, 5);
        g.setColor(new Color(255, 255, 255, 190));
        g.fillPolygon(new int[]{x - 4, x, x - 6}, new int[]{y - 18, y - 14, y - 14}, 3);
        g.setColor(bord);
        g.setStroke(new BasicStroke(1.3f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g.drawPolygon(gx, gy, 5);

        // petit nez noir brillant, visage
        g.setColor(SOMBRE);
        g.fillOval(x - 2, y - 1, 4, 3);
        g.setColor(Color.WHITE);
        g.fillOval(x - 1, y - 1, 1, 1);
        visage(g, x, y - 4, anim, 9, new Color(240, 150, 60), true);

        brillants(g, anim, new double[][]{{x + 30, y - 36, 0}, {x - 30, y - 34, 2.1}, {x + 3, y - 58, 4.2}},
                new Color(255, 255, 255));
    }
}