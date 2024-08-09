package com.zxzinn.novelai.controller.filemanager;

import com.zxzinn.novelai.service.filemanager.FileManagerService;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeView;
import lombok.extern.log4j.Log4j2;

import java.io.File;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

@Log4j2
public class FileTreeController {
    private final FileManagerService fileManagerService;
    private final TreeView<String> fileTreeView;
    private FilteredList<TreeItem<String>> filteredTreeItems;

    public FileTreeController(FileManagerService fileManagerService, TreeView<String> fileTreeView) {
        this.fileManagerService = fileManagerService;
        this.fileTreeView = fileTreeView;
    }

    public void refreshTreeView() {
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

    public void setSearchFilter(String searchText) {
        if (filteredTreeItems != null) {
            filteredTreeItems.setPredicate(createFilterPredicate(searchText));
        }
    }

    private Predicate<TreeItem<String>> createFilterPredicate(String searchText) {
        return treeItem -> {
            if (searchText == null || searchText.isEmpty()) {
                return true;
            }
            return treeItem.getValue().toLowerCase().contains(searchText.toLowerCase());
        };
    }

    public void handleBranchExpanded(TreeItem.TreeModificationEvent<String> event) {
        TreeItem<String> source = event.getTreeItem();
        fileManagerService.setDirectoryExpanded(buildFullPath(source), true);
    }

    public void handleBranchCollapsed(TreeItem.TreeModificationEvent<String> event) {
        TreeItem<String> source = event.getTreeItem();
        fileManagerService.setDirectoryExpanded(buildFullPath(source), false);
    }

    String buildFullPath(TreeItem<String> item) {
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
}