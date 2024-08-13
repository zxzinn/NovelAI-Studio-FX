package com.zxzinn.novelai.test;

import lombok.Setter;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;

import java.io.*;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Base64;
import java.util.function.Predicate;
import java.util.logging.Logger;
import java.util.logging.Level;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class DanbooruRelatedTagFetcher {

    private static final String API_URL = "https://danbooru.donmai.us/related_tag.json";
    private static final String POST_API_URL = "https://danbooru.donmai.us/posts.json";
    private static final OkHttpClient client = new OkHttpClient();
    private static final Gson gson = new Gson();
    private static final Logger logger = Logger.getLogger(DanbooruRelatedTagFetcher.class.getName());

    private static final List<String> NSFW_TAGS = Arrays.asList(
            "sex", "nude", "naked", "nipples", "breasts", "penis", "vagina", "pussy",
            "explicit", "nsfw", "hentai", "porn", "adult", "xxx"
    );

    private static final List<String> DESIRED_TAGS = Arrays.asList(
            "1girl"
    );

    private String baseQuery;
    private String category;
    private int limit;
    @Setter
    private LocalDate cutoffDate;
    @Setter
    private Predicate<String> tagFilter;

    public DanbooruRelatedTagFetcher(String baseQuery, String category, int limit) {
        this.baseQuery = baseQuery;
        this.category = category;
        this.limit = limit;
        this.cutoffDate = LocalDate.of(2023, 11, 1); // 默認截止日期
        this.tagFilter = tag -> true; // 默認接受所有標籤
    }

    public void fetchAndSaveRelatedTags(String fileName) throws IOException {
        String url = String.format("%s?query=%s&category=%s&limit=%d", API_URL, URLEncoder.encode(baseQuery, StandardCharsets.UTF_8), category, limit);

        Request request = new Request.Builder()
                .url(url)
                .build();

        try (Response response = client.newCall(request).execute();
             BufferedWriter writer = new BufferedWriter(new FileWriter(fileName))) {

            if (!response.isSuccessful()) throw new IOException("Unexpected code " + response);

            String jsonData = response.body().string();
            JsonObject jsonObject = gson.fromJson(jsonData, JsonObject.class);
            JsonArray relatedTagsArray = jsonObject.getAsJsonArray("related_tags");

            // 寫入 HTML 頭部
            writer.write("<!DOCTYPE html><html><head><title>Hololive Characters</title>");
            writer.write("<style>");
            writer.write("body{font-family:Arial,sans-serif;}");
            writer.write(".character{display:flex;align-items:center;margin-bottom:20px;}");
            writer.write(".character img{width:200px;margin-right:20px;}");
            writer.write("#searchBox{margin-bottom:20px;padding:5px;width:300px;}");
            writer.write("</style>");
            writer.write("</head><body>");
            writer.write("<input type='text' id='searchBox' placeholder='Search characters...'>");
            writer.write("<div id='characterList'>");

            for (JsonElement element : relatedTagsArray) {
                JsonObject tagObject = element.getAsJsonObject();
                String tagName = tagObject.getAsJsonObject("tag").get("name").getAsString();
                String createdAt = tagObject.getAsJsonObject("tag").get("created_at").getAsString();

                LocalDate tagDate = LocalDate.parse(createdAt.split("T")[0], DateTimeFormatter.ISO_LOCAL_DATE);

                if (tagDate.isBefore(cutoffDate) && tagFilter.test(tagName)) {
                    try {
                        String[] imageInfo = fetchCharacterImage(tagName);
                        if (imageInfo != null) {
                            String base64Image = downloadAndEncodeImage(imageInfo[0]);
                            if (base64Image != null && !base64Image.isEmpty()) {
                                writer.write(String.format("<div class='character'><img src='data:image/%s;base64,%s' alt='%s'><div>%s</div></div>",
                                        imageInfo[1], base64Image, tagName, tagName));
                                writer.flush();
                                System.out.println("Added: " + tagName);
                            } else {
                                logger.warning("Empty image for character: " + tagName);
                            }
                        }
                    } catch (IOException e) {
                        logger.log(Level.WARNING, "Failed to fetch image for " + tagName, e);
                    }
                }
            }

            writer.write("</div>");

            // 添加 JavaScript 用於搜索功能
            writer.write("<script>");
            writer.write("document.getElementById('searchBox').addEventListener('input', function() {");
            writer.write("    var searchTerm = this.value.toLowerCase();");
            writer.write("    var characters = document.getElementsByClassName('character');");
            writer.write("    for (var i = 0; i < characters.length; i++) {");
            writer.write("        var characterName = characters[i].getElementsByTagName('div')[0].textContent.toLowerCase();");
            writer.write("        if (characterName.includes(searchTerm)) {");
            writer.write("            characters[i].style.display = '';");
            writer.write("        } else {");
            writer.write("            characters[i].style.display = 'none';");
            writer.write("        }");
            writer.write("    }");
            writer.write("});");
            writer.write("</script>");

            // 寫入 HTML 尾部
            writer.write("</body></html>");
        }
    }

    private String[] fetchCharacterImage(String characterTag) throws IOException {
        String encodedCharacterTag = URLEncoder.encode(characterTag, StandardCharsets.UTF_8.toString());
        String desiredTagsString = DESIRED_TAGS.stream()
                .map(tag -> {
                    try {
                        return URLEncoder.encode(tag, StandardCharsets.UTF_8.toString());
                    } catch (UnsupportedEncodingException e) {
                        return "";
                    }
                })
                .collect(Collectors.joining("+"));

        String url = String.format("%s?tags=%s+%s&limit=100&order=random", POST_API_URL, encodedCharacterTag, desiredTagsString);

        Request request = new Request.Builder()
                .url(url)
                .build();

        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                logger.warning("Failed to fetch image for " + characterTag + ". Response: " + response);
                return null;
            }

            String jsonData = response.body().string();
            JsonArray postsArray = gson.fromJson(jsonData, JsonArray.class);

            for (JsonElement postElement : postsArray) {
                JsonObject postObject = postElement.getAsJsonObject();
                String tagString = postObject.get("tag_string").getAsString().toLowerCase();

                // 放寬限制，只檢查是否不包含 NSFW 標籤
                if (NSFW_TAGS.stream().noneMatch(tagString::contains)) {
                    String fileUrl = postObject.get("file_url").getAsString();
                    String fileExt = postObject.get("file_ext").getAsString();
                    return new String[]{fileUrl, fileExt};
                }
            }

            logger.warning("No suitable image found for character: " + characterTag);
            return null;
        }
    }

    private String downloadAndEncodeImage(String imageUrl) throws IOException {
        URL url = new URL(imageUrl);
        try (InputStream in = url.openStream()) {
            byte[] imageBytes = in.readAllBytes();
            if (imageBytes.length == 0) {
                logger.warning("Downloaded image is empty: " + imageUrl);
                return null;
            }
            return Base64.getEncoder().encodeToString(imageBytes);
        }
    }

    public static void main(String[] args) {
        try {
            String baseQuery = "hololive";
            DanbooruRelatedTagFetcher fetcher = new DanbooruRelatedTagFetcher(baseQuery, "character", 5000);
            fetcher.setCutoffDate(LocalDate.of(2023, 11, 1));
            // fetcher.setTagFilter(tag -> tag.contains("hololive") || tag.contains("(hololive)"));

            String fileName = "hololive_characters_with_images.html";

            fetcher.fetchAndSaveRelatedTags(fileName);
            System.out.println("HTML file with character info and images has been created successfully.");
        } catch (IOException e) {
            logger.log(Level.SEVERE, "An error occurred", e);
        }
    }
}