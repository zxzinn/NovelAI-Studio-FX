package com.zxzinn.novelai.utils.image;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import javafx.scene.image.Image;
import javafx.scene.image.PixelWriter;
import javafx.scene.image.WritableImage;
import lombok.extern.log4j.Log4j2;
import org.yaml.snakeyaml.Yaml;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.ImageOutputStream;
import java.awt.image.BufferedImage;
import java.awt.image.WritableRaster;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.zip.GZIPInputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

@Log4j2
public class ImageUtils {

    private final Gson gson = new Gson();

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

    public BufferedImage loadImage(File file) throws IOException {
        return ImageIO.read(file);
    }

    public String imageToBase64(BufferedImage image) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ImageIO.write(image, "png", baos);
        byte[] imageBytes = baos.toByteArray();
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
        ImageWriter writer = ImageIO.getImageWritersByFormatName("png").next();
        ImageWriteParam writeParam = writer.getDefaultWriteParam();
        ImageOutputStream outputStream = ImageIO.createImageOutputStream(outputFile);
        writer.setOutput(outputStream);
        IIOImage iioImage = new IIOImage(image, null, null);
        writer.write(null, iioImage, writeParam);
        writer.dispose();
        outputStream.close();
    }

    public String getLSBMetadata(BufferedImage image) throws IOException {
        byte[] alphaChannel = extractAlphaChannel(image);
        byte[] metadataBytes = extractMetadataFromAlpha(alphaChannel);
        String jsonMetadata = decompressAndParseMetadata(metadataBytes);
        return formatMetadata(jsonMetadata);
    }

    private byte[] extractAlphaChannel(BufferedImage image) {
        int width = image.getWidth();
        int height = image.getHeight();
        byte[] alphaChannel = new byte[width * height];
        int index = 0;

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int pixel = image.getRGB(x, y);
                int alpha = (pixel >> 24) & 0xFF;
                alphaChannel[index++] = (byte) (alpha & 1);
            }
        }
        return alphaChannel;
    }

    private byte[] extractMetadataFromAlpha(byte[] alphaChannel) throws IOException {
        ByteArrayOutputStream metadataStream = new ByteArrayOutputStream();
        String magic = "stealth_pngcomp";
        int magicLength = magic.length();
        int dataLength;

        // 檢查魔術數字
        for (int i = 0; i < magicLength; i++) {
            if ((char) alphaChannel[i] != magic.charAt(i)) {
                throw new IOException("Invalid magic number");
            }
        }

        // 讀取數據長度
        dataLength = readInt32(alphaChannel, magicLength);

        // 讀取元數據
        for (int i = 0; i < dataLength; i++) {
            int byteValue = 0;
            for (int j = 0; j < 8; j++) {
                byteValue |= (alphaChannel[magicLength + 4 + i * 8 + j] & 1) << (7 - j);
            }
            metadataStream.write(byteValue);
        }

        return metadataStream.toByteArray();
    }

    private int readInt32(byte[] data, int offset) {
        return ((data[offset] & 1) << 31) |
                ((data[offset + 1] & 1) << 30) |
                ((data[offset + 2] & 1) << 29) |
                ((data[offset + 3] & 1) << 28);
    }

    private String decompressAndParseMetadata(byte[] compressedMetadata) throws IOException {
        try (GZIPInputStream gzipInputStream = new GZIPInputStream(new ByteArrayInputStream(compressedMetadata));
             ByteArrayOutputStream resultStream = new ByteArrayOutputStream()) {
            byte[] buffer = new byte[1024];
            int length;
            while ((length = gzipInputStream.read(buffer)) > 0) {
                resultStream.write(buffer, 0, length);
            }
            return resultStream.toString(StandardCharsets.UTF_8);
        }
    }

    private String formatMetadata(String jsonMetadata) {
        JsonObject jsonObject = gson.fromJson(jsonMetadata, JsonObject.class);
        Map<String, Object> map = convertJsonObjectToMap(jsonObject);
        Yaml yaml = new Yaml();
        return yaml.dump(map);
    }

    private Map<String, Object> convertJsonObjectToMap(JsonObject jsonObject) {
        Map<String, Object> map = new HashMap<>();
        for (Map.Entry<String, JsonElement> entry : jsonObject.entrySet()) {
            if (entry.getValue().isJsonObject()) {
                map.put(entry.getKey(), convertJsonObjectToMap(entry.getValue().getAsJsonObject()));
            } else if (entry.getValue().isJsonArray()) {
                map.put(entry.getKey(), gson.fromJson(entry.getValue(), Object[].class));
            } else if (entry.getValue().isJsonPrimitive()) {
                JsonElement element = entry.getValue();
                if (element.getAsJsonPrimitive().isBoolean()) {
                    map.put(entry.getKey(), element.getAsBoolean());
                } else if (element.getAsJsonPrimitive().isNumber()) {
                    map.put(entry.getKey(), element.getAsNumber());
                } else {
                    map.put(entry.getKey(), element.getAsString());
                }
            } else if (entry.getValue().isJsonNull()) {
                map.put(entry.getKey(), null);
            }
        }
        return map;
    }
}