package org.java.ressources.entites;

/** Rôle visuel et gameplay d'une sucrerie ennemie. */
public enum TypeEnnemi {
    MACARON,
    GUIMAUVE,
    GOMME,
    BOSS;

    /** Convertit l'ancien numéro de la console de développement. */
    public static TypeEnnemi depuisId(int id) {
        TypeEnnemi[] types = values();
        return types[Math.clamp(types.length - 1, 0, id)];
    }
}
