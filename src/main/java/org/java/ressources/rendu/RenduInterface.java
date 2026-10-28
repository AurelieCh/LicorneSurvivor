package org.java.ressources.rendu;

import org.java.ressources.donnees.Amelio;
import org.java.ressources.donnees.Constantes;
import org.java.ressources.donnees.Monde;
import org.java.ressources.donnees.Pouvoir;
import org.java.ressources.donnees.Personnage;
import org.java.ressources.donnees.Progression;
import org.java.ressources.donnees.Reglages;
import org.java.ressources.donnees.Succes;
import org.java.ressources.entites.Bonus;
import org.java.ressources.entites.Ennemi;
import org.java.ressources.entites.Obstacle;
import org.java.ressources.jeu.Carte;
import org.java.ressources.jeu.Partie;
import org.java.ressources.jeu.Session;
import org.java.ressources.reseau.ReseauLan;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.FontMetrics;
import java.awt.GradientPaint;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.geom.AffineTransform;
import java.util.Random;
import java.util.Deque;

import static org.java.ressources.donnees.Constantes.H;
import static org.java.ressources.donnees.Constantes.NB_PARAMS;
import static org.java.ressources.donnees.Constantes.W;
import static org.java.ressources.rendu.Dessin.F_GRAND;
import static org.java.ressources.rendu.Dessin.F_MINI;
import static org.java.ressources.rendu.Dessin.F_MOYEN;
import static org.java.ressources.rendu.Dessin.F_PETIT;
import static org.java.ressources.rendu.Dessin.F_TITRE;
import static org.java.ressources.rendu.Dessin.SOMBRE;
import static org.java.ressources.rendu.Dessin.bouton;
import static org.java.ressources.rendu.Dessin.chrono;
import static org.java.ressources.rendu.Dessin.etoile;
import static org.java.ressources.rendu.Dessin.plaque;
import static org.java.ressources.rendu.Dessin.texteCentre;

/** Tous les écrans « interface » : HUD, menus, pause, paramètres, succès, fin de partie. */
public class RenduInterface {
    private static final int MINI_W = 180, MINI_H = 120;

    private final double[][] etincelles = new double[70][3];
    /** Décor des côtés du menu principal, par monde : {type, x, y, u}. */
    private static final double[][][] SCENERIE = {
            {{3, 62, 540, 44}, {0, 120, 612, 40}, {1, 36, 612, 36}, {2, 888, 560, 44}, {0, 842, 618, 38}, {1, 925, 618, 34}},
            {{0, 66, 560, 48}, {1, 118, 616, 40}, {3, 30, 620, 34}, {1, 890, 556, 42}, {0, 846, 622, 46}, {3, 930, 624, 30}},
            {{0, 64, 560, 48}, {1, 120, 618, 40}, {3, 34, 622, 38}, {3, 888, 548, 40}, {1, 850, 620, 42}, {0, 930, 622, 38}},
    };
    private final Obstacle[][] decorMenu = new Obstacle[Monde.TOUS.length][];

    public RenduInterface() {
        Random r = new Random(5);
        for (double[] f : etincelles) {
            f[0] = r.nextDouble() * W;
            f[1] = r.nextDouble() * H;
            f[2] = r.nextDouble() * 6.28;
        }
        for (int m = 0; m < Monde.TOUS.length; m++) {
            decorMenu[m] = new Obstacle[SCENERIE[m].length];
            for (int i = 0; i < SCENERIE[m].length; i++) {
                double[] d = SCENERIE[m][i];
                Obstacle o = new Obstacle();
                o.monde = m;
                o.type = (int) d[0];
                o.x = d[1];
                o.y = d[2];
                o.echelle = d[3];
                o.graine = r.nextDouble();
                decorMenu[m][i] = o;
            }
        }
    }

    // ------------------------------------------------------------------
    // Fond des écrans de menu
    // ------------------------------------------------------------------
    public void fondMenu(Graphics2D g, Session s) {
        Monde mo = Monde.TOUS[s.mondeSel];
        g.setPaint(new GradientPaint(0, 0, mo.haut, 0, H, mo.bas));
        g.fillRect(0, 0, W, H);
        g.setColor(mo.deco);
        for (double[] f : etincelles) {
            int sz = (int) Math.max(1, 3 + 2 * Math.sin(s.anim * 2 + f[2]));
            g.fillOval((int) f[0], (int) f[1], sz, sz);
        }
        // un aperçu de la direction artistique du monde choisi, sur les côtés
        for (Obstacle o : decorMenu[s.mondeSel]) Obstacles.dessiner(g, o, s.anim);
    }

    // ------------------------------------------------------------------
    // HUD
    // ------------------------------------------------------------------
    /** Icône d'un pouvoir (centrée sur x, y). */
    public static void icone(Graphics2D g, int type, int x, int y, double anim) {
        g.setColor(Pouvoir.TOUS[type].col);
        g.fillOval(x - 14, y - 14, 28, 28);
        g.setColor(Color.WHITE);
        g.setStroke(new BasicStroke(2.5f));
        g.drawOval(x - 14, y - 14, 28, 28);
        switch (type) {
            case Pouvoir.COEUR:
                g.fillOval(x - 8, y - 8, 9, 9);
                g.fillOval(x - 1, y - 8, 9, 9);
                g.fillPolygon(new int[]{x - 8, x + 8, x}, new int[]{y - 1, y - 1, y + 8}, 3);
                break;
            case Pouvoir.AIMANT:
                g.setStroke(new BasicStroke(3));
                g.drawArc(x - 7, y - 8, 14, 14, 180, 180);
                g.drawLine(x - 7, y - 1, x - 7, y - 8);
                g.drawLine(x + 7, y - 1, x + 7, y - 8);
                break;
            case Pouvoir.BOUCLIER:
                g.setStroke(new BasicStroke(3));
                g.drawOval(x - 7, y - 7, 14, 14);
                g.fillOval(x - 3, y - 3, 6, 6);
                break;
            case Pouvoir.TURBO:
                g.fillPolygon(new int[]{x + 3, x - 5, x, x - 3, x + 5, x},
                        new int[]{y - 9, y + 1, y + 1, y + 9, y - 2, y - 2}, 6);
                break;
            case Pouvoir.NOVA:
                etoile(g, x, y, 10, 4.5, anim * 2);
                break;
            case Pouvoir.DOUBLE:
                g.setFont(F_MINI);
                texteCentre(g, "x2", x, y + 5);
                break;
            default:
                break;
        }
    }

    public void hud(Graphics2D g, Session s) {
        Partie p = s.partie;
        Monde mo = p.monde;
        Reglages reg = s.reg;

        // barre d'expérience en haut
        g.setColor(new Color(255, 255, 255, 150));
        g.fillRect(0, 0, W, 10);
        g.setColor(p.tXp > 0 ? new Color(120, 220, 170) : new Color(190, 150, 255));
        g.fillRect(0, 0, (int) (W * Math.min(1.0, (double) p.xp / p.xpSuivant)), 10);

        // barre de vie
        g.setColor(new Color(80, 50, 110, 170));
        g.fillRoundRect(14, 20, 224, 20, 12, 12);
        g.setColor(new Color(255, 110, 160));
        g.fillRoundRect(16, 22, (int) (220 * Math.max(0, p.pvie / p.pvieMax)), 16, 10, 10);
        g.setFont(F_PETIT);
        g.setColor(Color.WHITE);
        g.drawString((int) Math.max(0, p.pvie) + " / " + (int) p.pvieMax, 24, 36);

        // pouvoirs actifs (avec le temps restant)
        double[] restes = {p.tBouclier, p.tTurbo, p.tXp};
        int[] types = {Pouvoir.BOUCLIER, Pouvoir.TURBO, Pouvoir.DOUBLE};
        int ey = 48;
        for (int k = 0; k < restes.length; k++) {
            if (restes[k] <= 0) continue;
            Pouvoir pv = Pouvoir.TOUS[types[k]];
            g.setColor(new Color(80, 50, 110, 160));
            g.fillRoundRect(14, ey, 170, 18, 12, 12);
            g.setColor(pv.col);
            g.fillRoundRect(16, ey + 2, (int) (166 * Math.min(1, restes[k] / pv.duree)), 14, 10, 10);
            g.setFont(F_MINI);
            g.setColor(SOMBRE);
            g.drawString(pv.nom, 22, ey + 14);
            ey += 22;
        }

        // infos à droite
        plaque(g, W - 292, 16, 278, 34);
        g.setFont(F_MOYEN);
        g.setColor(SOMBRE);
        g.drawString("Niv. " + p.niveau, W - 280, 40);
        g.drawString(chrono(p.temps), W - 190, 40);
        g.drawString("x" + p.kills, W - 100, 40);

        plaque(g, W - 292, 56, 278, 28);
        g.setFont(F_MINI);
        g.setColor(SOMBRE);
        String competence = p.personnage.competence + " [E] ";
        competence += p.competenceTimer > 0 ? Math.ceil(p.competenceTimer) + "s" : "PRÊTE";
        g.drawString(competence, W - 280, 75);

        // alerte boss
        if (s.etat == org.java.ressources.jeu.Etat.JEU && !p.bossApparu && p.temps >= mo.dureeBoss - 15) {
            g.setFont(F_GRAND);
            String t = mo.boss + " arrive...";
            int w = g.getFontMetrics().stringWidth(t) + 40;
            plaque(g, W / 2 - w / 2, 68, w, 42);
            g.setColor(new Color(235, 70, 125, 150 + (int) (100 * Math.sin(s.anim * 8))));
            texteCentre(g, t, W / 2, 100);
        }
        // barre de vie du boss
        for (Ennemi e : p.ennemis) {
            if (!e.boss) continue;
            g.setColor(new Color(80, 50, 110, 190));
            g.fillRoundRect(W / 2 - 200, 56, 400, 18, 12, 12);
            g.setColor(new Color(255, 120, 170));
            g.fillRoundRect(W / 2 - 198, 58, (int) (396 * Math.max(0, e.pv / e.pvMax)), 14, 10, 10);
            plaque(g, W / 2 - 110, 78, 220, 24);
            g.setFont(F_MINI);
            g.setColor(SOMBRE);
            texteCentre(g, mo.boss, W / 2, 95);
            flecheVers(g, p, e);
        }
        // message « pouvoir ramassé »
        if (p.msgT > 0 && s.etat == org.java.ressources.jeu.Etat.JEU) {
            Pouvoir pv = Pouvoir.TOUS[p.msgType];
            String t = pv.nom + " : " + pv.desc;
            g.setFont(F_MOYEN);
            int w = g.getFontMetrics().stringWidth(t) + 40;
            int a = (int) (255 * Math.min(1, p.msgT * 2));
            g.setColor(new Color(80, 50, 110, Math.min(210, a)));
            g.fillRoundRect(W / 2 - w / 2, 118, w, 34, 24, 24);
            g.setColor(new Color(255, 255, 255, a));
            texteCentre(g, t, W / 2, 141);
        }
        if (reg.miniCarte) miniCarte(g, p);
        // cible de la souris
        if (reg.modeSouris && s.etat == org.java.ressources.jeu.Etat.JEU) {
            g.setStroke(new BasicStroke(2));
            g.setColor(new Color(255, 110, 160, 200));
            int mx = (int) s.sourisX, my = (int) s.sourisY;
            g.drawOval(mx - 10, my - 10, 20, 20);
            g.fillOval(mx - 2, my - 2, 4, 4);
        }
    }

    public void console(Graphics2D g, String texte, Deque<String> lignes) {
        g.setColor(new Color(12, 10, 24, 235));
        g.fillRoundRect(70, 70, 820, 255, 14, 14);
        g.setColor(new Color(120, 255, 180));
        g.setStroke(new BasicStroke(2));
        g.drawRoundRect(70, 70, 820, 255, 14, 14);
        g.setFont(F_MOYEN);
        g.drawString("CONSOLE DEV  [F2 fermer]", 92, 100);
        g.setFont(F_PETIT);
        int y = 130;
        for (String ligne : lignes) {
            g.drawString(ligne, 92, y);
            y += 22;
        }
        g.drawString("> " + texte + "_", 92, 300);
    }

    /** Flèche au bord de l'écran qui montre où est le boss quand il est hors de vue. */
    private void flecheVers(Graphics2D g, Partie p, Ennemi boss) {
        double sx = boss.x - p.camX, sy = boss.y - p.camY;
        if (sx > 0 && sx < W && sy > 0 && sy < H) return;
        double cx = W / 2.0, cy = H / 2.0;
        double a = Math.atan2(sy - cy, sx - cx);
        double ex = Math.clamp(cx + Math.cos(a) * 2000, 40, W - 40);
        double ey = Math.clamp(cy + Math.sin(a) * 2000, 70, H - 60);
        AffineTransform old = g.getTransform();
        g.translate(ex, ey);
        g.rotate(a);
        g.setColor(new Color(255, 100, 150, 230));
        g.fillPolygon(new int[]{16, -10, -4, -10}, new int[]{0, -12, 0, 12}, 4);
        g.setColor(Color.WHITE);
        g.setStroke(new BasicStroke(2));
        g.drawPolygon(new int[]{16, -10, -4, -10}, new int[]{0, -12, 0, 12}, 4);
        g.setTransform(old);
    }

    private void miniCarte(Graphics2D g, Partie p) {
        Carte c = p.carte;
        if (c.mini == null) c.mini = RenduMonde.creerMini(c, MINI_W, MINI_H);
        int x = W - MINI_W - 14, y = H - MINI_H - 14;
        double sc = MINI_W / (double) c.largeur;
        g.setColor(new Color(255, 255, 255, 190));
        g.fillRoundRect(x - 4, y - 4, MINI_W + 8, MINI_H + 8, 14, 14);
        g.drawImage(c.mini, x, y, null);
        g.setColor(new Color(235, 60, 90, 200));
        for (Ennemi e : p.ennemis) {
            int r = e.boss ? 4 : 1;
            if (e.boss) g.setColor(new Color(255, 70, 150));
            g.fillRect((int) (x + e.x * sc) - r / 2, (int) (y + e.y * sc) - r / 2, r + 1, r + 1);
            if (e.boss) g.setColor(new Color(235, 60, 90, 200));
        }
        g.setColor(new Color(255, 230, 80));
        for (Bonus b : p.bonus) g.fillRect((int) (x + b.x * sc) - 1, (int) (y + b.y * sc) - 1, 3, 3);
        g.setColor(Color.WHITE);
        g.setStroke(new BasicStroke(1.2f));
        g.drawRect((int) (x + p.camX * sc), (int) (y + p.camY * sc), (int) (W * sc), (int) (H * sc));
        g.setColor(new Color(255, 110, 160));
        g.fillOval((int) (x + p.px * sc) - 3, (int) (y + p.py * sc) - 3, 7, 7);
        g.setColor(Color.WHITE);
        g.drawOval((int) (x + p.px * sc) - 3, (int) (y + p.py * sc) - 3, 7, 7);
    }

    // ------------------------------------------------------------------
    // Niveau supérieur, pause, fin
    // ------------------------------------------------------------------
    public void niveau(Graphics2D g, Session s) {
        Partie p = s.partie;
        g.setColor(new Color(60, 30, 80, 170));
        g.fillRect(0, 0, W, H);
        g.setColor(Color.WHITE);
        g.setFont(F_TITRE);
        texteCentre(g, "NIVEAU " + p.niveau + " !", W / 2, 125);
        g.setFont(F_MOYEN);
        texteCentre(g, "Choisis une amélioration (clic ou touches 1 / 2 / 3)", W / 2, 160);
        for (int i = 0; i < p.choix.size(); i++) {
            Amelio a = p.choix.get(i);
            Rectangle r = Zones.carte(i);
            g.setColor(a.col);
            g.fillRoundRect(r.x, r.y, r.width, r.height, 30, 30);
            g.setColor(Color.WHITE);
            g.setStroke(new BasicStroke(4));
            g.drawRoundRect(r.x, r.y, r.width, r.height, 30, 30);
            g.setColor(SOMBRE);
            g.setFont(F_TITRE);
            texteCentre(g, String.valueOf(i + 1), r.x + r.width / 2, r.y + 70);
            g.setFont(F_MOYEN);
            texteCentre(g, a.nom, r.x + r.width / 2, r.y + 125);
            g.setFont(F_PETIT);
            int ly = r.y + 170;
            for (String ligne : a.desc.split("\n")) {
                texteCentre(g, ligne, r.x + r.width / 2, ly);
                ly += 24;
            }
        }
    }

    public void pause(Graphics2D g, Session s) {
        g.setColor(new Color(60, 30, 80, 190));
        g.fillRect(0, 0, W, H);
        g.setColor(Color.WHITE);
        g.setFont(F_TITRE);
        texteCentre(g, "PAUSE", W / 2, 170);
        bouton(g, Zones.btnReprendre(), "Reprendre (P)", new Color(255, 130, 170));
        bouton(g, Zones.btnParams(), "Paramètres (O)", new Color(190, 150, 240));
        bouton(g, Zones.btnAbandon(), "Abandonner (X)", new Color(150, 130, 200));
        g.setColor(Color.WHITE);
        g.setFont(F_PETIT);
        texteCentre(g, "Musique : " + (s.reg.musique ? "activée" : "coupée") + "  (touche M)", W / 2, 428);
        texteCentre(g, "Abandonner te fait quand même gagner le sucre de la partie.", W / 2, 454);

        // légende des pouvoirs
        texteCentre(g, "Pouvoirs lâchés par les ennemis  (% = part des drops)", W / 2, 500);
        double tot = Pouvoir.poidsTotal();
        for (int i = 0; i < Pouvoir.TOUS.length; i++) {
            Pouvoir pv = Pouvoir.TOUS[i];
            int cx = W / 2 - 330 + (i % 2) * 340;
            int cy = 530 + (i / 2) * 36;
            icone(g, i, cx + 16, cy, s.anim);
            g.setFont(F_MINI);
            g.setColor(Color.WHITE);
            g.drawString(pv.nom + " - " + pv.desc + "  " + Math.round(100 * pv.poids / tot) + "%", cx + 38, cy + 5);
        }
    }

    public void fin(Graphics2D g, Session s) {
        Partie p = s.partie;
        Monde mo = p.monde;
        g.setColor(new Color(60, 30, 80, 190));
        g.fillRect(0, 0, W, H);
        g.setColor(Color.WHITE);
        g.setFont(F_TITRE);
        texteCentre(g, p.victoire ? "VICTOIRE !" : "Oh non, tu as été croquée...", W / 2, 170);
        g.setFont(F_MOYEN);
        if (p.victoire) texteCentre(g, "Boss vaincu : " + mo.boss + "  (" + mo.nom + ")", W / 2, 210);
        else texteCentre(g, mo.nom, W / 2, 210);
        g.setFont(F_GRAND);
        texteCentre(g, "Survie : " + chrono(p.temps) + "     Sucreries vaincues : " + p.kills, W / 2, 280);
        g.setColor(new Color(255, 220, 120));
        texteCentre(g, "+ " + p.gainRun + " sucre   (total : " + s.prog.sucre + ")", W / 2, 335);
        if (p.nouveauMonde) {
            g.setColor(new Color(160, 255, 200));
            texteCentre(g, "Nouveau monde débloqué : " + Monde.TOUS[s.prog.mondeMax].nom + " !", W / 2, 395);
        }
        g.setColor(Color.WHITE);
        g.setFont(F_MOYEN);
        texteCentre(g, "ESPACE : boutique et nouvelle partie", W / 2, 470);
    }

    // ------------------------------------------------------------------
    // Succès
    // ------------------------------------------------------------------
    public void popup(Graphics2D g, Session s) {
        Succes su = s.popups.getFirst();
        double entree = Math.min(1, (3.5 - s.popupTimer) * 4);
        double sortie = Math.min(1, s.popupTimer * 4);
        double ease = Math.min(entree, sortie);
        int w = 360, h = 70;
        int x = (int) (W - 14 - w * ease), y = 56;
        g.setColor(new Color(255, 248, 215));
        g.fillRoundRect(x, y, w, h, 24, 24);
        g.setColor(new Color(255, 190, 60));
        g.setStroke(new BasicStroke(4));
        g.drawRoundRect(x, y, w, h, 24, 24);
        g.setColor(new Color(255, 200, 60));
        etoile(g, x + 36, y + h / 2.0, 20, 9, s.anim * 2);
        g.setColor(SOMBRE);
        g.setFont(F_PETIT);
        g.drawString("Succès débloqué !   +" + su.recompense + " sucre", x + 70, y + 27);
        g.setFont(F_MOYEN);
        g.drawString(su.nom, x + 70, y + 52);
    }

    public void succes(Graphics2D g, Session s) {
        Progression pr = s.prog;
        g.setColor(SOMBRE);
        g.setFont(F_TITRE);
        texteCentre(g, "Succès  " + pr.succesOk.size() + " / " + Succes.TOUS.length, W / 2, 80);
        for (int i = 0; i < Succes.TOUS.length; i++) {
            Succes su = Succes.TOUS[i];
            boolean ok = pr.succesOk.contains(su.id);
            int x = 30 + (i % 2) * 460, y = 105 + (i / 2) * 56;
            g.setColor(ok ? new Color(255, 248, 215) : new Color(255, 255, 255, 110));
            g.fillRoundRect(x, y, 440, 50, 22, 22);
            g.setColor(ok ? new Color(255, 190, 60) : new Color(190, 180, 210));
            g.setStroke(new BasicStroke(3));
            g.drawRoundRect(x, y, 440, 50, 22, 22);
            g.setColor(ok ? new Color(255, 200, 60) : new Color(200, 190, 220));
            etoile(g, x + 28, y + 25, 16, 7, ok ? s.anim * 2 : 0);
            g.setColor(ok ? SOMBRE : new Color(140, 125, 165));
            g.setFont(F_MOYEN);
            g.drawString(su.nom, x + 56, y + 21);
            g.setFont(F_PETIT);
            g.drawString(su.desc, x + 56, y + 42);
            String rec = "+" + su.recompense;
            FontMetrics fm = g.getFontMetrics();
            g.drawString(rec, x + 425 - fm.stringWidth(rec), y + 21);
        }
        g.setColor(SOMBRE);
        g.setFont(F_PETIT);
        texteCentre(g, "Ennemis vaincus : " + pr.totalKills + "     Parties jouées : " + pr.parties
                + "     Sucre gagné au total : " + pr.totalSucre, W / 2, 590);
        texteCentre(g, "ÉCHAP / ESPACE / clic : retour", W / 2, 615);
    }

    // ------------------------------------------------------------------
    // Paramètres
    // ------------------------------------------------------------------
    private void pilule(Graphics2D g, Rectangle r, String texte, Color c) {
        int w = 150, h = 28;
        int x = r.x + r.width - 16 - w, y = r.y + 4;
        g.setColor(c);
        g.fillRoundRect(x, y, w, h, 24, 24);
        g.setColor(Color.WHITE);
        g.setFont(F_PETIT);
        texteCentre(g, texte, x + w / 2, y + 20);
    }

    private void texteCarte(Graphics2D g, String texte, int x, int y, int largeur, int lignesMax) {
        FontMetrics fm = g.getFontMetrics();
        String reste = texte;
        for (int ligne = 0; ligne < lignesMax && !reste.isEmpty(); ligne++) {
            int fin = reste.length();
            while (fin > 0 && fm.stringWidth(reste.substring(0, fin)) > largeur) {
                fin = reste.lastIndexOf(' ', fin - 1);
                if (fin < 0) fin = 0;
            }

            if (fin == 0) {
                fin = reste.length();
                while (fin > 1 && fm.stringWidth(reste.substring(0, fin)) > largeur) fin--;
            }
            String morceau = reste.substring(0, fin).trim();
            g.drawString(morceau, x, y + ligne * 19);
            reste = reste.substring(Math.min(fin, reste.length())).trim();
        }
    }

    public void params(Graphics2D g, Session s) {
        Reglages reg = s.reg;
        boolean sombre = s.etatRetour != org.java.ressources.jeu.Etat.MENU;
        if (sombre) {
            g.setColor(new Color(60, 30, 80, 205));
            g.fillRect(0, 0, W, H);
        }
        g.setColor(sombre ? Color.WHITE : SOMBRE);
        g.setFont(F_TITRE);
        texteCentre(g, "Paramètres", W / 2, 105);
        g.setColor(new Color(255, 255, 255, sombre ? 35 : 110));
        g.fillRoundRect(W / 2 - 330, 110, 660, 420, 34, 34);

        String[] labels = {"Volume de la musique", "Volume des bruitages", "Affichage", "Contrôle de la licorne",
                "Secousses de l'écran", "Particules", "Pause auto (perte de focus)", "Mini-carte",
                "Images par seconde", "Effacer la progression", "Commandes et raccourcis"};
        Color rose = new Color(255, 130, 170);
        Color gris = new Color(175, 165, 200);

        for (int i = 0; i < NB_PARAMS; i++) {
            Rectangle r = Zones.ligneParam(i);
            boolean sel = i == s.paramSel;
            g.setColor(new Color(255, 255, 255, sel ? 245 : 205));
            g.fillRoundRect(r.x, r.y, r.width, r.height, 26, 26);
            g.setColor(sel ? rose : new Color(200, 190, 220));
            g.setStroke(new BasicStroke(sel ? 4 : 2));
            g.drawRoundRect(r.x, r.y, r.width, r.height, 26, 26);
            g.setColor(SOMBRE);
            g.setFont(F_MOYEN);
            if (i == NB_PARAMS - 1) {
                texteCentre(g, "Commandes et raccourcis  (Entrée)", r.x + r.width / 2, r.y + 25);
                continue;
            }
            g.drawString(labels[i], r.x + 18, r.y + 25);

            if (i < 2) {
                double v = i == 0 ? reg.volMusique : reg.volSfx;
                boolean muet = i == 0 && !reg.musique;
                int x0 = r.x + 300, w = 250, cy = r.y + 18;
                g.setColor(new Color(215, 205, 235));
                g.fillRoundRect(x0, cy - 5, w, 10, 10, 10);
                g.setColor(muet ? gris : rose);
                g.fillRoundRect(x0, cy - 5, (int) (w * v), 10, 10, 10);
                int kx = x0 + (int) (w * v);
                g.setColor(Color.WHITE);
                g.fillOval(kx - 10, cy - 10, 20, 20);
                g.setColor(muet ? gris : rose);
                g.setStroke(new BasicStroke(3));
                g.drawOval(kx - 10, cy - 10, 20, 20);
                g.setColor(SOMBRE);
                g.setFont(F_MOYEN);
                String t = muet ? "coupée" : Math.round(v * 100) + " %";
                FontMetrics fm = g.getFontMetrics();
                g.drawString(t, r.x + r.width - 16 - fm.stringWidth(t), r.y + 25);
            } else if (i == 2) {
                pilule(g, r, reg.pleinEcran ? "Plein écran" : "Fenêtre", rose);
            } else if (i == 3) {
                pilule(g, r, reg.modeSouris ? "Souris" : "Clavier", rose);
            } else if (i == 4) {
                pilule(g, r, reg.secousses ? "Activées" : "Désactivées", reg.secousses ? rose : gris);
            } else if (i == 5) {
                pilule(g, r, reg.effets ? "Complètes" : "Réduites", reg.effets ? rose : gris);
            } else if (i == 6) {
                pilule(g, r, reg.pauseAuto ? "Activée" : "Désactivée", reg.pauseAuto ? rose : gris);
            } else if (i == 7) {
                pilule(g, r, reg.miniCarte ? "Affichée" : "Cachée", reg.miniCarte ? rose : gris);
            } else if (i == 8) {
                pilule(g, r, reg.fps + " FPS", reg.fps == 60 ? rose : gris);
            } else {
                pilule(g, r, s.confirmReset ? "Confirmer ?" : "Effacer",
                        s.confirmReset ? new Color(235, 80, 100) : new Color(190, 150, 240));
            }
        }

        g.setColor(sombre ? Color.WHITE : SOMBRE);
        g.setFont(F_PETIT);
    }

    public void commandes(Graphics2D g, Session s) {
        g.setColor(new Color(60, 30, 80, 220));
        g.fillRect(0, 0, W, H);
        g.setColor(Color.WHITE);
        g.setFont(F_TITRE);
        texteCentre(g, "Commandes", W / 2, 72);
        g.setColor(new Color(255, 255, 255, 220));
        g.fillRoundRect(85, 100, 790, 440, 28, 28);
        g.setColor(SOMBRE);
        g.setFont(F_GRAND);
        texteCentre(g, "Pendant la partie", W / 2, 150);
        g.setFont(F_MOYEN);
        g.drawString("Z Q S D / flèches", 180, 198);
        texteAdapte(g, "Déplacer le personnage", 430, 198, 350, 22);
        g.drawString("Souris", 180, 240);
        texteAdapte(g, "Déplacer le personnage vers le curseur", 430, 240, 350, 22);
        g.drawString("E", 180, 282);
        texteAdapte(g, "Activer la compétence du personnage", 430, 282, 350, 22);
        g.drawString("P / Échap", 180, 324);
        texteAdapte(g, "Mettre en pause ou revenir", 430, 324, 350, 22);
        g.drawString("F11", 180, 366);
        texteAdapte(g, "Basculer le plein écran", 430, 366, 350, 22);
        g.setFont(F_PETIT);
        texteAdapte(g, "Dans les menus : Haut / Bas choisir, Gauche / Droite régler, Entrée valider.",
                145, 430, 670, 20);
        texteAdapte(g, "Échap : retour - C : choisir un personnage depuis le menu principal.",
                145, 462, 670, 20);
        texteAdapte(g, "L : ouvrir la coop LAN - dans le salon : H héberger, R rechercher.",
                145, 490, 670, 20);
        texteCentre(g, "Échap / Entrée : fermer", W / 2, 555);
    }

    public void multi(Graphics2D g, Session s) {
        g.setColor(SOMBRE);
        g.setFont(F_TITRE);
        texteCentre(g, "Coopération LAN", W / 2, 96);
        g.setFont(F_PETIT);
        texteCentre(g, "Même Wi-Fi  •  jusqu'à 4 joueurs  •  l'hôte lance la partie", W / 2, 122);
        g.setColor(new Color(255, 255, 255, 205));
        g.fillRoundRect(95, 145, 770, 320, 30, 30);
        g.setColor(SOMBRE);
        g.setFont(F_MOYEN);
        String role = s.reseau.estDansSalon()
                ? (s.reseau.role() == ReseauLan.Role.HOTE ? "Salon hébergé" : "Salon rejoint")
                : "Aucun salon";
        texteCentre(g, role, W / 2, 180);
        g.setFont(F_PETIT);
        if (!s.reseauMessage.isBlank()) {
            g.setColor(new Color(180, 70, 110));
            texteCentre(g, s.reseauMessage, W / 2, 207);
        }
        int i = 0;
        for (ReseauLan.Joueur joueur : s.reseau.joueurs()) {
            Rectangle r = Zones.partieLan(i++);
            g.setColor(joueur.id().equals(s.reseau.idLocal())
                    ? new Color(255, 235, 245) : new Color(248, 242, 255));
            g.fillRoundRect(r.x, r.y, r.width, r.height, 18, 18);
            g.setColor(SOMBRE);
            g.setFont(F_PETIT);
            g.drawString(joueur.nom(), r.x + 20, r.y + 27);
            g.setFont(F_PETIT);
            g.drawString("Personnage " + (joueur.personnage() + 1), r.x + 300, r.y + 27);
            g.drawString(joueur.pret() ? "PRÊT" : "EN ATTENTE", r.x + 570, r.y + 27);
        }
        if (i == 0 && !s.partiesLan.isEmpty()) {
            g.setFont(F_MOYEN);
            texteCentre(g, "Parties disponibles", W / 2, 235);
            for (int n = 0; n < s.partiesLan.size() && n < 4; n++) {
                Rectangle r = Zones.partieLan(n);
                ReseauLan.PartieTrouvee partie = s.partiesLan.get(n);
                g.setColor(new Color(248, 242, 255));
                g.fillRoundRect(r.x, r.y, r.width, r.height, 18, 18);
                g.setColor(SOMBRE);
                g.setFont(F_PETIT);
                g.drawString(partie.nom(), r.x + 20, r.y + 27);
                g.setFont(F_PETIT);
                g.drawString(partie.joueurs() + " / " + ReseauLan.MAX_JOUEURS + " joueurs", r.x + 350, r.y + 27);
                g.drawString("CLIQUER POUR REJOINDRE", r.x + 535, r.y + 27);
            }
        } else if (i == 0) {
            g.setColor(new Color(120, 100, 150));
            texteCentre(g, "Héberge une partie ou recherche les parties disponibles.", W / 2, 270);
        }
        if (s.reseau.estDansSalon() && s.reseau.role() == ReseauLan.Role.HOTE) {
            bouton(g, Zones.boutonLanLancer(), "LANCER LA COOP", new Color(255, 130, 170));
        } else if (!s.reseau.estDansSalon()) {
            bouton(g, Zones.boutonLanHeberger(), "Héberger", new Color(255, 130, 170));
            bouton(g, Zones.boutonLanRechercher(), "Rechercher", new Color(190, 150, 240));
        }
        bouton(g, Zones.boutonLanRetour(), "Quitter le salon", new Color(150, 170, 230));
        g.setColor(SOMBRE);
        g.setFont(F_PETIT);
        texteCentre(g, "La partie démarrera pour tous les joueurs quand l'hôte valide.", W / 2, 570);
    }

    private void texteAdapte(Graphics2D g, String texte, int x, int y, int largeur, int interligne) {
        FontMetrics fm = g.getFontMetrics();
        String reste = texte;
        int ligne = 0;
        while (!reste.isEmpty()) {
            int fin = reste.length();
            while (fin > 0 && fm.stringWidth(reste.substring(0, fin)) > largeur) {
                int espace = reste.lastIndexOf(' ', fin - 1);
                fin = espace >= 0 ? espace : fin - 1;
            }
            if (fin <= 0) break;
            g.drawString(reste.substring(0, fin).trim(), x, y + ligne++ * interligne);
            reste = reste.substring(fin).trim();
        }
    }

    public void personnages(Graphics2D g, Session s) {
        Progression pr = s.prog;
        String[] attaques = {
                "Étoiles qui tournent et blessent au contact.",
                "Flèches rapides en éventail, avec perforation.",
                "Petites bombes lentes qui explosent en zone.",
                "Lames projetées, puissantes et perforantes.",
                "Boomerangs qui traversent plusieurs ennemis.",
                "Rayons très rapides qui traversent les ennemis."
        };
        String[] competences = {
                "Nova : grosse onde de choc autour de la licorne.",
                "Pluie de flèches : douze flèches dans toutes les directions.",
                "Bouclier sucré : protection temporaire et explosion proche.",
                "Entaille astrale : salve de lames en arc frontal.",
                "Ronces tournoyantes : anneau de boomerangs autour de soi.",
                "Prisme explosif : six rayons puissants dans toutes les directions."
        };
        g.setColor(SOMBRE);
        g.setFont(F_TITRE);
        texteCentre(g, "Choisir un personnage", W / 2, 70);
        g.setFont(F_PETIT);
        texteCentre(g, "Chaque personnage a une attaque automatique et une compétence différentes.", W / 2, 98);
        for (int i = 0; i < Personnage.TOUS.length; i++) {
            Personnage p = Personnage.TOUS[i];
            Rectangle r = Zones.personnage(i);
            boolean selection = pr.personnageSel == i;
            boolean debloque = pr.personnageDebloque(i);
            g.setColor(debloque ? new Color(255, 255, 255, 225) : new Color(220, 210, 230, 170));
            g.fillRoundRect(r.x, r.y, r.width, r.height, 22, 22);
            g.setColor(selection ? new Color(255, 130, 170) : p.couleur);
            g.setStroke(new BasicStroke(selection ? 4 : 2));
            g.drawRoundRect(r.x, r.y, r.width, r.height, 22, 22);
            if (debloque) {
                AffineTransform old = g.getTransform();
                g.translate(r.x + 48, r.y + 70);
                g.scale(0.85, 0.85);
                Sprites.personnage(g, 0, 0, i, s.anim);
                g.setTransform(old);
            }
            g.setColor(SOMBRE);
            g.setFont(F_GRAND);
            g.drawString(debloque ? p.nom : "Verrouillée", r.x + 90, r.y + 32);
            g.setFont(F_MINI);
            if (debloque) {
                texteCarte(g, attaques[i], r.x + 90, r.y + 57, 285, 2);
                texteCarte(g, competences[i], r.x + 90, r.y + 87, 285, 2);
            } else {
                g.drawString("Débloque ce personnage pour l'utiliser.", r.x + 90, r.y + 64);
            }
        }
        g.setColor(SOMBRE);
        g.setFont(F_PETIT);
    }

    // ------------------------------------------------------------------
    // Menu principal / boutique
    // ------------------------------------------------------------------
    public void menu(Graphics2D g, Session s) {
        Progression pr = s.prog;
        int sel = s.mondeSel;
        Monde mo = Monde.TOUS[sel];
        g.setColor(SOMBRE);
        g.setFont(F_TITRE);
        texteCentre(g, "Licorne Survivor", W / 2, 112);

        // choix du monde
        g.setColor(new Color(255, 255, 255, 190));
        g.fillRoundRect(W / 2 - 300, 145, 600, 75, 28, 28);
        g.setColor(new Color(255, 130, 170));
        g.setStroke(new BasicStroke(3));
        g.drawRoundRect(W / 2 - 300, 145, 600, 75, 28, 28);
        g.setFont(F_GRAND);
        g.setColor(sel > 0 ? new Color(255, 130, 170) : new Color(200, 190, 215));
        texteCentre(g, "<", W / 2 - 272, 185);
        g.setColor(sel < pr.mondeMax ? new Color(255, 130, 170) : new Color(200, 190, 215));
        texteCentre(g, ">", W / 2 + 272, 185);
        g.setColor(SOMBRE);
        g.setFont(F_MOYEN);
        texteCentre(g, "Monde " + (sel + 1) + " : " + mo.nom, W / 2, 172);
        g.setFont(F_PETIT);
        String info = pr.gagne[sel] ? "Terminé !   Meilleur temps : " + chrono(pr.meilleur[sel])
                : pr.meilleur[sel] > 0 ? "Meilleur temps : " + chrono(pr.meilleur[sel])
                : "Boss : " + mo.boss;
        texteCentre(g, info + "   -   carte " + mo.largeur + " x " + mo.hauteur, W / 2, 194);
        if (sel == pr.mondeMax && pr.mondeMax < Monde.TOUS.length - 1) {
            g.setColor(new Color(120, 90, 150));
            g.setFont(F_MINI);
            texteCentre(g, "Bats le boss pour débloquer le monde suivant", W / 2, 213);
        }

        Personnage perso = Personnage.TOUS[pr.personnageSel];
        g.setColor(new Color(255, 255, 255, 215));
        g.fillRoundRect(180, 230, 600, 92, 24, 24);
        g.setColor(perso.couleur);
        g.setStroke(new BasicStroke(3));
        g.drawRoundRect(180, 230, 600, 92, 24, 24);
        AffineTransform old = g.getTransform();
        g.translate(240, 292);
        g.scale(0.62, 0.62);
        Sprites.personnage(g, 0, 0, perso.id, s.anim);
        g.setTransform(old);
        g.setColor(perso.couleur);
        g.setFont(F_MINI);
        g.drawString("LICORNE ACTIVE", 310, 252);
        g.setColor(SOMBRE);
        g.setFont(F_GRAND);
        g.drawString(perso.nom, 310, 280);
        g.setFont(F_PETIT);
        texteAdapte(g, "Attaque : " + perso.arme, 475, 265, 280, 17);
        texteAdapte(g, "Compétence : " + perso.competence, 475, 290, 280, 17);
        g.setFont(F_MINI);
        g.setColor(new Color(120, 100, 150));
        g.drawString("Changer de personnage en cliquant ici", 310, 308);

        g.setColor(new Color(255, 130, 170));
        g.setFont(F_GRAND);
        texteCentre(g, "Sucre : " + pr.sucre, W / 2, 360);

        String[] noms = {"Coeur de sucre  (+15 PV max)", "Corne affûtée  (+10% dégâts)", "Sabots de vent  (+5% vitesse)"};
        for (int i = 0; i < 3; i++) {
            Rectangle r = Zones.ligneBoutique(i);
            int niv = pr.niveauMeta(i);
            boolean max = niv >= Constantes.NIV_MAX;
            boolean possible = !max && pr.sucre >= pr.cout(niv);
            g.setColor(possible ? new Color(255, 255, 255, 230) : new Color(255, 255, 255, 145));
            g.fillRoundRect(r.x, r.y, r.width, r.height, 22, 22);
            g.setColor(possible ? new Color(180, 155, 220) : new Color(190, 180, 210));
            g.setStroke(new BasicStroke(2));
            g.drawRoundRect(r.x, r.y, r.width, r.height, 22, 22);
            g.setColor(SOMBRE);
            g.setFont(F_MOYEN);
            g.drawString("[" + (i + 1) + "]  " + noms[i], r.x + 18, r.y + 23);
            String droite = max ? "MAX" : "niv " + niv + "/" + Constantes.NIV_MAX + "  -  " + pr.cout(niv) + " sucre";
            FontMetrics fm = g.getFontMetrics();
            g.drawString(droite, r.x + r.width - 18 - fm.stringWidth(droite), r.y + 23);
        }

        bouton(g, Zones.boutonJouer(), "JOUER (Espace)", new Color(255, 130, 170));
        bouton(g, Zones.boutonSucces(), "Succès (S)  " + pr.succesOk.size() + "/" + Succes.TOUS.length, new Color(190, 150, 240));
        bouton(g, Zones.boutonParams(), "Paramètres (O)", new Color(150, 170, 230));
        bouton(g, Zones.boutonMultijoueur(), "Coop LAN (L)", new Color(160, 190, 240));
        bouton(g, Zones.boutonQuitter(), "Quitter", new Color(145, 125, 180));

        g.setColor(SOMBRE);
        g.setFont(F_PETIT);
    }
}
