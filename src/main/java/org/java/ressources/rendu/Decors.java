package org.java.ressources.rendu;

import org.java.ressources.donnees.Monde;
import org.java.ressources.entites.Decor;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.geom.Arc2D;
import java.awt.geom.Line2D;
import java.awt.geom.Path2D;

import static org.java.ressources.rendu.Dessin.alpha;
import static org.java.ressources.rendu.Dessin.h;
import static org.java.ressources.rendu.Dessin.melange;
import static org.java.ressources.rendu.Dessin.ov;
import static org.java.ressources.rendu.Dessin.poly;

/** Dessin des décors posés au sol (fleurs, mousse, fissures de lave...). Petits et nombreux : on reste simple. */
public final class Decors {
    private Decors() {
    }

    public static void dessiner(Graphics2D g, Decor d, double anim) {
        switch (d.monde) {
            case 0:
                prairie(g, d);
                break;
            case 1:
                foret(g, d, anim);
                break;
            default:
                volcan(g, d, anim);
                break;
        }
    }

    private static void prairie(Graphics2D g, Decor d) {
        double x = d.x, y = d.y, t = d.taille;
        Color[] pal = Monde.TOUS[0].palette;
        Color c = pal[(int) (d.graine * 50) % pal.length];
        switch (d.type) {
            case 0: // fleur
                for (int k = 0; k < 5; k++) {
                    double a = k * 1.2566 + d.graine * 6;
                    g.setColor(melange(c, Color.WHITE, 0.2));
                    ov(g, x + Math.cos(a) * t * 0.45, y + Math.sin(a) * t * 0.36, t * 0.34, t * 0.27);
                }
                g.setColor(new Color(255, 214, 90));
                ov(g, x, y, t * 0.25, t * 0.2);
                break;
            case 1: // touffe d'herbe sucrée
                g.setStroke(new BasicStroke(2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                for (int k = -1; k <= 1; k++) {
                    g.setColor(k == 0 ? new Color(120, 205, 160) : new Color(150, 222, 178));
                    Path2D.Double p = new Path2D.Double();
                    p.moveTo(x + k * 2, y);
                    p.quadTo(x + k * 5, y - t * 0.6, x + k * 8 + d.graine * 4, y - t * (0.9 - Math.abs(k) * 0.2));
                    g.draw(p);
                }
                break;
            case 2: // vermicelles
                g.setStroke(new BasicStroke(2.4f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                for (int k = 0; k < 6; k++) {
                    double px = x + (h(d.graine, k) - 0.5) * t * 2, py = y + (h(d.graine, k + 9) - 0.5) * t;
                    double a = h(d.graine, k + 18) * 3.14;
                    g.setColor(pal[k % pal.length]);
                    g.draw(new Line2D.Double(px, py, px + Math.cos(a) * 5, py + Math.sin(a) * 3));
                }
                break;
            case 3: // nappe de chantilly
                g.setColor(new Color(255, 255, 255, 80));
                ov(g, x, y, t, t * 0.5);
                ov(g, x + t * 0.5, y - t * 0.1, t * 0.7, t * 0.38);
                ov(g, x - t * 0.5, y + t * 0.1, t * 0.6, t * 0.3);
                g.setColor(new Color(255, 255, 255, 70));
                ov(g, x - t * 0.2, y - t * 0.15, t * 0.4, t * 0.15);
                break;
            default: // bonbon emballé
                g.setColor(c);
                poly(g, x - t * 0.5, y, x - t, y - t * 0.4, x - t, y + t * 0.4);
                poly(g, x + t * 0.5, y, x + t, y - t * 0.4, x + t, y + t * 0.4);
                ov(g, x, y, t * 0.55, t * 0.42);
                g.setColor(alpha(Color.WHITE, 170));
                ov(g, x - t * 0.15, y - t * 0.12, t * 0.2, t * 0.1);
                break;
        }
    }

    private static void foret(Graphics2D g, Decor d, double anim) {
        double x = d.x, y = d.y, t = d.taille;
        switch (d.type) {
            case 0: { // petit champignon lumineux
                double a = 0.55 + 0.45 * Math.sin(anim * 2 + d.graine * 9);
                g.setColor(new Color(200, 255, 220, (int) (36 * a)));
                ov(g, x, y - t * 0.3, t * 1.7, t * 1.1);
                g.setColor(new Color(240, 230, 220));
                g.fillRect((int) (x - t * 0.18), (int) (y - t * 0.6), (int) (t * 0.36), (int) (t * 0.6));
                g.setColor(d.graine > 0.5 ? new Color(150, 240, 220) : new Color(210, 170, 255));
                g.fill(new Arc2D.Double(x - t * 0.6, y - t * 1.1, t * 1.2, t, 0, 180, Arc2D.CHORD));
                g.setColor(alpha(Color.WHITE, 200));
                ov(g, x - t * 0.18, y - t * 0.75, t * 0.1, t * 0.08);
                break;
            }
            case 1: // fougère
                g.setStroke(new BasicStroke(1.8f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                for (int k = -2; k <= 2; k++) {
                    g.setColor(k % 2 == 0 ? new Color(88, 176, 148) : new Color(120, 206, 168));
                    Path2D.Double p = new Path2D.Double();
                    p.moveTo(x, y);
                    p.quadTo(x + k * t * 0.35, y - t * 0.6, x + k * t * 0.7, y - t * (1.0 - Math.abs(k) * 0.15));
                    g.draw(p);
                }
                break;
            case 2: // tache de mousse
                g.setColor(new Color(110, 190, 150, 48));
                ov(g, x, y, t * 0.9, t * 0.5);
                g.setColor(new Color(140, 215, 170, 40));
                ov(g, x - t * 0.15, y - t * 0.05, t * 0.5, t * 0.28);
                break;
            case 3: { // fleur de lune
                double a = 0.5 + 0.5 * Math.sin(anim * 1.6 + d.graine * 7);
                g.setColor(new Color(220, 240, 255, (int) (30 + 34 * a)));
                ov(g, x, y, t * 1.7, t * 1.3);
                for (int k = 0; k < 5; k++) {
                    double ang = k * 1.2566 + d.graine * 6;
                    g.setColor(new Color(236, 244, 255));
                    ov(g, x + Math.cos(ang) * t * 0.42, y + Math.sin(ang) * t * 0.34, t * 0.3, t * 0.22);
                }
                g.setColor(new Color(255, 240, 160));
                ov(g, x, y, t * 0.22, t * 0.18);
                break;
            }
            default: // petite pierre
                g.setColor(new Color(0, 0, 0, 50));
                ov(g, x, y + t * 0.2, t * 0.7, t * 0.25);
                g.setColor(new Color(124, 130, 160));
                ov(g, x, y, t * 0.65, t * 0.42);
                g.setColor(new Color(170, 176, 204));
                ov(g, x - t * 0.15, y - t * 0.1, t * 0.35, t * 0.18);
                break;
        }
    }

    private static void volcan(Graphics2D g, Decor d, double anim) {
        double x = d.x, y = d.y, t = d.taille;
        switch (d.type) {
            case 0: { // fissure de lave
                Path2D.Double p = new Path2D.Double();
                p.moveTo(x - t * 0.5, y);
                double px = x - t * 0.5, py = y;
                for (int k = 0; k < 5; k++) {
                    px += t * 0.22;
                    py += (h(d.graine, k) - 0.5) * t * 0.5;
                    p.lineTo(px, py);
                }
                double a = 0.5 + 0.5 * Math.sin(anim * 2 + d.graine * 9);
                g.setStroke(new BasicStroke(7f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                g.setColor(new Color(255, 90, 20, (int) (30 + 20 * a)));
                g.draw(p);
                g.setStroke(new BasicStroke(2.2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                g.setColor(new Color(255, 160, 60, (int) (150 + 90 * a)));
                g.draw(p);
                break;
            }
            case 1: // pépite de chocolat
                g.setColor(new Color(0, 0, 0, 60));
                ov(g, x, y + t * 0.35, t * 0.8, t * 0.3);
                g.setColor(new Color(92, 54, 40));
                poly(g, x - t * 0.6, y + t * 0.2, x - t * 0.3, y - t * 0.4, x + t * 0.4, y - t * 0.3, x + t * 0.7, y + t * 0.25, x, y + t * 0.4);
                g.setColor(new Color(134, 86, 62));
                poly(g, x - t * 0.3, y - t * 0.4, x + t * 0.4, y - t * 0.3, x + t * 0.1, y, x - t * 0.4, y - t * 0.05);
                break;
            case 2: // flaque de caramel
                g.setColor(new Color(150, 90, 36, 200));
                ov(g, x, y + t * 0.04, t, t * 0.5);
                g.setColor(new Color(214, 140, 56, 230));
                ov(g, x, y, t * 0.94, t * 0.46);
                g.setColor(new Color(255, 214, 140, 150));
                ov(g, x - t * 0.3, y - t * 0.15, t * 0.35, t * 0.1);
                break;
            case 3: // éclats de roche
                g.setColor(new Color(60, 38, 30));
                for (int k = 0; k < 3; k++) {
                    double sx = x + (k - 1) * t * 0.7, sy = y + (h(d.graine, k) - 0.5) * t * 0.5;
                    poly(g, sx - t * 0.25, sy + t * 0.2, sx, sy - t * 0.35, sx + t * 0.28, sy + t * 0.22);
                }
                break;
            default: { // braise au sol
                double a = 0.5 + 0.5 * Math.sin(anim * 5 + d.graine * 20);
                g.setColor(new Color(255, 110, 30, (int) (40 + 40 * a)));
                ov(g, x, y, t * 2.4, t * 2.0);
                g.setColor(new Color(255, 190, 80, (int) (140 + 110 * a)));
                ov(g, x, y, t * 0.6, t * 0.5);
                break;
            }
        }
    }
}
