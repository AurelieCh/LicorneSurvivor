package org.java.ressources.rendu;

import java.awt.Rectangle;

import static org.java.ressources.donnees.Constantes.W;

/** Les zones cliquables de l'interface (partagées entre le dessin et la souris). */
public final class Zones {
    private Zones() {
    }

    // choix d'amélioration au niveau supérieur
    public static Rectangle carte(int i) {
        return new Rectangle(W / 2 - 125 + (i - 1) * 280, 190, 250, 280);
    }

    // menu pause
    public static Rectangle btnReprendre() {
        return new Rectangle(W / 2 - 140, 215, 280, 54);
    }

    public static Rectangle btnParams() {
        return new Rectangle(W / 2 - 140, 280, 280, 54);
    }

    public static Rectangle btnAbandon() {
        return new Rectangle(W / 2 - 140, 345, 280, 54);
    }

    // paramètres
    public static Rectangle ligneParam(int i) {
        return new Rectangle(W / 2 - 310, 120 + i * 36, 620, 32);
    }

    /** Zone de la barre de volume (lignes 0 et 1). */
    public static Rectangle zoneCurseur(int i) {
        Rectangle r = ligneParam(i);
        return new Rectangle(r.x + 290, r.y, 270, r.height);
    }

    // menu principal
    public static Rectangle flecheG() {
        return new Rectangle(W / 2 - 300, 130, 56, 60);
    }

    public static Rectangle flecheD() {
        return new Rectangle(W / 2 + 244, 130, 56, 60);
    }

    public static Rectangle ligneBoutique(int i) {
        return new Rectangle(W / 2 - 300, 375 + i * 37, 600, 34);
    }

    public static Rectangle boutonJouer() {
        return new Rectangle(W / 2 - 300, 495, 190, 56);
    }

    public static Rectangle boutonSucces() {
        return new Rectangle(W / 2 - 95, 495, 190, 56);
    }

    public static Rectangle boutonParams() {
        return new Rectangle(W / 2 + 110, 495, 190, 56);
    }

    public static Rectangle boutonMultijoueur() {
        return new Rectangle(W / 2 - 115, 558, 230, 38);
    }

    public static Rectangle boutonQuitter() {
        return new Rectangle(W - 170, 78, 140, 36);
    }

    public static Rectangle cartePersonnage() {
        return new Rectangle(180, 230, 600, 92);
    }

    public static Rectangle personnage(int i) {
        return new Rectangle(70 + (i % 2) * 430, 125 + (i / 2) * 145, 390, 125);
    }

    public static Rectangle partieLan(int i) {
        return new Rectangle(130, 235 + i * 52, 700, 42);
    }

    public static Rectangle boutonLanHeberger() {
        return new Rectangle(150, 485, 200, 54);
    }

    public static Rectangle boutonLanRechercher() {
        return new Rectangle(380, 485, 200, 54);
    }

    public static Rectangle boutonLanRetour() {
        return new Rectangle(610, 485, 200, 54);
    }

    public static Rectangle boutonLanLancer() {
        return new Rectangle(W / 2 - 145, 485, 260, 54);
    }
}
