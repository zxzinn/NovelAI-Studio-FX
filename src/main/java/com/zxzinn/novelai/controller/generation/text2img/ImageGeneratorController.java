package com.zxzinn.novelai.controller.generation.text2img;

import com.zxzinn.novelai.api.GenerationPayload;
import com.zxzinn.novelai.api.ImageGenerationPayload;
import com.zxzinn.novelai.api.APIClient;
import com.zxzinn.novelai.controller.generation.AbstractGenerationController;
import com.zxzinn.novelai.service.filemanager.FilePreviewService;
import com.zxzinn.novelai.service.generation.ImageGenerationService;
import com.zxzinn.novelai.utils.common.SettingsManager;
import com.zxzinn.novelai.utils.embed.EmbedProcessor;
import com.zxzinn.novelai.utils.image.ImageUtils;
import javafx.fxml.FXML;
import javafx.scene.control.CheckBox;
import lombok.extern.log4j.Log4j2;

@Log4j2
public class ImageGeneratorController extends AbstractGenerationController {
    @FXML private CheckBox smeaCheckBox;
    @FXML private CheckBox smeaDynCheckBox;

    public ImageGeneratorController(APIClient apiClient, EmbedProcessor embedProcessor,
                                    SettingsManager settingsManager,
                                    ImageGenerationService imageGenerationService,
                                    ImageUtils imageUtils,
                                    FilePreviewService filePreviewService) {
        super(apiClient, embedProcessor, settingsManager, imageGenerationService, imageUtils, filePreviewService);
    }

    @FXML
    @Override
    public void initialize() {
        super.initialize();
        smeaCheckBox.setSelected(settingsManager.getBoolean("smea", true));
        smeaDynCheckBox.setSelected(settingsManager.getBoolean("smeaDyn", false));
        smeaCheckBox.selectedProperty().addListener((obs, oldVal, newVal) ->
                settingsManager.setBoolean("smea", newVal));
        smeaDynCheckBox.selectedProperty().addListener((obs, oldVal, newVal) ->
                settingsManager.setBoolean("smeaDyn", newVal));
    }

    @Override
    protected GenerationPayload createGenerationPayload(String processedPositivePrompt, String processedNegativePrompt) {
        ImageGenerationPayload payload = new ImageGenerationPayload();
        payload.setInput(processedPositivePrompt);
        payload.setModel(modelComboBox.getValue());
        payload.setAction("generate");

        GenerationPayload.GenerationParameters parameters = new GenerationPayload.GenerationParameters();
        parameters.setWidth(Integer.parseInt(widthField.getText()));
        parameters.setHeight(Integer.parseInt(heightField.getText()));
        parameters.setScale(Integer.parseInt(ratioField.getText()));
        parameters.setSampler(samplerComboBox.getValue());
        parameters.setSteps(Integer.parseInt(stepsField.getText()));
        parameters.setN_samples(Integer.parseInt(countField.getText()));
        parameters.setUcPreset(false);
        parameters.setQualityToggle(false);
        parameters.setSm(smeaCheckBox.isSelected());
        parameters.setSm_dyn(smeaDynCheckBox.isSelected());
        parameters.setSeed(Long.parseLong(seedField.getText()));
        parameters.setNegative_prompt(processedNegativePrompt);

        payload.setParameters(parameters);
        return payload;
    }
}