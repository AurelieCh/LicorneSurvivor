package org.java.ressources.jeu;

import org.java.ressources.donnees.Monde;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * La « géographie » d'une carte : une rivière sinueuse avec des gués, des sentiers qui mènent
 * aux découvertes, et deux champs de bruit lisses (densité de forêt, champs de fleurs).
 * <p>
 * Tout est décrit par des courbes et des champs continus, pas par des secteurs : la carte ne se découpe
 * plus en carrés mais en clairières, sous-bois, berges et chemins.
 */
public final class Terrain {
    /** Distance entre deux points de la rivière ou d'un sentier. */
    public static final double PAS = 36;
    /** Demi-longueur d'un gué : on peut traverser la rivière à pied sur cette portion. */
    public static final double DEMI_GUE = 95;
    /** Largeur d'un sentier. */
    public static final double LARGEUR_SENTIER = 46;
    /** La rivière dépasse de la carte, sous les bordures. */
    private static final double DEBORD = 90;

    public final int largeur, hauteur;
    /** Rivière horizontale (de gauche à droite) ou verticale. */
    public final boolean horizontale;
    /** Dans le volcan, la « rivière » est de la lave : on peut la traverser, mais ça brûle. */
    public final boolean lave;
    public final double largeurRiviere;
    /** Axe de la rivière : {x, y} tous les PAS pixels. */
    public final double[][] riviere;
    /** Centres des gués : {x, y}. */
    public final double[][] gues;
    /** Chaque sentier est une suite de points {x, y}. */
    public final List<double[][]> sentiers = new ArrayList<>();
    /** Formes pré-calculées par le rendu (cache). Jamais lue par la logique du jeu. */
    public Object cache;

    private final Bruit bruitForet, bruitFleurs;

    private Terrain(Monde m, Random rnd) {
        this.largeur = m.largeur;
        this.hauteur = m.hauteur;
        this.lave = m.id == 2;
        this.largeurRiviere = m.id == 1 ? 100 : m.id == 2 ? 84 : 92;
        this.bruitForet = new Bruit(rnd, largeur, hauteur, 520, 230);
        this.bruitFleurs = new Bruit(rnd, largeur, hauteur, 380, 170);

        // --- la rivière : un axe qui serpente, loin du centre (là où commence la partie) ---
        horizontale = rnd.nextBoolean();
        double longueur = horizontale ? largeur : hauteur;
        double transversal = horizontale ? hauteur : largeur;
        double base = transversal * (0.15 + rnd.nextDouble() * 0.07);
        if (rnd.nextBoolean()) base = transversal - base;
        double a1 = 55 + rnd.nextDouble() * 25, p1 = rnd.nextDouble() * Math.PI * 2, f1 = 1.3 + rnd.nextDouble();
        double a2 = 15 + rnd.nextDouble() * 12, p2 = rnd.nextDouble() * Math.PI * 2, f2 = 3.5 + rnd.nextDouble() * 1.5;
        int n = (int) Math.ceil((longueur + 2 * DEBORD) / PAS) + 1;
        riviere = new double[n][2];
        for (int i = 0; i < n; i++) {
            double s = -DEBORD + i * PAS, t = s / longueur;
            double off = a1 * Math.sin(Math.PI * 2 * f1 * t + p1) + a2 * Math.sin(Math.PI * 2 * f2 * t + p2);
            riviere[i][0] = horizontale ? s : base + off;
            riviere[i][1] = horizontale ? base + off : s;
        }

        // --- deux gués, l'un dans la première moitié de la rivière, l'autre dans la seconde ---
        gues = new double[2][];
        for (int k = 0; k < 2; k++) {
            double frac = (k == 0 ? 0.20 : 0.62) + rnd.nextDouble() * 0.16;
            int i = (int) Math.round((frac * longueur + DEBORD) / PAS);
            gues[k] = riviere[Math.clamp(i, 1, n - 2)].clone();
        }
    }

    public static Terrain generer(Monde m, Random rnd) {
        return new Terrain(m, rnd);
    }

    // ------------------------------------------------------------------ champs continus

    /** Densité de forêt en (x, y), de 0 (clairière) à 1 (sous-bois dense). */
    public double foret(double x, double y) {
        return champ(bruitForet, x, y);
    }

    /** Intensité des fleurs / herbes hautes en (x, y), de 0 à 1. */
    public double fleurs(double x, double y) {
        return champ(bruitFleurs, x, y);
    }

    private static double champ(Bruit b, double x, double y) {
        return Math.clamp((b.val(x, y) - 0.5) * 1.9 + 0.5, 0.0, 1.0);
    }

    // ------------------------------------------------------------------ distances

    public double demiLargeur() {
        return largeurRiviere / 2;
    }

    /** Distance au bord de l'axe de la rivière (0 si on est dessus). */
    public double distRiviere(double x, double y) {
        return distLigne(riviere, x, y);
    }

    public double distGue(double x, double y) {
        double best = Double.MAX_VALUE;
        for (double[] g : gues) best = Math.min(best, Math.hypot(x - g[0], y - g[1]));
        return best;
    }

    public double distSentier(double x, double y) {
        double best = Double.MAX_VALUE;
        for (double[][] s : sentiers) best = Math.min(best, distLigne(s, x, y));
        return best;
    }

    private static double distLigne(double[][] pts, double x, double y) {
        double best = Double.MAX_VALUE;
        for (int i = 0; i + 1 < pts.length; i++) {
            double ax = pts[i][0], ay = pts[i][1], bx = pts[i + 1][0], by = pts[i + 1][1];
            double dx = bx - ax, dy = by - ay, l2 = dx * dx + dy * dy;
            double t = l2 < 1e-9 ? 0 : Math.clamp(((x - ax) * dx + (y - ay) * dy) / l2, 0.0, 1.0);
            best = Math.min(best, Math.hypot(x - (ax + dx * t), y - (ay + dy * t)));
        }
        return best;
    }

    // ------------------------------------------------------------------ sentiers

    /** Côté de la rivière où se trouve le point : -1 ou +1. */
    private int cote(double x, double y) {
        double s = horizontale ? x : y, v = horizontale ? y : x;
        double f = Math.clamp((s + DEBORD) / PAS, 0.0, riviere.length - 1.001);
        int i = (int) f;
        int k = horizontale ? 1 : 0;
        double axe = riviere[i][k] + (riviere[i + 1][k] - riviere[i][k]) * (f - i);
        return v < axe ? -1 : 1;
    }

    /**
     * Trace un sentier du départ vers chaque cible. Si la cible est de l'autre côté de la rivière,
     * le sentier passe par le gué le plus proche.
     */
    public void tracerSentiers(double dx, double dy, List<double[]> cibles, Random rnd) {
        for (double[] b : cibles) {
            List<double[]> etapes = new ArrayList<>();
            etapes.add(new double[]{dx, dy});
            if (cote(dx, dy) != cote(b[0], b[1])) {
                double mx = (dx + b[0]) / 2, my = (dy + b[1]) / 2;
                double[] gue = gues[0];
                if (Math.hypot(mx - gues[1][0], my - gues[1][1]) < Math.hypot(mx - gue[0], my - gue[1])) gue = gues[1];
                etapes.add(gue);
            }
            etapes.add(b);
            // un sentier ne doit pas passer dans l'eau (hors gué) : on réessaie avec un autre tracé
            for (int essai = 0; essai < 24; essai++) {
                double[][] s = courbe(etapes, rnd, essai < 8 ? 1.0 : essai < 16 ? 0.5 : 0.15);
                if (!traverseEau(s)) {
                    sentiers.add(s);
                    break;
                }
            }
        }
    }

    /** Vrai si un point du sentier tombe sur un maillon de rivière qui bloque (donc hors gué). */
    private boolean traverseEau(double[][] s) {
        // marge : le joueur (rayon 16) doit pouvoir suivre le sentier sans frotter contre la berge
        double r = demiLargeur() * 0.92 + 30;
        for (double[] p : s) {
            if (distRiviere(p[0], p[1]) > r) continue;
            for (double[] q : riviere) {
                if (distGue(q[0], q[1]) < DEMI_GUE) continue; // pas de maillon dans un gué
                if (Math.hypot(p[0] - q[0], p[1] - q[1]) < r) return true;
            }
        }
        return false;
    }

    private static double[][] courbe(List<double[]> etapes, Random rnd, double amplitude) {
        List<double[]> pts = new ArrayList<>();
        for (int e = 0; e + 1 < etapes.size(); e++) {
            double[] a = etapes.get(e), b = etapes.get(e + 1);
            double l = Math.hypot(b[0] - a[0], b[1] - a[1]);
            if (l < 1) continue;
            double nx = -(b[1] - a[1]) / l, ny = (b[0] - a[0]) / l;
            double amp = Math.min(120, l * 0.2) * amplitude * (rnd.nextBoolean() ? 1 : -1);
            double freq = 1.5 + rnd.nextDouble() * 1.5, phase = rnd.nextDouble() * Math.PI * 2;
            int steps = Math.max(2, (int) Math.ceil(l / 30));
            for (int i = (e == 0 ? 0 : 1); i <= steps; i++) {
                double t = i / (double) steps;
                double off = amp * Math.sin(Math.PI * t) * Math.sin(Math.PI * freq * t + phase);
                pts.add(new double[]{a[0] + (b[0] - a[0]) * t + nx * off, a[1] + (b[1] - a[1]) * t + ny * off});
            }
        }
        return pts.toArray(new double[0][]);
    }

    // ------------------------------------------------------------------ bruit lisse

    /** Bruit de valeur : une grille de nombres au hasard, lissée entre les nœuds. Deux échelles superposées. */
    private static final class Bruit {
        private final Grille grand, petit;

        Bruit(Random r, double w, double h, double cellGrand, double cellPetit) {
            grand = new Grille(r, w, h, cellGrand);
            petit = new Grille(r, w, h, cellPetit);
        }

        double val(double x, double y) {
            return 0.65 * grand.val(x, y) + 0.35 * petit.val(x, y);
        }

        private static final class Grille {
            final double cell;
            final int gw, gh;
            final double[] v;

            Grille(Random r, double w, double h, double cell) {
                this.cell = cell;
                gw = (int) Math.ceil(w / cell) + 2;
                gh = (int) Math.ceil(h / cell) + 2;
                v = new double[gw * gh];
                for (int i = 0; i < v.length; i++) v[i] = r.nextDouble();
            }

            double val(double x, double y) {
                double fx = Math.max(0, x / cell), fy = Math.max(0, y / cell);
                int ix = Math.min((int) fx, gw - 2), iy = Math.min((int) fy, gh - 2);
                double tx = lisse(fx - ix), ty = lisse(fy - iy);
                double a = v[iy * gw + ix], b = v[iy * gw + ix + 1];
                double c = v[(iy + 1) * gw + ix], d = v[(iy + 1) * gw + ix + 1];
                return (a + (b - a) * tx) * (1 - ty) + (c + (d - c) * tx) * ty;
            }

            private static double lisse(double t) {
                t = Math.clamp(t, 0.0, 1.0);
                return t * t * (3 - 2 * t);
            }
        }
    }
}
