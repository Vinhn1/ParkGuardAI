package com.example.parkguardai.data.repository;

import android.content.Context;

import com.example.parkguardai.data.model.EvidenceImage;
import com.example.parkguardai.data.model.GuardDraft;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;

/** Lưu bản nháp trong noBackupFilesDir; chỉ xóa bằng hành động rõ của người dùng. */
public final class GuardDraftStore {
    private final File directory;

    public GuardDraftStore(Context context) {
        directory = new File(context.getNoBackupFilesDir(), "guard_drafts");
    }

    public String newId() { return UUID.randomUUID().toString(); }

    public File imageFile(String id, boolean crop) {
        requireId(id);
        return new File(directory, id + (crop ? "_crop.jpg" : "_original.jpg"));
    }

    public synchronized void save(GuardDraft draft) throws IOException {
        requireId(draft.id);
        if (!directory.exists() && !directory.mkdirs()) throw new IOException("Draft storage unavailable");
        JSONObject json = new JSONObject();
        try {
            json.put("id", draft.id);
            json.put("ownerUid", draft.ownerUid);
            json.put("capturedAtMillis", draft.capturedAtMillis);
            json.put("locationAtMillis", draft.locationAtMillis);
            json.put("latitude", draft.latitude);
            json.put("longitude", draft.longitude);
            json.put("accuracyMeters", draft.accuracyMeters);
            json.put("locationAccepted", draft.locationAccepted);
            json.put("originalName", draft.originalName);
            json.put("cropName", draft.cropName);
            json.put("plate", draft.plate);
            json.put("isBatch", draft.isBatch);
            if (draft.batchPlates != null && !draft.batchPlates.isEmpty()) {
                org.json.JSONArray platesArr = new org.json.JSONArray();
                for (String p : draft.batchPlates) {
                    platesArr.put(p);
                }
                json.put("batchPlates", platesArr);
            }
            json.put("typeCode", draft.typeCode);
            json.put("note", draft.note);
            json.put("scopeId", draft.scopeId);
            json.put("penaltyAmount", draft.penaltyAmount);
            json.put("sent", draft.sent);
            if (draft.originalUpload != null) json.put("originalUpload", imageJson(draft.originalUpload));
            if (draft.cropUpload != null) json.put("cropUpload", imageJson(draft.cropUpload));
        } catch (JSONException failure) {
            throw new IOException("Draft serialization failed", failure);
        }
        File temp = new File(directory, draft.id + ".tmp");
        try (FileOutputStream output = new FileOutputStream(temp)) {
            output.write(json.toString().getBytes(StandardCharsets.UTF_8));
            output.getFD().sync();
        }
        if (!temp.renameTo(jsonFile(draft.id))) throw new IOException("Draft write failed");
    }

    public synchronized GuardDraft load(String id, String ownerUid) throws IOException {
        requireId(id);
        File file = jsonFile(id);
        if (!file.isFile()) throw new IOException("Draft missing");
        try {
            JSONObject json = new JSONObject(new String(Files.readAllBytes(file.toPath()), StandardCharsets.UTF_8));
            GuardDraft draft = new GuardDraft();
            draft.id = json.getString("id");
            draft.ownerUid = json.getString("ownerUid");
            if (!ownerUid.equals(draft.ownerUid) || !id.equals(draft.id)) throw new IOException("Draft owner mismatch");
            draft.capturedAtMillis = json.getLong("capturedAtMillis");
            draft.locationAtMillis = json.optLong("locationAtMillis");
            draft.latitude = json.optDouble("latitude");
            draft.longitude = json.optDouble("longitude");
            draft.accuracyMeters = (float) json.optDouble("accuracyMeters");
            draft.locationAccepted = json.optBoolean("locationAccepted");
            draft.originalName = json.getString("originalName");
            draft.cropName = json.optString("cropName");
            draft.plate = json.optString("plate");
            draft.isBatch = json.optBoolean("isBatch", false);
            if (json.has("batchPlates")) {
                org.json.JSONArray platesArr = json.getJSONArray("batchPlates");
                for (int i = 0; i < platesArr.length(); i++) {
                    draft.batchPlates.add(platesArr.getString(i));
                }
            }
            draft.typeCode = json.optString("typeCode");
            draft.note = json.optString("note");
            draft.scopeId = json.optString("scopeId");
            draft.penaltyAmount = json.optString("penaltyAmount", "100.000 đ");
            draft.sent = json.optBoolean("sent");
            if (json.has("originalUpload")) draft.originalUpload = readImage(json.getJSONObject("originalUpload"));
            if (json.has("cropUpload")) draft.cropUpload = readImage(json.getJSONObject("cropUpload"));
            return draft;
        } catch (JSONException failure) {
            throw new IOException("Draft unreadable", failure);
        }
    }

    public synchronized List<GuardDraft> list(String ownerUid) {
        List<GuardDraft> drafts = new ArrayList<>();
        File[] files = directory.listFiles((folder, name) -> name.endsWith(".json"));
        if (files == null) return drafts;
        for (File file : files) {
            String id = file.getName().substring(0, file.getName().length() - 5);
            try { drafts.add(load(id, ownerUid)); }
            catch (IOException | IllegalArgumentException ignored) { /* Ẩn bản nháp lỗi hoặc tài khoản khác. */ }
        }
        drafts.sort((a, b) -> Long.compare(b.capturedAtMillis, a.capturedAtMillis));
        return drafts;
    }

    public synchronized boolean delete(String id, String ownerUid) throws IOException {
        load(id, ownerUid);
        boolean a = !imageFile(id, false).exists() || imageFile(id, false).delete();
        boolean b = !imageFile(id, true).exists() || imageFile(id, true).delete();
        boolean c = a && b && jsonFile(id).delete();
        return a && b && c;
    }

    private JSONObject imageJson(EvidenceImage image) throws JSONException {
        JSONObject json = new JSONObject();
        json.put("secureUrl", image.secureUrl);
        json.put("publicId", image.publicId);
        json.put("uploadedAt", image.uploadedAt.getTime());
        json.put("imageType", image.imageType);
        json.put("isPrimary", image.isPrimary);
        return json;
    }

    private EvidenceImage readImage(JSONObject json) throws JSONException {
        return new EvidenceImage(json.getString("secureUrl"), json.getString("publicId"),
                new Date(json.getLong("uploadedAt")), json.getString("imageType"),
                json.getBoolean("isPrimary"));
    }

    private File jsonFile(String id) { return new File(directory, id + ".json"); }

    private void requireId(String id) {
        if (id == null || !id.matches("[0-9a-fA-F-]{36}")) throw new IllegalArgumentException("Invalid draft ID");
    }
}
