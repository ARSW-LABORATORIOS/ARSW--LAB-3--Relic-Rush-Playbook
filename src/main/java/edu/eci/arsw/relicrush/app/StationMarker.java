package edu.eci.arsw.relicrush.app;

import javax.swing.*;
import java.awt.*;

/**
 * A circular station marker: the icon lives inside a colored disc, the
 * station name sits below it. When the station changes hands, the disc
 * briefly pulses larger before settling back, so the transition is visible
 * even during fast back-to-back events.
 */
final class StationMarker extends JPanel {
    private static final int DIAMETER = 52;
    private static final int WIDTH = 96;
    private static final int HEIGHT = 76;

    private final StationIcon icon;
    private final JLabel nameLabel;
    private final Color freeColor = new Color(0xF3E6C4);
    private final Color freeBorder = new Color(0xB99A5F);
    private Color fillColor = freeColor;
    private Color borderColor = freeBorder;
    private double pulse = 1.0;
    private Timer pulseTimer;

    StationMarker(String stationName) {
        setLayout(null);
        setOpaque(false);
        setPreferredSize(new Dimension(WIDTH, HEIGHT));
        icon = new StationIcon(stationName);
        nameLabel = new JLabel(stationName, SwingConstants.CENTER);
        nameLabel.setFont(nameLabel.getFont().deriveFont(Font.BOLD, 10f));
        nameLabel.setBounds(0, DIAMETER + 4, WIDTH, 28);
        add(nameLabel);
    }

    void setOccupied(Color color, String who) {
        this.fillColor = color;
        this.borderColor = color.darker();
        nameLabel.setText("<html><center>en uso<br>" + who + "</center></html>");
        flash();
    }

    void setFree() {
        this.fillColor = freeColor;
        this.borderColor = freeBorder;
        nameLabel.setText("libre");
        flash();
    }

    /** Brief grow-then-settle pulse, so a state change is obvious even if it's quick. */
    private void flash() {
        if (pulseTimer != null && pulseTimer.isRunning()) {
            pulseTimer.stop();
        }
        pulse = 1.4;
        pulseTimer = new Timer(25, e -> {
            pulse = pulse - 0.05;
            if (pulse <= 1.0) {
                pulse = 1.0;
                ((Timer) e.getSource()).stop();
            }
            repaint();
        });
        pulseTimer.start();
        repaint();
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        int d = (int) (DIAMETER * pulse);
        int cx = (WIDTH - d) / 2;
        int cy = (DIAMETER - d) / 2;

        g2.setColor(fillColor);
        g2.fillOval(cx, cy, d, d);
        g2.setColor(borderColor);
        g2.setStroke(new BasicStroke(2.5f));
        g2.drawOval(cx, cy, d, d);

        int iconX = (WIDTH - icon.getIconWidth()) / 2;
        int iconY = (DIAMETER - icon.getIconHeight()) / 2;
        icon.paintIcon(this, g2, iconX, iconY);
        g2.dispose();
    }
}
