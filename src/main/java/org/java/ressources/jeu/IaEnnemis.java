package org.java.ressources.jeu;

import org.java.ressources.entites.Ennemi;
import org.java.ressources.entites.TirEnnemi;
import org.java.ressources.entites.TypeEnnemi;
import org.java.ressources.entites.ZoneDanger;

import java.awt.Color;

import static org.java.ressources.donnees.Constantes.DT;

/**
 * Comportements des ennemis « spéciaux » : tireur, kamikaze et boss à patterns d'attaque.
 * Les ennemis de base (macaron, guimauve, gomme) foncent simplement sur le joueur (voir Partie).
 */
final class IaEnnemis {
    private IaEnnemis() {
    }

    // ------------------------------------------------------------------
    // Tireur : garde ses distances et lance des bonbons (avec un « ! » d'avertissement)
    // ------------------------------------------------------------------
    static void tireur(Partie p, Ennemi en) {
        double dx = p.px - en.x, dy = p.py - en.y;
        double d = Math.max(1, Math.hypot(dx, dy));
        double ang = Math.atan2(dy, dx);
        en.cd -= DT;
        if (en.etape == 1) { // il vise : immobile un court instant
            en.minuteur -= DT;
            if (en.minuteur <= 0) {
                en.etape = 0;
                en.cd = 2.3 + p.rnd.nextDouble() * 0.8;
                int nb = en.elite ? 3 : 1;
                for (int k = 0; k < nb; k++) {
                    tirer(p, en, ang + (k - (nb - 1) / 2.0) * 0.24, 235, 7, TirEnnemi.BONBON, en.degats * 1.7, 3.2);
                }
            }
            return;
        }
        if (d > 330) p.avancerEnnemi(en, ang, en.vitesse);
        else if (d < 210) p.avancerEnnemi(en, ang + Math.PI, en.vitesse * 0.9);
        else p.avancerEnnemi(en, ang + en.sens * Math.PI / 2, en.vitesse * 0.55);
        if (en.cd <= 0 && d < 520) {
            en.etape = 1;
            en.minuteur = 0.55;
        }
    }

    static void tirer(Partie p, Ennemi en, double angle, double vitesse, double rayon, int genre,
                      double degats, double duree) {
        if (p.tirsEnnemis.size() >= 320) return;
        TirEnnemi t = new TirEnnemi();
        t.x = en.x;
        t.y = en.y;
        t.vx = Math.cos(angle) * vitesse;
        t.vy = Math.sin(angle) * vitesse;
        t.rayon = rayon;
        t.genre = genre;
        t.degats = degats;
        t.vie = duree;
        p.tirsEnnemis.add(t);
    }

    // ------------------------------------------------------------------
    // Kamikaze : fonce, s'arrête près du joueur, clignote puis explose
    // ------------------------------------------------------------------
    static void kamikaze(Partie p, Ennemi en) {
        double dx = p.px - en.x, dy = p.py - en.y;
        double d = Math.hypot(dx, dy);
        if (en.etape == 0) {
            p.avancerEnnemi(en, Math.atan2(dy, dx), en.vitesse);
            if (d < 90) {
                en.etape = 1;
                en.minuteur = 0.6;
            }
            return;
        }
        en.minuteur -= DT;
        en.flash = ((int) (en.minuteur * 14)) % 2 == 0 ? 0.05 : 0; // clignote en blanc
        if (en.minuteur > 0) return;
        double rayon = en.elite ? 115 : 80;
        if (Math.hypot(p.px - en.x, p.py - en.y) < rayon + Partie.RAYON_JOUEUR * 0.5) {
            p.blesserJoueur(en.degats * 4.5, 0.8);
        }
        for (Partie.JoueurReseau joueur : p.joueursReseau) {
            if (joueur.vie > 0 && joueur.esquiveT <= 0
                    && Math.hypot(joueur.x - en.x, joueur.y - en.y) < rayon + Partie.RAYON_JOUEUR * 0.5) {
                joueur.vie = Math.max(0, joueur.vie - en.degats * 4.5);
            }
        }
        // anneau d'explosion visible un instant
        ZoneDanger z = new ZoneDanger();
        z.x = en.x;
        z.y = en.y;
        z.rayon = rayon;
        z.ennemie = true;
        z.explosee = true;
        z.apres = 0.3;
        z.delaiMax = 0.3;
        p.zones.add(z);
        p.explosion(en.x, en.y, 26, new Color(255, 150, 80));
        p.shake = Math.max(p.shake, 7);
        en.pv = 0;
    }

    // ------------------------------------------------------------------
    // Boss : 5 attaques, 3 phases (la 3e est une « rage » sous 33 % de vie)
    // ------------------------------------------------------------------
    static final int CHARGE = 0, ANNEAU = 1, PILONNAGE = 2, INVOCATION = 3, SOUFFLE = 4;
    /** Étapes du boss (champ Ennemi.etape). */
    static final int NORMAL = 0, TELEGRAPHE = 1, EN_CHARGE = 2, REPOS = 3, INCANTATION = 4;

    static void boss(Partie p, Ennemi en) {
        double ratio = en.pv / en.pvMax;
        int phase = ratio > 0.66 ? 1 : ratio > 0.33 ? 2 : 3;
        if (phase != en.phase) {
            en.phase = phase;
            en.cd = Math.max(en.cd, 1.0);
            p.shake = 14;
            p.explosion(en.x, en.y, 40, en.col);
            p.annonce(phase == 2 ? "Le boss se déchaîne !" : "RAGE ! Le boss accélère !");
        }
        double enrage = phase == 3 ? 1.25 : 1.0;
        double dx = p.px - en.x, dy = p.py - en.y;
        double d = Math.max(1, Math.hypot(dx, dy));
        en.minuteur -= DT;

        switch (en.etape) {
            case NORMAL -> {
                en.x += dx / d * en.vitesse * enrage * DT;
                en.y += dy / d * en.vitesse * enrage * DT;
                en.cd -= DT;
                if (en.cd <= 0) choisirAttaque(p, en, phase);
            }
            case TELEGRAPHE -> { // immobile : il suit le joueur, puis verrouille sa direction
                en.flash = ((int) (en.minuteur * 10)) % 2 == 0 ? 0.05 : 0;
                if (en.minuteur > 0.3) {
                    en.dirX = dx / d;
                    en.dirY = dy / d;
                }
                if (en.minuteur <= 0) {
                    en.etape = EN_CHARGE;
                    en.minuteur = 0.7;
                }
            }
            case EN_CHARGE -> {
                en.x += en.dirX * 460 * enrage * DT;
                en.y += en.dirY * 460 * enrage * DT;
                double marge = 60;
                boolean mur = en.x < marge || en.x > p.carte.largeur - marge
                        || en.y < marge || en.y > p.carte.hauteur - marge;
                en.x = Math.clamp(en.x, marge, p.carte.largeur - marge);
                en.y = Math.clamp(en.y, marge, p.carte.hauteur - marge);
                if (en.minuteur <= 0 || mur) {
                    en.etape = REPOS;
                    en.minuteur = 1.0; // étourdi : c'est le moment de l'attaquer
                    p.shake = Math.max(p.shake, 8);
                    if (en.monde == 1) zone(p, en.x, en.y, 170, 0.5, en.degats * 0.9); // onde de choc
                }
            }
            case REPOS -> {
                if (en.minuteur <= 0) {
                    en.etape = NORMAL;
                    en.cd = 1.4;
                }
            }
            default -> { // INCANTATION
                en.flash = ((int) (en.minuteur * 8)) % 2 == 0 ? 0.05 : 0;
                if (en.minuteur <= 0) declencher(p, en, phase);
            }
        }
    }

    private static void choisirAttaque(Partie p, Ennemi en, int phase) {
        double[] w = new double[5];
        switch (en.monde) {
            case 0 -> { // Reine Meringue : projectiles et renforts
                w[ANNEAU] = 3;
                w[PILONNAGE] = 2;
                if (phase >= 2) {
                    w[INVOCATION] = 2;
                    w[CHARGE] = 1;
                }
            }
            case 1 -> { // Roi Guimauve : charges et ondes de choc
                w[CHARGE] = 3;
                w[PILONNAGE] = 2;
                w[ANNEAU] = 1;
                if (phase >= 2) w[INVOCATION] = 1.5;
            }
            default -> { // Dragon Caramel : souffle de feu
                w[SOUFFLE] = 3;
                w[CHARGE] = 2;
                w[PILONNAGE] = 2;
                if (phase >= 2) w[INVOCATION] = 1;
                if (phase >= 3) w[ANNEAU] = 2;
            }
        }
        if (en.attaque >= 0) w[en.attaque] *= 0.25;       // évite de répéter la même attaque
        if (p.ennemis.size() > 140) w[INVOCATION] = 0;    // pas de renforts si l'écran est déjà plein
        double total = 0;
        for (double v : w) total += v;
        double r = p.rnd.nextDouble() * total;
        int a = 0;
        for (int i = 0; i < w.length; i++) {
            r -= w[i];
            if (r < 0) {
                a = i;
                break;
            }
        }
        en.attaque = a;
        en.dirX = p.rnd.nextDouble() * Math.PI * 2; // angle de départ (anneaux)
        en.dirY = 0;                                // numéro de la vague en cours
        switch (a) {
            case CHARGE -> {
                en.etape = TELEGRAPHE;
                en.minuteur = phase == 3 ? 0.8 : 1.0;
            }
            case ANNEAU -> {
                en.etape = INCANTATION;
                en.minuteur = 0.75;
                en.rafales = phase == 3 ? 1 : 0;
            }
            case PILONNAGE -> {
                en.etape = INCANTATION;
                en.minuteur = 0.55;
            }
            case INVOCATION -> {
                en.etape = INCANTATION;
                en.minuteur = 0.9;
            }
            default -> { // SOUFFLE
                en.etape = INCANTATION;
                en.minuteur = 0.7;
                en.rafales = phase >= 2 ? 1 : 0;
            }
        }
    }

    /** Fin de l'incantation : l'attaque part. */
    private static void declencher(Partie p, Ennemi en, int phase) {
        double dmg = en.degats * 0.5;
        switch (en.attaque) {
            case ANNEAU -> {
                int n = 10 + 4 * phase;
                double base = en.dirX + (en.dirY % 2) * Math.PI / n;
                for (int i = 0; i < n; i++) {
                    tirer(p, en, base + i * 2 * Math.PI / n, 175 + 20 * phase, 8, TirEnnemi.BONBON, dmg, 5);
                }
                p.audio.tir();
            }
            case PILONNAGE -> pilonnage(p, en, phase);
            case INVOCATION -> invoquer(p, en, phase);
            case SOUFFLE -> {
                int n = 7 + 3 * (phase - 1);
                double visee = Math.atan2(p.py - en.y, p.px - en.x) + (en.dirY % 2) * 0.12;
                for (int i = 0; i < n; i++) {
                    double a = visee + (i - (n - 1) / 2.0) * (0.95 / n);
                    tirer(p, en, a, 250 + p.rnd.nextDouble() * 80, 9, TirEnnemi.FLAMME, dmg, 1.9);
                }
                p.audio.tir();
            }
            default -> {
            }
        }
        if (en.rafales > 0) { // deuxième vague : on recommence juste après
            en.rafales--;
            en.dirY++;
            en.minuteur = 0.55;
            return;
        }
        en.etape = NORMAL;
        en.cd = Math.max(0.8, 2.7 - 0.5 * phase) + p.rnd.nextDouble() * 0.5;
    }

    private static void zone(Partie p, double x, double y, double rayon, double delai, double degats) {
        if (p.zones.size() >= 60) return;
        double m = 50;
        ZoneDanger z = new ZoneDanger();
        z.x = Math.clamp(x, m, p.carte.largeur - m);
        z.y = Math.clamp(y, m, p.carte.hauteur - m);
        z.rayon = rayon;
        z.delai = delai;
        z.delaiMax = delai;
        z.degats = degats;
        z.ennemie = true;
        p.zones.add(z);
    }

    /** Des zones rouges apparaissent sous le joueur (et autour) : il faut bouger ou esquiver. */
    private static void pilonnage(Partie p, Ennemi en, int phase) {
        int n = 4 + 2 * phase;
        for (int k = 0; k < n; k++) {
            double x, y;
            if (k == 0) {
                x = p.px;
                y = p.py;
            } else if (k == 1 && phase >= 2) { // anticipe le déplacement du joueur
                x = p.px;
                y = p.py;
                double vx = p.px - p.dernierPx, vy = p.py - p.dernierPy;
                x += vx * 55;
                y += vy * 55;
            } else {
                double a = p.rnd.nextDouble() * Math.PI * 2;
                double r = 70 + p.rnd.nextDouble() * 190;
                x = p.px + Math.cos(a) * r;
                y = p.py + Math.sin(a) * r;
            }
            zone(p, x, y, 64, 1.15 + k * 0.12, en.degats * 0.9);
        }
    }

    private static void invoquer(Partie p, Ennemi en, int phase) {
        TypeEnnemi[] types = switch (en.monde) {
            case 0 -> new TypeEnnemi[]{TypeEnnemi.MACARON, TypeEnnemi.GOMME};
            case 1 -> new TypeEnnemi[]{TypeEnnemi.GOMME, TypeEnnemi.GUIMAUVE};
            default -> phase >= 3
                    ? new TypeEnnemi[]{TypeEnnemi.GOMME, TypeEnnemi.KAMIKAZE, TypeEnnemi.TIREUR}
                    : new TypeEnnemi[]{TypeEnnemi.GOMME, TypeEnnemi.KAMIKAZE};
        };
        int n = 3 + phase;
        for (int k = 0; k < n; k++) {
            double a = k * 2 * Math.PI / n + p.rnd.nextDouble() * 0.4;
            p.creerEnnemiA(types[p.rnd.nextInt(types.length)], en.x + Math.cos(a) * 90, en.y + Math.sin(a) * 90);
        }
        p.explosion(en.x, en.y, 20, en.col);
    }
}
