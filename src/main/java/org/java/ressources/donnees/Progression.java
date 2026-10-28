package org.java.ressources.donnees;

import org.java.ressources.entites.TypeArme;

import java.util.HashSet;
import java.util.Set;
import java.util.function.Consumer;

/** Progression permanente du joueur : sucre, améliorations, mondes, succès, statistiques. */
public class Progression {
    public int sucre, metaVie, metaDegats, metaVitesse;
    public int personnageSel;
    public int mondeMax = 0, totalKills = 0, totalSucre = 0, parties = 0, achats = 0;
    public final double[] meilleur = new double[Monde.TOUS.length];
    public final boolean[] gagne = new boolean[Monde.TOUS.length];
    public final Set<String> succesOk = new HashSet<>();
    public final Set<TypeArme> armesUtilisees = new HashSet<>();

    /** Appelé quand un succès vient d'être débloqué (pour la fenêtre et le son). */
    public Consumer<Succes> surDeblocage;

    public int cout(int niv) {
        return 25 * (niv + 1);
    }

    public int niveauMeta(int i) {
        return i == 0 ? metaVie : i == 1 ? metaDegats : metaVitesse;
    }

    /** Achète l'amélioration permanente i (0 vie, 1 dégâts, 2 vitesse). */
    public boolean acheter(int i) {
        int niv = niveauMeta(i);
        if (niv >= Constantes.NIV_MAX || sucre < cout(niv)) return false;
        sucre -= cout(niv);
        if (i == 0) metaVie++;
        else if (i == 1) metaDegats++;
        else metaVitesse++;
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
        if (armesUtilisees.contains(TypeArme.EPEE)) debloquer("armeEpee");
        if (armesUtilisees.contains(TypeArme.BOMBE)) debloquer("armeBombe");
        if (armesUtilisees.contains(TypeArme.RAYON)) debloquer("armeRayon");
    }

    /** Efface la progression (les réglages sont conservés). */
    public void reinitialiser() {
        sucre = metaVie = metaDegats = metaVitesse = 0;
        mondeMax = 0;
        totalKills = totalSucre = parties = achats = 0;
        for (int i = 0; i < Monde.TOUS.length; i++) {
            meilleur[i] = 0;
            gagne[i] = false;
        }
        succesOk.clear();
        armesUtilisees.clear();
    }
}
