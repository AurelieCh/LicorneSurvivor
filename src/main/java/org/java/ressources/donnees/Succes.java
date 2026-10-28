package org.java.ressources.donnees;

/** Un succès à débloquer, avec sa récompense en sucre. */
public class Succes {
    public final String id, nom, desc;
    public final int recompense;

    public Succes(String id, String nom, String desc, int recompense) {
        this.id = id;
        this.nom = nom;
        this.desc = desc;
        this.recompense = recompense;
    }

    public static final Succes[] TOUS = {
            new Succes("kill1", "Première bouchée", "Vaincre un ennemi", 10),
            new Succes("kill500", "Chasseuse de sucreries", "Vaincre 500 ennemis (au total)", 40),
            new Succes("kill3000", "Fléau des bonbons", "Vaincre 3000 ennemis (au total)", 120),
            new Succes("surv60", "Minute magique", "Survivre 60 secondes", 15),
            new Succes("niv10", "Montée en puissance", "Atteindre le niveau 10", 30),
            new Succes("niv20", "Licorne légendaire", "Atteindre le niveau 20", 80),
            new Succes("etoiles6", "Pluie d'étoiles", "Avoir 6 étoiles qui tournent", 40),
            new Succes("proj6", "Arc-en-ciel complet", "Tirer 6 projectiles à la fois", 40),
            new Succes("coins", "Tour du monde", "Toucher les 4 coins d'une carte", 50),
            new Succes("boss1", "Reine détrônée", "Gagner dans la Prairie Cupcake", 60),
            new Succes("boss2", "Roi détrôné", "Gagner dans la Forêt de Guimauve", 100),
            new Succes("boss3", "Dragon apprivoisé", "Gagner dans le Volcan Chocolat", 150),
            new Succes("intouchable", "Intouchable", "Gagner sans perdre un seul PV", 200),
            new Succes("achat", "Gourmande", "Acheter une amélioration permanente", 10),
            new Succes("parties10", "Accro au sucre", "Jouer 10 parties", 25),
            new Succes("sucre1000", "Dentiste inquiet", "Gagner 1000 sucre au total", 60),
            new Succes("armeEpee", "Lame en main", "Jouer une partie avec Lame", 25),
            new Succes("armeBombe", "Pluie de guimauves", "Jouer une partie avec Boum", 25),
            new Succes("armeRayon", "Lumière concentrée", "Jouer une partie avec Prisme", 35),
    };
}
