package org.java.ressources.ui;

import javax.swing.JFrame;
import javax.swing.ImageIcon;
import javax.swing.SwingUtilities;
import java.awt.Dimension;
import java.awt.GraphicsEnvironment;
import java.awt.GraphicsConfiguration;
import java.awt.Rectangle;

/** La fenêtre du jeu : redimensionnable, avec un plein écran bord à bord (F11). */
public class Fenetre {
    private final JFrame frame = new JFrame("Licorne Survivor");
    private final Jeu jeu;

    public Fenetre(Jeu jeu) {
        this.jeu = jeu;
        var icone = Fenetre.class.getResource("/icon.png");
        if (icone != null) {
            frame.setIconImage(new ImageIcon(icone).getImage());
        }
        frame.add(jeu);
        frame.pack();
        frame.setMinimumSize(new Dimension(480, 360));
        frame.setResizable(true);
        frame.setLocationRelativeTo(null);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
    }

    public void afficher() {
        frame.setVisible(true);
        if (jeu.session().reg.pleinEcran) appliquer();
        jeu.requestFocusInWindow();
    }

    /** Applique le réglage « plein écran » ou « fenêtre » de la session. */
    public void appliquer() {
        jeu.ignorerFocus(1500); // changer de mode fait perdre le focus un instant : pas de pause auto
        frame.dispose();
        if (jeu.session().reg.pleinEcran) {
            GraphicsConfiguration config = frame.getGraphicsConfiguration();
            if (config == null) {
                config = GraphicsEnvironment.getLocalGraphicsEnvironment()
                        .getDefaultScreenDevice().getDefaultConfiguration();
            }
            Rectangle ecran = config.getBounds();
            frame.setUndecorated(true);
            frame.setResizable(false);
            frame.setBounds(ecran);
            frame.setVisible(true);
            frame.toFront();
            frame.requestFocus();
        } else {
            frame.setUndecorated(false);
            frame.setResizable(true);
            frame.setExtendedState(JFrame.NORMAL);
            frame.pack();
            frame.setLocationRelativeTo(null);
            frame.setVisible(true);
        }
        SwingUtilities.invokeLater(jeu::requestFocusInWindow);
    }

    public void fermer() {
        frame.dispose();
    }
}
