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
import javafx.scene.layout.StackPane;
import javafx.scene.web.WebView;
import javafx.stage.DirectoryChooser;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.io.FilenameUtils;
import org.apache.tika.Tika;
import org.apache.tika.metadata.Metadata;
import org.apache.tika.parser.AutoDetectParser;
import org.apache.tika.parser.ParseContext;
import org.apache.tika.sax.BodyContentHandler;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;


@Log4j2
public class FileManagerController {
    @FXML private TreeView<String> fileTreeView;
    @FXML private StackPane previewPane;
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
    private Tika tika;
    private WebView webView;

    public FileManagerController(SettingsManager settingsManager) {
        this.settingsManager = settingsManager;
        this.tika = new Tika();
    }
    @FXML
    public void initialize() {
        if (settingsManager == null) {
            log.error("SettingsManager is null in FileManagerController");
            return;
        }
        initializeFileManagerService();
        setupEventHandlers();
        webView = new WebView();
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
        List<String> pathParts = new ArrayList<>();
        TreeItem<String> current = item;
        while (current != null && !current.getValue().equals("監視的目錄")) {
            pathParts.add(0, current.getValue());
            current = current.getParent();
        }

        if (current == null || current.getParent() != null) {
            // 這種情況不應該發生，但如果發生了，我們記錄一個錯誤
            log.error("無法找到根目錄，路徑可能不完整: {}", String.join(File.separator, pathParts));
            return String.join(File.separator, pathParts);
        }

        // 現在我們在根節點（"監視的目錄"）
        // 我們需要找到這個特定目錄的完整路徑
        String watchedDir = pathParts.remove(0); // 移除並獲取監視目錄名稱
        String watchedDirFullPath = findWatchedDirectoryPath(watchedDir);

        if (watchedDirFullPath == null) {
            log.error("無法找到監視目錄的完整路徑: {}", watchedDir);
            return String.join(File.separator, pathParts);
        }

        // 組合完整路徑
        return Paths.get(watchedDirFullPath, String.join(File.separator, pathParts)).toString();
    }

    private String findWatchedDirectoryPath(String dirName) {
        // 這個方法需要從 FileManagerService 獲取監視目錄的完整路徑
        // 你可能需要在 FileManagerService 中添加一個方法來獲取這個信息
        // 這裡是一個示例實現
        return fileManagerService.getWatchedDirectoryFullPath(dirName);
    }

    private void updatePreview(TreeItem<String> item) {
        String fullPath = buildFullPath(item);
        File file = new File(fullPath);
        if (file.isFile()) {
            try {
                String mimeType = tika.detect(file);
                if (mimeType.startsWith("image/")) {
                    displayImage(file);
                } else if (mimeType.startsWith("text/") || mimeType.equals("application/pdf")) {
                    displayText(file);
                } else {
                    displayUnsupportedFormat(mimeType);
                }
                updateMetadataList(file);
            } catch (Exception e) {
                log.error("無法載入預覽", e);
                displayError("無法載入預覽：" + e.getMessage());
            }
        } else {
            clearPreview();
        }
    }

    private void displayImage(File file) {
        Image image = new Image(file.toURI().toString());
        ImageView imageView = new ImageView(image);
        imageView.setPreserveRatio(true);
        imageView.fitWidthProperty().bind(previewPane.widthProperty());
        imageView.fitHeightProperty().bind(previewPane.heightProperty());
        Platform.runLater(() -> previewPane.getChildren().setAll(imageView));
    }

    private void displayText(File file) throws IOException {
        String content = Files.readString(file.toPath());
        String extension = FilenameUtils.getExtension(file.getName());
        String mimeType = tika.detect(file);

        Platform.runLater(() -> {
            webView.getEngine().loadContent(formatContent(content, extension, mimeType), "text/html");
            previewPane.getChildren().setAll(webView);
        });
    }

    private String formatContent(String content, String extension, String mimeType) {
        String highlightLanguage = getHighlightLanguage(extension, mimeType);
        String escapedContent = escapeHtml(content);

        return """
            <!DOCTYPE html>
            <html>
            <head>
                <link rel="stylesheet" href="https://cdnjs.cloudflare.com/ajax/libs/highlight.js/11.8.0/styles/default.min.css">
                <script src="https://cdnjs.cloudflare.com/ajax/libs/highlight.js/11.8.0/highlight.min.js"></script>
                <style>
                    body {
                        font-family: Arial, sans-serif;
                        line-height: 1.6;
                        padding: 20px;
                        background-color: #f0f0f0;
                    }
                    pre {
                        background-color: #ffffff;
                        border: 1px solid #ddd;
                        border-radius: 4px;
                        padding: 16px;
                        font-size: 14px;
                    }
                    code {
                        font-family: 'Courier New', Courier, monospace;
                    }
                </style>
            </head>
            <body>
                <pre><code class="%s">%s</code></pre>
                <script>hljs.highlightAll();</script>
            </body>
            </html>
            """.formatted(highlightLanguage, escapedContent);
    }

    private String getHighlightLanguage(String extension, String mimeType) {
        return switch (extension.toLowerCase()) {
            case "java" -> "java";
            case "py" -> "python";
            case "js" -> "javascript";
            case "html" -> "html";
            case "css" -> "css";
            case "json" -> "json";
            case "xml" -> "xml";
            default -> mimeType.startsWith("text/") ? "plaintext" : "";
        };
    }

    private String escapeHtml(String content) {
        return content.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#39;");
    }

    private void displayUnsupportedFormat(String mimeType) {
        Label label = new Label("不支援的文件格式：" + mimeType);
        Platform.runLater(() -> previewPane.getChildren().setAll(label));
    }

    private void displayError(String message) {
        Label label = new Label(message);
        Platform.runLater(() -> previewPane.getChildren().setAll(label));
    }

    private void clearPreview() {
        Platform.runLater(() -> previewPane.getChildren().clear());
    }

    private void updateMetadataList(File file) {
        metadataListView.getItems().clear();
        try {
            BodyContentHandler handler = new BodyContentHandler();
            Metadata metadata = new Metadata();
            FileInputStream inputstream = new FileInputStream(file);
            ParseContext pcontext = new ParseContext();

            AutoDetectParser parser = new AutoDetectParser();
            parser.parse(inputstream, handler, metadata, pcontext);

            for (String name : metadata.names()) {
                metadataListView.getItems().add(name + ": " + metadata.get(name));
            }
        } catch (Exception e) {
            log.error("無法讀取檔案元數據", e);
            metadataListView.getItems().add("無法讀取元數據：" + e.getMessage());
        }
    }

    private String getFileExtension(File file) {
        String name = file.getName();
        int lastIndexOf = name.lastIndexOf(".");
        if (lastIndexOf == -1) {
            return ""; // 沒有副檔名
        }
        return name.substring(lastIndexOf + 1);
    }

    private boolean isImageFile(String fileExtension) {
        String[] imageExtensions = {"jpg", "jpeg", "png", "gif", "bmp"};
        for (String ext : imageExtensions) {
            if (ext.equalsIgnoreCase(fileExtension)) {
                return true;
            }
        }
        return false;
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