package org.java.ressources.rendu;

import org.java.ressources.donnees.Monde;

import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Composite;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.util.Random;

import static org.java.ressources.donnees.Constantes.DT;
import static org.java.ressources.donnees.Constantes.H;
import static org.java.ressources.donnees.Constantes.W;
import static org.java.ressources.rendu.Dessin.alpha;
import static org.java.ressources.rendu.Dessin.ov;

/**
 * Particules d'ambiance en avant-plan, propres à chaque monde :
 * confettis (prairie), lucioles et brume (forêt), braises et fumée (volcan).
 * Elles vivent dans le repère de l'écran et glissent un peu quand la caméra bouge (parallaxe).
 */
public class Ambiance {
    private final int type;
    /** Par particule : x, y, phase, taille, couleur. */
    private final double[][] p;
    /** Grandes formes lentes (brume ou fumée) : x, y, rayon, phase. */
    private final double[][] brume;
    private final Color[] pastel;
    /** Une image par nuage de brume (dessinée une fois, à la résolution de l'écran) et son point d'ancrage. */
    private BufferedImage[] nuages;
    private int[] nuageAncreX, nuageAncreY;
    private int nuagesVersion = -1;

    public Ambiance(Monde m) {
        type = m.id;
        Random r = new Random(77 + m.id);
        int n = type == 0 ? 50 : type == 1 ? 46 : 60;
        p = new double[n][5];
        for (double[] a : p) {
            a[0] = r.nextDouble() * W;
            a[1] = r.nextDouble() * H;
            a[2] = r.nextDouble() * 6.28;
            a[3] = 0.6 + r.nextDouble() * 0.8;
            a[4] = r.nextInt(5);
        }
        int nb = type == 0 ? 0 : 7;
        brume = new double[nb][4];
        for (double[] b : brume) {
            b[0] = r.nextDouble() * W;
            b[1] = r.nextDouble() * H;
            b[2] = 120 + r.nextDouble() * 140;
            b[3] = r.nextDouble() * 6.28;
        }
        pastel = new Color[]{new Color(255, 160, 190), new Color(160, 235, 200), new Color(190, 170, 240),
                new Color(255, 215, 150), new Color(150, 200, 255)};
    }

    /** dcx, dcy : déplacement de la caméra depuis la dernière image. */
    public void maj(double dcx, double dcy, double anim) {
        double par = type == 1 ? 0.7 : 0.55;
        for (double[] a : p) {
            if (type == 0) { // confettis qui tombent en se balançant
                a[0] += (Math.sin(anim * 1.4 + a[2]) * 24) * DT;
                a[1] += (26 + 22 * a[3]) * DT;
            } else if (type == 1) { // lucioles qui flânent
                a[0] += Math.cos(anim * 0.7 + a[2] * 2) * 20 * DT;
                a[1] += Math.sin(anim * 0.9 + a[2]) * 16 * DT;
            } else { // braises qui montent
                a[0] += Math.sin(anim * 1.3 + a[2]) * 14 * DT;
                a[1] -= (30 + 50 * a[3]) * DT;
            }
            a[0] -= dcx * par;
            a[1] -= dcy * par;
            a[0] = ((a[0] + 20) % (W + 40) + (W + 40)) % (W + 40) - 20;
            a[1] = ((a[1] + 20) % (H + 40) + (H + 40)) % (H + 40) - 20;
        }
        for (double[] b : brume) {
            b[0] += (type == 1 ? 8 : -6) * DT - dcx * 0.9;
            b[1] -= dcy * 0.9;
            double m = b[2];
            b[0] = ((b[0] + m) % (W + 2 * m) + (W + 2 * m)) % (W + 2 * m) - m;
            b[1] = ((b[1] + m) % (H + 2 * m) + (H + 2 * m)) % (H + 2 * m) - m;
        }
    }

    public void dessiner(Graphics2D g, double anim) {
        // brume (forêt) ou fumée (volcan), sous les particules
        if (brume.length > 0) dessinerBrume(g, anim);
        AffineTransform old = g.getTransform();
        for (double[] a : p) {
            if (type == 0) {
                g.setTransform(old);
                g.translate(a[0], a[1]);
                g.rotate(anim * 1.5 + a[2]);
                g.setColor(alpha(pastel[(int) a[4]], 200));
                g.fillRoundRect((int) (-4 * a[3]), (int) (-2 * a[3]), (int) (8 * a[3]), (int) (4 * a[3]), 2, 2);
            } else if (type == 1) {
                double clign = 0.35 + 0.65 * Math.max(0, Math.sin(anim * 2.2 + a[2] * 3));
                g.setColor(new Color(220, 255, 140, (int) (28 * clign)));
                ov(g, a[0], a[1], 9 * a[3], 9 * a[3]);
                g.setColor(new Color(230, 255, 170, (int) (90 * clign)));
                ov(g, a[0], a[1], 4.5 * a[3], 4.5 * a[3]);
                g.setColor(new Color(255, 255, 220, (int) (255 * clign)));
                ov(g, a[0], a[1], 1.8 * a[3], 1.8 * a[3]);
            } else {
                double clign = 0.5 + 0.5 * Math.sin(anim * 5 + a[2] * 4);
                g.setColor(new Color(255, 120, 30, (int) (60 * clign + 20)));
                ov(g, a[0], a[1], 5 * a[3], 5 * a[3]);
                g.setColor(new Color(255, 200, 90, (int) (200 * clign + 40)));
                ov(g, a[0], a[1], 1.8 * a[3], 1.8 * a[3]);
            }
        }
        g.setTransform(old);
    }

    /** Dessine chaque nuage de brume avec la pulsation qui lui est propre (transparence appliquée à l'affichage). */
    private void dessinerBrume(Graphics2D g, double anim) {
        double sc = CacheRendu.echelle();
        if (nuages == null || nuagesVersion != CacheRendu.version()) creerNuages(sc);
        AffineTransform t = g.getTransform();
        Composite ancien = g.getComposite();
        g.setTransform(new AffineTransform());
        for (int i = 0; i < brume.length; i++) {
            double[] b = brume[i];
            double pulse = 0.6 + 0.4 * Math.sin(anim * 0.5 + b[3]);
            g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, (float) Math.clamp(pulse, 0, 1)));
            int x = (int) Math.round(sc * b[0] + t.getTranslateX()) - nuageAncreX[i];
            int y = (int) Math.round(sc * b[1] + t.getTranslateY()) - nuageAncreY[i];
            g.drawImage(nuages[i], x, y, null);
        }
        g.setComposite(ancien);
        g.setTransform(t);
    }

    /** Les deux ellipses de chaque nuage, à leur transparence maximale. */
    private void creerNuages(double sc) {
        nuagesVersion = CacheRendu.version();
        nuages = new BufferedImage[brume.length];
        nuageAncreX = new int[brume.length];
        nuageAncreY = new int[brume.length];
        Color c = type == 1 ? new Color(200, 225, 255, 16) : new Color(40, 20, 16, 34);
        for (int i = 0; i < brume.length; i++) {
            double r = brume[i][2];
            int ax = (int) Math.ceil(r * sc) + 2, ay = (int) Math.ceil(r * 0.6 * sc) + 2;
            int w = ax + (int) Math.ceil(1.15 * r * sc) + 2, h = 2 * ay;
            BufferedImage img = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
            Graphics2D g = img.createGraphics();
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.translate(ax, ay);
            g.scale(sc, sc);
            g.setColor(c);
            ov(g, 0, 0, r, r * 0.55);
            ov(g, r * 0.4, -r * 0.1, r * 0.7, r * 0.4);
            g.dispose();
            nuages[i] = img;
            nuageAncreX[i] = ax;
            nuageAncreY[i] = ay;
        }
    }
}