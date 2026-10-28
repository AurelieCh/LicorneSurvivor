package org.java.ressources.entites;

import java.util.ArrayList;
import java.util.List;

/** Un projectile arc-en-ciel. */
public class Tir {
    public double x, y, vx, vy, degats, vie;
    public double rayonExplosion;
    public int perfo;
    public TypeArme arme;
    public final List<Ennemi> touches = new ArrayList<>();
}
