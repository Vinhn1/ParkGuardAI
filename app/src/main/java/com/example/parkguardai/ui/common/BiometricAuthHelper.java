package com.example.parkguardai.ui.common;

import android.content.Context;
import android.content.SharedPreferences;
import android.security.keystore.KeyGenParameterSpec;
import android.security.keystore.KeyProperties;
import android.util.Base64;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.biometric.BiometricManager;
import androidx.biometric.BiometricPrompt;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.google.android.material.materialswitch.MaterialSwitch;

import org.json.JSONObject;

import java.nio.charset.StandardCharsets;
import java.security.KeyStore;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;

/**
 * Hỗ trợ đăng nhập sinh trắc học (Vân tay / Face ID) bằng AndroidX BiometricPrompt
 * và mã hóa thông tin phiên bằng Android Keystore AES-256-GCM (không lưu mật khẩu thuần).
 */
public final class BiometricAuthHelper {
    private static final String PREFS = "parkguard_biometric_prefs";
    private static final String KEY_ENABLED = "biometric_enabled";
    private static final String KEY_CIPHER = "biometric_cipher";
    private static final String KEY_IV = "biometric_iv";
    private static final String KEY_ALIAS = "ParkGuardBiometricKey";
    private static final String ANDROID_KEYSTORE = "AndroidKeyStore";
    private static final String TRANSFORMATION = "AES/GCM/NoPadding";
    private static final int ALLOWED_AUTH =
            BiometricManager.Authenticators.BIOMETRIC_STRONG
                    | BiometricManager.Authenticators.BIOMETRIC_WEAK;

    public interface AuthCallback {
        void onSuccess(@NonNull String email, @NonNull String password);
        void onError(@NonNull String message);
    }

    public interface SessionCallback {
        void onVerified();
        void onCancelled();
        void onError(@NonNull String message);
    }

    private BiometricAuthHelper() { }

    public static boolean isEnabled(@NonNull Context context) {
        return prefs(context).getBoolean(KEY_ENABLED, true);
    }

    public static void setEnabled(@NonNull Context context, boolean enabled) {
        prefs(context).edit().putBoolean(KEY_ENABLED, enabled).apply();
        if (!enabled) {
            clearCredentials(context);
        }
    }

    public static boolean canAuthenticate(@NonNull Context context) {
        int result = BiometricManager.from(context).canAuthenticate(ALLOWED_AUTH);
        return result == BiometricManager.BIOMETRIC_SUCCESS;
    }

    @NonNull
    public static String unavailableReason(@NonNull Context context) {
        int result = BiometricManager.from(context).canAuthenticate(ALLOWED_AUTH);
        if (result == BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED) {
            return "Thiết bị chưa cài đặt vân tay hoặc khuôn mặt trong Cài đặt hệ thống.";
        }
        if (result == BiometricManager.BIOMETRIC_ERROR_NO_HARDWARE
                || result == BiometricManager.BIOMETRIC_ERROR_HW_UNAVAILABLE) {
            return "Thiết bị không hỗ trợ hoặc cảm biến sinh trắc học đang bận.";
        }
        return "Không thể sử dụng xác thực sinh trắc học lúc này.";
    }

    public static boolean hasSavedCredentials(@NonNull Context context) {
        SharedPreferences p = prefs(context);
        return p.contains(KEY_CIPHER) && p.contains(KEY_IV);
    }

    public static void saveCredentials(@NonNull Context context,
                                       @NonNull String email,
                                       @NonNull String password) {
        if (!isEnabled(context) || email.isEmpty() || password.isEmpty()) return;
        try {
            SecretKey key = getOrCreateKey();
            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            cipher.init(Cipher.ENCRYPT_MODE, key);
            JSONObject payload = new JSONObject();
            payload.put("e", email);
            payload.put("p", password);
            byte[] encrypted = cipher.doFinal(payload.toString().getBytes(StandardCharsets.UTF_8));
            String cipherB64 = Base64.encodeToString(encrypted, Base64.NO_WRAP);
            String ivB64 = Base64.encodeToString(cipher.getIV(), Base64.NO_WRAP);
            prefs(context).edit()
                    .putString(KEY_CIPHER, cipherB64)
                    .putString(KEY_IV, ivB64)
                    .apply();
        } catch (Exception ignored) { }
    }

    public static void clearCredentials(@NonNull Context context) {
        prefs(context).edit().remove(KEY_CIPHER).remove(KEY_IV).apply();
    }

    public static void authenticateAndLoadCredentials(@NonNull Fragment fragment,
                                                      @NonNull AuthCallback callback) {
        Context context = fragment.requireContext();
        if (!isEnabled(context)) {
            callback.onError("Đăng nhập vân tay / Face ID đang tắt trong phần Hồ sơ.");
            return;
        }
        if (!canAuthenticate(context)) {
            callback.onError(unavailableReason(context));
            return;
        }
        if (!hasSavedCredentials(context)) {
            callback.onError("Hãy đăng nhập bằng email và mật khẩu 1 lần để kích hoạt vân tay / Face ID.");
            return;
        }

        BiometricPrompt prompt = new BiometricPrompt(
                fragment,
                ContextCompat.getMainExecutor(context),
                new BiometricPrompt.AuthenticationCallback() {
                    @Override
                    public void onAuthenticationSucceeded(@NonNull BiometricPrompt.AuthenticationResult result) {
                        String[] creds = decryptCredentials(context);
                        if (creds != null) {
                            callback.onSuccess(creds[0], creds[1]);
                        } else {
                            clearCredentials(context);
                            callback.onError("Phiên sinh trắc học đã hết hạn. Vui lòng đăng nhập bằng mật khẩu.");
                        }
                    }

                    @Override
                    public void onAuthenticationError(int errorCode, @NonNull CharSequence errString) {
                        if (errorCode != BiometricPrompt.ERROR_USER_CANCELED
                                && errorCode != BiometricPrompt.ERROR_NEGATIVE_BUTTON) {
                            callback.onError(errString.toString());
                        }
                    }
                });

        BiometricPrompt.PromptInfo info = new BiometricPrompt.PromptInfo.Builder()
                .setTitle("Đăng nhập ParkGuard AI")
                .setSubtitle("Xác thực vân tay hoặc khuôn mặt để tiếp tục")
                .setNegativeButtonText("Dùng mật khẩu")
                .setAllowedAuthenticators(ALLOWED_AUTH)
                .build();

        prompt.authenticate(info);
    }

    /**
     * Xác thực sinh trắc học khi mở lại app đã có phiên Firebase Auth sẵn.
     */
    public static void authenticateSession(@NonNull Fragment fragment,
                                           @NonNull SessionCallback callback) {
        Context context = fragment.requireContext();
        if (!isEnabled(context) || !canAuthenticate(context)) {
            callback.onVerified();
            return;
        }

        BiometricPrompt prompt = new BiometricPrompt(
                fragment,
                ContextCompat.getMainExecutor(context),
                new BiometricPrompt.AuthenticationCallback() {
                    @Override
                    public void onAuthenticationSucceeded(@NonNull BiometricPrompt.AuthenticationResult result) {
                        callback.onVerified();
                    }

                    @Override
                    public void onAuthenticationError(int errorCode, @NonNull CharSequence errString) {
                        if (errorCode == BiometricPrompt.ERROR_USER_CANCELED
                                || errorCode == BiometricPrompt.ERROR_NEGATIVE_BUTTON) {
                            callback.onCancelled();
                        } else {
                            callback.onError(errString.toString());
                        }
                    }
                });

        BiometricPrompt.PromptInfo info = new BiometricPrompt.PromptInfo.Builder()
                .setTitle("Mở khóa ParkGuard AI")
                .setSubtitle("Quét vân tay hoặc khuôn mặt để vào tài khoản của bạn")
                .setNegativeButtonText("Dùng mật khẩu")
                .setAllowedAuthenticators(ALLOWED_AUTH)
                .build();

        prompt.authenticate(info);
    }

    @Nullable
    private static String[] decryptCredentials(@NonNull Context context) {
        try {
            SharedPreferences p = prefs(context);
            String cipherB64 = p.getString(KEY_CIPHER, null);
            String ivB64 = p.getString(KEY_IV, null);
            if (cipherB64 == null || ivB64 == null) return null;

            SecretKey key = getOrCreateKey();
            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            byte[] iv = Base64.decode(ivB64, Base64.NO_WRAP);
            cipher.init(Cipher.DECRYPT_MODE, key, new GCMParameterSpec(128, iv));
            byte[] plain = cipher.doFinal(Base64.decode(cipherB64, Base64.NO_WRAP));
            JSONObject obj = new JSONObject(new String(plain, StandardCharsets.UTF_8));
            String email = obj.optString("e", "");
            String password = obj.optString("p", "");
            if (email.isEmpty() || password.isEmpty()) return null;
            return new String[]{email, password};
        } catch (Exception e) {
            return null;
        }
    }

    private static SecretKey getOrCreateKey() throws Exception {
        KeyStore ks = KeyStore.getInstance(ANDROID_KEYSTORE);
        ks.load(null);
        if (ks.containsAlias(KEY_ALIAS)) {
            KeyStore.Entry entry = ks.getEntry(KEY_ALIAS, null);
            if (entry instanceof KeyStore.SecretKeyEntry) {
                return ((KeyStore.SecretKeyEntry) entry).getSecretKey();
            }
        }
        KeyGenerator generator = KeyGenerator.getInstance(
                KeyProperties.KEY_ALGORITHM_AES, ANDROID_KEYSTORE);
        KeyGenParameterSpec spec = new KeyGenParameterSpec.Builder(
                KEY_ALIAS,
                KeyProperties.PURPOSE_ENCRYPT | KeyProperties.PURPOSE_DECRYPT)
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setKeySize(256)
                .build();
        generator.init(spec);
        return generator.generateKey();
    }

    private static SharedPreferences prefs(@NonNull Context context) {
        return context.getApplicationContext().getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    /** Gắn logic bật/tắt đăng nhập vân tay / Face ID dùng chung cho Sinh viên, Bảo vệ và Admin. */
    public static void bindSwitch(@NonNull Fragment fragment, @NonNull MaterialSwitch switchView) {
        Context context = fragment.requireContext();
        switchView.setChecked(isEnabled(context));
        switchView.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (!buttonView.isPressed()) return;
            Context ctx = fragment.requireContext();
            if (isChecked) {
                if (!canAuthenticate(ctx)) {
                    buttonView.setChecked(false);
                    Toast.makeText(ctx, unavailableReason(ctx), Toast.LENGTH_LONG).show();
                    return;
                }
                setEnabled(ctx, true);
                Toast.makeText(ctx,
                        hasSavedCredentials(ctx)
                                ? "Đã bật đăng nhập vân tay / Face ID"
                                : "Đã bật. Đăng nhập lại 1 lần để lưu khóa sinh trắc học.",
                        Toast.LENGTH_SHORT).show();
            } else {
                setEnabled(ctx, false);
                Toast.makeText(ctx, "Đã tắt đăng nhập vân tay / Face ID", Toast.LENGTH_SHORT).show();
            }
        });
    }
}
