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
    /** Rivière, gués, sentiers et champs de bruit (forêt, fleurs) : la géographie de la carte. */
    public final Terrain terrain;
    public final List<Obstacle> obstacles = new ArrayList<>();
    public final List<Obstacle> solides = new ArrayList<>();
    public final List<Obstacle> dangers = new ArrayList<>();
    public final List<Decor> decors = new ArrayList<>();
    /** Récompenses et événements distants, répartis sur les secteurs logiques de la carte. */
    public final List<PointInteret> pointsInteret = new ArrayList<>();
    /** Image de la mini-carte (créée à la demande par le rendu). */
    public BufferedImage mini;

    private Carte(Monde monde, Random rnd) {
        this.monde = monde;
        this.largeur = monde.largeur;
        this.hauteur = monde.hauteur;
        this.terrain = Terrain.generer(monde, rnd);
    }

    public static Carte generer(Monde m, Random rnd) {
        Carte c = new Carte(m, rnd);
        Terrain t = c.terrain;
        double cx = c.largeur / 2.0, cy = c.hauteur / 2.0;

        c.poserRiviere();
        // Les découvertes sont placées avant les obstacles : ce sont les obstacles qui s'écartent d'elles.
        c.genererPointsInteret(rnd);
        List<double[]> cibles = new ArrayList<>();
        for (PointInteret p : c.pointsInteret) cibles.add(new double[]{p.x, p.y});
        t.tracerSentiers(cx, cy, cibles, rnd);

        double total = 0;
        for (Monde.TypeObstacle to : m.obstacles) total += to.poids;

        int essais = 0;
        while (c.obstacles.size() < m.nbObstacles + c.nbMaillonsRiviere && essais++ < m.nbObstacles * 60) {
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
            Monde.TypeObstacle to = m.obstacles[ti];
            double u = to.uMin + rnd.nextDouble() * (to.uMax - to.uMin);
            double rayon = u * to.coll;

            double x = rnd.nextDouble() * c.largeur, y = rnd.nextDouble() * c.hauteur;
            double marge = 90 + u;
            if (x < marge || x > c.largeur - marge || y < marge || y > c.hauteur - marge) continue;
            if (Math.hypot(x - cx, y - cy) < 300 + u) continue; // clairière de départ

            // Forêt : les obstacles hauts se concentrent dans les zones denses, les étangs / mares dans les clairières.
            // Ce champ est continu, donc les bois ont des contours naturels au lieu de se découper en secteurs.
            double dens = t.foret(x, y);
            double accepte = to.plat ? 0.25 + 0.75 * (1 - dens) : 0.05 + 0.95 * Math.pow(dens, 1.5);
            if (rnd.nextDouble() > accepte) continue;

            // jamais sur la rivière, ses gués, un sentier ou une découverte
            if (t.distRiviere(x, y) < rayon + t.demiLargeur() + 24) continue;
            if (t.distGue(x, y) < Terrain.DEMI_GUE + 70 + rayon) continue;
            if (t.distSentier(x, y) < rayon + Terrain.LARGEUR_SENTIER / 2 + 14) continue;
            boolean pres = false;
            for (PointInteret p : c.pointsInteret) {
                if (Math.hypot(x - p.x, y - p.y) < rayon + 110) {
                    pres = true;
                    break;
                }
            }
            if (pres) continue;

            // on laisse toujours un passage d'au moins 64 px entre deux obstacles solides
            boolean ok = true;
            for (Obstacle o : c.obstacles) {
                double ecart = (to.solide && o.solide) ? 64 : 10;
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
            o.solide = to.solide;
            o.danger = to.danger;
            o.plat = to.plat;
            c.obstacles.add(o);
            if (o.solide) c.solides.add(o);
            if (o.danger) c.dangers.add(o);
        }

        // décors : ils se regroupent en champs (fleurs, herbes...) et laissent libres l'eau et les sentiers
        double totalDecor = 0;
        for (Monde.TypeDecor d : m.decors) totalDecor += d.poids;
        int poses = 0;
        for (int n = 0; poses < m.nbDecors && n < m.nbDecors * 6; n++) {
            double r = rnd.nextDouble() * totalDecor;
            int ti = 0;
            for (int i = 0; i < m.decors.length; i++) {
                r -= m.decors[i].poids;
                if (r < 0) {
                    ti = i;
                    break;
                }
            }
            Monde.TypeDecor td = m.decors[ti];
            double x = 20 + rnd.nextDouble() * (c.largeur - 40);
            double y = 20 + rnd.nextDouble() * (c.hauteur - 40);
            if (rnd.nextDouble() > 0.12 + 0.88 * t.fleurs(x, y)) continue;
            if (t.distRiviere(x, y) < t.demiLargeur() + 6) continue;
            if (t.distSentier(x, y) < Terrain.LARGEUR_SENTIER / 2) continue;
            Decor d = new Decor();
            d.monde = m.id;
            d.type = ti;
            d.x = x;
            d.y = y;
            d.taille = td.tMin + rnd.nextDouble() * (td.tMax - td.tMin);
            d.graine = rnd.nextDouble();
            if (c.libre(d.x, d.y, 0)) continue;
            c.decors.add(d);
            poses++;
        }

        // filet de sécurité : une découverte de repli ne doit jamais être coincée dans un obstacle
        for (PointInteret p : c.pointsInteret) {
            if (!c.libre(p.x, p.y, 34)) continue;
            double[] corrige = c.repousser(p.x, p.y, 34);
            if (corrige != null) {
                p.x = corrige[0];
                p.y = corrige[1];
            }
        }
        return c;
    }

    private int nbMaillonsRiviere;

    /**
     * Pose la rivière : une chaîne de cercles invisibles qui bloquent le passage (ou brûlent, dans le volcan).
     * Le dessin se fait ailleurs, d'un seul trait. Aux gués, la chaîne s'interrompt : on traverse à pied.
     */
    private void poserRiviere() {
        double rayon = terrain.demiLargeur() * 0.92;
        for (double[] p : terrain.riviere) {
            if (p[0] < 0 || p[0] > largeur || p[1] < 0 || p[1] > hauteur) continue;
            if (terrain.distGue(p[0], p[1]) < Terrain.DEMI_GUE) continue;
            Obstacle o = new Obstacle();
            o.monde = monde.id;
            o.x = p[0];
            o.y = p[1];
            o.echelle = 1;
            o.r = rayon;
            o.riviere = true;
            o.plat = true;
            o.solide = !terrain.lave;
            o.danger = terrain.lave;
            obstacles.add(o);
            if (o.solide) solides.add(o);
            if (o.danger) dangers.add(o);
            nbMaillonsRiviere++;
        }
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
                if (terrain.distRiviere(x, y) < terrain.demiLargeur() + 110) continue;
                boolean distinct = true;
                for (PointInteret autre : pointsInteret) {
                    if (Math.hypot(x - autre.x, y - autre.y) < 170) { distinct = false; break; }
                }
                if (distinct) { p.x = x; p.y = y; pose = true; }
            }
            // Repli jouable et explicite si aucune place n'a été trouvée.
            if (!pose) {
                double angle = secteur * Math.PI / 2 + .45;
                p.x = Math.clamp(cx + Math.cos(angle) * largeur * .31, 70, largeur - 70);
                p.y = Math.clamp(cy + Math.sin(angle) * hauteur * .31, 70, hauteur - 70);
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
