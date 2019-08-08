package com.sheryv.tools.movielinkgripper.ui;

import com.sheryv.util.Strings;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.*;
import java.util.function.Consumer;

public class UiUtils {
    public static <T> JComponent appendRow(JPanel panel, String label, T value, Class<T> type, Consumer<T> onChange) {
        JPanel row = new JPanel(new BorderLayout(10, 0));
        row.add(new JLabel(label), BorderLayout.WEST);
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
        panel.add(row);
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
