package com.zxzinn.novelai.workflow;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.Pane;
import javafx.stage.Stage;
import javafx.scene.paint.Color;
import javafx.scene.effect.DropShadow;
import javafx.geometry.Point2D;
import javafx.scene.shape.CubicCurve;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class WorkflowApplication extends Application {

    private Pane workflowPane;
    private List<WorkflowNode> nodes = new ArrayList<>();
    private List<Connection> connections = new ArrayList<>();
    private WorkflowNode sourceNode;
    private PortCircle sourcePort;
    private CubicCurve previewCurve;

    private static final double GRID_SIZE = 20;
    private static final double CONNECTION_DISTANCE_THRESHOLD = 50; // 連接距離閾值

    @Override
    public void start(Stage primaryStage) {
        workflowPane = new Pane();
        workflowPane.setPrefSize(1000, 800);
        workflowPane.setStyle("-fx-background-color: #2b2b2b;");

        drawGrid();

        Scene scene = new Scene(workflowPane);
        primaryStage.setScene(scene);
        primaryStage.setTitle("NovelAI Workflow");

        createNodes();

        primaryStage.show();
    }

    private void drawGrid() {
        for (int i = 0; i < workflowPane.getPrefWidth(); i += GRID_SIZE) {
            javafx.scene.shape.Line vline = new javafx.scene.shape.Line(i, 0, i, workflowPane.getPrefHeight());
            vline.setStroke(Color.gray(0.2));
            workflowPane.getChildren().add(vline);
        }
        for (int i = 0; i < workflowPane.getPrefHeight(); i += GRID_SIZE) {
            javafx.scene.shape.Line hline = new javafx.scene.shape.Line(0, i, workflowPane.getPrefWidth(), i);
            hline.setStroke(Color.gray(0.2));
            workflowPane.getChildren().add(hline);
        }
    }

    private void createNodes() {
        PromptNode promptNode = new PromptNode("提示詞", 50, 50);
        SamplerNode samplerNode = new SamplerNode("採樣器", 300, 50);
        OutputNode outputNode = new OutputNode("輸出", 550, 50);

        nodes.add(promptNode);
        nodes.add(samplerNode);
        nodes.add(outputNode);

        workflowPane.getChildren().addAll(promptNode, samplerNode, outputNode);
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
            setPrefSize(200, 150);
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
            dragDeltaX = getLayoutX() - event.getSceneX();
            dragDeltaY = getLayoutY() - event.getSceneY();
        }

        private void onMouseDragged(MouseEvent event) {
            double newX = snapToGrid(event.getSceneX() + dragDeltaX);
            double newY = snapToGrid(event.getSceneY() + dragDeltaY);
            setLayoutX(newX);
            setLayoutY(newY);
            updateConnections();
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

    private class PromptNode extends WorkflowNode {
        public PromptNode(String title, double x, double y) {
            super(title, x, y);
            addOutputPort("提示詞");

            TextArea promptArea = new TextArea();
            promptArea.setLayoutX(10);
            promptArea.setLayoutY(40);
            promptArea.setPrefSize(180, 100);
            getChildren().add(promptArea);
        }
    }

    private class SamplerNode extends WorkflowNode {
        public SamplerNode(String title, double x, double y) {
            super(title, x, y);
            addInputPort("提示詞");
            addOutputPort("圖像");

            ComboBox<String> samplerComboBox = new ComboBox<>();
            samplerComboBox.getItems().addAll("Euler a", "Euler", "LMS", "Heun", "DPM2", "DPM2 a", "DPM++ 2S a", "DPM++ 2M", "DPM++ SDE", "DPM fast", "DPM adaptive", "LMS Karras", "DPM2 Karras", "DPM2 a Karras", "DPM++ 2S a Karras", "DPM++ 2M Karras", "DPM++ SDE Karras", "DDIM", "PLMS");
            samplerComboBox.setLayoutX(10);
            samplerComboBox.setLayoutY(40);
            samplerComboBox.setPrefWidth(180);

            Slider stepsSlider = new Slider(1, 150, 28);
            stepsSlider.setLayoutX(10);
            stepsSlider.setLayoutY(80);
            stepsSlider.setPrefWidth(180);

            Label stepsLabel = new Label("步數: 28");
            stepsLabel.setLayoutX(10);
            stepsLabel.setLayoutY(110);
            stepsLabel.setStyle("-fx-text-fill: white;");

            stepsSlider.valueProperty().addListener((obs, oldVal, newVal) ->
                    stepsLabel.setText(String.format("步數: %d", newVal.intValue())));

            getChildren().addAll(samplerComboBox, stepsSlider, stepsLabel);
        }
    }

    private class OutputNode extends WorkflowNode {
        public OutputNode(String title, double x, double y) {
            super(title, x, y);
            addInputPort("圖像");

            Button generateButton = new Button("生成");
            generateButton.setLayoutX(50);
            generateButton.setLayoutY(60);
            generateButton.setPrefSize(100, 30);

            getChildren().add(generateButton);
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
        private final WorkflowNode sourceNode;
        private final PortCircle sourcePort;
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

        public WorkflowNode getSourceNode() {
            return sourceNode;
        }

        public WorkflowNode getTargetNode() {
            return targetNode;
        }
    }

    public static void main(String[] args) {
        launch(args);
    }
}