package com.zxzinn.novelai.test;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import lombok.Getter;
import lombok.Setter;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

import java.io.*;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.*;
import java.util.function.Predicate;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

public class DanbooruRelatedTagFetcher {

    private static final String RELATED_TAG_API_URL = "https://danbooru.donmai.us/related_tag.json";
    private static final String POST_API_URL = "https://danbooru.donmai.us/posts.json";
    private static final OkHttpClient client = new OkHttpClient();
    private static final Gson gson = new Gson();
    private static final Logger logger = Logger.getLogger(DanbooruRelatedTagFetcher.class.getName());

    private static final int MAX_IMAGES_PER_TAG = 5;

    private final List<TagQuery> queries;
    @Setter private LocalDate cutoffDate;
    @Setter private Predicate<String> tagFilter;
    @Setter private Set<String> excludedTags;
    @Setter private int minimumScore;

    private final BufferedWriter htmlWriter;
    private final BufferedWriter yamlWriter;
    private final Map<String, List<String>> allTags = new HashMap<>();

    public DanbooruRelatedTagFetcher(List<TagQuery> queries, String htmlFileName, String yamlFileName) throws IOException {
        this.queries = queries;
        this.cutoffDate = LocalDate.now().minusMonths(1); // Default to 1 month ago
        this.tagFilter = tag -> true; // Default to accepting all tags
        this.excludedTags = new HashSet<>();
        this.minimumScore = 0;
        this.htmlWriter = new BufferedWriter(new FileWriter(htmlFileName));
        this.yamlWriter = new BufferedWriter(new FileWriter(yamlFileName));
    }

    public void fetchAndSaveRelatedTags() throws IOException {
        try {
            writeHtmlHeader();
            writeYamlHeader();

            for (TagQuery query : queries) {
                fetchAndSaveRelatedTags(query);
            }

            writeHtmlFooter();
            writeYamlFooter();
        } finally {
            htmlWriter.close();
            yamlWriter.close();
        }
    }

    private void fetchAndSaveRelatedTags(TagQuery query) throws IOException {
        List<String> tags = fetchRelatedTags(query);
        allTags.put(query.getCategory(), tags);

        writeHtmlSection(query.getCategory(), tags);
        writeYamlSection(query.getCategory(), tags);
    }

    private List<String> fetchRelatedTags(TagQuery query) throws IOException {
        String url = buildRelatedTagApiUrl(query);
        Request request = new Request.Builder().url(url).build();

        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) throw new IOException("Unexpected code " + response);

            assert response.body() != null;
            String jsonData = response.body().string();
            JsonObject jsonObject = gson.fromJson(jsonData, JsonObject.class);

            if (!jsonObject.has("related_tags")) {
                logger.warning("Response doesn't contain 'related_tags' array");
                return new ArrayList<>();
            }

            JsonArray relatedTagsArray = jsonObject.getAsJsonArray("related_tags");

            List<RelatedTag> relatedTags = new ArrayList<>();
            for (JsonElement element : relatedTagsArray) {
                if (!element.isJsonObject()) {
                    logger.warning("Invalid element in 'related_tags' array");
                    continue;
                }

                JsonObject tagObject = element.getAsJsonObject();

                if (!tagObject.has("tag") || !tagObject.has("cosine_similarity")) {
                    logger.warning("Invalid tag object structure: " + tagObject);
                    continue;
                }

                JsonObject tag = tagObject.getAsJsonObject("tag");
                String tagName = getStringFromJson(tag, "name");
                int category = getIntFromJson(tag, "category");
                double cosine = tagObject.get("cosine_similarity").getAsDouble();

                if (tagName == null || category == 0) {
                    logger.warning("Invalid tag data: " + tag);
                    continue;
                }

                if (category == 4 && isTagAllowed(tagName)) { // 4 represents character category
                    relatedTags.add(new RelatedTag(tagName, cosine));
                }
            }

            // Sort by cosine similarity (descending order)
            relatedTags.sort((a, b) -> Double.compare(b.cosine, a.cosine));

            return relatedTags.stream()
                    .map(tag -> tag.name)
                    .collect(Collectors.toList());
        }
    }

    private String getStringFromJson(JsonObject jsonObject, String key) {
        JsonElement element = jsonObject.get(key);
        return element != null && !element.isJsonNull() ? element.getAsString() : null;
    }

    private int getIntFromJson(JsonObject jsonObject, String key) {
        JsonElement element = jsonObject.get(key);
        return element != null && !element.isJsonNull() ? element.getAsInt() : 0;
    }

    private String buildRelatedTagApiUrl(TagQuery query) throws UnsupportedEncodingException {
        return String.format("%s?query=%s&category=%s&limit=%d",
                RELATED_TAG_API_URL,
                URLEncoder.encode(query.getQuery(), StandardCharsets.UTF_8),
                query.getCategory(),
                query.getLimit());
    }

    private boolean isTagAllowed(String tag) {
        return !excludedTags.contains(tag) && tagFilter.test(tag);
    }

    private void writeHtmlHeader() throws IOException {
        htmlWriter.write("<!DOCTYPE html><html><head><title>Danbooru Tags</title>");
        htmlWriter.write("<style>");
        htmlWriter.write("body { font-family: Arial, sans-serif; }");
        htmlWriter.write(".character { display: flex; flex-direction: column; margin-bottom: 40px; }");
        htmlWriter.write(".character-name { font-weight: bold; margin-bottom: 10px; font-size: 18px; }");
        htmlWriter.write(".image-container { display: flex; flex-wrap: wrap; gap: 10px; }");
        htmlWriter.write(".image-wrapper { width: 200px; height: 200px; display: flex; justify-content: center; align-items: center; overflow: hidden; }");
        htmlWriter.write(".character img { max-width: 100%; max-height: 100%; object-fit: contain; }");
        htmlWriter.write("#searchBox { margin-bottom: 20px; padding: 5px; width: 300px; }");
        htmlWriter.write("</style>");
        htmlWriter.write("</head><body>");
        htmlWriter.write("<input type='text' id='searchBox' placeholder='Search tags...'>");
        htmlWriter.write("<div id='tagList'>");
    }

    private void writeHtmlSection(String category, List<String> tags) throws IOException {
        htmlWriter.write("<h2>" + category + "</h2>");
        for (String tagName : tags) {
            htmlWriter.write("<div class='character'>");
            htmlWriter.write("<div class='character-name'>" + tagName + "</div>");
            htmlWriter.write("<div class='image-container'>");

            List<String[]> imageInfoList = fetchCharacterImages(tagName);
            for (String[] imageInfo : imageInfoList) {
                String base64Image = downloadAndEncodeImage(imageInfo[0]);
                if (base64Image != null && !base64Image.isEmpty()) {
                    htmlWriter.write(String.format("<div class='image-wrapper'><img src='data:image/%s;base64,%s' alt='%s'></div>",
                            imageInfo[1], base64Image, tagName));
                }
            }

            htmlWriter.write("</div></div>");
            htmlWriter.flush();  // 確保內容被寫入文件
            System.out.println("Added: " + tagName);
        }
    }

    private void writeHtmlFooter() throws IOException {
        htmlWriter.write("</div>");
        htmlWriter.write("<script>");
        htmlWriter.write("document.getElementById('searchBox').addEventListener('input', function() {");
        htmlWriter.write("    var searchTerm = this.value.toLowerCase();");
        htmlWriter.write("    var characters = document.getElementsByClassName('character');");
        htmlWriter.write("    for (var i = 0; i < characters.length; i++) {");
        htmlWriter.write("        var characterName = characters[i].getElementsByClassName('character-name')[0].textContent.toLowerCase();");
        htmlWriter.write("        if (characterName.includes(searchTerm)) {");
        htmlWriter.write("            characters[i].style.display = '';");
        htmlWriter.write("        } else {");
        htmlWriter.write("            characters[i].style.display = 'none';");
        htmlWriter.write("        }");
        htmlWriter.write("    }");
        htmlWriter.write("});");
        htmlWriter.write("</script>");
        htmlWriter.write("</body></html>");
    }

    private void writeYamlHeader() throws IOException {
        yamlWriter.write("tagset:\n");
    }

    private void writeYamlSection(String category, List<String> tags) throws IOException {
        String queryName = queries.get(0).getQuery(); // 假設我們只有一個查詢
        yamlWriter.write("  " + queryName + ":\n");
        yamlWriter.write("    tags:\n");
        for (String tag : tags) {
            yamlWriter.write("      - " + tag + "\n");
        }
        yamlWriter.write("    ArtistTagPrefix: false\n");
        yamlWriter.write("    Sampling: 1\n");
        yamlWriter.flush();  // 確保內容被寫入文件
    }

    private void writeYamlFooter() throws IOException {
        String queryName = queries.get(0).getQuery(); // 假設我們只有一個查詢
        yamlWriter.write("condition: " + queryName);
    }

    private List<String[]> fetchCharacterImages(String characterTag) throws IOException {
        List<String[]> imageInfoList = new ArrayList<>();
        String encodedCharacterTag = URLEncoder.encode(characterTag + " 1girl rating:general", StandardCharsets.UTF_8.toString());
        String url = String.format("%s?tags=%s&limit=100&order=random&score:>=%d", POST_API_URL, encodedCharacterTag, minimumScore);

        logger.info("Fetching images for " + characterTag + " with minimum score: " + minimumScore);

        Request request = new Request.Builder().url(url).build();

        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                logger.warning("Failed to fetch images for " + characterTag + ". Response: " + response);
                return imageInfoList;
            }

            String jsonData = response.body().string();
            JsonArray postsArray = gson.fromJson(jsonData, JsonArray.class);

            for (JsonElement postElement : postsArray) {
                if (imageInfoList.size() >= MAX_IMAGES_PER_TAG) break;

                JsonObject postObject = postElement.getAsJsonObject();

                if (!postObject.has("preview_file_url") || !postObject.has("file_ext") || !postObject.has("score")) {
                    continue;
                }

                int score = postObject.get("score").getAsInt();
                if (score < minimumScore) {
                    continue;  // 跳過分數低於最小分數的圖片
                }

                String fileUrl = postObject.get("preview_file_url").getAsString();
                String fileExt = postObject.get("file_ext").getAsString();
                imageInfoList.add(new String[]{fileUrl, fileExt});
            }

            if (imageInfoList.isEmpty()) {
                logger.warning("No suitable images found for character: " + characterTag);
            }
        } catch (Exception e) {
            logger.warning("Error processing image data for " + characterTag + ": " + e.getMessage());
        }

        return imageInfoList;
    }

    private String downloadAndEncodeImage(String imageUrl) {
        try {
            Request request = new Request.Builder()
                    .url(imageUrl)
                    .addHeader("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472.124 Safari/537.36")
                    .build();

            try (Response response = client.newCall(request).execute()) {
                if (!response.isSuccessful()) {
                    logger.warning("Failed to download image: " + imageUrl + ". Response: " + response);
                    return null;
                }

                byte[] imageBytes = response.body().bytes();
                if (imageBytes.length == 0) {
                    logger.warning("Downloaded image is empty: " + imageUrl);
                    return null;
                }
                return Base64.getEncoder().encodeToString(imageBytes);
            }
        } catch (IOException e) {
            logger.warning("Failed to download image: " + imageUrl + ". Error: " + e.getMessage());
            return null;
        }
    }

    public static void main(String[] args) {
        try {
            List<TagQuery> queries = new ArrayList<>();
            final String query = "touhou";
            queries.add(new TagQuery(query, "4", 500)); // Use "4" for character category

            String htmlFileName = query + ".html";
            String yamlFileName = query + ".yml";

            DanbooruRelatedTagFetcher fetcher = new DanbooruRelatedTagFetcher(queries, htmlFileName, yamlFileName);
            fetcher.setCutoffDate(LocalDate.of(2023, 10, 1));
            fetcher.setMinimumScore(10);

            Set<String> excludedTags = new HashSet<>(Arrays.asList("cigarette", "sex"));
            fetcher.setExcludedTags(excludedTags);

            fetcher.fetchAndSaveRelatedTags();
            System.out.println("HTML and YAML files have been created successfully.");
        } catch (Exception e) {
            logger.log(Level.SEVERE, "An error occurred", e);
        }
    }

    @Getter
    public static class TagQuery {
        private final String query;
        private final String category;
        private final int limit;

        public TagQuery(String query, String category, int limit) {
            this.query = query;
            this.category = category;
            this.limit = limit;
        }
    }

    private static class RelatedTag {
        String name;
        double cosine;

        RelatedTag(String name, double cosine) {
            this.name = name;
            this.cosine = cosine;
        }
    }
}