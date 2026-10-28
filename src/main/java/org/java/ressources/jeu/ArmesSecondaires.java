package org.java.ressources.jeu;

import org.java.ressources.donnees.ArmeSecondaire;
import org.java.ressources.entites.Eclair;
import org.java.ressources.entites.Ennemi;
import org.java.ressources.entites.ZoneDanger;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;

import static org.java.ressources.donnees.Constantes.DT;

/** Les armes secondaires (aura, météores, éclairs) : elles s'ajoutent à l'attaque de base. */
final class ArmesSecondaires {
    private ArmesSecondaires() {
    }

    static void maj(Partie p) {
        for (int i = p.eclairs.size() - 1; i >= 0; i--) {
            Eclair e = p.eclairs.get(i);
            e.vie -= DT;
            if (e.vie <= 0) p.eclairs.remove(i);
        }
        aura(p);
        meteores(p);
        eclairs(p);
    }

    /** Brûle tout ce qui est proche du joueur, deux fois par seconde. */
    private static void aura(Partie p) {
        int i = ArmeSecondaire.AURA.ordinal();
        int niv = p.niveauArme[i];
        if (niv <= 0) return;
        p.minuteurArme[i] -= DT;
        if (p.minuteurArme[i] > 0) return;
        p.minuteurArme[i] = 0.5;
        boolean evo = p.evolueeArme[i];
        double rayon = p.rayonAura();
        double dmg = p.degats * (0.5 + 0.22 * niv) * (evo ? 2.0 : 1.0);
        boolean touche = false;
        for (Ennemi e : p.ennemis) {
            if (Math.hypot(e.x - p.px, e.y - p.py) < rayon + e.rayon * 0.5) {
                p.blesser(e, dmg);
                touche = true;
            }
        }
        if (evo && touche) p.pvie = Math.min(p.pvieMax, p.pvie + 1.2); // l'aura royale soigne
    }

    /** Des zones d'impact apparaissent sous les ennemis puis explosent (on voit venir le coup). */
    private static void meteores(Partie p) {
        int i = ArmeSecondaire.METEORES.ordinal();
        int niv = p.niveauArme[i];
        if (niv <= 0) return;
        p.minuteurArme[i] -= DT;
        if (p.minuteurArme[i] > 0) return;
        boolean evo = p.evolueeArme[i];
        p.minuteurArme[i] = Math.max(0.8, 3.2 - 0.45 * niv) * (evo ? 0.8 : 1.0);
        int n = (1 + niv / 2) * (evo ? 2 : 1);
        List<Ennemi> proches = new ArrayList<>();
        for (Ennemi e : p.ennemis) {
            if (proches.size() < 40 && Math.hypot(e.x - p.px, e.y - p.py) < 560) proches.add(e);
        }
        for (int k = 0; k < n && p.zones.size() < 80; k++) {
            double x, y;
            if (proches.isEmpty()) {
                double a = p.rnd.nextDouble() * Math.PI * 2;
                double r = 120 + p.rnd.nextDouble() * 180;
                x = p.px + Math.cos(a) * r;
                y = p.py + Math.sin(a) * r;
            } else {
                Ennemi cible = proches.get(p.rnd.nextInt(proches.size()));
                double d = Math.max(1, Math.hypot(p.px - cible.x, p.py - cible.y));
                // on vise un peu devant l'ennemi, puisqu'il avance vers le joueur
                x = cible.x + (p.px - cible.x) / d * cible.vitesse * 0.7;
                y = cible.y + (p.py - cible.y) / d * cible.vitesse * 0.7;
            }
            ZoneDanger z = new ZoneDanger();
            z.x = x;
            z.y = y;
            z.rayon = 58 + 6 * niv + (evo ? 24 : 0);
            z.delai = 0.7 + k * 0.1;
            z.delaiMax = z.delai;
            z.degats = p.degats * (2.2 + 0.7 * niv) * (evo ? 1.5 : 1.0);
            z.ennemie = false;
            z.evolue = evo;
            p.zones.add(z);
        }
    }

    /** Un éclair part du joueur et saute d'ennemi en ennemi. */
    private static void eclairs(Partie p) {
        int i = ArmeSecondaire.ECLAIRS.ordinal();
        int niv = p.niveauArme[i];
        if (niv <= 0) return;
        p.minuteurArme[i] -= DT;
        if (p.minuteurArme[i] > 0) return;
        boolean evo = p.evolueeArme[i];
        Ennemi cible = p.plusProche(480);
        if (cible == null) {
            p.minuteurArme[i] = 0.2;
            return;
        }
        p.minuteurArme[i] = Math.max(0.9, 2.8 - 0.35 * niv) * (evo ? 0.75 : 1.0);
        int sauts = 2 + niv + (evo ? 5 : 0);
        double dmg = p.degats * (1.0 + 0.35 * niv) * (evo ? 1.6 : 1.0);
        Eclair arc = new Eclair(sauts + 1);
        arc.x[0] = p.px;
        arc.y[0] = p.py;
        List<Ennemi> touches = new ArrayList<>();
        Ennemi courant = cible;
        while (courant != null && arc.n < sauts) {
            touches.add(courant);
            arc.n++;
            arc.x[arc.n] = courant.x;
            arc.y[arc.n] = courant.y;
            p.blesser(courant, dmg);
            p.explosion(courant.x, courant.y, 3, new Color(190, 220, 255));
            Ennemi suivant = null;
            double meilleure = 230;
            for (Ennemi e : p.ennemis) {
                if (touches.contains(e)) continue;
                double d = Math.hypot(e.x - courant.x, e.y - courant.y);
                if (d < meilleure) {
                    meilleure = d;
                    suivant = e;
                }
            }
            courant = suivant;
        }
        arc.n++; // nombre de points = nombre de touches + le point de départ
        p.eclairs.add(arc);
    }
}
