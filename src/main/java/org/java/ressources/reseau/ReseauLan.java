package org.java.ressources.reseau;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.Closeable;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Map;

/**
 * Réseau local du salon. La découverte est en UDP, puis le salon utilise TCP.
 * Le protocole est volontairement textuel et versionné pour rester inspectable.
 */
public final class ReseauLan implements Closeable {
    public static final int VERSION = 1;
    public static final int DECOUVERTE_PORT = 38741;
    public static final int MAX_JOUEURS = 4;

    public enum Role { AUCUN, HOTE, CLIENT }

    public record PartieTrouvee(String nom, String adresse, int port, int joueurs) {}
    public record Joueur(String id, String nom, int personnage, boolean pret) {}
    public record Entree(String id, int touches, int sourisX, int sourisY, boolean competence, int personnage) {}

    private final String nomLocal;
    private final String idLocal = UUID.randomUUID().toString();
    private final List<Joueur> joueurs = new CopyOnWriteArrayList<>();
    private final List<Socket> connexions = new CopyOnWriteArrayList<>();
    private final Map<Socket, String> identifiantsConnexions = new ConcurrentHashMap<>();
    private final Map<String, Entree> entrees = new ConcurrentHashMap<>();
    private volatile Role role = Role.AUCUN;
    private volatile boolean enCours;
    private volatile boolean lancementDemande;
    private volatile String dernierSync = "";
    private volatile int mondeLancement = -1;
    private volatile long seedLancement;
    private volatile boolean finDemandee;
    private volatile boolean victoireReseau;
    private volatile int port;
    private volatile ServerSocket serveur;
    private volatile DatagramSocket annonce;
    private Thread threadServeur;
    private Thread threadDecouverte;

    public ReseauLan(String nomLocal) {
        this.nomLocal = nomLocal == null || nomLocal.isBlank() ? "Joueur" : nomLocal.trim();
    }

    public Role role() { return role; }
    public boolean estDansSalon() { return role != Role.AUCUN; }
    public boolean lancementDemande() { return lancementDemande; }
    public void consommerLancement() { lancementDemande = false; }
    public int mondeLancement() { return mondeLancement; }
    public long seedLancement() { return seedLancement; }
    public boolean finDemandee() { return finDemandee; }
    public boolean victoireReseau() { return victoireReseau; }
    public void consommerFin() { finDemandee = false; }
    public List<Joueur> joueurs() { return Collections.unmodifiableList(new ArrayList<>(joueurs)); }
    public int port() { return port; }
    public String idLocal() { return idLocal; }

    public void heberger() throws IOException {
        fermer();
        serveur = new ServerSocket(0);
        port = serveur.getLocalPort();
        role = Role.HOTE;
        enCours = true;
        joueurs.add(new Joueur(idLocal, nomLocal, 0, true));
        threadServeur = new Thread(this::boucleServeur, "lan-hote");
        threadServeur.setDaemon(true);
        threadServeur.start();
        demarrerAnnonce();
    }

    public void rejoindre(PartieTrouvee partie) throws IOException {
        fermer();
        Socket socket = new Socket(partie.adresse(), partie.port());
        socket.setTcpNoDelay(true);
        role = Role.CLIENT;
        enCours = true;
        connexions.add(socket);
        envoyer(socket, "HELLO|" + idLocal + "|" + nettoyer(nomLocal));
        Thread t = new Thread(() -> lire(socket), "lan-client");
        t.setDaemon(true);
        t.start();
    }

    public void demanderLancement(int monde, long seed) {
        if (role != Role.HOTE) return;
        mondeLancement = monde;
        seedLancement = seed;
        diffuser("START|" + monde + "|" + seed);
        lancementDemande = true;
    }

    public void envoyerEntree(int touches, int sourisX, int sourisY, boolean competence, int personnage) {
        if (role == Role.CLIENT) {
            envoyerATous("INPUT|" + idLocal + "|" + touches + "|" + sourisX + "|" + sourisY + "|"
                    + competence + "|" + personnage);
        }
    }

    public List<Entree> prendreEntrees() {
        return new ArrayList<>(entrees.values());
    }
    public String dernierSync() { return dernierSync; }
    /** Retourne le dernier état reçu une seule fois, puis le retire de la file logique. */
    public synchronized String prendreDernierSync() {
        String sync = dernierSync;
        dernierSync = "";
        return sync;
    }
    public void diffuserSync(String sync) {
        if (role == Role.HOTE) envoyerATous("SYNC|" + sync);
    }

    public void diffuserFin(boolean victoire) {
        if (role == Role.HOTE) envoyerATous("END|" + victoire);
    }

    public void changerPersonnage(int personnage) {
        envoyerATous("CHAR|" + idLocal + "|" + personnage);
    }

    public static List<PartieTrouvee> rechercher(long dureeMs) {
        List<PartieTrouvee> resultats = new ArrayList<>();
        try (DatagramSocket socket = new DatagramSocket()) {
            socket.setBroadcast(true);
            socket.setSoTimeout((int) Math.max(100, dureeMs));
            byte[] demande = ("FIND|" + VERSION).getBytes(StandardCharsets.UTF_8);
            socket.send(new DatagramPacket(demande, demande.length,
                    InetAddress.getByName("255.255.255.255"), DECOUVERTE_PORT));
            byte[] buffer = new byte[512];
            long fin = System.currentTimeMillis() + dureeMs;
            while (System.currentTimeMillis() < fin) {
                try {
                    DatagramPacket paquet = new DatagramPacket(buffer, buffer.length);
                    socket.receive(paquet);
                    String[] p = new String(paquet.getData(), 0, paquet.getLength(), StandardCharsets.UTF_8).split("\\|");
                    if (p.length >= 5 && "HOST".equals(p[0]) && VERSION == Integer.parseInt(p[1])) {
                        PartieTrouvee trouvee = new PartieTrouvee(p[2], paquet.getAddress().getHostAddress(),
                                Integer.parseInt(p[3]), Integer.parseInt(p[4]));
                        resultats.removeIf(x -> x.adresse().equals(trouvee.adresse()) && x.port() == trouvee.port());
                        resultats.add(trouvee);
                    }
                } catch (java.net.SocketTimeoutException ignored) {
                    break;
                } catch (NumberFormatException ignored) {
                    // Une annonce invalide ne doit pas faire tomber la recherche.
                }
            }
        } catch (IOException ignored) {
            // L'absence de réseau est un état normal pour le menu LAN.
        }
        return resultats;
    }

    private void demarrerAnnonce() {
        threadDecouverte = new Thread(() -> {
            try (DatagramSocket socket = new DatagramSocket(DECOUVERTE_PORT)) {
                annonce = socket;
                socket.setBroadcast(true);
                byte[] buffer = new byte[128];
                while (enCours && role == Role.HOTE) {
                    DatagramPacket demande = new DatagramPacket(buffer, buffer.length);
                    socket.receive(demande);
                    String texte = new String(demande.getData(), 0, demande.getLength(), StandardCharsets.UTF_8);
                    if (texte.equals("FIND|" + VERSION)) {
                        String annonceTexte = "HOST|" + VERSION + "|" + nettoyer(nomLocal) + "|" + port + "|" + joueurs.size();
                        byte[] reponse = annonceTexte.getBytes(StandardCharsets.UTF_8);
                        socket.send(new DatagramPacket(reponse, reponse.length, demande.getAddress(), demande.getPort()));
                    }
                }
            } catch (IOException ignored) {
                // La fermeture du réseau interrompt volontairement receive().
            } finally {
                annonce = null;
            }
        }, "lan-decouverte");
        threadDecouverte.setDaemon(true);
        threadDecouverte.start();
    }

    private void boucleServeur() {
        while (enCours) {
            try {
                Socket socket = serveur.accept();
                if (connexions.size() >= MAX_JOUEURS - 1) {
                    envoyer(socket, "FULL");
                    socket.close();
                    continue;
                }
                socket.setTcpNoDelay(true);
                connexions.add(socket);
                Thread t = new Thread(() -> lire(socket), "lan-joueur");
                t.setDaemon(true);
                t.start();
            } catch (IOException e) {
                if (enCours) e.printStackTrace();
                break;
            }
        }
    }

    private void lire(Socket socket) {
        try (BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8))) {
            String ligne;
            while (enCours && (ligne = in.readLine()) != null) traiter(socket, ligne);
        } catch (IOException ignored) {
            // La fermeture du socket signale une déconnexion normale ou réseau.
        } finally {
            connexions.remove(socket);
            String id = identifiantsConnexions.remove(socket);
            if (id != null && role == Role.HOTE) {
                entrees.remove(id);
                joueurs.removeIf(joueur -> joueur.id().equals(id));
                diffuser("ROSTER|" + roster());
            }
            try {
                socket.close();
            } catch (IOException ignored) {
                // Le socket est déjà fermé.
            }
        }
    }

    private void traiter(Socket socket, String ligne) {
        String[] p = ligne.split("\\|", -1);
        if (p.length == 0) return;
        if (role == Role.HOTE && "HELLO".equals(p[0]) && p.length >= 3) {
            identifiantsConnexions.put(socket, p[1]);
            joueurs.add(new Joueur(p[1], p[2], 0, false));
            diffuser("ROSTER|" + roster());
        } else if ("ROSTER".equals(p[0]) && p.length >= 2) {
            remplacerRoster(p[1]);
        } else if ("CHAR".equals(p[0]) && p.length >= 3) {
            modifierPersonnage(p[1], Integer.parseInt(p[2]));
            if (role == Role.HOTE) diffuser("ROSTER|" + roster());
        } else if ("START".equals(p[0]) && p.length >= 3) {
            try {
                mondeLancement = Integer.parseInt(p[1]);
                seedLancement = Long.parseLong(p[2]);
                lancementDemande = true;
            } catch (NumberFormatException ignored) {
                // Un lancement invalide ne doit pas démarrer une partie incohérente.
            }
        } else if ("END".equals(p[0]) && p.length >= 2 && role == Role.CLIENT) {
            victoireReseau = Boolean.parseBoolean(p[1]);
            finDemandee = true;
        } else if ("INPUT".equals(p[0]) && p.length >= 7 && role == Role.HOTE) {
            try {
                entrees.put(p[1], new Entree(p[1], Integer.parseInt(p[2]), Integer.parseInt(p[3]),
                        Integer.parseInt(p[4]), Boolean.parseBoolean(p[5]), Integer.parseInt(p[6])));
            } catch (NumberFormatException ignored) {
                // Une entrée malformée est simplement ignorée.
            }
        } else if ("SYNC".equals(p[0]) && p.length >= 2 && role == Role.CLIENT) {
            synchronized (this) {
                dernierSync = ligne.substring("SYNC|".length());
            }
        }
    }

    private String roster() {
        StringBuilder b = new StringBuilder();
        for (Joueur j : joueurs) {
            if (!b.isEmpty()) b.append(';');
            b.append(j.id()).append(',').append(nettoyer(j.nom())).append(',').append(j.personnage()).append(',').append(j.pret());
        }
        return b.toString();
    }

    private void remplacerRoster(String texte) {
        joueurs.clear();
        entrees.clear();
        for (String ligne : texte.split(";")) {
            String[] p = ligne.split(",", -1);
            if (p.length >= 4) {
                try { joueurs.add(new Joueur(p[0], p[1], Integer.parseInt(p[2]), Boolean.parseBoolean(p[3]))); }
                catch (NumberFormatException ignored) { }
            }
        }
    }

    private void modifierPersonnage(String id, int personnage) {
        for (int i = 0; i < joueurs.size(); i++) {
            Joueur j = joueurs.get(i);
            if (j.id().equals(id)) joueurs.set(i, new Joueur(j.id(), j.nom(), personnage, j.pret()));
        }
    }

    private void diffuser(String message) {
        envoyerATous(message);
    }

    private void envoyerATous(String message) {
        for (Socket socket : connexions) envoyer(socket, message);
    }

    private static void envoyer(Socket socket, String message) {
        try {
            synchronized (socket) {
                BufferedWriter out = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8));
                out.write(message);
                out.newLine();
                out.flush();
            }
        } catch (IOException ignored) { }
    }

    private static String nettoyer(String texte) {
        return texte.replace('|', ' ').replace(';', ' ').replace(',', ' ').trim();
    }

    @Override
    public void close() {
        enCours = false;
        role = Role.AUCUN;
        lancementDemande = false;
        finDemandee = false;
        victoireReseau = false;
        dernierSync = "";
        mondeLancement = -1;
        seedLancement = 0;
        joueurs.clear();
        if (annonce != null) annonce.close();
        try { if (serveur != null) serveur.close(); } catch (IOException ignored) { }
        for (Socket socket : connexions) try { socket.close(); } catch (IOException ignored) { }
        connexions.clear();
        identifiantsConnexions.clear();
        serveur = null;
    }

    public void fermer() { close(); }
}
