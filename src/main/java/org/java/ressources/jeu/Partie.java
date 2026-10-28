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
import org.java.ressources.entites.TirEnnemi;
import org.java.ressources.entites.ZoneDanger;
import org.java.ressources.entites.Eclair;
import org.java.ressources.donnees.ArmeSecondaire;
import org.java.ressources.entites.TypeEnnemi;
import org.java.ressources.entites.TypeArme;
import org.java.ressources.reseau.ReseauLan;

import java.awt.Color;
import java.awt.event.KeyEvent;
import java.time.LocalDate;
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
    final Audio audio;
    final Random rnd = new Random();

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

    // pouvoirs en cours
    public double tBouclier, tTurbo, tXp, chanceBonus = 1;
    public double novaT, novaX, novaY;
    public int msgType;
    public double msgT;

    // ---- esquive (dash) ----
    public static final double ESQUIVE_DUREE = 0.18, ESQUIVE_RECHARGE = 2.2, ESQUIVE_VITESSE = 760;
    public double esquiveT, esquiveRecharge, esquiveDx = 1, esquiveDy;
    /** Temps écoulé depuis le début de la dernière esquive (sert à l'affichage). */
    public double esquiveDepuis = 9;
    /** Dernière direction de déplacement (l'esquive part dans cette direction). */
    private double dirX = 1, dirY;
    /** Position du joueur au pas précédent (le boss s'en sert pour viser devant lui). */
    double dernierPx, dernierPy;
    public int esquivesRun;

    // ---- projectiles ennemis, zones qui explosent, éclairs ----
    public final List<TirEnnemi> tirsEnnemis = new ArrayList<>();
    public final List<ZoneDanger> zones = new ArrayList<>();
    public final List<Eclair> eclairs = new ArrayList<>();

    // ---- armes secondaires et évolutions ----
    public final int[] niveauArme = new int[ArmeSecondaire.values().length];
    public final boolean[] evolueeArme = new boolean[ArmeSecondaire.values().length];
    final double[] minuteurArme = new double[ArmeSecondaire.values().length];
    public boolean constellation;
    public double rayonOrbite = 75, multOrbite = 1;
    public int evolutionsRun;
    /** Combien de fois chaque amélioration a été prise (sert aux conditions d'évolution). */
    private final Map<String, Integer> ameliosPrises = new HashMap<>();

    // ---- modes de jeu (réglés par le menu AVANT l'appel à lancer) ----
    public static final int MODE_NORMAL = 0, MODE_CAUCHEMAR = 1, MODE_INFINI = 2;
    public static final String[] MODIFICATEURS = {"Marée de gommes", "Pluie de pouvoirs", "Course folle", "Géants"};
    public int modeJeu = MODE_NORMAL;
    public boolean defiJour;
    public int modificateurDefi = -1;
    public boolean cauchemar, infini;
    private double modSpawn = 1, modDrop = 1, modVitEnn = 1, modPv = 1, modXp = 1;
    public int relances, revivesRestants, bossNum;
    public boolean victoireAcquise;
    private double prochainBoss, prochainElite, prochainEncerclement, gainXp = 1, xpReste;
    /** Bandeau d'annonce au centre de l'écran (phase du boss, élite, évolution...). */
    public String annonce = "";
    public double annonceT;

    public static String dateDuJour() {
        return LocalDate.now().toString();
    }

    public static int modificateurDuJour() {
        return Math.floorMod(dateDuJour().hashCode(), MODIFICATEURS.length);
    }

    /** Graine du défi du jour : la même carte pour tout le monde, toute la journée. */
    public static long graineDuJour(int monde) {
        return LocalDate.now().toEpochDay() * 1_000_003L + monde * 7919L + 17;
    }

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
        public double esquiveT, esquiveRecharge, dirX = 1, dirY;

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
        pvieMax = 100 + 15 * prog.meta[Progression.VIE];
        pvie = pvieMax;
        degats = 10 * (1 + 0.10 * prog.meta[Progression.DEGATS]);
        vitesse = 190 * (1 + 0.05 * prog.meta[Progression.VITESSE]);
        cadence = 1.6 * (1 + 0.04 * prog.meta[Progression.CADENCE]);
        projectiles = 1;
        perforation = 0;
        portee = 90 + 12 * prog.meta[Progression.AIMANT];
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
        chanceBonus = 1 + 0.05 * prog.meta[Progression.CHANCE];
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
        reinitialiserNouveautes();
        majCamera(true);
    }

    /** Remet à zéro tout ce qui est propre à une partie (esquive, armes secondaires, modes, méta). */
    private void reinitialiserNouveautes() {
        tirsEnnemis.clear();
        zones.clear();
        eclairs.clear();
        java.util.Arrays.fill(niveauArme, 0);
        java.util.Arrays.fill(evolueeArme, false);
        java.util.Arrays.fill(minuteurArme, 0.5);
        ameliosPrises.clear();
        constellation = false;
        rayonOrbite = 75;
        multOrbite = 1;
        evolutionsRun = 0;
        esquiveT = 0;
        esquiveRecharge = 0;
        esquiveDepuis = 9;
        esquivesRun = 0;
        dirX = 1;
        dirY = 0;
        relances = 1 + prog.meta[Progression.RELANCE];
        revivesRestants = prog.meta[Progression.REVIVE];
        gainXp = 1 + 0.05 * prog.meta[Progression.XP];
        xpReste = 0;
        bossNum = 0;
        victoireAcquise = false;
        prochainBoss = 0;
        prochainElite = 45;
        prochainEncerclement = 80;
        annonce = "";
        annonceT = 0;
        cauchemar = modeJeu == MODE_CAUCHEMAR && !defiJour;
        infini = modeJeu == MODE_INFINI && !defiJour;
        modSpawn = modDrop = modVitEnn = modPv = modXp = 1;
        modificateurDefi = defiJour ? modificateurDuJour() : -1;
        if (defiJour) {
            switch (modificateurDefi) {
                case 0 -> modSpawn = 1.35;
                case 1 -> {
                    modDrop = 3;
                    modPv = 1.25;
                }
                case 2 -> {
                    modVitEnn = 1.15;
                    vitesse *= 1.15;
                }
                default -> {
                    modPv = 1.6;
                    modXp = 1.5;
                }
            }
            annonce("Défi du jour : " + MODIFICATEURS[modificateurDefi]);
        } else if (cauchemar) {
            annonce("Mode Cauchemar : ennemis plus forts, sucre x1,5");
        } else if (infini) {
            annonce("Mode Infini : un nouveau boss toutes les 90 s");
        }
    }

    public void annonce(String texte) {
        annonce = texte;
        annonceT = 3.2;
    }

    /** Difficulté effective du monde (le mode Cauchemar l'augmente). */
    double diff() {
        return monde.diff * (cauchemar ? 1.35 : 1.0);
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
            joueur.esquiveRecharge = Math.max(0, joueur.esquiveRecharge - DT);
            if (dx != 0 || dy != 0) {
                double n = Math.hypot(dx, dy);
                joueur.dirX = dx / n;
                joueur.dirY = dy / n;
            }
            if ((entree.touches() & 16) != 0 && joueur.esquiveRecharge <= 0 && joueur.esquiveT <= 0) {
                joueur.esquiveT = ESQUIVE_DUREE;
                joueur.esquiveRecharge = ESQUIVE_RECHARGE;
            }
            if (joueur.esquiveT > 0 || dx != 0 || dy != 0) {
                double pas;
                double mx, my;
                if (joueur.esquiveT > 0) {
                    joueur.esquiveT -= DT;
                    pas = ESQUIVE_VITESSE * DT;
                    mx = joueur.dirX;
                    my = joueur.dirY;
                } else {
                    double n = Math.hypot(dx, dy);
                    pas = vitesse * (tTurbo > 0 ? 1.25 : 1) * DT;
                    mx = dx / n;
                    my = dy / n;
                }
                joueur.x += mx * pas;
                joueur.y += my * pas;
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
            joueur.tirTimer -= DT;
            if (joueur.tirTimer <= 0) {
                Ennemi cible = plusProche(joueur.x, joueur.y, 640);
                if (cible != null) {
                    tirerDepuis(joueur.x, joueur.y, joueur.personnage, cible);
                    joueur.tirTimer = joueur.personnage.typeArme == TypeArme.BOMBE ? 2.8 : 1.6;
                }
            }
            if (joueur.competence && !joueur.competencePrecedente) {
                for (Ennemi ennemi : ennemis) {
                    if (Math.hypot(ennemi.x - joueur.x, ennemi.y - joueur.y) < 180) {
                        blesser(ennemi, degats * 0.35);
                    }
                }
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
                    .append(',').append(joueur.personnage.id);
        }
        b.append("|E:");
        for (int i = 0; i < ennemis.size(); i++) {
            if (i > 0) b.append(';');
            Ennemi e = ennemis.get(i);
            b.append(Math.round(e.x)).append(',').append(Math.round(e.y)).append(',')
                    .append(Math.round(e.pv)).append(',').append(e.type.ordinal()).append(',').append(e.boss)
                    .append(',').append(e.elite).append(',').append(Math.round(e.rayon))
                    .append(',').append(Math.round(e.pvMax)).append(',').append(e.etape)
                    .append(',').append(e.attaque).append(',').append(Math.round(e.minuteur * 100))
                    .append(',').append(Math.round(Math.atan2(e.dirY, e.dirX) * 100));
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
        b.append("|B:");
        for (int i = 0; i < tirsEnnemis.size(); i++) {
            if (i > 0) b.append(';');
            TirEnnemi t = tirsEnnemis.get(i);
            b.append(Math.round(t.x)).append(',').append(Math.round(t.y)).append(',')
                    .append(Math.round(t.rayon)).append(',').append(t.genre);
        }
        b.append("|Z:");
        for (int i = 0; i < zones.size(); i++) {
            if (i > 0) b.append(';');
            ZoneDanger z = zones.get(i);
            b.append(Math.round(z.x)).append(',').append(Math.round(z.y)).append(',')
                    .append(Math.round(z.rayon)).append(',').append(Math.round(z.delai * 100)).append(',')
                    .append(Math.round(z.delaiMax * 100)).append(',').append(z.ennemie).append(',')
                    .append(z.explosee).append(',').append(Math.round(z.apres * 100));
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
                    e.monde = monde.id;
                    if (champs.length >= 12) { // nouveau format : taille réelle, élite, état du boss
                        e.elite = Boolean.parseBoolean(champs[5]);
                        e.rayon = Double.parseDouble(champs[6]);
                        e.pvMax = Math.max(e.pv, Double.parseDouble(champs[7]));
                        e.etape = Integer.parseInt(champs[8]);
                        e.attaque = Integer.parseInt(champs[9]);
                        e.minuteur = Double.parseDouble(champs[10]) / 100;
                        double angle = Double.parseDouble(champs[11]) / 100;
                        e.dirX = Math.cos(angle);
                        e.dirY = Math.sin(angle);
                    } else {
                        e.rayon = e.boss ? 48 : 16;
                    }
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
        tirsEnnemis.clear();
        if (sections.length > 3 && sections[3].startsWith("B:")) {
            for (String ligne : sections[3].substring(2).split(";")) {
                String[] champs = ligne.split(",");
                if (champs.length < 4) continue;
                try {
                    TirEnnemi t = new TirEnnemi();
                    t.x = Double.parseDouble(champs[0]);
                    t.y = Double.parseDouble(champs[1]);
                    t.rayon = Double.parseDouble(champs[2]);
                    t.genre = Integer.parseInt(champs[3]);
                    tirsEnnemis.add(t);
                } catch (NumberFormatException ignored) {
                    // Un projectile ennemi invalide est simplement ignoré.
                }
            }
        }
        zones.clear();
        if (sections.length > 4 && sections[4].startsWith("Z:")) {
            for (String ligne : sections[4].substring(2).split(";")) {
                String[] champs = ligne.split(",");
                if (champs.length < 8) continue;
                try {
                    ZoneDanger z = new ZoneDanger();
                    z.x = Double.parseDouble(champs[0]);
                    z.y = Double.parseDouble(champs[1]);
                    z.rayon = Double.parseDouble(champs[2]);
                    z.delai = Double.parseDouble(champs[3]) / 100;
                    z.delaiMax = Math.max(0.01, Double.parseDouble(champs[4]) / 100);
                    z.ennemie = Boolean.parseBoolean(champs[5]);
                    z.explosee = Boolean.parseBoolean(champs[6]);
                    z.apres = Double.parseDouble(champs[7]) / 100;
                    zones.add(z);
                } catch (NumberFormatException ignored) {
                    // Une zone invalide est simplement ignorée.
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
        boolean vic = gagneLaPartie || victoireAcquise;
        victoire = vic;
        double mult = (cauchemar ? 1.5 : 1.0) * (defiJour ? 1.5 : 1.0);
        int base = kills / 2 + (int) (temps / 10) + (vic ? 100 * (mondeJeu + 1) : 0) + (infini ? 60 * bossNum : 0);
        gainRun = (int) Math.round(base * mult);
        prog.sucre += gainRun;
        prog.totalSucre += gainRun;
        prog.meilleur[mondeJeu] = Math.max(prog.meilleur[mondeJeu], temps);
        if (gagneLaPartie) {
            validerVictoire();
        } else if (victoireAcquise) {
            audio.victoire(); // mode Infini : la victoire était déjà acquise
        } else {
            audio.defaite();
        }
        if (defiJour) {
            String date = dateDuJour();
            int score = kills + (int) temps + (vic ? 500 : 0);
            if (!date.equals(prog.defiDate)) {
                prog.defiDate = date;
                prog.defiScore = 0;
            }
            prog.defiScore = Math.max(prog.defiScore, score);
        }
        explosion(px, py, 40, Color.WHITE);
        issue = Issue.FIN;
        prog.verifierGlobal();
    }

    /** Premier boss vaincu : succès, monde suivant, etc. (une seule fois par partie). */
    private void validerVictoire() {
        prog.gagne[mondeJeu] = true;
        prog.debloquer("boss" + (mondeJeu + 1));
        if (!degatsSubis) prog.debloquer("intouchable");
        if (cauchemar) prog.debloquer("cauchemar");
        if (defiJour) prog.debloquer("defi");
        if (mondeJeu == prog.mondeMax && prog.mondeMax < Monde.TOUS.length - 1) {
            prog.mondeMax++;
            nouveauMonde = true;
        }
        audio.victoire();
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
        esquiveRecharge = Math.max(0, esquiveRecharge - DT);
        esquiveDepuis += DT;
        annonceT = Math.max(0, annonceT - DT);

        dernierPx = px;
        dernierPy = py;
        deplacerJoueur(touches);

        // attaque automatique propre à chaque personnage (voir PROFILS)
        attaqueDeBase();
        ArmesSecondaires.maj(this);

        majTirs();
        majEtoiles();
        majTirsEnnemis();
        majZones();
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
        if (esquiveT > 0) { // esquive : bond rapide, invulnérable
            esquiveT -= DT;
            nx += esquiveDx * ESQUIVE_VITESSE * DT;
            ny += esquiveDy * ESQUIVE_VITESSE * DT;
            invuln = Math.max(invuln, 0.1);
            if (reg.effets && rnd.nextInt(2) == 0) explosion(px, py, 1, Color.WHITE);
        } else if (reg.modeSouris) {
            double ddx = cibleX + camX - px, ddy = cibleY + camY - py;
            double d = Math.hypot(ddx, ddy);
            if (d > 3) {
                double pas = Math.min(d, vit * DT);
                nx += ddx / d * pas;
                ny += ddy / d * pas;
                dirX = ddx / d;
                dirY = ddy / d;
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
                dirX = dx / n;
                dirY = dy / n;
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
            double sx = px + Math.cos(a) * rayonOrbite, sy = py + Math.sin(a) * rayonOrbite;
            for (Ennemi en : ennemis) {
                if (Math.hypot(en.x - sx, en.y - sy) < en.rayon + 12) blesser(en, degats * 2.5 * multOrbite * DT);
            }
        }
    }

    private void majApparitions() {
        spawnTimer -= DT;
        if (spawnTimer <= 0 && ennemis.size() < 250) {
            spawnTimer = Math.max(0.3, (1.1 - temps * 0.007) / (1 + (diff() - 1) * 0.4)) / modSpawn;
            int n = 1 + (int) (temps / 60);
            for (int i = 0; i < n; i++) {
                double r = rnd.nextDouble();
                TypeEnnemi type = TypeEnnemi.MACARON;
                if (temps > 25 && r < 0.20) type = TypeEnnemi.GUIMAUVE;
                else if (temps > 10 && r < 0.45) type = TypeEnnemi.GOMME;
                // les nouveaux ennemis arrivent plus tard dans la partie
                double r2 = rnd.nextDouble();
                if (temps > 35 && r2 < 0.10 && compter(TypeEnnemi.TIREUR) < 7) type = TypeEnnemi.TIREUR;
                else if (temps > 50 && r2 < 0.19) type = TypeEnnemi.KAMIKAZE;
                spawnEnnemi(type, false);
            }
        }
        // événements : élite et encerclement (pas pendant un combat de boss)
        if (temps >= prochainElite && bossVivant()) {
            if (spawnElite()) prochainElite = temps + (cauchemar ? 30 : 42);
            else prochainElite = temps + 1;
        }
        if (temps >= prochainEncerclement && bossVivant()) {
            prochainEncerclement = temps + 85;
            encerclement();
        }
        if (!bossApparu && temps >= monde.dureeBoss) {
            bossApparu = true;
            spawnEnnemi(TypeEnnemi.BOSS, true);
        } else if (infini && bossApparu && prochainBoss > 0 && temps >= prochainBoss && bossVivant()) {
            prochainBoss = 0;
            spawnEnnemi(TypeEnnemi.BOSS, true);
            annonce("Un nouveau boss arrive, plus solide !");
        }
    }

    boolean bossVivant() {
        for (Ennemi e : ennemis) if (e.boss) return false;
        return true;
    }

    private int compter(TypeEnnemi type) {
        int n = 0;
        for (Ennemi e : ennemis) if (e.type == type) n++;
        return n;
    }

    /** Un ennemi plus gros, plus solide, qui lâche toujours un pouvoir. */
    private boolean spawnElite() {
        TypeEnnemi[] types = {TypeEnnemi.MACARON, TypeEnnemi.GUIMAUVE, TypeEnnemi.GOMME, TypeEnnemi.TIREUR};
        int avant = ennemis.size();
        spawnEnnemi(types[rnd.nextInt(types.length)], false);
        if (ennemis.size() <= avant) return false;
        Ennemi e = ennemis.getLast();
        e.elite = true;
        e.pvMax *= 6;
        e.pv = e.pvMax;
        e.rayon *= 1.45;
        e.degats *= 1.4;
        e.vitesse *= 0.95;
        annonce("Un ennemi élite approche !");
        return true;
    }

    /** Un cercle de gommes se referme autour du joueur. */
    private void encerclement() {
        annonce("Encerclement !");
        int n = 22;
        for (int k = 0; k < n; k++) {
            double a = k * 2 * Math.PI / n;
            double x = px + Math.cos(a) * 560, y = py + Math.sin(a) * 560;
            if (x < 40 || x > carte.largeur - 40 || y < 40 || y > carte.hauteur - 40) continue;
            if (carte.libre(x, y, 24)) continue;
            creerEnnemiA(TypeEnnemi.GOMME, x, y);
        }
    }

    /** Crée un ennemi à une position précise (renforts du boss, encerclement). */
    Ennemi creerEnnemiA(TypeEnnemi type, double x, double y) {
        if (ennemis.size() >= 320) return null;
        Ennemi e = new Ennemi();
        e.x = x;
        e.y = y;
        initialiserEnnemi(e, type, false);
        ennemis.add(e);
        return e;
    }

    /** Déplace un ennemi vers l'angle donné en contournant les obstacles (sauf le boss, qui écrase tout). */
    void avancerEnnemi(Ennemi en, double ang, double vit) {
        if (en.contourne > 0) {
            ang += en.sens * 1.1;
            en.contourne -= DT;
        }
        double nx = en.x + Math.cos(ang) * vit * DT;
        double ny = en.y + Math.sin(ang) * vit * DT;
        if (!en.boss) {
            double[] c = carte.repousser(nx, ny, en.rayon * 0.8);
            if (c != null) {
                nx = c[0];
                ny = c[1];
                if (en.contourne <= 0) en.contourne = 0.6;
            }
        }
        en.x = nx;
        en.y = ny;
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
            if (!en.boss && !en.elite && Math.hypot(en.x - px, en.y - py) > 1500) {
                ennemis.remove(i);
                continue;
            }
            if (en.boss) IaEnnemis.boss(this, en);
            else if (en.type == TypeEnnemi.TIREUR) IaEnnemis.tireur(this, en);
            else if (en.type == TypeEnnemi.KAMIKAZE) IaEnnemis.kamikaze(this, en);
            else avancerEnnemi(en, Math.atan2(py - en.y, px - en.x), en.vitesse);

            if (Math.hypot(px - en.x, py - en.y) < en.rayon + RAYON_JOUEUR) blesserJoueur(en.degats, 0.8);
            for (JoueurReseau joueur : joueursReseau) {
                if (joueur.vie > 0 && joueur.esquiveT <= 0 && Math.hypot(joueur.x - en.x, joueur.y - en.y)
                        < en.rayon + RAYON_JOUEUR) {
                    joueur.vie = Math.max(0, joueur.vie - en.degats * DT);
                }
            }
        }
        if (pvie <= 0) {
            if (revivesRestants > 0) {
                revivre();
            } else {
                terminer(false);
                return false;
            }
        }
        if (bossTue) {
            if (infini) {
                bossVaincuInfini();
            } else {
                terminer(true);
                return false;
            }
        }
        return true;
    }

    /** Inflige des dégâts au joueur, sauf s'il est invulnérable (esquive, coup récent) ou protégé. */
    boolean blesserJoueur(double degatsRecus, double invulnerabilite) {
        if (invuln > 0 || tBouclier > 0) return false;
        pvie -= degatsRecus;
        invuln = invulnerabilite;
        shake = 10;
        degatsSubis = true;
        audio.degat();
        explosion(px, py, 12, new Color(255, 120, 160));
        return true;
    }

    /** « Seconde chance » (amélioration permanente) : le joueur revient avec 40 % de vie. */
    private void revivre() {
        revivesRestants--;
        pvie = pvieMax * 0.4;
        invuln = 3;
        novaX = px;
        novaY = py;
        novaT = 0.6;
        shake = 14;
        for (Ennemi en : ennemis) {
            if (!en.boss && Math.hypot(en.x - px, en.y - py) < 420) blesser(en, degats * 6);
        }
        tirsEnnemis.clear();
        zones.removeIf(z -> z.ennemie && !z.explosee);
        explosion(px, py, 40, new Color(255, 230, 140));
        audio.pouvoir();
        annonce("Seconde chance !");
    }

    /** Mode Infini : le boss est vaincu mais la partie continue, avec un boss plus solide ensuite. */
    private void bossVaincuInfini() {
        bossTue = false;
        bossNum++;
        if (!victoireAcquise) {
            victoireAcquise = true;
            validerVictoire();
        }
        if (bossNum >= 3) prog.debloquer("infini");
        prochainBoss = temps + 90;
        tirsEnnemis.clear();
        zones.removeIf(z -> z.ennemie);
        pvie = Math.min(pvieMax, pvie + pvieMax * 0.3);
        shake = 14;
        annonce(nouveauMonde ? "Boss vaincu ! Monde suivant débloqué, on continue !"
                : "Boss vaincu ! La partie continue...");
    }

    /** Esquive : petit bond rapide dans la direction du déplacement, invulnérable pendant l'action. */
    public boolean activerEsquive() {
        if (carte == null || esquiveRecharge > 0 || esquiveT > 0) return false;
        double n = Math.hypot(dirX, dirY);
        if (n < 1e-6) {
            dirX = 1;
            dirY = 0;
            n = 1;
        }
        esquiveDx = dirX / n;
        esquiveDy = dirY / n;
        esquiveT = ESQUIVE_DUREE;
        esquiveDepuis = 0;
        esquiveRecharge = ESQUIVE_RECHARGE * (tTurbo > 0 ? 0.6 : 1.0);
        invuln = Math.max(invuln, ESQUIVE_DUREE + 0.12);
        esquivesRun++;
        prog.totalEsquives++;
        prog.verifierGlobal();
        explosion(px, py, 8, Color.WHITE);
        audio.esquive();
        return true;
    }

    private void majTirsEnnemis() {
        for (int i = tirsEnnemis.size() - 1; i >= 0; i--) {
            TirEnnemi t = tirsEnnemis.get(i);
            t.x += t.vx * DT;
            t.y += t.vy * DT;
            t.vie -= DT;
            boolean retirer = t.vie <= 0 || t.x < -20 || t.x > carte.largeur + 20
                    || t.y < -20 || t.y > carte.hauteur + 20 || carte.libre(t.x, t.y, 4);
            if (!retirer && Math.hypot(px - t.x, py - t.y) < t.rayon + RAYON_JOUEUR * 0.75) {
                if (tBouclier > 0) { // le bouclier absorbe le projectile
                    retirer = true;
                    explosion(t.x, t.y, 4, Color.WHITE);
                } else if (invuln <= 0) { // pendant l'esquive, le projectile passe à travers
                    blesserJoueur(t.degats, 0.7);
                    retirer = true;
                }
            }
            if (!retirer) {
                for (JoueurReseau joueur : joueursReseau) {
                    if (joueur.vie > 0 && joueur.esquiveT <= 0
                            && Math.hypot(joueur.x - t.x, joueur.y - t.y) < t.rayon + RAYON_JOUEUR * 0.75) {
                        joueur.vie = Math.max(0, joueur.vie - t.degats);
                        retirer = true;
                        break;
                    }
                }
            }
            if (retirer) tirsEnnemis.remove(i);
        }
    }

    /** Zones à retardement : pilonnage du boss (blesse le joueur) et météores (blessent les ennemis). */
    private void majZones() {
        for (int i = zones.size() - 1; i >= 0; i--) {
            ZoneDanger z = zones.get(i);
            if (z.explosee) {
                z.apres -= DT;
                if (z.apres <= 0) zones.remove(i);
                continue;
            }
            z.delai -= DT;
            if (z.delai > 0) continue;
            z.explosee = true;
            z.apres = 0.3;
            if (z.ennemie) {
                if (Math.hypot(px - z.x, py - z.y) < z.rayon + RAYON_JOUEUR * 0.5) blesserJoueur(z.degats, 0.8);
                for (JoueurReseau joueur : joueursReseau) {
                    if (joueur.vie > 0 && joueur.esquiveT <= 0
                            && Math.hypot(joueur.x - z.x, joueur.y - z.y) < z.rayon + RAYON_JOUEUR * 0.5) {
                        joueur.vie = Math.max(0, joueur.vie - z.degats);
                    }
                }
                shake = Math.max(shake, 6);
                explosion(z.x, z.y, 14, new Color(255, 120, 90));
            } else {
                for (Ennemi c : ennemis) {
                    if (Math.hypot(c.x - z.x, c.y - z.y) <= z.rayon + c.rayon * 0.5) blesser(c, z.degats);
                }
                explosion(z.x, z.y, z.evolue ? 22 : 14, new Color(255, 190, 90));
                shake = Math.max(shake, 2);
            }
        }
    }

    /** Rayon de l'aura de sucre (arme secondaire), selon son niveau et son évolution. */
    public double rayonAura() {
        int i = ArmeSecondaire.AURA.ordinal();
        return 62 + 13 * niveauArme[i] + (evolueeArme[i] ? 40 : 0);
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
                xpReste += g.valeur * (tXp > 0 ? 2 : 1) * gainXp * modXp;
                int gagne = (int) xpReste;
                xp += gagne;
                xpReste -= gagne;
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
        initialiserEnnemi(e, type, boss);
        ennemis.add(e);
    }

    /** Donne à un ennemi ses statistiques (vie, vitesse, dégâts) selon son type, le temps et la difficulté. */
    private void initialiserEnnemi(Ennemi e, TypeEnnemi type, boolean boss) {
        Monde mo = monde;
        double diff = diff();
        e.type = type;
        e.monde = mo.id;
        e.boss = boss;
        e.sens = rnd.nextBoolean() ? 1 : -1;
        e.col = boss ? mo.bossCol : mo.palette[rnd.nextInt(mo.palette.length)];
        double echelle = (1 + temps / 90.0) * diff * modPv;   // plus costaud avec le temps et les mondes
        double vit = (1 + (diff - 1) * 0.25) * modVitEnn;
        double dgt = 1 + (diff - 1) * 0.5;
        if (boss) {
            e.pvMax = mo.bossPv * (1 + 0.7 * bossNum) * (cauchemar ? 1.3 : 1.0);
            e.rayon = 48;
            e.vitesse = 55 * vit;
            e.degats = 25 * dgt;
            e.cd = 2.5;
            e.phase = 1;
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
        } else if (type == TypeEnnemi.TIREUR) {
            e.pvMax = 28 * echelle;
            e.rayon = 14;
            e.vitesse = 70 * vit;
            e.degats = 6 * dgt;
            e.cd = 1.2 + rnd.nextDouble();
        } else if (type == TypeEnnemi.KAMIKAZE) {
            e.pvMax = 14 * echelle;
            e.rayon = 13;
            e.vitesse = 125 * vit;
            e.degats = 5 * dgt;
        } else {
            e.pvMax = 10 * echelle;
            e.rayon = 11;
            e.vitesse = 105 * vit;
            e.degats = 8 * dgt;
        }
        e.pv = e.pvMax;
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

    /** Console : donne un niveau à une arme secondaire (0 aura, 1 météores, 2 éclairs). */
    public void debugArme(int arme, int niveau) {
        if (arme < 0 || arme >= niveauArme.length) return;
        niveauArme[arme] = Math.clamp(niveau, 0, ArmeSecondaire.NIV_MAX);
        minuteurArme[arme] = 0.3;
    }

    /** Console : remplit les conditions d'évolution (elles seront proposées au prochain niveau). */
    public void debugEvolutions() {
        for (ArmeSecondaire a : ArmeSecondaire.values()) {
            niveauArme[a.ordinal()] = ArmeSecondaire.NIV_MAX;
            ameliosPrises.put(a.ameliorationRequise, a.nbRequis);
        }
        ameliosPrises.put("Corne puissante", 2);
        etoiles = Math.max(etoiles, 6);
    }

    public boolean debugElite() {
        return carte != null && spawnElite();
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

    Ennemi plusProche(double distMax) {
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
        t.degats = degats * (personnageTireur.typeArme == TypeArme.EPEE ? 1.8 : 1);
        t.perfo = personnageTireur.typeArme == TypeArme.BOOMERANG ? 2 : 0;
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

    void blesser(Ennemi e, double d) {
        e.pv -= d;
        e.flash = 0.08;
    }

    private void mort(Ennemi e) {
        kills++;
        prog.totalKills++;
        audio.mort();
        explosion(e.x, e.y, e.boss ? 80 : 10, e.col);
        int nb = e.boss ? 20 : (e.elite ? 6 : 1);
        for (int i = 0; i < nb; i++) {
            Gemme g = new Gemme();
            g.x = e.x + (e.boss || e.elite ? rnd.nextGaussian() * 30 : 0);
            g.y = e.y + (e.boss || e.elite ? rnd.nextGaussian() * 30 : 0);
            g.valeur = e.boss ? 3 : (e.elite || e.type == TypeEnnemi.GUIMAUVE || e.type == TypeEnnemi.TIREUR ? 2 : 1);
            gemmes.add(g);
        }
        if (e.boss) {
            bossTue = true;
            tirsEnnemis.clear();
            zones.removeIf(z -> z.ennemie);
            return;
        }
        if (e.elite && bonus.size() < 8) { // une élite lâche toujours un pouvoir
            Bonus b = new Bonus();
            b.x = Math.clamp(e.x, 30, carte.largeur - 30);
            b.y = Math.clamp(e.y, 30, carte.hauteur - 30);
            b.type = tirerPouvoir();
            b.vie = 18;
            bonus.add(b);
            shake = Math.max(shake, 5);
            return;
        }
        // chance de lâcher un pouvoir : les guimauves (costaudes) en lâchent plus, les gommes moins
        double mult = e.type == TypeEnnemi.GUIMAUVE ? 2.0
                : e.type == TypeEnnemi.GOMME ? 0.6 : 1.0;
        if (bossApparu) mult *= 1.5; // pendant le combat de boss, ça tombe plus souvent
        if (bonus.size() < 5 && rnd.nextDouble() < Constantes.TAUX_DROP * mult * chanceBonus * modDrop) {
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
        ajouterArmesSecondaires(tous);
        Collections.shuffle(tous, rnd);
        List<Amelio> resultat = new ArrayList<>(tous.subList(0, 3));
        // une évolution disponible est toujours proposée (elle remplace la première carte)
        List<Amelio> evolutions = evolutionsDisponibles();
        if (!evolutions.isEmpty()) resultat.set(0, evolutions.get(rnd.nextInt(evolutions.size())));
        return resultat;
    }

    private int compte(String cle) {
        return ameliosPrises.getOrDefault(cle, 0);
    }

    /** Nouvelles armes secondaires (niveau 0) ou niveau supérieur de celles qu'on possède déjà. */
    private void ajouterArmesSecondaires(List<Amelio> tous) {
        for (ArmeSecondaire a : ArmeSecondaire.values()) {
            final int i = a.ordinal();
            int niv = niveauArme[i];
            if (niv >= ArmeSecondaire.NIV_MAX) continue;
            String desc = niv == 0 ? "NOUVELLE ARME\n" + a.desc
                    : "Niveau " + (niv + 1) + " / " + ArmeSecondaire.NIV_MAX + "\n+ dégâts et + de puissance";
            tous.add(new Amelio(a.nom, desc, a.couleur, () -> {
                niveauArme[i]++;
                if (niveauArme[i] == 1) minuteurArme[i] = 0.4;
            }, "arme:" + a.name(), false));
        }
    }

    /** Évolutions possibles : arme au niveau max + la bonne amélioration prise assez de fois. */
    private List<Amelio> evolutionsDisponibles() {
        List<Amelio> liste = new ArrayList<>();
        Color or = new Color(255, 215, 90);
        for (ArmeSecondaire a : ArmeSecondaire.values()) {
            final int i = a.ordinal();
            if (niveauArme[i] < ArmeSecondaire.NIV_MAX || evolueeArme[i]) continue;
            if (compte(a.ameliorationRequise) < a.nbRequis) continue;
            liste.add(new Amelio(a.nomEvo, "ÉVOLUTION\n" + a.descEvo, or, () -> {
                evolueeArme[i] = true;
                evolutionTerminee(a.nomEvo);
            }, "evo:" + a.name(), true));
        }
        if (!constellation && etoiles >= 6 && compte("Corne puissante") >= 2) {
            liste.add(new Amelio("Constellation", "ÉVOLUTION\n+2 étoiles, orbite large,\ndégâts x2", or, () -> {
                constellation = true;
                etoiles += 2;
                rayonOrbite = 108;
                multOrbite = 2.2;
                evolutionTerminee("Constellation");
            }, "evo:CONSTELLATION", true));
        }
        return liste;
    }

    private void evolutionTerminee(String nom) {
        evolutionsRun++;
        prog.debloquer("evolution");
        shake = 12;
        explosion(px, py, 40, new Color(255, 225, 120));
        annonce("ÉVOLUTION : " + nom + " !");
    }

    /** Relance les trois choix de niveau (nombre limité par partie, augmenté par la boutique). */
    public boolean relancer() {
        if (choix.isEmpty() || relances <= 0) return false;
        relances--;
        choix = tirerChoix();
        audio.clic();
        return true;
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
        Amelio a = choix.get(i);
        a.effet.run();
        ameliosPrises.merge(a.cle, 1, Integer::sum);
        choix = new ArrayList<>();
        audio.clic();
        if (xpSuivant > 0 && xp >= xpSuivant) niveauSup();
    }
}
