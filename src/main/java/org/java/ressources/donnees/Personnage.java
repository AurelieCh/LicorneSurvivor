package org.java.ressources.donnees;

import org.java.ressources.entites.TypeArme;

import java.awt.Color;

/** Personnage jouable : arme automatique et compétence active. */
public final class Personnage {
    public static final Personnage[] TOUS = {
            new Personnage(0, "Lumi", "Étoiles filantes", "Nova céleste", TypeArme.ETOILE, 0, null, new Color(255, 170, 220)),
            new Personnage(1, "Flèche", "Tir en éventail", "Pluie de flèches", TypeArme.FLECHE, 1, null, new Color(160, 220, 255)),
            new Personnage(2, "Boum", "Petites bombes", "Bouclier sucré", TypeArme.BOMBE, 2, null, new Color(255, 210, 120)),
            new Personnage(3, "Lame", "Lames projetées", "Entaille astrale", TypeArme.EPEE, 1, "boss2", new Color(220, 190, 255)),
            new Personnage(4, "Ronce", "Boomerang sucré", "Ronces tournoyantes", TypeArme.BOOMERANG, 2, "kill500", new Color(150, 235, 180)),
            new Personnage(5, "Prisme", "Rayon prismatique", "Prisme explosif", TypeArme.RAYON, 2, "boss3", new Color(255, 235, 130))
    };

    public final int id;
    public final String nom, arme, competence;
    public final TypeArme typeArme;
    public final int mondeDeblocage;
    public final String succesDeblocage;
    public final Color couleur;

    private Personnage(int id, String nom, String arme, String competence, TypeArme typeArme,
                       int mondeDeblocage, String succesDeblocage, Color couleur) {
        this.id = id;
        this.nom = nom;
        this.arme = arme;
        this.competence = competence;
        this.typeArme = typeArme;
        this.mondeDeblocage = mondeDeblocage;
        this.succesDeblocage = succesDeblocage;
        this.couleur = couleur;
    }
}
