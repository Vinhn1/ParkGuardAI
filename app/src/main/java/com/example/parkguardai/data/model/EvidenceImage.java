package com.example.parkguardai.data.model;

import java.util.Date;

/** Biên nhận ảnh từ Cloudinary, chỉ tạo sau callback upload thành công. */
public final class EvidenceImage {
    public final String secureUrl;
    public final String publicId;
    public final Date uploadedAt;
    public final String imageType;
    public final boolean isPrimary;

    public EvidenceImage(String secureUrl, String publicId, Date uploadedAt,
                         String imageType, boolean isPrimary) {
        this.secureUrl = secureUrl;
        this.publicId = publicId;
        this.uploadedAt = uploadedAt;
        this.imageType = imageType;
        this.isPrimary = isPrimary;
    }
}
