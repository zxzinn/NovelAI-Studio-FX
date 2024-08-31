package com.zxzinn.novelai.controller.filemanager;

import com.zxzinn.novelai.service.filemanager.FileManagerService;
import javafx.scene.control.TreeItem;
import org.apache.commons.io.FilenameUtils;
import org.kordamp.ikonli.fontawesome5.FontAwesomeSolid;
import org.kordamp.ikonli.javafx.FontIcon;

import java.io.File;

public class TreeItemFactory {
    private final FileManagerService fileManagerService;

    public TreeItemFactory(FileManagerService fileManagerService) {
        this.fileManagerService = fileManagerService;
    }

    public TreeItem<String> createTreeItem(File file) {
        TreeItem<String> item = new TreeItem<>(file.getName(), getFileIcon(file));
        if (file.isDirectory()) {
            item.setExpanded(fileManagerService.isDirectoryExpanded(file.getAbsolutePath()));
        }
        return item;
    }

    private FontIcon getFileIcon(File file) {
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