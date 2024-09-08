package com.zxzinn.novelai.component;

import com.zxzinn.novelai.utils.common.NAIConstants;
import com.zxzinn.novelai.utils.common.PropertiesManager;
import javafx.collections.FXCollections;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.control.TitledPane;
import javafx.scene.layout.VBox;
import lombok.Getter;

public class SamplingSettingsPane extends TitledPane {

    private final PropertiesManager propertiesManager;
    @Getter private ComboBox<String> samplerComboBox;
    private TextField stepsField;
    private TextField seedField;

    public SamplingSettingsPane() {
        this.propertiesManager = PropertiesManager.getInstance();
        setText("Sampling Settings");
        getStyleClass().add("settings-section");
        initializeComponents();
        loadSettings();
        setupListeners();
    }

    private void initializeComponents() {
        VBox content = new VBox(5);
        content.getStyleClass().add("settings-content");

        samplerComboBox = new ComboBox<>();
        samplerComboBox.setMaxWidth(Double.MAX_VALUE);
        samplerComboBox.getStyleClass().add("right-aligned-text");

        stepsField = new TextField();
        stepsField.getStyleClass().add("right-aligned-text");

        seedField = new TextField();
        seedField.getStyleClass().add("right-aligned-text");

        content.getChildren().addAll(
                new Label("採樣器"),
                samplerComboBox,
                new Label("步驟"),
                stepsField,
                new Label("種子"),
                seedField
        );

        setContent(content);
    }

    private void loadSettings() {
        samplerComboBox.setItems(FXCollections.observableArrayList(NAIConstants.SAMPLERS));
        samplerComboBox.setValue(propertiesManager.getString("sampler", "k_euler"));
        stepsField.setText(String.valueOf(propertiesManager.getInt("steps", 28)));
        seedField.setText(String.valueOf(propertiesManager.getInt("seed", 0)));
    }

    private void setupListeners() {
        samplerComboBox.valueProperty().addListener((obs, oldVal, newVal) -> propertiesManager.setString("sampler", newVal));
        stepsField.textProperty().addListener((obs, oldVal, newVal) -> propertiesManager.setInt("steps", Integer.parseInt(newVal)));
        seedField.textProperty().addListener((obs, oldVal, newVal) -> propertiesManager.setInt("seed", Integer.parseInt(newVal)));
    }

    public String getSampler() {
        return samplerComboBox.getValue();
    }

    public int getSteps() {
        return Integer.parseInt(stepsField.getText());
    }

    public long getSeed() {
        return Long.parseLong(seedField.getText());
    }
}