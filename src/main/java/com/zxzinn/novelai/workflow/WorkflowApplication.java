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
import javafx.geometry.Bounds;
import javafx.geometry.Insets;
import javafx.geometry.Point2D;
import javafx.scene.Group;
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
import javafx.scene.shape.Line;
import javafx.scene.transform.Scale;
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

@Log4j2
public class WorkflowApplication extends Application {

    private static final double GRID_SIZE = 20;
    private static final double CONNECTION_DISTANCE_THRESHOLD = 50;
    private static final double ZOOM_FACTOR = 1.1;
    private static final double MIN_ZOOM = 0.1;
    private static final double MAX_ZOOM = 10;

    private Pane workflowPane;
    private Group zoomGroup;
    private final List<WorkflowNode> nodes = new ArrayList<>();
    private final List<Connection> connections = new ArrayList<>();
    private WorkflowNode sourceNode;
    private PortCircle sourcePort;
    private CubicCurve previewCurve;

    private PropertiesManager propertiesManager;
    private APIClient apiClient;

    private Point2D lastMousePosition;
    private Scale scaleTransform;
    private GenerationNode generationNode;

    @Override
    public void start(Stage primaryStage) {
        propertiesManager = PropertiesManager.getInstance();
        apiClient = new APIClient(Endpoint.GENERATE_IMAGE);

        workflowPane = new Pane();
        workflowPane.setStyle("-fx-background-color: #2b2b2b;");

        zoomGroup = new Group(workflowPane);
        scaleTransform = new Scale();
        zoomGroup.getTransforms().add(scaleTransform);

        drawGrid();

        Pane outerPane = new Pane(zoomGroup);
        outerPane.setPrefSize(1200, 800);

        Scene scene = new Scene(outerPane);
        primaryStage.setScene(scene);
        primaryStage.setTitle("NovelAI Workflow");

        createNodes();
        createPresetConnections();

        setupZoomAndPan(scene, outerPane);
        setupContextMenu(workflowPane);
        setupKeyboardShortcuts(scene);

        primaryStage.show();
    }

    private void updateWorkflowPaneSize() {
        double minX = Double.MAX_VALUE;
        double minY = Double.MAX_VALUE;
        double maxX = Double.MIN_VALUE;
        double maxY = Double.MIN_VALUE;

        for (WorkflowNode node : nodes) {
            minX = Math.min(minX, node.getLayoutX());
            minY = Math.min(minY, node.getLayoutY());
            maxX = Math.max(maxX, node.getLayoutX() + node.getWidth());
            maxY = Math.max(maxY, node.getLayoutY() + node.getHeight());
        }

        // 添加一些邊距
        double margin = 50;
        workflowPane.setPrefSize(maxX - minX + margin * 2, maxY - minY + margin * 2);

        // 移動所有節點，確保它們都在可見區域內
        double offsetX = margin - minX;
        double offsetY = margin - minY;
        for (WorkflowNode node : nodes) {
            node.setLayoutX(node.getLayoutX() + offsetX);
            node.setLayoutY(node.getLayoutY() + offsetY);
        }
    }

    private void setupZoomAndPan(Scene scene, Pane outerPane) {
        scene.setOnScroll(event -> {
            event.consume();
            double zoomFactor = event.getDeltaY() > 0 ? ZOOM_FACTOR : 1 / ZOOM_FACTOR;
            zoom(zoomFactor, new Point2D(event.getX(), event.getY()));
        });

        outerPane.setOnMousePressed(event -> {
            lastMousePosition = new Point2D(event.getX(), event.getY());
        });

        outerPane.setOnMouseDragged(event -> {
            if (lastMousePosition != null) {
                double deltaX = event.getX() - lastMousePosition.getX();
                double deltaY = event.getY() - lastMousePosition.getY();

                // 限制平移範圍
                double newTranslateX = zoomGroup.getTranslateX() + deltaX;
                double newTranslateY = zoomGroup.getTranslateY() + deltaY;

                double minTranslateX = outerPane.getWidth() - workflowPane.getWidth() * scaleTransform.getX();
                double minTranslateY = outerPane.getHeight() - workflowPane.getHeight() * scaleTransform.getY();

                newTranslateX = Math.min(0, Math.max(newTranslateX, minTranslateX));
                newTranslateY = Math.min(0, Math.max(newTranslateY, minTranslateY));

                zoomGroup.setTranslateX(newTranslateX);
                zoomGroup.setTranslateY(newTranslateY);

                lastMousePosition = new Point2D(event.getX(), event.getY());
            }
        });
    }

    private void zoom(double factor, Point2D pivot) {
        double oldScale = scaleTransform.getX();
        double newScale = oldScale * factor;
        newScale = Math.max(MIN_ZOOM, Math.min(MAX_ZOOM, newScale));

        Point2D mousePoint = zoomGroup.sceneToLocal(pivot);

        scaleTransform.setX(newScale);
        scaleTransform.setY(newScale);

        Point2D newMousePoint = zoomGroup.sceneToLocal(pivot);

        zoomGroup.setTranslateX(zoomGroup.getTranslateX() - (newMousePoint.getX() - mousePoint.getX()) * newScale);
        zoomGroup.setTranslateY(zoomGroup.getTranslateY() - (newMousePoint.getY() - mousePoint.getY()) * newScale);
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
            Point2D nodeCenter = generationNode.localToScene(
                    generationNode.getBoundsInLocal().getWidth() / 2,
                    generationNode.getBoundsInLocal().getHeight() / 2
            );

            double sceneWidth = zoomGroup.getScene().getWidth();
            double sceneHeight = zoomGroup.getScene().getHeight();

            double newTranslateX = sceneWidth / 2 - nodeCenter.getX() * scaleTransform.getX();
            double newTranslateY = sceneHeight / 2 - nodeCenter.getY() * scaleTransform.getY();

            zoomGroup.setTranslateX(newTranslateX);
            zoomGroup.setTranslateY(newTranslateY);
        }
    }

    private void setupContextMenu(Pane pane) {
        ContextMenu contextMenu = new ContextMenu();
        MenuItem addNodeItem = new MenuItem("添加新節點");
        addNodeItem.setOnAction(e -> addNewNode(contextMenu.getX(), contextMenu.getY()));
        contextMenu.getItems().add(addNodeItem);

        pane.setOnContextMenuRequested(event -> {
            contextMenu.show(pane, event.getScreenX(), event.getScreenY());
        });
    }

    private void addNewNode(double x, double y) {
        Point2D localPoint = zoomGroup.sceneToLocal(x, y);
        WorkflowNode newNode = new CustomNode("自定義節點", localPoint.getX(), localPoint.getY());
        nodes.add(newNode);
        workflowPane.getChildren().add(newNode);
        updateWorkflowPaneSize();  // 添加新節點後更新工作流程面板大小
    }

    private void drawGrid() {
        double width = 10000;
        double height = 10000;

        for (double x = 0; x < width; x += GRID_SIZE) {
            Line vline = new Line(x, 0, x, height);
            vline.setStroke(Color.gray(0.2));
            workflowPane.getChildren().add(vline);
        }

        for (double y = 0; y < height; y += GRID_SIZE) {
            Line hline = new Line(0, y, width, y);
            hline.setStroke(Color.gray(0.2));
            workflowPane.getChildren().add(hline);
        }
    }

    private void createNodes() {
        ApiSettingsNode apiSettingsNode = new ApiSettingsNode("API設置", 50, 50);
        PromptNode positivePromptNode = new PromptNode("正面提示詞", 50, 250);
        PromptNode negativePromptNode = new PromptNode("負面提示詞", 50, 450);
        SamplingSettingsNode samplingSettingsNode = new SamplingSettingsNode("採樣設置", 300, 50);
        OutputSettingsNode outputSettingsNode = new OutputSettingsNode("輸出設置", 300, 250);
        Text2ImageSettingsNode text2ImageSettingsNode = new Text2ImageSettingsNode("文生圖設置", 300, 450);
        generationNode = new GenerationNode("生成", 550, 250);

        nodes.add(apiSettingsNode);
        nodes.add(positivePromptNode);
        nodes.add(negativePromptNode);
        nodes.add(samplingSettingsNode);
        nodes.add(outputSettingsNode);
        nodes.add(text2ImageSettingsNode);
        nodes.add(generationNode);

        workflowPane.getChildren().addAll(apiSettingsNode, positivePromptNode, negativePromptNode,
                samplingSettingsNode, outputSettingsNode, text2ImageSettingsNode, generationNode);
    }

    private void createPresetConnections() {
        connectNodes("API設置");
        connectNodes("正面提示詞");
        connectNodes("負面提示詞");
        connectNodes("採樣設置");
        connectNodes("輸出設置");
        connectNodes("文生圖設置");
    }

    private void connectNodes(String sourceTitle) {
        WorkflowNode sourceNode = findNodeByTitle(sourceTitle);
        WorkflowNode targetNode = findNodeByTitle("生成");

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

    private abstract class WorkflowNode extends Pane {
        protected String title;
        protected List<PortCircle> inputPorts = new ArrayList<>();
        protected List<PortCircle> outputPorts = new ArrayList<>();
        private double dragDeltaX, dragDeltaY;

        public WorkflowNode(String title, double x, double y) {
            this.title = title;
            setLayoutX(snapToGrid(x));
            setLayoutY(snapToGrid(y));
            setPrefSize(200, 200);
            setStyle("-fx-background-color: #3c3f41; -fx-border-color: #5e5e5e; -fx-border-width: 1; -fx-border-radius: 5; -fx-background-radius: 5;");

            DropShadow dropShadow = new DropShadow();
            dropShadow.setColor(Color.BLACK);
            dropShadow.setRadius(10);
            setEffect(dropShadow);

            Label titleLabel = new Label(title);
            titleLabel.setStyle("-fx-text-fill: white; -fx-font-weight: bold;");
            titleLabel.setLayoutX(10);
            titleLabel.setLayoutY(10);

            getChildren().add(titleLabel);

            setOnMousePressed(this::onMousePressed);
            setOnMouseDragged(this::onMouseDragged);
            setOnMouseReleased(this::onMouseReleased);

            setupContextMenu();
        }

        private void setupContextMenu() {
            ContextMenu contextMenu = new ContextMenu();
            MenuItem deleteItem = new MenuItem("刪除節點");
            deleteItem.setOnAction(e -> deleteNode());
            contextMenu.getItems().add(deleteItem);

            setOnContextMenuRequested(e -> contextMenu.show(this, e.getScreenX(), e.getScreenY()));
        }

        private void deleteNode() {
            workflowPane.getChildren().remove(this);
            nodes.remove(this);
            connections.removeIf(conn -> conn.getSourceNode() == this || conn.getTargetNode() == this);
            workflowPane.getChildren().removeIf(node -> node instanceof Connection &&
                    (((Connection) node).getSourceNode() == this || ((Connection) node).getTargetNode() == this));
        }

        protected void addInputPort(String name) {
            PortCircle port = new PortCircle(name, true);
            inputPorts.add(port);
            updatePorts();
        }

        protected void addOutputPort(String name) {
            PortCircle port = new PortCircle(name, false);
            outputPorts.add(port);
            updatePorts();
        }

        private void updatePorts() {
            getChildren().removeIf(node -> node instanceof PortCircle);

            for (int i = 0; i < inputPorts.size(); i++) {
                PortCircle inputPort = inputPorts.get(i);
                inputPort.setLayoutX(0);
                inputPort.setLayoutY(40 + i * 30);
                getChildren().add(inputPort);
            }

            for (int i = 0; i < outputPorts.size(); i++) {
                PortCircle outputPort = outputPorts.get(i);
                outputPort.setLayoutX(getPrefWidth());
                outputPort.setLayoutY(40 + i * 30);
                getChildren().add(outputPort);
            }
        }

        private void onMousePressed(MouseEvent event) {
            toFront();
            Point2D localPoint = sceneToLocal(event.getSceneX(), event.getSceneY());
            dragDeltaX = getLayoutX() - localPoint.getX();
            dragDeltaY = getLayoutY() - localPoint.getY();
        }

        private void onMouseDragged(MouseEvent event) {
            Point2D localPoint = sceneToLocal(event.getSceneX(), event.getSceneY());
            double newX = snapToGrid(localPoint.getX() + dragDeltaX);
            double newY = snapToGrid(localPoint.getY() + dragDeltaY);
            relocate(newX, newY);
            updateConnections();
            updateWorkflowPaneSize();  // 在拖動節點後更新工作流程面板大小
        }

        private void onMouseReleased(MouseEvent event) {
            updateConnections();
        }

        private void updateConnections() {
            connections.stream()
                    .filter(conn -> conn.getSourceNode() == this || conn.getTargetNode() == this)
                    .forEach(Connection::update);
        }

        public List<PortCircle> getAllPorts() {
            List<PortCircle> allPorts = new ArrayList<>(inputPorts);
            allPorts.addAll(outputPorts);
            return allPorts;
        }
    }

    private double snapToGrid(double value) {
        return Math.round(value / GRID_SIZE) * GRID_SIZE;
    }

    private class ApiSettingsNode extends WorkflowNode {
        private final TextField apiKeyField;
        private final ComboBox<String> modelComboBox;

        public ApiSettingsNode(String title, double x, double y) {
            super(title, x, y);
            addOutputPort("API設置");

            VBox content = new VBox(5);
            content.setLayoutX(10);
            content.setLayoutY(40);
            content.setPrefWidth(180);

            apiKeyField = new TextField();
            apiKeyField.setPromptText("API Key");

            modelComboBox = new ComboBox<>();
            modelComboBox.getItems().addAll(NAIConstants.MODELS);
            modelComboBox.setValue(propertiesManager.getString("model", "nai-diffusion-3"));

            content.getChildren().addAll(new Label("API Key"), apiKeyField,
                    new Label("模型"), modelComboBox);

            getChildren().add(content);

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

        public PromptNode(String title, double x, double y) {
            super(title, x, y);
            addOutputPort("提示詞");

            promptArea = new TextArea();
            promptArea.setLayoutX(10);
            promptArea.setLayoutY(40);
            promptArea.setPrefSize(180, 150);
            getChildren().add(promptArea);

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

        public SamplingSettingsNode(String title, double x, double y) {
            super(title, x, y);
            addOutputPort("採樣設置");

            VBox content = new VBox(5);
            content.setLayoutX(10);
            content.setLayoutY(40);
            content.setPrefWidth(180);

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

            getChildren().add(content);

            setupListeners();
        }

        private void setupListeners() {
            samplerComboBox.valueProperty().addListener((obs, oldVal, newVal) ->
                    propertiesManager.setString("sampler", newVal));
            stepsSlider.valueProperty().addListener((obs, oldVal, newVal) -> {
                int steps = newVal.intValue();
                propertiesManager.setInt("steps", steps);
                ((Label)((VBox)getChildren().getFirst()).getChildren().get(4)).setText("步數: " + steps);
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

        public OutputSettingsNode(String title, double x, double y) {
            super(title, x, y);
            addOutputPort("輸出設置");

            VBox content = new VBox(5);
            content.setLayoutX(10);
            content.setLayoutY(40);
            content.setPrefWidth(180);

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

            getChildren().add(content);

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

        public Text2ImageSettingsNode(String title, double x, double y) {
            super(title, x, y);
            addOutputPort("文生圖設置");

            VBox content = new VBox(5);
            content.setLayoutX(10);
            content.setLayoutY(40);
            content.setPrefWidth(180);

            smeaCheckBox = new CheckBox("SMEA");
            smeaCheckBox.setSelected(propertiesManager.getBoolean("smea", true));

            smeaDynCheckBox = new CheckBox("SMEA DYN");
            smeaDynCheckBox.setSelected(propertiesManager.getBoolean("smeaDyn", false));

            content.getChildren().addAll(smeaCheckBox, smeaDynCheckBox);

            getChildren().add(content);

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

        public GenerationNode(String title, double x, double y) {
            super(title, x, y);
            addInputPort("API設置");
            addInputPort("正面提示詞");
            addInputPort("負面提示詞");
            addInputPort("採樣設置");
            addInputPort("輸出設置");
            addInputPort("文生圖設置");
            addOutputPort("生成結果");

            Button generateButton = new Button("生成");
            generateButton.setLayoutX(50);
            generateButton.setLayoutY(40);
            generateButton.setPrefSize(100, 30);

            generateButton.setOnAction(event -> generateImage());

            previewImageView = new ImageView();
            previewImageView.setPreserveRatio(true);

            imageScrollPane = new ScrollPane(previewImageView);
            imageScrollPane.setLayoutX(10);
            imageScrollPane.setLayoutY(80);
            imageScrollPane.setPrefSize(180, 100);
            imageScrollPane.setFitToWidth(true);
            imageScrollPane.setFitToHeight(true);

            getChildren().addAll(generateButton, imageScrollPane);
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

    private class CustomNode extends WorkflowNode {
        private final TextArea customTextArea;

        public CustomNode(String title, double x, double y) {
            super(title, x, y);
            addInputPort("輸入");
            addOutputPort("輸出");

            customTextArea = new TextArea();
            customTextArea.setLayoutX(10);
            customTextArea.setLayoutY(40);
            customTextArea.setPrefSize(180, 150);
            customTextArea.setPromptText("在此輸入自定義邏輯...");

            getChildren().add(customTextArea);
        }

        public String getCustomLogic() {
            return customTextArea.getText();
        }
    }

    private class PortCircle extends javafx.scene.shape.Circle {
        private final String portName;
        private final boolean isInput;

        public PortCircle(String portName, boolean isInput) {
            super(5);
            this.portName = portName;
            this.isInput = isInput;
            setFill(Color.LIGHTBLUE);
            setStroke(Color.BLUE);

            setOnMouseEntered(e -> setFill(Color.YELLOW));
            setOnMouseExited(e -> setFill(Color.LIGHTBLUE));
            setOnMousePressed(this::onMousePressed);
            setOnMouseReleased(this::onMouseReleased);
            setOnMouseDragged(this::onMouseDragged);

            Tooltip tooltip = new Tooltip(portName);
            Tooltip.install(this, tooltip);
        }

        private void onMousePressed(MouseEvent event) {
            if (!isInput) {
                sourceNode = (WorkflowNode) getParent();
                sourcePort = this;
                startConnection(event);
            }
            event.consume();
        }

        private void onMouseDragged(MouseEvent event) {
            if (previewCurve != null) {
                Point2D end = localToScene(new Point2D(0, 0));
                updatePreviewCurve(previewCurve.getStartX(), previewCurve.getStartY(),
                        event.getSceneX(), event.getSceneY());
            }
            event.consume();
        }

        private void onMouseReleased(MouseEvent event) {
            if (sourceNode != null) {
                PortCircle targetPort = findNearestPort(event.getSceneX(), event.getSceneY());
                if (targetPort != null && targetPort.isInput && sourceNode != targetPort.getParent()) {
                    createConnection((WorkflowNode) targetPort.getParent(), targetPort);
                }
            }
            endConnection();
            event.consume();
        }

        private void startConnection(MouseEvent event) {
            Point2D start = localToScene(new Point2D(0, 0));
            previewCurve = createCurve(start.getX(), start.getY(), event.getSceneX(), event.getSceneY());
            workflowPane.getChildren().add(previewCurve);
        }

        private void createConnection(WorkflowNode targetNode, PortCircle targetPort) {
            Connection connection = new Connection(sourceNode, sourcePort, targetNode, targetPort);
            connections.add(connection);
            workflowPane.getChildren().add(connection);
        }

        private void endConnection() {
            sourceNode = null;
            sourcePort = null;
            if (previewCurve != null) {
                workflowPane.getChildren().remove(previewCurve);
                previewCurve = null;
            }
        }
    }

    private PortCircle findNearestPort(double sceneX, double sceneY) {
        PortCircle nearestPort = null;
        double minDistance = Double.MAX_VALUE;

        Point2D mousePoint = new Point2D(sceneX, sceneY);

        for (WorkflowNode node : nodes) {
            for (PortCircle port : node.getAllPorts()) {
                Point2D portPosition = port.localToScene(new Point2D(0, 0));
                double distance = mousePoint.distance(portPosition);
                if (distance < minDistance && distance < CONNECTION_DISTANCE_THRESHOLD) {
                    minDistance = distance;
                    nearestPort = port;
                }
            }
        }

        return nearestPort;
    }

    private CubicCurve createCurve(double startX, double startY, double endX, double endY) {
        CubicCurve curve = new CubicCurve();
        curve.setStartX(startX);
        curve.setStartY(startY);
        curve.setEndX(endX);
        curve.setEndY(endY);
        curve.setControlX1(startX + 100);
        curve.setControlY1(startY);
        curve.setControlX2(endX - 100);
        curve.setControlY2(endY);
        curve.setStroke(Color.LIGHTGREEN);
        curve.setFill(null);
        curve.setStrokeWidth(2);
        return curve;
    }

    private void updatePreviewCurve(double startX, double startY, double endX, double endY) {
        previewCurve.setEndX(endX);
        previewCurve.setEndY(endY);
        previewCurve.setControlX1(startX + (endX - startX) / 2);
        previewCurve.setControlY1(startY);
        previewCurve.setControlX2(startX + (endX - startX) / 2);
        previewCurve.setControlY2(endY);
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
            update();

            setOnContextMenuRequested(this::showContextMenu);
        }

        public void update() {
            if (sourcePort != null && targetPort != null) {
                Point2D start = sourcePort.localToScene(new Point2D(0, 0));
                Point2D end = targetPort.localToScene(new Point2D(0, 0));
                setStartX(start.getX());
                setStartY(start.getY());
                setEndX(end.getX());
                setEndY(end.getY());
                setControlX1(getStartX() + (getEndX() - getStartX()) / 2);
                setControlY1(getStartY());
                setControlX2(getStartX() + (getEndX() - getStartX()) / 2);
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
            workflowPane.getChildren().remove(this);
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