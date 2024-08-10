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
        try {
            BodyContentHandler handler = new BodyContentHandler();
            Metadata metadata = new Metadata();
            FileInputStream inputstream = new FileInputStream(file);
            ParseContext pcontext = new ParseContext();

            AutoDetectParser parser = new AutoDetectParser();
            parser.parse(inputstream, handler, metadata, pcontext);

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