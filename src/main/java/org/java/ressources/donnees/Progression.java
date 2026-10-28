package org.java.ressources.donnees;

import org.java.ressources.entites.TypeArme;

import java.util.HashSet;
import java.util.Set;
import java.util.function.Consumer;

/** Progression permanente du joueur : sucre, améliorations, mondes, succès, statistiques. */
public class Progression {
    // Indices des améliorations permanentes (boutique).
    public static final int VIE = 0, DEGATS = 1, VITESSE = 2, CADENCE = 3, AIMANT = 4,
            XP = 5, CHANCE = 6, RELANCE = 7, REVIVE = 8;
    public static final int NB_META = 9;

    /** Nom affiché dans la boutique. */
    public static final String[] META_NOMS = {
            "Coeur de sucre  (+15 PV max)",
            "Corne affûtée  (+10% dégâts)",
            "Sabots de vent  (+5% vitesse)",
            "Gâchette sucrée  (+4% cadence de tir)",
            "Aimant de poche  (bonbons attirés de plus loin)",
            "Bonbons dorés  (+5% d'expérience)",
            "Trèfle porte-bonheur  (+5% de pouvoirs)",
            "Dé magique  (+1 relance de choix par partie)",
            "Seconde chance  (reviens à 40% de PV)"
    };
    /** Clés de sauvegarde (les trois premières sont celles des anciennes versions). */
    public static final String[] META_CLES = {
            "metaVie", "metaDegats", "metaVitesse", "metaCadence", "metaAimant",
            "metaXp", "metaChance", "metaRelance", "metaRevive"
    };
    private static final int[] META_MAX = {10, 10, 10, 10, 5, 8, 8, 3, 2};
    private static final int[] META_COUT = {25, 25, 25, 35, 30, 40, 40, 120, 300};

    public int sucre;
    public final int[] meta = new int[NB_META];
    public int personnageSel;
    public int mondeMax = 0, totalKills = 0, totalSucre = 0, parties = 0, achats = 0, totalEsquives = 0;
    public final double[] meilleur = new double[Monde.TOUS.length];
    public final boolean[] gagne = new boolean[Monde.TOUS.length];
    public final Set<String> succesOk = new HashSet<>();
    public final Set<TypeArme> armesUtilisees = new HashSet<>();
    /** Défi du jour : date (aaaa-mm-jj) du dernier défi joué et meilleur score ce jour-là. */
    public String defiDate = "";
    public int defiScore = 0;

    /** Appelé quand un succès vient d'être débloqué (pour la fenêtre et le son). */
    public Consumer<Succes> surDeblocage;

    public int max(int i) {
        return META_MAX[i];
    }

    public int niveauMeta(int i) {
        return meta[i];
    }

    /** Prix du prochain niveau de l'amélioration i. */
    public int coutProchain(int i) {
        return META_COUT[i] * (meta[i] + 1);
    }

    /** Achète l'amélioration permanente i (voir les constantes VIE, DEGATS...). */
    public boolean acheter(int i) {
        if (i < 0 || i >= NB_META) return false;
        int cout = coutProchain(i);
        if (meta[i] >= META_MAX[i] || sucre < cout) return false;
        sucre -= cout;
        meta[i]++;
        achats++;
        verifierGlobal();
        return true;
    }

    public boolean personnageDebloque(int id) {
        if (id < 0 || id >= Personnage.TOUS.length || mondeMax < Personnage.TOUS[id].mondeDeblocage) return false;
        String succes = Personnage.TOUS[id].succesDeblocage;
        return succes == null || succesOk.contains(succes);
    }

    public void debloquer(String id) {
        if (succesOk.contains(id)) return;
        for (Succes s : Succes.TOUS) {
            if (s.id.equals(id)) {
                succesOk.add(id);
                sucre += s.recompense;
                totalSucre += s.recompense;
                if (surDeblocage != null) surDeblocage.accept(s);
                return;
            }
        }
    }

    public void enregistrerArme(TypeArme arme) {
        if (armesUtilisees.add(arme)) verifierGlobal();
    }

    /** Succès qui ne dépendent pas de la partie en cours. */
    public void verifierGlobal() {
        if (totalKills >= 1) debloquer("kill1");
        if (totalKills >= 500) debloquer("kill500");
        if (totalKills >= 3000) debloquer("kill3000");
        if (parties >= 10) debloquer("parties10");
        if (totalSucre >= 1000) debloquer("sucre1000");
        if (achats >= 1) debloquer("achat");
        if (totalEsquives >= 100) debloquer("danse");
        if (armesUtilisees.contains(TypeArme.EPEE)) debloquer("armeEpee");
        if (armesUtilisees.contains(TypeArme.BOMBE)) debloquer("armeBombe");
        if (armesUtilisees.contains(TypeArme.RAYON)) debloquer("armeRayon");
    }

    /** Efface la progression (les réglages sont conservés). */
    public void reinitialiser() {
        sucre = 0;
        java.util.Arrays.fill(meta, 0);
        mondeMax = 0;
        totalKills = totalSucre = parties = achats = totalEsquives = 0;
        defiDate = "";
        defiScore = 0;
        for (int i = 0; i < Monde.TOUS.length; i++) {
            meilleur[i] = 0;
            gagne[i] = false;
        }
        succesOk.clear();
        armesUtilisees.clear();
    }
}
