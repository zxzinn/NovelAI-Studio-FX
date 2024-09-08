package com.zxzinn.novelai.component;

import com.zxzinn.novelai.utils.common.PropertiesManager;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.control.TitledPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import lombok.Getter;

public class OutputSettingsPane extends TitledPane {

    private final PropertiesManager propertiesManager;
    private TextField widthField;
    private TextField heightField;
    private TextField ratioField;
    private TextField countField;
    @Getter private TextField outputDirectoryField;

    public OutputSettingsPane() {
        this.propertiesManager = PropertiesManager.getInstance();
        setText("Output Settings");
        getStyleClass().add("settings-section");
        initializeComponents();
        loadSettings();
        setupListeners();
    }

    private void initializeComponents() {
        VBox content = new VBox(10);
        content.getStyleClass().add("settings-content");

        HBox resolutionBox = new HBox(10);
        resolutionBox.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
        widthField = new TextField();
        heightField = new TextField();
        widthField.getStyleClass().addAll("resolution-field", "right-aligned-text");
        heightField.getStyleClass().addAll("resolution-field", "right-aligned-text");
        resolutionBox.getChildren().addAll(
                new Label("解析度"),
                widthField,
                new Label("×"),
                heightField
        );

        ratioField = new TextField();
        ratioField.getStyleClass().add("right-aligned-text");
        HBox.setHgrow(ratioField, javafx.scene.layout.Priority.ALWAYS);

        countField = new TextField();
        countField.getStyleClass().add("right-aligned-text");
        HBox.setHgrow(countField, javafx.scene.layout.Priority.ALWAYS);

        outputDirectoryField = new TextField();
        outputDirectoryField.getStyleClass().add("right-aligned-text");
        HBox.setHgrow(outputDirectoryField, javafx.scene.layout.Priority.ALWAYS);

        content.getChildren().addAll(
                resolutionBox,
                createLabeledHBox("比例", ratioField),
                createLabeledHBox("生成數量", countField),
                createLabeledHBox("輸出目錄", outputDirectoryField)
        );

        setContent(content);
    }

    private HBox createLabeledHBox(String labelText, javafx.scene.control.Control control) {
        HBox hbox = new HBox(10);
        hbox.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
        Label label = new Label(labelText);
        label.getStyleClass().add("settings-label");
        label.setMinWidth(80);
        hbox.getChildren().addAll(label, control);
        return hbox;
    }

    private void loadSettings() {
        widthField.setText(String.valueOf(propertiesManager.getInt("width", 832)));
        heightField.setText(String.valueOf(propertiesManager.getInt("height", 1216)));
        ratioField.setText(String.valueOf(propertiesManager.getInt("ratio", 7)));
        countField.setText(String.valueOf(propertiesManager.getInt("count", 1)));
        outputDirectoryField.setText(propertiesManager.getString("outputDirectory", "output"));
    }

    private void setupListeners() {
        widthField.textProperty().addListener((obs, oldVal, newVal) -> propertiesManager.setInt("width", Integer.parseInt(newVal)));
        heightField.textProperty().addListener((obs, oldVal, newVal) -> propertiesManager.setInt("height", Integer.parseInt(newVal)));
        ratioField.textProperty().addListener((obs, oldVal, newVal) -> propertiesManager.setInt("ratio", Integer.parseInt(newVal)));
        countField.textProperty().addListener((obs, oldVal, newVal) -> propertiesManager.setInt("count", Integer.parseInt(newVal)));
        outputDirectoryField.textProperty().addListener((obs, oldVal, newVal) -> propertiesManager.setString("outputDirectory", newVal));
    }

    public int getOutputWidth() {
        return Integer.parseInt(widthField.getText());
    }

    public int getOutputHeight() {
        return Integer.parseInt(heightField.getText());
    }

    public int getRatio() {
        return Integer.parseInt(ratioField.getText());
    }

    public int getCount() {
        return Integer.parseInt(countField.getText());
    }

    public String getOutputDirectory() {
        return outputDirectoryField.getText();
    }
}