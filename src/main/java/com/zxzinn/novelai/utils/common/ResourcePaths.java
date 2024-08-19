package com.zxzinn.novelai.utils.common;

public final class ResourcePaths {
    public static final String FXML_ROOT = "/com/zxzinn/novelai/fxml/";
    public static final String CSS_ROOT = "/com/zxzinn/novelai/css/";
    public static final String TOKENIZER_ROOT = "/com/zxzinn/novelai/tokenizers/";

    public static final String MAIN_VIEW_FXML = FXML_ROOT + "MainView.fxml";
    public static final String FILE_MANAGER_FXML = FXML_ROOT + "filemanager/FileManager.fxml";
    public static final String IMAGE_GENERATOR_FXML = FXML_ROOT + "generator/ImageGenerator.fxml";
    public static final String IMAGE_CONTROL_BAR_FXML = FXML_ROOT + "generator/ImageControlBar.fxml";
    public static final String HISTORY_IMAGE_PANE = FXML_ROOT + "generator/HistoryImagesPane.fxml";
    public static final String PROMPT_CONTROLS = FXML_ROOT + "generator/PromptControls.fxml";
    public static final String PROMPT_AREA = FXML_ROOT + "generator/PromptArea.fxml";
    public static final String PROMPT_PREVIEW_AREA = FXML_ROOT + "generator/PromptPreviewArea.fxml";

    public static final String STYLES_CSS = CSS_ROOT + "style-purple.css";

    public static final String SIMPLE_TOKENIZER = TOKENIZER_ROOT + "bpe_simple_vocab_16e6.txt.gz";

    private ResourcePaths() {
    }
}