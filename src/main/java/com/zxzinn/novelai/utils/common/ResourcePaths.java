package com.zxzinn.novelai.utils.common;

import javafx.fxml.FXML;

public final class ResourcePaths {
    public static final String FXML_ROOT = "/com/zxzinn/novelai/fxml/";
    public static final String CSS_ROOT = "/com/zxzinn/novelai/css/";
    public static final String TOKENIZER_ROOT = "/com/zxzinn/novelai/tokenizers/";
    public static final String EXECUTABLE_ROOT = "/com/zxzinn/novelai/executable/";
    public static final String ICON_PATH = "/com/zxzinn/novelai/icon/";

    public static final String MAIN_VIEW_FXML = FXML_ROOT + "MainView.fxml";
    public static final String TASK_MONITOR_FXML = FXML_ROOT + "task_monitor.fxml";
    public static final String FILE_MANAGER_FXML = FXML_ROOT + "filemanager/FileManager.fxml";
    public static final String IMAGE_GENERATOR_FXML = FXML_ROOT + "generator/ImageGenerator.fxml";

    public static final String META_READER_PATH = EXECUTABLE_ROOT + "meta_reader.exe";

    public static final String STYLES_CSS = CSS_ROOT + "style-purple.css";

    public static final String SIMPLE_TOKENIZER = TOKENIZER_ROOT + "bpe_simple_vocab_16e6.txt.gz";


    private ResourcePaths() {
    }
}