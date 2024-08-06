package com.zxzinn.novelai.utils.embed;

import lombok.extern.log4j.Log4j2;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.error.YAMLException;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.Map;

@Log4j2
public class YamlLoader {
    private static final String EMBEDS_DIRECTORY = "embeds";

    public Map<String, Object> loadYamlFile(String tagName) throws FileNotFoundException, YAMLException {
        String yamlFilePath = EMBEDS_DIRECTORY + File.separator + tagName.replace("/", File.separator) + ".yml";
        File yamlFile = new File(yamlFilePath);
        String relativePath = "." + File.separator + yamlFilePath;

        if (!yamlFile.exists()) {
            throw new FileNotFoundException("YAML file not found: " + relativePath);
        }

        try (InputStream inputStream = new FileInputStream(yamlFile)) {
            Yaml yaml = new Yaml();
            Map<String, Object> yamlData = yaml.load(inputStream);
            log.debug("Loaded YAML file: {}", relativePath);
            return yamlData;
        } catch (YAMLException e) {
            log.error("Invalid YAML file: {}", relativePath, e);
            throw e;
        } catch (Exception e) {
            log.error("Error loading YAML file: {}", relativePath, e);
            throw new RuntimeException("Error loading YAML file: " + relativePath, e);
        }
    }
}