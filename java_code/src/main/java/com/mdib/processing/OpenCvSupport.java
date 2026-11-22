package com.mdib.processing;

import nu.pattern.OpenCV;
import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.core.MatOfByte;
import org.opencv.imgcodecs.Imgcodecs;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Loads the OpenCV native library once and offers safe image I/O.
 *
 * <p>Images are read through {@code Files.readAllBytes} + {@code imdecode} instead of
 * {@code Imgcodecs.imread}, because {@code imread} cannot open paths containing
 * non-ASCII characters on Windows.
 */
public final class OpenCvSupport {

    private static volatile boolean loaded;

    private OpenCvSupport() {
    }

    /** Extracts and loads the bundled native OpenCV library (idempotent, thread-safe). */
    public static void ensureLoaded() {
        if (!loaded) {
            synchronized (OpenCvSupport.class) {
                if (!loaded) {
                    OpenCV.loadLocally();
                    loaded = true;
                }
            }
        }
    }

    /** @return the OpenCV version string, e.g. "4.9.0". */
    public static String version() {
        ensureLoaded();
        return Core.VERSION;
    }

    /**
     * Reads an image as an 8-bit, 3-channel BGR matrix.
     *
     * <p>The caller owns the returned {@link Mat} and must call {@link Mat#release()}
     * when done, since its pixel data lives in native memory.
     *
     * @throws IOException if the file cannot be read or is not a decodable image
     */
    public static Mat readBgr(Path path) throws IOException {
        ensureLoaded();
        byte[] bytes = Files.readAllBytes(path);
        MatOfByte encoded = new MatOfByte(bytes);
        Mat image;
        try {
            image = Imgcodecs.imdecode(encoded, Imgcodecs.IMREAD_COLOR);
        } finally {
            encoded.release();
        }
        if (image.empty()) {
            image.release();
            throw new IOException("Not a decodable image: " + path);
        }
        return image;
    }
}
