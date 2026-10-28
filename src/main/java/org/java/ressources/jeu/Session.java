package org.java.ressources.jeu;

import org.java.ressources.audio.Audio;
import org.java.ressources.donnees.Progression;
import org.java.ressources.donnees.Reglages;
import org.java.ressources.donnees.Succes;
import org.java.ressources.reseau.ReseauLan;

import java.util.ArrayList;
import java.util.List;

/**
 * Tout ce qui est partagé entre l'interface (entrées, fenêtre) et le rendu :
 * réglages, progression, audio, partie en cours et état des menus.
 */
public class Session {
    public final Reglages reg = new Reglages();
    public final Progression prog = new Progression();
    public final Audio audio = new Audio(reg);
    public final Partie partie = new Partie(prog, reg, audio);
    public final ReseauLan reseau = new ReseauLan("Joueur");

    public Etat etat = Etat.MENU;
    /** Écran d'où l'on vient quand on ouvre les paramètres (MENU ou PAUSE). */
    public Etat etatRetour = Etat.MENU;
    public double anim = 0;

    public int mondeSel = 0;
    public int paramSel = 0;
    public boolean confirmReset = false;

    /** Position de la souris dans le repère du jeu (960 x 640). */
    public double sourisX = 480, sourisY = 320;

    /** Fenêtres « succès débloqué » en attente d'affichage. */
    public final List<Succes> popups = new ArrayList<>();
    public double popupTimer = 0;
    public java.util.List<ReseauLan.PartieTrouvee> partiesLan = java.util.List.of();
    public String reseauMessage = "";

    /** Vrai si on affiche le monde de jeu (partie, pause, fin, paramètres ouverts depuis la pause). */
    public boolean surLeJeu() {
        return etat == Etat.JEU || etat == Etat.NIVEAU || etat == Etat.PAUSE || etat == Etat.FIN
                || (etat == Etat.PARAMS && etatRetour != Etat.MENU);
    }

    public void fermerReseau() {
        reseau.fermer();
        partiesLan = java.util.List.of();
        reseauMessage = "";
    }
}
