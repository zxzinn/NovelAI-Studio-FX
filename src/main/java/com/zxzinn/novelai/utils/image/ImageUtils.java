package com.zxzinn.novelai.utils.image;

import lombok.extern.log4j.Log4j2;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

@Log4j2
public class ImageUtils {

    public static BufferedImage extractImageFromZip(byte[] zipData) throws IOException {
        try (ZipInputStream zis = new ZipInputStream(new ByteArrayInputStream(zipData))) {
            ZipEntry entry = zis.getNextEntry();
            if (entry == null) {
                throw new IOException("Zip 文件是空的");
            }

            return ImageIO.read(zis);
        }
    }

    public static void saveImage(BufferedImage image, String fileName) throws IOException {
        if (image == null) {
            log.error("無法保存圖像：圖像對象為null");
            return;
        }

        File outputDir = new File("output");
        if (!outputDir.exists() && !outputDir.mkdir()) {
            log.error("無法創建輸出目錄");
            return;
        }

        File outputFile = new File(outputDir, fileName);

        if (!ImageIO.write(image, "png", outputFile)) {
            log.error("沒有合適的寫入器來保存PNG圖像");
        }
    }
}