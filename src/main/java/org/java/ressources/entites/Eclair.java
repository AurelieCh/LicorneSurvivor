package org.java.ressources.entites;

/** Un arc électrique à afficher un instant (suite de points reliés). */
public class Eclair {
    public final double[] x, y;
    /** Nombre de points réellement utilisés. */
    public int n;
    public double vie = 0.28;

    public Eclair(int capacite) {
        x = new double[capacite];
        y = new double[capacite];
    }
}
