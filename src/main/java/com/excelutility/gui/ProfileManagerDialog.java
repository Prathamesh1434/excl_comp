package com.excelutility.gui;

import com.excelutility.core.FilterProfile;
import com.excelutility.io.FilterProfileService;

import javax.swing.*;
import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Optional;

/**
 * A dialog for loading and deleting Filter Profiles.
 */
public class ProfileManagerDialog extends JDialog {

    private final FilterProfileService profileService;
    private JList<String> profileList;
    private DefaultListModel<String> listModel;
    private FilterProfile selectedProfile = null;

    public ProfileManagerDialog(Frame owner, FilterProfileService profileService) {
        super(owner, "Profile Manager", true);
        this.profileService = profileService;

        initComponents();
        loadProfiles();

        setSize(400, 300);
        setLocationRelativeTo(owner);
    }

    private void initComponents() {
        listModel = new DefaultListModel<>();
        profileList = new JList<>(listModel);
        profileList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        JScrollPane scrollPane = new JScrollPane(profileList);

        JButton loadButton = new JButton("Load");
        loadButton.addActionListener(e -> loadSelectedProfile());

        JButton deleteButton = new JButton("Delete");
        deleteButton.addActionListener(e -> deleteSelectedProfile());

        JButton cancelButton = new JButton("Cancel");
        cancelButton.addActionListener(e -> {
            selectedProfile = null;
            setVisible(false);
        });

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        buttonPanel.add(loadButton);
        buttonPanel.add(deleteButton);
        buttonPanel.add(cancelButton);

        setLayout(new BorderLayout(10, 10));
        add(scrollPane, BorderLayout.CENTER);
        add(buttonPanel, BorderLayout.SOUTH);
        ((JComponent) getContentPane()).setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
    }

    private void loadProfiles() {
        listModel.clear();
        List<File> profiles = profileService.getAvailableProfiles();
        for (File profileFile : profiles) {
            listModel.addElement(profileFile.getName());
        }
    }

    private void loadSelectedProfile() {
        String selectedValue = profileList.getSelectedValue();
        if (selectedValue == null) {
            JOptionPane.showMessageDialog(this, "Please select a profile to load.", "No Profile Selected", JOptionPane.WARNING_MESSAGE);
            return;
        }

        File profileFile = new File(profileService.getProfileDir().toFile(), selectedValue);
        try {
            this.selectedProfile = profileService.loadProfile(profileFile);
            setVisible(false);
        } catch (IOException e) {
            JOptionPane.showMessageDialog(this, "Error loading profile: " + e.getMessage(), "Load Error", JOptionPane.ERROR_MESSAGE);
            this.selectedProfile = null;
        }
    }

    private void deleteSelectedProfile() {
        String selectedValue = profileList.getSelectedValue();
        if (selectedValue == null) {
            JOptionPane.showMessageDialog(this, "Please select a profile to delete.", "No Profile Selected", JOptionPane.WARNING_MESSAGE);
            return;
        }

        int confirm = JOptionPane.showConfirmDialog(this, "Are you sure you want to delete the profile '" + selectedValue + "'?", "Confirm Deletion", JOptionPane.YES_NO_OPTION);
        if (confirm == JOptionPane.YES_OPTION) {
            File profileFile = new File(profileService.getProfileDir().toFile(), selectedValue);
            try {
                profileService.deleteProfile(profileFile);
                loadProfiles(); // Refresh the list
            } catch (IOException e) {
                JOptionPane.showMessageDialog(this, "Error deleting profile: " + e.getMessage(), "Delete Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    /**
     * Shows the dialog and returns the selected profile if one was loaded.
     * @return An Optional containing the loaded FilterProfile, or empty if canceled.
     */
    public Optional<FilterProfile> showDialog() {
        setVisible(true);
        return Optional.ofNullable(selectedProfile);
    }
}
