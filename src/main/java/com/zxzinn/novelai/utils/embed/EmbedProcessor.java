package com.zxzinn.novelai.utils.embed;

import lombok.extern.log4j.Log4j2;
import org.yaml.snakeyaml.error.YAMLException;

import java.io.FileNotFoundException;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Log4j2
public class EmbedProcessor {
    private final EmbedDetector embedDetector;
    private final YamlLoader yamlLoader;

    public EmbedProcessor() {
        this.embedDetector = new EmbedDetector();
        this.yamlLoader = new YamlLoader();
    }

    public String processPrompt(String input) {
        log.info("Processing prompt: {}", input);

        String processedInput = processEmbeds(input);
        String finalResult = processStringPatterns(processedInput);

        log.info("Final processed prompt: {}", finalResult);
        return finalResult;
    }

    @SuppressWarnings("unchecked")
    private String processEmbeds(String input) {
        List<EmbedDetector.EmbedTag> embeds = embedDetector.detectEmbeds(input);
        StringBuilder result = new StringBuilder(input);

        for (int i = embeds.size() - 1; i >= 0; i--) {
            EmbedDetector.EmbedTag embed = embeds.get(i);
            try {
                Map<String, Object> yamlData = yamlLoader.loadYamlFile(embed.name());
                Map<String, Object> tagset = (Map<String, Object>) yamlData.get("tagset");
                ConditionProcessor conditionProcessor = new ConditionProcessor(tagset);
                List<String> generatedTags = conditionProcessor.processConditions((String) yamlData.get("condition"));

                if (!generatedTags.isEmpty()) {
                    String replacement = String.join(",", generatedTags);
                    result.replace(embed.start(), embed.end(), replacement);
                    log.debug("Generated tags for {}: {}", embed.name(), generatedTags);
                } else {
                    result.delete(embed.start(), embed.end());
                    log.debug("No tags generated for: {}", embed.name());
                }
            } catch (FileNotFoundException e) {
                log.warn("YAML file not found for tag: {}", embed.name());
                result.delete(embed.start(), embed.end());
            } catch (YAMLException e) {
                log.warn("Invalid YAML file for tag: {}. Error: {}", embed.name(), e.getMessage());
                result.delete(embed.start(), embed.end());
            } catch (Exception e) {
                log.error("Error processing YAML file for tag: {}. Error: {}", embed.name(), e.getMessage());
                result.delete(embed.start(), embed.end());
            }
        }

        return result.toString();
    }

    private String processStringPatterns(String input) {
        Pattern bracketPattern = Pattern.compile("\\{(\\w+)(?:=null)?}");
        Matcher bracketMatcher = bracketPattern.matcher(input);

        StringBuilder stringBuilder = new StringBuilder();
        while (bracketMatcher.find()) {
            String replacement = "{" + bracketMatcher.group(1) + "}";
            bracketMatcher.appendReplacement(stringBuilder, Matcher.quoteReplacement(replacement));
        }
        bracketMatcher.appendTail(stringBuilder);

        String result = stringBuilder.toString();
        result = result.replaceAll(",,+", ",").replaceAll("^,|,$", "");

        return result;
    }
}