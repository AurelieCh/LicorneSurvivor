package org.java.ressources.entites;

/** Un élément de décor posé au sol, sans collision. */
public class Decor {
    public int monde, type;
    public double x, y, taille, graine;
    /** Image pré-dessinée par le rendu (cache). Jamais lue par la logique du jeu. */
    public Object cache;
}