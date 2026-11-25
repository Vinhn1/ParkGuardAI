package com.example.parkguardai.ai.ocr;

import android.graphics.Bitmap;

import com.google.android.gms.tasks.Task;
import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.text.Text;
import com.google.mlkit.vision.text.TextRecognition;
import com.google.mlkit.vision.text.TextRecognizer;
import com.google.mlkit.vision.text.latin.TextRecognizerOptions;

/** Đọc chữ Latin từ vùng ảnh do bảo vệ chọn; không tự xác nhận biển số. */
public final class PlateTextReader implements AutoCloseable {
    private final TextRecognizer recognizer =
            TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS);

    public Task<Text> read(Bitmap bitmap) {
        return recognizer.process(InputImage.fromBitmap(bitmap, 0));
    }

    @Override public void close() {
        recognizer.close();
    }
}
