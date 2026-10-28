package org.java.ressources.donnees;

import java.awt.Color;

/** Une amélioration proposée à chaque montée de niveau. */
public class Amelio {
    public final String nom, desc;
    public final Color col;
    public final Runnable effet;

    public Amelio(String nom, String desc, Color col, Runnable effet) {
        this.nom = nom;
        this.desc = desc;
        this.col = col;
        this.effet = effet;
    }
}
