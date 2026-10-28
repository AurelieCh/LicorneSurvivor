package org.java.ressources.rendu;

import org.java.ressources.donnees.ArmeSecondaire;
import org.java.ressources.entites.Eclair;
import org.java.ressources.entites.Ennemi;
import org.java.ressources.entites.TirEnnemi;
import org.java.ressources.entites.TypeEnnemi;
import org.java.ressources.entites.ZoneDanger;
import org.java.ressources.jeu.Partie;

import java.awt.AlphaComposite;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Composite;
import java.awt.Graphics2D;
import java.awt.geom.AffineTransform;
import java.awt.geom.Arc2D;

import static org.java.ressources.rendu.Dessin.F_MOYEN;
import static org.java.ressources.rendu.Dessin.etoile;
import static org.java.ressources.rendu.Dessin.ov;

/**
 * Effets de combat ajoutés avec les boss à patterns, les nouveaux ennemis et les armes secondaires.
 * Dessinés dans le repère du monde (la translation de la caméra est déjà appliquée).
 */
final class EffetsCombat {
    private EffetsCombat() {
    }

    private static final Color ROUGE = new Color(255, 60, 80);
    private static final Color OR = new Color(255, 215, 90);

    /** Tout ce qui est « au sol » : zones d'impact, aura, avertissements du boss et des kamikazes. */
    static void sol(Graphics2D g, Partie p, double anim) {
        for (ZoneDanger z : p.zones) zone(g, z);
        aura(g, p, anim);
        for (Ennemi e : p.ennemis) {
            if (e.boss) avertissementBoss(g, p, e, anim);
            else if (e.type == TypeEnnemi.KAMIKAZE && e.etape == 1) {
                double r = e.elite ? 115 : 80;
                double pulse = 0.5 + 0.5 * Math.sin(anim * 30);
                g.setColor(new Color(255, 70, 70, (int) (40 + 60 * pulse)));
                ov(g, e.x, e.y, r, r);
                g.setStroke(new BasicStroke(3));
                g.setColor(new Color(255, 90, 90, 200));
                g.drawOval((int) (e.x - r), (int) (e.y - r), (int) (2 * r), (int) (2 * r));
            }
        }
    }

    /** Ce qui est « en l'air » : projectiles ennemis, éclairs, halos d'élite, « ! », traînée d'esquive. */
    static void air(Graphics2D g, Partie p, double anim) {
        if (p.esquiveT > 0) traineeEsquive(g, p);
        for (Ennemi e : p.ennemis) {
            if (e.elite) haloElite(g, e, anim);
            if (e.type == TypeEnnemi.TIREUR && e.etape == 1) {
                g.setFont(F_MOYEN);
                g.setColor(ROUGE);
                g.drawString("!", (int) e.x - 4, (int) (e.y - e.rayon - 8));
            }
            if (e.boss && e.etape == 3) etourdi(g, e, anim);
        }
        for (TirEnnemi t : p.tirsEnnemis) tirEnnemi(g, t, anim);
        for (Eclair arc : p.eclairs) eclair(g, arc, anim);
    }

    // ------------------------------------------------------------------

    private static void zone(Graphics2D g, ZoneDanger z) {
        Color base = z.ennemie ? ROUGE : new Color(255, 185, 90);
        int x = (int) z.x, y = (int) z.y, r = (int) z.rayon;
        if (z.explosee) {
            double a = Math.clamp(z.apres / 0.3, 0, 1);
            g.setColor(new Color(255, 245, 220, (int) (150 * a)));
            ov(g, z.x, z.y, z.rayon * (1.05 - 0.1 * a), z.rayon * (1.05 - 0.1 * a));
            g.setStroke(new BasicStroke(4));
            g.setColor(new Color(base.getRed(), base.getGreen(), base.getBlue(), (int) (220 * a)));
            g.drawOval(x - r, y - r, 2 * r, 2 * r);
            return;
        }
        double prog = 1 - Math.max(0, z.delai) / Math.max(0.01, z.delaiMax);
        g.setColor(new Color(base.getRed(), base.getGreen(), base.getBlue(), (int) (35 + 55 * prog)));
        ov(g, z.x, z.y, z.rayon, z.rayon);
        double ri = z.rayon * prog;
        g.setColor(new Color(base.getRed(), base.getGreen(), base.getBlue(), (int) (60 + 80 * prog)));
        ov(g, z.x, z.y, ri, ri);
        g.setStroke(new BasicStroke(3));
        g.setColor(new Color(base.getRed(), base.getGreen(), base.getBlue(), 230));
        g.drawOval(x - r, y - r, 2 * r, 2 * r);
    }

    private static void aura(Graphics2D g, Partie p, double anim) {
        int i = ArmeSecondaire.AURA.ordinal();
        if (p.niveauArme[i] <= 0) return;
        boolean evo = p.evolueeArme[i];
        Color c = evo ? OR : new Color(255, 150, 200);
        double r = p.rayonAura() * (1 + 0.02 * Math.sin(anim * 5));
        g.setColor(new Color(c.getRed(), c.getGreen(), c.getBlue(), evo ? 40 : 30));
        ov(g, p.px, p.py, r, r);
        g.setStroke(new BasicStroke(evo ? 4 : 3));
        g.setColor(new Color(c.getRed(), c.getGreen(), c.getBlue(), 120));
        g.drawOval((int) (p.px - r), (int) (p.py - r), (int) (2 * r), (int) (2 * r));
    }

    private static void avertissementBoss(Graphics2D g, Partie p, Ennemi e, double anim) {
        double pulse = 0.5 + 0.5 * Math.sin(anim * 20);
        if (e.etape == 1) { // couloir de charge
            AffineTransform ancien = g.getTransform();
            g.translate(e.x, e.y);
            g.rotate(Math.atan2(e.dirY, e.dirX));
            g.setColor(new Color(255, 60, 80, (int) (50 + 60 * pulse)));
            g.fillRect(0, (int) -e.rayon, 330 + (int) e.rayon, (int) (2 * e.rayon));
            g.setStroke(new BasicStroke(3));
            g.setColor(new Color(255, 80, 100, 220));
            g.drawRect(0, (int) -e.rayon, 330 + (int) e.rayon, (int) (2 * e.rayon));
            g.setTransform(ancien);
        } else if (e.etape == 4) {
            Color c = switch (e.attaque) {
                case 1 -> new Color(255, 130, 190);   // anneau
                case 2 -> ROUGE;                       // pilonnage
                case 3 -> new Color(130, 230, 150);   // invocation
                default -> new Color(255, 150, 60);   // souffle
            };
            if (e.attaque == 4) { // cône de souffle vers le joueur
                double visee = Math.atan2(p.py - e.y, p.px - e.x);
                g.setColor(new Color(c.getRed(), c.getGreen(), c.getBlue(), (int) (70 + 60 * pulse)));
                g.fill(new Arc2D.Double(e.x - 340, e.y - 340, 680, 680,
                        -Math.toDegrees(visee) - 28, 56, Arc2D.PIE));
            } else {
                double r = e.rayon + 12 + 10 * pulse;
                g.setStroke(new BasicStroke(5));
                g.setColor(new Color(c.getRed(), c.getGreen(), c.getBlue(), 200));
                g.drawOval((int) (e.x - r), (int) (e.y - r), (int) (2 * r), (int) (2 * r));
            }
        }
    }

    private static void haloElite(Graphics2D g, Ennemi e, double anim) {
        double r = e.rayon + 6 + 2 * Math.sin(anim * 6);
        g.setStroke(new BasicStroke(3));
        g.setColor(new Color(255, 215, 90, 210));
        g.drawOval((int) (e.x - r), (int) (e.y - r), (int) (2 * r), (int) (2 * r));
        // petite couronne au-dessus
        int cx = (int) e.x, cy = (int) (e.y - e.rayon - 6);
        g.setColor(OR);
        g.fillPolygon(new int[]{cx - 10, cx - 10, cx - 5, cx, cx + 5, cx + 10, cx + 10},
                new int[]{cy + 5, cy - 7, cy - 1, cy - 9, cy - 1, cy - 7, cy + 5}, 7);
    }

    private static void etourdi(Graphics2D g, Ennemi e, double anim) {
        g.setColor(OR);
        for (int k = 0; k < 3; k++) {
            double a = anim * 5 + k * 2 * Math.PI / 3;
            etoile(g, e.x + Math.cos(a) * 34, e.y - e.rayon - 8 + Math.sin(a) * 8, 8, 3.5, anim * 4);
        }
    }

    private static void tirEnnemi(Graphics2D g, TirEnnemi t, double anim) {
        int x = (int) t.x, y = (int) t.y, r = (int) t.rayon;
        if (t.genre == TirEnnemi.FLAMME) {
            g.setColor(new Color(255, 120, 40, 110));
            g.fillOval(x - r - 4, y - r - 4, 2 * r + 8, 2 * r + 8);
            g.setColor(new Color(255, 170, 60));
            g.fillOval(x - r, y - r, 2 * r, 2 * r);
            g.setColor(new Color(255, 235, 140));
            g.fillOval(x - r / 2, y - r / 2, r, r);
        } else {
            g.setColor(new Color(255, 90, 150, 100));
            g.fillOval(x - r - 3, y - r - 3, 2 * r + 6, 2 * r + 6);
            g.setColor(new Color(255, 120, 175));
            g.fillOval(x - r, y - r, 2 * r, 2 * r);
            g.setColor(Color.WHITE);
            g.fillOval(x - r / 2, y - r / 2, Math.max(2, r / 2), Math.max(2, r / 2));
            g.setStroke(new BasicStroke(2));
            g.setColor(new Color(150, 40, 90));
            g.drawOval(x - r, y - r, 2 * r, 2 * r);
        }
    }

    private static void eclair(Graphics2D g, Eclair arc, double anim) {
        double a = Math.clamp(arc.vie / 0.28, 0, 1);
        for (int passe = 0; passe < 2; passe++) {
            g.setStroke(new BasicStroke(passe == 0 ? 5f : 2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g.setColor(passe == 0 ? new Color(150, 200, 255, (int) (160 * a))
                    : new Color(255, 255, 255, (int) (255 * a)));
            for (int i = 0; i + 1 < arc.n; i++) {
                double x0 = arc.x[i], y0 = arc.y[i], x1 = arc.x[i + 1], y1 = arc.y[i + 1];
                double px = x0, py = y0;
                int morceaux = 5;
                double nx = -(y1 - y0), ny = x1 - x0;
                double len = Math.max(1, Math.hypot(nx, ny));
                nx /= len;
                ny /= len;
                for (int k = 1; k <= morceaux; k++) {
                    double t = k / (double) morceaux;
                    double dec = k == morceaux ? 0 : Math.sin(anim * 90 + i * 7 + k * 13) * 9;
                    double qx = x0 + (x1 - x0) * t + nx * dec, qy = y0 + (y1 - y0) * t + ny * dec;
                    g.drawLine((int) px, (int) py, (int) qx, (int) qy);
                    px = qx;
                    py = qy;
                }
            }
        }
    }

    private static void traineeEsquive(Graphics2D g, Partie p) {
        Composite ancien = g.getComposite();
        for (int k = 1; k <= 4; k++) {
            float a = 0.35f - k * 0.07f;
            g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, Math.max(0.05f, a)));
            g.setColor(new Color(255, 230, 250));
            ov(g, p.px - p.esquiveDx * k * 20, p.py - p.esquiveDy * k * 20, 17, 21);
        }
        g.setComposite(ancien);
    }
}
