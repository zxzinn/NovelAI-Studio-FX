package com.zxzinn.novelai.workflow;

import com.zxzinn.novelai.api.APIClient;
import com.zxzinn.novelai.api.Endpoint;
import com.zxzinn.novelai.api.GenerationPayload;
import com.zxzinn.novelai.model.UIComponentsData;
import com.zxzinn.novelai.utils.common.NAIConstants;
import com.zxzinn.novelai.utils.common.PropertiesManager;
import com.zxzinn.novelai.utils.image.ImageUtils;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.geometry.HPos;
import javafx.geometry.Insets;
import javafx.geometry.Point2D;
import javafx.geometry.VPos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.effect.DropShadow;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyCode;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.CubicCurve;
import javafx.scene.shape.Circle;
import javafx.stage.Stage;
import lombok.Getter;
import lombok.extern.log4j.Log4j2;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import javafx.scene.layout.Region;
import javafx.scene.Node;
import javafx.scene.Parent;

@Log4j2
public class WorkflowApplication extends Application {

    private PropertiesManager propertiesManager;
    private APIClient apiClient;
    private GenerationNode generationNode;
    private WorkflowPane workflowPane;

    private List<WorkflowNode> nodes = new ArrayList<>();
    private List<Connection> connections = new ArrayList<>();
    private double mouseAnchorX;
    private double mouseAnchorY;
    private WorkflowNode sourceNode;
    private PortCircle sourcePort;
    private Connection previewConnection;

    @Override
    public void start(Stage primaryStage) {
        propertiesManager = PropertiesManager.getInstance();
        apiClient = new APIClient(Endpoint.GENERATE_IMAGE);

        workflowPane = new WorkflowPane();
        Scene scene = new Scene(workflowPane, 1200, 800);
        primaryStage.setScene(scene);
        primaryStage.setTitle("NovelAI Workflow");

        createNodes(workflowPane);
        createPresetConnections();

        setupKeyboardShortcuts(scene);

        primaryStage.show();
    }

    private void createNodes(WorkflowPane workflowPane) {
        ApiSettingsNode apiSettingsNode = new ApiSettingsNode("API設置");
        PromptNode positivePromptNode = new PromptNode("正面提示詞");
        PromptNode negativePromptNode = new PromptNode("負面提示詞");
        SamplingSettingsNode samplingSettingsNode = new SamplingSettingsNode("採樣設置");
        OutputSettingsNode outputSettingsNode = new OutputSettingsNode("輸出設置");
        Text2ImageSettingsNode text2ImageSettingsNode = new Text2ImageSettingsNode("文生圖設置");
        generationNode = new GenerationNode("生成");

        nodes.add(apiSettingsNode);
        nodes.add(positivePromptNode);
        nodes.add(negativePromptNode);
        nodes.add(samplingSettingsNode);
        nodes.add(outputSettingsNode);
        nodes.add(text2ImageSettingsNode);
        nodes.add(generationNode);

        workflowPane.getChildren().addAll(apiSettingsNode, positivePromptNode, negativePromptNode,
                samplingSettingsNode, outputSettingsNode, text2ImageSettingsNode, generationNode);

        // 設置節點的初始位置
        double startX = 50;
        double startY = 50;
        double spacing = 220;

        for (int i = 0; i < nodes.size(); i++) {
            WorkflowNode node = nodes.get(i);
            node.setLayoutX(startX + (i % 3) * spacing);
            node.setLayoutY(startY + (i / 3) * spacing);
        }

        // 將生成節點放在中心位置
        generationNode.setLayoutX(startX + spacing);
        generationNode.setLayoutY(startY + 2 * spacing);
    }

    private void createPresetConnections() {
        for (WorkflowNode node : nodes) {
            if (node != generationNode) {
                connectNodes(node.title, "生成");
            }
        }
    }

    private void connectNodes(String sourceTitle, String targetTitle) {
        WorkflowNode sourceNode = findNodeByTitle(sourceTitle);
        WorkflowNode targetNode = findNodeByTitle(targetTitle);

        if (sourceNode != null && targetNode != null) {
            PortCircle sourcePort = sourceNode.outputPorts.getFirst();
            PortCircle targetPort = targetNode.inputPorts.stream()
                    .filter(port -> port.portName.equals(sourceTitle))
                    .findFirst()
                    .orElse(null);

            if (sourcePort != null && targetPort != null) {
                Connection connection = new Connection(sourceNode, sourcePort, targetNode, targetPort);
                connections.add(connection);
                workflowPane.getChildren().add(connection);
            }
        }
    }

    private WorkflowNode findNodeByTitle(String title) {
        return nodes.stream()
                .filter(node -> node.title.equals(title))
                .findFirst()
                .orElse(null);
    }

    private void startConnection(WorkflowNode sourceNode, PortCircle sourcePort) {
        this.sourceNode = sourceNode;
        this.sourcePort = sourcePort;
        previewConnection = new Connection(sourceNode, sourcePort, null, null);
        workflowPane.getChildren().add(previewConnection);
    }

    private void updateConnection(double sceneX, double sceneY) {
        if (previewConnection != null) {
            previewConnection.setEndX(sceneX);
            previewConnection.setEndY(sceneY);
        }
    }

    private void endConnection(double sceneX, double sceneY) {
        if (previewConnection != null) {
            PortCircle targetPort = findPortAt(sceneX, sceneY);
            if (targetPort != null && targetPort.isInput && targetPort.parentNode != sourceNode) {
                Connection connection = new Connection(sourceNode, sourcePort, targetPort.parentNode, targetPort);
                connections.add(connection);
                workflowPane.getChildren().add(connection);
            }
            workflowPane.getChildren().remove(previewConnection);
            previewConnection = null;
            sourceNode = null;
            sourcePort = null;
        }
    }

    private PortCircle findPortAt(double sceneX, double sceneY) {
        for (WorkflowNode node : nodes) {
            for (PortCircle port : node.getAllPorts()) {
                Point2D portPosition = port.localToScene(port.getBoundsInLocal().getCenterX(), port.getBoundsInLocal().getCenterY());
                if (portPosition.distance(sceneX, sceneY) < 10) {
                    return port;
                }
            }
        }
        return null;
    }

    private void setupKeyboardShortcuts(Scene scene) {
        scene.setOnKeyPressed(event -> {
            if (event.getCode() == KeyCode.SPACE) {
                focusOnGenerationNode();
            }
        });
    }

    private void focusOnGenerationNode() {
        if (generationNode != null) {
            generationNode.requestFocus();
        }
    }

    private abstract class WorkflowNode extends Region {
        protected String title;
        protected List<PortCircle> inputPorts = new ArrayList<>();
        protected List<PortCircle> outputPorts = new ArrayList<>();
        protected VBox content;

        public WorkflowNode(String title) {
            this.title = title;
            setStyle("-fx-background-color: #3c3f41; -fx-border-color: #5e5e5e; -fx-border-width: 1; -fx-border-radius: 5; -fx-background-radius: 5;");

            DropShadow dropShadow = new DropShadow();
            dropShadow.setColor(Color.BLACK);
            dropShadow.setRadius(10);
            setEffect(dropShadow);

            Label titleLabel = new Label(title);
            titleLabel.setStyle("-fx-text-fill: white; -fx-font-weight: bold;");

            content = new VBox(5);
            content.setPadding(new Insets(10));
            content.getChildren().add(titleLabel);

            getChildren().add(content);

            setupContextMenu();
            setOnMousePressed(event -> {
                mouseAnchorX = event.getSceneX() - getLayoutX();
                mouseAnchorY = event.getSceneY() - getLayoutY();
                toFront();
            });

            setOnMouseDragged(event -> {
                setLayoutX(event.getSceneX() - mouseAnchorX);
                setLayoutY(event.getSceneY() - mouseAnchorY);
                event.consume();
            });

            // 添加大小監聽器
            content.heightProperty().addListener((obs, oldVal, newVal) -> {
                setPrefHeight(newVal.doubleValue() + 20); // 添加一些額外的空間
            });
            content.widthProperty().addListener((obs, oldVal, newVal) -> {
                setPrefWidth(newVal.doubleValue() + 20); // 添加一些額外的空間
            });
        }

        private void setupContextMenu() {
            ContextMenu contextMenu = new ContextMenu();
            MenuItem deleteItem = new MenuItem("刪除節點");
            deleteItem.setOnAction(e -> deleteNode());
            contextMenu.getItems().add(deleteItem);

            setOnContextMenuRequested(e -> contextMenu.show(this, e.getScreenX(), e.getScreenY()));
        }

        private void deleteNode() {
            Parent parent = getParent();
            if (parent instanceof Pane) {
                ((Pane) parent).getChildren().remove(this);
            }
            nodes.remove(this);
            connections.removeIf(conn -> conn.getSourceNode() == this || conn.getTargetNode() == this);
        }

        protected void addInputPort(String name) {
            PortCircle port = new PortCircle(name, true, this);
            inputPorts.add(port);
            getChildren().add(port);
            updatePortPositions();
        }

        protected void addOutputPort(String name) {
            PortCircle port = new PortCircle(name, false, this);
            outputPorts.add(port);
            getChildren().add(port);
            updatePortPositions();
        }

        protected void updatePortPositions() {
            double inputY = 0;
            for (PortCircle port : inputPorts) {
                port.relocate(0, inputY);
                inputY += 20;
            }

            double outputY = 0;
            for (PortCircle port : outputPorts) {
                port.relocate(getWidth(), outputY);
                outputY += 20;
            }
        }

        @Override
        protected void layoutChildren() {
            super.layoutChildren();
            updatePortPositions();
        }

        public List<PortCircle> getAllPorts() {
            List<PortCircle> allPorts = new ArrayList<>(inputPorts);
            allPorts.addAll(outputPorts);
            return allPorts;
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
            VBox.setMargin(promptArea, new Insets(5, 0, 0, 0));

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
                    e.printStackTrace();
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

                // 調整節點大小以適應圖片
                double imageWidth = image.getWidth();
                double imageHeight = image.getHeight();
                double maxWidth = 400;  // 最大寬度
                double maxHeight = 300; // 最大高度

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

    private class PortCircle extends Circle {
        private final String portName;
        private final boolean isInput;
        private WorkflowNode parentNode;

        public PortCircle(String portName, boolean isInput, WorkflowNode parentNode) {
            super(5);
            this.portName = portName;
            this.isInput = isInput;
            this.parentNode = parentNode;
            setFill(Color.LIGHTBLUE);
            setStroke(Color.BLUE);

            setOnMouseEntered(e -> setFill(Color.YELLOW));
            setOnMouseExited(e -> setFill(Color.LIGHTBLUE));

            Tooltip tooltip = new Tooltip(portName);
            Tooltip.install(this, tooltip);

            setOnMousePressed(this::startConnection);
            setOnMouseDragged(this::updateConnection);
            setOnMouseReleased(this::endConnection);
        }

        private void startConnection(MouseEvent event) {
            if (!isInput) {
                WorkflowApplication.this.startConnection(parentNode, this);
            }
            event.consume();
        }

        private void updateConnection(MouseEvent event) {
            if (!isInput) {
                WorkflowApplication.this.updateConnection(event.getSceneX(), event.getSceneY());
            }
            event.consume();
        }

        private void endConnection(MouseEvent event) {
            if (!isInput) {
                WorkflowApplication.this.endConnection(event.getSceneX(), event.getSceneY());
            }
            event.consume();
        }
    }

    private class Connection extends CubicCurve {
        @Getter
        private final WorkflowNode sourceNode;
        private final PortCircle sourcePort;
        @Getter
        private final WorkflowNode targetNode;
        private final PortCircle targetPort;

        public Connection(WorkflowNode sourceNode, PortCircle sourcePort, WorkflowNode targetNode, PortCircle targetPort) {
            this.sourceNode = sourceNode;
            this.sourcePort = sourcePort;
            this.targetNode = targetNode;
            this.targetPort = targetPort;
            setStroke(Color.LIGHTGREEN);
            setFill(null);
            setStrokeWidth(2);

            sourceNode.layoutXProperty().addListener((obs, oldVal, newVal) -> update());
            sourceNode.layoutYProperty().addListener((obs, oldVal, newVal) -> update());
            if (targetNode != null) {
                targetNode.layoutXProperty().addListener((obs, oldVal, newVal) -> update());
                targetNode.layoutYProperty().addListener((obs, oldVal, newVal) -> update());
            }

            setOnContextMenuRequested(this::showContextMenu);
            update();
        }

        public void update() {
            if (sourcePort != null) {
                Point2D sourcePoint = sourcePort.localToScene(sourcePort.getBoundsInLocal().getCenterX(), sourcePort.getBoundsInLocal().getCenterY());
                setStartX(sourcePoint.getX());
                setStartY(sourcePoint.getY());
                setControlX1(getStartX() + 50);
                setControlY1(getStartY());
            }
            if (targetPort != null) {
                Point2D targetPoint = targetPort.localToScene(targetPort.getBoundsInLocal().getCenterX(), targetPort.getBoundsInLocal().getCenterY());
                setEndX(targetPoint.getX());
                setEndY(targetPoint.getY());
                setControlX2(getEndX() - 50);
                setControlY2(getEndY());
            }
        }

        private void showContextMenu(javafx.scene.input.ContextMenuEvent event) {
            ContextMenu contextMenu = new ContextMenu();
            MenuItem deleteItem = new MenuItem("刪除連接");
            deleteItem.setOnAction(e -> deleteConnection());
            contextMenu.getItems().add(deleteItem);
            contextMenu.show(this, event.getScreenX(), event.getScreenY());
        }

        private void deleteConnection() {
            connections.remove(this);
            Parent parent = getParent();
            if (parent instanceof Pane) {
                ((Pane) parent).getChildren().remove(this);
            }
        }
    }

    private class WorkflowPane extends Pane {
        private double lastMouseX;
        private double lastMouseY;

        public WorkflowPane() {
            setStyle("-fx-background-color: #2b2b2b;");
            setPrefSize(1200, 800);

            setOnMousePressed(event -> {
                lastMouseX = event.getX();
                lastMouseY = event.getY();
            });

            setOnMouseDragged(event -> {
                double deltaX = event.getX() - lastMouseX;
                double deltaY = event.getY() - lastMouseY;

                for (Node child : getChildren()) {
                    if (child instanceof WorkflowNode) {
                        WorkflowNode node = (WorkflowNode) child;
                        node.setLayoutX(node.getLayoutX() + deltaX);
                        node.setLayoutY(node.getLayoutY() + deltaY);
                    }
                }

                lastMouseX = event.getX();
                lastMouseY = event.getY();
            });
        }

        @Override
        protected void layoutChildren() {
            super.layoutChildren();
            for (Node child : getChildren()) {
                if (child instanceof Connection) {
                    ((Connection) child).update();
                }
            }
        }
    }

    private void showAlert(String title, String content) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }

    public static void main(String[] args) {
        launch(args);
    }
}