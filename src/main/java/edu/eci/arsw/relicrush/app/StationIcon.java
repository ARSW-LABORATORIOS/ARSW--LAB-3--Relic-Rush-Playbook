package edu.eci.arsw.relicrush.app;

import javax.swing.*;
import java.awt.*;

/**
 * Hand-drawn station icon so it renders consistently regardless of which
 * emoji glyphs the OS font happens to support (Swing does not render color
 * emoji reliably).
 */
final class StationIcon implements Icon {
    private static final int SIZE = 34;
    private final String kind;

    StationIcon(String stationName) {
        this.kind = kindFor(stationName);
    }

    static String kindFor(String stationName) {
        if (stationName.startsWith("Arcane Anvil")) return "anvil";
        if (stationName.startsWith("Dragon Furnace")) return "furnace";
        if (stationName.startsWith("Crystal Lens")) return "lens";
        if (stationName.startsWith("Rune Press")) return "press";
        if (stationName.startsWith("Moon Altar")) return "altar";
        if (stationName.startsWith("Obsidian Table")) return "table";
        return "default";
    }

    @Override
    public void paintIcon(Component c, Graphics g, int x, int y) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.translate(x, y);
        switch (kind) {
            case "anvil" -> paintAnvil(g2);
            case "furnace" -> paintFurnace(g2);
            case "lens" -> paintLens(g2);
            case "press" -> paintPress(g2);
            case "altar" -> paintAltar(g2);
            case "table" -> paintTable(g2);
            default -> paintDefault(g2);
        }
        g2.dispose();
    }

    @Override
    public int getIconWidth() {
        return SIZE;
    }

    @Override
    public int getIconHeight() {
        return SIZE;
    }

    private void paintAnvil(Graphics2D g2) {
        g2.setColor(new Color(0x6B6B6F));
        g2.fillRect(4, 20, 26, 5);
        g2.setColor(new Color(0x4A4A4D));
        g2.fillRect(14, 25, 6, 6);
        g2.setColor(new Color(0x8A8A8E));
        g2.fillArc(4, 11, 26, 14, 0, 180);
        g2.setStroke(new BasicStroke(2.5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g2.setColor(new Color(0x7A5C30));
        g2.drawLine(10, 5, 17, 13);
        g2.drawLine(24, 5, 17, 13);
        g2.fillOval(8, 2, 4, 4);
        g2.fillOval(22, 2, 4, 4);
    }

    private void paintFurnace(Graphics2D g2) {
        g2.setColor(new Color(0x6B5A4A));
        g2.fillPolygon(new int[]{9, 25, 22, 12}, new int[]{31, 31, 15, 15}, 4);
        g2.setColor(new Color(0xE0622C));
        g2.fillOval(12, 4, 10, 14);
        g2.setColor(new Color(0xF6A13A));
        g2.fillOval(15, 8, 4, 8);
    }

    private void paintLens(Graphics2D g2) {
        int[] xs = {17, 27, 17, 7};
        int[] ys = {3, 17, 31, 17};
        g2.setColor(new Color(0x8FB8D9));
        g2.fillPolygon(xs, ys, 4);
        g2.setColor(new Color(0x4A6B85));
        g2.setStroke(new BasicStroke(1.2f));
        g2.drawPolygon(xs, ys, 4);
    }

    private void paintPress(Graphics2D g2) {
        g2.setColor(new Color(0x7A6A52));
        g2.fillRoundRect(6, 4, 22, 26, 4, 4);
        g2.setColor(new Color(0xE8C86A));
        g2.setStroke(new BasicStroke(1.5f));
        g2.drawLine(10, 12, 24, 12);
        g2.drawLine(10, 17, 20, 17);
        g2.drawLine(10, 22, 24, 22);
    }

    private void paintAltar(Graphics2D g2) {
        g2.setColor(new Color(0x7C7F95));
        g2.fillPolygon(new int[]{7, 27, 22, 12}, new int[]{31, 31, 22, 22}, 4);
        g2.setColor(new Color(0xE8DFA0));
        g2.fillArc(11, 2, 17, 17, 60, 240);
        g2.setColor(new Color(0xF3E6C4));
        g2.fillArc(15, 4, 15, 15, 60, 240);
    }

    private void paintTable(Graphics2D g2) {
        g2.setColor(new Color(0x241F2E));
        g2.fillPolygon(new int[]{17, 27, 27, 17, 7, 7}, new int[]{4, 10, 20, 27, 20, 10}, 6);
        g2.setColor(new Color(0x9A7CE0));
        g2.fillOval(14, 13, 4, 4);
        g2.fillOval(20, 17, 3, 3);
    }

    private void paintDefault(Graphics2D g2) {
        g2.setColor(new Color(0x8A8F98));
        g2.fillOval(6, 6, 22, 22);
    }
}
