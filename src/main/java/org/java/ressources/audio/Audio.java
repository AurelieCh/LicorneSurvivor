package org.java.ressources.audio;

import org.java.ressources.donnees.Reglages;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.SourceDataLine;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Musique et bruitages synthétisés en direct (aucun fichier son). Les volumes viennent des Réglages.
 * <p>
 * La musique de chaque monde est un morceau de 16 mesures (environ 30 s avant de reboucler) :
 * A1 (légère) - A2 (batterie + arpèges, mélodie qui monte) - B (pont sur d'autres accords) - A3 (final, mélodie doublée).
 */
public class Audio implements Runnable {
    static final float SR = 44100f;
    static final int R = -99; // silence
    static final double DEUX_PI = 2 * Math.PI;

    // mélodie principale de 32 croches (4 mesures), une par monde
    static final int[][] MELO = {
            {0, 4, 7, 12, 7, 4, 7, R, 9, 12, 16, 12, 9, 7, 4, R, 5, 9, 12, 9, 5, 9, 7, R, 7, 11, 14, 11, 7, 5, 4, 2},
            {0, 4, 7, 9, 7, 4, 2, R, 4, 7, 11, 7, 4, 2, 0, R, 5, 9, 12, 9, 5, 4, 2, R, 7, 11, 14, 11, 7, 4, 2, 0},
            {0, 3, 7, 12, 10, 7, 3, 7, 8, 12, 15, 12, 8, 7, 3, R, 5, 8, 12, 8, 5, 3, 2, R, 7, 10, 14, 10, 7, 3, 2, 0}
    };
    static final int[] BASE = {72, 67, 69};              // note MIDI de départ

    // accords des parties A : fondamentales (en demi-tons depuis BASE) et tierces (4 = majeur, 3 = mineur)
    static final int[][] BASSE = {{0, -3, -7, -5}, {0, -3, 5, 7}, {0, -4, -7, -5}};
    static final int[][] TIERCE = {{4, 3, 4, 4}, {4, 3, 4, 4}, {3, 4, 3, 4}};
    // accords du pont (partie B)
    static final int[][] BASSE_B = {{5, 7, 4, 9}, {5, 7, 4, 9}, {5, 0, -4, -5}};
    static final int[][] TIERCE_B = {{4, 4, 3, 3}, {4, 4, 3, 3}, {3, 3, 4, 4}};
    // mélodie du pont : numéros de notes de l'accord (0 fondamentale, 1 tierce, 2 quinte, 3 octave, 4 octave + tierce)
    static final int[][] MOTIF_B = {
            {3, R, 2, 3, 4, R, 3, 2},
            {3, R, 2, 3, 4, R, 3, 2},
            {4, 3, R, 2, 3, R, 2, 1},
            {4, 3, 2, 1, 2, 1, 0, 1}
    };
    static final int[] ARPEGE = {0, 1, 2, 3, 2, 1, 2, 3};
    static final int[] BPM = {132, 120, 144};
    static final int PAS_MORCEAU = 128;                  // 16 mesures de 8 croches

    private final Reglages reg;
    public volatile boolean calme = false;
    public volatile int monde = 0;

    private boolean ok = false;
    private SourceDataLine line;
    private final List<double[]> voix = new ArrayList<>(); // {f0, f1, durée, t, vol, forme, délai, phase}
    private final Random rnd = new Random();

    // état de la musique
    private long n = 0;
    private int mondeCourant = -1;
    private int dernierPas = -1;
    private double phLead, phLead2, phBasse, phArp, phKick;
    private double fLead, fLead2, fBasse, fArp;
    private boolean lead, lead2, arp, kick, snare, charley;
    private double gainKick = 1;
    private final double[] echo = new double[(int) (SR * 2)];
    private int echoPos = 0, echoLen = 1;

    public Audio(Reglages reg) {
        this.reg = reg;
    }

    public void demarrer() {
        try {
            AudioFormat fmt = new AudioFormat(SR, 16, 1, true, false);
            line = AudioSystem.getSourceDataLine(fmt);
            line.open(fmt, 8192);
            line.start();
            ok = true;
            Thread t = new Thread(this, "audio");
            t.setDaemon(true);
            t.start();
        } catch (Exception e) {
            ok = false; // pas de carte son : le jeu marche quand même, en silence
        }
    }

    private static double freq(int midi) {
        return 440.0 * Math.pow(2, (midi - 69) / 12.0);
    }

    @Override
    public void run() {
        byte[] buf = new byte[1024];
        while (true) {
            for (int i = 0; i < 512; i++) {
                double s = (reg.musique ? echantillonMusique() * reg.volMusique : 0) + echantillonSfx() * reg.volSfx;
                s = Math.clamp(s, -1, 1);
                short v = (short) (s * 30000);
                buf[2 * i] = (byte) (v & 0xff);
                buf[2 * i + 1] = (byte) ((v >> 8) & 0xff);
            }
            line.write(buf, 0, buf.length);
        }
    }

    /** Note de l'accord : 0 fondamentale, 1 tierce, 2 quinte, 3 octave, 4 octave + tierce. */
    private static int noteAccord(int k, int tierce) {
        return switch (k) {
            case 0 -> 0;
            case 1 -> tierce;
            case 2 -> 7;
            case 3 -> 12;
            default -> 12 + tierce;
        };
    }

    /** Prépare ce qui se joue pendant cette croche (appelé une seule fois par croche). */
    private void preparerPas(int m, int idx) {
        int section = idx / 32;             // 0 = A1, 1 = A2, 2 = pont, 3 = A3
        int dansSection = idx % 32;
        int mesure = dansSection / 8;
        int croche = dansSection % 8;
        boolean pont = section == 2;
        int racine = pont ? BASSE_B[m][mesure] : BASSE[m][mesure];
        int tierce = pont ? TIERCE_B[m][mesure] : TIERCE[m][mesure];
        int reduite = racine > 6 ? racine - 12 : racine;   // pour rester dans le registre de la mélodie

        // mélodie
        int note;
        if (pont) {
            int k = MOTIF_B[mesure][croche];
            note = k == R ? R : BASE[m] + reduite + noteAccord(k, tierce);
        } else {
            note = MELO[m][dansSection];
            if (note != R) {
                note += BASE[m];
                if (section == 1 && mesure >= 2) note += 12;   // A2 : la mélodie monte d'une octave sur la fin
            }
        }
        lead = note != R;
        if (lead) fLead = freq(note);
        lead2 = section == 3 && lead;                         // A3 : mélodie doublée à l'octave
        if (lead2) fLead2 = freq(note + 12);

        // arpège (absent de l'intro)
        arp = section >= 1;
        if (arp) fArp = freq(BASE[m] - 12 + reduite + noteAccord(ARPEGE[croche], tierce));

        // basse
        fBasse = freq(BASE[m] - 24 + racine + ((dansSection / 2) % 2 == 1 ? 7 : 0));

        // batterie
        kick = croche == 0 || croche == 4 || (pont && (croche == 3 || croche == 6));
        gainKick = section == 0 ? 0.6 : 1;
        boolean roulement = section == 3 && mesure == 3 && croche >= 4;
        snare = section >= 1 && (croche == 2 || croche == 6 || roulement);
        charley = croche % 2 == 1;
        if (kick) phKick = 0;
    }

    private double echantillonMusique() {
        int m = monde;
        if (m != mondeCourant) {
            mondeCourant = m;
            n = 0;
            dernierPas = -1;
            java.util.Arrays.fill(echo, 0);
        }
        int pasLen = (int) (SR * 60.0 / BPM[m] / 2);
        int idx = (int) ((n / pasLen) % PAS_MORCEAU);
        int pos = (int) (n % pasLen);
        if (idx != dernierPas) {
            dernierPas = idx;
            preparerPas(m, idx);
            echoLen = (int) (pasLen * 1.5);
            if (echoPos >= echoLen) echoPos = 0;
        }
        double t = n / (double) SR;
        // petites rampes au début et à la fin de chaque note : pas de « clic » entre deux notes
        double attaque = Math.min(1, pos / (0.003 * SR));
        double relache = Math.min(1, (pasLen - pos) / (0.006 * SR));
        double nuance = attaque * relache;
        double s = 0;

        // mélodie : onde carrée « chiptune » avec un léger vibrato sur les notes tenues
        double voixLead = 0;
        if (lead) {
            double vib = pos > pasLen * 0.4 ? 1 + 0.006 * Math.sin(DEUX_PI * 5.5 * t) : 1;
            phLead += fLead * vib / SR;
            phLead -= Math.floor(phLead);
            double env = Math.max(0, 1 - pos / (double) pasLen * 0.8) * nuance;
            voixLead += (phLead < 0.25 ? 1 : -1) * 0.07 * env;
            if (lead2) {
                phLead2 += fLead2 * vib / SR;
                phLead2 -= Math.floor(phLead2);
                voixLead += (phLead2 < 0.5 ? 1 : -1) * 0.03 * env;
            }
        }
        // écho de la mélodie (croche pointée) : donne de l'espace
        double retard = echo[echoPos];
        echo[echoPos] = voixLead + retard * 0.38;
        echoPos = (echoPos + 1) % echoLen;
        s += voixLead + retard * 0.42;

        // arpège doux (onde étroite, note pincée)
        if (arp) {
            phArp += fArp / SR;
            phArp -= Math.floor(phArp);
            double env = Math.exp(-pos / (double) pasLen * 2.6) * nuance;
            s += (phArp < 0.125 ? 1 : -1) * 0.032 * env;
        }
        // basse : onde triangle
        phBasse += fBasse / SR;
        phBasse -= Math.floor(phBasse);
        double benv = (1 - pos / (double) pasLen * 0.35) * nuance;
        s += (4 * Math.abs(phBasse - 0.5) - 1) * 0.13 * benv;

        // batterie
        if (kick && pos < 0.16 * SR) {
            double tk = pos / (double) SR;
            phKick += (48 + 110 * Math.exp(-tk * 38)) / SR;
            s += Math.sin(DEUX_PI * phKick) * 0.2 * gainKick * Math.exp(-tk * 16);
        }
        if (snare && pos < 0.14 * SR) {
            double tk = pos / (double) SR;
            s += (rnd.nextDouble() * 2 - 1) * 0.07 * Math.exp(-tk * 26)
                    + Math.sin(DEUX_PI * 185 * tk) * 0.035 * Math.exp(-tk * 45);
        }
        if (charley && pos < 0.03 * SR) {
            s += (rnd.nextDouble() * 2 - 1) * 0.03 * (1 - pos / (0.03 * SR));
        }
        n++;
        return calme ? s * 0.35 : s;
    }

    private double echantillonSfx() {
        double s = 0;
        synchronized (voix) {
            for (int i = voix.size() - 1; i >= 0; i--) {
                double[] v = voix.get(i);
                if (v[6] > 0) {
                    v[6] -= 1.0 / SR;
                    continue;
                }
                double p = v[3] / v[2];
                if (p >= 1) {
                    voix.remove(i);
                    continue;
                }
                double f = v[0] + (v[1] - v[0]) * p;
                v[7] += f / SR;
                v[7] -= Math.floor(v[7]);
                double w;
                if (v[5] == 0) w = v[7] < 0.5 ? 1 : -1;
                else if (v[5] == 1) w = 4 * Math.abs(v[7] - 0.5) - 1;
                else w = rnd.nextDouble() * 2 - 1;
                s += w * v[4] * (1 - p);
                v[3] += 1.0 / SR;
            }
        }
        return s;
    }

    private void son(double f0, double f1, double dur, double vol, int forme, double delai) {
        if (!ok || reg.volSfx <= 0) return;
        synchronized (voix) {
            if (voix.size() < 24) voix.add(new double[]{f0, f1, dur, 0, vol, forme, delai, 0});
        }
    }

    public void tir() { son(1000, 1500, 0.05, 0.04, 0, 0); }

    public void mort() { son(500, 150, 0.12, 0.06, 2, 0); }

    public void bonbon() { son(1400, 2000, 0.04, 0.03, 0, 0); }

    public void degat() { son(220, 70, 0.25, 0.12, 1, 0); }

    public void clic() { son(800, 1000, 0.05, 0.06, 0, 0); }

    public void pouvoir() {
        double[] f = {660, 880, 1320};
        for (int i = 0; i < f.length; i++) son(f[i], f[i] * 1.05, 0.09, 0.07, 1, i * 0.05);
    }

    public void niveau() {
        double[] f = {523, 659, 784, 1046};
        for (int i = 0; i < f.length; i++) son(f[i], f[i], 0.12, 0.07, 0, i * 0.08);
    }

    public void succes() {
        double[] f = {784, 988, 1175, 1568};
        for (int i = 0; i < f.length; i++) son(f[i], f[i], 0.14, 0.07, 0, i * 0.09);
    }

    public void victoire() {
        double[] f = {523, 659, 784, 1046, 784, 1046, 1318};
        for (int i = 0; i < f.length; i++) son(f[i], f[i], 0.18, 0.08, 0, i * 0.14);
    }

    public void defaite() {
        double[] f = {392, 330, 262, 196};
        for (int i = 0; i < f.length; i++) son(f[i], f[i], 0.25, 0.08, 1, i * 0.2);
    }
}