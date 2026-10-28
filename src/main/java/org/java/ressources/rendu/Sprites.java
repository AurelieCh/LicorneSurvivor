package org.java.ressources.rendu;

import org.java.ressources.entites.Ennemi;
import org.java.ressources.entites.TypeEnnemi;

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

/** Dessin de la licorne et des ennemis (centrés sur leur position). */
public final class Sprites {
    private Sprites() {
    }

    /**
     * Dessine la licorne (joueur) à l'écran, avec ses yeux, sa crinière et sa corne.
      * @param g Contexte graphique
     * @param x Position x du centre de la licorne
     * @param y0 Position y du centre de la licorne
     * @param anim Animation
     */
    public static void licorne(Graphics2D g, int x, int y0, double anim) {
        int y = y0 + (int) Math.round(Math.sin(anim * 5) * 1.2); // petit flottement
        Stroke ancienTrait = g.getStroke();

        // crinière derrière la tête : cinq mèches colorées de chaque côté, qui ondulent (la plus extérieure d'abord)
        g.setStroke(new BasicStroke(5.5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        for (int i = 4; i >= 0; i--) {
            for (int cote = -1; cote <= 1; cote += 2) {
                double ph = anim * 3.2 + i * 0.7 + (cote > 0 ? 1.3 : 0);
                double ondul = Math.sin(ph) * 2.2;
                double ext = 17 + i * 2.6;
                Path2D.Double p = new Path2D.Double();
                p.moveTo(x + cote * 11, y - 19 + i * 0.5);
                p.curveTo(x + cote * (ext + 6) + ondul, y - 14,
                        x + cote * (ext + 8) - ondul, y + 4 + i * 2,
                        x + cote * (ext - 2) + ondul * 0.6, y + 16 + i * 2.2);
                g.setColor(Color.getHSBColor(i / 6f, 0.5f, 1f));
                g.draw(p);
            }
        }

        // oreilles (avec l'intérieur rose)
        for (int c = -1; c <= 1; c += 2) {
            Path2D.Double oreille = new Path2D.Double();
            oreille.moveTo(x + c * 21, y - 9);
            oreille.quadTo(x + c * 27, y - 24, x + c * 20, y - 33);
            oreille.quadTo(x + c * 12, y - 24, x + c * 8, y - 19);
            oreille.closePath();
            g.setColor(new Color(255, 214, 230));
            g.fill(oreille);
            Path2D.Double dedans = new Path2D.Double();
            dedans.moveTo(x + c * 19, y - 14);
            dedans.quadTo(x + c * 22.5, y - 23, x + c * 19.5, y - 27);
            dedans.quadTo(x + c * 15, y - 22, x + c * 13, y - 19);
            dedans.closePath();
            g.setColor(new Color(255, 165, 195));
            g.fill(dedans);
        }

        // tête : dégradé doux avec un reflet en haut à gauche
        g.setPaint(new RadialGradientPaint(new Point2D.Float(x - 7, y - 11), 36f, new float[]{0f, 0.6f, 1f},
                new Color[]{new Color(255, 255, 255), new Color(255, 242, 250), new Color(250, 226, 241)}));
        g.fillOval(x - 23, y - 20, 46, 41);
        g.setColor(new Color(255, 170, 200));
        g.setStroke(new BasicStroke(2));
        g.drawOval(x - 23, y - 20, 46, 41);

        // corne dorée en spirale
        Path2D.Double corne = new Path2D.Double();
        corne.moveTo(x - 6.5, y - 17);
        corne.quadTo(x - 4, y - 32, x, y - 47);
        corne.quadTo(x + 4, y - 32, x + 6.5, y - 17);
        corne.closePath();
        g.setPaint(new GradientPaint(x - 6, y - 18, new Color(255, 240, 150),
                x + 6, y - 46, new Color(240, 170, 35)));
        g.fill(corne);
        g.setColor(new Color(214, 150, 30, 210));
        g.setStroke(new BasicStroke(1.6f));
        for (int k = 0; k < 3; k++) {
            double ty = (6 + k * 6.5) / 29.0;
            double demi = 6.5 * (1 - ty) * 0.95;
            int yy = (int) Math.round(y - 18 - ty * 29);
            g.drawLine((int) Math.round(x - demi), yy + 2, (int) Math.round(x + demi), yy - 2);
        }

        // mèche sur le front : trois petites boucles arc-en-ciel
        g.setStroke(new BasicStroke(4f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        for (int k = -1; k <= 1; k++) {
            double balance = Math.sin(anim * 4 + k) * 1.3;
            Path2D.Double meche = new Path2D.Double();
            meche.moveTo(x + k * 5, y - 18);
            meche.quadTo(x + k * 8 + balance, y - 15, x + k * 9 + balance, y - 11);
            g.setColor(Color.getHSBColor((k + 1) * 0.16f, 0.5f, 1f));
            g.draw(meche);
        }

        // yeux : grands, brillants, avec cils ; ils clignent de temps en temps
        boolean cligne = (anim % 4.2) < 0.13;
        g.setStroke(new BasicStroke(1.8f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        if (cligne) {
            g.setColor(SOMBRE);
            g.drawArc(x - 15, y - 3, 10, 7, 180, 180);
            g.drawArc(x + 5, y - 3, 10, 7, 180, 180);
        } else {
            for (int c = -1; c <= 1; c += 2) {
                int ex = c < 0 ? x - 14 : x + 4;
                g.setColor(SOMBRE);
                g.fillOval(ex, y - 7, 10, 14);
                g.setColor(new Color(150, 105, 205));
                g.fillOval(ex + 1, y + 1, 8, 5);
                g.setColor(SOMBRE);
                g.fillOval(ex + 3, y - 1, 4, 5);
                g.setColor(Color.WHITE);
                g.fillOval(ex + 2, y - 5, 4, 5);
                g.fillOval(ex + 6, y + 2, 2, 2);
                g.setColor(SOMBRE);
                g.drawLine(c < 0 ? ex : ex + 10, y - 5, c < 0 ? ex - 3 : ex + 13, y - 8);
            }
        }

        // joues et sourire
        g.setColor(new Color(255, 140, 175, 150));
        g.fillOval(x - 20, y + 5, 10, 6);
        g.fillOval(x + 10, y + 5, 10, 6);
        g.setColor(SOMBRE);
        g.setStroke(new BasicStroke(1.8f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g.drawArc(x - 6, y + 5, 12, 8, 200, 140);

        // petites étoiles scintillantes autour de la corne
        double[][] brillants = {{x + 15, y - 38, 0}, {x - 17, y - 33, 2.1}, {x + 7, y - 55, 4.2}};
        for (double[] b : brillants) {
            double lueur = Math.max(0, Math.sin(anim * 4 + b[2]));
            if (lueur < 0.05) continue;
            g.setColor(new Color(255, 250, 200, (int) (230 * lueur)));
            etoile(g, b[0], b[1], 2.5 + 3 * lueur, 1.1 + lueur, anim * 2 + b[2]);
        }
        g.setStroke(ancienTrait);
    }

    /** Variante visuelle de chaque personnage jouable. */
    public static void personnage(Graphics2D g, int x, int y, int id, double anim) {
        SpritesPersonnages.dessiner(g, x, y, id, anim);
    }

    private static void yeux(Graphics2D g, int x, int y, int ecart, int t, boolean mechant) {
        g.setColor(SOMBRE);
        g.fillOval(x - ecart - t / 2, y, t, t + 1);
        g.fillOval(x + ecart - t / 2, y, t, t + 1);
        g.setColor(Color.WHITE);
        int h = Math.max(1, t / 3);
        g.fillOval(x - ecart - t / 2 + 1, y + 1, h, h);
        g.fillOval(x + ecart - t / 2 + 1, y + 1, h, h);
        if (mechant) {
            g.setColor(SOMBRE);
            g.setStroke(new BasicStroke(2));
            g.drawLine(x - ecart - t, y - 5, x - ecart + t / 2, y - 1);
            g.drawLine(x + ecart - t / 2, y - 1, x + ecart + t, y - 5);
        }
    }

    public static void ennemi(Graphics2D g, Ennemi e) {
        int x = (int) e.x, y = (int) e.y, r = (int) e.rayon;
        Color c = e.flash > 0 ? Color.WHITE : e.col;
        if (e.type == TypeEnnemi.TIREUR) { // sucette-canon : reste à distance et lance des bonbons
            Color corps = e.monde == 1 ? new Color(110, 170, 185) : e.monde == 2 ? new Color(176, 98, 62) : e.col;
            g.setColor(e.flash > 0 ? Color.WHITE : new Color(95, 62, 125));
            g.fillRoundRect(x - r / 4, y - r - 7, r / 2 + 2, r, 6, 6); // le canon
            g.setColor(e.flash > 0 ? Color.WHITE : corps);
            g.fillOval(x - r, y - r, 2 * r, 2 * r);
            g.setColor(new Color(255, 255, 255, 170));
            g.fillOval(x - r / 2, y - r + 2, r / 2, r / 3);
            g.setColor(new Color(95, 62, 125));
            g.setStroke(new BasicStroke(2));
            g.drawOval(x - r, y - r, 2 * r, 2 * r);
            g.fillOval(x - r / 4 - 1, y - r - 9, r / 2 + 4, 6);        // bouche du canon
            yeux(g, x, y - 2, r / 3, 4, true);
            return;
        }
        if (e.type == TypeEnnemi.KAMIKAZE) { // bonbon-bombe : mèche allumée, pointes
            g.setColor(e.flash > 0 ? Color.WHITE : new Color(98, 56, 124));
            etoile(g, x, y, r + 6, r - 2, 0.3);
            g.setColor(e.flash > 0 ? Color.WHITE : new Color(235, 72, 112));
            g.fillOval(x - r + 2, y - r + 2, 2 * r - 4, 2 * r - 4);
            g.setColor(new Color(255, 255, 255, 150));
            g.fillOval(x - r / 2, y - r + 4, r / 2, r / 3);
            g.setColor(new Color(240, 215, 130));
            g.setStroke(new BasicStroke(2));
            g.drawLine(x, y - r + 1, x + 6, y - r - 9);                // la mèche
            g.setColor(new Color(255, 232, 90));
            g.fillOval(x + 3, y - r - 14, 8, 8);
            yeux(g, x, y - 1, r / 3, 4, true);
            return;
        }
        if (e.type == TypeEnnemi.GUIMAUVE) { // brute : silhouette ronde, détails adaptés au monde
            if (e.monde == 1) {
                // Guimauve forestière : un petit champignon, sans perdre le corps moelleux.
                g.setColor(e.flash > 0 ? Color.WHITE : new Color(190, 224, 218));
                g.fillRoundRect(x - r, y - r / 3, 2 * r, r + r / 2, r / 2, r / 2);
                g.setColor(new Color(72, 116, 113));
                g.setStroke(new BasicStroke(3));
                g.drawRoundRect(x - r, y - r / 3, 2 * r, r + r / 2, r / 2, r / 2);
                g.setColor(e.flash > 0 ? Color.WHITE : new Color(88, 137, 132));
                g.fillArc(x - r - 2, y - r - 5, 2 * r + 4, r + 12, 0, 180);
                g.setColor(new Color(210, 235, 220));
                g.fillOval(x - r / 2, y - r + 1, 5, 5);
                g.fillOval(x + r / 4, y - r + 5, 4, 4);
                yeux(g, x, y - r / 8, r / 3, 6, true);
            } else if (e.monde == 2) {
                // Guimauve chocolat : corps arrondi, glaçage et deux petites pointes de caramel.
                g.setColor(e.flash > 0 ? Color.WHITE : new Color(132, 76, 53));
                g.fillRoundRect(x - r, y - r, 2 * r, 2 * r, r, r);
                g.setColor(new Color(83, 42, 34));
                g.setStroke(new BasicStroke(3));
                g.drawRoundRect(x - r, y - r, 2 * r, 2 * r, r, r);
                g.setColor(new Color(213, 145, 62));
                g.fillArc(x - r + 2, y - r - 2, 2 * r - 4, r + 12, 0, 180);
                g.fillOval(x - r / 2, y - r - 6, r / 5 + 2, r / 3);
                g.fillOval(x + r / 4, y - r - 8, r / 5 + 2, r / 3);
                yeux(g, x, y - r / 4, r / 3, 6, true);
            } else {
                g.setColor(e.flash > 0 ? Color.WHITE : new Color(255, 250, 252));
                g.fillRoundRect(x - r, y - r, 2 * r, 2 * r, r, r);
                g.setColor(e.col);
                g.setStroke(new BasicStroke(3));
                g.drawRoundRect(x - r, y - r, 2 * r, 2 * r, r, r);
                yeux(g, x, y - r / 4, r / 3, 6, true);
            }
            return;
        }
        if (e.type == TypeEnnemi.MACARON) { // créature légère : macaron décliné selon le décor
            if (e.monde == 1) {
                g.setColor(e.flash > 0 ? Color.WHITE : new Color(116, 174, 164));
                g.fillOval(x - r, y - r, 2 * r, r + 4);
                g.setColor(new Color(224, 244, 225));
                g.fillRoundRect(x - r + 2, y - 3, 2 * r - 4, 7, 6, 6);
                g.setColor(new Color(67, 112, 106));
                g.fillOval(x - r, y + 2, 2 * r, r - 2);
                g.setColor(new Color(210, 235, 200));
                g.fillOval(x - r / 2, y - r - 5, r / 3, r / 2);
                g.fillOval(x + r / 5, y - r - 8, r / 4, r / 2);
                yeux(g, x, y - r / 2, r / 3, 5, true);
            } else if (e.monde == 2) {
                g.setColor(e.flash > 0 ? Color.WHITE : new Color(151, 83, 48));
                g.fillOval(x - r, y - r, 2 * r, r + 4);
                g.setColor(new Color(255, 205, 110));
                g.fillRoundRect(x - r + 2, y - 3, 2 * r - 4, 7, 6, 6);
                g.setColor(new Color(102, 48, 34));
                g.fillOval(x - r, y + 2, 2 * r, r - 2);
                yeux(g, x, y - r / 2, r / 3, 5, true);
            } else {
                g.setColor(c);
                g.fillOval(x - r, y - r, 2 * r, r + 2);
                g.setColor(new Color(255, 250, 240));
                g.fillRoundRect(x - r + 2, y - 3, 2 * r - 4, 7, 6, 6);
                g.setColor(c);
                g.fillOval(x - r, y + 2, 2 * r, r - 2);
                yeux(g, x, y - r / 2, r / 3, 5, true);
            }
            return;
        }
        if (e.type == TypeEnnemi.GOMME) { // gomme : bonbon rond décliné en mousse, chocolat ou gelée
            Color corps = e.monde == 1 ? new Color(104, 174, 184)
                    : e.monde == 2 ? new Color(180, 91, 57) : c;
            g.setColor(e.flash > 0 ? Color.WHITE : corps);
            g.fillOval(x - r, y - r, 2 * r, 2 * r);
            g.setColor(e.monde == 1 ? new Color(183, 236, 220)
                    : e.monde == 2 ? new Color(241, 171, 77) : new Color(255, 255, 255, 170));
            g.fillOval(x - r / 2, y - r + 2, r / 2, r / 3);
            if (e.monde == 1) {
                g.setColor(new Color(221, 244, 205));
                g.fillOval(x + r / 3, y - r / 3, r / 4, r / 5);
            } else if (e.monde == 2) {
                g.setColor(new Color(93, 45, 34));
                g.drawArc(x - r / 2, y - r / 3, r, r / 2, 200, 140);
            }
            yeux(g, x, y - 2, r / 3, 4, true);
            return;
        }
        if (e.type == TypeEnnemi.BOSS) { // boss : même gabarit, identité forte par monde
            if (e.monde == 1) {
                g.setColor(e.flash > 0 ? Color.WHITE : new Color(72, 128, 119));
                g.fillOval(x - r, y - r / 4, 2 * r, r + 8);
                g.fillOval(x - (int) (r * 0.75), y - (int) (r * 0.6), (int) (r * 1.5), (int) (r * 0.9));
                g.setColor(new Color(181, 224, 192));
                g.fillArc(x - r, y - (int) (r * 1.1), 2 * r, r, 0, 180);
                g.setColor(new Color(224, 244, 205));
                g.fillOval(x - r / 2, y - r - 3, r / 4, r / 3);
                g.fillOval(x + r / 4, y - r - 7, r / 4, r / 3);
                yeux(g, x, y - 10, 16, 12, true);
            } else if (e.monde == 2) {
                g.setColor(e.flash > 0 ? Color.WHITE : new Color(116, 58, 39));
                g.fillOval(x - r, y - r / 4, 2 * r, r + 8);
                g.fillOval(x - (int) (r * 0.75), y - (int) (r * 0.6), (int) (r * 1.5), (int) (r * 0.9));
                g.setColor(new Color(239, 164, 55));
                g.fillPolygon(new int[]{x - 20, x - 12, x - 3, x + 8, x + 20, x + 12, x},
                        new int[]{y - r / 2, y - r - 13, y - r / 2, y - r - 17, y - r / 2, y - r - 13, y - r / 2}, 7);
                g.setColor(new Color(247, 204, 106));
                g.fillOval(x - r + 8, y + 4, 14, 10);
                yeux(g, x, y - 10, 16, 12, true);
            } else {
                g.setColor(c);
                g.fillOval(x - r, y - r / 4, 2 * r, r + 8);
                g.fillOval(x - (int) (r * 0.75), y - (int) (r * 0.6), (int) (r * 1.5), (int) (r * 0.9));
                g.fillOval(x - (int) (r * 0.45), y - (int) (r * 1.05), (int) (r * 0.9), (int) (r * 0.7));
                g.setColor(new Color(255, 255, 255, 160));
                g.fillOval(x - r + 8, y + 4, 14, 10);
                g.setColor(new Color(255, 220, 90));
                int cy = y - (int) (r * 1.05);
                g.fillPolygon(new int[]{x - 20, x - 20, x - 10, x, x + 10, x + 20, x + 20},
                        new int[]{cy + 6, cy - 14, cy - 2, cy - 18, cy - 2, cy - 14, cy + 6}, 7);
                yeux(g, x, y - 10, 16, 12, true);
            }
            return;
        }
        if (e.monde == 1) {
            g.setColor(c);
            g.fillOval(x - r, y - r / 2, 2 * r, r + 8);
            g.fillPolygon(new int[]{x - r + 4, x - r - 9, x - r + 2, x, x + r - 2, x + r + 9, x + r - 4},
                    new int[]{y, y - r - 8, y + r / 2, y + r, y + r / 2, y - r - 8, y}, 7);
            yeux(g, x, y - 4, r / 3, 5, true);
            return;
        }
        if (e.monde == 2) {
            g.setColor(c);
            g.fillOval(x - r, y - r, 2 * r, 2 * r);
            g.setColor(new Color(255, 210, 80));
            g.fillOval(x - r / 2, y - r - 4, r / 3, r / 2);
            g.fillOval(x + r / 5, y - r - 8, r / 4, r / 2);
            g.setColor(new Color(110, 45, 30));
            g.drawOval(x - r / 2, y, r / 3, r / 3);
            yeux(g, x, y - 4, r / 3, 5, true);
        }
    }
}
