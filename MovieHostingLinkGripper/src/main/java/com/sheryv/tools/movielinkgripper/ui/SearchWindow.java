package com.sheryv.tools.movielinkgripper.ui;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.sheryv.tools.movielinkgripper.Episode;
import com.sheryv.tools.movielinkgripper.Gripper;
import com.sheryv.tools.movielinkgripper.Series;
import com.sheryv.tools.movielinkgripper.Transformer;
import com.sheryv.tools.movielinkgripper.config.Configuration;
import com.sheryv.tools.movielinkgripper.search.SearchItem;
import com.sheryv.tools.movielinkgripper.search.TmdbApi;
import com.sheryv.util.FileUtils;
import com.sheryv.util.SerialisationUtils;
import com.sheryv.util.ThrowableFunction;
import lombok.extern.slf4j.Slf4j;

import javax.annotation.Nullable;
import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
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
  private JProgressBar progressBar;
  private JButton findSizeBtn;
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
        setProgress(-1);
        searchBtn.setEnabled(false);
        Integer season = (Integer) seasonNumber.getValue();
        setDetails(i, season);
        fillList(null);
        
        this.doInBackground(i, item ->
                api.getTvEpisodes(i.getId(), season).getEpisodes()
                    .stream()
                    .map(ep -> {
                      Optional<Episode> found = lastSeries.getEpisodes().stream()
                          .filter(l -> l.getN() == ep.getEpisodeNumber() && season == lastSeries.getSeason())
                          .findAny();
                      if (found.isPresent()) {
                        Episode f = found.get();
                        return new Episode(f.getPage(), ep.getName(), (int) ep.getEpisodeNumber(), f.getDlLink(), 0, f.getType(), f.getFormat(), f.getLastSize());
                      }
                      return new Episode("", ep.getName(), (int) ep.getEpisodeNumber(), "");
                    })
                    .collect(Collectors.toList()),
            episodeList -> {
              if (!episodeList.isEmpty()) {
                Series series = new Series(i.getName(), season, lastSeries.getLang(), lastSeries.getProviderUrl(), episodeList);
                fillList(series);
              }
            }, o -> {
              searchBtn.setEnabled(true);
              setProgress(100);
            });
      });
    });
    
    loadFromFileBtn.addActionListener(e -> {
      loadFromFile();
    });
    
    saveToFileBtn.addActionListener(e -> {
      if (lastSeries != null) {
        setProgress(-1);
        doInBackground(
            null,
            o -> FileUtils.saveFile(SerialisationUtils.toJsonPretty(lastSeries), Paths.get(configuration.getDefaultFilePathWithEpisodesList())),
            null,
            o -> setProgress(100)
        );
      }
    });
    showFileListButton.addActionListener(e -> showFileNamesList(lastSeries));
    findSizeBtn.addActionListener(e -> {
      if (lastSeries == null)
        return;
      setProgress(-1);
      findSizeBtn.setEnabled(false);
      float part = 100f / lastSeries.getEpisodes().size();
      doInBackground(
          lastSeries,
          ep -> {
            float prog = 0;
            for (Episode episode : lastSeries.getEpisodes()) {
              Long size = getDownloadSize(episode.getDlLink());
              episode.setLastSize(size);
              prog += part;
              setProgress((int) prog);
              EventQueue.invokeLater(() -> fillList(lastSeries));
            }
            return null;
          },
          null,
          o -> {
            findSizeBtn.setEnabled(true);
            setProgress(100);
            fillList(lastSeries);
          }
      );
    });
    
    episodes.setLayout(new BoxLayout(episodes, BoxLayout.Y_AXIS));
    episodes.setAlignmentY(0);
    loadFromFile();
    return this;
  }
  
  private void loadFromFile() {
    setProgress(-1);
    doInBackground(
        null,
        o -> Transformer.loadSeries(FileUtils.readFileInMemory(configuration.getDefaultFilePathWithEpisodesList())),
        series -> {
          seasonNumber.setValue(series.getSeason());
          searchText.setText(series.getName());
          fillList(series);
        },
        o -> setProgress(100)
    );
  }
  
  private void fillList(Series season) {
    episodes.removeAll();
    if (season == null)
      return;
    
    lastSeries = season;
    List<Episode> seasonEpisodes = season.getEpisodes();
    for (int i = 0; i < seasonEpisodes.size(); i++) {
      Episode episode = seasonEpisodes.get(i);
      JButton toIdmBtn = new JButton("IDM");
      toIdmBtn.setMargin(new Insets(2, 2, 1, 2));
      toIdmBtn.addActionListener(e -> {
        Gripper.addToIDM(season, episode, configuration);
      });
      JButton loadSizeBtn = new JButton("Size");
      loadSizeBtn.setMargin(new Insets(2, 2, 1, 2));
      loadSizeBtn.addActionListener(e -> {
        loadSize(episode);
      });
      String sizeString = episode.getLastSize() == 0 ? "" : FileUtils.sizeString(episode.getLastSize());
      JComponent row = UiUtils.appendRow(episodes, String.format("%02d [%s] %s", episode.getN(), sizeString, episode.getName()), episode.getDlLink(),
          String.class, episode::setDlLink, Arrays.asList(toIdmBtn, loadSizeBtn));
      if (i % 2 != 0) {
        row.setBackground(new Color(223, 223, 223));
      }
           /* row.addMouseListener(new MouseAdapter() {
                private Border grayBorder = BorderFactory.createLineBorder(Color.DARK_GRAY, 2);
                private Border prev;

                public void mouseEntered(MouseEvent e) {
                    JPanel parent = (JPanel) e.getSource();
                    prev = parent.getBorder();
                    parent.setBorder(grayBorder);
                    parent.revalidate();
                }

                public void mouseExited(MouseEvent e) {

                    Component c = SwingUtilities.getDeepestComponentAt(
                            e.getComponent(), e.getX(), e.getY());
                    // doesn't work if you move your mouse into the combobox popup
                    if (c == null) {
                        JPanel parent = (JPanel) e.getSource();
                        parent.setBorder(prev);
                        parent.revalidate();
                    }
                }
            });*/
    }
    episodes.updateUI();
  }
  
  private void setDetails(SearchItem i, int season) {
    nameField.setText(i.getName());
    detailsField.setText(String.format("%s [%d, %s, S%d] %s | %.2f", i.getOriginalName(),
        i.getId(), i.getOriginalLanguage(), season, i.getFirstAirDate(), i.getPopularity()));
    
  }
  
  private void showFileNamesList(Series series) {
    JPanel topPanel = new JPanel();
    topPanel.setLayout(new BoxLayout(topPanel, BoxLayout.X_AXIS));
    
    JTextArea textarea = new JTextArea();
    textarea.setEditable(false);
    Font font = textarea.getFont();
    textarea.setFont(font.deriveFont(font.getSize() + 5f));
    
    JTextField field = new JTextField();
    field.getDocument().addDocumentListener(new DocumentListener() {
      public void changedUpdate(DocumentEvent e) {
      }
      
      public void removeUpdate(DocumentEvent e) {
      }
      
      public void insertUpdate(DocumentEvent e) {
        String collect = series.getEpisodes().stream().map(ep -> ep.generateFileName(series, field.getText())).collect(Collectors.joining("\n"));
        textarea.setText(collect);
      }
    });
    field.setText("mp4");
    JPanel panel = new JPanel();
    
    JButton createFilesBtn = new JButton("Create empty templates files");
    createFilesBtn.addActionListener(e -> {
      JFileChooser fileChooser = new JFileChooser(Configuration.get().getDownloadDir());
      fileChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
      int option = fileChooser.showOpenDialog(panel);
      if (option == JFileChooser.APPROVE_OPTION) {
        File file = fileChooser.getSelectedFile();
        series.getEpisodes().stream().map(ep -> ep.generateFileName(series, field.getText())).forEach(f ->
            {
              try {
                Path dir = Paths.get(file.getAbsolutePath(), Gripper.createSeriesDirectoryName(series));
                Files.createDirectories(dir);
                Files.createFile(Paths.get(file.getAbsolutePath(), Gripper.createSeriesDirectoryName(series), f));
                log.info("Files generated in " + dir.toAbsolutePath());
              } catch (IOException ex) {
                ex.printStackTrace();
              }
            }
        );
      }
    });
    topPanel.add(new JLabel("Extension:"));
    topPanel.add(Box.createRigidArea(new Dimension(5, 5)));
    topPanel.add(field);
    topPanel.add(Box.createRigidArea(new Dimension(5, 5)));
    topPanel.add(createFilesBtn);
    
    panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
    panel.add(topPanel);
    panel.add(Box.createRigidArea(new Dimension(5, 5)));
    panel.add(textarea);
    
    JOptionPane.showMessageDialog(null, panel, "Episodes files list", JOptionPane.PLAIN_MESSAGE);
  }
  
  private void loadSize(Episode episode) {
    if (episode.getDlLink() == null)
      return;
    setProgress(-1);
    doInBackground(
        episode,
        ep -> getDownloadSize(ep.getDlLink()),
        lastSize -> {
          episode.setLastSize(lastSize);
          fillList(lastSeries);
        },
        o -> setProgress(100)
    );
  }
  
  private Long getDownloadSize(String urlString) throws IOException {
    URL url = new URL(urlString);
    HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();
    urlConnection.connect();
    long contentLengthLong = urlConnection.getContentLengthLong();
    urlConnection.disconnect();
    return contentLengthLong;
  }
  
  private void setProgress(int value) {
    EventQueue.invokeLater(() -> {
      if (value < 0) {
        progressBar.setIndeterminate(true);
      } else {
        if (progressBar.isIndeterminate())
          progressBar.setIndeterminate(false);
        progressBar.setValue(value);
      }
    });
  }
  
  private <T, R> void doInBackground(@Nullable T in, ThrowableFunction<T, R> work, @Nullable Consumer<R> success, @Nullable Consumer<R> finished) {
    Executors.newSingleThreadExecutor().submit(() -> {
      R result = null;
      try {
        result = work.apply(in);
        if (success != null) {
          final R r = result;
          EventQueue.invokeLater(() -> success.accept(r));
        }
      } catch (Exception e) {
        log.error("In Background task", e);
      }
      if (finished != null) {
        final R r = result;
        EventQueue.invokeLater(() -> finished.accept(r));
      }
    });
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
    gbc.gridwidth = 7;
    gbc.weightx = 1.0;
    gbc.anchor = GridBagConstraints.WEST;
    gbc.fill = GridBagConstraints.HORIZONTAL;
    properties.add(searchText, gbc);
    loadFromFileBtn = new JButton();
    loadFromFileBtn.setText("Load From File");
    gbc = new GridBagConstraints();
    gbc.gridx = 6;
    gbc.gridy = 2;
    gbc.fill = GridBagConstraints.HORIZONTAL;
    properties.add(loadFromFileBtn, gbc);
    searchBtn = new JButton();
    searchBtn.setText("Search");
    gbc = new GridBagConstraints();
    gbc.gridx = 10;
    gbc.gridy = 0;
    gbc.fill = GridBagConstraints.HORIZONTAL;
    properties.add(searchBtn, gbc);
    saveToFileBtn = new JButton();
    saveToFileBtn.setText("Save to File");
    gbc = new GridBagConstraints();
    gbc.gridx = 8;
    gbc.gridy = 2;
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
    gbc.gridx = 9;
    gbc.gridy = 0;
    gbc.fill = GridBagConstraints.HORIZONTAL;
    properties.add(spacer10, gbc);
    showFileListButton = new JButton();
    showFileListButton.setText("Show File List");
    gbc = new GridBagConstraints();
    gbc.gridx = 10;
    gbc.gridy = 2;
    gbc.fill = GridBagConstraints.HORIZONTAL;
    properties.add(showFileListButton, gbc);
    final JPanel spacer11 = new JPanel();
    gbc = new GridBagConstraints();
    gbc.gridx = 7;
    gbc.gridy = 2;
    gbc.fill = GridBagConstraints.HORIZONTAL;
    properties.add(spacer11, gbc);
    findSizeBtn = new JButton();
    findSizeBtn.setText("Find file size");
    gbc = new GridBagConstraints();
    gbc.gridx = 4;
    gbc.gridy = 2;
    gbc.fill = GridBagConstraints.HORIZONTAL;
    properties.add(findSizeBtn, gbc);
    final JPanel spacer12 = new JPanel();
    gbc = new GridBagConstraints();
    gbc.gridx = 5;
    gbc.gridy = 2;
    gbc.fill = GridBagConstraints.HORIZONTAL;
    properties.add(spacer12, gbc);
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
    final JPanel spacer13 = new JPanel();
    gbc = new GridBagConstraints();
    gbc.gridx = 1;
    gbc.gridy = 4;
    gbc.gridwidth = 3;
    gbc.fill = GridBagConstraints.VERTICAL;
    mainPanel.add(spacer13, gbc);
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
    final JPanel spacer14 = new JPanel();
    gbc = new GridBagConstraints();
    gbc.gridx = 2;
    gbc.gridy = 3;
    gbc.fill = GridBagConstraints.HORIZONTAL;
    mainPanel.add(spacer14, gbc);
    progressBar = new JProgressBar();
    progressBar.setMinimumSize(new Dimension(10, 15));
    progressBar.setPreferredSize(new Dimension(146, 15));
    gbc = new GridBagConstraints();
    gbc.gridx = 3;
    gbc.gridy = 7;
    gbc.fill = GridBagConstraints.HORIZONTAL;
    mainPanel.add(progressBar, gbc);
  }
  
  /**
   * @noinspection ALL
   */
  public JComponent $$$getRootComponent$$$() {
    return mainPanel;
  }
  
  private void createUIComponents() {
    // TODO: place custom component creation code here
  }
}
