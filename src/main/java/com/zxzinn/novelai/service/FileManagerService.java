package com.zxzinn.novelai.service;

import com.zxzinn.novelai.utils.SettingsManager;
import javafx.application.Platform;
import javafx.scene.control.TreeItem;
import lombok.extern.log4j.Log4j2;
import org.kordamp.ikonli.fontawesome5.FontAwesomeSolid;
import org.kordamp.ikonli.javafx.FontIcon;

import java.io.File;
import java.io.IOException;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.*;

@Log4j2
public class FileManagerService {
    private static final String WATCHED_DIRECTORIES_KEY = "watchedDirectories";
    private static final String EXPANDED_PREFIX = "expanded_";
    private static final int BATCH_SIZE = 100;

    private final Set<Path> watchedDirectories;
    private final Map<WatchKey, Path> watchKeyToPath;
    private final WatchService watchService;
    private final SettingsManager settingsManager;
    private final ExecutorService executorService;
    private final ScheduledExecutorService scheduledExecutorService;

    public FileManagerService(SettingsManager settingsManager) throws IOException {
        this.watchedDirectories = ConcurrentHashMap.newKeySet();
        this.watchKeyToPath = new ConcurrentHashMap<>();
        this.watchService = FileSystems.getDefault().newWatchService();
        this.settingsManager = settingsManager;
        this.executorService = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());
        this.scheduledExecutorService = Executors.newSingleThreadScheduledExecutor();

        loadWatchedDirectories();
        startWatchService();
    }

    private void loadWatchedDirectories() {
        List<String> savedDirectories = settingsManager.getStringList(WATCHED_DIRECTORIES_KEY, new ArrayList<>());
        for (String dir : savedDirectories) {
            try {
                addWatchedDirectory(dir);
            } catch (IOException e) {
                log.error("無法加載已保存的監視目錄: {}", dir, e);
            }
        }
    }

    private void startWatchService() {
        scheduledExecutorService.scheduleWithFixedDelay(this::processWatchEvents, 0, 1, TimeUnit.SECONDS);
    }

    private void processWatchEvents() {
        WatchKey key;
        while ((key = watchService.poll()) != null) {
            Path dir = watchKeyToPath.get(key);
            if (dir == null) {
                continue;
            }

            for (WatchEvent<?> event : key.pollEvents()) {
                WatchEvent.Kind<?> kind = event.kind();
                if (kind == StandardWatchEventKinds.OVERFLOW) {
                    continue;
                }

                @SuppressWarnings("unchecked")
                WatchEvent<Path> ev = (WatchEvent<Path>) event;
                Path fileName = ev.context();
                Path fullPath = dir.resolve(fileName);

                if (kind == StandardWatchEventKinds.ENTRY_CREATE) {
                    if (Files.isDirectory(fullPath)) {
                        try {
                            addWatchedDirectory(fullPath.toString());
                        } catch (IOException e) {
                            log.error("無法添加新創建的目錄到監視列表: {}", fullPath, e);
                        }
                    }
                }
            }

            boolean valid = key.reset();
            if (!valid) {
                watchKeyToPath.remove(key);
                if (watchKeyToPath.isEmpty()) {
                    break;
                }
            }
        }
    }

    public void addWatchedDirectory(String path) throws IOException {
        Path directory = Paths.get(path);
        if (Files.isDirectory(directory) && !watchedDirectories.contains(directory)) {
            WatchKey key = directory.register(watchService, StandardWatchEventKinds.ENTRY_CREATE,
                    StandardWatchEventKinds.ENTRY_DELETE, StandardWatchEventKinds.ENTRY_MODIFY);
            watchedDirectories.add(directory);
            watchKeyToPath.put(key, directory);
            log.info("已添加監視目錄: {}", directory);

            settingsManager.addToStringList(WATCHED_DIRECTORIES_KEY, path);
        }
    }

    public void removeWatchedDirectory(String path) {
        Path directory = Paths.get(path);
        if (watchedDirectories.remove(directory)) {
            watchKeyToPath.entrySet().removeIf(entry -> entry.getValue().equals(directory));
            log.info("已移除監視目錄: {}", directory);

            settingsManager.removeFromStringList(WATCHED_DIRECTORIES_KEY, path);
        }
    }

    public CompletableFuture<TreeItem<String>> getDirectoryTree() {
        return CompletableFuture.supplyAsync(() -> {
            TreeItem<String> root = new TreeItem<>("監視的目錄");
            root.setExpanded(true);

            List<CompletableFuture<TreeItem<String>>> futures = watchedDirectories.stream()
                    .map(this::createTreeItemAsync)
                    .toList();

            CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();

            for (CompletableFuture<TreeItem<String>> future : futures) {
                root.getChildren().add(future.join());
            }

            return root;
        }, executorService);
    }

    private CompletableFuture<TreeItem<String>> createTreeItemAsync(Path path) {
        return CompletableFuture.supplyAsync(() -> createTreeItem(path.toFile()), executorService);
    }

    private TreeItem<String> createTreeItem(File file) {
        TreeItem<String> item = new TreeItem<>(file.getName(), getFileIcon(file));
        if (file.isDirectory()) {
            item.setExpanded(isDirectoryExpanded(file.getAbsolutePath()));
            loadChildrenInBatches(item, file);
        }
        return item;
    }

    private void loadChildrenInBatches(TreeItem<String> parentItem, File parentFile) {
        File[] children = parentFile.listFiles();
        if (children == null) {
            return;
        }

        List<File> childList = Arrays.asList(children);
        for (int i = 0; i < childList.size(); i += BATCH_SIZE) {
            final int start = i;
            final int end = Math.min(start + BATCH_SIZE, childList.size());
            CompletableFuture.runAsync(() -> {
                List<TreeItem<String>> batch = childList.subList(start, end).stream()
                        .map(this::createTreeItem)
                        .toList();
                Platform.runLater(() -> parentItem.getChildren().addAll(batch));
            }, executorService);
        }
    }

    private FontIcon getFileIcon(File file) {
        if (file.isDirectory()) {
            return new FontIcon(FontAwesomeSolid.FOLDER);
        } else {
            String extension = getFileExtension(file);
            return switch (extension.toLowerCase()) {
                case "png", "jpg", "jpeg", "gif" -> new FontIcon(FontAwesomeSolid.IMAGE);
                case "txt" -> new FontIcon(FontAwesomeSolid.FILE_ALT);
                default -> new FontIcon(FontAwesomeSolid.FILE);
            };
        }
    }

    private String getFileExtension(File file) {
        String name = file.getName();
        int lastIndexOf = name.lastIndexOf(".");
        if (lastIndexOf == -1) {
            return "";
        }
        return name.substring(lastIndexOf + 1);
    }

    public void setDirectoryExpanded(String path, boolean expanded) {
        settingsManager.setBoolean(EXPANDED_PREFIX + path, expanded);
    }

    private boolean isDirectoryExpanded(String path) {
        return settingsManager.getBoolean(EXPANDED_PREFIX + path, false);
    }

    public void shutdown() {
        executorService.shutdown();
        scheduledExecutorService.shutdown();
        try {
            watchService.close();
        } catch (IOException e) {
            log.error("關閉 WatchService 時發生錯誤", e);
        }
    }
}