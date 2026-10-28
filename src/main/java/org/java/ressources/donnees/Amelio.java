package org.java.ressources.donnees;

import java.awt.Color;

/** Une amélioration proposée à chaque montée de niveau. */
public class Amelio {
    public final String nom, desc;
    public final Color col;
    public final Runnable effet;
    /** Clé de comptage (pour les conditions d'évolution). Par défaut : le nom. */
    public final String cle;
    /** Vrai pour une évolution d'arme (carte dorée). */
    public final boolean evolution;

    public Amelio(String nom, String desc, Color col, Runnable effet) {
        this(nom, desc, col, effet, nom, false);
    }

    public Amelio(String nom, String desc, Color col, Runnable effet, String cle, boolean evolution) {
        this.nom = nom;
        this.desc = desc;
        this.col = col;
        this.effet = effet;
        this.cle = cle;
        this.evolution = evolution;
    }
}
