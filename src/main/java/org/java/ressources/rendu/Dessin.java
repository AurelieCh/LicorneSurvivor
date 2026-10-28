package org.java.ressources.rendu;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Path2D;

/** Petits outils de dessin partagés par tous les rendus. */
public final class Dessin {
    private Dessin() {
    }

    public static final Color SOMBRE = new Color(80, 50, 110);
    public static final Font F_TITRE = new Font("SansSerif", Font.BOLD, 52);
    public static final Font F_GRAND = new Font("SansSerif", Font.BOLD, 30);
    public static final Font F_MOYEN = new Font("SansSerif", Font.BOLD, 20);
    public static final Font F_PETIT = new Font("SansSerif", Font.PLAIN, 16);
    public static final Font F_MINI = new Font("SansSerif", Font.BOLD, 13);

    public static void texteCentre(Graphics2D g, String s, int cx, int y) {
        FontMetrics fm = g.getFontMetrics();
        g.drawString(s, cx - fm.stringWidth(s) / 2, y);
    }

    public static String chrono(double t) {
        return String.format("%d:%02d", (int) t / 60, (int) t % 60);
    }

    public static Color alpha(Color c, int a) {
        return new Color(c.getRed(), c.getGreen(), c.getBlue(), Math.clamp(a, 0, 255));
    }

    /** Mélange a et b : t = 0 donne a, t = 1 donne b. */
    public static Color melange(Color a, Color b, double t) {
        t = Math.clamp(t, 0, 1);
        return new Color(
                (int) (a.getRed() + (b.getRed() - a.getRed()) * t),
                (int) (a.getGreen() + (b.getGreen() - a.getGreen()) * t),
                (int) (a.getBlue() + (b.getBlue() - a.getBlue()) * t));
    }

    /** Nombre pseudo-aléatoire stable entre 0 et 1, à partir d'une graine et d'un indice. */
    public static double h(double graine, int k) {
        double v = Math.sin(graine * 12.9898 + k * 78.233 + 4.1) * 43758.5453;
        return v - Math.floor(v);
    }

    public static void ov(Graphics2D g, double cx, double cy, double rx, double ry) {
        g.fill(new Ellipse2D.Double(cx - rx, cy - ry, 2 * rx, 2 * ry));
    }

    public static void ovContour(Graphics2D g, double cx, double cy, double rx, double ry) {
        g.draw(new Ellipse2D.Double(cx - rx, cy - ry, 2 * rx, 2 * ry));
    }

    private static Path2D chemin(double[] xy) {
        Path2D.Double p = new Path2D.Double();
        p.moveTo(xy[0], xy[1]);
        for (int i = 2; i + 1 < xy.length; i += 2) p.lineTo(xy[i], xy[i + 1]);
        p.closePath();
        return p;
    }

    /** Polygone plein : poly(g, x1, y1, x2, y2, ...). */
    public static void poly(Graphics2D g, double... xy) {
        g.fill(chemin(xy));
    }

    public static void contour(Graphics2D g, double... xy) {
        g.draw(chemin(xy));
    }

    public static void etoile(Graphics2D g, double cx, double cy, double rExt, double rInt, double rot) {
        double[] pts = new double[20];
        for (int i = 0; i < 10; i++) {
            double r = (i % 2 == 0) ? rExt : rInt;
            double a = rot + i * Math.PI / 5;
            pts[2 * i] = cx + Math.cos(a) * r;
            pts[2 * i + 1] = cy + Math.sin(a) * r;
        }
        poly(g, pts);
    }

    public static void bouton(Graphics2D g, Rectangle r, String texte, Color fond) {
        g.setColor(fond);
        g.fillRoundRect(r.x, r.y, r.width, r.height, 28, 28);
        g.setColor(Color.WHITE);
        g.setStroke(new BasicStroke(3));
        g.drawRoundRect(r.x, r.y, r.width, r.height, 28, 28);
        g.setFont(F_MOYEN);
        texteCentre(g, texte, r.x + r.width / 2, r.y + r.height / 2 + 7);
    }

    /** Fond clair translucide pour garder le texte lisible sur n'importe quel décor. */
    public static void plaque(Graphics2D g, int x, int y, int w, int h) {
        g.setColor(new Color(255, 255, 255, 175));
        g.fillRoundRect(x, y, w, h, 20, 20);
        g.setColor(new Color(255, 255, 255, 120));
        g.setStroke(new BasicStroke(2));
        g.drawRoundRect(x, y, w, h, 20, 20);
    }
}
