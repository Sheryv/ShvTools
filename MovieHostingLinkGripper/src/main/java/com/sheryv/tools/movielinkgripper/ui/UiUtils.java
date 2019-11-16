package com.sheryv.tools.movielinkgripper.ui;

import com.sheryv.util.Strings;
import lombok.extern.slf4j.Slf4j;

import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.*;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.io.IOException;
import java.util.Collections;
import java.util.function.Consumer;

@Slf4j
public class UiUtils {
    public static <T> JComponent appendRow(JPanel panel, String label, T value, Class<T> type, Consumer<T> onChange) {
        return appendRow(panel, label, value, type, onChange, Collections.emptyList());
    }

    public static <T> JComponent appendRow(JPanel panel, String label, T value, Class<T> type, Consumer<T> onChange, java.util.List<JComponent> additionalComponents) {
        JPanel row = new JPanel(new BorderLayout(10, 0));
        JLabel title = new JLabel(label);
        title.setMinimumSize(new Dimension(60, 10));
        title.setPreferredSize(new Dimension(160, 12));
        title.setToolTipText(label);
        row.add(title, BorderLayout.WEST);
        row.setMaximumSize(new Dimension(Short.MAX_VALUE, 26));
        row.setBorder(BorderFactory.createEmptyBorder(3, 5, 3, 5));
        JComponent box;
        if (type.equals(String.class) || type.equals(Integer.class) || type.equals(Long.class) || type.equals(Float.class) || type.equals(Double.class)) {
            JTextField jTextField = new JTextField(value != null ? String.valueOf(value) : "");
            jTextField.getDocument().addDocumentListener(new DocumentListener() {
                @Override
                public void insertUpdate(DocumentEvent e) {
                    triggerEvent(type, jTextField, onChange);
                }

                @Override
                public void removeUpdate(DocumentEvent e) {
                    triggerEvent(type, jTextField, onChange);
                }

                @Override
                public void changedUpdate(DocumentEvent e) {
                    triggerEvent(type, jTextField, onChange);
                }
            });
            box = jTextField;
        } else if (type.equals(Boolean.class)) {
            JCheckBox jCheckBox = new JCheckBox();
            boolean selected = false;
            if (value != null)
                selected = (boolean) value;
            jCheckBox.setSelected(selected);
            jCheckBox.addChangeListener(e -> {
                JCheckBox c = (JCheckBox) e.getSource();
                onChange.accept((T) ((Boolean) c.isSelected()));
            });
            box = jCheckBox;
        } else {
            throw new IllegalArgumentException("Unknown type");
        }

        row.add(box, BorderLayout.CENTER);
        if (box instanceof JTextField) {
            JPanel btns = new JPanel();
            btns.setLayout(new BoxLayout(btns, BoxLayout.X_AXIS));

            JButton clearBtn = new JButton("X");
            clearBtn.addActionListener(e -> {
                ((JTextField) box).setText("");
            });

            JButton pasteBtn = new JButton("P");
            pasteBtn.addActionListener(e -> {
                try {
                    String data = (String) Toolkit.getDefaultToolkit()
                            .getSystemClipboard().getData(DataFlavor.stringFlavor);
                    log.debug("Read from clipboard: {}", data);
                    ((JTextField) box).setText(data);
                } catch (UnsupportedFlavorException ex) {
                    ex.printStackTrace();
                } catch (IOException ex) {
                    ex.printStackTrace();
                }
            });
            btns.add(clearBtn);
            btns.add(pasteBtn);
            for (JComponent component : additionalComponents) {
                btns.add(component);
            }
            row.add(btns, BorderLayout.EAST);
        }
        panel.add(row);
        JPanel separator = new JPanel(true);
        separator.setBackground(new Color(210, 210, 210));
        separator.setPreferredSize(new Dimension(100, 1));
        separator.setMaximumSize(new Dimension(Integer.MAX_VALUE, 1));
        panel.add(separator);
        return row;
    }

    private static <T> void triggerEvent(Class<T> type, JTextField field, Consumer<T> onChange) {
        String text = field.getText();
        if (!Strings.isNullOrEmpty(text)) {
            try {
                T value = parseString(type, text);
                onChange.accept(value);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }


    private static <T> T parseString(Class<T> type, String value) {
        if (type.equals(Integer.class)) {
            return (T) ((Integer) Integer.parseInt(value));
        }
        if (type.equals(Long.class)) {
            return (T) ((Long) Long.parseLong(value));
        }
        if (type.equals(Float.class)) {
            return (T) ((Float) Float.parseFloat(value));
        }
        if (type.equals(Double.class)) {
            return (T) ((Double) Double.parseDouble(value));
        }
        return (T) value;
    }
}
