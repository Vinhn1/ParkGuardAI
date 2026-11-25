package com.example.parkguardai.ai.model;

import org.tensorflow.lite.DataType;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;

/** Hợp đồng tensor đo trực tiếp từ asset pilot; từ chối model khác trước suy luận. */
public final class PlateModelContract {
    public static final String ASSET_NAME = "parkguard_yolo11n_best.tflite";
    private static final String SHA256 =
            "f2735814b03e6df5f0c8c3e195a53bade43b58236f1a343532342016720efb50";
    public static final int INPUT_SIZE = 640;
    public static final int OUTPUT_CHANNELS = 5;
    public static final int OUTPUT_CANDIDATES = 8400;

    private PlateModelContract() { }

    public static void requireChecksum(byte[] model) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(model);
            StringBuilder hex = new StringBuilder();
            for (byte value : digest) hex.append(String.format("%02x", value & 255));
            if (!SHA256.contentEquals(hex)) {
                throw new IllegalArgumentException("Plate model asset changed");
            }
        } catch (NoSuchAlgorithmException impossible) {
            throw new IllegalStateException(impossible);
        }
    }

    public static void requireCompatible(int[] input, DataType inputType,
                                         int[] output, DataType outputType) {
        if (!Arrays.equals(input, new int[] {1, 3, INPUT_SIZE, INPUT_SIZE})
                || !Arrays.equals(output, new int[] {1, OUTPUT_CHANNELS, OUTPUT_CANDIDATES})
                || inputType != DataType.FLOAT32 || outputType != DataType.FLOAT32) {
            throw new IllegalArgumentException("Plate model tensor contract changed");
        }
    }
}
