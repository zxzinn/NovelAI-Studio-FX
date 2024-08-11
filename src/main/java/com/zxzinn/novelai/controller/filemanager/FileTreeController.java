package com.zxzinn.novelai.controller.filemanager;

import com.zxzinn.novelai.service.filemanager.FileManagerService;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.scene.control.SelectionMode;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeView;
import lombok.Setter;
import lombok.extern.log4j.Log4j2;

import java.io.File;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

@Log4j2
public class FileTreeController {
    private final FileManagerService fileManagerService;
    @Setter
    private TreeView<String> fileTreeView;
    private FilteredList<TreeItem<String>> filteredTreeItems;

    public FileTreeController(FileManagerService fileManagerService) {
        this.fileManagerService = fileManagerService;
    }

    public void initialize() {
        refreshTreeView();
    }

    public void refreshTreeView() {
        if (fileTreeView == null) {
            log.error("fileTreeView is null in FileTreeController");
            return;
        }
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

    public String buildFullPath(TreeItem<String> item) {
        List<String> pathParts = new ArrayList<>();
        TreeItem<String> current = item;
        while (current != null && !current.getValue().equals("監視的目錄")) {
            pathParts.addFirst(current.getValue());
            current = current.getParent();
        }

        if (current == null || current.getParent() != null) {
            log.error("無法找到根目錄，路徑可能不完整: {}", String.join(File.separator, pathParts));
            return String.join(File.separator, pathParts);
        }

        String watchedDir = pathParts.removeFirst();
        String watchedDirFullPath = fileManagerService.getWatchedDirectoryFullPath(watchedDir);

        if (watchedDirFullPath == null) {
            log.error("無法找到監視目錄的完整路徑: {}", watchedDir);
            return String.join(File.separator, pathParts);
        }

        return Paths.get(watchedDirFullPath, String.join(File.separator, pathParts)).toString();
    }

    public void selectAllInDirectory(TreeItem<String> directoryItem) {
        if (directoryItem != null && directoryItem.isExpanded()) {
            ObservableList<TreeItem<String>> children = directoryItem.getChildren();
            fileTreeView.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
            int startIndex = fileTreeView.getRow(directoryItem) + 1;
            int endIndex = startIndex + children.size();
            fileTreeView.getSelectionModel().selectRange(startIndex, endIndex);
        }
    }
}