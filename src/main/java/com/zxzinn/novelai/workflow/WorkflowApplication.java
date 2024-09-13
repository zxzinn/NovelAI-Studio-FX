package com.zxzinn.novelai.workflow;

import com.zxzinn.nodefx.NodeFXApplication;
import com.zxzinn.novelai.api.APIClient;
import com.zxzinn.novelai.api.Endpoint;
import com.zxzinn.novelai.api.GenerationPayload;
import com.zxzinn.novelai.model.UIComponentsData;
import com.zxzinn.novelai.utils.common.NAIConstants;
import com.zxzinn.novelai.utils.common.PropertiesManager;
import com.zxzinn.novelai.utils.image.ImageUtils;
import javafx.application.Platform;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.VBox;
import lombok.extern.log4j.Log4j2;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.concurrent.CompletableFuture;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

@Log4j2
public class WorkflowApplication extends NodeFXApplication {

    private PropertiesManager propertiesManager;
    private APIClient apiClient;
    private GenerationNode generationNode;

    @Override
    protected void initializeComponents() {
        propertiesManager = PropertiesManager.getInstance();
        apiClient = new APIClient(Endpoint.GENERATE_IMAGE);
    }

    @Override
    protected void createNodes() {
        nodes.add(new ApiSettingsNode("API設置"));
        nodes.add(new PromptNode("正面提示詞"));
        nodes.add(new PromptNode("負面提示詞"));
        nodes.add(new SamplingSettingsNode("採樣設置"));
        nodes.add(new OutputSettingsNode("輸出設置"));
        nodes.add(new Text2ImageSettingsNode("文生圖設置"));
        generationNode = new GenerationNode("生成");
        nodes.add(generationNode);

        workflowPane.getChildren().addAll(nodes);
        positionNodes();
    }

    private void positionNodes() {
        double startX = 50;
        double startY = 50;
        double spacing = 220;

        for (int i = 0; i < nodes.size(); i++) {
            WorkflowNode node = nodes.get(i);
            node.setLayoutX(startX + (i % 3) * spacing);
            node.setLayoutY(startY + (i / 3) * spacing);
        }

        generationNode.setLayoutX(startX + spacing);
        generationNode.setLayoutY(startY + 2 * spacing);
    }

    @Override
    protected void createPresetConnections() {
        for (WorkflowNode node : nodes) {
            if (node != generationNode) {
                connectNodes(node.title, "生成");
            }
        }
    }

    private class ApiSettingsNode extends WorkflowNode {
        private final TextField apiKeyField;
        private final ComboBox<String> modelComboBox;

        public ApiSettingsNode(String title) {
            super(title);
            addOutputPort("API設置");

            apiKeyField = new TextField();
            apiKeyField.setPromptText("API Key");

            modelComboBox = new ComboBox<>();
            modelComboBox.getItems().addAll(NAIConstants.MODELS);
            modelComboBox.setValue(propertiesManager.getString("model", "nai-diffusion-3"));

            content.getChildren().addAll(new Label("API Key"), apiKeyField,
                    new Label("模型"), modelComboBox);

            setupListeners();
        }

        private void setupListeners() {
            apiKeyField.textProperty().addListener((obs, oldVal, newVal) ->
                    propertiesManager.setString("apiKey", newVal));
            modelComboBox.valueProperty().addListener((obs, oldVal, newVal) ->
                    propertiesManager.setString("model", newVal));
        }

        public String getApiKey() {
            return apiKeyField.getText();
        }

        public String getModel() {
            return modelComboBox.getValue();
        }
    }

    private class PromptNode extends WorkflowNode {
        private final TextArea promptArea;

        public PromptNode(String title) {
            super(title);
            addOutputPort("提示詞");

            promptArea = new TextArea();
            promptArea.setPrefRowCount(5);
            VBox.setMargin(promptArea, new javafx.geometry.Insets(5, 0, 0, 0));

            content.getChildren().add(promptArea);

            setupListeners();
        }

        private void setupListeners() {
            promptArea.textProperty().addListener((obs, oldVal, newVal) ->
                    propertiesManager.setString(title.equals("正面提示詞") ? "positivePrompt" : "negativePrompt", newVal));
        }

        public String getPromptText() {
            return promptArea.getText();
        }
    }

    private class SamplingSettingsNode extends WorkflowNode {
        private final ComboBox<String> samplerComboBox;
        private final Slider stepsSlider;
        private final TextField seedField;

        public SamplingSettingsNode(String title) {
            super(title);
            addOutputPort("採樣設置");

            samplerComboBox = new ComboBox<>();
            samplerComboBox.getItems().addAll(NAIConstants.SAMPLERS);
            samplerComboBox.setValue(propertiesManager.getString("sampler", "k_euler"));

            stepsSlider = new Slider(1, 150, 28);
            Label stepsLabel = new Label("步數: 28");

            seedField = new TextField();
            seedField.setPromptText("種子");

            content.getChildren().addAll(
                    new Label("採樣器"), samplerComboBox,
                    new Label("步數"), stepsSlider, stepsLabel,
                    new Label("種子"), seedField
            );

            setupListeners();
        }

        private void setupListeners() {
            samplerComboBox.valueProperty().addListener((obs, oldVal, newVal) ->
                    propertiesManager.setString("sampler", newVal));
            stepsSlider.valueProperty().addListener((obs, oldVal, newVal) -> {
                int steps = newVal.intValue();
                propertiesManager.setInt("steps", steps);
                ((Label)content.getChildren().get(4)).setText("步數: " + steps);
            });
            seedField.textProperty().addListener((obs, oldVal, newVal) ->
                    propertiesManager.setInt("seed", Integer.parseInt(newVal)));
        }

        public String getSampler() {
            return samplerComboBox.getValue();
        }

        public int getSteps() {
            return (int) stepsSlider.getValue();
        }

        public long getSeed() {
            return Long.parseLong(seedField.getText());
        }
    }

    private class OutputSettingsNode extends WorkflowNode {
        private final TextField widthField;
        private final TextField heightField;
        private final TextField ratioField;
        private final TextField countField;
        private final TextField outputDirectoryField;

        public OutputSettingsNode(String title) {
            super(title);
            addOutputPort("輸出設置");

            widthField = new TextField(String.valueOf(propertiesManager.getInt("width", 832)));
            heightField = new TextField(String.valueOf(propertiesManager.getInt("height", 1216)));
            ratioField = new TextField(String.valueOf(propertiesManager.getInt("ratio", 7)));
            countField = new TextField(String.valueOf(propertiesManager.getInt("count", 1)));
            outputDirectoryField = new TextField(propertiesManager.getString("outputDirectory", "output"));

            content.getChildren().addAll(
                    new Label("寬度"), widthField,
                    new Label("高度"), heightField,
                    new Label("比例"), ratioField,
                    new Label("生成數量"), countField,
                    new Label("輸出目錄"), outputDirectoryField
            );

            setupListeners();
        }

        private void setupListeners() {
            widthField.textProperty().addListener((obs, oldVal, newVal) ->
                    propertiesManager.setInt("width", Integer.parseInt(newVal)));
            heightField.textProperty().addListener((obs, oldVal, newVal) ->
                    propertiesManager.setInt("height", Integer.parseInt(newVal)));
            ratioField.textProperty().addListener((obs, oldVal, newVal) ->
                    propertiesManager.setInt("ratio", Integer.parseInt(newVal)));
            countField.textProperty().addListener((obs, oldVal, newVal) ->
                    propertiesManager.setInt("count", Integer.parseInt(newVal)));
            outputDirectoryField.textProperty().addListener((obs, oldVal, newVal) ->
                    propertiesManager.setString("outputDirectory", newVal));
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

    private class Text2ImageSettingsNode extends WorkflowNode {
        private final CheckBox smeaCheckBox;
        private final CheckBox smeaDynCheckBox;

        public Text2ImageSettingsNode(String title) {
            super(title);
            addOutputPort("文生圖設置");

            smeaCheckBox = new CheckBox("SMEA");
            smeaCheckBox.setSelected(propertiesManager.getBoolean("smea", true));

            smeaDynCheckBox = new CheckBox("SMEA DYN");
            smeaDynCheckBox.setSelected(propertiesManager.getBoolean("smeaDyn", false));

            content.getChildren().addAll(smeaCheckBox, smeaDynCheckBox);

            setupListeners();
        }

        private void setupListeners() {
            smeaCheckBox.selectedProperty().addListener((obs, oldVal, newVal) ->
                    propertiesManager.setBoolean("smea", newVal));
            smeaDynCheckBox.selectedProperty().addListener((obs, oldVal, newVal) ->
                    propertiesManager.setBoolean("smeaDyn", newVal));
        }

        public boolean isSmea() {
            return smeaCheckBox.isSelected();
        }

        public boolean isSmeaDyn() {
            return smeaDynCheckBox.isSelected();
        }
    }

    private class GenerationNode extends WorkflowNode {
        private final ImageView previewImageView;
        private final ScrollPane imageScrollPane;

        public GenerationNode(String title) {
            super(title);
            addInputPort("API設置");
            addInputPort("正面提示詞");
            addInputPort("負面提示詞");
            addInputPort("採樣設置");
            addInputPort("輸出設置");
            addInputPort("文生圖設置");
            addOutputPort("生成結果");

            Button generateButton = new Button("生成");
            generateButton.setPrefSize(100, 30);
            generateButton.setOnAction(event -> generateImage());

            previewImageView = new ImageView();
            previewImageView.setPreserveRatio(true);

            imageScrollPane = new ScrollPane(previewImageView);
            imageScrollPane.setPrefSize(180, 100);
            imageScrollPane.setFitToWidth(true);
            imageScrollPane.setFitToHeight(true);

            content.getChildren().addAll(generateButton, imageScrollPane);
        }

        private void generateImage() {
            UIComponentsData uiData = collectUIData();
            CompletableFuture.runAsync(() -> {
                try {
                    GenerationPayload payload = createGenerationPayload(uiData);
                    byte[] zipData = apiClient.generateImage(payload, uiData.apiKey);
                    byte[] imageData = extractImageFromZip(zipData);
                    handleGeneratedImage(imageData);
                } catch (Exception e) {
                    log.error("生成圖像時發生錯誤", e);
                    Platform.runLater(() -> showAlert("錯誤", "生成圖像時發生錯誤: " + e.getMessage()));
                }
            });
        }

        private byte[] extractImageFromZip(byte[] zipData) throws IOException {
            try (ZipInputStream zis = new ZipInputStream(new ByteArrayInputStream(zipData))) {
                ZipEntry entry = zis.getNextEntry();
                if (entry == null) {
                    throw new IOException("ZIP file is empty");
                }
                ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
                byte[] buffer = new byte[4096];
                int len;
                while ((len = zis.read(buffer)) > 0) {
                    outputStream.write(buffer, 0, len);
                }
                return outputStream.toByteArray();
            }
        }

        private void handleGeneratedImage(byte[] imageData) {
            Platform.runLater(() -> {
                Image image = new Image(new ByteArrayInputStream(imageData));
                previewImageView.setImage(image);

                double imageWidth = image.getWidth();
                double imageHeight = image.getHeight();
                double maxWidth = 400;
                double maxHeight = 300;

                if (imageWidth > maxWidth || imageHeight > maxHeight) {
                    double widthRatio = maxWidth / imageWidth;
                    double heightRatio = maxHeight / imageHeight;
                    double scale = Math.min(widthRatio, heightRatio);
                    imageWidth *= scale;
                    imageHeight *= scale;
                }

                previewImageView.setFitWidth(imageWidth);
                previewImageView.setFitHeight(imageHeight);

                imageScrollPane.setPrefSize(imageWidth + 20, imageHeight + 20);
                setPrefSize(Math.max(200, imageWidth + 40), Math.max(200, imageHeight + 120));

                saveImageToFile(imageData);
            });
        }

        private void saveImageToFile(byte[] imageData) {
            OutputSettingsNode outputSettingsNode = (OutputSettingsNode) findNodeByTitle("輸出設置");
            String outputDirectory = outputSettingsNode.getOutputDirectory();
            ImageUtils.saveImage(imageData, outputDirectory).ifPresent(file ->
                    showAlert("成功", "圖像已保存到: " + file.getAbsolutePath())
            );
        }

        private UIComponentsData collectUIData() {
            UIComponentsData data = new UIComponentsData();

            ApiSettingsNode apiSettingsNode = (ApiSettingsNode) findNodeByTitle("API設置");
            data.apiKey = apiSettingsNode.getApiKey();
            data.model = apiSettingsNode.getModel();

            PromptNode positivePromptNode = (PromptNode) findNodeByTitle("正面提示詞");
            data.positivePromptPreviewText = positivePromptNode.getPromptText();

            PromptNode negativePromptNode = (PromptNode) findNodeByTitle("負面提示詞");
            data.negativePromptPreviewText = negativePromptNode.getPromptText();

            SamplingSettingsNode samplingSettingsNode = (SamplingSettingsNode) findNodeByTitle("採樣設置");
            data.sampler = samplingSettingsNode.getSampler();
            data.steps = samplingSettingsNode.getSteps();
            data.seed = samplingSettingsNode.getSeed();

            OutputSettingsNode outputSettingsNode = (OutputSettingsNode) findNodeByTitle("輸出設置");
            data.outputWidth = outputSettingsNode.getOutputWidth();
            data.outputHeight = outputSettingsNode.getOutputHeight();
            data.ratio = outputSettingsNode.getRatio();
            data.count = outputSettingsNode.getCount();

            Text2ImageSettingsNode text2ImageSettingsNode = (Text2ImageSettingsNode) findNodeByTitle("文生圖設置");
            data.smea = text2ImageSettingsNode.isSmea();
            data.smeaDyn = text2ImageSettingsNode.isSmeaDyn();

            return data;
        }

        private GenerationPayload createGenerationPayload(UIComponentsData uiData) {
            GenerationPayload payload = new GenerationPayload();
            GenerationPayload.GenerationParameters params = new GenerationPayload.GenerationParameters();
            payload.setParameters(params);

            payload.setInput(uiData.positivePromptPreviewText);
            payload.setModel(uiData.model);
            payload.setAction("generate");

            params.setParams_version(1);
            params.setWidth(uiData.outputWidth);
            params.setHeight(uiData.outputHeight);
            params.setScale(uiData.ratio);
            params.setSampler(uiData.sampler);
            params.setSteps(uiData.steps);
            params.setN_samples(uiData.count);
            params.setSeed(uiData.seed);
            params.setSm(uiData.smea);
            params.setSm_dyn(uiData.smeaDyn);

            params.setNegative_prompt(uiData.negativePromptPreviewText);
            return payload;
        }
    }

    public static void main(String[] args) {
        launch(args);
    }
}