package org.java.ressources.jeu;

import org.java.ressources.audio.Audio;
import org.java.ressources.donnees.Amelio;
import org.java.ressources.donnees.Constantes;
import org.java.ressources.donnees.Monde;
import org.java.ressources.donnees.Pouvoir;
import org.java.ressources.donnees.Personnage;
import org.java.ressources.donnees.Progression;
import org.java.ressources.donnees.Reglages;
import org.java.ressources.entites.Bonus;
import org.java.ressources.entites.Ennemi;
import org.java.ressources.entites.Gemme;
import org.java.ressources.entites.Particule;
import org.java.ressources.entites.Tir;
import org.java.ressources.entites.TypeEnnemi;
import org.java.ressources.entites.TypeArme;
import org.java.ressources.reseau.ReseauLan;

import java.awt.Color;
import java.awt.event.KeyEvent;
import java.util.*;

import static org.java.ressources.donnees.Constantes.DT;
import static org.java.ressources.donnees.Constantes.H;
import static org.java.ressources.donnees.Constantes.W;

/** La logique d'une partie : joueur, ennemis, tirs, pouvoirs, caméra. Aucun dessin ici. */
public class Partie {
    /** Ce qui s'est passé pendant la dernière mise à jour (lu par l'interface). */
    public enum Issue {AUCUNE, NIVEAU, FIN}

    static final double RAYON_JOUEUR = 16;

    private final Progression prog;
    private final Reglages reg;
    private final Audio audio;
    private final Random rnd = new Random();

    public Monde monde;
    public Carte carte;
    public int mondeJeu;
    public Personnage personnage = Personnage.TOUS[0];
    public Issue issue = Issue.AUCUNE;

    // joueur
    public double px, py, pvie, pvieMax, vitesse, degats, cadence, portee;
    public double tirTimer, invuln, angleOrbite, spawnTimer, shake, temps, brulure, competenceTimer;
    public double porteeArme, rayonExplosion, vitesseArme;
    public int projectiles, perforation, etoiles, niveau, xp, xpSuivant, kills;
    public boolean bossApparu, bossTue, degatsSubis;
    private final boolean[] coins = new boolean[4];

    /**
     * Réglages de l'attaque de base d'un personnage. TOUT l'équilibrage est dans le tableau PROFILS.
     * degats  : multiplicateur des dégâts de base
     * cadence : multiplicateur de la cadence de tir (plus petit = plus lent)
     * ecart   : angle (radians) entre deux projectiles d'une même salve
     * vitesse : vitesse du projectile (px/s)
     * portee  : distance maximale à laquelle le personnage commence à tirer (px)
     * tirs    : projectiles par salve (les améliorations « +1 projectile » s'y ajoutent)
     * perfo   : nombre d'ennemis traversés en plus du premier
     */
    private static final class Profil {
        final double degats, cadence, ecart, vitesse, portee;
        final int tirs, perfo;

        Profil(double degats, double cadence, double ecart, double vitesse, double portee, int tirs, int perfo) {
            this.degats = degats;
            this.cadence = cadence;
            this.ecart = ecart;
            this.vitesse = vitesse;
            this.portee = portee;
            this.tirs = tirs;
            this.perfo = perfo;
        }
    }

    private static final Profil[] PROFILS = {
            //          dégâts cadence écart vitesse portée tirs perfo
            new Profil(0.85, 1.00, 0.20, 460, 600, 1, 0),   // Lumi   : étoile filante téléguidée + 2 étoiles en orbite
            new Profil(0.60, 1.00, 0.13, 700, 640, 3, 1),   // Flèche : éventail de 3 flèches rapides, perforation 1
            new Profil(1.30, 0.55, 0.30, 360, 520, 1, 0),   // Boum   : bombe lente lobée, explose en zone
            new Profil(2.00, 0.75, 0.20, 520, 300, 1, 0),   // Lame   : dégâts d'un coup de mêlée, cadence
            new Profil(0.90, 0.60, 0.35, 540, 340, 1, 4),   // Ronce  : boomerang aller-retour, traverse beaucoup
            new Profil(1.00, 1.00, 0.10, 1100, 380, 1, 0)   // Prisme : (le faisceau a ses propres constantes)
    };

    /** Boomerangs qui ont dépassé leur point le plus éloigné et qui reviennent vers le joueur. */
    private final Set<Tir> boomerangsRetour = Collections.newSetFromMap(new java.util.IdentityHashMap<>());

    private final Set<Tir> rayonsScindes = Collections.newSetFromMap(new java.util.IdentityHashMap<>());

    // Lame : coup circulaire (lu par le rendu pour dessiner l'arc)
    public static final double COUP_DUREE = 0.22, COUP_ARC = 1.9; // arc ≈ 109°
    public double coupT, coupPortee;
    public final double[] coupAngle = new double[8];
    public int coupNb;

    // Prisme : faisceaux continus (lus par le rendu)
    private static final double FAISCEAU_LONGUEUR = 380, FAISCEAU_DPS = 1.2;
    public final double[] faisceauAngle = new double[8];
    public final boolean[] faisceauOn = new boolean[8];
    public int nbFaisceaux;
    public double faisceauLongueur = FAISCEAU_LONGUEUR;

    private Profil profil() {
        return PROFILS[Math.clamp(personnage.id, 0, PROFILS.length - 1)];
    }

    private Profil profil(Personnage personnage) {
        return PROFILS[Math.clamp(personnage.id, 0, PROFILS.length - 1)];
    }

    // pouvoirs en cours
    public double tBouclier, tTurbo, tXp, chanceBonus = 1;
    public double novaT, novaX, novaY;
    public int msgType;
    public double msgT;

    // caméra et souris (position de la souris dans le repère de l'écran)
    public double camX, camY, cibleX, cibleY;
    private double ciblePositionLocaleX, ciblePositionLocaleY;
    private boolean positionLocaleConnue;

    // résultat de la partie
    public int gainRun;
    public boolean victoire, nouveauMonde;

    public final List<Ennemi> ennemis = new ArrayList<>();
    public final List<Tir> tirs = new ArrayList<>();
    public final List<Gemme> gemmes = new ArrayList<>();
    public final List<Bonus> bonus = new ArrayList<>();
    public final List<Particule> particules = new ArrayList<>();
    public final List<JoueurReseau> joueursReseau = new ArrayList<>();
    public List<Amelio> choix = new ArrayList<>();

    /** Avatar d'un client piloté par l'hôte. Il ne possède pas de simulation locale complète. */
    public static final class JoueurReseau {
        public String id;
        public double x, y, vie = 100;
        public double cibleX, cibleY;
        public Personnage personnage = Personnage.TOUS[0];
        public boolean competence;
        public boolean competencePrecedente;
        public double tirTimer;
        public double attaqueT;

        public JoueurReseau(String id) {
            this.id = id;
        }

        public void definirPosition(double x, double y) {
            if (this.x == 0 && this.y == 0 && cibleX == 0 && cibleY == 0) {
                this.x = x;
                this.y = y;
            }
            cibleX = x;
            cibleY = y;
        }
    }

    public Partie(Progression prog, Reglages reg, Audio audio) {
        this.prog = prog;
        this.reg = reg;
        this.audio = audio;
        monde = Monde.TOUS[0];
    }

    // ------------------------------------------------------------------
    // Démarrage et fin
    // ------------------------------------------------------------------
    public void lancer(int idxMonde) {
        lancer(idxMonde, Personnage.TOUS[0]);
    }

    public void lancer(int idxMonde, Personnage choixPersonnage) {
        lancer(idxMonde, choixPersonnage, System.nanoTime());
    }

    public void lancer(int idxMonde, Personnage choixPersonnage, long seed) {
        mondeJeu = idxMonde;
        monde = Monde.TOUS[idxMonde];
        personnage = choixPersonnage;
        rnd.setSeed(seed);
        prog.enregistrerArme(personnage.typeArme);
        carte = Carte.generer(monde, rnd);
        prog.parties++;
        degatsSubis = false;
        nouveauMonde = false;
        issue = Issue.AUCUNE;
        px = carte.largeur / 2.0;
        py = carte.hauteur / 2.0;
        pvieMax = 100 + 15 * prog.metaVie;
        pvie = pvieMax;
        degats = 10 * (1 + 0.10 * prog.metaDegats);
        vitesse = 190 * (1 + 0.05 * prog.metaVitesse);
        cadence = 1.6;
        projectiles = 1;
        perforation = 0;
        portee = 90;
        porteeArme = 82;
        vitesseArme = 1;
        tirTimer = 0;
        competenceTimer = 0;
        invuln = 0;
        angleOrbite = 0;
        spawnTimer = 0.5;
        shake = 0;
        brulure = 0;
        temps = 0;
        niveau = 1;
        xp = 0;
        xpSuivant = 5;
        kills = 0;
        bossApparu = false;
        bossTue = false;
        tBouclier = tTurbo = tXp = 0;
        chanceBonus = 1;
        novaT = 0;
        msgT = 0;
        Arrays.fill(coins, false);
        ennemis.clear();
        tirs.clear();
        boomerangsRetour.clear();
        gemmes.clear();
        bonus.clear();
        particules.clear();
        choix = new ArrayList<>();
        joueursReseau.clear();
        rayonsScindes.clear();
        rayonExplosion = 56;
        etoiles = personnage.typeArme == TypeArme.ETOILE ? 2 : 0;
        coupT = 0;
        nbFaisceaux = 0;
        java.util.Arrays.fill(faisceauOn, false);
        positionLocaleConnue = false;
        majCamera(true);
    }

    /** Applique les entrées reçues des clients et déplace leurs avatars sur la carte de l'hôte. */
    public void majJoueursReseau(List<ReseauLan.Entree> entrees) {
        for (ReseauLan.Entree entree : entrees) {
            JoueurReseau joueur = null;
            for (JoueurReseau existant : joueursReseau) {
                if (existant.id.equals(entree.id())) {
                    joueur = existant;
                    break;
                }
            }
            if (joueur == null) {
                joueur = new JoueurReseau(entree.id());
                joueur.x = px;
                joueur.y = py;
                joueur.cibleX = px;
                joueur.cibleY = py;
                joueursReseau.add(joueur);
            }
            if (entree.personnage() >= 0 && entree.personnage() < Personnage.TOUS.length) {
                joueur.personnage = Personnage.TOUS[entree.personnage()];
            }
            double dx = 0, dy = 0;
            if ((entree.touches() & 1) != 0) dx--;
            if ((entree.touches() & 2) != 0) dx++;
            if ((entree.touches() & 4) != 0) dy--;
            if ((entree.touches() & 8) != 0) dy++;
            if (dx != 0 || dy != 0) {
                double n = Math.hypot(dx, dy);
                double vitesseClient = vitesse * (tTurbo > 0 ? 1.25 : 1);
                joueur.x += dx / n * vitesseClient * DT;
                joueur.y += dy / n * vitesseClient * DT;
                double[] corrige = carte.repousser(joueur.x, joueur.y, RAYON_JOUEUR);
                if (corrige != null) {
                    joueur.x = corrige[0];
                    joueur.y = corrige[1];
                }
            }
            joueur.cibleX = joueur.x;
            joueur.cibleY = joueur.y;
            joueur.competencePrecedente = joueur.competence;
            joueur.competence = entree.competence();
            joueur.attaqueT = Math.max(0, joueur.attaqueT - DT);
            joueur.tirTimer -= DT;
            if (joueur.tirTimer <= 0) {
                Ennemi cible = plusProche(joueur.x, joueur.y, 640);
                if (cible != null) {
                    tirerDepuis(joueur.x, joueur.y, joueur.personnage, cible);
                    joueur.attaqueT = 0.35;
                    Profil pf = profil(joueur.personnage);
                    joueur.tirTimer = 1.0 / (cadence * pf.cadence * (tTurbo > 0 ? 1.8 : 1.0));
                }
            }
            if (joueur.competence && !joueur.competencePrecedente) {
                for (Ennemi ennemi : ennemis) {
                    if (Math.hypot(ennemi.x - joueur.x, ennemi.y - joueur.y) < 180) {
                        blesser(ennemi, degats * profil(joueur.personnage).degats * 0.35);
                    }
                }
                joueur.attaqueT = 0.6;
            }
        }
    }

    public String serialiserJoueursReseau() {
        StringBuilder b = new StringBuilder("P:");
        b.append("HOST,").append(Math.round(px)).append(',').append(Math.round(py)).append(',')
                .append(Math.round(pvie)).append(',').append(personnage.id);
        for (JoueurReseau joueur : joueursReseau) {
            b.append(';').append(joueur.id).append(',').append(Math.round(joueur.x)).append(',')
                    .append(Math.round(joueur.y)).append(',').append(Math.round(joueur.vie))
                    .append(',').append(joueur.personnage.id).append(',')
                    .append(Math.round(joueur.attaqueT * 100)).append(',').append(joueur.competence);
        }
        b.append("|E:");
        for (int i = 0; i < ennemis.size(); i++) {
            if (i > 0) b.append(';');
            Ennemi e = ennemis.get(i);
            b.append(Math.round(e.x)).append(',').append(Math.round(e.y)).append(',')
                    .append(Math.round(e.pv)).append(',').append(e.type.ordinal()).append(',').append(e.boss);
        }
        b.append("|T:");
        for (int i = 0; i < tirs.size(); i++) {
            if (i > 0) b.append(';');
            Tir t = tirs.get(i);
            b.append(Math.round(t.x)).append(',').append(Math.round(t.y)).append(',')
                    .append(Math.round(t.vx)).append(',').append(Math.round(t.vy)).append(',')
                    .append(Math.round(t.vie * 100)).append(',').append(t.arme.ordinal()).append(',')
                    .append(Math.round(t.rayonExplosion));
        }
        return b.toString();
    }

    public void appliquerJoueursReseau(String texte) {
        appliquerJoueursReseau(texte, "");
    }

    public void appliquerJoueursReseau(String texte, String idLocal) {
        if (texte == null || texte.isBlank()) return;
        String[] sections = texte.split("\\|", -1);
        String joueurs = sections.length > 0 && sections[0].startsWith("P:")
                ? sections[0].substring(2) : sections[0];
        java.util.Set<String> presents = new java.util.HashSet<>();
        for (String ligne : joueurs.split(";")) {
            String[] champs = ligne.split(",");
            if (champs.length < 5) continue;
            try {
                String idJoueur = champs[0];
                double x = Double.parseDouble(champs[1]);
                double y = Double.parseDouble(champs[2]);
                double vie = Double.parseDouble(champs[3]);
                int id = Integer.parseInt(champs[4]);
                presents.add(idJoueur);
                if (idJoueur.equals("HOST") || !idJoueur.equals(idLocal)) {
                    JoueurReseau joueur = joueurReseau(idJoueur);
                    if (joueur == null) {
                        joueur = new JoueurReseau(idJoueur);
                        joueur.x = x;
                        joueur.y = y;
                        joueursReseau.add(joueur);
                    }
                    joueur.definirPosition(x, y);
                    joueur.vie = vie;
                    if (id >= 0 && id < Personnage.TOUS.length) joueur.personnage = Personnage.TOUS[id];
                    if (champs.length >= 7) {
                        joueur.attaqueT = Double.parseDouble(champs[5]) / 100;
                        joueur.competence = champs.length >= 8 && Boolean.parseBoolean(champs[6]);
                    }
                } else {
                    if (!positionLocaleConnue) {
                        px = x;
                        py = y;
                        positionLocaleConnue = true;
                    }
                    ciblePositionLocaleX = x;
                    ciblePositionLocaleY = y;
                    pvie = vie;
                }
            } catch (NumberFormatException ignored) {
                // Un avatar corrompu n'empêche pas les autres de rester visibles.
            }
        }
        joueursReseau.removeIf(joueur -> !presents.contains(joueur.id));
        if (sections.length > 1 && sections[1].startsWith("E:")) {
            ennemis.clear();
            String donnees = sections[1].substring(2);
            for (String ligne : donnees.split(";")) {
                String[] champs = ligne.split(",");
                if (champs.length < 5) continue;
                try {
                    Ennemi e = new Ennemi();
                    e.x = Double.parseDouble(champs[0]);
                    e.y = Double.parseDouble(champs[1]);
                    e.pv = Double.parseDouble(champs[2]);
                    e.pvMax = e.pv;
                    e.type = TypeEnnemi.depuisId(Integer.parseInt(champs[3]));
                    e.boss = Boolean.parseBoolean(champs[4]);
                    e.rayon = e.boss ? 48 : 16;
                    e.col = e.boss ? monde.bossCol : monde.palette[0];
                    ennemis.add(e);
                } catch (NumberFormatException ignored) {
                    // Une entité invalide est ignorée sans interrompre la synchronisation.
                }
            }
        }
        tirs.clear();
        if (sections.length > 2 && sections[2].startsWith("T:")) {
            String donnees = sections[2].substring(2);
            for (String ligne : donnees.split(";")) {
                String[] champs = ligne.split(",");
                if (champs.length < 7) continue;
                try {
                    Tir t = new Tir();
                    t.x = Double.parseDouble(champs[0]);
                    t.y = Double.parseDouble(champs[1]);
                    t.vx = Double.parseDouble(champs[2]);
                    t.vy = Double.parseDouble(champs[3]);
                    t.vie = Double.parseDouble(champs[4]) / 100;
                    int arme = Integer.parseInt(champs[5]);
                    if (arme < 0 || arme >= TypeArme.values().length) continue;
                    t.arme = TypeArme.values()[arme];
                    t.rayonExplosion = Double.parseDouble(champs[6]);
                    tirs.add(t);
                } catch (NumberFormatException ignored) {
                    // Un projectile invalide n'empêche pas les autres d'être affichés.
                }
            }
        }
    }

    private JoueurReseau joueurReseau(String id) {
        for (JoueurReseau joueur : joueursReseau) {
            if (joueur.id.equals(id)) return joueur;
        }
        return null;
    }

    /** L'hôte reste autoritaire, mais les états reçus par le client sont affichés sans saut. */
    public void lisserJoueursReseau() {
        if (positionLocaleConnue) {
            double ecartX = ciblePositionLocaleX - px;
            double ecartY = ciblePositionLocaleY - py;
            if (Math.hypot(ecartX, ecartY) > 180) {
                px = ciblePositionLocaleX;
                py = ciblePositionLocaleY;
            } else {
                px += ecartX * 0.25;
                py += ecartY * 0.25;
            }
        }
        for (JoueurReseau joueur : joueursReseau) {
            double ecartX = joueur.cibleX - joueur.x;
            double ecartY = joueur.cibleY - joueur.y;
            if (Math.hypot(ecartX, ecartY) > 180) {
                joueur.x = joueur.cibleX;
                joueur.y = joueur.cibleY;
            } else {
                joueur.x += ecartX * 0.25;
                joueur.y += ecartY * 0.25;
            }
        }
    }

    /** Termine la partie (mort, victoire ou abandon) et distribue le sucre. */
    public void terminer(boolean gagneLaPartie) {
        victoire = gagneLaPartie;
        gainRun = kills / 2 + (int) (temps / 10) + (gagneLaPartie ? 100 * (mondeJeu + 1) : 0);
        prog.sucre += gainRun;
        prog.totalSucre += gainRun;
        prog.meilleur[mondeJeu] = Math.max(prog.meilleur[mondeJeu], temps);
        if (gagneLaPartie) {
            prog.gagne[mondeJeu] = true;
            prog.debloquer("boss" + (mondeJeu + 1));
            if (!degatsSubis) prog.debloquer("intouchable");
            if (mondeJeu == prog.mondeMax && prog.mondeMax < Monde.TOUS.length - 1) {
                prog.mondeMax++;
                nouveauMonde = true;
            }
            audio.victoire();
        } else {
            audio.defaite();
        }
        explosion(px, py, 40, Color.WHITE);
        issue = Issue.FIN;
        prog.verifierGlobal();
    }

    // ------------------------------------------------------------------
    // Mise à jour (60 fois par seconde)
    // ------------------------------------------------------------------
    public void maj(Set<Integer> touches) {
        temps += DT;
        shake = Math.max(0, shake - 30 * DT);
        invuln = Math.max(0, invuln - DT);
        tBouclier = Math.max(0, tBouclier - DT);
        tTurbo = Math.max(0, tTurbo - DT);
        tXp = Math.max(0, tXp - DT);
        novaT = Math.max(0, novaT - DT);
        msgT = Math.max(0, msgT - DT);
        brulure = Math.max(0, brulure - DT);
        competenceTimer = Math.max(0, competenceTimer - DT);

        deplacerJoueur(touches);

        // attaque automatique propre à chaque personnage (voir PROFILS)
        attaqueDeBase();

        majTirs();
        majEtoiles();
        majApparitions();
        if (!majEnnemis()) return; // la partie vient de se terminer
        majBonus();
        majGemmes();
        verifierSucces();
        majCamera(false);
        if (xpSuivant > 0 && xp >= xpSuivant) niveauSup();
    }

    /** Attaque automatique : chaque personnage a sa propre mécanique. */
    private void attaqueDeBase() {
        Profil pf = profil();
        TypeArme arme = personnage.typeArme;
        coupT = Math.max(0, coupT - DT);

        if (arme == TypeArme.RAYON) { // Prisme : faisceau continu, pas de cadence
            majFaisceaux();
            return;
        }
        tirTimer -= DT;
        if (tirTimer > 0) return;

        Ennemi cible = plusProche(arme == TypeArme.EPEE ? porteeCoup() + 30 : pf.portee);
        if (cible == null) return;

        if (arme == TypeArme.EPEE) coupCirculaire(cible);
        else if (arme == TypeArme.FLECHE) volee();
        else tirer(cible);
        tirTimer = 1.0 / (cadence * pf.cadence * (tTurbo > 0 ? 1.8 : 1.0));
    }

    /** Les n ennemis les plus proches, du plus proche au plus lointain. */
    private List<Ennemi> plusProches(double x, double y, double distMax, int n) {
        List<Ennemi> res = new ArrayList<>();
        for (Ennemi e : ennemis) {
            if (Math.hypot(e.x - x, e.y - y) < distMax) res.add(e);
        }
        res.sort(Comparator.comparingDouble(a -> Math.hypot(a.x - x, a.y - y)));
        return res.size() > n ? new ArrayList<>(res.subList(0, n)) : res;
    }

    // ---- Flèche : volée sur plusieurs cibles ---------------------------------------

    private void volee() {
        Profil pf = profil();
        int nb = projectiles + pf.tirs - 1;
        List<Ennemi> cibles = plusProches(px, py, pf.portee, nb);
        if (cibles.isEmpty()) return;
        double vit = pf.vitesse * vitesseArme;
        for (int i = 0; i < nb; i++) {
            double a;
            if (i < cibles.size()) {
                Ennemi e = cibles.get(i);
                a = Math.atan2(e.y - py, e.x - px);
            } else { // plus de flèches que d'ennemis : le surplus s'ouvre en éventail autour de la 1re cible
                int j = i - cibles.size();
                Ennemi e = cibles.getFirst();
                a = Math.atan2(e.y - py, e.x - px) + (j % 2 == 0 ? 1 : -1) * ((double) j / 2 + 1) * pf.ecart;
            }
            Tir t = new Tir();
            t.x = px;
            t.y = py;
            t.vx = Math.cos(a) * vit;
            t.vy = Math.sin(a) * vit;
            t.degats = degats * pf.degats;
            t.perfo = perforation + pf.perfo;
            t.vie = 1.0;
            t.arme = TypeArme.FLECHE;
            tirs.add(t);
        }
        audio.tir();
    }

    // ---- Lame : coup de mêlée circulaire -------------------------------------------

    private double porteeCoup() {
        return porteeArme + 13 + 6 * perforation; // « Lame longue » et « perçant » augmentent la portée
    }

    private void coupCirculaire(Ennemi cible) {
        Profil pf = profil();
        double base = Math.atan2(cible.y - py, cible.x - px);
        coupNb = Math.min(8, projectiles); // « +1 projectile » = un coup de plus, réparti autour d'elle
        coupPortee = porteeCoup();
        for (int i = 0; i < coupNb; i++) coupAngle[i] = base + i * 2 * Math.PI / coupNb;
        coupT = COUP_DUREE;
        double dmg = degats * pf.degats;
        for (Ennemi en : ennemis) {
            double dx = en.x - px, dy = en.y - py, d = Math.hypot(dx, dy);
            if (d > coupPortee + en.rayon) continue;
            double ang = Math.atan2(dy, dx);
            boolean touche = false;
            for (int i = 0; i < coupNb && !touche; i++) {
                double diff = Math.atan2(Math.sin(ang - coupAngle[i]), Math.cos(ang - coupAngle[i]));
                touche = Math.abs(diff) <= COUP_ARC / 2;
            }
            if (!touche) continue;
            blesser(en, dmg);
            if (!en.boss && d > 1) { // petit recul : le coup repousse
                en.x += dx / d * 14;
                en.y += dy / d * 14;
            }
            explosion(en.x, en.y, 3, Color.WHITE);
        }
        shake = Math.max(shake, 2);
        audio.tir();
    }

    // ---- Prisme : faisceaux continus -----------------------------------------------

    private void majFaisceaux() {
        nbFaisceaux = Math.min(8, projectiles); // « +1 projectile » = un faisceau de plus, sur une autre cible
        faisceauLongueur = FAISCEAU_LONGUEUR + 45 * perforation;
        List<Ennemi> cibles = plusProches(px, py, faisceauLongueur, nbFaisceaux);
        double dps = degats * FAISCEAU_DPS * (cadence / 1.6) * (tTurbo > 0 ? 1.8 : 1.0);
        for (int i = 0; i < nbFaisceaux; i++) {
            if (i >= cibles.size()) {
                faisceauOn[i] = false;
                continue;
            }
            Ennemi c = cibles.get(i);
            double voulu = Math.atan2(c.y - py, c.x - px);
            if (!faisceauOn[i]) {
                faisceauAngle[i] = voulu;
            } else { // le faisceau balaie vers sa cible au lieu de sauter
                double diff = Math.atan2(Math.sin(voulu - faisceauAngle[i]), Math.cos(voulu - faisceauAngle[i]));
                faisceauAngle[i] += Math.clamp(diff, -7 * DT, 7 * DT);
            }
            faisceauOn[i] = true;
            double ca = Math.cos(faisceauAngle[i]), sa = Math.sin(faisceauAngle[i]);
            for (Ennemi en : ennemis) {
                double dx = en.x - px, dy = en.y - py;
                double along = dx * ca + dy * sa;
                if (along < 0 || along > faisceauLongueur + en.rayon) continue;
                if (Math.abs(-dx * sa + dy * ca) < en.rayon + 5) {
                    blesser(en, dps * DT);
                    if (rnd.nextInt(8) == 0) explosion(en.x, en.y, 1, Color.getHSBColor(rnd.nextFloat(), 0.5f, 1f));
                }
            }
        }
        for (int i = nbFaisceaux; i < faisceauOn.length; i++) faisceauOn[i] = false;
    }

    private void deplacerJoueur(Set<Integer> touches) {
        double vit = vitesse * (tTurbo > 0 ? 1.25 : 1.0);
        double nx = px, ny = py;
        if (reg.modeSouris) {
            double ddx = cibleX + camX - px, ddy = cibleY + camY - py;
            double d = Math.hypot(ddx, ddy);
            if (d > 3) {
                double pas = Math.min(d, vit * DT);
                nx += ddx / d * pas;
                ny += ddy / d * pas;
            }
        } else {
            double dx = 0, dy = 0;
            if (touches.contains(KeyEvent.VK_Q) || touches.contains(KeyEvent.VK_A) || touches.contains(KeyEvent.VK_LEFT)) dx -= 1;
            if (touches.contains(KeyEvent.VK_D) || touches.contains(KeyEvent.VK_RIGHT)) dx += 1;
            if (touches.contains(KeyEvent.VK_Z) || touches.contains(KeyEvent.VK_W) || touches.contains(KeyEvent.VK_UP)) dy -= 1;
            if (touches.contains(KeyEvent.VK_S) || touches.contains(KeyEvent.VK_DOWN)) dy += 1;
            if (dx != 0 || dy != 0) {
                double n = Math.hypot(dx, dy);
                nx += dx / n * vit * DT;
                ny += dy / n * vit * DT;
            }
        }
        double[] c = carte.repousser(nx, ny, RAYON_JOUEUR);
        if (c != null) {
            nx = c[0];
            ny = c[1];
        }
        px = nx;
        py = ny;

        // la lave brûle (sauf sous le bouclier)
        if (carte.dansDanger(px, py) && tBouclier <= 0) {
            pvie -= 14 * DT;
            degatsSubis = true;
            brulure = 0.25;
            if (rnd.nextDouble() < 0.35) explosion(px + rnd.nextGaussian() * 8, py + 8, 1, new Color(255, 150, 40));
        }

        // « Tour du monde » : toucher les 4 coins de la carte
        double z = 220;
        if (px < z && py < z) coins[0] = true;
        if (px > carte.largeur - z && py < z) coins[1] = true;
        if (px < z && py > carte.hauteur - z) coins[2] = true;
        if (px > carte.largeur - z && py > carte.hauteur - z) coins[3] = true;
    }

    private void majTirs() {
        for (int i = tirs.size() - 1; i >= 0; i--) {
            Tir t = tirs.get(i);
            guider(t);
            t.x += t.vx * DT;
            t.y += t.vy * DT;
            t.vie -= DT;
            boolean retirer = t.x < -20 || t.x > carte.largeur + 20 || t.y < -20 || t.y > carte.hauteur + 20;
            if (t.vie <= 0) {
                if (t.rayonExplosion > 0) exploser(t); // une bombe explose aussi là où elle atterrit
                retirer = true;
            }
            // un boomerang qui est revenu au joueur disparaît
            if (!retirer && t.arme == TypeArme.BOOMERANG && boomerangsRetour.contains(t)
                    && Math.hypot(px - t.x, py - t.y) < 22) {
                retirer = true;
            }
            if (!retirer) {
                for (Ennemi en : ennemis) {
                    if (t.touches.contains(en)) continue;
                    if (Math.hypot(en.x - t.x, en.y - t.y) < en.rayon + 7) {
                        if (t.rayonExplosion > 0) {
                            exploser(t);
                            retirer = true;
                            break;
                        }
                        blesser(en, t.degats);
                        t.touches.add(en);
                        explosion(t.x, t.y, 3, Color.WHITE);
                        if (t.arme == TypeArme.RAYON && t.perfo < 50 && !rayonsScindes.contains(t)) {
                            scinder(t, en);
                        }
                        t.perfo--;
                        if (t.perfo < 0) {
                            retirer = true;
                            break;
                        }
                    }
                }
            }
            if (retirer) {
                boomerangsRetour.remove(t);
                rayonsScindes.remove(t);
                tirs.remove(i);
            }
        }
    }

    private void scinder(Tir parent, Ennemi touche) {
        rayonsScindes.add(parent);
        double base = Math.atan2(parent.vy, parent.vx);
        for (int k = -1; k <= 1; k++) {
            double a = base + k * 0.5;
            Tir f = new Tir();
            f.x = parent.x;
            f.y = parent.y;
            f.vx = Math.cos(a) * 900;
            f.vy = Math.sin(a) * 900;
            f.degats = parent.degats * 0.6;
            f.perfo = 1;
            f.vie = 0.4;
            f.arme = TypeArme.RAYON;
            f.touches.add(touche);
            rayonsScindes.add(f);
            tirs.add(f);
            explosion(parent.x, parent.y, 4, Color.getHSBColor((k + 1) / 3f, 0.5f, 1f));
        }
    }

    /** Comportement propre à certaines armes : étoile téléguidée de Lumi, aller-retour du boomerang de Ronce. */
    private void guider(Tir t) {
        if (t.arme == TypeArme.ETOILE) {
            Ennemi cible = plusProche(t.x, t.y, 280);
            if (cible != null) {
                double v = Math.hypot(t.vx, t.vy);
                double cap = Math.atan2(t.vy, t.vx);
                double voulu = Math.atan2(cible.y - t.y, cible.x - t.x);
                double diff = Math.atan2(Math.sin(voulu - cap), Math.cos(voulu - cap));
                cap += Math.clamp(diff, -4.5 * DT, 4.5 * DT);
                t.vx = Math.cos(cap) * v;
                t.vy = Math.sin(cap) * v;
            }
        } else if (t.arme == TypeArme.BOOMERANG) {
            // le joueur « attire » le boomerang : il freine, fait demi-tour puis revient
            double dx = px - t.x, dy = py - t.y, d = Math.hypot(dx, dy);
            double acc = 520 * vitesseArme * vitesseArme;
            if (d > 1) {
                t.vx += dx / d * acc * DT;
                t.vy += dy / d * acc * DT;
            }
            // au demi-tour, il peut retoucher les mêmes ennemis (et récupère un peu de perforation)
            if (!boomerangsRetour.contains(t) && (t.x - px) * t.vx + (t.y - py) * t.vy < 0) {
                boomerangsRetour.add(t);
                t.touches.clear();
                t.perfo += 3;
            }
        }
    }

    /** Explosion de zone d'une bombe. */
    private void exploser(Tir t) {
        for (Ennemi c : ennemis) {
            if (Math.hypot(c.x - t.x, c.y - t.y) <= t.rayonExplosion + c.rayon * 0.5) blesser(c, t.degats);
        }
        explosion(t.x, t.y, 22, new Color(255, 190, 90));
        shake = Math.max(shake, 3);
    }

    private void majEtoiles() {
        angleOrbite += 3.2 * DT;
        for (int i = 0; i < etoiles; i++) {
            double a = angleOrbite + i * 2 * Math.PI / etoiles;
            double sx = px + Math.cos(a) * 75, sy = py + Math.sin(a) * 75;
            for (Ennemi en : ennemis) {
                if (Math.hypot(en.x - sx, en.y - sy) < en.rayon + 12) blesser(en, degats * 2.5 * DT);
            }
        }
    }

    private void majApparitions() {
        spawnTimer -= DT;
        if (spawnTimer <= 0 && ennemis.size() < 250) {
            spawnTimer = Math.max(0.3, (1.1 - temps * 0.007) / (1 + (monde.diff - 1) * 0.4));
            int n = 1 + (int) (temps / 60);
            for (int i = 0; i < n; i++) {
                double r = rnd.nextDouble();
                TypeEnnemi type = TypeEnnemi.MACARON;
                if (temps > 25 && r < 0.20) type = TypeEnnemi.GUIMAUVE;
                else if (temps > 10 && r < 0.45) type = TypeEnnemi.GOMME;
                spawnEnnemi(type, false);
            }
        }
        if (!bossApparu && temps >= monde.dureeBoss) {
            bossApparu = true;
            spawnEnnemi(TypeEnnemi.BOSS, true);
        }
    }

    /** Déplace les ennemis et gère les contacts. Renvoie false si la partie vient de se terminer. */
    private boolean majEnnemis() {
        for (int i = ennemis.size() - 1; i >= 0; i--) {
            Ennemi en = ennemis.get(i);
            en.flash = Math.max(0, en.flash - DT);
            if (en.pv <= 0) {
                ennemis.remove(i);
                mort(en);
                continue;
            }
            // trop loin sur la grande carte : on le retire (il sera remplacé par de nouvelles apparitions)
            if (!en.boss && Math.hypot(en.x - px, en.y - py) > 1500) {
                ennemis.remove(i);
                continue;
            }
            double ang = Math.atan2(py - en.y, px - en.x);
            if (en.contourne > 0) {
                ang += en.sens * 1.1;
                en.contourne -= DT;
            }
            double nx = en.x + Math.cos(ang) * en.vitesse * DT;
            double ny = en.y + Math.sin(ang) * en.vitesse * DT;
            if (!en.boss) { // le boss est trop gros : il écrase tout sur son passage
                double[] c = carte.repousser(nx, ny, en.rayon * 0.8);
                if (c != null) {
                    nx = c[0];
                    ny = c[1];
                    if (en.contourne <= 0) en.contourne = 0.6;
                }
            }
            en.x = nx;
            en.y = ny;

            if (invuln <= 0 && tBouclier <= 0 && Math.hypot(px - en.x, py - en.y) < en.rayon + RAYON_JOUEUR) {
                pvie -= en.degats;
                invuln = 0.8;
                shake = 10;
                degatsSubis = true;
                audio.degat();
                explosion(px, py, 12, new Color(255, 120, 160));
            }
            for (JoueurReseau joueur : joueursReseau) {
                if (joueur.vie > 0 && Math.hypot(joueur.x - en.x, joueur.y - en.y)
                        < en.rayon + RAYON_JOUEUR) {
                    joueur.vie = Math.max(0, joueur.vie - en.degats * DT);
                }
            }
        }
        if (pvie <= 0) {
            terminer(false);
            return false;
        }
        if (bossTue) {
            terminer(true);
            return false;
        }
        return true;
    }

    private void majBonus() {
        for (int i = bonus.size() - 1; i >= 0; i--) {
            Bonus b = bonus.get(i);
            b.vie -= DT;
            if (b.vie <= 0) {
                bonus.remove(i);
                continue;
            }
            if (Math.hypot(px - b.x, py - b.y) < 30) {
                bonus.remove(i);
                ramasser(b.type);
            }
        }
    }

    private void majGemmes() {
        for (int i = gemmes.size() - 1; i >= 0; i--) {
            Gemme g = gemmes.get(i);
            double d = Math.hypot(px - g.x, py - g.y);
            if (g.aimante || d < portee) {
                double a = Math.atan2(py - g.y, px - g.x);
                double v = g.aimante ? 540 : 320;
                g.x += Math.cos(a) * v * DT;
                g.y += Math.sin(a) * v * DT;
            }
            if (d < 20) {
                xp += g.valeur * (tXp > 0 ? 2 : 1);
                gemmes.remove(i);
                audio.bonbon();
            }
        }
    }

    public void majCamera(boolean immediat) {
        double tx = Math.clamp(px - W / 2.0, 0, carte.largeur - W);
        double ty = Math.clamp(py - H / 2.0, 0, carte.hauteur - H);
        if (immediat) {
            camX = tx;
            camY = ty;
        } else {
            double k = 1 - Math.exp(-9 * DT);
            camX += (tx - camX) * k;
            camY += (ty - camY) * k;
        }
    }

    public void majParticules() {
        for (int i = particules.size() - 1; i >= 0; i--) {
            Particule p = particules.get(i);
            p.x += p.vx * DT;
            p.y += p.vy * DT;
            p.vx *= 0.96;
            p.vy *= 0.96;
            p.vie -= DT;
            if (p.vie <= 0) particules.remove(i);
        }
    }

    private void verifierSucces() {
        prog.verifierGlobal();
        if (temps >= 60) prog.debloquer("surv60");
        if (niveau >= 10) prog.debloquer("niv10");
        if (niveau >= 20) prog.debloquer("niv20");
        if (etoiles >= 6) prog.debloquer("etoiles6");
        if (projectiles >= 6) prog.debloquer("proj6");
        if (coins[0] && coins[1] && coins[2] && coins[3]) prog.debloquer("coins");
    }

    // ------------------------------------------------------------------
    // Ennemis
    // ------------------------------------------------------------------
    private void spawnEnnemi(TypeEnnemi type, boolean boss) {
        Monde mo = monde;
        Ennemi e = new Ennemi();
        boolean place = false;
        // on le fait apparaître juste hors de l'écran, dans la carte et pas dans un obstacle
        for (int essai = 0; essai < 14 && !place; essai++) {
            double a = rnd.nextDouble() * Math.PI * 2;
            double d = 560 + rnd.nextDouble() * 180;
            double x = Math.clamp(px + Math.cos(a) * d, 40, carte.largeur - 40);
            double y = Math.clamp(py + Math.sin(a) * d, 40, carte.hauteur - 40);
            if (x > camX - 30 && x < camX + W + 30 && y > camY - 30 && y < camY + H + 30) continue; // visible
            if (!boss && carte.libre(x, y, 24)) continue;
            e.x = x;
            e.y = y;
            place = true;
        }
        if (!place) {
            if (!boss) return;
            e.x = px < carte.largeur / 2.0 ? carte.largeur - 100 : 100; // le boss arrive toujours
            e.y = py < carte.hauteur / 2.0 ? carte.hauteur - 100 : 100;
        }
        e.type = type;
        e.monde = mo.id;
        e.boss = boss;
        e.sens = rnd.nextBoolean() ? 1 : -1;
        e.col = boss ? mo.bossCol : mo.palette[rnd.nextInt(mo.palette.length)];
        double echelle = (1 + temps / 90.0) * mo.diff;     // plus costaud avec le temps et les mondes
        double vit = 1 + (mo.diff - 1) * 0.25;
        double dgt = 1 + (mo.diff - 1) * 0.5;
        if (boss) {
            e.pvMax = mo.bossPv;
            e.rayon = 48;
            e.vitesse = 55 * vit;
            e.degats = 25 * dgt;
        } else if (type == TypeEnnemi.MACARON) {
            e.pvMax = 20 * echelle;
            e.rayon = 16;
            e.vitesse = 60 * vit;
            e.degats = 10 * dgt;
        } else if (type == TypeEnnemi.GUIMAUVE) {
            e.pvMax = 60 * echelle;
            e.rayon = 22;
            e.vitesse = 35 * vit;
            e.degats = 15 * dgt;
        } else {
            e.pvMax = 10 * echelle;
            e.rayon = 11;
            e.vitesse = 105 * vit;
            e.degats = 8 * dgt;
        }
        e.pv = e.pvMax;
        ennemis.add(e);
    }

    /** Commandes temporaires de la console de développement. */
    public int debugSpawn(int type, int nombre, boolean boss) {
        if (carte == null) return 0;
        int avant = ennemis.size();
        int total = Math.clamp(nombre, 1, 1000000);
        TypeEnnemi typeEnnemi = TypeEnnemi.depuisId(type);
        for (int i = 0; i < total; i++) spawnEnnemi(typeEnnemi, boss);
        return ennemis.size() - avant;
    }

    public void debugTuerTous() {
        for (Ennemi e : ennemis) e.pv = 0;
    }

    public void debugNiveau(int valeur) {
        int cible = Math.clamp(valeur, niveau, 999);
        issue = Issue.AUCUNE;
        choix = new ArrayList<>();
        // Les niveaux intermédiaires prennent le premier choix pour ne pas
        // empiler des écrans de sélection dans la console de développement.
        while (niveau < cible - 1) {
            xp = xpSuivant;
            niveauSup();
            if (!choix.isEmpty()) {
                choix.getFirst().effet.run();
                choix = new ArrayList<>();
            }
            issue = Issue.AUCUNE;
        }
        if (niveau < cible) {
            xp = xpSuivant;
            niveauSup();
        }
    }

    private Ennemi plusProche(double distMax) {
        return plusProche(px, py, distMax);
    }

    private Ennemi plusProche(double x, double y, double distMax) {
        Ennemi best = null;
        double bd = distMax;
        for (Ennemi e : ennemis) {
            double d = Math.hypot(e.x - x, e.y - y);
            if (d < bd) {
                bd = d;
                best = e;
            }
        }
        return best;
    }

    private void tirerDepuis(double x, double y, Personnage personnageTireur, Ennemi cible) {
        Profil pf = profil(personnageTireur);
        double a = Math.atan2(cible.y - y, cible.x - x);
        Tir t = new Tir();
        t.x = x;
        t.y = y;
        double vitesseTir = switch (personnageTireur.typeArme) {
            case FLECHE -> 680;
            case BOMBE -> 360;
            case EPEE -> 520;
            case BOOMERANG -> 300;
            case RAYON -> 900;
            default -> 520;
        };
        t.vx = Math.cos(a) * vitesseTir;
        t.vy = Math.sin(a) * vitesseTir;
        t.degats = degats * pf.degats;
        t.perfo = perforation + pf.perfo;
        t.vie = personnageTireur.typeArme == TypeArme.BOMBE ? 1.8 : 1.4;
        t.arme = personnageTireur.typeArme;
        if (personnageTireur.typeArme == TypeArme.BOMBE) t.rayonExplosion = rayonExplosion;
        tirs.add(t);
    }

    private void tirer(Ennemi cible) {
        Profil pf = profil();
        TypeArme arme = personnage.typeArme;
        double vit = pf.vitesse * vitesseArme;

        // la bombe est lobée : elle vise là où l'ennemi sera et atterrit pile dessus
        double cx = cible.x, cy = cible.y;
        double dureeBombe = 0;
        if (arme == TypeArme.BOMBE) {
            double d0 = Math.max(1, Math.hypot(px - cible.x, py - cible.y));
            double dt = d0 / vit;
            cx += (px - cible.x) / d0 * cible.vitesse * dt;
            cy += (py - cible.y) / d0 * cible.vitesse * dt;
            double d1 = Math.hypot(cx - px, cy - py);
            dureeBombe = Math.clamp(d1 / vit, 0.35, 1.5);
            vit = d1 / dureeBombe;
        }

        double base = Math.atan2(cy - py, cx - px);
        int nb = projectiles + pf.tirs - 1;
        for (int i = 0; i < nb; i++) {
            double a = base + (i - (nb - 1) / 2.0) * pf.ecart;
            Tir t = new Tir();
            t.x = px;
            t.y = py;
            t.vx = Math.cos(a) * vit;
            t.vy = Math.sin(a) * vit;
            t.degats = degats * pf.degats;
            t.perfo = perforation + pf.perfo;
            switch (arme) {
                case BOMBE:
                    t.vie = dureeBombe;
                    t.rayonExplosion = rayonExplosion;
                    break;
                case EPEE:
                    t.vie = porteeArme * 3.2 / vit;
                    break;
                case RAYON:
                    t.vie = pf.portee / vit;
                    break;
                case BOOMERANG:
                    t.vie = 3.2;
                    break;
                case ETOILE:
                    t.vie = 1.6;
                    break;
                default: // flèches
                    t.vie = 1.0;
                    break;
            }
            t.arme = arme;
            tirs.add(t);
        }
        audio.tir();
    }

    /** Active la compétence propre au personnage. */
    public boolean activerCompetence() {
        if (competenceTimer > 0) return false;
        if (personnage.id == 0) {
            novaX = px;
            novaY = py;
            novaT = 0.6;
            for (Ennemi en : ennemis) {
                if (Math.hypot(en.x - px, en.y - py) < 360) blesser(en, degats * 5);
            }
        } else if (personnage.id == 1) {
            for (int i = 0; i < 12; i++) {
                double a = i * Math.PI * 2 / 12.0;
                Tir t = new Tir();
                t.x = px;
                t.y = py;
                t.vx = Math.cos(a) * 600;
                t.vy = Math.sin(a) * 600;
                t.degats = degats * 1.8;
                t.perfo = perforation + 2;
                t.vie = 1.2;
                t.arme = personnage.typeArme;
                tirs.add(t);
            }
        } else if (personnage.id == 2) {
            // Boum est la spécialiste défensive : son bouclier reste sa signature.
            tBouclier = 5;
            for (Ennemi en : ennemis) {
                if (Math.hypot(en.x - px, en.y - py) < 150) blesser(en, degats * 3);
            }
        } else if (personnage.id == 3) {
            // Lame : une salve courte de lames dans un arc frontal.
            Ennemi cible = plusProche(640);
            double angleCible = cible == null
                    ? angleOrbite
                    : Math.atan2(cible.y - py, cible.x - px);
            for (int i = -3; i <= 3; i++) {
                Tir t = new Tir();
                double a = angleCible + i * 0.16;
                t.x = px;
                t.y = py;
                t.vx = Math.cos(a) * 650;
                t.vy = Math.sin(a) * 650;
                t.degats = degats * 1.5;
                t.perfo = perforation + 2;
                t.vie = 0.8;
                t.arme = TypeArme.EPEE;
                tirs.add(t);
            }
        } else if (personnage.id == 4) {
            // Ronce : anneau de boomerangs qui traverse plusieurs ennemis.
            for (int i = 0; i < 8; i++) {
                double a = i * Math.PI * 2 / 8.0;
                Tir t = new Tir();
                t.x = px;
                t.y = py;
                t.vx = Math.cos(a) * 360;
                t.vy = Math.sin(a) * 360;
                t.degats = degats * 1.3;
                t.perfo = perforation + 3;
                t.vie = 1.5;
                t.arme = TypeArme.BOOMERANG;
                tirs.add(t);
            }
        } else {
            // Prisme : six rayons concentrés, sans protection défensive.
            for (int i = 0; i < 6; i++) {
                double a = i * Math.PI * 2 / 6.0;
                Tir t = new Tir();
                t.x = px;
                t.y = py;
                t.vx = Math.cos(a) * 950;
                t.vy = Math.sin(a) * 950;
                t.degats = degats * 2.2;
                t.perfo = 99;
                t.vie = 0.7;
                t.arme = TypeArme.RAYON;
                tirs.add(t);
            }
        }
        competenceTimer = 12;
        shake = 8;
        audio.pouvoir();
        return true;
    }

    private void blesser(Ennemi e, double d) {
        e.pv -= d;
        e.flash = 0.08;
    }

    private void mort(Ennemi e) {
        kills++;
        prog.totalKills++;
        audio.mort();
        explosion(e.x, e.y, e.boss ? 80 : 10, e.col);
        int nb = e.boss ? 20 : 1;
        for (int i = 0; i < nb; i++) {
            Gemme g = new Gemme();
            g.x = e.x + (e.boss ? rnd.nextGaussian() * 30 : 0);
            g.y = e.y + (e.boss ? rnd.nextGaussian() * 30 : 0);
            g.valeur = e.boss ? 3 : (e.type == TypeEnnemi.GUIMAUVE ? 2 : 1);
            gemmes.add(g);
        }
        if (e.boss) {
            bossTue = true;
            return;
        }
        // chance de lâcher un pouvoir : les guimauves (costaudes) en lâchent plus, les gommes moins
        double mult = e.type == TypeEnnemi.GUIMAUVE ? 2.0
                : e.type == TypeEnnemi.GOMME ? 0.6 : 1.0;
        if (bossApparu) mult *= 1.5; // pendant le combat de boss, ça tombe plus souvent
        if (bonus.size() < 5 && rnd.nextDouble() < Constantes.TAUX_DROP * mult * chanceBonus) {
            Bonus b = new Bonus();
            b.x = Math.clamp(e.x, 30, carte.largeur - 30);
            b.y = Math.clamp(e.y, 30, carte.hauteur - 30);
            b.type = tirerPouvoir();
            b.vie = 14;
            bonus.add(b);
        }
    }

    private int tirerPouvoir() {
        double r = rnd.nextDouble() * Pouvoir.poidsTotal();
        for (int i = 0; i < Pouvoir.TOUS.length; i++) {
            r -= Pouvoir.TOUS[i].poids;
            if (r < 0) return i;
        }
        return 0;
    }

    private void ramasser(int type) {
        Pouvoir p = Pouvoir.TOUS[type];
        switch (type) {
            case Pouvoir.COEUR:
                pvie = Math.min(pvieMax, pvie + 30);
                break;
            case Pouvoir.AIMANT:
                for (Gemme g : gemmes) g.aimante = true;
                break;
            case Pouvoir.BOUCLIER:
                tBouclier = p.duree;
                break;
            case Pouvoir.TURBO:
                tTurbo = p.duree;
                break;
            case Pouvoir.NOVA:
                novaT = 0.6;
                novaX = px;
                novaY = py;
                shake = 12;
                for (Ennemi en : ennemis) blesser(en, degats * 8);
                break;
            case Pouvoir.DOUBLE:
                tXp = p.duree;
                break;
            default:
                break;
        }
        msgType = type;
        msgT = 2.5;
        explosion(px, py, 14, p.col);
        audio.pouvoir();
    }

    public void explosion(double x, double y, int n, Color c) {
        if (particules.size() > 700) return;
        if (!reg.effets) n = (n + 2) / 3;
        for (int i = 0; i < n; i++) {
            Particule p = new Particule();
            double a = rnd.nextDouble() * Math.PI * 2;
            double v = 40 + rnd.nextDouble() * 180;
            p.x = x;
            p.y = y;
            p.vx = Math.cos(a) * v;
            p.vy = Math.sin(a) * v;
            p.vieMax = 0.4 + rnd.nextDouble() * 0.5;
            p.vie = p.vieMax;
            p.taille = 3 + rnd.nextDouble() * 5;
            p.col = rnd.nextBoolean() ? c : Color.WHITE;
            particules.add(p);
        }
    }

    // ------------------------------------------------------------------
    // Niveaux et améliorations
    // ------------------------------------------------------------------
    private List<Amelio> tirerChoix() {
        List<Amelio> tous = new ArrayList<>();
        if (projectiles < 6)
            tous.add(new Amelio("Double arc-en-ciel", "+1 projectile\nà chaque tir",
                    new Color(255, 170, 200), () -> projectiles++));
        if (etoiles < 6)
            tous.add(new Amelio("Paillettes magiques", "+1 étoile qui\ntourne autour de toi",
                    new Color(255, 230, 140), () -> etoiles++));
        tous.add(new Amelio("Corne puissante", "+25% de dégâts",
                new Color(255, 190, 150), () -> degats *= 1.25));
        tous.add(new Amelio("Sabots turbo", "+12% de vitesse",
                new Color(170, 230, 255), () -> vitesse *= 1.12));
        tous.add(new Amelio("Cadence folle", "+20% de vitesse\nde tir",
                new Color(200, 180, 255), () -> cadence *= 1.2));
        tous.add(new Amelio("Arc-en-ciel perçant", "Les tirs traversent\n1 ennemi de plus",
                new Color(160, 235, 200), () -> perforation++));
        tous.add(new Amelio("Aimant à bonbons", "Attire les bonbons\nde plus loin",
                new Color(255, 180, 230), () -> portee += 60));
        tous.add(new Amelio("Trèfle de sucre", "+40% de chance\nde drop de pouvoirs",
                new Color(190, 240, 170), () -> chanceBonus *= 1.4));
        tous.add(new Amelio("Câlin de guimauve", "+20 PV max\net soigne 50 PV",
                new Color(255, 215, 225), () -> {
            pvieMax += 20;
            pvie = Math.min(pvieMax, pvie + 50);
        }));
        switch (personnage.typeArme) {
            case EPEE:
                tous.add(new Amelio("Lame longue", "+18 portée de frappe",
                        new Color(220, 190, 255), () -> porteeArme += 18));
                tous.add(new Amelio("Tranchant astral", "+25% dégâts de mêlée",
                        new Color(190, 150, 255), () -> degats *= 1.25));
                break;
            case BOMBE:
                tous.add(new Amelio("Gros pétards", "+14 rayon d'explosion",
                        new Color(255, 170, 90), () -> rayonExplosion += 14));
                tous.add(new Amelio("Mèche courte", "+18% cadence des bombes",
                        new Color(255, 215, 120), () -> cadence *= 1.18));
                break;
            case RAYON:
                tous.add(new Amelio("Lentille prismatique", "+30% dégâts du rayon",
                        new Color(255, 230, 120), () -> degats *= 1.30));
                tous.add(new Amelio("Faisceau large", "le rayon traverse encore mieux",
                        new Color(220, 240, 255), () -> perforation += 2));
                break;
            case BOOMERANG:
                tous.add(new Amelio("Retour acéré", "+1 traversée du boomerang",
                        new Color(150, 235, 180), () -> perforation++));
                tous.add(new Amelio("Lancer vif", "+15% vitesse des boomerangs",
                        new Color(180, 255, 210), () -> vitesseArme *= 1.15));
                break;
            default:
                break;
        }
        Collections.shuffle(tous, rnd);
        return new ArrayList<>(tous.subList(0, 3));
    }

    private void niveauSup() {
        xp -= xpSuivant;
        niveau++;
        xpSuivant = prochainSeuil(xpSuivant);
        pvie = Math.min(pvieMax, pvie + 10);
        choix = tirerChoix();
        issue = Issue.NIVEAU;
        audio.niveau();
    }

    private static int prochainSeuil(int seuilActuel) {
        long suivant = (long) (Math.max(1, seuilActuel) * 1.3) + 3;
        return (int) Math.min(1_000_000L, suivant);
    }

    /** Applique l'amélioration choisie. Si plusieurs niveaux d'un coup, un nouveau choix sera proposé. */
    public void appliquerChoix(int i) {
        if (i < 0 || i >= choix.size()) return;
        choix.get(i).effet.run();
        choix = new ArrayList<>();
        audio.clic();
        if (xpSuivant > 0 && xp >= xpSuivant) niveauSup();
    }
}
