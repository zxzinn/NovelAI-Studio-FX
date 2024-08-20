package com.zxzinn.novelai.service.filemanager;

import lombok.extern.log4j.Log4j2;
import org.apache.tika.metadata.Metadata;
import org.apache.tika.parser.AutoDetectParser;
import org.apache.tika.parser.ParseContext;
import org.apache.tika.sax.BodyContentHandler;

import java.io.File;
import java.io.FileInputStream;
import java.util.ArrayList;
import java.util.List;

@Log4j2
public class MetadataService {

    public List<String> getMetadata(File file) {
        List<String> metadataList = new ArrayList<>();
        if (file == null || !file.exists()) {
            metadataList.add("文件不存在或無法訪問");
            return metadataList;
        }

        if (file.isDirectory()) {
            metadataList.add("名稱: " + file.getName());
            metadataList.add("路徑: " + file.getAbsolutePath());
            metadataList.add("類型: 目錄");
            metadataList.add("最後修改時間: " + new java.util.Date(file.lastModified()));
            return metadataList;
        }

        try (FileInputStream fileinputstream = new FileInputStream(file)) {
            BodyContentHandler handler = new BodyContentHandler();
            Metadata metadata = new Metadata();
            ParseContext context = new ParseContext();

            AutoDetectParser parser = new AutoDetectParser();
            parser.parse(fileinputstream, handler, metadata, context);

            for (String name : metadata.names()) {
                metadataList.add(name + ": " + metadata.get(name));
            }
        } catch (Exception e) {
            log.error("無法讀取檔案元數據", e);
            metadataList.add("無法讀取元數據：" + e.getMessage());
        }
        return metadataList;
    }
}