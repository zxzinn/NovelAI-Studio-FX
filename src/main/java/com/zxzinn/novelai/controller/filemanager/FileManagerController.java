package com.zxzinn.novelai.controller.filemanager;

import com.zxzinn.novelai.service.filemanager.FileManagerService;
import com.zxzinn.novelai.utils.common.SettingsManager;
import com.zxzinn.novelai.utils.image.ImageProcessor;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.stage.DirectoryChooser;
import lombok.extern.log4j.Log4j2;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

@Log4j2
public class FileManagerController {
    @FXML private TreeView<String> fileTreeView;
    @FXML private ImageView previewImageView;
    @FXML private ListView<String> metadataListView;
    @FXML private TextField searchField;
    @FXML private Button addButton;
    @FXML private Button removeButton;
    @FXML private Button constructDatabaseButton;
    @FXML private Button fitButton;
    @FXML private Button originalSizeButton;
    @FXML private TextField watermarkTextField;
    @FXML private CheckBox clearLSBCheckBox;
    @FXML private Button processButton;

    private final SettingsManager settingsManager;
    private FileManagerService fileManagerService;
    private FilteredList<TreeItem<String>> filteredTreeItems;

    public FileManagerController(SettingsManager settingsManager) {
        this.settingsManager = settingsManager;
    }

    @FXML
    public void initialize() {
        if (settingsManager == null) {
            log.error("SettingsManager is null in FileManagerController");
            return;
        }
        initializeFileManagerService();
    }

    private void initializeFileManagerService() {
        if (fileManagerService != null) {
            return;
        }

        try {
            fileManagerService = new FileManagerService(settingsManager);
            setupEventHandlers();
            refreshTreeView();
        } catch (IOException e) {
            log.error("無法初始化FileManagerService", e);
            showAlert("錯誤", "無法初始化文件管理服務: " + e.getMessage());
        }
    }

    private void setupEventHandlers() {
        addButton.setOnAction(event -> addWatchedDirectory());
        removeButton.setOnAction(event -> removeWatchedDirectory());
        constructDatabaseButton.setOnAction(event -> constructDatabase());
        fitButton.setOnAction(event -> fitImage());
        originalSizeButton.setOnAction(event -> showOriginalSize());

        fileTreeView.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue != null) {
                updatePreview(newValue);
            }
        });

        fileTreeView.addEventHandler(TreeItem.branchExpandedEvent(), this::handleBranchExpanded);
        fileTreeView.addEventHandler(TreeItem.branchCollapsedEvent(), this::handleBranchCollapsed);

        searchField.textProperty().addListener((observable, oldValue, newValue) -> {
            if (filteredTreeItems != null) {
                filteredTreeItems.setPredicate(createFilterPredicate(newValue));
            }
        });

        processButton.setOnAction(event -> processSelectedImage());
    }

    private void processSelectedImage() {
        TreeItem<String> selectedItem = fileTreeView.getSelectionModel().getSelectedItem();
        if (selectedItem != null && selectedItem.getParent() != null) {
            String path = buildFullPath(selectedItem);
            File file = new File(path);
            if (file.isFile()) {
                try {
                    BufferedImage image = ImageIO.read(file);
                    if (!watermarkTextField.getText().isEmpty()) {
                        ImageProcessor.addWatermark(image, watermarkTextField.getText());
                    }
                    if (clearLSBCheckBox.isSelected()) {
                        ImageProcessor.clearMetadata(image);
                    }
                    File outputFile = new File(file.getParentFile(), "processed_" + file.getName());
                    ImageProcessor.saveImage(image, outputFile);
                    showAlert("成功", "圖像處理完成: " + outputFile.getName());
                    refreshTreeView();
                } catch (IOException e) {
                    log.error("處理圖像時發生錯誤", e);
                    showAlert("錯誤", "處理圖像時發生錯誤: " + e.getMessage());
                }
            }
        }
    }

    private void handleBranchExpanded(TreeItem.TreeModificationEvent<String> event) {
        TreeItem<String> source = event.getTreeItem();
        fileManagerService.setDirectoryExpanded(buildFullPath(source), true);
    }

    private void handleBranchCollapsed(TreeItem.TreeModificationEvent<String> event) {
        TreeItem<String> source = event.getTreeItem();
        fileManagerService.setDirectoryExpanded(buildFullPath(source), false);
    }

    private void addWatchedDirectory() {
        DirectoryChooser directoryChooser = new DirectoryChooser();
        directoryChooser.setTitle("選擇要監視的目錄");
        File selectedDirectory = directoryChooser.showDialog(fileTreeView.getScene().getWindow());

        if (selectedDirectory != null) {
            try {
                fileManagerService.addWatchedDirectory(selectedDirectory.getAbsolutePath());
                refreshTreeView();
            } catch (IOException e) {
                log.error("無法添加監視目錄", e);
                showAlert("錯誤", "無法添加監視目錄: " + e.getMessage());
            } catch (Exception e) {
                log.error("添加監視目錄時發生未知錯誤", e);
                showAlert("錯誤", "添加監視目錄時發生未知錯誤: " + e.getMessage());
            }
        }
    }

    private void refreshTreeView() {
        fileManagerService.getDirectoryTree().thenAccept(root -> {
            Platform.runLater(() -> {
                ObservableList<TreeItem<String>> observableList = FXCollections.observableArrayList(root.getChildren());
                filteredTreeItems = new FilteredList<>(observableList);
                TreeItem<String> filteredRoot = new TreeItem<>("監視的目錄");
                filteredRoot.setExpanded(true);
                filteredRoot.getChildren().addAll(filteredTreeItems);
                fileTreeView.setRoot(filteredRoot);
                log.info("已刷新檔案樹視圖");
            });
        });
    }

    private Predicate<TreeItem<String>> createFilterPredicate(String searchText) {
        return treeItem -> {
            if (searchText == null || searchText.isEmpty()) {
                return true;
            }
            return treeItem.getValue().toLowerCase().contains(searchText.toLowerCase());
        };
    }

    private void removeWatchedDirectory() {
        TreeItem<String> selectedItem = fileTreeView.getSelectionModel().getSelectedItem();
        if (selectedItem != null) {
            String path = buildFullPath(selectedItem);
            log.info("嘗試移除監視目錄: {}", path);
            if (path.isEmpty()) {
                // 如果路徑為空，說明選中的是根目錄 "監視的目錄"
                log.warn("無法移除根目錄");
                showAlert("警告", "無法移除根目錄。請選擇一個具體的監視目錄。");
                return;
            }
            fileManagerService.removeWatchedDirectory(path);
            refreshTreeView();
        } else {
            log.warn("嘗試移除監視目錄時未選中任何項目");
            showAlert("警告", "請選擇一個要移除的監視目錄。");
        }
    }

    private String buildFullPath(TreeItem<String> item) {
        if (item.getParent() == null || item.getParent().getValue().equals("監視的目錄")) {
            // 這是直接選中的監視目錄
            return item.getValue();
        }

        List<String> pathParts = new ArrayList<>();
        TreeItem<String> current = item;
        while (current != null && !current.getValue().equals("監視的目錄")) {
            pathParts.addFirst(current.getValue());
            current = current.getParent();
        }

        return String.join(File.separator, pathParts);
    }

    private void updatePreview(TreeItem<String> item) {
        String fullPath = buildFullPath(item);
        File file = new File(fullPath);
        if (file.isFile()) {
            try {
                Image image = new Image(file.toURI().toString());
                previewImageView.setImage(image);
                updateMetadataList(file);
            } catch (Exception e) {
                log.error("無法載入預覽圖片", e);
                previewImageView.setImage(null);
            }
        } else {
            previewImageView.setImage(null);
            metadataListView.getItems().clear();
        }
    }

    private void updateMetadataList(File file) {
        // TODO: 實現讀取和顯示元數據的功能
        metadataListView.getItems().clear();
        metadataListView.getItems().add("文件名: " + file.getName());
        metadataListView.getItems().add("大小: " + file.length() + " bytes");
        // 添加更多元數據...
    }


    private void constructDatabase() {
        // TODO: 實現構建數據庫功能
        showAlert("提示", "構建數據庫功能尚未實現");
    }

    private void fitImage() {
        if (previewImageView.getImage() != null) {
            previewImageView.setFitWidth(previewImageView.getParent().getBoundsInLocal().getWidth());
            previewImageView.setFitHeight(previewImageView.getParent().getBoundsInLocal().getHeight());
            previewImageView.setPreserveRatio(true);
        }
    }

    private void showOriginalSize() {
        if (previewImageView.getImage() != null) {
            previewImageView.setFitWidth(0);
            previewImageView.setFitHeight(0);
        }
    }

    private void showAlert(String title, String content) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }

    public void shutdown() {
        if (fileManagerService != null) {
            fileManagerService.shutdown();
        }
    }
}