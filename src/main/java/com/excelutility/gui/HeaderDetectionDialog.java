package com.excelutility.gui;

import com.excelutility.core.ConcatenationMode;
import com.excelutility.core.HeaderDetector;
import net.miginfocom.swing.MigLayout;
import org.apache.poi.ss.usermodel.Sheet;
import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;

public class HeaderDetectionDialog extends JDialog {

    private final List<JCheckBox> rowCheckBoxes = new ArrayList<>();
    private final JComboBox<ConcatenationMode> modeCombo;
    private boolean confirmed = false;

    public HeaderDetectionDialog(Frame owner, Sheet sheet) {
        super(owner, "Header Row Selection", true);
        setSize(800, 600);
        setLocationRelativeTo(owner);
        setLayout(new BorderLayout(10, 10));
        ((JPanel)getContentPane()).setBorder(UIConstants.BORDER_EMPTY_10);


        // --- Detection Logic ---
        HeaderDetector detector = new HeaderDetector();
        HeaderDetector.HeaderDetectionResult result = detector.detectHeader(sheet);
        List<Integer> detectedRows = result.getDetectedHeaderRows();

        // --- Main Panel ---
        JPanel mainPanel = new JPanel(new MigLayout("wrap 1, fillx"));

        int rowsToScan = Math.min(20, sheet.getLastRowNum() + 1);
        for (int i = 0; i < rowsToScan; i++) {
            JPanel rowPanel = new JPanel(new MigLayout("insets 0", "[][]"));
            JCheckBox checkBox = new JCheckBox("Row " + (i + 1));
            checkBox.setFont(UIConstants.FONT_BODY);
            checkBox.setSelected(detectedRows.contains(i));
            rowCheckBoxes.add(checkBox);
            rowPanel.add(checkBox);

            final int currentRowIndex = i;
            result.getConfidenceScores().stream()
                .filter(r -> r.getRowIndex() == currentRowIndex)
                .findFirst()
                .ifPresent(r -> {
                    JLabel confidenceLabel = new JLabel(String.format("(Confidence: %.2f, Reason: %s)", r.getScore(), r.getReason()));
                    confidenceLabel.setFont(UIConstants.FONT_LABEL);
                    rowPanel.add(confidenceLabel, "gapleft 20");
                });

            mainPanel.add(rowPanel);
        }

        add(new JScrollPane(mainPanel), BorderLayout.CENTER);

        // --- Top Panel for Mode Selection ---
        JPanel topPanel = new JPanel(new MigLayout("insets 0"));
        JLabel modeLabel = new JLabel("Header Concatenation Mode:");
        modeLabel.setFont(UIConstants.FONT_LABEL.deriveFont(Font.BOLD));
        topPanel.add(modeLabel);
        modeCombo = new JComboBox<>(ConcatenationMode.values());
        modeCombo.setFont(UIConstants.FONT_LABEL);
        topPanel.add(modeCombo, "gapleft 10");
        add(topPanel, BorderLayout.NORTH);

        // --- Bottom Panel ---
        JPanel bottomPanel = new JPanel(new MigLayout("fillx, insets 5 0 0 0", "push[][]"));
        JButton okButton = new JButton("OK");
        okButton.setFont(UIConstants.FONT_BUTTON);
        JButton cancelButton = new JButton("Cancel");
        cancelButton.setFont(UIConstants.FONT_BUTTON);
        bottomPanel.add(okButton, "sg btn, w 100!");
        bottomPanel.add(cancelButton, "sg btn, w 100!");
        add(bottomPanel, BorderLayout.SOUTH);

        // --- Listeners ---
        okButton.addActionListener(e -> {
            confirmed = true;
            dispose();
        });
        cancelButton.addActionListener(e -> dispose());
    }

    public boolean isConfirmed() {
        return confirmed;
    }

    public List<Integer> getSelectedHeaderRowIndices() {
        List<Integer> selectedIndices = new ArrayList<>();
        for (int i = 0; i < rowCheckBoxes.size(); i++) {
            if (rowCheckBoxes.get(i).isSelected()) {
                selectedIndices.add(i);
            }
        }
        return selectedIndices;
    }

    public ConcatenationMode getConcatenationMode() {
        return (ConcatenationMode) modeCombo.getSelectedItem();
    }
}
