package com.zxzinn.novelai.component;

import com.zxzinn.novelai.utils.common.PropertiesManager;
import javafx.scene.control.CheckBox;
import javafx.scene.control.TitledPane;
import javafx.scene.layout.VBox;
import lombok.Getter;

public class Text2ImageSettingsPane extends TitledPane {

    private final PropertiesManager propertiesManager;
    @Getter private CheckBox smeaCheckBox;
    @Getter private CheckBox smeaDynCheckBox;

    public Text2ImageSettingsPane(PropertiesManager propertiesManager) {
        this.propertiesManager = propertiesManager;
        setText("Text2Image Settings");
        getStyleClass().add("settings-section");
        initializeComponents();
        loadSettings();
        setupListeners();
    }

    private void initializeComponents() {
        VBox content = new VBox(5);
        content.getStyleClass().add("settings-content");

        smeaCheckBox = new CheckBox("SMEA");
        smeaDynCheckBox = new CheckBox("SMEA DYN");

        content.getChildren().addAll(smeaCheckBox, smeaDynCheckBox);

        setContent(content);
    }

    private void loadSettings() {
        smeaCheckBox.setSelected(propertiesManager.getBoolean("smea", true));
        smeaDynCheckBox.setSelected(propertiesManager.getBoolean("smeaDyn", false));
    }

    private void setupListeners() {
        smeaCheckBox.selectedProperty().addListener((obs, oldVal, newVal) -> propertiesManager.setBoolean("smea", newVal));
        smeaDynCheckBox.selectedProperty().addListener((obs, oldVal, newVal) -> propertiesManager.setBoolean("smeaDyn", newVal));
    }

    public boolean isSmea() {
        return smeaCheckBox.isSelected();
    }

    public boolean isSmeaDyn() {
        return smeaDynCheckBox.isSelected();
    }
}