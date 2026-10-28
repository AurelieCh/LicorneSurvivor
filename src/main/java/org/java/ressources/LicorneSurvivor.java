package org.java.ressources;

import org.java.ressources.ui.Fenetre;
import org.java.ressources.ui.Jeu;

import javax.swing.SwingUtilities;

/**
 * LICORNE SURVIVOR
 * ---
 * Ta licorne tire toute seule sur les méchantes sucreries : toi, tu esquives !
 * ---
 * Organisation du code :
 *   donnees  - mondes, succès, pouvoirs, réglages, progression, sauvegarde
 *   entites  - ennemis, tirs, obstacles, décors... (données seulement)
 *   audio    - musique et bruitages synthétisés
 *   jeu      - la carte (génération, collisions), la partie (logique), la session
 *   rendu    - tout le dessin : sols, obstacles, décors, ambiance, interface
 *   ui       - la fenêtre, le clavier, la souris, la boucle de jeu
 * ---
 * Déplacement : Z Q S D (ou W A S D, ou les flèches) - ou souris selon le réglage
 * Plein écran : F11     Pause : P     Réglages : O
 */
public class LicorneSurvivor {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            Jeu jeu = new Jeu();
            Fenetre fenetre = new Fenetre(jeu);
            jeu.setFenetre(fenetre);
            fenetre.afficher();
        });
    }
}
