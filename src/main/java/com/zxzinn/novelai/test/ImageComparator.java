package com.zxzinn.novelai.test;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

public class ImageComparator {

    public static void main(String[] args) {
        String image1Path = "C:\\Users\\User\\Downloads\\4E2749324E3B5C93214E1B0B94631212.png";
        String image2Path = "C:\\Users\\User\\Downloads\\FC52300CC729147BC911302A841DC878.png";
        String outputDir = "C:\\Users\\User\\Downloads\\difference_maps\\";

        try {
            compareImages(image1Path, image2Path, outputDir);
        } catch (IOException e) {
            System.out.println("圖片處理錯誤: " + e.getMessage());
        }
    }

    public static void compareImages(String imagePath1, String imagePath2, String outputDir) throws IOException {
        BufferedImage img1 = ImageIO.read(new File(imagePath1));
        BufferedImage img2 = ImageIO.read(new File(imagePath2));

        if (img1.getWidth() != img2.getWidth() || img1.getHeight() != img2.getHeight()) {
            System.out.println("圖片尺寸不同，無法比較。");
            return;
        }

        int width = img1.getWidth();
        int height = img1.getHeight();
        int totalPixels = width * height;

        BufferedImage diffRImage = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        BufferedImage diffGImage = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        BufferedImage diffBImage = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        BufferedImage diffAImage = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        BufferedImage diffAllImage = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);

        int[] maxDiff = new int[5]; // R, G, B, A, All
        int[] totalDiff = new int[5];
        int[] diffPixels = new int[5];

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int rgb1 = img1.getRGB(x, y);
                int rgb2 = img2.getRGB(x, y);

                int r1 = (rgb1 >> 16) & 0xff;
                int g1 = (rgb1 >> 8) & 0xff;
                int b1 = rgb1 & 0xff;
                int a1 = (rgb1 >> 24) & 0xff;

                int r2 = (rgb2 >> 16) & 0xff;
                int g2 = (rgb2 >> 8) & 0xff;
                int b2 = rgb2 & 0xff;
                int a2 = (rgb2 >> 24) & 0xff;

                int diffR = Math.abs(r1 - r2);
                int diffG = Math.abs(g1 - g2);
                int diffB = Math.abs(b1 - b2);
                int diffA = Math.abs(a1 - a2);
                int diffAll = diffR + diffG + diffB + diffA;

                updateDiffStats(diffR, 0, maxDiff, totalDiff, diffPixels);
                updateDiffStats(diffG, 1, maxDiff, totalDiff, diffPixels);
                updateDiffStats(diffB, 2, maxDiff, totalDiff, diffPixels);
                updateDiffStats(diffA, 3, maxDiff, totalDiff, diffPixels);
                updateDiffStats(diffAll, 4, maxDiff, totalDiff, diffPixels);

                setDiffPixel(diffRImage, x, y, diffR, maxDiff[0]);
                setDiffPixel(diffGImage, x, y, diffG, maxDiff[1]);
                setDiffPixel(diffBImage, x, y, diffB, maxDiff[2]);
                setDiffPixel(diffAImage, x, y, diffA, maxDiff[3]);
                setDiffPixel(diffAllImage, x, y, diffAll, maxDiff[4]);
            }
        }

        // 保存差異圖
        new File(outputDir).mkdirs(); // 確保輸出目錄存在
        ImageIO.write(diffRImage, "PNG", new File(outputDir + "diff_R.png"));
        ImageIO.write(diffGImage, "PNG", new File(outputDir + "diff_G.png"));
        ImageIO.write(diffBImage, "PNG", new File(outputDir + "diff_B.png"));
        ImageIO.write(diffAImage, "PNG", new File(outputDir + "diff_A.png"));
        ImageIO.write(diffAllImage, "PNG", new File(outputDir + "diff_All.png"));

        // 輸出統計信息
        String[] channels = {"R", "G", "B", "A", "All"};
        for (int i = 0; i < 5; i++) {
            double percentDiff = (double) diffPixels[i] / totalPixels * 100;
            double avgDiff = diffPixels[i] > 0 ? (double) totalDiff[i] / diffPixels[i] : 0;
            System.out.printf("%s 通道 - 差異像素: %d (%.2f%%), 平均差異: %.2f, 最大差異: %d\n",
                    channels[i], diffPixels[i], percentDiff, avgDiff, maxDiff[i]);
        }
    }

    private static void updateDiffStats(int diff, int index, int[] maxDiff, int[] totalDiff, int[] diffPixels) {
        if (diff > 0) {
            maxDiff[index] = Math.max(maxDiff[index], diff);
            totalDiff[index] += diff;
            diffPixels[index]++;
        }
    }

    private static void setDiffPixel(BufferedImage img, int x, int y, int diff, int maxDiff) {
        if (diff == 0) {
            img.setRGB(x, y, Color.BLACK.getRGB());
        } else {
            float hue = (1 - (float) diff / maxDiff) * 0.3f;
            int rgb = Color.HSBtoRGB(hue, 1.0f, 1.0f);
            img.setRGB(x, y, rgb);
        }
    }
}