package org.java.ressources.donnees;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.Collections;
import java.util.Properties;

/** Lecture / écriture de la progression et des réglages dans ~/.licorne_survivor.properties. */
public final class Sauvegarde {
    private Sauvegarde() {
    }

    private static File fichier() {
        return new File(System.getProperty("user.home"), ".licorne_survivor.properties");
    }

    private static int entier(Properties p, String cle, int defaut) {
        try {
            return Integer.parseInt(p.getProperty(cle, String.valueOf(defaut)));
        } catch (NumberFormatException e) {
            return defaut;
        }
    }

    private static double pourcent(Properties p, String cle, int defaut) {
        return Math.clamp(entier(p, cle, defaut), 0, 100) / 100.0;
    }

    public static void charger(Progression pr, Reglages rg) {
        try (FileInputStream in = new FileInputStream(fichier())) {
            Properties p = new Properties();
            p.load(in);
            pr.sucre = entier(p, "sucre", 0);
            pr.metaVie = entier(p, "metaVie", 0);
            pr.metaDegats = entier(p, "metaDegats", 0);
            pr.metaVitesse = entier(p, "metaVitesse", 0);
            pr.personnageSel = Math.clamp(entier(p, "personnageSel", 0), 0, Personnage.TOUS.length - 1);
            pr.mondeMax = Math.clamp(entier(p, "mondeMax", 0), 0, Monde.TOUS.length - 1);
            pr.totalKills = entier(p, "totalKills", 0);
            pr.totalSucre = entier(p, "totalSucre", 0);
            pr.parties = entier(p, "parties", 0);
            pr.achats = entier(p, "achats", 0);
            for (int i = 0; i < Monde.TOUS.length; i++) {
                pr.meilleur[i] = entier(p, "meilleur" + i, 0);
                pr.gagne[i] = "true".equals(p.getProperty("gagne" + i));
            }
            String s = p.getProperty("succes", "");
            if (!s.isEmpty()) Collections.addAll(pr.succesOk, s.split(","));

            rg.musique = !"false".equals(p.getProperty("musique"));
            rg.volMusique = pourcent(p, "volMusique", 80);
            rg.volSfx = pourcent(p, "volSfx", 90);
            rg.pleinEcran = "true".equals(p.getProperty("pleinEcran", "true"));
            rg.modeSouris = "true".equals(p.getProperty("modeSouris"));
            rg.secousses = !"false".equals(p.getProperty("secousses"));
            rg.effets = !"false".equals(p.getProperty("effets"));
            rg.pauseAuto = !"false".equals(p.getProperty("pauseAuto"));
            rg.miniCarte = !"false".equals(p.getProperty("miniCarte"));
            int f = entier(p, "fps", 60);
            rg.fps = java.util.Arrays.stream(Reglages.FPS_CHOIX).anyMatch(valeur -> valeur == f) ? f : 60;
        } catch (Exception e) {
            // pas de sauvegarde : on commence à zéro
        }
    }

    public static void sauver(Progression pr, Reglages rg) {
        Properties p = new Properties();
        p.setProperty("sucre", String.valueOf(pr.sucre));
        p.setProperty("metaVie", String.valueOf(pr.metaVie));
        p.setProperty("metaDegats", String.valueOf(pr.metaDegats));
        p.setProperty("metaVitesse", String.valueOf(pr.metaVitesse));
        p.setProperty("personnageSel", String.valueOf(pr.personnageSel));
        p.setProperty("mondeMax", String.valueOf(pr.mondeMax));
        p.setProperty("totalKills", String.valueOf(pr.totalKills));
        p.setProperty("totalSucre", String.valueOf(pr.totalSucre));
        p.setProperty("parties", String.valueOf(pr.parties));
        p.setProperty("achats", String.valueOf(pr.achats));
        for (int i = 0; i < Monde.TOUS.length; i++) {
            p.setProperty("meilleur" + i, String.valueOf((int) pr.meilleur[i]));
            p.setProperty("gagne" + i, String.valueOf(pr.gagne[i]));
        }
        p.setProperty("succes", String.join(",", pr.succesOk));

        p.setProperty("musique", String.valueOf(rg.musique));
        p.setProperty("volMusique", String.valueOf((int) Math.round(rg.volMusique * 100)));
        p.setProperty("volSfx", String.valueOf((int) Math.round(rg.volSfx * 100)));
        p.setProperty("pleinEcran", String.valueOf(rg.pleinEcran));
        p.setProperty("modeSouris", String.valueOf(rg.modeSouris));
        p.setProperty("secousses", String.valueOf(rg.secousses));
        p.setProperty("effets", String.valueOf(rg.effets));
        p.setProperty("pauseAuto", String.valueOf(rg.pauseAuto));
        p.setProperty("miniCarte", String.valueOf(rg.miniCarte));
        p.setProperty("fps", String.valueOf(rg.fps));
        try (FileOutputStream out = new FileOutputStream(fichier())) {
            p.store(out, "Licorne Survivor");
        } catch (Exception e) {
            // tant pis, on ne plante pas le jeu pour ça
        }
    }
}
