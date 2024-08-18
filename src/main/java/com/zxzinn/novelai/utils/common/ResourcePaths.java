package com.zxzinn.novelai.utils.common;

public final class ResourcePaths {
    public static final String FXML_ROOT = "/com/zxzinn/novelai/fxml/";
    public static final String CSS_ROOT = "/com/zxzinn/novelai/css/";

    public static final String MAIN_VIEW_FXML = FXML_ROOT + "MainView.fxml";
    public static final String FILE_MANAGER_FXML = FXML_ROOT + "filemanager/FileManager.fxml";
    public static final String IMAGE_GENERATOR_FXML = FXML_ROOT + "generator/ImageGenerator.fxml";

    public static final String STYLES_CSS = CSS_ROOT + "styles.css";

    private ResourcePaths() {
        // 私有構造函數防止實例化
    }
}