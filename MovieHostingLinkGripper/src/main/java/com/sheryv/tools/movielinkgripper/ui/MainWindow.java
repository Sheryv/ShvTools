package com.sheryv.tools.movielinkgripper.ui;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.sheryv.tools.movielinkgripper.Bootstrapper;
import com.sheryv.tools.movielinkgripper.EpisodesTypes;
import com.sheryv.tools.movielinkgripper.Transformer;
import com.sheryv.tools.movielinkgripper.config.AbstractMode;
import com.sheryv.tools.movielinkgripper.config.Configuration;
import com.sheryv.tools.movielinkgripper.config.HostingConfig;
import com.sheryv.tools.movielinkgripper.config.RunMode;
import com.sheryv.tools.movielinkgripper.provider.Hosting;
import com.sheryv.util.FileUtils;
import com.sheryv.util.SerialisationUtils;
import com.sheryv.util.VersionUtils;
import lombok.AllArgsConstructor;
import lombok.Data;

import javax.swing.*;
import java.awt.*;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.awt.dnd.DragSource;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Objects;
import java.util.concurrent.Executors;

public class MainWindow {
    private JPanel mainPanel;
    private JList hostingList;
    private JList typesList;
    private JList providersList;
    private JPanel properties;
    private JButton saveBtn;
    private JButton continueBtn;
    private JButton openSearchWindowButton;
    private JLabel statusBar;

    private boolean isWorking = false;

    public MainWindow() {
    }

    public static void main(String[] args) {
        Bootstrapper.main(new String[]{"i"});
    }

    private MainWindow init() {
        final Configuration config = Configuration.get();
        RunMode mode = config.findRunMode();

        DefaultListModel<HostingConfig> m = new DefaultListModel<>();

        for (HostingConfig h : config.getHostings()) {
            m.addElement(h);
        }

        hostingList.setModel(m);
        hostingList.getSelectionModel().setSelectionMode(
                ListSelectionModel.SINGLE_SELECTION);
        hostingList.setTransferHandler(new ListItemTransferHandler());
        hostingList.setDropMode(DropMode.INSERT);
        hostingList.setDragEnabled(true);
        // https://java-swing-tips.blogspot.com/2008/10/rubber-band-selection-drag-and-drop.html
//        hostingList.setLayoutOrientation(JList.HORIZONTAL_WRAP);
//        list.setVisibleRowCount(0);
//        list.setFixedCellWidth(80);
//        list.setFixedCellHeight(80);
//        hostingList.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        hostingList.setCellRenderer(new ListCellRenderer<HostingConfig>() {
            private final JCheckBox p = new JCheckBox();

            @Override
            public Component getListCellRendererComponent(
                    JList<? extends HostingConfig> list, HostingConfig value, int index,
                    boolean isSelected, boolean cellHasFocus) {
                p.setText(String.format("%2d > %s", value.getPriority(), value.getCode()));
                p.setSelected(value.isEnabled());
//                label.setForeground(isSelected ? list.getSelectionForeground()
//                        : list.getForeground());
//                p.add(icon);
//                p.add(label, BorderLayout.SOUTH);
                p.setBackground(list.getBackground());
                return p;
            }
        });
        MouseListener mouseListener = new MouseAdapter() {
            public void mouseClicked(MouseEvent mouseEvent) {
                if (mouseEvent.getButton() == MouseEvent.BUTTON1) {
                    int index = hostingList.locationToIndex(mouseEvent.getPoint());
                    if (index >= 0) {
                        HostingConfig config = (HostingConfig) hostingList.getModel().getElementAt(index);
                        config.setEnabled(!config.isEnabled());
                        System.out.println(config);
                        hostingList.updateUI();
                        //                        hostingList.setModel(hostingList.getModel());
                    }
                }
            }
        };
        hostingList.addMouseListener(mouseListener);

        DefaultListModel<EpisodeTypeConfig> typesModel = new DefaultListModel<>();
        for (EpisodesTypes types : EpisodesTypes.values()) {
            boolean enabled = config.getAllowedEpisodeTypes().contains(types);
            typesModel.addElement(new EpisodeTypeConfig(types, enabled));
        }
        typesList.setModel(typesModel);
        typesList.setCellRenderer(new ListCellRenderer<EpisodeTypeConfig>() {
            private final JCheckBox p = new JCheckBox();

            @Override
            public Component getListCellRendererComponent(
                    JList<? extends EpisodeTypeConfig> list, EpisodeTypeConfig value, int index,
                    boolean isSelected, boolean cellHasFocus) {
                p.setText(value.getType().toString());
                p.setSelected(value.isEnabled());
//                label.setForeground(isSelected ? list.getSelectionForeground()
//                        : list.getForeground());
//                p.add(icon);
//                p.add(label, BorderLayout.SOUTH);
                p.setBackground(list.getBackground());
                return p;
            }
        });
        typesList.addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent mouseEvent) {
                if (mouseEvent.getButton() == MouseEvent.BUTTON1) {
                    int index = typesList.locationToIndex(mouseEvent.getPoint());
                    if (index >= 0) {
                        EpisodeTypeConfig config = (EpisodeTypeConfig) typesList.getModel().getElementAt(index);
                        config.setEnabled(!config.isEnabled());
                        typesList.updateUI();
                        //                        hostingList.setModel(hostingList.getModel());
                    }
                }
            }
        });
        DefaultListModel<String> providersModel = new DefaultListModel<>();
        for (String pro : Transformer.PROVIDERS.keySet()) {
            providersModel.addElement(pro);
        }
        providersList.setModel(providersModel);
        providersList.addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent mouseEvent) {
                if (mouseEvent.getButton() == MouseEvent.BUTTON1 && mouseEvent.getClickCount() == 2) {
                    int index = providersList.locationToIndex(mouseEvent.getPoint());
                    if (index >= 0) {
                        String provider = (String) providersList.getModel().getElementAt(index);
                        isWorking = true;
                        providersList.setEnabled(false);
                        properties.setEnabled(false);
                        mode.setProviderName(provider);
                        Executors.newSingleThreadExecutor().submit(() -> {
                            try {
                                mode.execute(config);
                            } catch (Exception e) {
                                e.printStackTrace();
                            } finally {
                                EventQueue.invokeLater(() -> {
                                    isWorking = false;
                                    properties.setEnabled(true);
                                    providersList.setEnabled(true);
                                });
                            }
                        });
                        //                        hostingList.setModel(hostingList.getModel());
                    }
                }
            }
        });

        properties.setLayout(new BoxLayout(properties, BoxLayout.Y_AXIS));
        properties.setAlignmentY(0);
        UiUtils.appendRow(properties, "seriesName", mode.getSeriesName(), String.class, mode::setSeriesName);
        UiUtils.appendRow(properties, "seasonIndex", mode.getSeasonIndex(), Integer.class, mode::setSeasonIndex);
        UiUtils.appendRow(properties, "relativeUrlToSeries", mode.getRelativeUrlToSeries(), String.class, mode::setRelativeUrlToSeries);
        UiUtils.appendRow(properties, "Num Of First Hostings Used Together", config.getNumOfTopHostingsUsedSimultaneously(), Integer.class, config::setNumOfTopHostingsUsedSimultaneously);
        UiUtils.appendRow(properties, "startIndex", config.getSearchStartIndex(), Integer.class, config::setSearchStartIndex);
        UiUtils.appendRow(properties, "stopIndex", config.getSearchStopIndex(), Integer.class, config::setSearchStopIndex);
//        UiUtils.appendRow(properties, "startIndex", Configuration.get().getSearchStartIndex(), Integer.class, integer -> Configuration.get().setSearchStartIndex(integer));
//        UiUtils.appendRow(properties, "startIndex", Configuration.get().getSearchStartIndex(), Integer.class, integer -> Configuration.get().setSearchStartIndex(integer));
//        UiUtils.appendRow(properties, "startIndex", Configuration.get().getSearchStartIndex(), Integer.class, integer -> Configuration.get().setSearchStartIndex(integer));

        saveBtn.addActionListener(e -> {
            try {
                String s = SerialisationUtils.toYaml(Configuration.get());
                FileUtils.saveFile(s, Paths.get(Configuration.CONFIG_FILE));
            } catch (JsonProcessingException ex) {
                ex.printStackTrace();
            }
        });

        openSearchWindowButton.addActionListener(e -> openSearchEpisodesWindow());

        Configuration.setOnPausedChange(aBoolean -> continueBtn.setEnabled(aBoolean));
        continueBtn.addActionListener(e -> Configuration.setPaused(false));
        statusBar.setText(VersionUtils.loadVersionByModuleName("movie-link-gripper-version"));
        return this;
    }

    public JPanel getMainPanel() {
        return mainPanel;
    }

    public static void createAndShowGUI() {
        EventQueue.invokeLater(() -> {
            try {
                UIManager.setLookAndFeel(
                        UIManager.getSystemLookAndFeelClassName());
            } catch (ClassNotFoundException e) {
                e.printStackTrace();
            } catch (InstantiationException e) {
                e.printStackTrace();
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            } catch (UnsupportedLookAndFeelException e) {
                e.printStackTrace();
            }
            JFrame f = new JFrame();
            f.setTitle("Movie Hosting Link Gripper");
            f.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
            f.getContentPane().add(new MainWindow().init().getMainPanel());
            f.setSize(600, 450);
//            f.setLocationRelativeTo(null);
            f.setVisible(true);
        });

    }

    public void openSearchEpisodesWindow() {
        JFrame f = new JFrame();
        f.setTitle("Search and fill episodes links");
        f.getContentPane().add(new SearchWindow().init().getMainPanel());
        f.setSize(1200, 750);
        f.setVisible(true);
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
        final JScrollPane scrollPane1 = new JScrollPane();
        scrollPane1.setPreferredSize(new Dimension(700, 400));
        GridBagConstraints gbc;
        gbc = new GridBagConstraints();
        gbc.gridx = 1;
        gbc.gridy = 1;
        gbc.weightx = 1.0;
        gbc.weighty = 1.0;
        gbc.fill = GridBagConstraints.BOTH;
        mainPanel.add(scrollPane1, gbc);
        hostingList = new JList();
        hostingList.setSelectionMode(0);
        scrollPane1.setViewportView(hostingList);
        final JPanel spacer1 = new JPanel();
        gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 1;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        mainPanel.add(spacer1, gbc);
        final JPanel spacer2 = new JPanel();
        gbc = new GridBagConstraints();
        gbc.gridx = 1;
        gbc.gridy = 0;
        gbc.fill = GridBagConstraints.VERTICAL;
        mainPanel.add(spacer2, gbc);
        final JPanel spacer3 = new JPanel();
        gbc = new GridBagConstraints();
        gbc.gridx = 1;
        gbc.gridy = 2;
        gbc.fill = GridBagConstraints.VERTICAL;
        mainPanel.add(spacer3, gbc);
        final JPanel spacer4 = new JPanel();
        gbc = new GridBagConstraints();
        gbc.gridx = 2;
        gbc.gridy = 1;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        mainPanel.add(spacer4, gbc);
        final JScrollPane scrollPane2 = new JScrollPane();
        gbc = new GridBagConstraints();
        gbc.gridx = 3;
        gbc.gridy = 1;
        gbc.weightx = 0.7;
        gbc.weighty = 1.0;
        gbc.fill = GridBagConstraints.BOTH;
        mainPanel.add(scrollPane2, gbc);
        typesList = new JList();
        typesList.setSelectionMode(0);
        scrollPane2.setViewportView(typesList);
        final JPanel spacer5 = new JPanel();
        gbc = new GridBagConstraints();
        gbc.gridx = 4;
        gbc.gridy = 1;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        mainPanel.add(spacer5, gbc);
        final JScrollPane scrollPane3 = new JScrollPane();
        gbc = new GridBagConstraints();
        gbc.gridx = 5;
        gbc.gridy = 1;
        gbc.weightx = 0.6;
        gbc.weighty = 1.0;
        gbc.fill = GridBagConstraints.BOTH;
        mainPanel.add(scrollPane3, gbc);
        providersList = new JList();
        providersList.setSelectionMode(0);
        scrollPane3.setViewportView(providersList);
        final JPanel spacer6 = new JPanel();
        gbc = new GridBagConstraints();
        gbc.gridx = 6;
        gbc.gridy = 1;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        mainPanel.add(spacer6, gbc);
        final JScrollPane scrollPane4 = new JScrollPane();
        gbc = new GridBagConstraints();
        gbc.gridx = 1;
        gbc.gridy = 3;
        gbc.gridwidth = 5;
        gbc.weighty = 1.0;
        gbc.fill = GridBagConstraints.BOTH;
        mainPanel.add(scrollPane4, gbc);
        properties = new JPanel();
        properties.setLayout(new FlowLayout(FlowLayout.CENTER, 5, 5));
        scrollPane4.setViewportView(properties);
        final JPanel spacer7 = new JPanel();
        gbc = new GridBagConstraints();
        gbc.gridx = 1;
        gbc.gridy = 4;
        gbc.fill = GridBagConstraints.VERTICAL;
        mainPanel.add(spacer7, gbc);
        final JPanel panel1 = new JPanel();
        panel1.setLayout(new FlowLayout(FlowLayout.CENTER, 5, 5));
        gbc = new GridBagConstraints();
        gbc.gridx = 1;
        gbc.gridy = 5;
        gbc.gridwidth = 5;
        gbc.fill = GridBagConstraints.BOTH;
        mainPanel.add(panel1, gbc);
        saveBtn = new JButton();
        saveBtn.setText("Save");
        panel1.add(saveBtn);
        continueBtn = new JButton();
        continueBtn.setEnabled(false);
        continueBtn.setText("Continue");
        panel1.add(continueBtn);
        openSearchWindowButton = new JButton();
        openSearchWindowButton.setText("Open search window");
        panel1.add(openSearchWindowButton);
        final JPanel spacer8 = new JPanel();
        gbc = new GridBagConstraints();
        gbc.gridx = 1;
        gbc.gridy = 6;
        gbc.fill = GridBagConstraints.VERTICAL;
        mainPanel.add(spacer8, gbc);
        statusBar = new JLabel();
        statusBar.setText("Label");
        gbc = new GridBagConstraints();
        gbc.gridx = 1;
        gbc.gridy = 7;
        gbc.gridwidth = 5;
        gbc.anchor = GridBagConstraints.WEST;
        mainPanel.add(statusBar, gbc);
    }

    /**
     * @noinspection ALL
     */
    public JComponent $$$getRootComponent$$$() {
        return mainPanel;
    }


    private class ListItemTransferHandler extends TransferHandler {
        protected final DataFlavor localObjectFlavor;
        protected int[] indices;
        protected int addIndex = -1; // Location where items were added
        protected int addCount; // Number of items added.

        public ListItemTransferHandler() {
            super();
            // localObjectFlavor = new ActivationDataFlavor(
            //   Object[].class, DataFlavor.javaJVMLocalObjectMimeType, "Array of items");
            localObjectFlavor = new DataFlavor(Object[].class, "Array of items");
        }

        @Override
        protected Transferable createTransferable(JComponent c) {
            JList<?> source = (JList<?>) c;
            c.getRootPane().getGlassPane().setVisible(true);

            indices = source.getSelectedIndices();
            Object[] transferedObjects = source.getSelectedValuesList().toArray(new Object[0]);
            // return new DataHandler(transferedObjects, localObjectFlavor.getMimeType());
            return new Transferable() {
                @Override
                public DataFlavor[] getTransferDataFlavors() {
                    return new DataFlavor[]{localObjectFlavor};
                }

                @Override
                public boolean isDataFlavorSupported(DataFlavor flavor) {
                    return Objects.equals(localObjectFlavor, flavor);
                }

                @Override
                public Object getTransferData(DataFlavor flavor)
                        throws UnsupportedFlavorException, IOException {
                    if (isDataFlavorSupported(flavor)) {
                        return transferedObjects;
                    } else {
                        throw new UnsupportedFlavorException(flavor);
                    }
                }
            };
        }

        @Override
        public boolean canImport(TransferSupport info) {
            return info.isDrop() && info.isDataFlavorSupported(localObjectFlavor);
        }

        @Override
        public int getSourceActions(JComponent c) {
            Component glassPane = c.getRootPane().getGlassPane();
            glassPane.setCursor(DragSource.DefaultMoveDrop);
            return MOVE; // COPY_OR_MOVE;
        }

        @SuppressWarnings("unchecked")
        @Override
        public boolean importData(TransferSupport info) {
            DropLocation tdl = info.getDropLocation();
            if (!canImport(info) || !(tdl instanceof JList.DropLocation)) {
                return false;
            }

            JList.DropLocation dl = (JList.DropLocation) tdl;
            JList target = (JList) info.getComponent();
            DefaultListModel listModel = (DefaultListModel) target.getModel();
            int max = listModel.getSize();
            int index = dl.getIndex();
            index = index < 0 ? max : index; // If it is out of range, it is appended to the end
            index = Math.min(index, max);

            addIndex = index;

            try {
                Object[] values = (Object[]) info.getTransferable().getTransferData(localObjectFlavor);
                for (int i = 0; i < values.length; i++) {
                    int idx = index++;
                    listModel.add(idx, values[i]);
                    target.addSelectionInterval(idx, idx);
                }
                addCount = values.length;
                return true;
            } catch (UnsupportedFlavorException | IOException ex) {
                ex.printStackTrace();
            }

            return false;
        }

        @Override
        protected void exportDone(JComponent c, Transferable data, int action) {
            c.getRootPane().getGlassPane().setVisible(false);
            cleanup(c, action == MOVE);
        }

        private void cleanup(JComponent c, boolean remove) {
            if (remove && Objects.nonNull(indices)) {
                if (addCount > 0) {
                    // https://github.com/aterai/java-swing-tips/blob/master/DragSelectDropReordering/src/java/example/MainPanel.java
                    for (int i = 0; i < indices.length; i++) {
                        if (indices[i] >= addIndex) {
                            indices[i] += addCount;
                        }
                    }
                }
                JList source = (JList) c;
                DefaultListModel model = (DefaultListModel) source.getModel();
                for (int i = indices.length - 1; i >= 0; i--) {
                    model.remove(indices[i]);
                }
                for (int i = 0; i < model.getSize(); i++) {
                    HostingConfig h = (HostingConfig) model.get(i);
                    h.setPriority((model.getSize() - i) * 10);
                }
                source.setModel(model);
            }

            indices = null;
            addCount = 0;
            addIndex = -1;


        }
    }

    @Data
    @AllArgsConstructor
    private class EpisodeTypeConfig {
        private EpisodesTypes type;
        private boolean enabled;

        public EpisodeTypeConfig(EpisodesTypes type) {
            this.type = type;
        }
    }
}
