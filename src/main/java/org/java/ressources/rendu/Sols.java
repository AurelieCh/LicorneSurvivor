package org.java.ressources.rendu;

import org.java.ressources.donnees.Monde;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.AffineTransform;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Path2D;
import java.awt.image.BufferedImage;
import java.util.Random;
import java.util.function.Consumer;

import static org.java.ressources.rendu.Dessin.alpha;

/**
 * Textures de sol, une par monde, fabriquées par le code au premier affichage.
 * Chaque texture se répète sans couture : on la pave sur toute la carte.
 */
public final class Sols {
    private Sols() {
    }

    public static final int T = 256;
    private static final BufferedImage[] CACHE = new BufferedImage[Monde.TOUS.length];

    public static BufferedImage de(Monde m) {
        if (CACHE[m.id] == null) CACHE[m.id] = creer(m);
        return CACHE[m.id];
    }

    /** Dessine le motif en (x, y) puis ses 8 copies décalées, pour que la texture boucle sans coupure. */
    private static void repeter(Graphics2D g, double x, double y, Consumer<Graphics2D> motif) {
        AffineTransform old = g.getTransform();
        for (int dx = -1; dx <= 1; dx++) {
            for (int dy = -1; dy <= 1; dy++) {
                g.setTransform(old);
                g.translate(x + dx * T, y + dy * T);
                motif.accept(g);
            }
        }
        g.setTransform(old);
    }

    private static BufferedImage creer(Monde m) {
        BufferedImage img = new BufferedImage(T, T, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = img.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setColor(m.sol);
        g.fillRect(0, 0, T, T);
        Random r = new Random(1000 + m.id);
        if (m.id == 0) prairie(g, r, m);
        else if (m.id == 1) foret(g, r);
        else volcan(g, r);
        g.dispose();
        return img;
    }

    /** Prairie : nappe vichy rose pâle, taches de crème et vermicelles. */
    private static void prairie(Graphics2D g, Random r, Monde m) {
        for (int i = 0; i < 4; i++) {
            for (int j = 0; j < 4; j++) {
                if ((i + j) % 2 == 0) {
                    g.setColor(new Color(255, 255, 255, 80));
                    g.fillRect(i * 64, j * 64, 64, 64);
                }
            }
        }
        for (int k = 0; k < 18; k++) {
            final double rad = 16 + r.nextDouble() * 34;
            final Color c = k % 2 == 0 ? new Color(255, 190, 222, 55) : new Color(200, 225, 255, 60);
            repeter(g, r.nextDouble() * T, r.nextDouble() * T, gg -> {
                gg.setColor(c);
                gg.fill(new Ellipse2D.Double(-rad, -rad * 0.7, 2 * rad, 1.4 * rad));
            });
        }
        for (int k = 0; k < 80; k++) {
            final Color c = m.palette[r.nextInt(m.palette.length)];
            final double a = r.nextDouble() * Math.PI;
            repeter(g, r.nextDouble() * T, r.nextDouble() * T, gg -> {
                gg.rotate(a);
                gg.setColor(alpha(c, 210));
                gg.fillRoundRect(-4, -1, 8, 3, 3, 3);
            });
        }
    }

    /** Forêt : mousse sombre, tâches de lumière et brins d'herbe. */
    private static void foret(Graphics2D g, Random r) {
        for (int k = 0; k < 40; k++) {
            final double rad = 18 + r.nextDouble() * 50;
            final Color c = k % 3 == 0 ? new Color(88, 140, 138, 60) : new Color(30, 64, 86, 85);
            repeter(g, r.nextDouble() * T, r.nextDouble() * T, gg -> {
                gg.setColor(c);
                gg.fill(new Ellipse2D.Double(-rad, -rad * 0.65, 2 * rad, 1.3 * rad));
            });
        }
        g.setStroke(new BasicStroke(1.5f));
        for (int k = 0; k < 90; k++) {
            final double h = 4 + r.nextDouble() * 6;
            final double pente = r.nextDouble() * 4 - 2;
            final boolean clair = r.nextBoolean();
            repeter(g, r.nextDouble() * T, r.nextDouble() * T, gg -> {
                gg.setColor(clair ? new Color(120, 190, 160, 140) : new Color(26, 58, 70, 170));
                gg.drawLine(0, 0, (int) pente, (int) -h);
            });
        }
        for (int k = 0; k < 70; k++) {
            final double s = 1 + r.nextDouble() * 1.5;
            repeter(g, r.nextDouble() * T, r.nextDouble() * T, gg -> {
                gg.setColor(new Color(190, 240, 220, 130));
                gg.fill(new Ellipse2D.Double(0, 0, s, s));
            });
        }
    }

    /** Volcan : chocolat noir, veines de caramel, fissures de lave incandescente. */
    private static void volcan(Graphics2D g, Random r) {
        for (int k = 0; k < 40; k++) {
            final double rad = 16 + r.nextDouble() * 46;
            final Color c = k % 2 == 0 ? new Color(98, 60, 46, 90) : new Color(50, 28, 22, 110);
            repeter(g, r.nextDouble() * T, r.nextDouble() * T, gg -> {
                gg.setColor(c);
                gg.fill(new Ellipse2D.Double(-rad, -rad * 0.7, 2 * rad, 1.4 * rad));
            });
        }
        // veines de caramel, puis fissures de lave (halo + trait)
        for (int k = 0; k < 8; k++) {
            final Path2D.Double chemin = marche(r, 7, 26);
            repeter(g, r.nextDouble() * T, r.nextDouble() * T, gg -> {
                gg.setStroke(new BasicStroke(3.5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                gg.setColor(new Color(196, 128, 62, 110));
                gg.draw(chemin);
            });
        }
        for (int k = 0; k < 5; k++) {
            final Path2D.Double chemin = marche(r, 6, 22);
            repeter(g, r.nextDouble() * T, r.nextDouble() * T, gg -> {
                gg.setStroke(new BasicStroke(8f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                gg.setColor(new Color(255, 90, 20, 45));
                gg.draw(chemin);
                gg.setStroke(new BasicStroke(2.2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                gg.setColor(new Color(255, 150, 50, 200));
                gg.draw(chemin);
            });
        }
        for (int k = 0; k < 45; k++) {
            final double s = 3 + r.nextDouble() * 5;
            repeter(g, r.nextDouble() * T, r.nextDouble() * T, gg -> {
                gg.setColor(new Color(40, 22, 18, 150));
                gg.fill(new Ellipse2D.Double(0, 1, s * 1.3, s * 0.8));
                gg.setColor(new Color(118, 80, 62));
                gg.fill(new Ellipse2D.Double(0, 0, s * 1.2, s * 0.8));
            });
        }
    }

    /** Ligne brisée au hasard, partant de (0, 0). */
    private static Path2D.Double marche(Random r, int segments, double pas) {
        Path2D.Double p = new Path2D.Double();
        p.moveTo(0, 0);
        double x = 0, y = 0, a = r.nextDouble() * Math.PI * 2;
        for (int i = 0; i < segments; i++) {
            a += (r.nextDouble() - 0.5) * 1.4;
            x += Math.cos(a) * pas;
            y += Math.sin(a) * pas;
            p.lineTo(x, y);
        }
        return p;
    }
}
