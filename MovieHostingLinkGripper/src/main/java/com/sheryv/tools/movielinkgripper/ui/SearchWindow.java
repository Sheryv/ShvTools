package com.sheryv.tools.movielinkgripper.ui;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.sheryv.tools.movielinkgripper.Episode;
import com.sheryv.tools.movielinkgripper.Series;
import com.sheryv.tools.movielinkgripper.Transformer;
import com.sheryv.tools.movielinkgripper.config.Configuration;
import com.sheryv.tools.movielinkgripper.search.SearchItem;
import com.sheryv.tools.movielinkgripper.search.TmdbApi;
import com.sheryv.util.FileUtils;
import com.sheryv.util.SerialisationUtils;

import javax.swing.*;
import java.awt.*;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

public class SearchWindow {
    private JPanel properties;
    private JPanel episodes;
    private JTextField searchText;
    private JButton searchBtn;
    private JButton loadFromFileBtn;
    private JButton saveToFileBtn;
    private JPanel mainPanel;
    private JSpinner seasonNumber;
    private JTextField detailsField;
    private JTextField nameField;
    private JButton showFileListButton;
    private TmdbApi api = new TmdbApi();
    private Series lastSeries;
    private Configuration configuration;

    public SearchWindow init() {
        configuration = Configuration.get();
        seasonNumber.setValue(1);
        searchBtn.addActionListener(e -> {
            detailsField.setText("");
            nameField.setText("");
            api.searchTv(searchText.getText()).ifPresent(i -> {
                searchBtn.setEnabled(false);
                Integer season = (Integer) seasonNumber.getValue();
                setDetails(i, season);
                fillList(null);
                Executors.newSingleThreadExecutor().submit(() -> {
                    List<Episode> episodeList = Collections.emptyList();
                    try {
                        episodeList = api.getTvEpisodes(i.getId(), season).getEpisodes()
                                .stream()
                                .map(ep -> {
                                    Optional<Episode> found = lastSeries.getEpisodes().stream()
                                            .filter(l -> l.getN() == ep.getEpisodeNumber() && season == lastSeries.getSeason())
                                            .findAny();
                                    if (found.isPresent()) {
                                        Episode f = found.get();
                                        return new Episode(f.getPage(), ep.getName(), (int) ep.getEpisodeNumber(), f.getDlLink(), 0, f.getType(), f.getFormat());
                                    }
                                    return new Episode("", ep.getName(), (int) ep.getEpisodeNumber(), "");
                                })
                                .collect(Collectors.toList());
                    } catch (Exception ex) {
                        ex.printStackTrace();
                    } finally {
                        final List<Episode> copy = new ArrayList<>(episodeList);
                        EventQueue.invokeLater(() -> {
                            searchBtn.setEnabled(true);
                            if (!copy.isEmpty()) {
                                Series series = new Series(i.getName(), season, lastSeries.getLang(), lastSeries.getProviderUrl(), copy);
                                fillList(series);
                            }
                        });
                    }
                });

            });
        });

        loadFromFileBtn.addActionListener(e -> {
            loadFromFile();
        });

        saveToFileBtn.addActionListener(e -> {
            if (lastSeries != null) {
                try {
                    FileUtils.saveFile(SerialisationUtils.toJsonPretty(lastSeries), Paths.get(configuration.getDefaultFilePathWithEpisodesList()));
                } catch (JsonProcessingException ex) {
                    ex.printStackTrace();
                }
            }
        });
        showFileListButton.addActionListener(e -> showFileNamesList(lastSeries));

        episodes.setLayout(new BoxLayout(episodes, BoxLayout.Y_AXIS));
        episodes.setAlignmentY(0);
        loadFromFile();
        return this;
    }

    private void loadFromFile() {
        try {
            Series series = Transformer.loadSeries(FileUtils.readFileInMemory(configuration.getDefaultFilePathWithEpisodesList()));
            seasonNumber.setValue(series.getSeason());
            searchText.setText(series.getName());
            fillList(series);
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }

    private void fillList(Series season) {
        episodes.removeAll();
        if (season == null)
            return;

        lastSeries = season;
        for (Episode episode : season.getEpisodes()) {
            UiUtils.appendRow(episodes, String.format("%02d: %s", episode.getN(), episode.getName()), episode.getDlLink(), String.class, episode::setDlLink);
        }
        episodes.updateUI();
    }

    private void setDetails(SearchItem i, int season) {
        nameField.setText(i.getName());
        detailsField.setText(String.format("%s [%d, %s, S%d] %s | %.2f", i.getOriginalName(),
                i.getId(), i.getOriginalLanguage(), season, i.getFirstAirDate(), i.getPopularity()));

    }

    private void showFileNamesList(Series series) {
        String collect = series.getEpisodes().stream().map(e -> e.generateFileName(series)).collect(Collectors.joining("\n"));
        JTextArea textarea = new JTextArea(collect);
        textarea.setEditable(false);
        Font font = textarea.getFont();
        textarea.setFont(font.deriveFont(font.getSize() + 5f));
        JOptionPane.showMessageDialog(null, textarea, "Episodes files list", JOptionPane.PLAIN_MESSAGE);
    }

    public JPanel getMainPanel() {
        return mainPanel;
    }

    {
// GUI initializer generated by IntelliJ IDEA GUI Designer
// >>> IMPORTANT!! <<<
// DO NOT EDIT OR ADD ANY CODE HERE!
        $$$setupUI$$$();
    }

    /**
     * Method generated by IntelliJ IDEA GUI Designer
     * >>> IMPORTANT!! <<<
     * DO NOT edit this method OR call it in your code!
     *
     * @noinspection ALL
     */
    private void $$$setupUI$$$() {
        mainPanel = new JPanel();
        mainPanel.setLayout(new GridBagLayout());
        final JPanel spacer1 = new JPanel();
        GridBagConstraints gbc;
        gbc = new GridBagConstraints();
        gbc.gridx = 4;
        gbc.gridy = 1;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        mainPanel.add(spacer1, gbc);
        final JPanel spacer2 = new JPanel();
        gbc = new GridBagConstraints();
        gbc.gridx = 1;
        gbc.gridy = 2;
        gbc.gridwidth = 3;
        gbc.fill = GridBagConstraints.VERTICAL;
        mainPanel.add(spacer2, gbc);
        final JPanel spacer3 = new JPanel();
        gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 1;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        mainPanel.add(spacer3, gbc);
        final JPanel spacer4 = new JPanel();
        gbc = new GridBagConstraints();
        gbc.gridx = 1;
        gbc.gridy = 0;
        gbc.gridwidth = 3;
        gbc.fill = GridBagConstraints.VERTICAL;
        mainPanel.add(spacer4, gbc);
        final JPanel spacer5 = new JPanel();
        gbc = new GridBagConstraints();
        gbc.gridx = 1;
        gbc.gridy = 6;
        gbc.gridwidth = 3;
        gbc.fill = GridBagConstraints.VERTICAL;
        mainPanel.add(spacer5, gbc);
        final JPanel panel1 = new JPanel();
        panel1.setLayout(new GridBagLayout());
        gbc = new GridBagConstraints();
        gbc.gridx = 1;
        gbc.gridy = 7;
        gbc.gridwidth = 3;
        gbc.fill = GridBagConstraints.BOTH;
        mainPanel.add(panel1, gbc);
        final JPanel spacer6 = new JPanel();
        gbc = new GridBagConstraints();
        gbc.gridx = 1;
        gbc.gridy = 8;
        gbc.gridwidth = 3;
        gbc.fill = GridBagConstraints.VERTICAL;
        mainPanel.add(spacer6, gbc);
        properties = new JPanel();
        properties.setLayout(new GridBagLayout());
        gbc = new GridBagConstraints();
        gbc.gridx = 1;
        gbc.gridy = 1;
        gbc.gridwidth = 3;
        gbc.weightx = 1.0;
        gbc.fill = GridBagConstraints.BOTH;
        mainPanel.add(properties, gbc);
        final JLabel label1 = new JLabel();
        label1.setText("Search name");
        gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.anchor = GridBagConstraints.WEST;
        properties.add(label1, gbc);
        final JPanel spacer7 = new JPanel();
        gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 1;
        gbc.fill = GridBagConstraints.VERTICAL;
        properties.add(spacer7, gbc);
        searchText = new JTextField();
        gbc = new GridBagConstraints();
        gbc.gridx = 2;
        gbc.gridy = 0;
        gbc.gridwidth = 6;
        gbc.weightx = 1.0;
        gbc.anchor = GridBagConstraints.WEST;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        properties.add(searchText, gbc);
        loadFromFileBtn = new JButton();
        loadFromFileBtn.setText("Load From File");
        gbc = new GridBagConstraints();
        gbc.gridx = 4;
        gbc.gridy = 2;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        properties.add(loadFromFileBtn, gbc);
        searchBtn = new JButton();
        searchBtn.setText("Search");
        gbc = new GridBagConstraints();
        gbc.gridx = 9;
        gbc.gridy = 0;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        properties.add(searchBtn, gbc);
        saveToFileBtn = new JButton();
        saveToFileBtn.setText("Save to File");
        gbc = new GridBagConstraints();
        gbc.gridx = 6;
        gbc.gridy = 2;
        gbc.gridwidth = 2;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        properties.add(saveToFileBtn, gbc);
        seasonNumber = new JSpinner();
        seasonNumber.setMinimumSize(new Dimension(50, 30));
        gbc = new GridBagConstraints();
        gbc.gridx = 2;
        gbc.gridy = 2;
        gbc.weightx = 0.1;
        gbc.anchor = GridBagConstraints.WEST;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        properties.add(seasonNumber, gbc);
        final JPanel spacer8 = new JPanel();
        gbc = new GridBagConstraints();
        gbc.gridx = 3;
        gbc.gridy = 2;
        gbc.weightx = 0.9;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        properties.add(spacer8, gbc);
        final JLabel label2 = new JLabel();
        label2.setText("Season");
        gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 2;
        gbc.anchor = GridBagConstraints.WEST;
        properties.add(label2, gbc);
        final JPanel spacer9 = new JPanel();
        gbc = new GridBagConstraints();
        gbc.gridx = 1;
        gbc.gridy = 0;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        properties.add(spacer9, gbc);
        final JPanel spacer10 = new JPanel();
        gbc = new GridBagConstraints();
        gbc.gridx = 8;
        gbc.gridy = 0;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        properties.add(spacer10, gbc);
        final JPanel spacer11 = new JPanel();
        gbc = new GridBagConstraints();
        gbc.gridx = 5;
        gbc.gridy = 2;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        properties.add(spacer11, gbc);
        showFileListButton = new JButton();
        showFileListButton.setText("Show File List");
        gbc = new GridBagConstraints();
        gbc.gridx = 9;
        gbc.gridy = 2;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        properties.add(showFileListButton, gbc);
        final JScrollPane scrollPane1 = new JScrollPane();
        gbc = new GridBagConstraints();
        gbc.gridx = 1;
        gbc.gridy = 5;
        gbc.gridwidth = 3;
        gbc.weightx = 1.0;
        gbc.weighty = 0.7;
        gbc.fill = GridBagConstraints.BOTH;
        mainPanel.add(scrollPane1, gbc);
        episodes = new JPanel();
        episodes.setLayout(new FlowLayout(FlowLayout.CENTER, 5, 5));
        scrollPane1.setViewportView(episodes);
        final JPanel spacer12 = new JPanel();
        gbc = new GridBagConstraints();
        gbc.gridx = 1;
        gbc.gridy = 4;
        gbc.gridwidth = 3;
        gbc.fill = GridBagConstraints.VERTICAL;
        mainPanel.add(spacer12, gbc);
        detailsField = new JTextField();
        detailsField.setEditable(false);
        gbc = new GridBagConstraints();
        gbc.gridx = 3;
        gbc.gridy = 3;
        gbc.weightx = 0.8;
        gbc.anchor = GridBagConstraints.WEST;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        mainPanel.add(detailsField, gbc);
        nameField = new JTextField();
        nameField.setEditable(false);
        gbc = new GridBagConstraints();
        gbc.gridx = 1;
        gbc.gridy = 3;
        gbc.weightx = 0.2;
        gbc.anchor = GridBagConstraints.WEST;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        mainPanel.add(nameField, gbc);
        final JPanel spacer13 = new JPanel();
        gbc = new GridBagConstraints();
        gbc.gridx = 2;
        gbc.gridy = 3;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        mainPanel.add(spacer13, gbc);
    }

    /**
     * @noinspection ALL
     */
    public JComponent $$$getRootComponent$$$() {
        return mainPanel;
    }

}
