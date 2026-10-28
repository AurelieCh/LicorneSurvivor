package org.java.ressources.rendu;

import org.java.ressources.entites.Decor;
import org.java.ressources.entites.Ennemi;
import org.java.ressources.entites.Obstacle;
import org.java.ressources.jeu.Carte;

import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferInt;
import java.util.HashMap;
import java.util.Map;

import static org.java.ressources.rendu.Dessin.ov;

/**
 * Cache d'images du rendu.
 * <p>
 * Dessiner des dizaines de formes vectorielles antialiasées par objet et par image coûte très cher.
 * Or un décor, un obstacle ou un ennemi ne change (presque) jamais d'aspect : on le dessine donc UNE fois
 * dans une petite image, puis on se contente de recopier cette image à chaque frame (très rapide).
 * <ul>
 *   <li>Les images sont faites à la résolution réelle de l'écran (échelle de la fenêtre) : pas de flou en plein écran.
 *       Si la taille de la fenêtre change, tout est refait au fil de l'eau.</li>
 *   <li>Les objets qui pulsent (lueurs, lave...) sont détectés automatiquement (on les dessine à plusieurs
 *       instants et on compare) et simplement redessinés {@link #IMAGES_PAR_SEC} fois par seconde, de façon décalée.</li>
 *   <li>Un budget de temps par image évite les à-coups : ce qui n'est pas encore en cache est dessiné à l'ancienne.</li>
 * </ul>
 * Tout se passe sur le thread d'affichage de Swing : aucune synchronisation nécessaire.
 */
final class CacheRendu {
    private CacheRendu() {
    }

    /** Fréquence de rafraîchissement des objets animés (lueurs, lave...). */
    private static final double IMAGES_PAR_SEC = 8;
    /** Temps maximum passé à fabriquer des images pendant une frame (nanosecondes). */
    private static final long BUDGET_NS = 6_000_000L;
    /** Au-delà, on vide le cache des décors (mémoire). */
    private static final long MEMOIRE_MAX = 160L * 1024 * 1024;

    private static final AffineTransform ID = new AffineTransform();

    private static double echelle = 1;
    private static int version = 0;
    private static long deadline;
    private static long memoire = 0;
    private static AffineTransform courante = new AffineTransform();

    /** Une image pré-dessinée et sa position par rapport au point d'ancrage de l'objet (en pixels écran). */
    private static final class Sprite {
        BufferedImage img;
        int dx, dy;
        int version = -1;
        boolean anime;
        int etape = Integer.MIN_VALUE;
        // zone recopiée depuis la toile de travail (pour redessiner un objet animé sans rescanner)
        int bx, by, bw, bh;
        long octets;
    }

    private interface Trace {
        void dessiner(Graphics2D g, double anim);
    }

    // ------------------------------------------------------------------
    // Gestion de l'échelle et du budget
    // ------------------------------------------------------------------

    /** À appeler au début de chaque image, tant que g n'a que l'échelle de la fenêtre. */
    static void debutImage(Graphics2D g) {
        double s = g.getTransform().getScaleX();
        if (Math.abs(s - echelle) > 0.004) {
            echelle = s;
            invalider();
        }
        deadline = System.nanoTime() + BUDGET_NS;
    }

    /** À appeler après avoir placé la caméra (translate) : mémorise où dessiner les images. */
    static void camera(Graphics2D g) {
        courante = g.getTransform();
    }

    static double echelle() {
        return echelle;
    }

    static int version() {
        return version;
    }

    private static void invalider() {
        version++;
        memoire = 0;
        ENNEMIS.clear();
    }

    /** Oublie toutes les images de cette carte (décors et obstacles). */
    static void purger(Carte c) {
        for (Obstacle o : c.obstacles) o.cache = null;
        for (Decor d : c.decors) d.cache = null;
        memoire = 0;
        ENNEMIS.clear();
        version++;
    }

    static boolean memoirePleine() {
        return memoire > MEMOIRE_MAX;
    }

    private static boolean peutCuire() {
        return System.nanoTime() < deadline;
    }

    // ------------------------------------------------------------------
    // Toile de travail réutilisée pour dessiner puis recadrer chaque image
    // ------------------------------------------------------------------
    private static BufferedImage toile;

    private static BufferedImage toile(int w, int h) {
        if (toile == null || toile.getWidth() < w || toile.getHeight() < h) {
            toile = new BufferedImage(Math.max(w, toile == null ? 0 : toile.getWidth()),
                    Math.max(h, toile == null ? 0 : toile.getHeight()), BufferedImage.TYPE_INT_ARGB);
        }
        return toile;
    }

    /** Dessine l'objet sur la toile, son point d'ancrage étant en (ax, ay) pixels. */
    private static void tracer(Trace t, double ancreX, double ancreY, int ax, int ay, int cw, int ch, double anim) {
        Graphics2D g = toile.createGraphics();
        g.setComposite(AlphaComposite.Clear);
        g.fillRect(0, 0, cw, ch);
        g.setComposite(AlphaComposite.SrcOver);
        g.setClip(0, 0, cw, ch);
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.translate(ax, ay);
        g.scale(echelle, echelle);
        g.translate(-ancreX, -ancreY);
        t.dessiner(g, anim);
        g.dispose();
    }

    /** Cherche la zone non transparente de la toile. Renvoie {x0, y0, x1, y1, empreinte}. */
    private static long[] scanner(int cw, int ch) {
        int[] px = ((DataBufferInt) toile.getRaster().getDataBuffer()).getData();
        int stride = toile.getWidth();
        int x0 = cw, y0 = ch, x1 = -1, y1 = -1;
        long hash = 1125899906842597L;
        for (int y = 0; y < ch; y++) {
            int base = y * stride;
            for (int x = 0; x < cw; x++) {
                int p = px[base + x];
                if ((p >>> 24) != 0) {
                    if (x < x0) x0 = x;
                    if (x > x1) x1 = x;
                    if (y < y0) y0 = y;
                    if (y > y1) y1 = y;
                    hash = hash * 31 + p;
                }
            }
        }
        return new long[]{x0, y0, x1 + 1, y1 + 1, hash};
    }

    /**
     * Fabrique (ou met à jour) le sprite d'un objet.
     *
     * @param ancien     sprite existant (à redessiner sur place s'il est animé) ou null
     * @param detecter   true : on dessine à plusieurs instants pour savoir si l'objet bouge
     * @param demiL, haut, bas  étendue maximale de l'objet autour de son point d'ancrage (unités du jeu)
     */
    private static Sprite cuire(Trace t, double ancreX, double ancreY, double demiL, double haut, double bas,
                                double anim, Sprite ancien, boolean detecter) {
        double s = echelle;
        int ax = (int) Math.ceil(demiL * s) + 2, ay = (int) Math.ceil(haut * s) + 2;
        int cw = ax + (int) Math.ceil(demiL * s) + 2, ch = ay + (int) Math.ceil(bas * s) + 2;
        toile(cw, ch);

        // simple remise à jour d'un objet animé déjà connu : on redessine dans la même zone
        if (ancien != null && ancien.version == version && ancien.img != null && ancien.anime) {
            tracer(t, ancreX, ancreY, ax, ay, cw, ch, anim);
            Graphics2D gs = ancien.img.createGraphics();
            gs.setComposite(AlphaComposite.Src);
            gs.drawImage(toile, 0, 0, ancien.bw, ancien.bh, ancien.bx, ancien.by, ancien.bx + ancien.bw,
                    ancien.by + ancien.bh, null);
            gs.dispose();
            return ancien;
        }

        Sprite sp = new Sprite();
        long[] r;
        long h0 = 0;
        int nb = detecter ? 3 : 1;
        // le dernier tracé est à l'instant courant : c'est lui qu'on recopie dans l'image
        double[] instants = detecter ? new double[]{anim + 0.9, anim + 2.1, anim} : new double[]{anim};
        int ux0 = cw, uy0 = ch, ux1 = 0, uy1 = 0;
        for (int k = 0; k < nb; k++) {
            tracer(t, ancreX, ancreY, ax, ay, cw, ch, instants[k]);
            r = scanner(cw, ch);
            if (k == 0) h0 = r[4];
            else if (r[4] != h0) sp.anime = true;
            if (r[2] > r[0]) {
                ux0 = (int) Math.min(ux0, r[0]);
                uy0 = (int) Math.min(uy0, r[1]);
                ux1 = (int) Math.max(ux1, r[2]);
                uy1 = (int) Math.max(uy1, r[3]);
            }
        }
        sp.version = version;
        if (ux1 <= ux0 || uy1 <= uy0) { // rien à dessiner
            sp.img = null;
            return sp;
        }
        int marge = sp.anime ? (int) Math.ceil(5 * s) : 0;
        ux0 = Math.max(0, ux0 - marge);
        uy0 = Math.max(0, uy0 - marge);
        ux1 = Math.min(cw, ux1 + marge);
        uy1 = Math.min(ch, uy1 + marge);
        sp.bx = ux0;
        sp.by = uy0;
        sp.bw = ux1 - ux0;
        sp.bh = uy1 - uy0;
        sp.dx = ux0 - ax;
        sp.dy = uy0 - ay;
        sp.img = new BufferedImage(sp.bw, sp.bh, BufferedImage.TYPE_INT_ARGB);
        Graphics2D gs = sp.img.createGraphics();
        gs.setComposite(AlphaComposite.Src);
        gs.drawImage(toile, 0, 0, sp.bw, sp.bh, ux0, uy0, ux1, uy1, null);
        gs.dispose();
        sp.octets = 4L * sp.bw * sp.bh;
        memoire += sp.octets;
        return sp;
    }

    /** Recopie le sprite à l'écran, pixel pour pixel (sans repasser par l'échelle de la fenêtre). */
    private static void afficher(Graphics2D g, Sprite sp, double x, double y) {
        AffineTransform t = courante;
        int dx = (int) Math.round(t.getScaleX() * x + t.getTranslateX()) + sp.dx;
        int dy = (int) Math.round(t.getScaleY() * y + t.getTranslateY()) + sp.dy;
        g.setTransform(ID);
        g.drawImage(sp.img, dx, dy, null);
        g.setTransform(t);
    }

    private static int etape(double anim, double graine) {
        return (int) Math.floor((anim + graine / IMAGES_PAR_SEC) * IMAGES_PAR_SEC);
    }

    // ------------------------------------------------------------------
    // Obstacles
    // ------------------------------------------------------------------
    static void obstacle(Graphics2D g, Obstacle o, double anim) {
        Sprite sp = (Sprite) o.cache;
        boolean aFaire = sp == null || sp.version != version;
        if (!aFaire && sp.anime) {
            int et = etape(anim, o.graine);
            if (et != sp.etape && peutCuire()) {
                double u = o.echelle;
                sp = cuire((gg, an) -> Obstacles.dessiner(gg, o, an), o.x, o.y, 2.7 * u + 24, 5.7 * u + 24,
                        1.5 * u + 24, anim, sp, false);
                sp.etape = et;
            }
        } else if (aFaire) {
            if (!peutCuire()) { // pas le temps : on dessine directement, le cache se fera aux prochaines images
                Obstacles.dessiner(g, o, anim);
                return;
            }
            double u = o.echelle;
            sp = cuire((gg, an) -> Obstacles.dessiner(gg, o, an), o.x, o.y, 2.7 * u + 24, 5.7 * u + 24,
                    1.5 * u + 24, anim, null, true);
            sp.etape = etape(anim, o.graine);
            o.cache = sp;
        }
        if (sp.img != null) afficher(g, sp, o.x, o.y);
    }

    // ------------------------------------------------------------------
    // Décors
    // ------------------------------------------------------------------
    static void decor(Graphics2D g, Decor d, double anim) {
        Sprite sp = (Sprite) d.cache;
        boolean aFaire = sp == null || sp.version != version;
        double ancreX = Math.round(d.x), ancreY = Math.round(d.y);
        if (!aFaire && sp.anime) {
            int et = etape(anim, d.graine);
            if (et != sp.etape && peutCuire()) {
                double t = d.taille;
                sp = cuire((gg, an) -> Decors.dessiner(gg, d, an), ancreX, ancreY, 3.2 * t + 16, 2.6 * t + 16,
                        2.6 * t + 16, anim, sp, false);
                sp.etape = et;
            }
        } else if (aFaire) {
            if (!peutCuire()) {
                Decors.dessiner(g, d, anim);
                return;
            }
            double t = d.taille;
            sp = cuire((gg, an) -> Decors.dessiner(gg, d, an), ancreX, ancreY, 3.2 * t + 16, 2.6 * t + 16,
                    2.6 * t + 16, anim, null, true);
            sp.etape = etape(anim, d.graine);
            d.cache = sp;
        }
        if (sp.img != null) afficher(g, sp, ancreX, ancreY);
    }

    // ------------------------------------------------------------------
    // Ennemis (ombre comprise) : seulement quelques variantes (type x couleur x éclair blanc)
    // ------------------------------------------------------------------
    private static final Map<Long, Sprite> ENNEMIS = new HashMap<>();

    static void ennemi(Graphics2D g, Ennemi e) {
        long cle = ((long) e.col.getRGB() & 0xffffffL) << 20 | (long) e.monde << 16 | (long) e.type.ordinal() << 8 | (e.flash > 0 ? 1 : 0) << 4
                | (e.boss ? 1 : 0) | ((long) e.rayon << 32);
        Sprite sp = ENNEMIS.get(cle);
        if (sp == null) {
            if (!peutCuire()) { // pas le temps : à l'ancienne
                dessinerEnnemiDirect(g, e);
                return;
            }
            final Ennemi modele = new Ennemi();
            modele.x = 0;
            modele.y = 0;
            modele.type = e.type;
            modele.monde = e.monde;
            modele.boss = e.boss;
            modele.rayon = e.rayon;
            modele.col = e.col;
            modele.flash = e.flash;
            double r = e.rayon;
            sp = cuire((gg, an) -> dessinerEnnemiDirect(gg, modele), 0, 0, r + 16, 1.5 * r + 36, 1.2 * r + 20, 0, null,
                    false);
            ENNEMIS.put(cle, sp);
        }
        if (sp.img != null) afficher(g, sp, (int) e.x, (int) e.y);
    }

    static void dessinerEnnemiDirect(Graphics2D g, Ennemi e) {
        g.setColor(new Color(0, 0, 0, 50));
        ov(g, e.x, e.y + e.rayon * 0.75, e.rayon * 0.95, e.rayon * 0.32);
        Sprites.ennemi(g, e);
    }
}