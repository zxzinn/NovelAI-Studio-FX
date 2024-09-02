package com.zxzinn.novelai.utils.image;

import lombok.extern.log4j.Log4j2;
import org.jetbrains.annotations.NotNull;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.awt.image.WritableRaster;
import java.io.*;
import java.util.Base64;

@Log4j2
public class ImageUtils {

    public static String imageToBase64(BufferedImage image) throws IOException {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        ImageIO.write(image, "png", outputStream);
        byte[] imageBytes = outputStream.toByteArray();
        return Base64.getEncoder().encodeToString(imageBytes);
    }

    public static void clearMetadata(@NotNull BufferedImage image) {
        WritableRaster raster = image.getRaster();
        int[] pixels = new int[raster.getNumBands()];

        for (int y = 0; y < raster.getHeight(); y++) {
            for (int x = 0; x < raster.getWidth(); x++) {
                raster.getPixel(x, y, pixels);
                for (int i = 0; i < pixels.length; i++) {
                    pixels[i] = pixels[i] & 0xFE;
                }
                raster.setPixel(x, y, pixels);
            }
        }
    }

    public static void saveImage(BufferedImage image, File outputFile) throws IOException {
        if (!ImageIO.write(image, "png", outputFile)) {
            throw new IOException("無法將圖像保存為PNG格式");
        }
    }

    public static BufferedImage loadImage(File file) throws IOException {
        return ImageIO.read(file);
    }
}