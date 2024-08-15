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
    private static final OkHttpClient client = new OkHttpClient();
    private static final Gson gson = new Gson();
    private static final Logger logger = Logger.getLogger(DanbooruRelatedTagFetcher.class.getName());

    private final List<TagQuery> queries;
    @Setter private LocalDate cutoffDate;
    @Setter private Predicate<String> tagFilter;
    @Setter private Set<String> excludedTags;

    private final BufferedWriter txtWriter;

    public DanbooruRelatedTagFetcher(List<TagQuery> queries, String txtFileName) throws IOException {
        this.queries = queries;
        this.cutoffDate = LocalDate.now().minusMonths(1); // Default to 1 month ago
        this.tagFilter = tag -> true; // Default to accepting all tags
        this.excludedTags = new HashSet<>();
        this.txtWriter = new BufferedWriter(new FileWriter(txtFileName));
    }

    public void fetchAndSaveRelatedTags() throws IOException {
        try {
            for (TagQuery query : queries) {
                fetchAndSaveRelatedTags(query);
            }
        } finally {
            txtWriter.close();
        }
    }

    private void fetchAndSaveRelatedTags(TagQuery query) throws IOException {
        List<String> tags = fetchRelatedTags(query);
        writeTxtSection(tags);
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
                String tagName = getStringFromJson(tag);
                int category = getIntFromJson(tag);
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

    private String getStringFromJson(JsonObject jsonObject) {
        JsonElement element = jsonObject.get("name");
        return element != null && !element.isJsonNull() ? element.getAsString() : null;
    }

    private int getIntFromJson(JsonObject jsonObject) {
        JsonElement element = jsonObject.get("category");
        return element != null && !element.isJsonNull() ? element.getAsInt() : 0;
    }

    private String buildRelatedTagApiUrl(TagQuery query) {
        return String.format("%s?query=%s&category=%s&limit=%d",
                RELATED_TAG_API_URL,
                URLEncoder.encode(query.query(), StandardCharsets.UTF_8),
                query.category(),
                query.limit());
    }

    private boolean isTagAllowed(String tag) {
        return !excludedTags.contains(tag) && tagFilter.test(tag);
    }

    private void writeTxtSection(List<String> tags) throws IOException {
        for (String tag : tags) {
            txtWriter.write(tag);
            txtWriter.newLine();
        }
        txtWriter.flush();  // 確保內容被寫入文件
    }

    public static void main(String[] args) {
        try {
            List<TagQuery> queries = new ArrayList<>();
            final String query = "touhou";
            queries.add(new TagQuery(query, "4", 500)); // Use "4" for character category

            String txtFileName = query + ".txt";

            DanbooruRelatedTagFetcher fetcher = new DanbooruRelatedTagFetcher(queries, txtFileName);
            fetcher.setCutoffDate(LocalDate.of(2023, 10, 1));

            Set<String> excludedTags = new HashSet<>(Arrays.asList("cigarette", "sex"));
            fetcher.setExcludedTags(excludedTags);

            fetcher.fetchAndSaveRelatedTags();
            System.out.println("TXT file has been created successfully.");
        } catch (Exception e) {
            logger.log(Level.SEVERE, "An error occurred", e);
        }
    }

    public record TagQuery(String query, String category, int limit) {}

    private static class RelatedTag {
        String name;
        double cosine;

        RelatedTag(String name, double cosine) {
            this.name = name;
            this.cosine = cosine;
        }
    }
}