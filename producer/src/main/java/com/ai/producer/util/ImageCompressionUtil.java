package com.ai.producer.util;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

@Component
@Slf4j
public class ImageCompressionUtil {

    private static final int MAX_WIDTH = 800;
    private static final int MAX_HEIGHT = 600;
    private static final int MAX_FILE_SIZE = 500 * 1024; // 500KB

    public byte[] compressImage(byte[] originalImage) throws IOException {
        try {
            // Read original image
            BufferedImage original = ImageIO.read(new ByteArrayInputStream(originalImage));
            if (original == null) {
                throw new IOException("Invalid image format");
            }

            // Calculate new dimensions while maintaining aspect ratio
            Dimension newDimensions = calculateDimensions(original.getWidth(), original.getHeight());

            // Resize image
            BufferedImage resized = new BufferedImage(
                    newDimensions.width, newDimensions.height, BufferedImage.TYPE_INT_RGB);

            Graphics2D g2d = resized.createGraphics();
            g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            g2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2d.drawImage(original, 0, 0, newDimensions.width, newDimensions.height, null);
            g2d.dispose();

            // Compress with different quality levels until under size limit
            float quality = 0.8f;
            byte[] compressed;

            do {
                compressed = compressWithQuality(resized, quality);
                quality -= 0.1f;

                if (quality < 0.3f) break; // Don't go below 30% quality

            } while (compressed.length > MAX_FILE_SIZE);

            log.info("Image compressed: Original size: {} KB, Compressed size: {} KB, Quality: {}%",
                    originalImage.length / 1024, compressed.length / 1024, (quality + 0.1f) * 100);

            return compressed;

        } catch (Exception e) {
            log.error("Error compressing image", e);
            throw new IOException("Failed to compress image: " + e.getMessage());
        }
    }

    private Dimension calculateDimensions(int originalWidth, int originalHeight) {
        double widthRatio = (double) MAX_WIDTH / originalWidth;
        double heightRatio = (double) MAX_HEIGHT / originalHeight;
        double ratio = Math.min(widthRatio, heightRatio);

        if (ratio >= 1.0) {
            // Image is already smaller than max dimensions
            return new Dimension(originalWidth, originalHeight);
        }

        int newWidth = (int) (originalWidth * ratio);
        int newHeight = (int) (originalHeight * ratio);

        return new Dimension(newWidth, newHeight);
    }

    private byte[] compressWithQuality(BufferedImage image, float quality) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();

        var writers = ImageIO.getImageWritersByFormatName("jpg");
        if (!writers.hasNext()) {
            throw new IOException("No JPEG writer found");
        }

        var writer = writers.next();
        var ios = ImageIO.createImageOutputStream(baos);
        writer.setOutput(ios);

        var param = writer.getDefaultWriteParam();
        param.setCompressionMode(javax.imageio.ImageWriteParam.MODE_EXPLICIT);
        param.setCompressionQuality(quality);

        writer.write(null, new javax.imageio.IIOImage(image, null, null), param);
        writer.dispose();
        ios.close();

        return baos.toByteArray();
    }
}

