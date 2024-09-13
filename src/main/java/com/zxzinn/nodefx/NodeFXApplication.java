package com.zxzinn.nodefx;

import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.geometry.Point2D;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.effect.DropShadow;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.CubicCurve;
import javafx.scene.shape.Circle;
import javafx.stage.Stage;
import lombok.Getter;

import java.util.ArrayList;
import java.util.List;

public abstract class NodeFXApplication extends Application {

    protected WorkflowPane workflowPane;
    protected List<WorkflowNode> nodes = new ArrayList<>();
    protected List<Connection> connections = new ArrayList<>();
    protected Connection previewConnection;

    @Override
    public void start(Stage primaryStage) {
        initializeComponents();
        setupScene(primaryStage);
        createNodes();
        createPresetConnections();
        primaryStage.show();
    }

    protected abstract void initializeComponents();

    protected void setupScene(Stage primaryStage) {
        workflowPane = new WorkflowPane();
        Scene scene = new Scene(workflowPane, 1200, 800);
        primaryStage.setScene(scene);
        primaryStage.setTitle("NodeFX Workflow");
    }

    protected abstract void createNodes();

    protected abstract void createPresetConnections();

    protected void connectNodes(String sourceTitle, String targetTitle) {
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

    protected WorkflowNode findNodeByTitle(String title) {
        return nodes.stream()
                .filter(node -> node.title.equals(title))
                .findFirst()
                .orElse(null);
    }

    public abstract class WorkflowNode extends Region {
        public  String title;
        protected List<PortCircle> inputPorts = new ArrayList<>();
        protected List<PortCircle> outputPorts = new ArrayList<>();
        protected VBox content;

        public WorkflowNode(String title) {
            this.title = title;
            initializeNode();
            setupEventHandlers();
            setupSizeListeners();
        }

        private void initializeNode() {
            setStyle("-fx-background-color: #3c3f41; -fx-border-color: #5e5e5e; -fx-border-width: 1; -fx-border-radius: 5; -fx-background-radius: 5;");
            setEffect(new DropShadow(10, Color.BLACK));

            Label titleLabel = new Label(title);
            titleLabel.setStyle("-fx-text-fill: white; -fx-font-weight: bold;");

            content = new VBox(5);
            content.setPadding(new Insets(10));
            content.getChildren().add(titleLabel);

            getChildren().add(content);

            setupContextMenu();
        }

        private void setupEventHandlers() {
            setOnMousePressed(this::handleMousePressed);
            setOnMouseDragged(this::handleMouseDragged);
        }

        private void handleMousePressed(MouseEvent event) {
            toFront();
            event.consume();
        }

        private void handleMouseDragged(MouseEvent event) {
            setLayoutX(getLayoutX() + event.getX());
            setLayoutY(getLayoutY() + event.getY());
            event.consume();
        }

        private void setupSizeListeners() {
            content.heightProperty().addListener((obs, oldVal, newVal) -> setPrefHeight(newVal.doubleValue() + 20));
            content.widthProperty().addListener((obs, oldVal, newVal) -> setPrefWidth(newVal.doubleValue() + 20));
        }

        private void setupContextMenu() {
            ContextMenu contextMenu = new ContextMenu();
            MenuItem deleteItem = new MenuItem("刪除節點");
            deleteItem.setOnAction(e -> deleteNode());
            contextMenu.getItems().add(deleteItem);

            setOnContextMenuRequested(e -> contextMenu.show(this, e.getScreenX(), e.getScreenY()));
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

        private void deleteNode() {
            workflowPane.getChildren().remove(this);
            nodes.remove(this);
            connections.removeIf(conn -> conn.getSourceNode() == this || conn.getTargetNode() == this);
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

    protected class PortCircle extends Circle {
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
                previewConnection = new Connection(parentNode, this, null, null);
                workflowPane.getChildren().add(previewConnection);
            }
            event.consume();
        }

        private void updateConnection(MouseEvent event) {
            if (!isInput && previewConnection != null) {
                previewConnection.setEndX(event.getSceneX());
                previewConnection.setEndY(event.getSceneY());
            }
            event.consume();
        }

        private void endConnection(MouseEvent event) {
            if (!isInput && previewConnection != null) {
                PortCircle targetPort = findPortAt(event.getSceneX(), event.getSceneY());
                if (targetPort != null && targetPort.isInput && targetPort.parentNode != parentNode) {
                    Connection connection = new Connection(parentNode, this, targetPort.parentNode, targetPort);
                    connections.add(connection);
                    workflowPane.getChildren().add(connection);
                }
                workflowPane.getChildren().remove(previewConnection);
                previewConnection = null;
            }
            event.consume();
        }
    }

    protected PortCircle findPortAt(double sceneX, double sceneY) {
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

    protected class Connection extends CubicCurve {
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
            workflowPane.getChildren().remove(this);
        }
    }

    protected class WorkflowPane extends Pane {
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
            connections.forEach(Connection::update);
        }
    }

    protected void showAlert(String title, String content) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }
}