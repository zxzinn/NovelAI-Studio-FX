package com.zxzinn.novelai.service.filemanager;

import com.google.inject.Inject;
import javafx.application.Platform;
import javafx.scene.control.TreeItem;
import lombok.Setter;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.jetbrains.annotations.NotNull;
import org.kordamp.ikonli.fontawesome5.FontAwesomeSolid;
import org.kordamp.ikonli.javafx.FontIcon;

import java.io.File;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.function.Function;
import java.util.function.Predicate;

@Log4j2
public class FileTreeBuilder {
    private static final int BATCH_SIZE = 100;

    private final ExecutorService executorService;
    private final Function<String, Boolean> isExpandedChecker;
    @Setter private Predicate<File> fileFilter;
    @Setter private Comparator<File> fileComparator;

    @Inject
    public FileTreeBuilder(ExecutorService executorService, Function<String, Boolean> isExpandedChecker,
                           Predicate<File> fileFilter, Comparator<File> fileComparator) {
        this.executorService = executorService;
        this.isExpandedChecker = isExpandedChecker;
        this.fileFilter = fileFilter;
        this.fileComparator = fileComparator;
    }

    @NotNull
    private TreeItem<String> createTreeItem(@NotNull File file) {
        TreeItem<String> item = new TreeItem<>(file.getName(), getFileIcon(file));
        if (file.isDirectory()) {
            item.setExpanded(isExpandedChecker.apply(file.getAbsolutePath()));
            CompletableFuture.runAsync(() -> loadChildrenInBatches(item, file), executorService);
        }
        return item;
    }

    private void loadChildrenInBatches(TreeItem<String> parentItem, @NotNull File parentFile) {
        Collection<File> children = FileUtils.listFiles(parentFile, null, false);
        List<File> childList = new ArrayList<>(children);
        childList.removeIf(file -> !fileFilter.test(file));
        childList.sort(fileComparator);

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

    @NotNull
    private FontIcon getFileIcon(@NotNull File file) {
        if (file.isDirectory()) {
            return new FontIcon(FontAwesomeSolid.FOLDER);
        } else {
            String extension = FilenameUtils.getExtension(file.getName());
            return switch (extension.toLowerCase()) {
                case "png", "jpg", "jpeg", "gif" -> new FontIcon(FontAwesomeSolid.IMAGE);
                case "txt" -> new FontIcon(FontAwesomeSolid.FILE_ALT);
                default -> new FontIcon(FontAwesomeSolid.FILE);
            };
        }
    }
}