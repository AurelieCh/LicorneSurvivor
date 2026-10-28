package org.java.ressources.donnees;

import java.awt.Color;

/**
 * Un pouvoir qui peut tomber d'un ennemi.
 * poids = chance relative d'être tiré quand un pouvoir tombe ; duree = durée de l'effet (0 = instantané).
 */
public class Pouvoir {
    public static final int COEUR = 0, AIMANT = 1, BOUCLIER = 2, TURBO = 3, NOVA = 4, DOUBLE = 5;

    public final String nom, desc;
    public final Color col;
    public final double poids, duree;

    public Pouvoir(String nom, String desc, Color col, double poids, double duree) {
        this.nom = nom;
        this.desc = desc;
        this.col = col;
        this.poids = poids;
        this.duree = duree;
    }

    /** L'ordre doit correspondre aux constantes ci-dessus. */
    public static final Pouvoir[] TOUS = {
            new Pouvoir("Coeur guimauve", "+30 PV", new Color(255, 130, 170), 30, 0),
            new Pouvoir("Aimant géant", "attire tous les bonbons", new Color(150, 200, 255), 25, 0),
            new Pouvoir("Bouclier arc-en-ciel", "invincible 6 s", new Color(255, 215, 90), 15, 6),
            new Pouvoir("Éclair turbo", "tir et vitesse boostés (8 s)", new Color(190, 150, 255), 20, 8),
            new Pouvoir("Nova sucrée", "dégâts à tous les ennemis", new Color(255, 150, 80), 15, 0),
            new Pouvoir("Double bonbons", "XP x2 pendant 10 s", new Color(120, 220, 170), 20, 10),
    };

    public static double poidsTotal() {
        double t = 0;
        for (Pouvoir p : TOUS) t += p.poids;
        return t;
    }
}
