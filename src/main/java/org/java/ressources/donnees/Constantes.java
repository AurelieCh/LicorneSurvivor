package org.java.ressources.donnees;

/** Constantes globales du jeu. */
public final class Constantes {
    private Constantes() {
    }

    /** Taille de la zone visible (repère logique du jeu, mise à l'échelle par la fenêtre). */
    public static final int W = 960, H = 640;
    public static final double DT = 1.0 / 60.0;
    public static final int NIV_MAX = 10;
    public static final int NB_PARAMS = 11;


    /** Chance de base qu'un ennemi lâche un pouvoir à sa mort (modulée selon l'ennemi). */
    public static final double TAUX_DROP = 0.06;
}
