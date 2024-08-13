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
import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.Yaml;

import java.io.*;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.function.Predicate;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

public class DanbooruRelatedTagFetcher {

    private static final String API_URL = "https://danbooru.donmai.us/related_tag.json";
    private static final String POST_API_URL = "https://danbooru.donmai.us/posts.json";
    private static final OkHttpClient client = new OkHttpClient();
    private static final Gson gson = new Gson();
    private static final Logger logger = Logger.getLogger(DanbooruRelatedTagFetcher.class.getName());

    private static final List<String> NSFW_TAGS = Arrays.asList("sex", "nude", "explicit");
    private static final int MAX_IMAGES_PER_TAG = 5;

    private final List<TagQuery> queries;
    @Setter private LocalDate cutoffDate;
    @Setter private Predicate<String> tagFilter;
    @Setter private Set<String> excludedTags;
    @Setter private int minimumScore;

    public DanbooruRelatedTagFetcher(List<TagQuery> queries) {
        this.queries = queries;
        this.cutoffDate = LocalDate.now().minusMonths(1); // Default to 1 month ago
        this.tagFilter = tag -> true; // Default to accepting all tags
        this.excludedTags = new HashSet<>();
        this.minimumScore = 0;
    }

    public void fetchAndSaveRelatedTags(String htmlFileName, String yamlFileName) throws IOException {
        Map<String, List<String>> allTags = new HashMap<>();

        for (TagQuery query : queries) {
            fetchRelatedTags(query, allTags);
        }

        writeHtmlFile(htmlFileName, allTags);
        writeYamlFile(yamlFileName, allTags);
    }

    private void fetchRelatedTags(TagQuery query, Map<String, List<String>> allTags) throws IOException {
        String url = buildApiUrl(query);
        Request request = new Request.Builder().url(url).build();

        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) throw new IOException("Unexpected code " + response);

            String jsonData = response.body().string();
            JsonObject jsonObject = gson.fromJson(jsonData, JsonObject.class);
            JsonArray relatedTagsArray = jsonObject.getAsJsonArray("related_tags");

            for (JsonElement element : relatedTagsArray) {
                JsonObject tagObject = element.getAsJsonObject();
                String tagName = tagObject.getAsJsonObject("tag").get("name").getAsString();
                String createdAt = tagObject.getAsJsonObject("tag").get("created_at").getAsString();

                LocalDate tagDate = LocalDate.parse(createdAt.split("T")[0], DateTimeFormatter.ISO_LOCAL_DATE);

                if (tagDate.isAfter(cutoffDate) && isTagAllowed(tagName)) {
                    allTags.computeIfAbsent(query.getCategory(), k -> new ArrayList<>()).add(tagName);
                }
            }
        }
    }

    private String buildApiUrl(TagQuery query) throws UnsupportedEncodingException {
        return String.format("%s?query=%s&category=%s&limit=%d",
                API_URL,
                URLEncoder.encode(query.getQuery(), StandardCharsets.UTF_8),
                query.getCategory(),
                query.getLimit());
    }

    private boolean isTagAllowed(String tag) {
        return !excludedTags.contains(tag) && tagFilter.test(tag);
    }

    private void writeHtmlFile(String htmlFileName, Map<String, List<String>> allTags) throws IOException {
        try (BufferedWriter htmlWriter = new BufferedWriter(new FileWriter(htmlFileName))) {
            writeHtmlHeader(htmlWriter);
            writeHtmlBody(htmlWriter, allTags);
            writeHtmlFooter(htmlWriter);
        }
    }

    private void writeHtmlHeader(BufferedWriter writer) throws IOException {
        writer.write("<!DOCTYPE html><html><head><title>Danbooru Tags</title>");
        writer.write("<style>");
        writer.write("body { font-family: Arial, sans-serif; }");
        writer.write(".character { display: flex; flex-direction: column; margin-bottom: 40px; }");
        writer.write(".character-name { font-weight: bold; margin-bottom: 10px; font-size: 18px; }");
        writer.write(".image-container { display: flex; flex-wrap: wrap; gap: 10px; }");
        writer.write(".image-wrapper { width: 200px; height: 200px; display: flex; justify-content: center; align-items: center; overflow: hidden; }");
        writer.write(".character img { max-width: 100%; max-height: 100%; object-fit: contain; }");
        writer.write("#searchBox { margin-bottom: 20px; padding: 5px; width: 300px; }");
        writer.write("</style>");
        writer.write("</head><body>");
        writer.write("<input type='text' id='searchBox' placeholder='Search tags...'>");
        writer.write("<div id='tagList'>");
    }

    private void writeHtmlBody(BufferedWriter writer, Map<String, List<String>> allTags) throws IOException {
        for (Map.Entry<String, List<String>> entry : allTags.entrySet()) {
            writer.write("<h2>" + entry.getKey() + "</h2>");
            for (String tagName : entry.getValue()) {
                writer.write("<div class='character'>");
                writer.write("<div class='character-name'>" + tagName + "</div>");
                writer.write("<div class='image-container'>");
                List<String[]> imageInfoList = fetchCharacterImages(tagName);
                for (String[] imageInfo : imageInfoList) {
                    String base64Image = downloadAndEncodeImage(imageInfo[0]);
                    if (base64Image != null && !base64Image.isEmpty()) {
                        writer.write("<div class='image-wrapper'>");
                        writer.write(String.format("<img src='data:image/%s;base64,%s' alt='%s'>",
                                imageInfo[1], base64Image, tagName));
                        writer.write("</div>");
                        writer.flush();
                    }
                }
                writer.write("</div></div>");
                System.out.println("Added: " + tagName);
            }
        }
    }

    private void writeHtmlFooter(BufferedWriter writer) throws IOException {
        writer.write("</div>");
        writer.write("<script>");
        writer.write("document.getElementById('searchBox').addEventListener('input', function() {");
        writer.write("    var searchTerm = this.value.toLowerCase();");
        writer.write("    var characters = document.getElementsByClassName('character');");
        writer.write("    for (var i = 0; i < characters.length; i++) {");
        writer.write("        var characterName = characters[i].getElementsByClassName('character-name')[0].textContent.toLowerCase();");
        writer.write("        if (characterName.includes(searchTerm)) {");
        writer.write("            characters[i].style.display = '';");
        writer.write("        } else {");
        writer.write("            characters[i].style.display = 'none';");
        writer.write("        }");
        writer.write("    }");
        writer.write("});");
        writer.write("</script>");
        writer.write("</body></html>");
    }

    private void writeYamlFile(String yamlFileName, Map<String, List<String>> allTags) throws IOException {
        Map<String, Object> yamlData = new HashMap<>();
        Map<String, Object> tagset = new HashMap<>();

        for (Map.Entry<String, List<String>> entry : allTags.entrySet()) {
            Map<String, Object> categoryMap = new HashMap<>();
            categoryMap.put("tags", entry.getValue());
            categoryMap.put("ArtistTagPrefix", entry.getKey().equals("artist"));
            categoryMap.put("Sampling", 1);
            tagset.put(entry.getKey(), categoryMap);
        }

        yamlData.put("tagset", tagset);
        yamlData.put("condition", String.join(" and ", allTags.keySet()));

        DumperOptions options = new DumperOptions();
        options.setDefaultFlowStyle(DumperOptions.FlowStyle.BLOCK);
        options.setPrettyFlow(true);
        Yaml yaml = new Yaml(options);
        try (FileWriter yamlWriter = new FileWriter(yamlFileName)) {
            yaml.dump(yamlData, yamlWriter);
        }
    }

    private List<String[]> fetchCharacterImages(String characterTag) throws IOException {
        List<String[]> imageInfoList = new ArrayList<>();
        String encodedCharacterTag = URLEncoder.encode(characterTag, StandardCharsets.UTF_8.toString());
        String url = String.format("%s?tags=%s+1girl&limit=100&order=random&score:>=%d", POST_API_URL, encodedCharacterTag, minimumScore);

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

                if (!postObject.has("tag_string") || !postObject.has("large_file_url") || !postObject.has("file_ext")) {
                    continue;
                }

                String tagString = postObject.get("tag_string").getAsString().toLowerCase();

                if (NSFW_TAGS.stream().noneMatch(tagString::contains)) {
                    String fileUrl = postObject.get("large_file_url").getAsString();
                    String fileExt = postObject.get("file_ext").getAsString();
                    imageInfoList.add(new String[]{fileUrl, fileExt});
                }
            }

            if (imageInfoList.isEmpty()) {
                logger.warning("No suitable images found for character: " + characterTag);
            }
        } catch (Exception e) {
            logger.warning("Error processing image data for " + characterTag + ": " + e.getMessage());
        }

        return imageInfoList;
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
        } catch (IOException e) {
            logger.warning("Failed to download image: " + imageUrl + ". Error: " + e.getMessage());
            return null;
        }
    }

    public static void main(String[] args) {
        try {
            List<TagQuery> queries = new ArrayList<>();
            queries.add(new TagQuery("hololive", "character", 500));
            queries.add(new TagQuery("1girl", "general", 100));

            DanbooruRelatedTagFetcher fetcher = new DanbooruRelatedTagFetcher(queries);
            fetcher.setCutoffDate(LocalDate.of(2023, 1, 1));
            fetcher.setMinimumScore(30);

            Set<String> excludedTags = new HashSet<>(Arrays.asList("nsfw", "r-18", "mature"));
            fetcher.setExcludedTags(excludedTags);

            String htmlFileName = "danbooru_tags_with_images.html";
            String yamlFileName = "danbooru_tags.yml";

            fetcher.fetchAndSaveRelatedTags(htmlFileName, yamlFileName);
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
}