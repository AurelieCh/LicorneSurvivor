package org.java.ressources.rendu;

import org.java.ressources.donnees.Monde;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.geom.Arc2D;
import java.awt.geom.Line2D;

import static org.java.ressources.donnees.Constantes.H;
import static org.java.ressources.donnees.Constantes.W;
import static org.java.ressources.rendu.Dessin.h;
import static org.java.ressources.rendu.Dessin.ov;
import static org.java.ressources.rendu.Dessin.poly;

/**
 * La limite de la carte : sucre d'orge (prairie), haie de guimauve (forêt), rochers pointus (volcan).
 * Le bord bas est dessiné après les personnages pour pouvoir les cacher un peu.
 */
public final class Bordures {
    private Bordures() {
    }

    private static int pas(Monde m) {
        return m.id == 0 ? 52 : m.id == 1 ? 46 : 40;
    }

    /** avant = false : haut, gauche et droite (derrière les personnages) ; avant = true : bas (devant). */
    public static void dessiner(Graphics2D g, Monde m, double camX, double camY, boolean avant) {
        int s = pas(m);
        int w = m.largeur, hh = m.hauteur;
        if (!avant) {
            if (camY < 160) {
                for (int i = (int) Math.floor((camX - 90) / s); i <= (int) Math.ceil((camX + W + 90) / s); i++) {
                    double x = i * (double) s;
                    if (x >= -s && x <= w + s) objet(g, m, x, -4, i, 0, s);
                }
            }
            for (int i = (int) Math.floor((camY - 90) / s); i <= (int) Math.ceil((camY + H + 90) / s); i++) {
                double y = i * (double) s;
                if (y < -s || y > hh + s) continue;
                if (camX < 160) objet(g, m, -4, y, i + 1000, 2, s);
                if (camX + W > w - 160) objet(g, m, w + 4, y, i + 2000, 3, s);
            }
        } else if (camY + H > hh - 160) {
            for (int i = (int) Math.floor((camX - 90) / s); i <= (int) Math.ceil((camX + W + 90) / s); i++) {
                double x = i * (double) s;
                if (x >= -s && x <= w + s) objet(g, m, x, hh + 4, i + 3000, 1, s);
            }
        }
    }

    private static void objet(Graphics2D g, Monde m, double x, double y, int idx, int cote, int s) {
        double j = h(idx, 3);
        if (m.id == 0) { // poteau en sucre d'orge, avec une lisse
            g.setColor(new Color(0, 0, 0, 50));
            ov(g, x, y, 13, 5);
            g.setStroke(new BasicStroke(4f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g.setColor(new Color(255, 238, 246));
            if (cote <= 1) g.draw(new Line2D.Double(x, y - 26, x + s, y - 26));
            else g.draw(new Line2D.Double(x, y - 26, x, y + s - 26));
            double haut = 54;
            for (int k = 0; k < 6; k++) {
                g.setColor(k % 2 == 0 ? new Color(255, 140, 170) : new Color(255, 252, 252));
                g.fillRect((int) (x - 4.5), (int) (y - (k + 1) * haut / 6), 9, (int) Math.ceil(haut / 6) + 1);
            }
            g.setStroke(new BasicStroke(8f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g.setColor(new Color(255, 140, 170));
            g.draw(new Arc2D.Double(x - 4, y - haut - 12, 16, 16, 0, 180, Arc2D.OPEN));
        } else if (m.id == 1) { // haie de guimauve
            double r = 26 + 10 * j;
            g.setColor(new Color(190, 170, 255, 24));
            ov(g, x, y - 16, r * 1.7, r * 1.3);
            g.setColor(new Color(40, 78, 104));
            ov(g, x, y - 12, r * 1.05, r * 0.85);
            g.setColor(new Color(196, 178, 240));
            ov(g, x - r * 0.25, y - 22, r * 0.85, r * 0.7);
            g.setColor(new Color(246, 240, 255));
            ov(g, x + r * 0.3, y - 28, r * 0.7, r * 0.6);
            g.setColor(new Color(255, 255, 255, 150));
            ov(g, x + r * 0.15, y - 36, r * 0.25, r * 0.15);
        } else { // rochers pointus avec lueur de lave à la base
            double hp = 48 + 44 * j;
            g.setColor(new Color(255, 100, 30, 100));
            ov(g, x, y, 24, 7);
            g.setColor(new Color(56, 30, 24));
            poly(g, x - 20, y, x - 6, y - hp, x + 4, y - hp * 0.62, x + 10, y - hp * 0.9, x + 22, y);
            g.setColor(new Color(96, 56, 42));
            poly(g, x - 20, y, x - 6, y - hp, x - 2, y - hp * 0.5, x - 8, y);
            g.setColor(new Color(255, 150, 60, 150));
            g.setStroke(new BasicStroke(2f));
            g.draw(new Line2D.Double(x + 8, y - 3, x + 14, y - hp * 0.3));
        }
    }
}
