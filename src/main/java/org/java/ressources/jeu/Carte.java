package org.java.ressources.jeu;

import org.java.ressources.donnees.Monde;
import org.java.ressources.entites.Decor;
import org.java.ressources.entites.Obstacle;
import org.java.ressources.entites.PointInteret;

import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * La carte d'une partie : grande zone de jeu avec obstacles solides, zones dangereuses et décors.
 * Elle est générée au hasard à chaque partie, selon le monde.
 */
public class Carte {
    /** Distance minimale entre le centre d'une entité et le bord de la carte. */
    public static final double MARGE = 30;

    public final Monde monde;
    public final int largeur, hauteur;
    public final List<Obstacle> obstacles = new ArrayList<>();
    public final List<Obstacle> solides = new ArrayList<>();
    public final List<Obstacle> dangers = new ArrayList<>();
    public final List<Decor> decors = new ArrayList<>();
    /** Récompenses et événements distants, répartis sur les secteurs logiques de la carte. */
    public final List<PointInteret> pointsInteret = new ArrayList<>();
    /** Image de la mini-carte (créée à la demande par le rendu). */
    public BufferedImage mini;

    private Carte(Monde monde) {
        this.monde = monde;
        this.largeur = monde.largeur;
        this.hauteur = monde.hauteur;
    }

    public static Carte generer(Monde m, Random rnd) {
        Carte c = new Carte(m);
        double cx = c.largeur / 2.0, cy = c.hauteur / 2.0;

        // des « bosquets » : les obstacles ont tendance à se regrouper, ça crée des zones et des couloirs
        int nbBosquets = 7 + m.id * 2;
        double[][] bosquets = new double[nbBosquets][2];
        for (double[] b : bosquets) {
            b[0] = 250 + rnd.nextDouble() * (c.largeur - 500);
            b[1] = 250 + rnd.nextDouble() * (c.hauteur - 500);
        }
        double total = 0;
        for (Monde.TypeObstacle t : m.obstacles) total += t.poids;

        int essais = 0;
        while (c.obstacles.size() < m.nbObstacles && essais++ < m.nbObstacles * 50) {
            // choix du type selon les poids
            double r = rnd.nextDouble() * total;
            int ti = 0;
            for (int i = 0; i < m.obstacles.length; i++) {
                r -= m.obstacles[i].poids;
                if (r < 0) {
                    ti = i;
                    break;
                }
            }
            Monde.TypeObstacle t = m.obstacles[ti];
            double u = t.uMin + rnd.nextDouble() * (t.uMax - t.uMin);
            double rayon = u * t.coll;

            double x, y;
            if (rnd.nextDouble() < 0.65) {
                double[] b = bosquets[rnd.nextInt(nbBosquets)];
                x = b[0] + rnd.nextGaussian() * 320;
                y = b[1] + rnd.nextGaussian() * 260;
            } else {
                x = rnd.nextDouble() * c.largeur;
                y = rnd.nextDouble() * c.hauteur;
            }
            double marge = 90 + u;
            if (x < marge || x > c.largeur - marge || y < marge || y > c.hauteur - marge) continue;
            if (Math.hypot(x - cx, y - cy) < 300 + u) continue; // clairière de départ

            // on laisse toujours un passage d'au moins 64 px entre deux obstacles solides
            boolean ok = true;
            for (Obstacle o : c.obstacles) {
                double ecart = (t.solide && o.solide) ? 64 : 10;
                if (Math.hypot(x - o.x, y - o.y) < rayon + o.r + ecart) {
                    ok = false;
                    break;
                }
            }
            if (!ok) continue;

            Obstacle o = new Obstacle();
            o.monde = m.id;
            o.type = ti;
            o.x = x;
            o.y = y;
            o.echelle = u;
            o.r = rayon;
            o.graine = rnd.nextDouble();
            o.solide = t.solide;
            o.danger = t.danger;
            o.plat = t.plat;
            c.obstacles.add(o);
            if (o.solide) c.solides.add(o);
            if (o.danger) c.dangers.add(o);
        }

        // décors
        double totalDecor = 0;
        for (Monde.TypeDecor d : m.decors) totalDecor += d.poids;
        for (int n = 0; n < m.nbDecors; n++) {
            double r = rnd.nextDouble() * totalDecor;
            int ti = 0;
            for (int i = 0; i < m.decors.length; i++) {
                r -= m.decors[i].poids;
                if (r < 0) {
                    ti = i;
                    break;
                }
            }
            Monde.TypeDecor t = m.decors[ti];
            Decor d = new Decor();
            d.monde = m.id;
            d.type = ti;
            d.x = 20 + rnd.nextDouble() * (c.largeur - 40);
            d.y = 20 + rnd.nextDouble() * (c.hauteur - 40);
            d.taille = t.tMin + rnd.nextDouble() * (t.tMax - t.tMin);
            d.graine = rnd.nextDouble();
            if (c.libre(d.x, d.y, 0)) continue;
            c.decors.add(d);
        }
        c.genererPointsInteret(rnd);
        return c;
    }

    private void genererPointsInteret(Random rnd) {
        int secteurs = Math.max(1, monde.nbSecteurs);
        double cx = largeur / 2.0, cy = hauteur / 2.0;
        for (int n = 0; n < monde.nbPointsInteret; n++) {
            int secteur = n % secteurs;
            PointInteret p = new PointInteret();
            p.secteur = secteur;
            p.type = PointInteret.Type.values()[n % PointInteret.Type.values().length];
            boolean pose = false;
            // Les quatre secteurs sont des quadrants : la boucle garde les points loin du départ et des murs.
            for (int essai = 0; essai < 180 && !pose; essai++) {
                double x = 110 + rnd.nextDouble() * (largeur - 220);
                double y = 110 + rnd.nextDouble() * (hauteur - 220);
                if (secteurs == 4) {
                    x = (secteur % 2 == 0) ? 90 + rnd.nextDouble() * (cx - 220)
                            : cx + 130 + rnd.nextDouble() * (largeur - cx - 220);
                    y = (secteur < 2) ? 90 + rnd.nextDouble() * (cy - 220)
                            : cy + 130 + rnd.nextDouble() * (hauteur - cy - 220);
                }
                if (Math.hypot(x - cx, y - cy) < Math.min(largeur, hauteur) * .22 || libre(x, y, 34)) continue;
                boolean distinct = true;
                for (PointInteret autre : pointsInteret) {
                    if (Math.hypot(x - autre.x, y - autre.y) < 170) { distinct = false; break; }
                }
                if (distinct) { p.x = x; p.y = y; pose = true; }
            }
            // Le générateur d'obstacles ne ferme pas la carte ; ce repli reste jouable et explicite.
            if (!pose) {
                double angle = secteur * Math.PI / 2 + .45;
                p.x = Math.clamp(largeur - 70, 70, cx + Math.cos(angle) * largeur * .31);
                p.y = Math.clamp(hauteur - 70, 70, cy + Math.sin(angle) * hauteur * .31);
                double[] corrige = repousser(p.x, p.y, 34);
                if (corrige != null) { p.x = corrige[0]; p.y = corrige[1]; }
            }
            pointsInteret.add(p);
        }
    }

    /** Secteur logique du point, compatible avec les mondes actuels à quatre quadrants. */
    public int secteur(double x, double y) {
        if (monde.nbSecteurs != 4) return 0;
        return (x >= largeur / 2.0 ? 1 : 0) + (y >= hauteur / 2.0 ? 2 : 0);
    }

    /** Vrai si le cercle (x, y, r) ne touche aucun obstacle solide. */
    public boolean libre(double x, double y, double r) {
        for (Obstacle o : solides) {
            if (Math.hypot(x - o.x, y - o.y) < r + o.r) return true;
        }
        return false;
    }

    /** Vrai si le point est dans une zone qui blesse (lave). */
    public boolean dansDanger(double x, double y) {
        for (Obstacle o : dangers) {
            if (Math.hypot(x - o.x, y - o.y) < o.r) return true;
        }
        return false;
    }

    /**
     * Repousse un cercle hors des obstacles solides et le garde dans la carte.
     * Renvoie la position corrigée {x, y}, ou null si rien n'a bougé.
     */
    public double[] repousser(double x, double y, double r) {
        boolean touche = false;
        for (int passe = 0; passe < 2; passe++) {
            boolean change = false;
            for (Obstacle o : solides) {
                double dx = x - o.x, dy = y - o.y, min = r + o.r;
                if (Math.abs(dx) > min || Math.abs(dy) > min) continue;
                double d2 = dx * dx + dy * dy;
                if (d2 < min * min) {
                    double d = Math.sqrt(d2);
                    if (d < 1e-6) {
                        dx = 1;
                        dy = 0;
                        d = 1;
                    }
                    x = o.x + dx / d * min;
                    y = o.y + dy / d * min;
                    change = true;
                    touche = true;
                }
            }
            if (!change) break;
        }
        double nx = Math.clamp(largeur - MARGE, MARGE, x);
        double ny = Math.clamp(hauteur - MARGE, MARGE, y);
        if (nx != x || ny != y) {
            touche = true;
            x = nx;
            y = ny;
        }
        return touche ? new double[]{x, y} : null;
    }
}
