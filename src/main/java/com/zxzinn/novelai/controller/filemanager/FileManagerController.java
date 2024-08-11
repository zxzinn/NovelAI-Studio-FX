package com.zxzinn.novelai.controller.filemanager;

import com.zxzinn.novelai.component.PreviewPane;
import com.zxzinn.novelai.service.filemanager.*;
import com.zxzinn.novelai.service.ui.AlertService;
import com.zxzinn.novelai.utils.common.SettingsManager;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.image.ImageView;
import javafx.scene.input.ScrollEvent;
import javafx.scene.layout.StackPane;
import javafx.scene.web.WebView;
import javafx.stage.DirectoryChooser;
import lombok.extern.log4j.Log4j2;

import java.io.File;
import java.io.IOException;
import java.nio.file.StandardWatchEventKinds;
import java.nio.file.WatchEvent;
import java.util.List;
import java.util.stream.Collectors;

@Log4j2
public class FileManagerController {
    @FXML private TreeView<String> fileTreeView;
    @FXML private Button selectAllButton;
    @FXML protected StackPane previewContainer;
    @FXML private ListView<String> metadataListView;
    @FXML private TextField searchField;
    @FXML private Button addButton;
    @FXML private Button removeButton;
    @FXML private Button constructDatabaseButton;
    @FXML private TextField watermarkTextField;
    @FXML private Button clearMetadataButton;
    @FXML private Button processButton;
    @FXML private ProgressBar progressBar;
    @FXML private Label progressLabel;

    private final SettingsManager settingsManager;
    private final FileManagerService fileManagerService;
    private final FilePreviewService filePreviewService;
    private final MetadataService metadataService;
    private final ImageProcessingService imageProcessingService;
    private final AlertService alertService;
    private final FileTreeController fileTreeController;
    protected PreviewPane previewPane;

    public FileManagerController(SettingsManager settingsManager,
                                 FileManagerService fileManagerService,
                                 FilePreviewService filePreviewService,
                                 MetadataService metadataService,
                                 ImageProcessingService imageProcessingService,
                                 AlertService alertService) {
        this.settingsManager = settingsManager;
        this.fileManagerService = fileManagerService;
        this.filePreviewService = filePreviewService;
        this.metadataService = metadataService;
        this.imageProcessingService = imageProcessingService;
        this.alertService = alertService;
        this.fileTreeController = new FileTreeController(fileManagerService);
    }

    @FXML
    public void initialize() {
        previewPane = new PreviewPane(filePreviewService);
        previewContainer.getChildren().add(previewPane);
        setupEventHandlers();
        fileTreeController.setFileTreeView(fileTreeView);
        fileTreeController.refreshTreeView();

        // 設置檔案系統變化的監聽器
        fileManagerService.setFileChangeListener(this::handleFileChange);
    }

    private void handleFileChange(String path, WatchEvent.Kind<?> kind) {
        fileTreeController.updateTreeItem(path, kind);
        // 更新預覽或元數據
        updatePreview(fileTreeView.getSelectionModel().getSelectedItem());
    }

    private void setupEventHandlers() {
        addButton.setOnAction(event -> addWatchedDirectory());
        removeButton.setOnAction(event -> removeWatchedDirectory());
        constructDatabaseButton.setOnAction(event -> constructDatabase());

        fileTreeView.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue != null) {
                updatePreview(newValue);
            }
        });

        fileTreeView.addEventHandler(TreeItem.branchExpandedEvent(), fileTreeController::handleBranchExpanded);
        fileTreeView.addEventHandler(TreeItem.branchCollapsedEvent(), fileTreeController::handleBranchCollapsed);

        searchField.textProperty().addListener((observable, oldValue, newValue) ->
                fileTreeController.setSearchFilter(newValue));

        selectAllButton.setOnAction(event -> selectAllInSelectedDirectory());
        clearMetadataButton.setOnAction(event -> clearMetadataForSelectedFiles());
    }

    private void clearMetadataForSelectedFiles() {
        List<File> selectedFiles = getSelectedImageFiles();
        if (selectedFiles.isEmpty()) {
            alertService.showAlert("警告", "請選擇要清除元數據的圖像文件。");
            return;
        }

        Task<Void> task = imageProcessingService.clearMetadataForFiles(selectedFiles);
        progressBar.progressProperty().bind(task.progressProperty());

        task.setOnSucceeded(e -> {
            alertService.showAlert("成功", String.format("已處理 %d 個文件的元數據清除。", selectedFiles.size()));
            updatePreview(fileTreeView.getSelectionModel().getSelectedItem());
            fileTreeController.refreshTreeView();
            progressBar.progressProperty().unbind();
            progressBar.setProgress(0);
        });

        task.setOnFailed(e -> {
            alertService.showAlert("錯誤", "處理過程中發生錯誤。");
            progressBar.progressProperty().unbind();
            progressBar.setProgress(0);
        });

        new Thread(task).start();
    }

    private void processSelectedImages() {
        List<File> selectedFiles = getSelectedImageFiles();
        if (selectedFiles.isEmpty()) {
            alertService.showAlert("警告", "請選擇要處理的圖像文件。");
            return;
        }

        String watermarkText = watermarkTextField.getText();
        Task<Void> task = imageProcessingService.processImages(selectedFiles, watermarkText, true);
        progressBar.progressProperty().bind(task.progressProperty());

        task.setOnSucceeded(e -> {
            alertService.showAlert("成功", String.format("已處理 %d 個文件。", selectedFiles.size()));
            fileTreeController.refreshTreeView();
            progressBar.setProgress(0);
            progressLabel.setText("處理完成");
        });

        task.setOnFailed(e -> {
            alertService.showAlert("錯誤", "處理過程中發生錯誤。");
            progressBar.setProgress(0);
            progressLabel.setText("處理失敗");
        });

        // 在任務執行過程中更新標籤
        task.progressProperty().addListener((observable, oldValue, newValue) -> {
            double progress = newValue.doubleValue();
            int percentage = (int) (progress * 100);
            progressLabel.setText(String.format("處理中... %d%%", percentage));
        });

        new Thread(task).start();
    }

    private List<File> getSelectedImageFiles() {
        return fileTreeView.getSelectionModel().getSelectedItems().stream()
                .map(item -> new File(fileTreeController.buildFullPath(item)))
                .filter(this::isImageFile)
                .collect(Collectors.toList());
    }

    private boolean isImageFile(File file) {
        if (!file.isFile()) return false;
        String name = file.getName().toLowerCase();
        return name.endsWith(".png") || name.endsWith(".jpg") || name.endsWith(".jpeg") || name.endsWith(".gif");
    }

    private void selectAllInSelectedDirectory() {
        TreeItem<String> selectedItem = fileTreeView.getSelectionModel().getSelectedItem();
        if (selectedItem != null) {
            fileTreeController.selectAllInDirectory(selectedItem);
        }
    }

    private void addWatchedDirectory() {
        DirectoryChooser directoryChooser = new DirectoryChooser();
        directoryChooser.setTitle("選擇要監視的目錄");
        File selectedDirectory = directoryChooser.showDialog(fileTreeView.getScene().getWindow());

        if (selectedDirectory != null) {
            try {
                fileManagerService.addWatchedDirectory(selectedDirectory.getAbsolutePath());
                fileTreeController.refreshTreeView();
            } catch (IOException e) {
                log.error("無法添加監視目錄", e);
                alertService.showAlert("錯誤", "無法添加監視目錄: " + e.getMessage());
            }
        }
    }

    private void removeWatchedDirectory() {
        TreeItem<String> selectedItem = fileTreeView.getSelectionModel().getSelectedItem();
        if (selectedItem != null) {
            String path = fileTreeController.buildFullPath(selectedItem);
            if (path.isEmpty()) {
                alertService.showAlert("警告", "無法移除根目錄。請選擇一個具體的監視目錄。");
                return;
            }
            fileManagerService.removeWatchedDirectory(path);
            fileTreeController.refreshTreeView();
        } else {
            alertService.showAlert("警告", "請選擇一個要移除的監視目錄。");
        }
    }

    private void updatePreview(TreeItem<String> item) {
        String fullPath = fileTreeController.buildFullPath(item);
        File file = new File(fullPath);
        previewPane.updatePreview(file);
        updateMetadataList(file);
    }

    private void updateMetadataList(File file) {
        if (file != null) {
            metadataListView.getItems().setAll(metadataService.getMetadata(file));
        } else {
            metadataListView.getItems().clear();
        }
    }


    private void constructDatabase() {
        // TODO: 實現構建數據庫功能
        alertService.showAlert("提示", "構建數據庫功能尚未實現");
    }

    public void shutdown() {
        fileManagerService.shutdown();
    }
}