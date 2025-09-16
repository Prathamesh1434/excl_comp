package com.excelutility.gui;

import com.excelutility.io.FilterProfileService;
import javax.swing.*;
import java.awt.*;
import java.io.IOException;

public class ProfileManagerDialog extends JDialog {

    private final FilterProfileService profileService;
    private final JList<String> profileList;
    private final DefaultListModel<String> listModel;
    private String selectedProfileForLoad = null;

    public ProfileManagerDialog(Frame owner, FilterProfileService profileService) {
        super(owner, "Manage Filter Profiles", true);
        this.profileService = profileService;

        setSize(400, 300);
        setLocationRelativeTo(owner);
        setLayout(new BorderLayout(10, 10));

        listModel = new DefaultListModel<>();
        profileList = new JList<>(listModel);
        profileList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        refreshProfileList();

        add(new JScrollPane(profileList), BorderLayout.CENTER);

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        JButton loadButton = new JButton("Load");
        JButton renameButton = new JButton("Rename");
        JButton deleteButton = new JButton("Delete");
        JButton closeButton = new JButton("Close");

        buttonPanel.add(loadButton);
        buttonPanel.add(renameButton);
        buttonPanel.add(deleteButton);
        buttonPanel.add(closeButton);
        add(buttonPanel, BorderLayout.SOUTH);

        loadButton.addActionListener(e -> loadSelectedProfile());
        renameButton.addActionListener(e -> renameSelectedProfile());
        deleteButton.addActionListener(e -> deleteSelectedProfile());
        closeButton.addActionListener(e -> dispose());
    }

    private void refreshProfileList() {
        listModel.clear();
        profileService.getAvailableProfiles().forEach(listModel::addElement);
    }

    private void loadSelectedProfile() {
        String selected = profileList.getSelectedValue();
        if (selected == null) {
            JOptionPane.showMessageDialog(this, "Please select a profile to load.", "Selection Required", JOptionPane.WARNING_MESSAGE);
            return;
        }
        this.selectedProfileForLoad = selected;
        dispose();
    }

    private void renameSelectedProfile() {
        String selected = profileList.getSelectedValue();
        if (selected == null) {
            JOptionPane.showMessageDialog(this, "Please select a profile to rename.", "Selection Required", JOptionPane.WARNING_MESSAGE);
            return;
        }
        String newName = JOptionPane.showInputDialog(this, "Enter new name for profile '" + selected + "':", "Rename Profile", JOptionPane.PLAIN_MESSAGE);
        if (newName != null && !newName.trim().isEmpty()) {
            try {
                profileService.renameProfile(selected, newName.trim());
                refreshProfileList();
            } catch (IOException e) {
                JOptionPane.showMessageDialog(this, "Error renaming profile: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void deleteSelectedProfile() {
        String selected = profileList.getSelectedValue();
        if (selected == null) {
            JOptionPane.showMessageDialog(this, "Please select a profile to delete.", "Selection Required", JOptionPane.WARNING_MESSAGE);
            return;
        }
        int choice = JOptionPane.showConfirmDialog(this, "Are you sure you want to delete the profile '" + selected + "'?", "Confirm Deletion", JOptionPane.YES_NO_OPTION);
        if (choice == JOptionPane.YES_OPTION) {
            try {
                profileService.deleteProfile(selected);
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
