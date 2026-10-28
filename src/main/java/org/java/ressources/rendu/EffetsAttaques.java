package org.java.ressources.rendu;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Stroke;
import java.awt.geom.Arc2D;

import static org.java.ressources.rendu.Dessin.etoile;

/**
 * Dessin des deux attaques sans projectile. Les coordonnées sont celles de l'écran
 * (position du joueur moins la caméra, comme pour le sprite du personnage).
 * ---
 * Appel type, à placer là où tu dessines les tirs :
 * ---
 *   double sx = p.px - p.camX, sy = p.py - p.camY;
 *   for (int i = 0; i < p.nbFaisceaux; i++)
 *       if (p.faisceauOn[i]) EffetsAttaques.faisceau(g, sx, sy, p.faisceauAngle[i], p.faisceauLongueur, p.temps);
 *   if (p.coupT > 0)
 *       for (int i = 0; i < p.coupNb; i++)
 *           EffetsAttaques.coup(g, sx, sy, p.coupAngle[i], Partie.COUP_ARC, p.coupPortee,
 *                   1 - p.coupT / Partie.COUP_DUREE);
 */
public final class EffetsAttaques {
    private EffetsAttaques() {
    }

    /** Coup de Lame : croissant lumineux qui s'élargit puis s'efface (progression de 0 à 1). */
    public static void coup(Graphics2D g, double cx, double cy, double angle, double arc,
                            double portee, double progression) {
        Stroke ancien = g.getStroke();
        double p = Math.clamp(progression, 0, 1);
        int alpha = (int) (235 * (1 - p));
        double r = portee * (0.6 + 0.4 * p);
        double debut = -Math.toDegrees(angle + arc / 2); // Java2D mesure les angles à l'envers de l'écran
        double etendue = Math.toDegrees(arc);

        g.setStroke(new BasicStroke(11f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g.setColor(new Color(190, 150, 255, alpha / 3));
        g.draw(new Arc2D.Double(cx - r, cy - r, 2 * r, 2 * r, debut, etendue, Arc2D.OPEN));
        g.setStroke(new BasicStroke(6f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g.setColor(new Color(225, 205, 255, alpha * 2 / 3));
        g.draw(new Arc2D.Double(cx - r, cy - r, 2 * r, 2 * r, debut, etendue, Arc2D.OPEN));
        g.setStroke(new BasicStroke(2.4f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g.setColor(new Color(255, 255, 255, alpha));
        g.draw(new Arc2D.Double(cx - r, cy - r, 2 * r, 2 * r, debut, etendue, Arc2D.OPEN));

        // une étincelle au bout du croissant
        double fin = angle + arc / 2 * (1 - 2 * (1 - p));
        g.setColor(new Color(255, 250, 220, alpha));
        etoile(g, cx + Math.cos(fin) * r, cy + Math.sin(fin) * r, 4 + 3 * (1 - p), 1.5, p * 6);
        g.setStroke(ancien);
    }

    /** Faisceau de Prisme : ligne arc-en-ciel qui ondule, avec un cœur blanc. */
    public static void faisceau(Graphics2D g, double cx, double cy, double angle, double longueur, double anim) {
        Stroke ancien = g.getStroke();
        double ca = Math.cos(angle), sa = Math.sin(angle);
        double debut = 16; // le faisceau part juste devant le personnage
        int segments = Math.max(4, (int) ((longueur - debut) / 18));
        double pas = (longueur - debut) / segments;
        for (int passe = 0; passe < 3; passe++) {
            float epaisseur = passe == 0 ? 9f : passe == 1 ? 4.5f : 1.8f;
            g.setStroke(new BasicStroke(epaisseur, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            for (int k = 0; k < segments; k++) {
                double d0 = debut + k * pas, d1 = d0 + pas + 1;
                if (passe == 2) {
                    g.setColor(new Color(255, 255, 255, 230));
                } else {
                    float teinte = (float) (((k / (double) segments) + anim * 0.6) % 1.0);
                    Color c = Color.getHSBColor(teinte, 0.55f, 1f);
                    g.setColor(new Color(c.getRed(), c.getGreen(), c.getBlue(), passe == 0 ? 70 : 190));
                }
                g.drawLine((int) (cx + ca * d0), (int) (cy + sa * d0), (int) (cx + ca * d1), (int) (cy + sa * d1));
            }
        }
        // lueur à l'extrémité
        g.setColor(new Color(255, 255, 255, 200));
        etoile(g, cx + ca * longueur, cy + sa * longueur, 5 + 2 * Math.sin(anim * 12), 1.8, anim * 5);
        g.setStroke(ancien);
    }
}