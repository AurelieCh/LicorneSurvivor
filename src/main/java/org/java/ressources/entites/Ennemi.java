package org.java.ressources.entites;

import java.awt.Color;

/** Une sucrerie méchante. */
public class Ennemi {
    public double x, y, pv, pvMax, rayon, vitesse, degats, flash;
    public TypeEnnemi type;
    public int monde;
    public boolean boss;
    /** Élite : plus grosse, plus solide, lâche toujours un pouvoir. */
    public boolean elite;
    public Color col;
    /** Temps restant pendant lequel l'ennemi contourne un obstacle, et sens du contournement (+1 / -1). */
    public double contourne;
    public int sens = 1;

    // --- petite machine à états (tireurs, kamikazes, boss) ---------------------------------
    /** Étape courante : 0 = normal ; pour le boss voir IaEnnemis (1 télégraphe, 2 charge, 3 repos, 4 incantation). */
    public int etape;
    /** Minuteur de l'étape en cours, et délai avant la prochaine action. */
    public double minuteur, cd;
    /** Direction verrouillée (charge du boss). */
    public double dirX, dirY;
    /** Phase du boss (1 à 3), dernière attaque lancée, rafales restantes. */
    public int phase = 1, attaque = -1, rafales;
}
