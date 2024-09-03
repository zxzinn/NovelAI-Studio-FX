package com.zxzinn.novelai.controller.filemanager;

import com.google.inject.Inject;
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
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.nio.file.Paths;
import java.nio.file.StandardWatchEventKinds;
import java.nio.file.WatchEvent;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Predicate;

@Log4j2
public class FileTreeController {
    private final FileManagerService fileManagerService;
    @Setter private TreeView<String> fileTreeView;
    private FilteredList<TreeItem<String>> filteredTreeItems;
    private final AtomicBoolean isRefreshing = new AtomicBoolean(false);
    private final Map<String, TreeItem<String>> pathToItemMap = new HashMap<>();
    private final TreeItemFactory treeItemFactory;

    @Inject
    public FileTreeController(FileManagerService fileManagerService) {
        this.fileManagerService = fileManagerService;
        this.treeItemFactory = new TreeItemFactory();
    }

    public void refreshTreeView() {
        if (fileTreeView == null) {
            log.error("fileTreeView is null in FileTreeController");
            return;
        }
        if (isRefreshing.compareAndSet(false, true)) {
            fileManagerService.getDirectoryTree().thenAccept(root -> Platform.runLater(() -> {
                try {
                    ObservableList<TreeItem<String>> observableList = FXCollections.observableArrayList(root.getChildren());
                    filteredTreeItems = new FilteredList<>(observableList);
                    TreeItem<String> filteredRoot = new TreeItem<>("監視的目錄");
                    filteredRoot.setExpanded(true);
                    filteredRoot.getChildren().addAll(filteredTreeItems);
                    fileTreeView.setRoot(filteredRoot);
                    fileTreeView.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
                    updatePathToItemMap(filteredRoot);
                    log.info("已刷新檔案樹視圖");
                } catch (Exception e) {
                    log.error("刷新檔案樹時發生錯誤", e);
                } finally {
                    isRefreshing.set(false);
                }
            }));
        } else {
            log.debug("刷新操作已在進行中，跳過此次刷新請求");
        }
    }

    private void updatePathToItemMap(TreeItem<String> item) {
        String path = buildFullPath(item);
        pathToItemMap.put(path, item);
        for (TreeItem<String> child : item.getChildren()) {
            updatePathToItemMap(child);
        }
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

    public String buildFullPath(TreeItem<String> item) {
        if (item == null) {
            return "";
        }

        List<String> pathParts = new ArrayList<>();
        TreeItem<String> current = item;
        while (current != null && !current.getValue().equals("監視的目錄")) {
            pathParts.addFirst(current.getValue());
            current = current.getParent();
        }

        if (pathParts.isEmpty()) {
            return "";
        }

        String watchedDir = pathParts.getFirst();
        String watchedDirFullPath = fileManagerService.getWatchedDirectoryFullPath(watchedDir);

        if (watchedDirFullPath == null) {
            return String.join(File.separator, pathParts);
        }

        pathParts.removeFirst();
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
            try {
                if (eventKind == StandardWatchEventKinds.ENTRY_DELETE) {
                    removeDeletedItem(path);
                } else if (eventKind == StandardWatchEventKinds.ENTRY_CREATE) {
                    addNewItem(path);
                } else if (eventKind == StandardWatchEventKinds.ENTRY_MODIFY) {
                    refreshTreeItem(path);
                }
            } catch (Exception e) {
                log.error("更新樹項目時發生錯誤", e);
            }
        });
    }

    private void refreshTreeItem(String path) {
        TreeItem<String> item = pathToItemMap.get(path);
        if (item != null) {
            File file = new File(path);
            item.getChildren().clear();
            loadChildrenInBatches(item, file);
        }
    }

    private void removeDeletedItem(String path) {
        TreeItem<String> item = pathToItemMap.remove(path);
        if (item != null) {
            TreeItem<String> parent = item.getParent();
            if (parent != null) {
                parent.getChildren().remove(item);
            }
        } else {
            log.warn("無法找到要刪除的項目: {}", path);
        }
    }

    private void addNewItem(String path) {
        File file = new File(path);
        String parentPath = file.getParent();
        TreeItem<String> parentItem = pathToItemMap.get(parentPath);
        if (parentItem != null) {
            TreeItem<String> newItem = treeItemFactory.createTreeItem(file);
            parentItem.getChildren().add(newItem);
            pathToItemMap.put(path, newItem);
            if (file.isDirectory()) {
                newItem.setExpanded(true);
                loadChildrenInBatches(newItem, file);
            }
        } else {
            log.warn("無法找到父項目來添加新項目: {}", path);
        }
    }

    private void loadChildrenInBatches(TreeItem<String> parentItem, File parentFile) {
        Collection<File> children = FileUtils.listFiles(parentFile, null, false);
        List<File> childList = new ArrayList<>(children);
        Collections.sort(childList);
        for (File child : childList) {
            TreeItem<String> childItem = treeItemFactory.createTreeItem(child);
            parentItem.getChildren().add(childItem);
            String childPath = child.getAbsolutePath();
            pathToItemMap.put(childPath, childItem);
            if (child.isDirectory()) {
                loadChildrenInBatches(childItem, child);
            }
        }
    }
}