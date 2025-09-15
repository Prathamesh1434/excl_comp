package com.excelutility.gui;

import com.excelutility.io.FilterProfileService;
import net.miginfocom.swing.MigLayout;

import javax.swing.*;
import java.awt.*;
import java.io.IOException;
import java.util.List;

public class FilterProfileManagerDialog extends JDialog {

    private final FilterProfileService profileService;
    private final DefaultListModel<String> listModel;
    private final JList<String> profileList;
    private String selectedProfileForLoad = null;

    public FilterProfileManagerDialog(Frame owner, FilterProfileService profileService) {
        super(owner, "Filter Profile Manager", true);
        this.profileService = profileService;
        this.listModel = new DefaultListModel<>();

        setLayout(new BorderLayout(10, 10));
        setSize(400, 500);
        setLocationRelativeTo(owner);

        // --- List of Profiles ---
        profileList = new JList<>(listModel);
        profileList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        JScrollPane scrollPane = new JScrollPane(profileList);
        scrollPane.setBorder(BorderFactory.createTitledBorder("Available Filter Profiles"));
        add(scrollPane, BorderLayout.CENTER);

        // --- Action Buttons ---
        JPanel buttonPanel = new JPanel(new MigLayout("fillx", "[grow][grow][grow]"));
        JButton loadButton = new JButton("Load");
        JButton deleteButton = new JButton("Delete");
        JButton closeButton = new JButton("Close");

        buttonPanel.add(loadButton, "growx");
        buttonPanel.add(deleteButton, "growx");
        buttonPanel.add(closeButton, "growx");
        add(buttonPanel, BorderLayout.SOUTH);

        // --- Action Listeners ---
        loadButton.addActionListener(e -> loadSelectedProfile());
        deleteButton.addActionListener(e -> deleteSelectedProfile());
        closeButton.addActionListener(e -> dispose());

        // Double-click to load
        profileList.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                if (evt.getClickCount() == 2) {
                    loadSelectedProfile();
                }
            }
        });

        refreshProfileList();
    }

    private void refreshProfileList() {
        listModel.clear();
        List<String> profiles = profileService.getAvailableProfiles();
        for (String profile : profiles) {
            listModel.addElement(profile);
        }
    }

    private void loadSelectedProfile() {
        selectedProfileForLoad = profileList.getSelectedValue();
        if (selectedProfileForLoad == null) {
            JOptionPane.showMessageDialog(this, "Please select a profile to load.", "No Profile Selected", JOptionPane.WARNING_MESSAGE);
            return;
        }
        dispose();
    }

    private void deleteSelectedProfile() {
        String selectedProfile = profileList.getSelectedValue();
        if (selectedProfile == null) {
            JOptionPane.showMessageDialog(this, "Please select a profile to delete.", "No Profile Selected", JOptionPane.WARNING_MESSAGE);
            return;
        }

        int confirm = JOptionPane.showConfirmDialog(this,
                "Are you sure you want to delete the profile '" + selectedProfile + "'?",
                "Confirm Deletion",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.WARNING_MESSAGE);

        if (confirm == JOptionPane.YES_OPTION) {
            try {
                profileService.deleteProfile(selectedProfile);
                JOptionPane.showMessageDialog(this, "Profile deleted successfully.", "Success", JOptionPane.INFORMATION_MESSAGE);
                refreshProfileList();
            } catch (IOException e) {
                JOptionPane.showMessageDialog(this, "Error deleting profile: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    public String getSelectedProfileForLoad() {
        return selectedProfileForLoad;
    }
}
