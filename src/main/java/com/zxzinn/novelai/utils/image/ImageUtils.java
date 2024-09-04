package com.zxzinn.novelai.utils.image;

import lombok.extern.log4j.Log4j2;
import org.jetbrains.annotations.NotNull;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.awt.image.WritableRaster;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Base64;
import java.util.Optional;


@Log4j2
public class ImageUtils {
    private static final String FILE_NAME_FORMAT = "NovelAI_FX_generated_%s.png";
    private static final DateTimeFormatter FILE_NAME_TIMESTAMP_FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss");

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

    public static Optional<File> saveImage(byte[] imageData, String outputDir) {
        try {
            Path outputPath = ensureOutputDirectoryExists(outputDir);
            File outputFile = createOutputFile(outputPath);
            writeImageDataToFile(imageData, outputFile);
            return Optional.of(outputFile);
        } catch (IOException e) {
            log.error("保存圖像時發生錯誤：{}", e.getMessage(), e);
            return Optional.empty();
        }
    }

    private static Path ensureOutputDirectoryExists(String outputDir) throws IOException {
        Path outputPath = Paths.get(outputDir);
        Files.createDirectories(outputPath);
        return outputPath;
    }

    private static File createOutputFile(Path outputPath) {
        String fileName = generateFileName();
        return outputPath.resolve(fileName).toFile();
    }

    private static String generateFileName() {
        String timeStamp = LocalDateTime.now().format(FILE_NAME_TIMESTAMP_FORMATTER);
        return String.format(FILE_NAME_FORMAT, timeStamp);
    }

    private static void writeImageDataToFile(byte[] imageData, File outputFile) throws IOException {
        Files.write(outputFile.toPath(), imageData);
    }
}