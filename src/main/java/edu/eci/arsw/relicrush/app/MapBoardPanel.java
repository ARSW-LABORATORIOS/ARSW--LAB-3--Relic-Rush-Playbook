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
    private static final int LOGICAL_W = 900;
    private static final int LOGICAL_H = 600;
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
        double rx = cx * 0.58;
        double ry = cy * 0.58;
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
        g2.draw(quad(40, 80, 300, 50, 560, 90));
        g2.draw(quad(100, 30, 400, 15, 700, 45));
        g2.draw(quad(60, 510, 350, 545, 700, 500));
        g2.draw(quad(200, 570, 500, 590, 820, 555));
    }

    private Path2D quad(int x1, int y1, int x2, int y2, int x3, int y3) {
        Path2D path = new Path2D.Double();
        path.moveTo(x1, y1);
        path.quadTo(x2, y2, x3, y3);
        return path;
    }

    private void paintIsland(Graphics2D g2) {
        Path2D island = new Path2D.Double();
        island.moveTo(60, 510);
        island.curveTo(30, 400, 50, 250, 120, 160);
        island.curveTo(200, 80, 380, 55, 540, 80);
        island.curveTo(700, 105, 820, 200, 840, 340);
        island.curveTo(855, 450, 780, 530, 660, 545);
        island.curveTo(520, 560, 340, 575, 200, 560);
        island.curveTo(110, 550, 70, 535, 60, 510);
        island.closePath();
        g2.setColor(new Color(0xB9C98E));
        g2.fill(island);
        g2.setColor(new Color(0x7C9A5E));
        g2.setStroke(new BasicStroke(3f));
        g2.draw(island);
    }

    private void paintMountains(Graphics2D g2) {
        // cadena de montañas en el fondo derecho, lejos del centro
        fillPoly(g2, 0xA49C8A, 580, 240, 630, 155, 680, 240);
        fillPoly(g2, 0x9C9483, 640, 230, 710, 130, 780, 230);
        fillPoly(g2, 0x8D8471, 720, 220, 800, 110, 870, 220);
        // nieve en los picos
        fillPoly(g2, 0xF4F1E8, 695, 165, 710, 130, 728, 165);
        fillPoly(g2, 0xF4F1E8, 778, 148, 800, 110, 820, 148);
        // montaña extra izquierda para dar profundidad
        fillPoly(g2, 0xB0A896, 530, 250, 575, 175, 620, 250);
    }

    private void paintCastle(Graphics2D g2) {
        // base del castillo
        g2.setColor(new Color(0x9A8F7A));
        g2.fillRect(100, 180, 110, 80);
        // almenas superiores de la muralla
        g2.setColor(new Color(0x847A67));
        for (int bx = 100; bx < 210; bx += 18) {
            g2.fillRect(bx, 165, 12, 18);
        }
        // ventanas
        g2.setColor(new Color(0x3A3028));
        g2.fillRect(120, 200, 14, 18);
        g2.fillRect(152, 200, 14, 18);
        g2.fillRect(184, 200, 14, 18);
        // puerta
        g2.setColor(new Color(0x3A3028));
        g2.fillRect(148, 228, 18, 32);
        g2.fillArc(148, 220, 18, 16, 0, 180);
        // torre izquierda
        g2.setColor(new Color(0x8A7F6A));
        g2.fillRect(88, 148, 28, 60);
        // almenas torre izquierda
        g2.setColor(new Color(0x756B58));
        for (int bx = 88; bx < 116; bx += 10) {
            g2.fillRect(bx, 136, 7, 14);
        }
        // torre derecha
        g2.setColor(new Color(0x8A7F6A));
        g2.fillRect(194, 148, 28, 60);
        // almenas torre derecha
        g2.setColor(new Color(0x756B58));
        for (int bx = 194; bx < 222; bx += 10) {
            g2.fillRect(bx, 136, 7, 14);
        }
        // techo cónico torre izquierda
        fillPoly(g2, 0x7A3A2A, 88, 148, 102, 112, 116, 148);
        // techo cónico torre derecha
        fillPoly(g2, 0x7A3A2A, 194, 148, 208, 112, 222, 148);
        // bandera torre izquierda
        g2.setColor(new Color(0x5C421F));
        g2.setStroke(new BasicStroke(1.5f));
        g2.drawLine(102, 112, 102, 92);
        fillPoly(g2, 0xC03030, 102, 92, 102, 106, 118, 99);
        // bandera torre derecha
        g2.drawLine(208, 112, 208, 92);
        fillPoly(g2, 0xC03030, 208, 92, 208, 106, 224, 99);
    }

    private void paintTrees(Graphics2D g2) {
        treeRound(g2, 340, 460, 14);
        treeRound(g2, 320, 475, 10);
        treePine(g2, 370, 440);
        treePine(g2, 390, 455);
        treeRound(g2, 680, 430, 13);
        treePine(g2, 700, 415);
        treeRound(g2, 740, 445, 10);
        treeRound(g2, 130, 460, 11);
        treePine(g2, 150, 445);
        treeRound(g2, 760, 200, 11);
        treePine(g2, 780, 185);
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
        path.moveTo(155, 300);
        path.quadTo(200, 430, 300, 490);
        path.quadTo(430, 540, 560, 500);
        path.quadTo(700, 460, 780, 370);
        path.quadTo(820, 280, 760, 190);
        path.quadTo(700, 130, 600, 120);
        path.quadTo(480, 110, 380, 140);
        path.quadTo(270, 170, 200, 240);
        path.quadTo(155, 270, 155, 300);
        g2.setColor(new Color(0x8B6914));
        g2.setStroke(new BasicStroke(6f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND, 1f, new float[]{4f, 14f}, 0f));
        g2.draw(path);
    }

    private void paintCompass(Graphics2D g2) {
        int cx = 855;
        int cy = 555;
        int[] xs = {cx, cx + 7, cx + 32, cx + 7, cx, cx - 7, cx - 32, cx - 7};
        int[] ys = {cy - 32, cy - 7, cy, cy + 7, cy + 32, cy + 7, cy, cy - 7};
        g2.setColor(new Color(0xF3E6C4));
        g2.fillPolygon(xs, ys, 8);
        g2.setColor(new Color(0x7A5C30));
        g2.setStroke(new BasicStroke(1.5f));
        g2.drawPolygon(xs, ys, 8);
        g2.drawOval(cx - 32, cy - 32, 64, 64);
        g2.setFont(new Font(Font.SERIF, Font.BOLD, 11));
        g2.drawString("N", cx - 4, cy - 36);
        g2.drawString("S", cx - 4, cy + 46);
        g2.drawString("E", cx + 36, cy + 4);
        g2.drawString("W", cx - 46, cy + 4);
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
