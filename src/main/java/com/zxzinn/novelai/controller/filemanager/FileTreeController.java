package com.zxzinn.novelai.controller.filemanager;

import com.zxzinn.novelai.service.filemanager.FileManagerService;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.event.EventType;
import javafx.scene.control.SelectionMode;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeView;
import lombok.Setter;
import lombok.extern.log4j.Log4j2;

import java.io.File;
import java.nio.file.Paths;
import java.nio.file.StandardWatchEventKinds;
import java.nio.file.WatchEvent;
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
            pathParts.add(0, current.getValue());
            current = current.getParent();
        }

        if (pathParts.isEmpty()) {
            log.warn("無法構建完整路徑：路徑為空");
            return "";
        }

        String watchedDir = pathParts.get(0);
        String watchedDirFullPath = fileManagerService.getWatchedDirectoryFullPath(watchedDir);

        if (watchedDirFullPath == null) {
            log.warn("無法找到監視目錄的完整路徑: {}", watchedDir);
            return String.join(File.separator, pathParts);
        }

        pathParts.remove(0);
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

    public void updateTreeItem(String path, WatchEvent.Kind<?> eventKind) {
        Platform.runLater(() -> {
            TreeItem<String> root = fileTreeView.getRoot();
            if (root != null) {
                updateTreeItemRecursive(root, path, eventKind);
            } else {
                log.warn("文件樹根節點為空");
            }
        });
    }

    private void updateTreeItemRecursive(TreeItem<String> item, String path, WatchEvent.Kind<?> eventKind) {
        String itemPath = buildFullPath(item);
        if (itemPath.isEmpty()) {
            return;
        }

        if (path.startsWith(itemPath)) {
            if (path.equals(itemPath)) {
                // 找到了匹配的項目
                TreeItem<String> parent = item.getParent();
                if (parent != null) {
                    if (eventKind == StandardWatchEventKinds.ENTRY_CREATE || eventKind == StandardWatchEventKinds.ENTRY_DELETE) {
                        // 處理子項修改事件
                        refreshChildren(item);
                    } else if (eventKind == StandardWatchEventKinds.ENTRY_MODIFY) {
                        // 處理值變更事件
                        item.setValue(new File(path).getName());
                    }
                }
            } else {
                // 繼續在子項中搜索
                for (TreeItem<String> child : item.getChildren()) {
                    updateTreeItemRecursive(child, path, eventKind);
                }
            }
        }
    }


    private void refreshChildren(TreeItem<String> item) {
        File file = new File(buildFullPath(item));
        if (file.isDirectory()) {
            item.getChildren().clear();
            File[] children = file.listFiles();
            if (children != null) {
                for (File child : children) {
                    item.getChildren().add(new TreeItem<>(child.getName()));
                }
            }
        }
    }
}