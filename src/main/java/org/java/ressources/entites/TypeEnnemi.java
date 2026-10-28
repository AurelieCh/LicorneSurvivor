package org.java.ressources.entites;

/**
 * Rôle visuel et gameplay d'une sucrerie ennemie.
 * ATTENTION : l'ordre compte (l'ordinal est envoyé sur le réseau). On ajoute toujours à la fin.
 */
public enum TypeEnnemi {
    MACARON,
    GUIMAUVE,
    GOMME,
    BOSS,
    /** Reste à distance et tire des bonbons. */
    TIREUR,
    /** Fonce sur le joueur puis explose après un court délai. */
    KAMIKAZE;

    /** Convertit un numéro (console de développement, réseau) en type, sans jamais planter. */
    public static TypeEnnemi depuisId(int id) {
        TypeEnnemi[] types = values();
        return types[Math.clamp(id, 0, types.length - 1)];
    }
}
