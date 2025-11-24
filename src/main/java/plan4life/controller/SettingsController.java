package plan4life.controller;

import plan4life.view.SettingsView;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

/**
 */
public class SettingsController {

    private SettingsView settingsView;

    public SettingsController(SettingsView view) {
        this.settingsView = view;
        addListeners();
    }

    private void addListeners() {

        // --- Cancel Button Logic ---
        settingsView.getCancelButton().addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                settingsView.dispose();
            }
        });

        settingsView.getSaveButton().addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {

                System.out.println("Settings 'Saved' (Demo)!");
                settingsView.dispose();
            }
        });
    }
}