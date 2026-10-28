package org.java.ressources.rendu;

import org.java.ressources.donnees.Monde;
import org.java.ressources.donnees.Pouvoir;
import org.java.ressources.donnees.Reglages;
import org.java.ressources.entites.Bonus;
import org.java.ressources.entites.Decor;
import org.java.ressources.entites.Ennemi;
import org.java.ressources.entites.Gemme;
import org.java.ressources.entites.Obstacle;
import org.java.ressources.entites.Particule;
import org.java.ressources.entites.Tir;
import org.java.ressources.entites.TypeArme;
import org.java.ressources.jeu.Carte;
import org.java.ressources.jeu.Etat;
import org.java.ressources.jeu.Partie;
import org.java.ressources.jeu.Session;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.AlphaComposite;
import java.awt.RadialGradientPaint;
import java.awt.Shape;
import java.awt.geom.AffineTransform;
import java.awt.geom.Point2D;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Random;

import static org.java.ressources.donnees.Constantes.H;
import static org.java.ressources.donnees.Constantes.W;
import static org.java.ressources.rendu.Dessin.etoile;
import static org.java.ressources.rendu.Dessin.ov;

/** Dessine le monde de jeu : sol, décors, obstacles, personnages, effets, lumière et ambiance. */
public class RenduMonde {
    private final Ambiance[] ambiances = new Ambiance[Monde.TOUS.length];
    private final List<Object> pile = new ArrayList<>();
    private final Random alea = new Random();
    private double derCamX, derCamY;
    private boolean camConnue;

    private static final Comparator<Object> PAR_Y = Comparator.comparingDouble(RenduMonde::cleY);
    private static final AffineTransform ID = new AffineTransform();

    // couleurs et tableaux réutilisés (évite de créer des objets à chaque image)
    private static final int TEINTES = 64;
    private static final Color[] TEINTE_GEMME = new Color[TEINTES], TEINTE_TIR = new Color[TEINTES];
    private static final Color TIR_BRILLANCE = new Color(255, 255, 255, 200);
    private static final Color ETOILE = new Color(255, 215, 80);
    private static final java.util.Map<Integer, Color[]> COULEURS_PARTICULES = new java.util.HashMap<>();
    private final int[] gemX = new int[4], gemY = new int[4];
    private static final int MAX_GEMMES_DESSINEES = 1200;

    static {
        for (int i = 0; i < TEINTES; i++) {
            TEINTE_GEMME[i] = Color.getHSBColor(i / (float) TEINTES, 0.45f, 1f);
            TEINTE_TIR[i] = Color.getHSBColor(i / (float) TEINTES, 0.5f, 1f);
        }
    }

    /** Couleur d'une particule pour une transparence donnée (16 niveaux, mis en cache). */
    private static Color couleurParticule(Color c, int alpha) {
        Color[] t = COULEURS_PARTICULES.computeIfAbsent(c.getRGB() & 0xffffff, k -> new Color[17]);
        int q = Math.clamp((alpha + 8) >> 4, 0, 16);
        Color r = t[q];
        if (r == null) r = t[q] = new Color(c.getRed(), c.getGreen(), c.getBlue(), Math.min(255, q * 16));
        return r;
    }

    private static int teinte(double v) {
        return Math.clamp((int) ((v - Math.floor(v)) * TEINTES), 0, TEINTES - 1);
    }

    // calque de lumière pré-dessiné (à la résolution de l'écran)
    private BufferedImage lumImg;
    private int lumMonde = -1, lumVersion = -1, lumCote;
    private int lumSourceCote;
    private Color lumBord;
    private static final double MAX_ECHELLE_LUMIERE = 1.5;

    /** Position verticale utilisée pour trier les éléments : ce qui est plus bas est devant. */
    private static double cleY(Object o) {
        if (o instanceof Obstacle) return ((Obstacle) o).y;
        if (o instanceof Ennemi) return ((Ennemi) o).y;
        if (o instanceof Partie.JoueurReseau) return ((Partie.JoueurReseau) o).y;
        return ((Partie) o).py;
    }

    public void dessiner(Graphics2D g, Session s) {
        Partie p = s.partie;
        Monde m = p.monde;
        Carte c = p.carte;
        Reglages reg = s.reg;
        double anim = s.anim;

        CacheRendu.debutImage(g);
        if (CacheRendu.memoirePleine()) CacheRendu.purger(c);

        Ambiance amb = ambiances[m.id];
        if (amb == null) amb = ambiances[m.id] = new Ambiance(m);
        double dcx = camConnue ? p.camX - derCamX : 0, dcy = camConnue ? p.camY - derCamY : 0;
        if (Math.abs(dcx) > 150 || Math.abs(dcy) > 150) {
            dcx = 0;
            dcy = 0;
        }
        derCamX = p.camX;
        derCamY = p.camY;
        camConnue = true;
        if (s.etat == Etat.JEU) amb.maj(dcx, dcy, anim);

        // hors de la carte
        g.setColor(m.bordure);
        g.fillRect(0, 0, W, H);

        double sx = p.shake > 0 && reg.secousses ? (alea.nextDouble() - 0.5) * p.shake : 0;
        double sy = p.shake > 0 && reg.secousses ? (alea.nextDouble() - 0.5) * p.shake : 0;
        int ox = (int) Math.round(-p.camX + sx), oy = (int) Math.round(-p.camY + sy);
        g.translate(ox, oy);
        CacheRendu.camera(g);

        double x0 = p.camX - 140, x1 = p.camX + W + 140, y0 = p.camY - 140, y1 = p.camY + H + 140;

        dessinerSol(g, m, p);
        Bordures.dessiner(g, m, p.camX, p.camY, false);

        for (Decor d : c.decors) {
            if (d.x > x0 && d.x < x1 && d.y > y0 && d.y < y1) CacheRendu.decor(g, d, anim);
        }
        for (Obstacle o : c.obstacles) { // zones plates : étangs, lave
            if (o.plat && o.x > x0 - 100 && o.x < x1 + 100 && o.y > y0 - 100 && o.y < y1 + 100) CacheRendu.obstacle(g, o, anim);
        }
        double echelleRendu = CacheRendu.echelle();
        int maxGemmesDessinees = Math.max(120, (int) (MAX_GEMMES_DESSINEES
                / Math.max(1, echelleRendu * echelleRendu)));
        int pasGemmes = Math.max(1, (p.gemmes.size() + maxGemmesDessinees - 1) / maxGemmesDessinees);
        int indexGemme = 0;
        for (Gemme ge : p.gemmes) {
            if (indexGemme++ % pasGemmes != 0) continue;
            if (ge.x < x0 || ge.x > x1 || ge.y < y0 || ge.y > y1) continue;
            g.setColor(TEINTE_GEMME[teinte(anim * 0.3 + ge.x * 0.002)]);
            int x = (int) ge.x, y = (int) ge.y;
            gemX[0] = x - 6;
            gemX[1] = x;
            gemX[2] = x + 6;
            gemX[3] = x;
            gemY[0] = y;
            gemY[1] = y - 8;
            gemY[2] = y;
            gemY[3] = y + 8;
            g.fillPolygon(gemX, gemY, 4);
            g.setColor(Color.WHITE);
            g.fillOval(x - 2, y - 4, 3, 3);
        }
        for (Bonus b : p.bonus) {
            if (b.vie < 3 && ((int) (b.vie * 6)) % 2 == 0) continue; // clignote avant de disparaître
            Pouvoir pv = Pouvoir.TOUS[b.type];
            int x = (int) b.x, y = (int) (b.y + Math.sin(anim * 4 + b.x) * 3);
            int halo = (int) (22 + 3 * Math.sin(anim * 6));
            g.setColor(new Color(pv.col.getRed(), pv.col.getGreen(), pv.col.getBlue(), 80));
            g.fillOval(x - halo, y - halo, 2 * halo, 2 * halo);
            RenduInterface.icone(g, b.type, x, y, anim);
        }

        // tout ce qui a de la hauteur est trié par position verticale, pour passer devant / derrière
        pile.clear();
        for (Obstacle o : c.obstacles) {
            if (!o.plat && o.x > x0 - 100 && o.x < x1 + 100 && o.y > y0 && o.y < y1 + 320) pile.add(o);
        }
        for (Ennemi e : p.ennemis) {
            if (e.x > x0 && e.x < x1 && e.y > y0 && e.y < y1) pile.add(e);
        }
        pile.addAll(p.joueursReseau);
        pile.add(p);
        pile.sort(PAR_Y);
        for (Object o : pile) {
            switch (o) {
                case Obstacle obstacle -> CacheRendu.obstacle(g, obstacle, anim);
                case Ennemi ennemi -> CacheRendu.ennemi(g, ennemi);
                case Partie.JoueurReseau joueurReseau -> dessinerJoueurReseau(g, joueurReseau, anim);
                case null, default -> dessinerJoueur(g, p, anim);
            }
        }
        Bordures.dessiner(g, m, p.camX, p.camY, true);

        if (p.personnage.typeArme == TypeArme.RAYON) {
            for (int i = 0; i < p.nbFaisceaux; i++) {
                if (p.faisceauOn[i]) {
                    EffetsAttaques.faisceau(g, p.px, p.py, p.faisceauAngle[i],
                            p.faisceauLongueur, p.temps);
                }
            }
        } else if (p.personnage.typeArme == TypeArme.EPEE && p.coupT > 0) {
            double progression = 1 - p.coupT / Partie.COUP_DUREE;
            for (int i = 0; i < p.coupNb; i++) {
                EffetsAttaques.coup(g, p.px, p.py, p.coupAngle[i], Partie.COUP_ARC,
                        p.coupPortee, progression);
            }
        }

        for (Tir t : p.tirs) {
            dessinerTir(g, t, anim, c.largeur, c.hauteur);
        }
        g.setColor(ETOILE);
        for (int i = 0; i < p.etoiles; i++) {
            double a = p.angleOrbite + i * 2 * Math.PI / p.etoiles;
            etoile(g, p.px + Math.cos(a) * 75, p.py + Math.sin(a) * 75, 13, 6, anim * 3);
        }

        if (p.novaT > 0) { // onde de la nova sucrée
            double prog = 1 - p.novaT / 0.6;
            int r = (int) (prog * Math.max(W, H) * 0.8);
            g.setStroke(new BasicStroke(8));
            g.setColor(new Color(255, 170, 90, (int) (200 * (1 - prog))));
            g.drawOval((int) p.novaX - r, (int) p.novaY - r, 2 * r, 2 * r);
        }
        for (Particule pt : p.particules) {
            int a = (int) (255 * Math.clamp(pt.vie / pt.vieMax, 0, 1));
            g.setColor(couleurParticule(pt.col, a));
            int sz = (int) pt.taille;
            g.fillOval((int) pt.x - sz / 2, (int) pt.y - sz / 2, sz, sz);
        }
        g.translate(-ox, -oy);

        // ambiance : particules d'avant-plan puis lumière
        amb.dessiner(g, anim);
        lumiere(g, m, p, ox, oy, anim);
    }

    private static void dessinerJoueurReseau(Graphics2D g, Partie.JoueurReseau joueur, double anim) {
        AffineTransform ancien = g.getTransform();
        g.setColor(new Color(0, 0, 0, 45));
        ov(g, joueur.x, joueur.y + 17, 17, 6);
        g.translate(joueur.x, joueur.y);
        g.scale(0.78, 0.78);
        Sprites.personnage(g, 0, 0, joueur.personnage.id, anim + joueur.attaqueT * 8);
        g.setTransform(ancien);
        if (joueur.attaqueT > 0) {
            double progression = 1 - joueur.attaqueT / 0.6;
            int rayon = 24 + (int) (18 * progression);
            int alpha = (int) (150 * Math.clamp(1 - progression, 0, 1));
            g.setColor(new Color(255, 235, 120, alpha));
            g.setStroke(new BasicStroke(3));
            g.drawOval((int) joueur.x - rayon, (int) joueur.y - rayon,
                    rayon * 2, rayon * 2);
        }
        g.setColor(new Color(255, 255, 255, 220));
        g.fillRoundRect((int) joueur.x - 18, (int) joueur.y - 30, 36, 4, 4, 4);
        g.setColor(new Color(120, 220, 170));
        g.fillRoundRect((int) joueur.x - 18, (int) joueur.y - 30,
                (int) (36 * Math.clamp(joueur.vie / 100, 0, 1)), 4, 4, 4);
    }

    private static void dessinerTir(Graphics2D g, Tir t, double anim, int largeurCarte, int hauteurCarte) {
        int x = (int) t.x, y = (int) t.y;
        double angle = Math.atan2(t.vy, t.vx);
        if (t.arme == TypeArme.FLECHE) {
            AffineTransform ancien = g.getTransform();
            g.translate(x, y);
            g.rotate(angle);
            g.setColor(new Color(90, 190, 255));
            g.fillPolygon(new int[]{-12, 8, 14, 8, -12}, new int[]{-3, -3, 0, 3, 3}, 5);
            g.setColor(Color.WHITE);
            g.fillPolygon(new int[]{-7, 7, 10, 7, -7}, new int[]{-1, -1, 0, 1, 1}, 5);
            g.setTransform(ancien);
        } else if (t.arme == TypeArme.BOMBE) {
            // La zone d'explosion ne doit pas être affichée pendant le vol :
            // elle serait coupée par les bords de la carte et donnerait un rayon déformé.
            g.setStroke(new BasicStroke(1));
            g.setColor(new Color(255, 150, 55));
            g.fillOval(x - 7, y - 7, 14, 14);
            g.setColor(new Color(255, 220, 110));
            g.fillOval(x - 4, y - 5, 6, 6);
            g.setColor(new Color(140, 65, 45));
            g.drawOval(x - 7, y - 7, 14, 14);
        } else if (t.arme == TypeArme.EPEE) {
            // Ne pas afficher une lame partiellement hors de la carte : le clip de
            // la carte coupe alors sa forme et donne l'impression qu'elle se déforme.
            if (t.x < 24 || t.x > largeurCarte - 24
                    || t.y < 24 || t.y > hauteurCarte - 24) return;
            AffineTransform ancien = g.getTransform();
            g.translate(x, y);
            g.rotate(angle);
            g.setStroke(new BasicStroke(1));
            g.setColor(new Color(175, 125, 255, 100));
            g.fillPolygon(new int[]{-18, 12, 22, 12, -18}, new int[]{-7, -4, 0, 4, 7}, 5);
            g.setColor(new Color(245, 235, 255));
            g.fillPolygon(new int[]{-15, 10, 17, 0, 10, -15}, new int[]{-3, -2, 0, 2, 3, 0}, 6);
            g.setColor(new Color(115, 75, 185));
            g.drawLine(-13, 0, 7, 0);
            g.setTransform(ancien);
        } else if (t.arme == TypeArme.BOOMERANG) {
            AffineTransform ancien = g.getTransform();
            g.translate(x, y);
            g.rotate(angle + anim * 5);
            g.setColor(new Color(120, 220, 150));
            g.setStroke(new BasicStroke(4));
            g.drawArc(-11, -9, 22, 18, 35, 110);
            g.setColor(new Color(220, 255, 190));
            g.drawArc(-7, -6, 14, 12, 35, 110);
            g.setTransform(ancien);
        } else if (t.arme == TypeArme.RAYON) {
            AffineTransform ancien = g.getTransform();
            g.translate(x, y);
            g.rotate(angle);
            g.setStroke(new BasicStroke(6, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g.setColor(new Color(255, 235, 100, 100));
            g.drawLine(-22, 0, 22, 0);
            g.setStroke(new BasicStroke(2));
            g.setColor(Color.WHITE);
            g.drawLine(-20, 0, 20, 0);
            g.setTransform(ancien);
        } else {
            g.setColor(TEINTE_TIR[teinte(anim * 0.8 + t.x * 0.004)]);
            etoile(g, x, y, 10, 4, anim * 4 + angle);
            g.setColor(TIR_BRILLANCE);
            g.fillOval(x - 2, y - 5, 4, 4);
        }
    }


    private void dessinerSol(Graphics2D g, Monde m, Partie p) {
        BufferedImage img = Sols.de(m);
        int t = Sols.T;
        Shape ancien = g.getClip();
        g.clipRect(0, 0, m.largeur, m.hauteur);
        int xDebut = (int) Math.floor(p.camX / t) * t, yDebut = (int) Math.floor(p.camY / t) * t;
        for (int y = yDebut; y < p.camY + H + t; y += t) {
            for (int x = xDebut; x < p.camX + W + t; x += t) g.drawImage(img, x, y, null);
        }
        g.setClip(ancien);
    }

    private void dessinerJoueur(Graphics2D g, Partie p, double anim) {
        g.setColor(new Color(0, 0, 0, 55));
        ov(g, p.px, p.py + 17, 21, 7);
        if (p.tBouclier > 0 && !(p.tBouclier < 1.5 && ((int) (p.tBouclier * 8)) % 2 == 0)) {
            g.setStroke(new BasicStroke(4));
            g.setColor(Color.getHSBColor((float) ((anim * 0.7) % 1.0), 0.5f, 1f));
            g.drawOval((int) p.px - 40, (int) p.py - 44, 80, 88);
            g.setColor(new Color(255, 255, 255, 60));
            g.fillOval((int) p.px - 40, (int) p.py - 44, 80, 88);
        }
        // la licorne clignote quand elle est touchée
        if (!(p.invuln > 0 && ((int) (p.invuln * 20)) % 2 == 0)) {
            Sprites.personnage(g, (int) p.px, (int) p.py, p.personnage.id, anim);
        }
    }

    /**
     * Vignette et lumière propres à chaque monde, dessinées par-dessus tout (repère de l'écran).
     * Le dégradé est coûteux à calculer : on le dessine une fois dans une image (à la résolution de l'écran)
     * et on se contente de la recopier à chaque frame.
     */
    private void lumiere(Graphics2D g, Monde m, Partie p, int ox, int oy, double anim) {
        double s = CacheRendu.echelle();
        AffineTransform base = g.getTransform();
        int ecW = (int) Math.round(W * s), ecH = (int) Math.round(H * s);
        int dx0 = (int) Math.round(base.getTranslateX()), dy0 = (int) Math.round(base.getTranslateY());
        if (lumImg == null || lumMonde != m.id || lumVersion != CacheRendu.version()) {
            creerLumiere(m, s, ecW, ecH);
        }
        g.setTransform(ID);
        if (m.id == 1) { // nuit : le disque de lumière suit la licorne
            int moitie = lumCote / 2;
            int ix0 = (int) Math.round(s * (p.px + ox) + base.getTranslateX()) - moitie;
            int iy0 = (int) Math.round(s * (p.py + oy - 10) + base.getTranslateY()) - moitie;
            int ix1 = ix0 + lumCote, iy1 = iy0 + lumCote, rx1 = dx0 + ecW, ry1 = dy0 + ecH;
            g.setColor(lumBord); // au-delà du disque, la couleur est uniforme
            if (iy0 > dy0) g.fillRect(dx0, dy0, ecW, Math.min(iy0, ry1) - dy0);
            if (iy1 < ry1) g.fillRect(dx0, Math.max(iy1, dy0), ecW, ry1 - Math.max(iy1, dy0));
            int haut = Math.max(iy0, dy0), bas = Math.min(iy1, ry1);
            if (bas > haut) {
                if (ix0 > dx0) g.fillRect(dx0, haut, Math.min(ix0, rx1) - dx0, bas - haut);
                if (ix1 < rx1) g.fillRect(Math.max(ix1, dx0), haut, rx1 - Math.max(ix1, dx0), bas - haut);
            }
            g.drawImage(lumImg, ix0, iy0, lumCote, lumCote, null);
        } else if (m.id == 2) { // chaleur : bords rouges qui pulsent
            double k = (115 + 22 * Math.sin(anim * 2)) / 137.0;
            java.awt.Composite ancien = g.getComposite();
            g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, (float) Math.clamp(k, 0, 1)));
            g.drawImage(lumImg, dx0, dy0, ecW, ecH, null);
            g.setComposite(ancien);
        } else {
            g.drawImage(lumImg, dx0, dy0, ecW, ecH, null);
        }
        g.setTransform(base);
        if (p.brulure > 0) { // flash orangé quand on se brûle
            g.setColor(new Color(255, 80, 0, (int) (70 * Math.min(1, p.brulure * 4))));
            g.fillRect(0, 0, W, H);
        }
    }

    /** Dessine le calque de lumière du monde dans une image à l'échelle de l'écran. */
    private void creerLumiere(Monde m, double s, int ecW, int ecH) {
        lumMonde = m.id;
        lumVersion = CacheRendu.version();
        float[] fr;
        Color[] co;
        if (m.id == 1) {
            // disque de rayon 430 autour de la licorne, avec le voile bleu nuit par-dessus
            fr = new float[]{0f, 0.4f, 1f};
            co = new Color[]{new Color(10, 10, 60, 0), new Color(12, 10, 60, 70), new Color(6, 6, 34, 175)};
            lumCote = 2 * (int) Math.ceil(430 * s);
            double imageScale = Math.min(s, MAX_ECHELLE_LUMIERE);
            lumSourceCote = 2 * (int) Math.ceil(430 * imageScale);
            lumImg = new BufferedImage(lumSourceCote, lumSourceCote, BufferedImage.TYPE_INT_ARGB);
            Graphics2D gg = lumImg.createGraphics();
            gg.setPaint(new RadialGradientPaint(new Point2D.Float(lumSourceCote / 2f, lumSourceCote / 2f),
                    (float) (430 * imageScale), fr, co));
            gg.fillRect(0, 0, lumSourceCote, lumSourceCote);
            gg.setColor(new Color(40, 30, 110, 34));
            gg.fillRect(0, 0, lumSourceCote, lumSourceCote);
            gg.dispose();
            lumBord = new Color(lumImg.getRGB(0, 0), true);
            return;
        }
        if (m.id == 0) { // doux et lumineux
            fr = new float[]{0.55f, 1f};
            co = new Color[]{new Color(255, 220, 240, 0), new Color(255, 170, 215, 90)};
        } else { // chaleur (dessinée à son intensité maximale, la pulsation se fait à l'affichage)
            fr = new float[]{0.45f, 1f};
            co = new Color[]{new Color(150, 30, 0, 0), new Color(150, 30, 0, 137)};
        }
        double imageScale = Math.min(s, MAX_ECHELLE_LUMIERE);
        int imageW = Math.max(1, (int) Math.round(W * imageScale));
        int imageH = Math.max(1, (int) Math.round(H * imageScale));
        lumImg = new BufferedImage(imageW, imageH, BufferedImage.TYPE_INT_ARGB);
        Graphics2D gg = lumImg.createGraphics();
        gg.scale(imageScale, imageScale);
        gg.setPaint(new RadialGradientPaint(new Point2D.Float(W / 2f, H / 2f), 600, fr, co));
        gg.fill(new java.awt.geom.Rectangle2D.Double(0, 0, W, H));
        gg.dispose();
        lumCote = 0;
        lumSourceCote = 0;
    }

    /** Mini-carte : le décor fixe est dessiné une seule fois dans une image. */
    static BufferedImage creerMini(Carte c, int mw, int mh) {
        Monde m = c.monde;
        BufferedImage img = new BufferedImage(mw, mh, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = img.createGraphics();
        double sc = mw / (double) c.largeur;
        g.setColor(Dessin.melange(m.sol, Color.BLACK, 0.25));
        g.fillRect(0, 0, mw, mh);
        g.setRenderingHint(java.awt.RenderingHints.KEY_ANTIALIASING, java.awt.RenderingHints.VALUE_ANTIALIAS_ON);
        for (Obstacle o : c.obstacles) {
            Color col;
            if (o.danger) col = new Color(255, 110, 40);
            else if (o.plat) col = new Color(80, 170, 210);
            else col = Dessin.melange(m.palette[0], Color.WHITE, 0.3);
            g.setColor(col);
            double r = Math.max(1.4, o.r * sc * (o.plat ? 1.0 : 1.5));
            ov(g, o.x * sc, o.y * sc, r, r);
        }
        g.dispose();
        return img;
    }
}