package org.java.ressources.jeu;

import org.java.ressources.donnees.Monde;
import org.java.ressources.donnees.Progression;
import org.java.ressources.donnees.Reglages;
import org.java.ressources.donnees.Sauvegarde;
import org.java.ressources.entites.PointInteret;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Random;
import java.util.Set;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class ExplorationTest {
    @Test
    void lesPointsInteretCouvrentLesSecteursEtEvitentLesObstacles() {
        for (Monde monde : Monde.TOUS) {
            Carte carte = Carte.generer(monde, new Random(1234 + monde.id));
            assertEquals(monde.nbPointsInteret, carte.pointsInteret.size());
            Set<Integer> secteurs = new HashSet<>();
            for (PointInteret point : carte.pointsInteret) {
                assertTrue(point.x >= Carte.MARGE && point.x <= carte.largeur - Carte.MARGE);
                assertTrue(point.y >= Carte.MARGE && point.y <= carte.hauteur - Carte.MARGE);
                assertFalse(carte.libre(point.x, point.y, 34), "point d'intérêt dans un obstacle");
                secteurs.add(point.secteur);
            }
            assertEquals(monde.nbSecteurs, secteurs.size());
        }
    }

    @Test
    void laProgressionConserveDesValeursParDefautPourLesAnciennesSauvegardes() {
        Progression progression = new Progression();
        assertEquals(0, progression.secretsTrouves);
        for (boolean secretTrouve : progression.secretMonde) assertFalse(secretTrouve);
        progression.secretMonde[0] = true;
        progression.secretsTrouves = 1;
        progression.reinitialiser();
        assertEquals(0, progression.secretsTrouves);
        for (boolean secretTrouve : progression.secretMonde) assertFalse(secretTrouve);
    }

    @Test
    void uneAncienneSauvegardeChargeLesSecretsAvecLesValeursParDefaut() throws Exception {
        String homeAvant = System.getProperty("user.home");
        Path home = Files.createTempDirectory("licorne-save-");
        try {
            Files.writeString(home.resolve(".licorne_survivor.properties"), "sucre=17\nmondeMax=1\n");
            System.setProperty("user.home", home.toString());
            Progression progression = new Progression();
            Sauvegarde.charger(progression, new Reglages());
            assertEquals(17, progression.sucre);
            assertEquals(0, progression.secretsTrouves);
            for (boolean secretTrouve : progression.secretMonde) assertFalse(secretTrouve);
        } finally {
            System.setProperty("user.home", homeAvant);
            Files.deleteIfExists(home.resolve(".licorne_survivor.properties"));
            Files.deleteIfExists(home);
        }
    }
}
