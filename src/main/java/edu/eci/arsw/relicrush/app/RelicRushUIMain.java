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
        Map<String, String> stationIcons = new HashMap<>();
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
            stationIcons.clear();
            for (edu.eci.arsw.relicrush.model.ForgeStation station : engine.stations()) {
                String icon = iconFor(station.name());
                stationIcons.put(station.name(), icon);
                JLabel label = new JLabel(stationHtml(icon, station.name(), "libre", null), SwingConstants.CENTER);
                label.setOpaque(true);
                label.setBackground(new Color(0xF3E6C4));
                label.setBorder(BorderFactory.createLineBorder(new Color(0xB99A5F), 2, true));
                label.setPreferredSize(new Dimension(150, 80));
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
                            label.setText(stationHtml(stationIcons.get(stationName), stationName, who, "#F3E6C4"));
                            label.setBackground(color);
                        }
                        case RELEASED -> {
                            label.setText(stationHtml(stationIcons.get(stationName), stationName, "libre", null));
                            label.setBackground(new Color(0xF3E6C4));
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

    private static String stationHtml(String icon, String stationName, String status, String textColorHex) {
        String color = textColorHex != null ? "color:" + textColorHex + ";" : "";
        return "<html><center style='" + color + "'>" + icon + " " + stationName + "<br><b>" + status + "</b></center></html>";
    }

    private static String iconFor(String stationName) {
        if (stationName.startsWith("Arcane Anvil")) return "⚒";
        if (stationName.startsWith("Dragon Furnace")) return "🔥";
        if (stationName.startsWith("Crystal Lens")) return "🔮";
        if (stationName.startsWith("Rune Press")) return "📜";
        if (stationName.startsWith("Moon Altar")) return "🌙";
        if (stationName.startsWith("Obsidian Table")) return "⬛";
        if (stationName.startsWith("Echo Forge")) return "🔔";
        if (stationName.startsWith("Solar Crucible")) return "☀";
        return "⚙";
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
