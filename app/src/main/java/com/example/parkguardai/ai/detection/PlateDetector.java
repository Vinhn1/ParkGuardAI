package com.example.parkguardai.ai.detection;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;

import com.example.parkguardai.ai.model.PlateModelContract;

import org.tensorflow.lite.DataType;
import org.tensorflow.lite.Interpreter;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/** Suy luận vùng biển số trên ảnh tĩnh; kết quả chỉ là gợi ý để người dùng chọn. */
public final class PlateDetector implements AutoCloseable {
    public static final class Detection {
        public final RectF box;
        public final float score;

        Detection(RectF box, float score) {
            this.box = box;
            this.score = score;
        }
    }

    private static final float MIN_SCORE = 0.25f;
    private static final float NMS_IOU = 0.45f;
    private static final int MAX_RESULTS = 3;
    private final ByteBuffer modelBuffer;
    private final Interpreter interpreter;

    public PlateDetector(Context context) throws IOException {
        try (InputStream stream = context.getApplicationContext().getAssets()
                .open(PlateModelContract.ASSET_NAME);
             ByteArrayOutputStream bytes = new ByteArrayOutputStream()) {
            byte[] chunk = new byte[8192];
            int count;
            while ((count = stream.read(chunk)) != -1) bytes.write(chunk, 0, count);
            byte[] model = bytes.toByteArray();
            PlateModelContract.requireChecksum(model);
            modelBuffer = ByteBuffer.allocateDirect(model.length).order(ByteOrder.nativeOrder());
            modelBuffer.put(model);
            modelBuffer.rewind();
        }
        Interpreter created = null;
        try {
            created = new Interpreter(modelBuffer, new Interpreter.Options().setNumThreads(2));
            PlateModelContract.requireCompatible(created.getInputTensor(0).shape(),
                    created.getInputTensor(0).dataType(),
                    created.getOutputTensor(0).shape(),
                    created.getOutputTensor(0).dataType());
        } catch (RuntimeException failure) {
            if (created != null) created.close();
            throw new IOException("Plate model unavailable or incompatible", failure);
        }
        interpreter = created;
    }

    public List<Detection> detect(Bitmap source) {
        if (source == null || source.isRecycled()) throw new IllegalArgumentException("Image unavailable");
        int size = PlateModelContract.INPUT_SIZE;
        float scale = Math.min((float) size / source.getWidth(), (float) size / source.getHeight());
        int scaledWidth = Math.max(1, Math.round(source.getWidth() * scale));
        int scaledHeight = Math.max(1, Math.round(source.getHeight() * scale));
        float padX = (size - scaledWidth) / 2f;
        float padY = (size - scaledHeight) / 2f;
        Bitmap letterbox = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888);
        try {
            Canvas canvas = new Canvas(letterbox);
            canvas.drawColor(Color.rgb(114, 114, 114));
            Paint paint = new Paint(Paint.FILTER_BITMAP_FLAG);
            canvas.drawBitmap(source, null,
                    new RectF(padX, padY, padX + scaledWidth, padY + scaledHeight), paint);
            int[] pixels = new int[size * size];
            letterbox.getPixels(pixels, 0, size, 0, 0, size, size);
            float[][][][] input = new float[1][3][size][size];
            for (int y = 0; y < size; y++) {
                for (int x = 0; x < size; x++) {
                    int color = pixels[y * size + x];
                    input[0][0][y][x] = Color.red(color) / 255f;
                    input[0][1][y][x] = Color.green(color) / 255f;
                    input[0][2][y][x] = Color.blue(color) / 255f;
                }
            }
            float[][][] output = new float[1][PlateModelContract.OUTPUT_CHANNELS]
                    [PlateModelContract.OUTPUT_CANDIDATES];
            interpreter.run(input, output);
            return decode(output[0], source.getWidth(), source.getHeight(), scale, padX, padY);
        } finally {
            letterbox.recycle();
        }
    }

    private List<Detection> decode(float[][] output, int width, int height,
                                   float scale, float padX, float padY) {
        List<Detection> proposals = new ArrayList<>();
        int size = PlateModelContract.INPUT_SIZE;
        for (int index = 0; index < PlateModelContract.OUTPUT_CANDIDATES; index++) {
            float score = output[4][index];
            float cx = output[0][index], cy = output[1][index];
            float boxWidth = output[2][index], boxHeight = output[3][index];
            if (!Float.isFinite(score) || score < MIN_SCORE || score > 1f
                    || !Float.isFinite(cx) || !Float.isFinite(cy)
                    || !Float.isFinite(boxWidth) || !Float.isFinite(boxHeight)
                    || boxWidth <= 0 || boxHeight <= 0) continue;
            float left = ((cx - boxWidth / 2f) * size - padX) / scale;
            float top = ((cy - boxHeight / 2f) * size - padY) / scale;
            float right = ((cx + boxWidth / 2f) * size - padX) / scale;
            float bottom = ((cy + boxHeight / 2f) * size - padY) / scale;
            RectF box = new RectF(Math.max(0f, left), Math.max(0f, top),
                    Math.min(width, right), Math.min(height, bottom));
            if (box.width() < 20f || box.height() < 12f) continue;
            proposals.add(new Detection(box, score));
        }
        proposals.sort(Comparator.comparingDouble((Detection item) -> item.score).reversed());
        List<Detection> selected = new ArrayList<>();
        for (Detection proposal : proposals) {
            boolean overlaps = false;
            for (Detection kept : selected) {
                if (intersectionOverUnion(proposal.box, kept.box) > NMS_IOU) {
                    overlaps = true;
                    break;
                }
            }
            if (!overlaps) selected.add(proposal);
            if (selected.size() == MAX_RESULTS) break;
        }
        return selected;
    }

    private float intersectionOverUnion(RectF first, RectF second) {
        float left = Math.max(first.left, second.left);
        float top = Math.max(first.top, second.top);
        float right = Math.min(first.right, second.right);
        float bottom = Math.min(first.bottom, second.bottom);
        float area = Math.max(0f, right - left) * Math.max(0f, bottom - top);
        float union = first.width() * first.height() + second.width() * second.height() - area;
        return union <= 0f ? 0f : area / union;
    }

    @Override public void close() {
        interpreter.close();
    }
}
