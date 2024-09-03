package com.zxzinn.novelai.service.filemanager;

import com.google.inject.Inject;
import com.zxzinn.novelai.controller.filemanager.FileTreeController;
import com.zxzinn.novelai.service.ui.AlertService;
import com.zxzinn.novelai.utils.common.TxtProcessor;
import com.zxzinn.novelai.utils.image.ImageUtils;
import javafx.application.Platform;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.Label;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeView;
import javafx.stage.FileChooser;
import lombok.extern.log4j.Log4j2;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

@Log4j2
public class FileOperationService {
    private final FileManagerService fileManagerService;
    private final ExecutorService executorService;
    private final AlertService alertService;

    @Inject
    public FileOperationService(FileManagerService fileManagerService, AlertService alertService) {
        this.fileManagerService = fileManagerService;
        this.alertService = alertService;
        this.executorService = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());
    }

    public void addWatchedDirectory(TreeView<String> fileTreeView, FileTreeController fileTreeController) {
        File selectedDirectory = fileManagerService.chooseDirectory(fileTreeView.getScene().getWindow());
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

    public void removeWatchedDirectory(TreeView<String> fileTreeView, FileTreeController fileTreeController, AlertService alertService) {
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

    public void clearMetadataForSelectedFiles(TreeView<String> fileTreeView, FileTreeController fileTreeController, AlertService alertService, ProgressBar progressBar, Label progressLabel) {
        List<File> selectedFiles = getSelectedImageFiles(fileTreeView, fileTreeController);
        if (selectedFiles.isEmpty()) {
            alertService.showAlert("警告", "請選擇要清除元數據的 PNG 文件。");
            return;
        }

        File cleanedDir = new File("cleaned");
        if (!cleanedDir.exists() && !cleanedDir.mkdir()) {
            alertService.showAlert("錯誤", "無法創建 cleaned 目錄");
            return;
        }

        AtomicInteger processedCount = new AtomicInteger(0);
        int totalFiles = selectedFiles.size();

        for (File file : selectedFiles) {
            if (!file.getName().toLowerCase().endsWith(".png")) {
                continue;
            }
            CompletableFuture.runAsync(() -> {
                try {
                    processMetadataCleaner(file, cleanedDir);
                    int completed = processedCount.incrementAndGet();
                    updateProgressOnUI(completed, totalFiles, progressBar, progressLabel);
                    if (completed == totalFiles) {
                        onProcessingComplete(selectedFiles, alertService, fileTreeController);
                    }
                } catch (Exception e) {
                    log.error("處理文件時發生錯誤: {}", file.getName(), e);
                    Platform.runLater(() -> alertService.showAlert("錯誤", "處理文件時發生錯誤: " + e.getMessage()));
                }
            }, executorService);
        }
    }

    private void processMetadataCleaner(File file, File cleanedDir) throws IOException {
        BufferedImage image = ImageIO.read(file);
        if (image == null) {
            throw new IOException("無法讀取圖像文件: " + file.getName());
        }

        ImageUtils.clearMetadata(image);

        File outputFile = new File(cleanedDir, file.getName());
        ImageUtils.saveImage(image, outputFile);
    }

    private void updateProgressOnUI(int completed, int total, ProgressBar progressBar, Label progressLabel) {
        Platform.runLater(() -> {
            double progress = (double) completed / total;
            progressBar.setProgress(progress);
            int percentage = (int) (progress * 100);
            progressLabel.setText(String.format("處理中... %d%%", percentage));
        });
    }

    private void onProcessingComplete(List<File> processedFiles, AlertService alertService, FileTreeController fileTreeController) {
        Platform.runLater(() -> {
            alertService.showAlert("成功", String.format("已處理 %d 個文件的元數據清除。", processedFiles.size()));
            refreshProcessedDirectories();
            fileTreeController.refreshTreeView();
        });
    }

    private void refreshProcessedDirectories() {
        File cleanedDir = new File("cleaned");
        if (cleanedDir.exists()) {
            try {
                fileManagerService.addWatchedDirectory(cleanedDir.getAbsolutePath());
            } catch (IOException e) {
                log.error("無法添加 cleaned 目錄到監視列表", e);
            }
        }
    }

    public void mergeSelectedTxtFiles(TreeView<String> fileTreeView, FileTreeController fileTreeController, AlertService alertService) {
        List<File> selectedFiles = getSelectedTxtFiles(fileTreeView, fileTreeController);
        if (selectedFiles.isEmpty()) {
            alertService.showAlert("警告", "請選擇要合併的txt文件。");
            return;
        }

        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("選擇輸出文件");
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("TXT Files", "*.txt"));
        File outputFile = fileChooser.showSaveDialog(fileTreeView.getScene().getWindow());

        if (outputFile != null) {
            try {
                TxtProcessor.mergeAndProcessTxtFiles(selectedFiles, outputFile);
                alertService.showAlert("成功", "txt文件已成功合併和處理。");
            } catch (IOException e) {
                log.error("合併txt文件時發生錯誤", e);
                alertService.showAlert("錯誤", "合併txt文件時發生錯誤: " + e.getMessage());
            }
        }
    }

    private List<File> getSelectedImageFiles(TreeView<String> fileTreeView, FileTreeController fileTreeController) {
        return fileTreeView.getSelectionModel().getSelectedItems().stream()
                .map(item -> new File(fileTreeController.buildFullPath(item)))
                .filter(this::isImageFile)
                .collect(Collectors.toList());
    }

    private List<File> getSelectedTxtFiles(TreeView<String> fileTreeView, FileTreeController fileTreeController) {
        return fileTreeView.getSelectionModel().getSelectedItems().stream()
                .map(item -> new File(fileTreeController.buildFullPath(item)))
                .filter(this::isTxtFile)
                .collect(Collectors.toList());
    }

    private boolean isImageFile(File file) {
        if (!file.isFile()) return false;
        String name = file.getName().toLowerCase();
        return name.endsWith(".png") || name.endsWith(".jpg") || name.endsWith(".jpeg") || name.endsWith(".gif");
    }

    private boolean isTxtFile(File file) {
        if (!file.isFile()) return false;
        return file.getName().toLowerCase().endsWith(".txt");
    }

    public void shutdown() {
        executorService.shutdown();
    }
}