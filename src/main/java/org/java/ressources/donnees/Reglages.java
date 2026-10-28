package org.java.ressources.donnees;

/** Réglages du joueur (sauvegardés dans le menu Paramètres). */
public class Reglages {
    /** Choix communs à tous les joueurs, y compris en partie LAN. */
    public static final int[] FPS_CHOIX = {30, 45, 60, 120, 144, 240};

    public volatile boolean musique = true;
    public volatile double volMusique = 0.8;   // 0 .. 1
    public volatile double volSfx = 0.9;       // 0 .. 1
    public boolean pleinEcran = true;
    public boolean modeSouris = false;
    public boolean secousses = true;
    public boolean effets = true;
    public boolean pauseAuto = true;
    public boolean miniCarte = true;
    /** Images affichées par seconde (30, 45 ou 60). La logique du jeu reste à 60 pas/s dans tous les cas. */
    public int fps = 60;
}
