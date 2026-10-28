package org.java.ressources.entites;

/**
 * Une zone au sol qui explose après un délai (le « télégraphe » est visible pendant l'attente).
 * ennemie = true : blesse le joueur (pilonnage du boss, onde de choc).
 * ennemie = false : blesse les ennemis (météores de l'arme secondaire).
 */
public class ZoneDanger {
    public double x, y, rayon, delai, delaiMax, degats;
    public boolean ennemie;
    public boolean explosee;
    /** Temps d'affichage de l'impact après l'explosion. */
    public double apres;
    public boolean evolue;
}
