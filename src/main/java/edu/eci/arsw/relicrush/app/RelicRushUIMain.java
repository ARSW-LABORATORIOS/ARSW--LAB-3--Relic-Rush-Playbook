package edu.eci.arsw.relicrush.app;

import edu.eci.arsw.relicrush.concurrency.LockPair;
import edu.eci.arsw.relicrush.game.Adventurer;
import edu.eci.arsw.relicrush.game.GameConfig;
import edu.eci.arsw.relicrush.game.GameEngine;

import javax.swing.*;
import javax.swing.text.BadLocationException;
import javax.swing.text.Style;
import javax.swing.text.StyleConstants;
import javax.swing.text.StyledDocument;
import java.awt.*;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.BrokenBarrierException;

/**
 * UI bonus (does not affect the graded lab). Adventurers, forge stations and
 * their state, scores/relics, simulation state and invariants, and
 * Start/Pause/Resume/Stop controls, all wired to the real GameEngine.
 */
public final class RelicRushUIMain {
    private RelicRushUIMain() {
    }

    private static final Color[] PALETTE = {
            new Color(0x7F77DD), // purple
            new Color(0x1D9E75), // teal
            new Color(0xD85A30), // coral
            new Color(0xD4537E), // pink
            new Color(0x378ADD), // blue
            new Color(0xBA7517), // amber
            new Color(0x639922), // green
            new Color(0x9C4FC0)  // violet
    };

    public static void main(String[] args) {
        SwingUtilities.invokeLater(RelicRushUIMain::buildAndShow);
    }

    private static void buildAndShow() {
        JFrame frame = new JFrame("Relic Rush");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(900, 660);
        frame.setLocationRelativeTo(null);

        JLabel roundLabel = new JLabel("Ronda 0");
        JLabel stateLabel = new JLabel("detenido");
        JLabel invariantLabel = new JLabel("invariante: -");
        JLabel totalLabel = new JLabel("total forjado: 0");
        roundLabel.setFont(roundLabel.getFont().deriveFont(Font.BOLD, 14f));
        for (JLabel label : new JLabel[]{roundLabel, stateLabel, invariantLabel, totalLabel}) {
            label.setBorder(BorderFactory.createEmptyBorder(4, 10, 4, 10));
        }

        JPanel headerPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        headerPanel.add(roundLabel);
        headerPanel.add(stateLabel);
        headerPanel.add(invariantLabel);
        headerPanel.add(totalLabel);

        JSpinner adventurersSpinner = new JSpinner(new SpinnerNumberModel(8, 2, 64, 1));
        JSpinner stationsSpinner = new JSpinner(new SpinnerNumberModel(6, 2, 8, 1));
        JSpinner roundsSpinner = new JSpinner(new SpinnerNumberModel(25, 1, 500, 1));

        JPanel configPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        configPanel.add(new JLabel("Aventureros:"));
        configPanel.add(adventurersSpinner);
        configPanel.add(new JLabel("Estaciones:"));
        configPanel.add(stationsSpinner);
        configPanel.add(new JLabel("Rondas:"));
        configPanel.add(roundsSpinner);

        JButton startButton = new JButton("Start");
        JButton pauseButton = new JButton("Pause");
        JButton resumeButton = new JButton("Resume");
        JButton stopButton = new JButton("Stop");
        pauseButton.setEnabled(false);
        resumeButton.setEnabled(false);
        stopButton.setEnabled(false);

        JPanel controlPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        controlPanel.add(startButton);
        controlPanel.add(pauseButton);
        controlPanel.add(resumeButton);
        controlPanel.add(stopButton);

        JPanel top = new JPanel();
        top.setLayout(new BoxLayout(top, BoxLayout.Y_AXIS));
        top.setBackground(new Color(0xF3E6C4));
        headerPanel.setBackground(new Color(0xF3E6C4));
        configPanel.setBackground(new Color(0xF3E6C4));
        controlPanel.setBackground(new Color(0xF3E6C4));
        top.add(headerPanel);
        top.add(configPanel);
        top.add(controlPanel);

        JPanel stationGrid = new JPanel(new GridLayout(0, 4, 10, 10));
        stationGrid.setBorder(BorderFactory.createTitledBorder("Forge stations"));
        stationGrid.setBackground(new Color(0x2A2E35));

        DefaultListModel<String> rosterModel = new DefaultListModel<>();
        JList<String> rosterList = new JList<>(rosterModel);
        Map<String, Color> adventurerColors = new HashMap<>();
        rosterList.setCellRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(
                    JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
                JLabel label = (JLabel) super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                String text = String.valueOf(value);
                String name = text.split(" : ")[0];
                Color color = adventurerColors.get(name);
                label.setFont(label.getFont().deriveFont(Font.BOLD));
                if (color != null) {
                    label.setForeground(isSelected ? Color.WHITE : color.darker());
                }
                return label;
            }
        });
        JScrollPane rosterScroll = new JScrollPane(rosterList);
        rosterScroll.setPreferredSize(new Dimension(220, 400));
        rosterScroll.setBorder(BorderFactory.createTitledBorder("Adventurers"));

        JTextPane logPane = new JTextPane();
        logPane.setEditable(false);
        logPane.setBackground(new Color(0x1E2126));
        JScrollPane logScroll = new JScrollPane(logPane);
        logScroll.setPreferredSize(new Dimension(900, 150));
        logScroll.setBorder(BorderFactory.createTitledBorder("Activity log"));

        frame.setLayout(new BorderLayout(8, 8));
        frame.add(top, BorderLayout.NORTH);
        frame.add(stationGrid, BorderLayout.CENTER);
        frame.add(rosterScroll, BorderLayout.EAST);
        frame.add(logScroll, BorderLayout.SOUTH);

        Map<String, JLabel> stationLabels = new HashMap<>();
        GameEngine[] engineHolder = new GameEngine[1];

        startButton.addActionListener(e -> {
            int adventurersCount = (Integer) adventurersSpinner.getValue();
            int stationsCount = (Integer) stationsSpinner.getValue();
            int roundsCount = (Integer) roundsSpinner.getValue();
            GameConfig config = new GameConfig(adventurersCount, stationsCount, roundsCount);
            GameEngine engine = new GameEngine(config);
            engineHolder[0] = engine;

            adventurerColors.clear();
            for (int i = 1; i <= adventurersCount; i++) {
                adventurerColors.put("adventurer-" + i, PALETTE[(i - 1) % PALETTE.length]);
            }

            stationGrid.removeAll();
            stationLabels.clear();
            for (edu.eci.arsw.relicrush.model.ForgeStation station : engine.stations()) {
                JLabel label = new JLabel(stationHtml(station.name(), "libre"), SwingConstants.CENTER);
                label.setIcon(new StationIcon(station.name()));
                label.setVerticalTextPosition(SwingConstants.BOTTOM);
                label.setHorizontalTextPosition(SwingConstants.CENTER);
                label.setOpaque(true);
                label.setBackground(new Color(0xF3E6C4));
                label.setBorder(BorderFactory.createLineBorder(new Color(0xB99A5F), 2, true));
                label.setPreferredSize(new Dimension(150, 100));
                stationGrid.add(label);
                stationLabels.put(station.name(), label);
            }
            stationGrid.revalidate();
            stationGrid.repaint();

            rosterModel.clear();
            for (int i = 1; i <= adventurersCount; i++) {
                rosterModel.addElement("adventurer-" + i + " : 0 relics");
            }
            logPane.setText("");

            LockPair.setListener((who, stationName, type) -> SwingUtilities.invokeLater(() -> {
                JLabel label = stationLabels.get(stationName);
                Color color = adventurerColors.getOrDefault(who, Color.DARK_GRAY);
                if (label != null) {
                    switch (type) {
                        case ACQUIRED -> {
                            label.setText(stationHtml(stationName, who));
                            label.setBackground(color);
                            label.setForeground(Color.WHITE);
                        }
                        case RELEASED -> {
                            label.setText(stationHtml(stationName, "libre"));
                            label.setBackground(new Color(0xF3E6C4));
                            label.setForeground(Color.BLACK);
                        }
                        case WAITING -> {
                            // Station keeps its current color; the log line is the evidence of the wait.
                        }
                    }
                }
                appendLog(logPane, who + " " + type.toString().toLowerCase() + " " + stationName, color);
            }));

            engine.setRoundListener((round, scoreSum, ledgerTotal, eventCount, invariantOk, roundAdventurers) ->
                    SwingUtilities.invokeLater(() -> {
                        roundLabel.setText("Ronda " + round);
                        totalLabel.setText("total forjado: " + ledgerTotal);
                        invariantLabel.setText(String.format(
                                "invariante: %s (%d = %d = %d)",
                                invariantOk ? "OK" : "ROTO", scoreSum, ledgerTotal, eventCount));
                        invariantLabel.setForeground(invariantOk ? new Color(0x2E7D32) : new Color(0xC62828));
                        for (int i = 0; i < roundAdventurers.size(); i++) {
                            Adventurer adventurer = roundAdventurers.get(i);
                            rosterModel.set(i, adventurer.getName() + " : " + adventurer.score() + " relics");
                        }
                    }));

            Thread engineThread = new Thread(() -> {
                try {
                    engine.run();
                    SwingUtilities.invokeLater(() -> setState(stateLabel, startButton, pauseButton, resumeButton, stopButton, "finalizado"));
                } catch (InterruptedException | BrokenBarrierException ex) {
                    SwingUtilities.invokeLater(() -> setState(stateLabel, startButton, pauseButton, resumeButton, stopButton, "detenido"));
                }
            }, "relic-rush-ui-engine");
            engineThread.start();

            setState(stateLabel, startButton, pauseButton, resumeButton, stopButton, "corriendo");
        });

        pauseButton.addActionListener(e -> {
            if (engineHolder[0] != null) {
                engineHolder[0].pause();
                setState(stateLabel, startButton, pauseButton, resumeButton, stopButton, "pausado");
            }
        });
        resumeButton.addActionListener(e -> {
            if (engineHolder[0] != null) {
                engineHolder[0].resume();
                setState(stateLabel, startButton, pauseButton, resumeButton, stopButton, "corriendo");
            }
        });
        stopButton.addActionListener(e -> {
            if (engineHolder[0] != null) {
                engineHolder[0].stopAll();
                setState(stateLabel, startButton, pauseButton, resumeButton, stopButton, "detenido");
            }
        });

        frame.setVisible(true);
    }

    private static String stationHtml(String stationName, String status) {
        return "<html><center>" + stationName + "<br><b>" + status + "</b></center></html>";
    }

    private static String kindFor(String stationName) {
        if (stationName.startsWith("Arcane Anvil")) return "anvil";
        if (stationName.startsWith("Dragon Furnace")) return "furnace";
        if (stationName.startsWith("Crystal Lens")) return "lens";
        if (stationName.startsWith("Rune Press")) return "press";
        if (stationName.startsWith("Moon Altar")) return "altar";
        if (stationName.startsWith("Obsidian Table")) return "table";
        return "default";
    }

    /**
     * Hand-drawn station icon (same shapes as the chat mockup, redone with
     * Graphics2D) so it renders consistently regardless of which emoji
     * glyphs the OS font happens to support.
     */
    private static final class StationIcon implements Icon {
        private static final int SIZE = 34;
        private final String kind;

        StationIcon(String stationName) {
            this.kind = kindFor(stationName);
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

    private static void appendLog(JTextPane pane, String text, Color color) {
        StyledDocument doc = pane.getStyledDocument();
        Style style = pane.addStyle("entry", null);
        StyleConstants.setForeground(style, color);
        try {
            doc.insertString(doc.getLength(), text + "\n", style);
        } catch (BadLocationException ignored) {
            // Text area is append-only; this can't happen with doc.getLength() as the offset.
        }
        pane.setCaretPosition(doc.getLength());
    }

    private static void setState(
            JLabel stateLabel, JButton start, JButton pause, JButton resume, JButton stop, String state) {
        stateLabel.setText(state);
        start.setEnabled(state.equals("detenido") || state.equals("finalizado"));
        pause.setEnabled(state.equals("corriendo"));
        resume.setEnabled(state.equals("pausado"));
        stop.setEnabled(state.equals("corriendo") || state.equals("pausado"));
    }
}
