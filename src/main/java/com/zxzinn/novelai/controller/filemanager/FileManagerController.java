package com.zxzinn.novelai.controller.filemanager;

import com.google.inject.Inject;
import com.zxzinn.novelai.component.ImagePreviewPane;
import com.zxzinn.novelai.service.filemanager.*;
import com.zxzinn.novelai.service.ui.AlertService;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.StackPane;
import lombok.extern.log4j.Log4j2;

import java.io.File;
import java.nio.file.WatchEvent;

@Log4j2
public class FileManagerController {
    @FXML private TreeView<String> fileTreeView;
    @FXML private Button selectAllButton;
    @FXML protected StackPane previewContainer;
    @FXML private TextArea metadataTextArea;
    @FXML private Button addButton;
    @FXML private Button removeButton;
    @FXML private Button clearMetadataButton;
    @FXML private Button mergeTxtButton;
    @FXML private ProgressBar progressBar;
    @FXML private Label progressLabel;

    private final FileManagerService fileManagerService;
    private final FilePreviewService filePreviewService;
    private final MetadataService metadataService;
    private final AlertService alertService;
    private final FileTreeController fileTreeController;
    private final FileOperationService fileOperationService;
    protected ImagePreviewPane imagePreviewPane;

    @Inject
    public FileManagerController(FileManagerService fileManagerService,
                                 FilePreviewService filePreviewService,
                                 MetadataService metadataService,
                                 AlertService alertService,
                                 FileOperationService fileOperationService) {
        this.fileManagerService = fileManagerService;
        this.filePreviewService = filePreviewService;
        this.metadataService = metadataService;
        this.alertService = alertService;
        this.fileOperationService = fileOperationService;
        this.fileTreeController = new FileTreeController(fileManagerService);
    }

    @FXML
    public void initialize() {
        imagePreviewPane = new ImagePreviewPane(filePreviewService);
        previewContainer.getChildren().add(imagePreviewPane);
        setupEventHandlers();
        fileTreeController.setFileTreeView(fileTreeView);
        fileTreeController.refreshTreeView();
        fileManagerService.setFileChangeListener(this::handleFileChange);
    }

    private void handleFileChange(String path, WatchEvent.Kind<?> kind) {
        fileTreeController.updateTreeItem(path, kind);
        updatePreview(fileTreeView.getSelectionModel().getSelectedItem());
    }

    private void setupEventHandlers() {
        addButton.setOnAction(event -> fileOperationService.addWatchedDirectory(fileTreeView, fileTreeController));
        removeButton.setOnAction(event -> fileOperationService.removeWatchedDirectory(fileTreeView, fileTreeController, alertService));
        fileTreeView.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue != null) {
                updatePreview(newValue);
            }
        });
        selectAllButton.setOnAction(event -> fileTreeController.selectAllInDirectory(fileTreeView.getSelectionModel().getSelectedItem()));
        clearMetadataButton.setOnAction(event -> fileOperationService.clearMetadataForSelectedFiles(fileTreeView, fileTreeController, alertService, progressBar, progressLabel));
        mergeTxtButton.setOnAction(event -> fileOperationService.mergeSelectedTxtFiles(fileTreeView, fileTreeController, alertService));
    }

    private void updatePreview(TreeItem<String> item) {
        String fullPath = fileTreeController.buildFullPath(item);
        File file = new File(fullPath);
        imagePreviewPane.updatePreview(file);
        metadataService.updateMetadataList(file, metadataTextArea);
    }

    public void shutdown() {
        fileManagerService.shutdown();
        metadataService.shutdown();
    }
}