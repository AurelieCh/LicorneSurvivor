package org.java.ressources.rendu;

import org.java.ressources.jeu.Terrain;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Path2D;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import static org.java.ressources.donnees.Constantes.H;
import static org.java.ressources.donnees.Constantes.W;

/**
 * Dessine la géographie d'une carte par-dessus la texture de sol : teintes continues (clairières, sous-bois,
 * champs de fleurs), sentiers, rivière avec ses gués. Les formes sont calculées une seule fois par carte.
 */
final class RenduTerrain {
    private RenduTerrain() {
    }

    /** Taille d'une case de l'image de teinte, en pixels de carte. L'image est lissée à l'affichage. */
    private static final int PX = 32;

    private record Pal(Color clair, Color dense, Color fleur,
                       Color sentBord, Color sentCorps,
                       Color berge, Color eau, Color profond, Color reflet, Color lueur,
                       Color gue, Color pierre) {
    }

    private static final Pal[] PAL = {
            // Prairie Cupcake : sentiers de sucre, rivière de sirop bleuté
            new Pal(new Color(255, 246, 220, 70), new Color(160, 215, 200, 90), new Color(255, 170, 212, 70),
                    new Color(240, 185, 200), new Color(255, 238, 218),
                    new Color(222, 150, 200), new Color(160, 208, 255), new Color(124, 180, 242), new Color(255, 255, 255), new Color(190, 225, 255, 50),
                    new Color(255, 238, 218), new Color(255, 252, 246)),
            // Forêt de Guimauve : mousse claire / sous-bois sombre, rivière féerique
            new Pal(new Color(110, 175, 165, 70), new Color(8, 24, 42, 115), new Color(176, 150, 255, 55),
                    new Color(30, 62, 80), new Color(98, 144, 132),
                    new Color(24, 52, 70), new Color(46, 120, 150), new Color(28, 84, 116), new Color(190, 240, 230), new Color(120, 220, 230, 45),
                    new Color(98, 144, 132), new Color(152, 192, 176)),
            // Volcan Chocolat : rivière de lave, gués de croûte refroidie
            new Pal(new Color(140, 86, 64, 70), new Color(14, 6, 6, 125), new Color(255, 120, 40, 55),
                    new Color(36, 20, 16), new Color(124, 86, 66),
                    new Color(38, 20, 16), new Color(255, 104, 26), new Color(255, 168, 52), new Color(255, 236, 150), new Color(255, 110, 30, 70),
                    new Color(92, 60, 48), new Color(152, 112, 92)),
    };

    private record Forme(Shape s, Rectangle2D b) {
        static Forme de(Shape s) {
            return new Forme(s, s.getBounds2D());
        }
    }

    private static final class Formes {
        BufferedImage teinte;
        final List<Forme> bordSentiers = new ArrayList<>(), corpsSentiers = new ArrayList<>();
        final List<double[]> cailloux = new ArrayList<>();
        Forme lueur, berge, eau, profond, reflet1, reflet2;
        final List<Forme> mousse = new ArrayList<>(), gues = new ArrayList<>();
        final List<double[]> pierres = new ArrayList<>();
    }

    private static Formes formes(Terrain t, int id) {
        if (t.cache instanceof Formes f) return f;
        Formes f = new Formes();
        Pal p = PAL[id];
        f.teinte = teinte(t, p);

        // sentiers
        Random r = new Random(77 + id);
        for (double[][] s : t.sentiers) {
            Path2D chemin = courbe(s, 0);
            f.bordSentiers.add(Forme.de(new BasicStroke((float) Terrain.LARGEUR_SENTIER + 12, BasicStroke.CAP_ROUND,
                    BasicStroke.JOIN_ROUND).createStrokedShape(chemin)));
            f.corpsSentiers.add(Forme.de(new BasicStroke((float) Terrain.LARGEUR_SENTIER, BasicStroke.CAP_ROUND,
                    BasicStroke.JOIN_ROUND).createStrokedShape(chemin)));
            for (int i = 0; i + 1 < s.length; i++) {
                if (r.nextDouble() > 0.55) continue;
                double a = r.nextDouble() * Math.PI * 2, d = Terrain.LARGEUR_SENTIER * (0.15 + r.nextDouble() * 0.45);
                f.cailloux.add(new double[]{s[i][0] + Math.cos(a) * d, s[i][1] + Math.sin(a) * d, 2 + r.nextDouble() * 3.5});
            }
        }

        // rivière
        double L = t.largeurRiviere;
        Path2D axe = courbe(t.riviere, 0);
        f.lueur = Forme.de(new BasicStroke((float) (L + 64), BasicStroke.CAP_BUTT, BasicStroke.JOIN_ROUND).createStrokedShape(axe));
        f.berge = Forme.de(new BasicStroke((float) (L + 30), BasicStroke.CAP_BUTT, BasicStroke.JOIN_ROUND).createStrokedShape(axe));
        f.eau = Forme.de(new BasicStroke((float) L, BasicStroke.CAP_BUTT, BasicStroke.JOIN_ROUND).createStrokedShape(axe));
        f.profond = Forme.de(new BasicStroke((float) (L * 0.52), BasicStroke.CAP_BUTT, BasicStroke.JOIN_ROUND).createStrokedShape(axe));
        f.reflet1 = Forme.de(new BasicStroke(2.6f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND, 1f,
                new float[]{15, 58}, 0f).createStrokedShape(courbe(t.riviere, L * 0.2)));
        f.reflet2 = Forme.de(new BasicStroke(2.2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND, 1f,
                new float[]{9, 44}, 21f).createStrokedShape(courbe(t.riviere, -L * 0.17)));

        // gués : la portion de rivière qu'on peut traverser à pied
        for (double[] g : t.gues) {
            List<double[]> morceau = new ArrayList<>();
            for (double[] q : t.riviere) {
                if (Math.hypot(q[0] - g[0], q[1] - g[1]) <= Terrain.DEMI_GUE) morceau.add(q);
            }
            if (morceau.size() < 2) continue;
            double[][] m = morceau.toArray(new double[0][]);
            Path2D chemin = courbe(m, 0);
            f.mousse.add(Forme.de(new BasicStroke((float) (L + 22), BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND).createStrokedShape(chemin)));
            f.gues.add(Forme.de(new BasicStroke((float) (L + 4), BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND).createStrokedShape(chemin)));
            Random rg = new Random((long) (g[0] * 31 + g[1]));
            for (int i = 0; i < 26; i++) {
                double[] q = m[rg.nextInt(m.length)];
                double a = rg.nextDouble() * Math.PI * 2, d = rg.nextDouble() * L * 0.42;
                f.pierres.add(new double[]{q[0] + Math.cos(a) * d, q[1] + Math.sin(a) * d, 4 + rg.nextDouble() * 6});
            }
        }
        t.cache = f;
        return f;
    }

    /** Ligne lissée (courbes de Bézier entre les milieux des segments), décalée de d pixels sur le côté. */
    private static Path2D courbe(double[][] pts, double d) {
        int n = pts.length;
        double[][] q = new double[n][2];
        for (int i = 0; i < n; i++) {
            double[] a = pts[Math.max(0, i - 1)], b = pts[Math.min(n - 1, i + 1)];
            double tx = b[0] - a[0], ty = b[1] - a[1], l = Math.hypot(tx, ty);
            if (l < 1e-9) l = 1;
            q[i][0] = pts[i][0] - ty / l * d;
            q[i][1] = pts[i][1] + tx / l * d;
        }
        Path2D.Double c = new Path2D.Double();
        c.moveTo(q[0][0], q[0][1]);
        for (int i = 1; i + 1 < n; i++) {
            c.quadTo(q[i][0], q[i][1], (q[i][0] + q[i + 1][0]) / 2, (q[i][1] + q[i + 1][1]) / 2);
        }
        c.lineTo(q[n - 1][0], q[n - 1][1]);
        return c;
    }

    /** Une case par PX pixels : mélange de la teinte « clairière » et « sous-bois », puis des champs de fleurs. */
    private static BufferedImage teinte(Terrain t, Pal p) {
        int w = (int) Math.ceil(t.largeur / (double) PX), h = (int) Math.ceil(t.hauteur / (double) PX);
        BufferedImage img = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                double wx = (x + 0.5) * PX, wy = (y + 0.5) * PX;
                double d = t.foret(wx, wy), f = t.fleurs(wx, wy);
                d = d * d * (3 - 2 * d);
                f = Math.max(0, f - 0.45) / 0.55;
                // 1) clairière -> sous-bois
                double a1 = lerp(p.clair.getAlpha(), p.dense.getAlpha(), d) / 255.0;
                double r1 = lerp(p.clair.getRed(), p.dense.getRed(), d);
                double g1 = lerp(p.clair.getGreen(), p.dense.getGreen(), d);
                double b1 = lerp(p.clair.getBlue(), p.dense.getBlue(), d);
                // 2) champs de fleurs par-dessus, surtout là où la forêt est claire
                double a2 = p.fleur.getAlpha() / 255.0 * f * f * (1 - 0.6 * d);
                double a = a2 + a1 * (1 - a2);
                double r = a < 1e-6 ? 0 : (p.fleur.getRed() * a2 + r1 * a1 * (1 - a2)) / a;
                double g = a < 1e-6 ? 0 : (p.fleur.getGreen() * a2 + g1 * a1 * (1 - a2)) / a;
                double b = a < 1e-6 ? 0 : (p.fleur.getBlue() * a2 + b1 * a1 * (1 - a2)) / a;
                img.setRGB(x, y, ((int) Math.round(a * 255) << 24) | ((int) Math.round(r) << 16)
                        | ((int) Math.round(g) << 8) | (int) Math.round(b));
            }
        }
        return img;
    }

    private static double lerp(double a, double b, double t) {
        return a + (b - a) * t;
    }

    private static Color alpha(Color c, int a) {
        return new Color(c.getRed(), c.getGreen(), c.getBlue(), Math.clamp(a, 0, 255));
    }

    private static boolean visible(Forme f, double x0, double y0, double x1, double y1) {
        Rectangle2D b = f.b();
        return b.getMaxX() >= x0 && b.getMinX() <= x1 && b.getMaxY() >= y0 && b.getMinY() <= y1;
    }

    /** Dessine le terrain visible. Appelé juste après le pavage de la texture de sol (coordonnées de la carte). */
    static void dessiner(Graphics2D g, Terrain t, int monde, double camX, double camY, double anim) {
        Formes f = formes(t, monde);
        Pal p = PAL[monde];
        double x0 = camX - 60, y0 = camY - 60, x1 = camX + W + 60, y1 = camY + H + 60;
        Object aa = g.getRenderingHint(RenderingHints.KEY_ANTIALIASING);
        Object interp = g.getRenderingHint(RenderingHints.KEY_INTERPOLATION);
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        // teintes lisses sur toute la carte : le sol change doucement d'une zone à l'autre
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        int sx0 = Math.max(0, (int) Math.floor(camX / PX) - 1), sy0 = Math.max(0, (int) Math.floor(camY / PX) - 1);
        int sx1 = Math.min(f.teinte.getWidth(), (int) Math.ceil((camX + W) / PX) + 1);
        int sy1 = Math.min(f.teinte.getHeight(), (int) Math.ceil((camY + H) / PX) + 1);
        if (sx1 > sx0 && sy1 > sy0) g.drawImage(f.teinte, sx0 * PX, sy0 * PX, sx1 * PX, sy1 * PX, sx0, sy0, sx1, sy1, null);
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, interp != null ? interp : RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);

        // sentiers
        for (int i = 0; i < f.bordSentiers.size(); i++) {
            if (!visible(f.bordSentiers.get(i), x0, y0, x1, y1)) continue;
            g.setColor(alpha(p.sentBord, 150));
            g.fill(f.bordSentiers.get(i).s());
            g.setColor(alpha(p.sentCorps, 215));
            g.fill(f.corpsSentiers.get(i).s());
        }
        g.setColor(alpha(p.sentBord, 170));
        for (double[] c : f.cailloux) {
            if (c[0] < x0 || c[0] > x1 || c[1] < y0 || c[1] > y1) continue;
            g.fill(new Ellipse2D.Double(c[0] - c[2], c[1] - c[2] * 0.7, c[2] * 2, c[2] * 1.4));
        }

        // rivière
        if (visible(f.lueur, x0, y0, x1, y1)) {
            g.setColor(p.lueur);
            g.fill(f.lueur.s());
            g.setColor(p.berge);
            g.fill(f.berge.s());
            g.setColor(p.eau);
            g.fill(f.eau.s());
            g.setColor(p.profond);
            g.fill(f.profond.s());
            double pulse = Math.sin(anim * 2.2);
            g.setColor(alpha(p.reflet, (int) (120 + 70 * pulse)));
            g.fill(f.reflet1.s());
            g.setColor(alpha(p.reflet, (int) (110 - 60 * pulse)));
            g.fill(f.reflet2.s());
            // gués
            for (int i = 0; i < f.gues.size(); i++) {
                if (!visible(f.gues.get(i), x0, y0, x1, y1)) continue;
                g.setColor(alpha(p.reflet, 120));
                g.fill(f.mousse.get(i).s());
                g.setColor(p.gue);
                g.fill(f.gues.get(i).s());
            }
            for (double[] s : f.pierres) {
                if (s[0] < x0 || s[0] > x1 || s[1] < y0 || s[1] > y1) continue;
                g.setColor(alpha(Color.BLACK, 45));
                g.fill(new Ellipse2D.Double(s[0] - s[2], s[1] - s[2] * 0.55 + 2, s[2] * 2, s[2] * 1.2));
                g.setColor(p.pierre);
                g.fill(new Ellipse2D.Double(s[0] - s[2], s[1] - s[2] * 0.7, s[2] * 2, s[2] * 1.4));
            }
        }
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, aa != null ? aa : RenderingHints.VALUE_ANTIALIAS_DEFAULT);
    }

    /** Version réduite pour la mini-carte : rivière et sentiers (sc = pixels de mini-carte par pixel de carte). */
    static void mini(Graphics2D g, Terrain t, int monde, double sc) {
        Pal p = PAL[monde];
        for (double[][] s : t.sentiers) {
            Path2D c = new Path2D.Double();
            for (int i = 0; i < s.length; i++) {
                if (i == 0) c.moveTo(s[i][0] * sc, s[i][1] * sc);
                else c.lineTo(s[i][0] * sc, s[i][1] * sc);
            }
            g.setStroke(new BasicStroke((float) Math.max(1.2, Terrain.LARGEUR_SENTIER * sc * 0.7), BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g.setColor(alpha(p.sentCorps, 150));
            g.draw(c);
        }
        Path2D c = new Path2D.Double();
        for (int i = 0; i < t.riviere.length; i++) {
            double x = t.riviere[i][0] * sc, y = t.riviere[i][1] * sc;
            if (i == 0) c.moveTo(x, y);
            else c.lineTo(x, y);
        }
        g.setStroke(new BasicStroke((float) Math.max(2, t.largeurRiviere * sc), BasicStroke.CAP_BUTT, BasicStroke.JOIN_ROUND));
        g.setColor(t.lave ? new Color(255, 110, 40) : new Color(80, 170, 210));
        g.draw(c);
        g.setStroke(new BasicStroke(1f));
    }
}
