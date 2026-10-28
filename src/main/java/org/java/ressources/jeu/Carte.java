package org.java.ressources.jeu;

import org.java.ressources.donnees.Monde;
import org.java.ressources.entites.Decor;
import org.java.ressources.entites.Obstacle;

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
        return c;
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
        double nx = Math.clamp(x, MARGE, largeur - MARGE);
        double ny = Math.clamp(y, MARGE, hauteur - MARGE);
        if (nx != x || ny != y) {
            touche = true;
            x = nx;
            y = ny;
        }
        return touche ? new double[]{x, y} : null;
    }
}
