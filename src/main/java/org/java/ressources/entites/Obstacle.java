package org.java.ressources.entites;

/**
 * Un obstacle de la carte. (x, y) est le point de contact avec le sol ; r est le rayon de collision ;
 * echelle (« u ») sert au dessin.
 */
public class Obstacle {
    public int monde, type;
    public double x, y, r, echelle, graine;
    public boolean solide, danger, plat;
    /** Image pré-dessinée par le rendu (cache). Jamais lue par la logique du jeu. */
    public Object cache;
}