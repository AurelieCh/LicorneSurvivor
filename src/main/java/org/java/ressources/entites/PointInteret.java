package org.java.ressources.entites;

/** Une découverte placée par la génération de carte. Son état appartient à une partie, jamais à la sauvegarde. */
public class PointInteret {
    public enum Type { COFFRE, RELIQUE, ELITE, SECRET }

    public double x, y;
    public int secteur;
    public Type type;
    public boolean revele, trouve;
}
