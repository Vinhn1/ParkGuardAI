package com.example.parkguardai.ai.ocr;

import android.graphics.Rect;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.google.mlkit.vision.text.Text;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Trích xuất và chuẩn hóa biển số xe Việt Nam từ kết quả nhận dạng văn bản ML Kit.
 * Xử lý đặc biệt cho biển số xe máy 2 dòng (ví dụ 48-B1 999.99, 59-P1 123.45)
 * và biển số ô tô 1 dòng (ví dụ 69A-123.45, 30A-999.99).
 * Tích hợp bộ quy tắc sửa lỗi OCR cho các ký tự bị nhận diện nhầm thường gặp.
 */
public final class PlateParser {
    private static final Set<String> IGNORED_WORDS = new HashSet<>(Arrays.asList(
            "HONDA", "YAMAHA", "SUZUKI", "PIAGGIO", "VESPA", "SYM", "VINFAST",
            "KAWASAKI", "DUCATI", "WAVE", "BLADE", "VISION", "LEAD", "SH",
            "AIRBLADE", "EXCITER", "WINNER", "SIRIUS", "JANUS", "GRANDE",
            "VIETNAM", "DONGNAI", "SAIGON", "HANOI"
    ));

    public static final class Result {
        public final String formattedPlate;
        public final String cleanPlate;
        @Nullable public final Rect boundingBox;

        public Result(String formattedPlate, String cleanPlate, @Nullable Rect boundingBox) {
            this.formattedPlate = formattedPlate;
            this.cleanPlate = cleanPlate;
            this.boundingBox = boundingBox;
        }
    }

    private PlateParser() { }

    @Nullable
    public static Result parse(Text textResult) {
        if (textResult == null) return null;

        List<Text.Line> allLines = new ArrayList<>();
        for (Text.TextBlock block : textResult.getTextBlocks()) {
            allLines.addAll(block.getLines());
        }

        // Sắp xếp các dòng theo trục Y (từ trên xuống dưới trong khung ảnh)
        Collections.sort(allLines, new Comparator<Text.Line>() {
            @Override
            public int compare(Text.Line l1, Text.Line l2) {
                Rect b1 = l1.getBoundingBox();
                Rect b2 = l2.getBoundingBox();
                int y1 = b1 != null ? b1.top : 0;
                int y2 = b2 != null ? b2.top : 0;
                return Integer.compare(y1, y2);
            }
        });

        // =========================================================================
        // BƯỚC 1: Ưu tiên tìm biển số xe máy 2 dòng Việt Nam (ghép Dòng trên + Dòng dưới)
        // Dòng trên: Mã tỉnh (2 số) + Sê-ri (1 chữ + 1 số hoặc 2 chữ) -> ví dụ: 48-B1, 59-P1, 29-A1
        // Dòng dưới: 4 hoặc 5 số đăng ký -> ví dụ: 999.99, 123.45, 8888
        // =========================================================================
        for (int i = 0; i < allLines.size(); i++) {
            Text.Line lineTop = allLines.get(i);
            String rawTop = lineTop.getText();
            if (isIgnored(rawTop)) continue;

            String correctedTop = correctMotorcycleTop(rawTop);
            if (correctedTop != null) {
                // Đã tìm thấy ứng viên dòng trên, tìm dòng dưới tiếp theo
                for (int j = 0; j < allLines.size(); j++) {
                    if (i == j) continue;
                    Text.Line lineBottom = allLines.get(j);
                    Rect boxTop = lineTop.getBoundingBox();
                    Rect boxBottom = lineBottom.getBoundingBox();

                    // Dòng dưới phải nằm phía dưới dòng trên
                    if (boxTop != null && boxBottom != null && boxBottom.top < boxTop.top) {
                        continue;
                    }

                    String rawBottom = lineBottom.getText();
                    if (isIgnored(rawBottom)) continue;

                    String correctedBottom = correctPlateDigits(rawBottom);
                    if (correctedBottom != null && (correctedBottom.length() == 4 || correctedBottom.length() == 5)) {
                        Rect combinedBox = null;
                        if (boxTop != null && boxBottom != null) {
                            combinedBox = new Rect(
                                    Math.min(boxTop.left, boxBottom.left),
                                    Math.min(boxTop.top, boxBottom.top),
                                    Math.max(boxTop.right, boxBottom.right),
                                    Math.max(boxTop.bottom, boxBottom.bottom)
                            );
                        }
                        String formatted = formatMotorcyclePlate(correctedTop, correctedBottom);
                        return new Result(formatted, correctedTop + correctedBottom, combinedBox);
                    }
                }
            }
        }

        // =========================================================================
        // BƯỚC 2: Tìm biển số 1 dòng (Ô tô hoặc xe máy viết liền một hàng)
        // Ví dụ: 69A-123.45, 51F-999.99, 48B1-99999
        // =========================================================================
        for (Text.Line line : allLines) {
            String raw = line.getText();
            if (isIgnored(raw)) continue;

            Result singleResult = parseSingleLineText(raw, line.getBoundingBox());
            if (singleResult != null) {
                return singleResult;
            }
        }

        // =========================================================================
        // BƯỚC 3: Phân tích toàn bộ văn bản thô (kể cả dấu xuống dòng \n và khoảng trắng)
        // =========================================================================
        String fullRaw = textResult.getText();
        Result rawResult = parseRawFullText(fullRaw);
        if (rawResult != null) {
            return rawResult;
        }

        return null;
    }

    /**
     * Chuẩn hóa và sửa lỗi OCR cho dòng trên biển số xe máy (Ví dụ: "48-B1", "L8-B1" -> "48-B1")
     * Dòng trên xe máy gồm: 2 số mã tỉnh (11-99) + sê-ri (1 chữ + 1 số hoặc 2 chữ cái).
     */
    @Nullable
    public static String correctMotorcycleTop(String raw) {
        if (raw == null) return null;
        String clean = raw.toUpperCase(Locale.ROOT).replaceAll("[^A-Z0-9]", "");
        if (clean.length() < 3 || clean.length() > 5) return null;

        char[] chars = clean.toCharArray();

        // 2 ký tự đầu BẮT BUỘC là số mã tỉnh (11-99)
        chars[0] = fixCharToDigit(chars[0]);
        chars[1] = fixCharToDigit(chars[1]);

        if (!Character.isDigit(chars[0]) || !Character.isDigit(chars[1])) {
            return null;
        }

        // Ký tự thứ 3 BẮT BUỘC là chữ cái sê-ri (A-Z)
        chars[2] = fixDigitToChar(chars[2]);
        if (!Character.isLetter(chars[2])) {
            return null;
        }

        // Ký tự thứ 4 (nếu có): có thể là số (ví dụ B1) hoặc chữ (ví dụ AA)
        if (chars.length >= 4) {
            // Thường là số (ví dụ 1, 2, 3...) trừ các biển đặc biệt 2 chữ cái
            if (Character.isDigit(chars[3]) || isCommonDigitConfusion(chars[3])) {
                chars[3] = fixCharToDigit(chars[3]);
            }
        }

        String fixed = new String(chars);

        // Định dạng đẹp: 2 số mã tỉnh + "-" + sê-ri (ví dụ: 48-B1, 59-P1)
        if (fixed.length() >= 4) {
            return fixed.substring(0, 2) + "-" + fixed.substring(2);
        } else if (fixed.length() == 3) {
            return fixed.substring(0, 2) + "-" + fixed.substring(2);
        }
        return fixed;
    }

    /**
     * Chuẩn hóa dòng dưới (Dãy 4 hoặc 5 số đăng ký).
     * Sửa tất cả các ký tự chữ bị nhận diện nhầm thành số (ví dụ 'B' -> '8', 'O' -> '0').
     */
    @Nullable
    public static String correctPlateDigits(String raw) {
        if (raw == null) return null;
        String clean = raw.toUpperCase(Locale.ROOT).replaceAll("[^A-Z0-9]", "");
        if (clean.length() < 4 || clean.length() > 6) return null;

        char[] chars = clean.toCharArray();
        StringBuilder digits = new StringBuilder();
        for (char c : chars) {
            char d = fixCharToDigit(c);
            if (Character.isDigit(d)) {
                digits.append(d);
            }
        }

        String res = digits.toString();
        if (res.length() == 4 || res.length() == 5) {
            return res;
        }
        return null;
    }

    /**
     * Phân tích biển số trên một dòng (cả ô tô lẫn xe máy viết liền)
     */
    @Nullable
    private static Result parseSingleLineText(String raw, @Nullable Rect box) {
        if (raw == null) return null;
        String clean = raw.toUpperCase(Locale.ROOT).replaceAll("[^A-Z0-9]", "");
        if (clean.length() < 7 || clean.length() > 10) return null;

        char[] chars = clean.toCharArray();
        // Sửa 2 ký tự đầu thành số
        chars[0] = fixCharToDigit(chars[0]);
        chars[1] = fixCharToDigit(chars[1]);

        // Ký tự thứ 3 là chữ
        chars[2] = fixDigitToChar(chars[2]);

        if (!Character.isDigit(chars[0]) || !Character.isDigit(chars[1]) || !Character.isLetter(chars[2])) {
            return null;
        }

        String fixed = new String(chars);

        // Kiểm tra mẫu xe máy 1 dòng: 2 số + 1 chữ + 1 số + 4/5 số (ví dụ: 48B199999)
        Pattern motoPattern = Pattern.compile("^([0-9]{2}[A-Z][0-9])([0-9]{4,5})$");
        Matcher mMoto = motoPattern.matcher(fixed);
        if (mMoto.find()) {
            String p1 = mMoto.group(1);
            String p2 = mMoto.group(2);
            String formatted = p1.substring(0, 2) + "-" + p1.substring(2) + " " + formatNumberPart(p2);
            return new Result(formatted, fixed, box);
        }

        // Kiểm tra mẫu ô tô 1 dòng: 2 số + 1 chữ + 4/5 số (ví dụ: 69A12345, 30A99999)
        Pattern carPattern = Pattern.compile("^([0-9]{2}[A-Z])([0-9]{4,5})$");
        Matcher mCar = carPattern.matcher(fixed);
        if (mCar.find()) {
            String p1 = mCar.group(1);
            String p2 = mCar.group(2);
            String formatted = p1 + "-" + formatNumberPart(p2);
            return new Result(formatted, fixed, box);
        }

        return null;
    }

    /**
     * Phân tích văn bản thô đa dòng (khi ML Kit trả về chuỗi có chứa \n)
     */
    @Nullable
    private static Result parseRawFullText(String raw) {
        if (raw == null) return null;
        String[] lines = raw.split("[\r\n]+");
        String candidateTop = null;
        String candidateBottom = null;

        for (String line : lines) {
            String trimmed = line.trim();
            if (isIgnored(trimmed)) continue;

            if (candidateTop == null) {
                String top = correctMotorcycleTop(trimmed);
                if (top != null) {
                    candidateTop = top;
                    continue;
                }
            }

            if (candidateBottom == null) {
                String bottom = correctPlateDigits(trimmed);
                if (bottom != null && (bottom.length() == 4 || bottom.length() == 5)) {
                    candidateBottom = bottom;
                }
            }
        }

        if (candidateTop != null && candidateBottom != null) {
            String formatted = formatMotorcyclePlate(candidateTop, candidateBottom);
            String clean = candidateTop.replaceAll("[^A-Z0-9]", "") + candidateBottom;
            return new Result(formatted, clean, null);
        }

        // Thử dạng 1 dòng trên toàn bộ text đã làm sạch
        String cleanAll = raw.toUpperCase(Locale.ROOT).replaceAll("[^A-Z0-9]", "");
        return parseSingleLineText(cleanAll, null);
    }

    /**
     * Sửa các ký tự chữ bị OCR nhận diện nhầm thành số tương ứng.
     */
    public static char fixCharToDigit(char c) {
        switch (c) {
            case 'L':
            case 'A':
                return '4'; // Chữ L và A trong font biển số thường là số 4
            case 'O':
            case 'D':
            case 'Q':
            case 'U':
                return '0';
            case 'I':
            case 'l':
            case '|':
            case 'T':
            case 'J':
                return '1';
            case 'Z':
                return '2';
            case 'E':
                return '3';
            case 'S':
                return '5';
            case 'G':
            case 'b':
                return '6';
            case 'B':
                return '8';
            case 'q':
            case 'g':
                return '9';
            default:
                return c;
        }
    }

    /**
     * Sửa các chữ số bị OCR nhận diện nhầm thành chữ cái sê-ri.
     */
    public static char fixDigitToChar(char c) {
        switch (c) {
            case '8': return 'B';
            case '0': return 'D';
            case '1': return 'T';
            case '5': return 'S';
            case '6': return 'G';
            case '2': return 'Z';
            case '4': return 'A';
            default: return c;
        }
    }

    /**
     * Chuẩn hóa chuỗi biển số xe nhập thủ công hoặc từ OCR về định dạng chuẩn Việt Nam.
     * Ví dụ: "48b199999" -> "48-B1 999.99", "54l43939" -> "54-L4 3939", "69a12345" -> "69A-123.45".
     */
    @NonNull
    public static String normalizePlate(String input) {
        if (input == null || input.trim().isEmpty()) return "";
        Result res = parseRawFullText(input.trim());
        if (res != null && !res.formattedPlate.isEmpty()) {
            return res.formattedPlate;
        }
        return input.trim().toUpperCase(Locale.ROOT);
    }

    private static boolean isCommonDigitConfusion(char c) {
        return c == 'I' || c == 'L' || c == 'O' || c == 'B' || c == 'S' || c == 'Z' || c == 'T';
    }

    private static boolean isIgnored(String text) {
        if (text == null) return true;
        String upper = text.toUpperCase(Locale.ROOT);
        for (String ignored : IGNORED_WORDS) {
            if (upper.contains(ignored)) return true;
        }
        return false;
    }

    private static String formatNumberPart(String numbers) {
        if (numbers.length() == 5) {
            return numbers.substring(0, 3) + "." + numbers.substring(3);
        }
        return numbers;
    }

    private static String formatMotorcyclePlate(String top, String bottom) {
        String numFormatted = formatNumberPart(bottom);
        // top đã có dạng "48-B1"
        return top + " " + numFormatted;
    }
}
