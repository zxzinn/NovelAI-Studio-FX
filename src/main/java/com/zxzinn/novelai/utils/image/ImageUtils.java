package com.zxzinn.novelai.utils.image;

import com.google.gson.Gson;
import javafx.scene.image.Image;
import javafx.scene.image.PixelWriter;
import javafx.scene.image.WritableImage;
import lombok.extern.log4j.Log4j2;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.ImageOutputStream;
import java.awt.image.BufferedImage;
import java.awt.image.WritableRaster;
import java.io.*;
import java.util.Base64;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

@Log4j2
public class ImageUtils {

    public BufferedImage extractImageFromZip(byte[] zipData) throws IOException {
        try (ZipInputStream zis = new ZipInputStream(new ByteArrayInputStream(zipData))) {
            ZipEntry entry = zis.getNextEntry();
            if (entry == null) {
                throw new IOException("Zip 文件是空的");
            }

            return ImageIO.read(zis);
        }
    }

    public Image convertToFxImage(BufferedImage image) {
        WritableImage wr = new WritableImage(image.getWidth(), image.getHeight());
        PixelWriter pw = wr.getPixelWriter();
        for (int x = 0; x < image.getWidth(); x++) {
            for (int y = 0; y < image.getHeight(); y++) {
                pw.setArgb(x, y, image.getRGB(x, y));
            }
        }
        return wr;
    }

    public String imageToBase64(BufferedImage image) throws IOException {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        ImageIO.write(image, "png", outputStream);
        byte[] imageBytes = outputStream.toByteArray();
        return Base64.getEncoder().encodeToString(imageBytes);
    }

    public static void clearMetadata(BufferedImage image) {
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

    public BufferedImage loadImage(File file) throws IOException {
        return ImageIO.read(file);
    }
}