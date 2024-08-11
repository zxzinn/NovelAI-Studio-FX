package com.zxzinn.novelai.service.filemanager;

import com.zxzinn.novelai.utils.common.SettingsManager;
import javafx.application.Platform;
import javafx.scene.control.TreeItem;
import lombok.Setter;
import lombok.extern.log4j.Log4j2;

import java.io.File;
import java.io.IOException;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.function.BiConsumer;
import java.util.function.Predicate;

@Log4j2
public class FileManagerService {
    private static final String WATCHED_DIRECTORIES_KEY = "watchedDirectories";
    private static final String EXPANDED_PREFIX = "expanded_";

    private final Set<Path> watchedDirectories;
    private final Map<WatchKey, Path> watchKeyToPath;
    private final WatchService watchService;
    private final SettingsManager settingsManager;
    private final ExecutorService executorService;
    private final ScheduledExecutorService scheduledExecutorService;
    @Setter
    private BiConsumer<String, WatchEvent.Kind<?>> fileChangeListener;

    private final DirectoryWatcher directoryWatcher;
    private final FileTreeBuilder fileTreeBuilder;

    private Predicate<File> fileFilter = file -> true;
    private Comparator<File> fileComparator = Comparator
            .comparing(File::isDirectory).reversed()
            .thenComparing(File::getName);

    public FileManagerService(SettingsManager settingsManager) throws IOException {
        this.watchedDirectories = ConcurrentHashMap.newKeySet();
        this.watchKeyToPath = new ConcurrentHashMap<>();
        this.watchService = FileSystems.getDefault().newWatchService();
        this.settingsManager = settingsManager;
        this.executorService = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());
        this.scheduledExecutorService = Executors.newSingleThreadScheduledExecutor();

        this.directoryWatcher = new DirectoryWatcher(watchService, watchKeyToPath, this::notifyFileChange);
        this.fileTreeBuilder = new FileTreeBuilder(executorService, this::isDirectoryExpanded, fileFilter, fileComparator);

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
        scheduledExecutorService.scheduleWithFixedDelay(directoryWatcher::processEvents, 0, 1, TimeUnit.SECONDS);
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
        Path directory = Paths.get(path).toAbsolutePath().normalize();
        log.info("嘗試移除監視目錄: {}", directory);

        Optional<Path> watchedDir = watchedDirectories.stream()
                .filter(dir -> dir.endsWith(directory.getFileName()))
                .findFirst();

        if (watchedDir.isPresent()) {
            Path dirToRemove = watchedDir.get();
            watchedDirectories.remove(dirToRemove);
            removeWatchKey(dirToRemove);
            log.info("已移除監視目錄: {}", dirToRemove);
            settingsManager.removeFromStringList(WATCHED_DIRECTORIES_KEY, dirToRemove.toString());
        } else {
            log.warn("嘗試移除不存在的監視目錄: {}", directory);
        }
    }

    private void removeWatchKey(Path directory) {
        WatchKey keyToRemove = null;
        for (Map.Entry<WatchKey, Path> entry : watchKeyToPath.entrySet()) {
            if (entry.getValue().equals(directory)) {
                keyToRemove = entry.getKey();
                break;
            }
        }
        if (keyToRemove != null) {
            keyToRemove.cancel();
            Path removedPath = watchKeyToPath.remove(keyToRemove);
            if (removedPath != null) {
                log.info("已從 watchKeyToPath 中移除路徑: {}", removedPath);
            } else {
                log.warn("無法從 watchKeyToPath 中移除路徑: {}", directory);
            }
        } else {
            log.warn("未找到與路徑 {} 對應的 WatchKey", directory);
        }
    }

    public CompletableFuture<TreeItem<String>> getDirectoryTree() {
        return fileTreeBuilder.buildDirectoryTree(watchedDirectories);
    }

    public void setDirectoryExpanded(String path, boolean expanded) {
        settingsManager.setBoolean(EXPANDED_PREFIX + path, expanded);
    }

    private boolean isDirectoryExpanded(String path) {
        return settingsManager.getBoolean(EXPANDED_PREFIX + path, false);
    }

    public String getWatchedDirectoryFullPath(String dirName) {
        for (Path path : watchedDirectories) {
            if (path.getFileName().toString().equals(dirName)) {
                return path.toString();
            }
        }
        return null;
    }

    private void notifyFileChange(Path path, WatchEvent.Kind<?> kind) {
        if (fileChangeListener != null) {
            Platform.runLater(() -> fileChangeListener.accept(path.toString(), kind));
        }
    }
    public void setFileFilter(Predicate<File> filter) {
        this.fileFilter = filter;
        fileTreeBuilder.setFileFilter(filter);
    }

    public void setFileComparator(Comparator<File> comparator) {
        this.fileComparator = comparator;
        fileTreeBuilder.setFileComparator(comparator);
    }

    public CompletableFuture<List<File>> searchFiles(String keyword) {
        return CompletableFuture.supplyAsync(() -> {
            List<File> results = new ArrayList<>();
            for (Path watchedDir : watchedDirectories) {
                searchFilesRecursively(watchedDir.toFile(), keyword, results);
            }
            return results;
        }, executorService);
    }

    private void searchFilesRecursively(File directory, String keyword, List<File> results) {
        File[] files = directory.listFiles();
        if (files != null) {
            for (File file : files) {
                if (file.isDirectory()) {
                    searchFilesRecursively(file, keyword, results);
                } else if (file.getName().toLowerCase().contains(keyword.toLowerCase())) {
                    results.add(file);
                }
            }
        }
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