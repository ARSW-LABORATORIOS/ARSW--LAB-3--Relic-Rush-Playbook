package edu.eci.arsw.relicrush.app;

import edu.eci.arsw.relicrush.concurrency.LockPair;
import edu.eci.arsw.relicrush.game.Adventurer;
import edu.eci.arsw.relicrush.game.GameConfig;
import edu.eci.arsw.relicrush.game.GameEngine;

import javax.swing.*;
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

    public static void main(String[] args) {
        SwingUtilities.invokeLater(RelicRushUIMain::buildAndShow);
    }

    private static void buildAndShow() {
        JFrame frame = new JFrame("Relic Rush");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(880, 640);
        frame.setLocationRelativeTo(null);

        JLabel roundLabel = new JLabel("Ronda 0");
        JLabel stateLabel = new JLabel("detenido");
        JLabel invariantLabel = new JLabel("invariante: -");
        JLabel totalLabel = new JLabel("total forjado: 0");
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
        top.add(headerPanel);
        top.add(configPanel);
        top.add(controlPanel);

        JPanel stationGrid = new JPanel(new GridLayout(0, 4, 8, 8));
        stationGrid.setBorder(BorderFactory.createTitledBorder("Forge stations"));

        DefaultListModel<String> rosterModel = new DefaultListModel<>();
        JList<String> rosterList = new JList<>(rosterModel);
        JScrollPane rosterScroll = new JScrollPane(rosterList);
        rosterScroll.setPreferredSize(new Dimension(220, 400));
        rosterScroll.setBorder(BorderFactory.createTitledBorder("Adventurers"));

        JTextArea logArea = new JTextArea();
        logArea.setEditable(false);
        JScrollPane logScroll = new JScrollPane(logArea);
        logScroll.setPreferredSize(new Dimension(880, 150));
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

            stationGrid.removeAll();
            stationLabels.clear();
            for (int i = 1; i <= stationsCount; i++) {
                JLabel label = new JLabel("libre", SwingConstants.CENTER);
                label.setOpaque(true);
                label.setBackground(Color.LIGHT_GRAY);
                label.setBorder(BorderFactory.createTitledBorder("Station " + i));
                label.setPreferredSize(new Dimension(140, 70));
                stationGrid.add(label);
            }
            stationGrid.revalidate();
            stationGrid.repaint();

            rosterModel.clear();
            for (int i = 1; i <= adventurersCount; i++) {
                rosterModel.addElement("adventurer-" + i + " : 0 relics");
            }
            logArea.setText("");

            LockPair.setListener((who, stationName, type) -> SwingUtilities.invokeLater(() -> {
                JLabel label = stationLabels.get(stationName);
                if (label != null) {
                    switch (type) {
                        case ACQUIRED -> {
                            label.setText("<html><center>en uso<br>" + who + "</center></html>");
                            label.setBackground(new Color(0xB7E1CD));
                        }
                        case RELEASED -> {
                            label.setText("libre");
                            label.setBackground(Color.LIGHT_GRAY);
                        }
                        case WAITING -> {
                            // Station keeps its current color; log line is enough evidence.
                        }
                    }
                }
                logArea.append(who + " " + type + " " + stationName + "\n");
                logArea.setCaretPosition(logArea.getDocument().getLength());
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

            // Match each label created above to its station by name, so the listener can find it.
            wireStationLabels(stationGrid, stationLabels, engine);

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

    private static void wireStationLabels(JPanel stationGrid, Map<String, JLabel> stationLabels, GameEngine engine) {
        Component[] components = stationGrid.getComponents();
        java.util.List<edu.eci.arsw.relicrush.model.ForgeStation> stations = engine.stations();
        for (int i = 0; i < components.length && i < stations.size(); i++) {
            stationLabels.put(stations.get(i).name(), (JLabel) components[i]);
        }
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
