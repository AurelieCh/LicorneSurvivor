package org.java.ressources.ui;

import org.java.ressources.donnees.Personnage;
import org.java.ressources.donnees.Progression;
import org.java.ressources.donnees.Reglages;
import org.java.ressources.donnees.Sauvegarde;
import org.java.ressources.jeu.Etat;
import org.java.ressources.jeu.Partie;
import org.java.ressources.jeu.Session;
import org.java.ressources.rendu.RenduInterface;
import org.java.ressources.rendu.RenduMonde;
import org.java.ressources.rendu.Zones;
import org.java.ressources.reseau.ReseauLan;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.util.*;

import static org.java.ressources.donnees.Constantes.*;

/**
 * Le panneau de jeu : boucle de temps, clavier, souris, passage d'un écran à l'autre.
 * La logique est dans Partie, le dessin dans rendu.*
 */
public class Jeu extends JPanel implements ActionListener, KeyListener, MouseListener, MouseMotionListener {
    private final Session s = new Session();
    private final Partie partie = s.partie;
    private final Set<Integer> touches = new HashSet<>();
    private final RenduMonde renduMonde = new RenduMonde();
    private final RenduInterface renduUi = new RenduInterface();

    private Fenetre fenetre;
    private long ignorerFocusJusqua = 0;
    private double echelle = 1, offX = 0, offY = 0;    // mise à l'échelle uniforme avec bandes éventuelles
    private boolean curseurMasque = false;
    private Cursor curseurVide;
    private int curseurActif = -1;                      // barre de volume en cours de glissement

    /** Plus de pas de logique que ça par image affichée : au-delà, on laisse le jeu ralentir plutôt que de s'étrangler. */
    private static final int PAS_MAX = 5;
    private long dernierTop = 0;
    /** Temps réel écoulé qui n'a pas encore été « joué » par la logique (en secondes, peut être légèrement négatif). */
    private double reste = 0;
    private final javax.swing.Timer timer;
    private boolean console;
    private String consoleTexte = "";
    private final Deque<String> consoleLignes = new ArrayDeque<>();
    private int reseauCadence;
    private int derniereEntreeReseau = Integer.MIN_VALUE;
    private int dernierSourisXReseau;
    private int dernierSourisYReseau;
    private boolean derniereCompetenceReseau;
    private int dernierPersonnageReseau = -1;


    public Jeu() {
        setPreferredSize(new Dimension(W, H));
        setFocusable(true);
        addKeyListener(this);
        addMouseListener(this);
        addMouseMotionListener(this);
        // pause automatique si la fenêtre perd le focus (réglable)
        addFocusListener(new FocusAdapter() {
            @Override
            public void focusLost(FocusEvent e) {
                touches.clear();
                if (s.etat == Etat.JEU && s.reg.pauseAuto && System.currentTimeMillis() > ignorerFocusJusqua) {
                    s.etat = Etat.PAUSE;
                }
            }
        });
        Sauvegarde.charger(s.prog, s.reg);
        s.audio.monde = s.mondeSel;
        s.audio.demarrer();
        s.prog.surDeblocage = su -> {
            if (s.popups.isEmpty()) s.popupTimer = 3.5;
            s.popups.add(su);
            s.audio.succes();
            sauver();
        };
        Runtime.getRuntime().addShutdownHook(new Thread(this::sauver));
        Runtime.getRuntime().addShutdownHook(new Thread(s.reseau::fermer));
        timer = new javax.swing.Timer(1000 / s.reg.fps, this); // boucle de jeu (30, 45 ou 60 images/s)
        timer.start();
    }

    private void selectionnerPersonnage(int direction) {
        int depart = s.prog.personnageSel;
        for (int i = 1; i <= Personnage.TOUS.length; i++) {
            int candidat = (depart + direction * i + Personnage.TOUS.length * 2) % Personnage.TOUS.length;
            if (s.prog.personnageDebloque(candidat)) {
                s.prog.personnageSel = candidat;
                s.audio.clic();
                return;
            }
        }
    }

    private void confirmerPersonnage() {
        sauver();
        s.etat = Etat.MENU;
        s.audio.clic();
    }

    public Session session() {
        return s;
    }

    public void setFenetre(Fenetre f) {
        this.fenetre = f;
    }

    void ignorerFocus(long ms) {
        ignorerFocusJusqua = System.currentTimeMillis() + ms;
    }

    private void sauver() {
        Sauvegarde.sauver(s.prog, s.reg);
    }

    private void consoleMessage(String message) {
        consoleLignes.addLast(message);
        while (consoleLignes.size() > 7) consoleLignes.removeFirst();
    }

    private void executerConsole(String ligne) {
        String[] a = ligne.trim().toLowerCase(Locale.ROOT).split("\\s+");
        if (a.length == 0 || a[0].isEmpty()) return;
        try {
            switch (a[0]) {
                case "help":
                    consoleMessage("lvl N | xp N | spawn TYPE [N] | boss | elite | kill | heal | sucre N | skill");
                    consoleMessage("arme I [niv] (0 aura 1 meteores 2 eclairs) | evo | esquive  (types : 4 tireur, 5 kamikaze)");
                    break;
                case "lvl":
                    partie.debugNiveau(a.length > 1 ? Integer.parseInt(a[1]) : partie.niveau + 1);
                    consoleMessage("Niveau : " + partie.niveau);
                    if (partie.issue == Partie.Issue.NIVEAU) console = false;
                    break;
                case "xp":
                    partie.xp += a.length > 1 ? Integer.parseInt(a[1]) : 1000;
                    consoleMessage("XP ajoutee.");
                    break;
                case "spawn":
                    int type = a.length > 1 ? Integer.parseInt(a[1]) : 0;
                    int nombre = a.length > 2 ? Integer.parseInt(a[2]) : 1;
                    int cree = partie.debugSpawn(type, nombre, false);
                    consoleMessage("Spawn : " + cree + " / " + nombre + " monstre(s).");
                    break;
                case "boss":
                    partie.debugSpawn(3, 1, true);
                    consoleMessage("Boss spawn.");
                    break;
                case "elite":
                    consoleMessage(partie.debugElite() ? "Elite spawn." : "Elite impossible.");
                    break;
                case "arme":
                    partie.debugArme(a.length > 1 ? Integer.parseInt(a[1]) : 0, a.length > 2 ? Integer.parseInt(a[2]) : 5);
                    consoleMessage("Arme secondaire mise a jour.");
                    break;
                case "evo":
                    partie.debugEvolutions();
                    consoleMessage("Conditions d'evolution remplies : tape lvl pour les voir.");
                    break;
                case "esquive":
                    consoleMessage(partie.activerEsquive() ? "Esquive !" : "Esquive en recharge.");
                    break;
                case "kill":
                    partie.debugTuerTous();
                    consoleMessage("Tous les monstres sont marques morts.");
                    break;
                case "heal":
                    partie.pvie = partie.pvieMax;
                    consoleMessage("Vie restauree.");
                    break;
                case "sucre":
                    s.prog.sucre += a.length > 1 ? Integer.parseInt(a[1]) : 1000;
                    consoleMessage("Sucre : " + s.prog.sucre);
                    break;
                case "skill":
                    consoleMessage(partie.activerCompetence() ? "Competence activee." : "Competence en recharge.");
                    break;
                case "clear":
                    consoleLignes.clear();
                    break;
                default:
                    consoleMessage("Commande inconnue. Tape help.");
                    break;
            }
        } catch (NumberFormatException ex) {
            consoleMessage("Nombre invalide.");
        }
    }

    // ------------------------------------------------------------------
    // Boucle de jeu
    // ------------------------------------------------------------------
    @Override
    public void actionPerformed(ActionEvent e) {
        // La logique avance au rythme du temps réel (60 pas/s), indépendamment de la vitesse d'affichage :
        // si une image met longtemps à se dessiner, on joue plusieurs pas d'un coup au lieu de ralentir le jeu.
        long maintenant = System.nanoTime();
        double ecoule = dernierTop == 0 ? DT : (maintenant - dernierTop) / 1e9;
        dernierTop = maintenant;
        reste += Math.min(ecoule, 0.25);
        int pas = (int) Math.round(reste / DT);
        if (pas > PAS_MAX) {
            pas = PAS_MAX;
            reste = 0; // trop de retard : on abandonne le surplus
        } else {
            reste -= pas * DT;
        }
        for (int i = 0; i < pas; i++) avancer();

        // en mode souris, on cache le curseur pendant la partie (on dessine notre propre cible)
        boolean masquer = s.reg.modeSouris && s.etat == Etat.JEU;
        if (masquer != curseurMasque) {
            curseurMasque = masquer;
            setCursor(masquer ? curseurVide() : Cursor.getDefaultCursor());
        }
        repaint();
    }

    /** Un pas de logique (1/60 s). */
    private void avancer() {
        s.anim += DT;
        if (s.etat == Etat.MULTI && s.reseau.lancementDemande()) {
            s.reseau.consommerLancement();
            lancerReseau(s.reseau.mondeLancement(), s.reseau.seedLancement());
        }
        s.audio.calme = s.etat == Etat.PAUSE || (s.etat == Etat.PARAMS && s.etatRetour == Etat.PAUSE);
        if (s.etat == Etat.JEU) {
            partie.cibleX = s.sourisX;
            partie.cibleY = s.sourisY;
            if (s.reseau.role() == ReseauLan.Role.CLIENT) {
                envoyerEntreeReseau();
                String sync = s.reseau.prendreDernierSync();
                if (!sync.isBlank()) {
                    partie.appliquerJoueursReseau(sync, s.reseau.idLocal());
                }
                partie.lisserJoueursReseau();
                partie.majCamera(false);
            } else {
                partie.maj(touches);
            }
            if (s.reseau.role() == ReseauLan.Role.HOTE) {
                partie.majJoueursReseau(s.reseau.prendreEntrees());
                if (++reseauCadence >= 3) {
                    reseauCadence = 0;
                    s.reseau.diffuserSync(partie.serialiserJoueursReseau());
                }
            }
            if (s.reseau.role() == ReseauLan.Role.HOTE && partie.issue == Partie.Issue.FIN) {
                s.reseau.diffuserFin(partie.victoire);
            }
            if (s.reseau.role() != ReseauLan.Role.CLIENT) traiterIssue();
            if (s.reseau.role() == ReseauLan.Role.CLIENT && s.reseau.finDemandee()) {
                s.reseau.consommerFin();
                partie.victoire = s.reseau.victoireReseau();
                s.etat = Etat.FIN;
            }
        }
        if (s.etat != Etat.PAUSE && s.etat != Etat.PARAMS) partie.majParticules();
        if (!s.popups.isEmpty()) {
            s.popupTimer -= DT;
            if (s.popupTimer <= 0) {
                s.popups.removeFirst();
                s.popupTimer = 3.5;
            }
        }
    }

    private int touchesReseau() {
        int masque = 0;
        if (touches.contains(KeyEvent.VK_Q) || touches.contains(KeyEvent.VK_A) || touches.contains(KeyEvent.VK_LEFT)) masque |= 1;
        if (touches.contains(KeyEvent.VK_D) || touches.contains(KeyEvent.VK_RIGHT)) masque |= 2;
        if (touches.contains(KeyEvent.VK_Z) || touches.contains(KeyEvent.VK_W) || touches.contains(KeyEvent.VK_UP)) masque |= 4;
        if (touches.contains(KeyEvent.VK_S) || touches.contains(KeyEvent.VK_DOWN)) masque |= 8;
        if (touches.contains(KeyEvent.VK_SPACE) || touches.contains(KeyEvent.VK_SHIFT)) masque |= 16;
        return masque;
    }

    private void envoyerEntreeReseau() {
        int touches = touchesReseau();
        int sourisX = (int) s.sourisX;
        int sourisY = (int) s.sourisY;
        boolean competence = this.touches.contains(KeyEvent.VK_E);
        int personnage = s.prog.personnageSel;
        if (touches == derniereEntreeReseau && sourisX == dernierSourisXReseau
                && sourisY == dernierSourisYReseau && competence == derniereCompetenceReseau
                && personnage == dernierPersonnageReseau) {
            return;
        }
        s.reseau.envoyerEntree(touches, sourisX, sourisY, competence, personnage);
        derniereEntreeReseau = touches;
        dernierSourisXReseau = sourisX;
        dernierSourisYReseau = sourisY;
        derniereCompetenceReseau = competence;
        dernierPersonnageReseau = personnage;
    }

    private Cursor curseurVide() {
        if (curseurVide == null) {
            try {
                curseurVide = Toolkit.getDefaultToolkit().createCustomCursor(
                        new BufferedImage(16, 16, BufferedImage.TYPE_INT_ARGB), new Point(0, 0), "vide");
            } catch (Exception ex) {
                curseurVide = Cursor.getDefaultCursor();
            }
        }
        return curseurVide;
    }

    /** Réagit à ce que la partie vient de signaler : niveau gagné ou fin de partie. */
    private void traiterIssue() {
        switch (partie.issue) {
            case NIVEAU:
                s.etat = Etat.NIVEAU;
                break;
            case FIN:
                s.etat = Etat.FIN;
                sauver();
                break;
            default:
                break;
        }
        partie.issue = Partie.Issue.AUCUNE;
    }

    // ------------------------------------------------------------------
    // Dessin
    // ------------------------------------------------------------------
    @Override
    protected void paintComponent(Graphics g0) {
        super.paintComponent(g0);
        Graphics2D g = (Graphics2D) g0;
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

        // Le jeu tient entièrement dans la fenêtre sans déformer les sprites.
        // Les bandes sur les côtés ou en haut restent préférables à un recadrage
        // qui ferait sortir l'interface et les éléments du monde de l'écran.
        echelle = Math.min(getWidth() / (double) W, getHeight() / (double) H);
        offX = (getWidth() - W * echelle) / 2;
        offY = (getHeight() - H * echelle) / 2;
        g.translate(offX, offY);
        g.scale(echelle, echelle);
        g.clipRect(0, 0, W, H);

        if (s.surLeJeu()) {
            renduMonde.dessiner(g, s);
            renduUi.hud(g, s);
            if (s.etat == Etat.NIVEAU) renduUi.niveau(g, s);
            if (s.etat == Etat.PAUSE) renduUi.pause(g, s);
            if (s.etat == Etat.FIN) renduUi.fin(g, s);
            if (s.etat == Etat.PARAMS) renduUi.params(g, s);
            if (s.etat == Etat.COMMANDES) renduUi.commandes(g, s);
            if (console) renduUi.console(g, consoleTexte, consoleLignes);
        } else {
            renduUi.fondMenu(g, s);
            if (s.etat == Etat.MENU) renduUi.menu(g, s);
            else if (s.etat == Etat.SUCCES) renduUi.succes(g, s);
            else if (s.etat == Etat.PERSONNAGES) renduUi.personnages(g, s);
            else if (s.etat == Etat.COMMANDES) renduUi.commandes(g, s);
            else if (s.etat == Etat.MULTI) renduUi.multi(g, s);
            else if (s.etat == Etat.BOUTIQUE) renduUi.boutique(g, s);
            else renduUi.params(g, s);
        }
        if (!s.popups.isEmpty()) renduUi.popup(g, s);
    }

    // ------------------------------------------------------------------
    // Actions des menus
    // ------------------------------------------------------------------
    private void lancer() {
        boolean defi = s.defiSel;
        partie.modeJeu = s.modeEffectif();
        partie.defiJour = defi;
        lancer(s.mondeSel, defi ? Partie.graineDuJour(s.mondeSel) : System.nanoTime());
    }

    /** Lancement d'une partie en coop LAN : toujours en mode normal (les clients ne connaissent pas les modes). */
    private void lancerReseau(int monde, long seed) {
        partie.modeJeu = Partie.MODE_NORMAL;
        partie.defiJour = false;
        lancer(monde, seed);
    }

    private void lancer(int monde, long seed) {
        s.mondeSel = monde;
        partie.lancer(monde, Personnage.TOUS[s.prog.personnageSel], seed);
        s.audio.monde = s.mondeSel;
        s.etat = Etat.JEU;
        s.audio.clic();
    }

    private void ouvrirMulti() {
        s.etat = Etat.MULTI;
        s.reseauMessage = "";
        s.audio.clic();
    }

    private void quitterJeu() {
        int choix = JOptionPane.showConfirmDialog(this,
                "Quitter Licorne Survivor ?",
                "Quitter le jeu",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.QUESTION_MESSAGE);
        if (choix == JOptionPane.YES_OPTION) {
            sauver();
            s.fermerReseau();
            if (timer != null) timer.stop();
            if (fenetre != null) fenetre.fermer();
        }
    }

    private void hebergerLan() {
        try {
            s.reseau.heberger();
            s.reseauMessage = "Salon créé. Les autres joueurs peuvent le trouver.";
            s.audio.clic();
        } catch (java.io.IOException ex) {
            s.reseauMessage = "Impossible d'ouvrir le salon : port réseau indisponible.";
        }
    }

    private void rechercherLan() {
        s.reseauMessage = "Recherche des parties sur le réseau...";
        Thread recherche = new Thread(() -> {
            java.util.List<ReseauLan.PartieTrouvee> resultats = ReseauLan.rechercher(1800);
            javax.swing.SwingUtilities.invokeLater(() -> {
                s.partiesLan = resultats;
                s.reseauMessage = resultats.isEmpty()
                        ? "Aucune partie trouvée. Vérifie le même Wi-Fi et le pare-feu."
                        : resultats.size() + " partie(s) trouvée(s).";
            });
        }, "lan-recherche");
        recherche.setDaemon(true);
        recherche.start();
    }

    private void rejoindreLan(int index) {
        if (index < 0 || index >= s.partiesLan.size()) return;
        try {
            s.reseau.rejoindre(s.partiesLan.get(index));
            s.reseauMessage = "Connexion au salon...";
            s.partiesLan = java.util.List.of();
            s.audio.clic();
        } catch (java.io.IOException ex) {
            s.reseauMessage = "Connexion impossible : la partie n'est plus disponible.";
        }
    }

    private void lancerLan() {
        if (s.reseau.role() == ReseauLan.Role.HOTE) {
            s.reseau.demanderLancement(s.mondeSel, System.nanoTime());
            s.audio.clic();
        }
    }

    private void changerPersonnage() {
        int depart = s.prog.personnageSel;
        for (int i = 1; i <= Personnage.TOUS.length; i++) {
            int candidat = (depart + i) % Personnage.TOUS.length;
            if (s.prog.personnageDebloque(candidat)) {
                s.prog.personnageSel = candidat;
                s.audio.clic();
                sauver();
                return;
            }
        }
    }

    private void changerMonde(int d) {
        int n = s.mondeSel + d;
        if (n >= 0 && n <= s.prog.mondeMax && n != s.mondeSel) {
            s.mondeSel = n;
            s.audio.monde = n;
            s.audio.clic();
        }
    }

    private void ouvrirBoutique() {
        s.etat = Etat.BOUTIQUE;
        s.audio.clic();
    }

    private void acheter(int i) {
        if (s.prog.acheter(i)) {
            s.audio.clic();
            sauver();
        }
    }

    private void changerMode() {
        s.modeSel = (s.modeSel + 1) % 3;
        if (!s.prog.gagne[s.mondeSel]) {
            s.modeSel = 0; // verrouillé tant que le monde n'est pas terminé
        }
        s.audio.clic();
    }

    private void basculerDefi() {
        s.defiSel = !s.defiSel;
        s.audio.clic();
    }

    private void relancerChoix() {
        partie.relancer();
    }

    private void choisirAmelioration(int i) {
        partie.appliquerChoix(i);
        s.etat = Etat.JEU;
        traiterIssue();
    }

    private void abandonner() {
        partie.terminer(false);
        traiterIssue();
    }

    private void ouvrirParams(Etat retour) {
        s.etatRetour = retour;
        s.etat = Etat.PARAMS;
        s.paramSel = 0;
        curseurActif = -1;
        s.confirmReset = false;
        s.audio.clic();
    }

    private void fermerParams() {
        s.confirmReset = false;
        curseurActif = -1;
        s.etat = s.etatRetour;
        s.audio.clic();
        sauver();
    }

    private void basculerPleinEcran() {
        s.reg.pleinEcran = !s.reg.pleinEcran;
        if (fenetre != null) fenetre.appliquer();
        s.audio.clic();
        sauver();
    }

    /** Valide la ligne i du menu Paramètres. */
    private void activer(int i) {
        Reglages r = s.reg;
        switch (i) {
            case 0:
                r.musique = !r.musique;
                break;
            case 2:
                basculerPleinEcran();
                return;
            case 3:
                r.modeSouris = !r.modeSouris;
                break;
            case 4:
                r.secousses = !r.secousses;
                break;
            case 5:
                r.effets = !r.effets;
                break;
            case 6:
                r.pauseAuto = !r.pauseAuto;
                break;
            case 7:
                r.miniCarte = !r.miniCarte;
                break;
            case 8:
                changerFps(1);
                break;
            case 9:
                if (s.confirmReset) {
                    s.prog.reinitialiser();
                    s.popups.clear();
                    s.mondeSel = 0;
                    s.audio.monde = 0;
                    s.confirmReset = false;
                } else {
                    s.confirmReset = true;
                    s.audio.clic();
                    return;
                }
                break;
            case 10:
                s.etat = Etat.COMMANDES;
                s.audio.clic();
                return;
            default:
                break;
        }
        s.audio.clic();
        sauver();
    }

    /** Gauche / droite sur une ligne de paramètre. */
    private void ajuster(int i, int dir) {
        Reglages r = s.reg;
        if (i == 0) {
            r.volMusique = Math.clamp(Math.round((r.volMusique + dir * 0.05) * 20) / 20.0, 0, 1);
            if (r.volMusique > 0) r.musique = true;
            sauver();
        } else if (i == 1) {
            r.volSfx = Math.clamp(Math.round((r.volSfx + dir * 0.05) * 20) / 20.0, 0, 1);
            s.audio.clic();
            sauver();
        } else if (i == 8) {
            changerFps(dir);
            s.audio.clic();
            sauver();
        } else if (i >= 2 && i <= 7) {
            activer(i);
        }
    }

    /** Passe au choix de FPS suivant (dir = 1) ou précédent (dir = -1) : 30, 45, 60, 120, 144, 240. */
    private void changerFps(int dir) {
        int[] choix = Reglages.FPS_CHOIX;
        int i = 0;
        for (int k = 0; k < choix.length; k++) if (choix[k] == s.reg.fps) i = k;
        s.reg.fps = choix[(i + dir + choix.length) % choix.length];

        if (timer != null) {
            timer.setDelay(1000 / s.reg.fps);
            // Évite de rejouer le temps écoulé avant le changement de cadence.
            reste = 0;
            dernierTop = System.nanoTime();
        }
    }

    private void reglerCurseur(int i, int mx) {
        Rectangle r = Zones.ligneParam(i);
        double v = Math.clamp((mx - (r.x + 300)) / 250.0, 0, 1);
        v = Math.round(v * 20) / 20.0;
        if (i == 0) {
            s.reg.volMusique = v;
            if (v > 0) s.reg.musique = true;
        } else {
            s.reg.volSfx = v;
        }
    }

    // ------------------------------------------------------------------
    // Clavier
    // ------------------------------------------------------------------
    private int numChoix(KeyEvent e) {
        int k = e.getKeyCode();
        char c = e.getKeyChar();
        if (k == KeyEvent.VK_1 || k == KeyEvent.VK_NUMPAD1 || c == '1' || c == '&') return 0;
        if (k == KeyEvent.VK_2 || k == KeyEvent.VK_NUMPAD2 || c == '2' || c == 'é') return 1;
        if (k == KeyEvent.VK_3 || k == KeyEvent.VK_NUMPAD3 || c == '3' || c == '"') return 2;
        return -1;
    }

    /** Chiffre tapé de 1 à 9 (renvoie 0 à 8), y compris sur la rangée du haut d'un clavier AZERTY. */
    private int chiffre(KeyEvent e) {
        int k = e.getKeyCode();
        char c = e.getKeyChar();
        if (k >= KeyEvent.VK_1 && k <= KeyEvent.VK_9) return k - KeyEvent.VK_1;
        if (k >= KeyEvent.VK_NUMPAD1 && k <= KeyEvent.VK_NUMPAD9) return k - KeyEvent.VK_NUMPAD1;
        if (c >= '1' && c <= '9') return c - '1';
        return switch (c) {
            case '&' -> 0;
            case 'é' -> 1;
            case '"' -> 2;
            case '\'' -> 3;
            case '(' -> 4;
            case '-' -> 5;
            case 'è' -> 6;
            case '_' -> 7;
            case 'ç' -> 8;
            default -> -1;
        };
    }

    @Override
    public void keyPressed(KeyEvent e) {
        if (e.getKeyCode() == KeyEvent.VK_F2) {
            console = !console;
            if (console) consoleMessage("Console dev activee. Tape help.");
            return;
        }
        if (console) {
            if (e.getKeyCode() == KeyEvent.VK_ESCAPE) {
                console = false;
            } else if (e.getKeyCode() == KeyEvent.VK_ENTER) {
                if (!consoleTexte.isBlank()) {
                    consoleMessage("> " + consoleTexte);
                    executerConsole(consoleTexte);
                }
                consoleTexte = "";
            } else if (e.getKeyCode() == KeyEvent.VK_BACK_SPACE && !consoleTexte.isEmpty()) {
                consoleTexte = consoleTexte.substring(0, consoleTexte.length() - 1);
            }
            return;
        }
        int k = e.getKeyCode();
        touches.add(k);
        int n = numChoix(e);
        boolean valider = k == KeyEvent.VK_SPACE || k == KeyEvent.VK_ENTER;

        if (k == KeyEvent.VK_M) {
            s.reg.musique = !s.reg.musique;
            sauver();
            return;
        }
        if (k == KeyEvent.VK_F11) {
            basculerPleinEcran();
            return;
        }
        switch (s.etat) {
            case MENU:
                if (valider) lancer();
                else if (k == KeyEvent.VK_LEFT) changerMonde(-1);
                else if (k == KeyEvent.VK_RIGHT) changerMonde(1);
                else if (k == KeyEvent.VK_C) s.etat = Etat.PERSONNAGES;
                else if (k == KeyEvent.VK_L) ouvrirMulti();
                else if (k == KeyEvent.VK_X) quitterJeu();
                else if (k == KeyEvent.VK_S) s.etat = Etat.SUCCES;
                else if (k == KeyEvent.VK_O) ouvrirParams(Etat.MENU);
                else if (k == KeyEvent.VK_B) ouvrirBoutique();
                else if (k == KeyEvent.VK_N) changerMode();
                else if (k == KeyEvent.VK_J) basculerDefi();
                else if (n >= 0) acheter(n);
                break;
            case BOUTIQUE:
                if (k == KeyEvent.VK_ESCAPE || k == KeyEvent.VK_B) {
                    s.etat = Etat.MENU;
                    s.audio.clic();
                } else if (k == KeyEvent.VK_UP) {
                    s.boutiqueSel = (s.boutiqueSel + Progression.NB_META - 1) % Progression.NB_META;
                } else if (k == KeyEvent.VK_DOWN) {
                    s.boutiqueSel = (s.boutiqueSel + 1) % Progression.NB_META;
                } else if (k == KeyEvent.VK_ENTER || k == KeyEvent.VK_SPACE) {
                    acheter(s.boutiqueSel);
                } else if (chiffre(e) >= 0 && chiffre(e) < Progression.NB_META) {
                    s.boutiqueSel = chiffre(e);
                    acheter(s.boutiqueSel);
                }
                break;
            case JEU:
                if (k == KeyEvent.VK_P || k == KeyEvent.VK_ESCAPE) s.etat = Etat.PAUSE;
                else if (k == KeyEvent.VK_O) ouvrirParams(Etat.JEU);
                else if (k == KeyEvent.VK_E) partie.activerCompetence();
                else if ((k == KeyEvent.VK_SPACE || k == KeyEvent.VK_SHIFT)
                        && s.reseau.role() != ReseauLan.Role.CLIENT) partie.activerEsquive();
                break;
            case PAUSE:
                if (k == KeyEvent.VK_P || k == KeyEvent.VK_ESCAPE || valider) s.etat = Etat.JEU;
                else if (k == KeyEvent.VK_O) ouvrirParams(Etat.PAUSE);
                else if (k == KeyEvent.VK_X) abandonner();
                break;
            case NIVEAU:
                if (n >= 0) choisirAmelioration(n);
                else if (k == KeyEvent.VK_R) relancerChoix();
                break;
            case FIN:
                if (valider) s.etat = Etat.MENU;
                break;
            case SUCCES:
                if (valider || k == KeyEvent.VK_ESCAPE) s.etat = Etat.MENU;
                break;
            case PERSONNAGES:
                if (k == KeyEvent.VK_ESCAPE || k == KeyEvent.VK_C) s.etat = Etat.MENU;
                else if (k == KeyEvent.VK_LEFT || k == KeyEvent.VK_UP) selectionnerPersonnage(-1);
                else if (k == KeyEvent.VK_RIGHT || k == KeyEvent.VK_DOWN) selectionnerPersonnage(1);
                else if (valider) confirmerPersonnage();
                break;
            case COMMANDES:
                if (valider || k == KeyEvent.VK_ESCAPE) s.etat = Etat.PARAMS;
                break;
            case MULTI:
                if (k == KeyEvent.VK_ESCAPE || k == KeyEvent.VK_Q) {
                    s.fermerReseau();
                    s.etat = Etat.MENU;
                } else if (k == KeyEvent.VK_H && !s.reseau.estDansSalon()) {
                    hebergerLan();
                } else if (k == KeyEvent.VK_R && !s.reseau.estDansSalon()) {
                    rechercherLan();
                } else if (valider && s.reseau.role() == ReseauLan.Role.HOTE) {
                    lancerLan();
                } else if (n >= 0) {
                    rejoindreLan(n);
                }
                break;
            case PARAMS:
                if (k == KeyEvent.VK_ESCAPE) {
                    fermerParams();
                } else if (k == KeyEvent.VK_UP) {
                    s.paramSel = (s.paramSel + NB_PARAMS - 1) % NB_PARAMS;
                    s.confirmReset = false;
                } else if (k == KeyEvent.VK_DOWN) {
                    s.paramSel = (s.paramSel + 1) % NB_PARAMS;
                    s.confirmReset = false;
                } else if (k == KeyEvent.VK_LEFT) {
                    ajuster(s.paramSel, -1);
                } else if (k == KeyEvent.VK_RIGHT) {
                    ajuster(s.paramSel, 1);
                } else if (valider) {
                    activer(s.paramSel);
                }
                break;
            default:
                break;
        }
    }

    @Override
    public void keyReleased(KeyEvent e) {
        touches.remove(e.getKeyCode());
    }

    @Override
    public void keyTyped(KeyEvent e) {
        if (console && !Character.isISOControl(e.getKeyChar()) && consoleTexte.length() < 80) {
            consoleTexte += e.getKeyChar();
        }
    }

    // ------------------------------------------------------------------
    // Souris (les coordonnées sont converties dans le repère 960 x 640 du jeu)
    // ------------------------------------------------------------------
    private int lx(MouseEvent e) {
        return (int) ((e.getX() - offX) / echelle);
    }

    private int ly(MouseEvent e) {
        return (int) ((e.getY() - offY) / echelle);
    }

    @Override
    public void mouseMoved(MouseEvent e) {
        int mx = lx(e), my = ly(e);
        s.sourisX = mx;
        s.sourisY = my;
        if (s.etat == Etat.PARAMS) {
            for (int i = 0; i < NB_PARAMS; i++) {
                if (Zones.ligneParam(i).contains(mx, my) && s.paramSel != i) {
                    s.paramSel = i;
                    s.confirmReset = false;
                }
            }
        }
    }

    @Override
    public void mouseDragged(MouseEvent e) {
        s.sourisX = lx(e);
        s.sourisY = ly(e);
        if (s.etat == Etat.PARAMS && curseurActif >= 0) reglerCurseur(curseurActif, lx(e));
    }

    @Override
    public void mousePressed(MouseEvent e) {
        int mx = lx(e), my = ly(e);
        s.sourisX = mx;
        s.sourisY = my;
        switch (s.etat) {
            case MENU:
                for (int i = 0; i < 3; i++) {
                    if (Zones.ligneBoutique(i).contains(mx, my)) acheter(i);
                }
                if (Zones.flecheG().contains(mx, my)) changerMonde(-1);
                if (Zones.flecheD().contains(mx, my)) changerMonde(1);
                if (Zones.cartePersonnage().contains(mx, my)) s.etat = Etat.PERSONNAGES;
                else if (Zones.boutonJouer().contains(mx, my)) lancer();
                else if (Zones.boutonSucces().contains(mx, my)) s.etat = Etat.SUCCES;
                else if (Zones.boutonParams().contains(mx, my)) ouvrirParams(Etat.MENU);
                else if (Zones.boutonMultijoueur().contains(mx, my)) ouvrirMulti();
                else if (Zones.boutonQuitter().contains(mx, my)) quitterJeu();
                else if (Zones.boutonBoutique().contains(mx, my)) ouvrirBoutique();
                else if (Zones.boutonMode().contains(mx, my)) changerMode();
                else if (Zones.boutonDefi().contains(mx, my)) basculerDefi();
                break;
            case BOUTIQUE:
                if (Zones.boutonBoutiqueRetour().contains(mx, my)) {
                    s.etat = Etat.MENU;
                    s.audio.clic();
                } else {
                    for (int i = 0; i < Progression.NB_META; i++) {
                        if (Zones.ligneBoutiquePlus(i).contains(mx, my)) {
                            s.boutiqueSel = i;
                            acheter(i);
                            break;
                        }
                    }
                }
                break;
            case NIVEAU:
                if (Zones.btnRelancer().contains(mx, my)) {
                    relancerChoix();
                    break;
                }
                for (int i = 0; i < partie.choix.size(); i++) {
                    if (Zones.carte(i).contains(mx, my)) {
                        choisirAmelioration(i);
                        break;
                    }
                }
                break;
            case PAUSE:
                if (Zones.btnReprendre().contains(mx, my)) s.etat = Etat.JEU;
                else if (Zones.btnParams().contains(mx, my)) ouvrirParams(Etat.PAUSE);
                else if (Zones.btnAbandon().contains(mx, my)) abandonner();
                break;
            case PARAMS:
                for (int i = 0; i < NB_PARAMS; i++) {
                    if (!Zones.ligneParam(i).contains(mx, my)) continue;
                    s.paramSel = i;
                    if (i < 2) {
                        if (Zones.zoneCurseur(i).contains(mx, my)) {
                            curseurActif = i;
                            reglerCurseur(i, mx);
                        }
                    } else {
                        activer(i);
                    }
                    break;
                }
                break;
            case PERSONNAGES:
                for (int i = 0; i < Personnage.TOUS.length; i++) {
                    if (Zones.personnage(i).contains(mx, my) && s.prog.personnageDebloque(i)) {
                        s.prog.personnageSel = i;
                        confirmerPersonnage();
                        break;
                    }
                }
                break;
            case COMMANDES:
                s.etat = Etat.PARAMS;
                break;
            case MULTI:
                if (Zones.boutonLanRetour().contains(mx, my)) {
                    s.fermerReseau();
                    s.etat = Etat.MENU;
                } else if (!s.reseau.estDansSalon() && Zones.boutonLanHeberger().contains(mx, my)) {
                    hebergerLan();
                } else if (!s.reseau.estDansSalon() && Zones.boutonLanRechercher().contains(mx, my)) {
                    rechercherLan();
                } else if (s.reseau.role() == ReseauLan.Role.HOTE && Zones.boutonLanLancer().contains(mx, my)) {
                    lancerLan();
                } else {
                    for (int i = 0; i < s.partiesLan.size(); i++) {
                        if (Zones.partieLan(i).contains(mx, my)) {
                            rejoindreLan(i);
                            break;
                        }
                    }
                }
                break;
            case FIN:
            case SUCCES:
                s.etat = Etat.MENU;
                break;
            default:
                break;
        }
    }

    @Override
    public void mouseReleased(MouseEvent e) {
        if (curseurActif >= 0) {
            if (curseurActif == 1) s.audio.clic(); // petit bip pour tester le volume des bruitages
            curseurActif = -1;
            sauver();
        }
    }

    @Override
    public void mouseClicked(MouseEvent e) {
    }

    @Override
    public void mouseEntered(MouseEvent e) {
    }

    @Override
    public void mouseExited(MouseEvent e) {
    }
}
