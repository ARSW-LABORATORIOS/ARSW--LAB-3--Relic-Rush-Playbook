package edu.eci.arsw.relicrush.app;

import javax.swing.*;
import java.awt.*;
import java.awt.geom.Path2D;
import java.util.List;

/**
 * Illustrated adventure-map background (mountains, castle, path, compass),
 * hand-drawn with Graphics2D. Station labels are added as real child
 * components positioned along the trail, so they stay interactive (their
 * background/text still update live from LockPair's events).
 */
final class MapBoardPanel extends JPanel {
    private static final int LOGICAL_W = 600;
    private static final int LOGICAL_H = 400;
    private static final int MARKER_W = 96;
    private static final int MARKER_H = 76;

    private List<StationMarker> orderedMarkers = List.of();

    MapBoardPanel() {
        setLayout(null);
        setPreferredSize(new Dimension(LOGICAL_W, LOGICAL_H));
        setOpaque(true);
    }

    void setStationMarkers(List<StationMarker> markers) {
        removeAll();
        this.orderedMarkers = markers;
        for (StationMarker marker : markers) {
            add(marker);
        }
        revalidate();
        repaint();
    }

    @Override
    public void doLayout() {
        int w = getWidth();
        int h = getHeight();
        int n = orderedMarkers.size();
        if (n == 0) return;
        double cx = w / 2.0;
        double cy = h / 2.0;
        double rx = cx * 0.72;
        double ry = cy * 0.72;
        for (int i = 0; i < n; i++) {
            double angle = 2 * Math.PI * i / n - Math.PI / 2;
            int px = (int) (cx + rx * Math.cos(angle));
            int py = (int) (cy + ry * Math.sin(angle));
            orderedMarkers.get(i).setBounds(px - MARKER_W / 2, py - MARKER_H / 2, MARKER_W, MARKER_H);
        }
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        int w = getWidth();
        int h = getHeight();

        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.scale(w / (double) LOGICAL_W, h / (double) LOGICAL_H);

        paintSea(g2);
        paintIsland(g2);
        paintMountains(g2);
        paintCastle(g2);
        paintTrees(g2);
        paintPath(g2);
        paintCompass(g2);
        g2.dispose();

        paintTitleBanner((Graphics2D) g.create(), w);
    }

    private void paintSea(Graphics2D g2) {
        g2.setColor(new Color(0x7FA9BA));
        g2.fillRect(0, 0, LOGICAL_W, LOGICAL_H);
        g2.setColor(new Color(0x5F8EA3));
        g2.setStroke(new BasicStroke(2f));
        g2.draw(quad(40, 50, 220, 30, 380, 60));
        g2.draw(quad(60, 340, 260, 370, 560, 320));
    }

    private Path2D quad(int x1, int y1, int x2, int y2, int x3, int y3) {
        Path2D path = new Path2D.Double();
        path.moveTo(x1, y1);
        path.quadTo(x2, y2, x3, y3);
        return path;
    }

    private void paintIsland(Graphics2D g2) {
        Path2D island = new Path2D.Double();
        island.moveTo(40, 340);
        island.curveTo(20, 260, 60, 160, 140, 110);
        island.curveTo(220, 60, 340, 50, 420, 90);
        island.curveTo(500, 120, 560, 180, 560, 260);
        island.curveTo(560, 340, 480, 380, 380, 370);
        island.curveTo(280, 360, 200, 390, 120, 380);
        island.curveTo(70, 375, 50, 370, 40, 340);
        island.closePath();
        g2.setColor(new Color(0xB9C98E));
        g2.fill(island);
        g2.setColor(new Color(0x7C9A5E));
        g2.setStroke(new BasicStroke(3f));
        g2.draw(island);
    }

    private void paintMountains(Graphics2D g2) {
        fillPoly(g2, 0xA49C8A, 250, 155, 285, 95, 320, 155);
        fillPoly(g2, 0x9C9483, 300, 150, 350, 70, 400, 150);
        fillPoly(g2, 0x8D8471, 360, 140, 420, 65, 480, 140);
        fillPoly(g2, 0xF4F1E8, 375, 105, 420, 65, 445, 105);
        fillPoly(g2, 0x7C7461, 410, 150, 450, 95, 490, 150);
        fillPoly(g2, 0xF4F1E8, 425, 120, 450, 95, 465, 120);
    }

    private void paintCastle(Graphics2D g2) {
        g2.setColor(new Color(0x9A8F7A));
        g2.fillRect(88, 150, 64, 42);
        g2.setColor(new Color(0x847A67));
        g2.fillRect(88, 150, 64, 8);
        g2.setColor(new Color(0x5C5145));
        g2.fillRect(104, 164, 8, 8);
        g2.fillRect(128, 164, 8, 8);
        g2.setColor(new Color(0x8A7F6A));
        g2.fillRect(83, 128, 14, 30);
        g2.fillRect(133, 128, 14, 30);
        fillPoly(g2, 0x7A4F3A, 83, 128, 90, 111, 97, 128);
        fillPoly(g2, 0x7A4F3A, 133, 128, 140, 111, 147, 128);
        g2.setColor(new Color(0x5C421F));
        g2.fillRect(112, 160, 16, 32);
        g2.setStroke(new BasicStroke(1.5f));
        g2.drawLine(90, 111, 90, 98);
        fillPoly(g2, 0xA83B3B, 90, 98, 90, 106, 100, 102);
        g2.drawLine(140, 111, 140, 98);
        fillPoly(g2, 0xA83B3B, 140, 98, 140, 106, 150, 102);
    }

    private void paintTrees(Graphics2D g2) {
        treeRound(g2, 240, 200, 10);
        treeRound(g2, 228, 210, 7);
        treePine(g2, 160, 220);
        treeRound(g2, 440, 290, 10);
        treePine(g2, 455, 280);
        treeRound(g2, 505, 205, 8);
        treeRound(g2, 90, 330, 8);
    }

    private void treeRound(Graphics2D g2, int cx, int cy, int r) {
        g2.setColor(new Color(0x6F8F4E));
        g2.fillOval(cx - r, cy - r, r * 2, r * 2);
        g2.setColor(new Color(0x7A5C30));
        g2.fillRect(cx - 2, cy + r - 1, 4, r);
    }

    private void treePine(Graphics2D g2, int cx, int topY) {
        fillPoly(g2, 0x5F8146, cx, topY, cx + 8, topY + 20, cx - 8, topY + 20);
        g2.setColor(new Color(0x7A5C30));
        g2.fillRect(cx - 2, topY + 19, 4, 9);
    }

    private void paintPath(Graphics2D g2) {
        Path2D path = new Path2D.Double();
        path.moveTo(100, 320);
        path.quadTo(160, 290, 200, 260);
        path.quadTo(260, 280, 300, 300);
        path.quadTo(340, 250, 380, 200);
        path.quadTo(420, 220, 460, 240);
        path.quadTo(490, 190, 520, 140);
        g2.setColor(new Color(0x5C421F));
        g2.setStroke(new BasicStroke(4f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND, 1f, new float[]{2f, 10f}, 0f));
        g2.draw(path);
    }

    private void paintCompass(Graphics2D g2) {
        int cx = 555;
        int cy = 360;
        int[] xs = {cx, cx + 5, cx + 24, cx + 5, cx, cx - 5, cx - 24, cx - 5};
        int[] ys = {cy - 24, cy - 5, cy, cy + 5, cy + 24, cy + 5, cy, cy - 5};
        g2.setColor(new Color(0xF3E6C4));
        g2.fillPolygon(xs, ys, 8);
        g2.setColor(new Color(0x7A5C30));
        g2.setStroke(new BasicStroke(1.5f));
        g2.drawPolygon(xs, ys, 8);
        g2.drawOval(cx - 24, cy - 24, 48, 48);
        g2.setFont(new Font(Font.SERIF, Font.BOLD, 9));
        g2.drawString("N", cx - 3, cy - 27);
        g2.drawString("S", cx - 3, cy + 34);
        g2.drawString("E", cx + 27, cy + 4);
        g2.drawString("W", cx - 33, cy + 4);
    }

    private void paintTitleBanner(Graphics2D g2, int panelWidth) {
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        String title = "RELIC RUSH";
        g2.setFont(new Font(Font.SERIF, Font.BOLD, 16));
        FontMetrics fm = g2.getFontMetrics();
        int textW = fm.stringWidth(title);
        int bx = panelWidth / 2 - textW / 2 - 14;
        int by = 8;
        g2.setColor(new Color(0xF3E6C4));
        g2.fillRoundRect(bx, by, textW + 28, 26, 8, 8);
        g2.setColor(new Color(0x7A5C30));
        g2.setStroke(new BasicStroke(2f));
        g2.drawRoundRect(bx, by, textW + 28, 26, 8, 8);
        g2.setColor(new Color(0x5C421F));
        g2.drawString(title, panelWidth / 2 - textW / 2, by + 18);
        g2.dispose();
    }

    private void fillPoly(Graphics2D g2, int rgb, int... coords) {
        int n = coords.length / 2;
        int[] xs = new int[n];
        int[] ys = new int[n];
        for (int i = 0; i < n; i++) {
            xs[i] = coords[i * 2];
            ys[i] = coords[i * 2 + 1];
        }
        g2.setColor(new Color(rgb));
        g2.fillPolygon(xs, ys, n);
    }
}
