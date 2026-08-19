package edu.eci.arsw.relicrush.app;

import javax.swing.*;
import java.awt.*;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * A circular station marker: the icon lives inside a colored disc, the
 * station name sits below it. Color and text change immediately when the
 * station changes hands - no scale/pulse animation, so several stations
 * updating at once still reads clean instead of noisy.
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
    private final Map<String, Color> waitingAdventurers = new LinkedHashMap<>();

    StationMarker(String stationName) {
        setLayout(null);
        setOpaque(false);
        setPreferredSize(new Dimension(WIDTH, HEIGHT));
        setToolTipText("libre");
        icon = new StationIcon(stationName);
        nameLabel = new JLabel(stationName, SwingConstants.CENTER);
        nameLabel.setFont(nameLabel.getFont().deriveFont(Font.BOLD, 10f));
        nameLabel.setBounds(0, DIAMETER + 4, WIDTH, 20);
        add(nameLabel);
    }

    /** Only the color changes; the station name label never gets touched or replaced. */
    void setOccupied(Color color, String who) {
        this.fillColor = color;
        this.borderColor = color.darker();
        setToolTipText("En uso: " + who);
        repaint();
    }

    void setFree() {
        this.fillColor = freeColor;
        this.borderColor = freeBorder;
        setToolTipText("libre");
        repaint();
    }

    /** Someone is blocked waiting for this station's lock: shown as a small "z" badge. */
    void addWaiting(String who, Color color) {
        waitingAdventurers.put(who, color);
        repaint();
    }

    /** They got the lock (or gave up waiting on it): clear their "z" badge. */
    void removeWaiting(String who) {
        if (waitingAdventurers.remove(who) != null) {
            repaint();
        }
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        int cx = (WIDTH - DIAMETER) / 2;

        g2.setColor(fillColor);
        g2.fillOval(cx, 0, DIAMETER, DIAMETER);
        g2.setColor(borderColor);
        g2.setStroke(new BasicStroke(2.5f));
        g2.drawOval(cx, 0, DIAMETER, DIAMETER);

        int iconX = (WIDTH - icon.getIconWidth()) / 2;
        int iconY = (DIAMETER - icon.getIconHeight()) / 2;
        icon.paintIcon(this, g2, iconX, iconY);

        paintWaitingBadge(g2, cx);
        g2.dispose();
    }

    /** Small ascending "z"s in the color of whoever is sleeping/blocked waiting on this station. */
    private void paintWaitingBadge(Graphics2D g2, int cx) {
        if (waitingAdventurers.isEmpty()) {
            return;
        }
        int baseX = cx + DIAMETER - 4;
        int baseY = 14;
        int i = 0;
        for (Color waitingColor : waitingAdventurers.values()) {
            if (i >= 3) {
                break;
            }
            float size = 9f + i * 2.5f;
            g2.setFont(getFont().deriveFont(Font.BOLD, size));
            g2.setColor(waitingColor);
            g2.drawString("z", baseX + i * 5, baseY - i * 8);
            i++;
        }
    }
}
