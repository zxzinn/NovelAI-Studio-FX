package com.zxzinn.novelai.utils.yaml;

import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.DumperOptions;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.*;

public class YamlProcessor {

    public static void mergeAndProcessYamlFiles(List<File> files, File outputFile) throws IOException {
        Set<String> allTags = new HashSet<>();

        for (File file : files) {
            try (FileReader reader = new FileReader(file)) {
                Yaml yaml = new Yaml();
                Map<String, Object> data = yaml.load(reader);

                if (data.containsKey("tagset")) {
                    Map<String, Object> tagset = (Map<String, Object>) data.get("tagset");
                    for (Object value : tagset.values()) {
                        Map<String, Object> tagGroup = (Map<String, Object>) value;
                        if (tagGroup.containsKey("tags")) {
                            allTags.addAll((List<String>) tagGroup.get("tags"));
                        }
                    }
                }
            }
        }

        // Create the final merged data
        Map<String, Object> mergedData = new LinkedHashMap<>();
        Map<String, Object> mergedTagset = new LinkedHashMap<>();
        Map<String, Object> mergedTagGroup = new LinkedHashMap<>();
        mergedTagGroup.put("tags", new ArrayList<>(allTags));
        mergedTagGroup.put("ArtistTagPrefix", false);
        mergedTagGroup.put("Sampling", 1);
        mergedTagset.put("merged", mergedTagGroup);
        mergedData.put("tagset", mergedTagset);
        mergedData.put("condition", "merged");

        // Write the merged data to the output file
        DumperOptions options = new DumperOptions();
        options.setDefaultFlowStyle(DumperOptions.FlowStyle.BLOCK);
        options.setPrettyFlow(true);
        Yaml yaml = new Yaml(options);

        try (FileWriter writer = new FileWriter(outputFile)) {
            yaml.dump(mergedData, writer);
        }
    }
}