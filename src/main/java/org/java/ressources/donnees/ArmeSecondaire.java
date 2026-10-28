package org.java.ressources.donnees;

import java.awt.Color;

/**
 * Les armes secondaires : elles s'ajoutent à l'attaque de base du personnage (choix de montée de niveau).
 * Chacune a 5 niveaux, puis une ÉVOLUTION si le niveau max est atteint ET que la bonne amélioration a été prise.
 */
public enum ArmeSecondaire {
    AURA("Aura de sucre", "Brûle les ennemis\nqui s'approchent",
            new Color(255, 170, 205),
            "Aura royale", "Aura immense qui\nte soigne en brûlant",
            "Câlin de guimauve", 1),
    METEORES("Pluie de météores", "Des météores sucrés\ntombent sur les ennemis",
            new Color(255, 190, 120),
            "Pluie d'astéroïdes", "Deux fois plus de météores,\nplus gros",
            "Cadence folle", 2),
    ECLAIRS("Éclairs arc-en-ciel", "Un éclair saute\nd'ennemi en ennemi",
            new Color(170, 215, 255),
            "Tempête de foudre", "Éclairs plus longs\net bien plus forts",
            "Double arc-en-ciel", 2);

    public static final int NIV_MAX = 5;

    public final String nom, desc, nomEvo, descEvo, ameliorationRequise;
    public final Color couleur;
    public final int nbRequis;

    ArmeSecondaire(String nom, String desc, Color couleur, String nomEvo, String descEvo,
                   String ameliorationRequise, int nbRequis) {
        this.nom = nom;
        this.desc = desc;
        this.couleur = couleur;
        this.nomEvo = nomEvo;
        this.descEvo = descEvo;
        this.ameliorationRequise = ameliorationRequise;
        this.nbRequis = nbRequis;
    }
}
