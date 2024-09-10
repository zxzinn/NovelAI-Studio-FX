package com.zxzinn.novelai.workflow;

import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Line;
import javafx.stage.Stage;

import java.util.ArrayList;
import java.util.List;

public class VisualWorkflowEditor extends Application {

    private Pane canvas;
    private TextField valueField;
    private ComboBox<String> controlTypeCombo;
    private final List<Connection> connections = new ArrayList<>();
    private WorkflowNode sourceNode;
    private Line tempLine;

    @Override
    public void start(Stage primaryStage) {
        BorderPane root = new BorderPane();

        VBox nodeList = createNodeList();
        canvas = new Pane();
        canvas.setStyle("-fx-background-color: #EEEEEE;");
        VBox propertyEditor = createPropertyEditor();

        root.setLeft(nodeList);
        root.setCenter(canvas);
        root.setRight(propertyEditor);

        tempLine = new Line();
        tempLine.setStroke(Color.GRAY);
        tempLine.setStrokeWidth(2);
        tempLine.setVisible(false);
        canvas.getChildren().add(tempLine);

        Scene scene = new Scene(root, 1000, 600);
        primaryStage.setTitle("視覺化工作流編輯器");
        primaryStage.setScene(scene);
        primaryStage.show();
    }

    private VBox createNodeList() {
        VBox nodeList = new VBox(10);
        nodeList.setPadding(new Insets(10));
        Button addWidthNode = new Button("添加寬度節點");
        Button addHeightNode = new Button("添加高度節點");
        nodeList.getChildren().addAll(addWidthNode, addHeightNode);

        addWidthNode.setOnAction(e -> addNode("寬度", 100));
        addHeightNode.setOnAction(e -> addNode("高度", 200));

        return nodeList;
    }

    private VBox createPropertyEditor() {
        VBox editor = new VBox(10);
        editor.setPadding(new Insets(10));
        Label propertyLabel = new Label("節點屬性");
        valueField = new TextField();
        valueField.setPromptText("值");
        controlTypeCombo = new ComboBox<>();
        controlTypeCombo.getItems().addAll("固定", "可變");
        controlTypeCombo.setPromptText("控制類型");
        editor.getChildren().addAll(propertyLabel, valueField, controlTypeCombo);
        return editor;
    }

    private void addNode(String type, double y) {
        WorkflowNode node = new WorkflowNode(type);
        node.setLayoutX(100);
        node.setLayoutY(y);
        canvas.getChildren().add(node);
    }

    private class WorkflowNode extends StackPane {
        private final Label titleLabel;
        private final Circle inputPort;
        private final Circle outputPort;
        private final String type;
        private int value = 512;
        private String controlType = "固定";

        public WorkflowNode(String type) {
            this.type = type;
            titleLabel = new Label(type + ": " + value);
            inputPort = new Circle(5, Color.BLUE);
            outputPort = new Circle(5, Color.RED);

            VBox layout = new VBox(5);
            layout.getChildren().addAll(inputPort, titleLabel, outputPort);

            this.getChildren().add(layout);

            setStyle("-fx-background-color: #AAAAAA; -fx-border-color: black; -fx-padding: 5;");
            setPrefSize(100, 70);

            setupDragging();
            setupConnecting();
            setupPropertyEditing();
        }

        private void setupDragging() {
            final Delta dragDelta = new Delta();

            setOnMousePressed(me -> {
                dragDelta.x = getLayoutX() - me.getSceneX();
                dragDelta.y = getLayoutY() - me.getSceneY();
                toFront();
            });

            setOnMouseDragged(me -> {
                setLayoutX(me.getSceneX() + dragDelta.x);
                setLayoutY(me.getSceneY() + dragDelta.y);
                updateConnections();
            });
        }

        private void setupConnecting() {
            outputPort.setOnMousePressed(me -> {
                sourceNode = this;
                tempLine.setStartX(getLayoutX() + getWidth() / 2);
                tempLine.setStartY(getLayoutY() + getHeight());
                tempLine.setEndX(me.getSceneX());
                tempLine.setEndY(me.getSceneY());
                tempLine.setVisible(true);
                me.consume();
            });

            outputPort.setOnMouseDragged(me -> {
                tempLine.setEndX(me.getSceneX());
                tempLine.setEndY(me.getSceneY());
                me.consume();
            });

            outputPort.setOnMouseReleased(me -> {
                tempLine.setVisible(false);
                sourceNode = null;
                me.consume();
            });

            inputPort.setOnMouseEntered(me -> {
                if (sourceNode != null && sourceNode != this) {
                    inputPort.setFill(Color.GREEN);
                }
            });

            inputPort.setOnMouseExited(me -> inputPort.setFill(Color.BLUE));

            inputPort.setOnMouseReleased(me -> {
                if (sourceNode != null && sourceNode != this) {
                    if (!isConnected(sourceNode, this)) {
                        Connection connection = new Connection(sourceNode, this);
                        connections.add(connection);
                        canvas.getChildren().add(connection);
                    }
                    sourceNode = null;
                    tempLine.setVisible(false);
                }
                inputPort.setFill(Color.BLUE);
                me.consume();
            });
        }

        private void setupPropertyEditing() {
            setOnMouseClicked(me -> {
                valueField.setText(String.valueOf(value));
                controlTypeCombo.setValue(controlType);

                valueField.setOnAction(e -> {
                    try {
                        value = Integer.parseInt(valueField.getText());
                        updateTitle();
                    } catch (NumberFormatException ex) {
                        // 處理非法輸入
                        valueField.setText(String.valueOf(value));
                    }
                });

                controlTypeCombo.setOnAction(e -> controlType = controlTypeCombo.getValue());
            });
        }

        private void updateTitle() {
            titleLabel.setText(type + ": " + value);
        }

        public void updateConnections() {
            connections.stream()
                    .filter(conn -> conn.startNode == this || conn.endNode == this)
                    .forEach(Connection::update);
        }
    }

    private boolean isConnected(WorkflowNode source, WorkflowNode target) {
        return connections.stream().anyMatch(conn ->
                (conn.startNode == source && conn.endNode == target) ||
                        (conn.startNode == target && conn.endNode == source)
        );
    }

    private class Connection extends Line {
        private final WorkflowNode startNode;
        private final WorkflowNode endNode;

        public Connection(WorkflowNode start, WorkflowNode end) {
            startNode = start;
            endNode = end;
            setStrokeWidth(2);
            setStroke(Color.GREEN);
            update();

            setOnMouseClicked(me -> {
                if (me.getClickCount() == 2) {
                    canvas.getChildren().remove(this);
                    connections.remove(this);
                }
            });
        }

        public void update() {
            setStartX(startNode.getLayoutX() + startNode.getWidth() / 2);
            setStartY(startNode.getLayoutY() + startNode.getHeight());
            setEndX(endNode.getLayoutX() + endNode.getWidth() / 2);
            setEndY(endNode.getLayoutY());
        }
    }

    private static class Delta {
        double x, y;
    }

    public static void main(String[] args) {
        launch(args);
    }
}