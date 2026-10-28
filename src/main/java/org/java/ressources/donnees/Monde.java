package org.java.ressources.donnees;

import java.awt.Color;

/**
 * Description d'un monde : difficulté, boss, taille de la carte, types d'obstacles
 * et de décors, couleurs. Les trois mondes sont définis tout en bas.
 */
public class Monde {

    /** Un type d'obstacle que le générateur de carte peut poser dans ce monde. */
    public static class TypeObstacle {
        /** u = unité visuelle ; le rayon de collision vaut u * coll. */
        public final double uMin, uMax, coll, poids;
        /** solide : bloque joueur et ennemis. danger : blesse le personnage. plat : dessiné au sol. */
        public final boolean solide, danger, plat;

        TypeObstacle(double uMin, double uMax, double coll, double poids,
                     boolean solide, boolean danger, boolean plat) {
            this.uMin = uMin;
            this.uMax = uMax;
            this.coll = coll;
            this.poids = poids;
            this.solide = solide;
            this.danger = danger;
            this.plat = plat;
        }

        /** Obstacle haut et solide (arbre, rocher...). */
        public static TypeObstacle haut(double uMin, double uMax, double coll, double poids) {
            return new TypeObstacle(uMin, uMax, coll, poids, true, false, false);
        }

        /** Obstacle plat mais infranchissable (étang...). */
        public static TypeObstacle eau(double uMin, double uMax, double coll, double poids) {
            return new TypeObstacle(uMin, uMax, coll, poids, true, false, true);
        }

        /** Zone plate qui brûle la licorne (lave). On peut la traverser, mais ça fait mal. */
        public static TypeObstacle lave(double uMin, double uMax, double coll, double poids) {
            return new TypeObstacle(uMin, uMax, coll, poids, false, true, true);
        }
    }

    /** Un type de décor posé au sol (sans collision). */
    public static class TypeDecor {
        public final double poids, tMin, tMax;

        public TypeDecor(double poids, double tMin, double tMax) {
            this.poids = poids;
            this.tMin = tMin;
            this.tMax = tMax;
        }
    }

    public final int id;
    public final String nom, boss;
    // fond des menus
    public Color haut, bas, deco;
    // boss
    public Color bossCol;
    public double bossPv;
    public int dureeBoss;
    /** Durée de référence d'un clear exploré, en secondes. */
    public int dureeCible;
    /** Nombre de secteurs et de découvertes à viser avant le boss. */
    public int nbSecteurs, objectifsRequis, nbPointsInteret;
    /** Petit texte utilisé par le HUD et l'écran de fin. */
    public String objectifExploration;
    public Color[] palette;
    public double diff;
    // carte
    public int largeur, hauteur, nbObstacles, nbDecors;
    public Color sol, bordure;
    public TypeObstacle[] obstacles;
    public TypeDecor[] decors;

    Monde(int id, String nom, String boss) {
        this.id = id;
        this.nom = nom;
        this.boss = boss;
    }

    Monde menu(Color haut, Color bas, Color deco) {
        this.haut = haut;
        this.bas = bas;
        this.deco = deco;
        return this;
    }

    Monde combat(Color bossCol, double bossPv, int dureeBoss, double diff, Color... palette) {
        this.bossCol = bossCol;
        this.bossPv = bossPv;
        this.dureeBoss = dureeBoss;
        this.diff = diff;
        this.palette = palette;
        return this;
    }

    Monde carte(int largeur, int hauteur, int nbObstacles, int nbDecors, Color sol, Color bordure) {
        this.largeur = largeur;
        this.hauteur = hauteur;
        this.nbObstacles = nbObstacles;
        this.nbDecors = nbDecors;
        this.sol = sol;
        this.bordure = bordure;
        return this;
    }

    Monde exploration(int dureeCible, int nbSecteurs, int objectifsRequis, int nbPointsInteret,
                      String objectifExploration) {
        this.dureeCible = dureeCible;
        this.nbSecteurs = nbSecteurs;
        this.objectifsRequis = objectifsRequis;
        this.nbPointsInteret = nbPointsInteret;
        this.objectifExploration = objectifExploration;
        return this;
    }

    Monde obstacles(TypeObstacle... t) {
        this.obstacles = t;
        return this;
    }

    Monde decors(TypeDecor... t) {
        this.decors = t;
        return this;
    }

    public static final Monde[] TOUS = {
            // 0 : un monde de bonbons pastel, lumineux et joyeux
            new Monde(0, "Prairie Cupcake", "La Reine Meringue")
                    .menu(new Color(255, 223, 239), new Color(214, 220, 255), new Color(255, 255, 255, 180))
                    .combat(new Color(255, 190, 215), 1500, 120, 1.0,
                            new Color(255, 160, 190), new Color(160, 235, 200), new Color(190, 170, 240),
                            new Color(255, 200, 150), new Color(150, 200, 255))
                    .carte(2400, 1600, 85, 700, new Color(255, 226, 238), new Color(214, 150, 200))
                    .exploration(300, 4, 3, 6, "Explore les clairières et retrouve les trésors sucrés")
                    .obstacles(TypeObstacle.haut(38, 52, 0.80, 3),    // cupcake géant
                            TypeObstacle.haut(34, 46, 0.34, 3),       // sucette-arbre
                            TypeObstacle.haut(36, 54, 0.90, 2),       // cristaux de sucre candi
                            TypeObstacle.haut(38, 52, 0.30, 4))       // arbre à barbe à papa
                    .decors(new TypeDecor(4, 8, 14), new TypeDecor(5, 8, 14), new TypeDecor(4, 6, 10),
                            new TypeDecor(1.5, 30, 60), new TypeDecor(1.5, 8, 12)),

            // 1 : forêt nocturne et féerique, lucioles et brume
            new Monde(1, "Forêt de Guimauve", "Le Roi Guimauve")
                    .menu(new Color(214, 246, 232), new Color(190, 214, 255), new Color(255, 255, 255, 200))
                    .combat(new Color(248, 244, 255), 2600, 135, 1.6,
                            new Color(160, 235, 200), new Color(150, 200, 255), new Color(220, 205, 255),
                            new Color(255, 240, 160))
                    .carte(3000, 2000, 130, 900, new Color(52, 94, 108), new Color(12, 26, 46))
                    .exploration(390, 4, 3, 7, "Éclaire la forêt et brise les cachettes de guimauve")
                    .obstacles(TypeObstacle.haut(44, 62, 0.32, 4),    // arbre guimauve
                            TypeObstacle.haut(34, 50, 0.45, 3),       // champignon géant
                            TypeObstacle.eau(70, 110, 0.95, 1.2),     // étang
                            TypeObstacle.haut(30, 44, 0.90, 2))       // pierre de lune
                    .decors(new TypeDecor(4, 8, 12), new TypeDecor(5, 10, 16), new TypeDecor(3, 30, 60),
                            new TypeDecor(3, 8, 12), new TypeDecor(1.5, 6, 10)),

            // 2 : volcan de chocolat, lave, braises et fumée
            new Monde(2, "Volcan Chocolat", "Le Dragon Caramel")
                    .menu(new Color(255, 226, 200), new Color(222, 176, 150), new Color(255, 235, 170, 200))
                    .combat(new Color(236, 168, 84), 4200, 150, 2.2,
                            new Color(230, 170, 100), new Color(170, 110, 80), new Color(255, 130, 130),
                            new Color(255, 180, 110))
                    .carte(3600, 2400, 170, 1100, new Color(72, 42, 34), new Color(26, 12, 10))
                    .exploration(480, 4, 3, 8, "Traverse le volcan et sécurise les reliques de caramel")
                    .obstacles(TypeObstacle.haut(40, 60, 0.50, 4),    // pic de chocolat
                            TypeObstacle.haut(36, 54, 0.90, 3),       // rocher fondu
                            TypeObstacle.lave(60, 100, 0.85, 2.5),    // mare de lave
                            TypeObstacle.haut(46, 64, 0.85, 1.5))     // cheminée volcanique
                    .decors(new TypeDecor(4, 30, 60), new TypeDecor(4, 6, 10), new TypeDecor(3, 20, 40),
                            new TypeDecor(3, 8, 14), new TypeDecor(3, 3, 5)),
    };
}
