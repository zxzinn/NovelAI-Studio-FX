package com.zxzinn.novelai.utils.common;

import lombok.extern.log4j.Log4j2;

import java.io.*;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Log4j2
public class TxtProcessor {

    public static void mergeAndProcessTxtFiles(List<File> files, File outputFile) throws IOException {
        Set<String> uniqueTags = new HashSet<>();

        for (File file : files) {
            try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    uniqueTags.add(line.trim());
                }
            }
        }

        try (BufferedWriter writer = new BufferedWriter(new FileWriter(outputFile))) {
            for (String tag : uniqueTags) {
                writer.write(tag);
                writer.newLine();
            }
        }
    }
}