package org.java.ressources.entites;

import java.awt.Color;

/** Une sucrerie méchante. */
public class Ennemi {
    public double x, y, pv, pvMax, rayon, vitesse, degats, flash;
    public TypeEnnemi type;
    public int monde;
    public boolean boss;
    public Color col;
    /** Temps restant pendant lequel l'ennemi contourne un obstacle, et sens du contournement (+1 / -1). */
    public double contourne;
    public int sens = 1;
}
