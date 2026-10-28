package org.java.ressources.rendu;

import org.java.ressources.donnees.Monde;
import org.java.ressources.entites.Obstacle;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.geom.AffineTransform;
import java.awt.geom.Arc2D;
import java.awt.geom.Line2D;
import java.awt.geom.Rectangle2D;
import java.awt.geom.RoundRectangle2D;

import static org.java.ressources.rendu.Dessin.alpha;
import static org.java.ressources.rendu.Dessin.contour;
import static org.java.ressources.rendu.Dessin.h;
import static org.java.ressources.rendu.Dessin.melange;
import static org.java.ressources.rendu.Dessin.ov;
import static org.java.ressources.rendu.Dessin.ovContour;
import static org.java.ressources.rendu.Dessin.poly;

/**
 * Le dessin de tous les obstacles, monde par monde.
 * Le point (0, 0) local est le point de contact avec le sol ; u est l'unité de taille de l'obstacle.
 */
public final class Obstacles {
    private Obstacles() {
    }

    public static void dessiner(Graphics2D g, Obstacle o, double anim) {
        AffineTransform old = g.getTransform();
        g.translate(o.x, o.y);
        g.setStroke(new BasicStroke(2));
        switch (o.monde) {
            case 0:
                prairie(g, o, anim);
                break;
            case 1:
                foret(g, o, anim);
                break;
            default:
                volcan(g, o, anim);
                break;
        }
        g.setTransform(old);
    }

    private static void ombre(Graphics2D g, double cx, double cy, double rx, double ry) {
        g.setColor(new Color(0, 0, 0, 50));
        ov(g, cx, cy, rx, ry);
    }

    // ==================================================================
    // Monde 1 : Prairie Cupcake - bonbons pastel
    // ==================================================================
    private static void prairie(Graphics2D g, Obstacle o, double anim) {
        double rr = o.echelle;
        Color[] pal = Monde.TOUS[0].palette;
        int i1 = (int) (o.graine * 100) % pal.length;
        Color c1 = pal[i1], c2 = pal[(i1 + 2) % pal.length];
        switch (o.type) {
            case 0: { // cupcake géant
                ombre(g, 0, 0.05 * rr, 1.15 * rr, 0.38 * rr);
                Color papier = melange(c1, Color.WHITE, 0.3);
                double[] godet = {-0.8 * rr, 0, 0.8 * rr, 0, rr, -1.0 * rr, -1.0 * rr, -1.0 * rr};
                g.setColor(papier);
                poly(g, godet);
                g.setColor(alpha(Color.WHITE, 130));
                g.setStroke(new BasicStroke((float) (rr * 0.07)));
                for (int k = -2; k <= 2; k++) {
                    g.draw(new Line2D.Double(k / 3.0 * 0.8 * rr, -0.04 * rr, k / 3.0 * 1.0 * rr, -0.96 * rr));
                }
                g.setStroke(new BasicStroke(2));
                g.setColor(melange(papier, Color.BLACK, 0.25));
                contour(g, godet);
                Color glace = melange(c2, Color.WHITE, 0.35);
                double[][] couches = {{1.05, -1.55, 0.95}, {0.82, -2.0, 0.8}, {0.55, -2.4, 0.7}};
                for (double[] c : couches) {
                    g.setColor(glace);
                    ov(g, 0, (c[1] + c[2] / 2) * rr, c[0] * rr, c[2] * rr / 2);
                    g.setColor(melange(glace, Color.BLACK, 0.18));
                    ovContour(g, 0, (c[1] + c[2] / 2) * rr, c[0] * rr, c[2] * rr / 2);
                    g.setColor(alpha(Color.WHITE, 150));
                    ov(g, -c[0] * rr * 0.4, (c[1] + c[2] * 0.3) * rr, c[0] * rr * 0.25, c[2] * rr * 0.1);
                }
                for (int k = 0; k < 7; k++) { // vermicelles sur la glace
                    double x = (h(o.graine, k) - 0.5) * 1.3 * rr, y = (-1.95 + h(o.graine, k + 20) * 0.6) * rr;
                    g.setColor(pal[k % pal.length]);
                    g.setStroke(new BasicStroke((float) (rr * 0.07), BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                    double a = h(o.graine, k + 40) * 3.14;
                    g.draw(new Line2D.Double(x, y, x + Math.cos(a) * 0.14 * rr, y + Math.sin(a) * 0.07 * rr));
                }
                g.setColor(new Color(226, 48, 86));
                ov(g, 0, -2.5 * rr, 0.22 * rr, 0.22 * rr);
                g.setColor(alpha(Color.WHITE, 190));
                ov(g, -0.07 * rr, -2.58 * rr, 0.06 * rr, 0.05 * rr);
                break;
            }
            case 1: { // sucette-arbre
                ombre(g, 0, 0, 0.6 * rr, 0.2 * rr);
                g.setColor(new Color(255, 246, 250));
                g.fill(new Rectangle2D.Double(-0.11 * rr, -2.5 * rr, 0.22 * rr, 2.5 * rr));
                g.setColor(c1);
                g.setStroke(new BasicStroke((float) (rr * 0.08)));
                for (int k = 0; k < 9; k++) {
                    g.draw(new Line2D.Double(-0.11 * rr, -k * 0.28 * rr, 0.11 * rr, -k * 0.28 * rr - 0.14 * rr));
                }
                double hy = -3.0 * rr;
                for (int k = 6; k >= 1; k--) {
                    g.setColor(k % 2 == 0 ? Color.WHITE : melange(c1, Color.WHITE, k == 5 ? 0.0 : 0.1));
                    ov(g, 0, hy, rr * k / 6, rr * k / 6 * 0.97);
                }
                g.setStroke(new BasicStroke(2.5f));
                g.setColor(melange(c1, Color.BLACK, 0.3));
                ovContour(g, 0, hy, rr, rr * 0.97);
                g.setColor(alpha(Color.WHITE, 190));
                ov(g, -0.42 * rr, hy - 0.5 * rr, 0.18 * rr, 0.1 * rr);
                break;
            }
            case 2: { // cristaux de sucre candi
                ombre(g, 0, 0.05 * rr, 1.15 * rr, 0.4 * rr);
                g.setColor(melange(c1, Color.WHITE, 0.45));
                ov(g, 0, -0.35 * rr, rr, 0.42 * rr);
                for (int k = 0; k < 5; k++) {
                    double x = (k - 2) * 0.4 * rr + (h(o.graine, k) - 0.5) * 0.16 * rr;
                    double hh = (1.0 + h(o.graine, k + 9) * 1.1 - Math.abs(k - 2) * 0.22) * rr;
                    double base = -0.3 * rr + Math.abs(k - 2) * 0.05 * rr;
                    Color cc = pal[(i1 + k) % pal.length];
                    double[] cris = {x - 0.22 * rr, base, x - 0.16 * rr, base - hh, x, base - hh - 0.26 * rr,
                            x + 0.16 * rr, base - hh, x + 0.22 * rr, base};
                    g.setColor(alpha(melange(cc, Color.WHITE, 0.25), 225));
                    poly(g, cris);
                    g.setColor(alpha(Color.WHITE, 120));
                    poly(g, x - 0.22 * rr, base, x - 0.16 * rr, base - hh, x, base - hh - 0.26 * rr, x - 0.02 * rr, base);
                    g.setColor(alpha(Color.WHITE, 200));
                    g.setStroke(new BasicStroke(1.6f));
                    contour(g, cris);
                }
                break;
            }
            default: { // arbre à barbe à papa
                ombre(g, 0, 0, 0.9 * rr, 0.3 * rr);
                g.setColor(new Color(255, 236, 226));
                g.fill(new RoundRectangle2D.Double(-0.17 * rr, -1.6 * rr, 0.34 * rr, 1.6 * rr, 0.12 * rr, 0.12 * rr));
                g.setColor(alpha(c1, 140));
                g.setStroke(new BasicStroke((float) (rr * 0.06)));
                for (int k = 0; k < 5; k++) {
                    g.draw(new Line2D.Double(-0.17 * rr, -k * 0.32 * rr, 0.17 * rr, -k * 0.32 * rr - 0.12 * rr));
                }
                Color nuage = o.graine < 0.5 ? new Color(255, 184, 216) : new Color(176, 208, 255);
                double[][] bouffees = {{-0.7, -2.3, 0.7}, {0.7, -2.3, 0.7}, {0, -3.0, 0.85}, {-0.45, -2.8, 0.7},
                        {0.45, -2.8, 0.7}, {0, -2.2, 0.8}};
                for (double[] b : bouffees) {
                    g.setColor(nuage);
                    ov(g, b[0] * rr, b[1] * rr, b[2] * rr, b[2] * rr * 0.9);
                }
                g.setColor(alpha(Color.WHITE, 130));
                for (double[] b : bouffees) ov(g, (b[0] - 0.2) * rr, (b[1] - 0.25) * rr, b[2] * rr * 0.35, b[2] * rr * 0.25);
                g.setColor(alpha(melange(nuage, Color.BLACK, 0.2), 90));
                ov(g, 0.35 * rr, -2.0 * rr, 0.7 * rr, 0.3 * rr);
                break;
            }
        }
    }

    // ==================================================================
    // Monde 2 : Forêt de Guimauve - nuit féerique
    // ==================================================================
    private static void foret(Graphics2D g, Obstacle o, double anim) {
        double u = o.echelle;
        Color lilas = new Color(205, 185, 245), blanc = new Color(250, 245, 255), menthe = new Color(190, 235, 225);
        switch (o.type) {
            case 0: { // arbre guimauve
                ombre(g, 0, 0.04 * u, u, 0.3 * u);
                g.setColor(new Color(220, 200, 255, 26));
                ov(g, 0, -2.7 * u, 1.9 * u, 1.4 * u);
                g.setColor(new Color(250, 238, 230));
                g.fill(new RoundRectangle2D.Double(-0.34 * u, -2.2 * u, 0.68 * u, 2.2 * u, 0.3 * u, 0.3 * u));
                g.setColor(new Color(140, 110, 190, 80));
                g.fill(new Rectangle2D.Double(0.06 * u, -2.1 * u, 0.26 * u, 2.1 * u));
                g.setColor(new Color(150, 120, 190, 110));
                g.setStroke(new BasicStroke(1.6f));
                for (int k = 1; k < 5; k++) {
                    g.draw(new Line2D.Double(-0.3 * u, -k * 0.42 * u, 0.3 * u, -k * 0.42 * u));
                }
                double[][] boules = {{-0.75, -2.35}, {0.75, -2.35}, {-0.45, -3.05}, {0.45, -3.05}, {0, -2.7}};
                Color[] cols = {lilas, menthe, blanc, lilas, blanc};
                for (int k = 0; k < boules.length; k++) {
                    double bx = boules[k][0] * u - 0.62 * u, by = boules[k][1] * u - 0.5 * u;
                    Color c = o.graine > 0.5 && k % 2 == 0 ? menthe : cols[k];
                    g.setColor(c);
                    g.fill(new RoundRectangle2D.Double(bx, by, 1.24 * u, u, 0.6 * u, 0.6 * u));
                    g.setColor(melange(c, new Color(110, 90, 170), 0.35));
                    g.setStroke(new BasicStroke(2f));
                    g.draw(new RoundRectangle2D.Double(bx, by, 1.24 * u, u, 0.6 * u, 0.6 * u));
                    g.setColor(alpha(Color.WHITE, 170));
                    ov(g, bx + 0.3 * u, by + 0.25 * u, 0.2 * u, 0.1 * u);
                }
                for (int k = 0; k < 2; k++) { // étincelles qui scintillent
                    double a = Math.max(0, Math.sin(anim * 2.5 + k * 2 + o.graine * 6));
                    g.setColor(new Color(255, 255, 220, (int) (230 * a)));
                    Dessin.etoile(g, (k == 0 ? -0.6 : 0.7) * u, (-3.4 + k * 0.5) * u, 0.12 * u, 0.04 * u, anim);
                }
                break;
            }
            case 1: { // champignon géant
                Color cap = o.graine > 0.5 ? new Color(255, 150, 205) : new Color(196, 150, 255);
                ombre(g, 0, 0.04 * u, u, 0.3 * u);
                g.setColor(alpha(cap, 34));
                ov(g, 0, -1.7 * u, 2.0 * u, 1.3 * u);
                g.setColor(new Color(250, 240, 232));
                g.fill(new RoundRectangle2D.Double(-0.32 * u, -1.6 * u, 0.64 * u, 1.6 * u, 0.3 * u, 0.3 * u));
                g.setColor(new Color(150, 120, 170, 80));
                g.fill(new Rectangle2D.Double(0.04 * u, -1.5 * u, 0.26 * u, 1.5 * u));
                g.setColor(melange(cap, Color.BLACK, 0.4));
                ov(g, 0, -1.55 * u, 1.38 * u, 0.34 * u);
                g.setColor(cap);
                g.fill(new Arc2D.Double(-1.4 * u, -2.85 * u, 2.8 * u, 2.5 * u, 0, 180, Arc2D.CHORD));
                g.setColor(alpha(Color.WHITE, 90));
                g.fill(new Arc2D.Double(-1.1 * u, -2.7 * u, 1.5 * u, 1.3 * u, 40, 100, Arc2D.CHORD));
                g.setColor(alpha(Color.WHITE, 225));
                double[][] points = {{-0.7, -1.9, 0.2}, {0.2, -2.45, 0.24}, {0.8, -1.85, 0.17}, {-0.2, -1.75, 0.13},
                        {-0.95, -1.7, 0.1}};
                for (double[] pt : points) ov(g, pt[0] * u, pt[1] * u, pt[2] * u, pt[2] * u * 0.8);
                for (int k = 0; k < 5; k++) { // lueur sous le chapeau
                    double a = 0.5 + 0.5 * Math.sin(anim * 2 + k + o.graine * 9);
                    g.setColor(new Color(255, 255, 190, (int) (150 * a + 50)));
                    ov(g, (k - 2) * 0.5 * u, -1.5 * u, 0.06 * u, 0.06 * u);
                }
                break;
            }
            case 2: { // étang
                g.setColor(new Color(10, 30, 44, 160));
                ov(g, 0, 0.06 * u, 1.12 * u, 0.86 * u);
                g.setColor(new Color(92, 138, 118));
                ov(g, 0, 0, 1.08 * u, 0.82 * u);
                g.setColor(new Color(40, 118, 152));
                ov(g, 0, 0.02 * u, 0.94 * u, 0.7 * u);
                g.setColor(new Color(72, 168, 196));
                ov(g, -0.06 * u, -0.06 * u, 0.7 * u, 0.5 * u);
                g.setColor(alpha(new Color(190, 240, 255), 80));
                ov(g, -0.3 * u, -0.25 * u, 0.3 * u, 0.1 * u);
                g.setStroke(new BasicStroke(1.8f));
                for (int k = 0; k < 3; k++) {
                    double ph = (anim * 0.3 + k / 3.0 + o.graine) % 1.0;
                    double cx = (h(o.graine, k) - 0.5) * 0.6 * u, cy = (h(o.graine, k + 5) - 0.5) * 0.3 * u;
                    g.setColor(new Color(255, 255, 255, (int) (120 * (1 - ph))));
                    ovContour(g, cx, cy, ph * 0.5 * u, ph * 0.32 * u);
                }
                for (int k = 0; k < 3; k++) { // nénuphars
                    double lx = (h(o.graine, k + 11) - 0.5) * 1.1 * u, ly = (h(o.graine, k + 14) - 0.5) * 0.7 * u;
                    g.setColor(new Color(64, 156, 104));
                    ov(g, lx, ly, 0.17 * u, 0.1 * u);
                    if (k == 0) {
                        g.setColor(new Color(255, 190, 220));
                        ov(g, lx, ly - 0.03 * u, 0.06 * u, 0.045 * u);
                    }
                }
                break;
            }
            default: { // pierre de lune
                ombre(g, 0, 0.04 * u, 1.1 * u, 0.36 * u);
                g.setColor(new Color(126, 128, 168));
                ov(g, 0, -0.55 * u, u, 0.7 * u);
                g.setColor(new Color(92, 94, 134, 160));
                ov(g, 0.15 * u, -0.3 * u, 0.85 * u, 0.4 * u);
                g.setColor(new Color(170, 172, 212));
                ov(g, -0.12 * u, -0.85 * u, 0.72 * u, 0.4 * u);
                g.setColor(new Color(86, 150, 124, 200));
                ov(g, 0.32 * u, -0.98 * u, 0.4 * u, 0.15 * u);
                ov(g, -0.55 * u, -0.7 * u, 0.22 * u, 0.1 * u);
                double a = 0.55 + 0.45 * Math.sin(anim * 1.8 + o.graine * 7);
                g.setColor(new Color(190, 235, 255, (int) (230 * a)));
                g.setStroke(new BasicStroke((float) (0.07 * u), BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                g.draw(new Arc2D.Double(-0.38 * u, -0.9 * u, 0.5 * u, 0.5 * u, 70, 220, Arc2D.OPEN));
                break;
            }
        }
    }

    // ==================================================================
    // Monde 3 : Volcan Chocolat
    // ==================================================================
    private static void volcan(Graphics2D g, Obstacle o, double anim) {
        double u = o.echelle;
        Color caramel = new Color(222, 146, 62);
        switch (o.type) {
            case 0: { // pic de chocolat
                ombre(g, 0, 0.03 * u, u, 0.3 * u);
                g.setColor(new Color(74, 42, 32));
                poly(g, 0.3 * u, 0, 0.5 * u, -1.3 * u, 0.85 * u, 0);
                double[] pic = {-0.66 * u, 0, -0.44 * u, -1.5 * u, -0.12 * u, -2.5 * u, 0.08 * u, -3.1 * u,
                        0.32 * u, -2.2 * u, 0.56 * u, -1.1 * u, 0.68 * u, 0};
                g.setColor(new Color(100, 58, 42));
                poly(g, pic);
                g.setColor(new Color(66, 36, 27));
                poly(g, 0.08 * u, -3.1 * u, 0.32 * u, -2.2 * u, 0.56 * u, -1.1 * u, 0.68 * u, 0, 0.12 * u, 0, 0.14 * u, -1.5 * u);
                g.setColor(alpha(new Color(255, 220, 190), 70));
                g.setStroke(new BasicStroke(2.2f));
                g.draw(new Line2D.Double(-0.1 * u, -2.5 * u, -0.38 * u, -1.4 * u));
                g.setColor(caramel);
                poly(g, -0.18 * u, -2.35 * u, 0.08 * u, -3.1 * u, 0.3 * u, -2.2 * u, 0.24 * u, -1.9 * u, 0.15 * u, -2.2 * u,
                        0.04 * u, -1.75 * u, -0.07 * u, -2.1 * u);
                g.setColor(alpha(Color.WHITE, 110));
                ov(g, 0.03 * u, -2.75 * u, 0.05 * u, 0.12 * u);
                break;
            }
            case 1: { // rocher fondu
                ombre(g, 0, 0.05 * u, 1.15 * u, 0.4 * u);
                g.setColor(new Color(84, 48, 36));
                ov(g, 0, -0.62 * u, u, 0.72 * u);
                g.setColor(new Color(52, 30, 24, 170));
                ov(g, 0.12 * u, -0.3 * u, 0.9 * u, 0.34 * u);
                g.setColor(new Color(124, 76, 54));
                ov(g, -0.14 * u, -0.95 * u, 0.7 * u, 0.36 * u);
                g.setColor(caramel);
                ov(g, -0.08 * u, -1.1 * u, 0.62 * u, 0.2 * u);
                for (int k = 0; k < 4; k++) { // coulées de caramel
                    double x = (-0.45 + k * 0.3) * u, len = (0.25 + h(o.graine, k) * 0.45) * u;
                    g.fill(new RoundRectangle2D.Double(x - 0.07 * u, -1.1 * u, 0.14 * u, len + 0.1 * u, 0.14 * u, 0.14 * u));
                }
                double a = 0.5 + 0.5 * Math.sin(anim * 2 + o.graine * 8);
                g.setColor(new Color(255, 130, 40, (int) (110 + 120 * a)));
                g.setStroke(new BasicStroke((float) (0.05 * u), BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                g.draw(new Line2D.Double(0.2 * u, -0.45 * u, 0.4 * u, -0.6 * u));
                g.draw(new Line2D.Double(0.4 * u, -0.6 * u, 0.34 * u, -0.78 * u));
                g.draw(new Line2D.Double(-0.5 * u, -0.4 * u, -0.3 * u, -0.5 * u));
                break;
            }
            case 2: { // mare de lave
                g.setColor(new Color(255, 90, 20, 36));
                ov(g, 0, 0, 1.55 * u, 1.15 * u);
                g.setColor(new Color(40, 20, 14));
                ov(g, 0, 0.05 * u, 1.14 * u, 0.88 * u);
                g.setColor(new Color(112, 58, 36));
                ov(g, 0, 0, 1.1 * u, 0.84 * u);
                g.setColor(new Color(255, 100, 24));
                ov(g, 0, 0, 0.97 * u, 0.73 * u);
                g.setColor(new Color(255, 160, 40));
                ov(g, 0, 0, 0.72 * u, 0.54 * u);
                double pulse = 1 + 0.07 * Math.sin(anim * 3 + o.graine * 6);
                g.setColor(new Color(255, 226, 120));
                ov(g, 0.05 * u, 0, 0.4 * u * pulse, 0.28 * u * pulse);
                for (int k = 0; k < 3; k++) { // bulles
                    double ph = (anim * 0.5 + k / 3.0 + o.graine) % 1.0;
                    double bx = (h(o.graine, k) - 0.5) * 1.2 * u, by = (h(o.graine, k + 3) - 0.5) * 0.8 * u;
                    g.setColor(new Color(255, 236, 160, (int) (230 * (1 - ph))));
                    ov(g, bx, by, ph * 0.14 * u, ph * 0.1 * u);
                }
                break;
            }
            default: { // cheminée volcanique
                ombre(g, 0, 0.05 * u, 1.2 * u, 0.4 * u);
                g.setColor(new Color(255, 120, 30, 40));
                ov(g, 0, -1.8 * u, 0.9 * u, 0.4 * u);
                g.setColor(new Color(92, 54, 40));
                poly(g, -1.0 * u, 0, -0.4 * u, -1.7 * u, 0.4 * u, -1.7 * u, u, 0);
                g.setColor(new Color(60, 34, 26));
                poly(g, 0.4 * u, -1.7 * u, u, 0, 0.35 * u, 0, 0.12 * u, -1.55 * u);
                g.setColor(caramel);
                g.setStroke(new BasicStroke((float) (0.11 * u), BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                g.draw(new Line2D.Double(-0.28 * u, -1.65 * u, -0.5 * u, -0.7 * u));
                g.draw(new Line2D.Double(0.05 * u, -1.65 * u, 0.12 * u, -0.9 * u));
                g.setColor(new Color(58, 30, 22));
                ov(g, 0, -1.7 * u, 0.44 * u, 0.15 * u);
                g.setColor(new Color(255, 140, 40));
                ov(g, 0, -1.7 * u, 0.35 * u, 0.1 * u);
                for (int k = 0; k < 4; k++) { // la fumée monte
                    double ph = (anim * 0.22 + k * 0.25 + o.graine) % 1.0;
                    double y = -1.8 * u - ph * 2.4 * u, r = (0.2 + ph * 0.55) * u;
                    double x = Math.sin(ph * 6 + k) * 0.25 * u;
                    g.setColor(new Color(70, 58, 56, (int) (130 * (1 - ph))));
                    ov(g, x, y, r, r * 0.85);
                }
                break;
            }
        }
    }
}
