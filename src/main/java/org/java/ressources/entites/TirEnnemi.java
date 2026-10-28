package org.java.ressources.entites;

/** Un projectile lancé par un ennemi (tireur, boss). Il blesse le joueur, pas les autres ennemis. */
public class TirEnnemi {
    public static final int BONBON = 0, FLAMME = 1;

    public double x, y, vx, vy, vie, rayon = 8, degats;
    public int genre = BONBON;
}
