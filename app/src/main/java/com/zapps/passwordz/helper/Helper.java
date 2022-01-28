package com.zapps.passwordz.helper;

import static androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_STRONG;

import android.app.Activity;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.ContentResolver;
import android.content.Context;
import android.net.Uri;
import android.util.Log;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;

import androidx.annotation.NonNull;
import androidx.biometric.BiometricManager;
import androidx.biometric.BiometricPrompt;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.FragmentActivity;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.auth.FirebaseAuth;
import com.shashank.sony.fancytoastlib.FancyToast;
import com.zapps.passwordz.R;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.concurrent.Executor;
import java.util.regex.Pattern;

// a generic helper class contains helper methods only.
public class Helper {
    private static final String TAG = "ZQ-Helper";
    public static boolean shakePrompted = false;
    public static final String KEY_HASHED_PASSWORD = "hashed_password";
    public static final String KEY_SHOW_SHAKE_PROMPT = "show_shake_prompt";

    public static final String MESSAGE_FIREBASE_USER_NULL = "Failed to access database.";
    public static final String MESSAGE_TYPE_CONVERSION_FAILED = "Failed to access database.";
    public static final String MESSAGE_DECRYPTION_FAILED = "Failed to decrypt data.";
    public static final String MESSAGE_ENCRYPTION_FAILED = "Failed to encrypt data.";
    public static final String MESSAGE_KEY_GENERATION_FAILED = "Key generation failed.";
    public static final String MESSAGE_ON_CANCELLED = "Failed to connect to database.";
    public static final String MESSAGE_USERNAME_ALREADY_EXISTS = "This username is already added.";
    public static final String MESSAGE_ERROR_READING_LOGINS = "Error reading login accounts.";
    public static final String MESSAGE_ERROR_READING_CARDS = "Error reading card details.";
    public static final String MESSAGE_NO_INTERNET = "You are not connected to internet.";

    public interface AuthenticationCallback {
        void result(boolean isSuccess);
    }

    // assuming id of parent layout is R.id.parent
    public static void animate(Activity activity) {
        Animation fade_in = AnimationUtils.loadAnimation(activity, R.anim.fade_in);
        ViewGroup viewGroup;
        try {
            viewGroup = activity.findViewById(R.id.parent);
            viewGroup.startAnimation(fade_in);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // prompts toast to let user know they can shake device to generate password
    public static void promptShake(Context context) {
        if (Remember.with(context).that(KEY_SHOW_SHAKE_PROMPT).was("false"))
            return;
        if (!shakePrompted) {
            shakePrompted = true;
            CToast.info(context, "Shake your device to generate random password");
        }
    }

    public static String generatePassword(int length) {
        return PasswordGenerator.generate(length);
    }

    public static void copyToClipboard(Context context, String text) {
        ClipboardManager clipboard = (ClipboardManager) context.getSystemService(Context.CLIPBOARD_SERVICE);
        ClipData clip = ClipData.newPlainText("label", text);
        clipboard.setPrimaryClip(clip);
        FancyToast.makeText(context, "Copied to clipboard", FancyToast.LENGTH_SHORT, FancyToast.INFO, false).show();
    }

    public static BiometricPrompt biometricPrompt(Context context, AuthenticationCallback authenticationCallback) {
        BiometricPrompt biometricPrompt;
        BiometricPrompt.PromptInfo promptInfo;
        Executor executor = ContextCompat.getMainExecutor(context);

        final BiometricPrompt.AuthenticationCallback callback = new BiometricPrompt.AuthenticationCallback() {
            @Override
            public void onAuthenticationError(int errorCode, @NonNull CharSequence errString) {
                super.onAuthenticationError(errorCode, errString);
                Log.d(TAG, "onAuthenticationError: " + errString);
                authenticationCallback.result(false);
            }

            @Override
            public void onAuthenticationSucceeded(@NonNull BiometricPrompt.AuthenticationResult result) {
                super.onAuthenticationSucceeded(result);
                authenticationCallback.result(true);
            }

            @Override
            public void onAuthenticationFailed() {
                super.onAuthenticationFailed();
                Log.d(TAG, "onAuthenticationFailed: ");
                authenticationCallback.result(false);
            }
        };
        biometricPrompt = new BiometricPrompt((FragmentActivity) context, executor, callback);

        promptInfo = new BiometricPrompt.PromptInfo.Builder()
                .setTitle("Authentication Required!")
                .setNegativeButtonText("Cancel")
                .build();
        biometricPrompt.authenticate(promptInfo);
        return biometricPrompt;
    }

    public static boolean isFingerprintSet(Context context) {
        BiometricManager biometricManager = BiometricManager.from(context);
        return biometricManager.canAuthenticate(BIOMETRIC_STRONG) == BiometricManager.BIOMETRIC_SUCCESS;
    }

    public static String getSHA256(String s) throws Exception {
        MessageDigest messageDigest = MessageDigest.getInstance("SHA-256");
        byte[] hash = messageDigest.digest(s.getBytes(StandardCharsets.UTF_8));
        StringBuilder hexString = new StringBuilder(2 * hash.length);
        for (byte b : hash) {
            String hex = Integer.toHexString(0xff & b);
            if (hex.length() == 1) {
                hexString.append('0');
            }
            hexString.append(hex);
        }
        return hexString.toString();
    }

    // TODO add unique key for extra security. also write it somewhere
    public static String getEncryptionKey(Context context) {
//        String hashedPassword = SharedPrefHelper.get(context, SharedPrefHelper.KEY_HASHED_PASSWORD);
        String hashedPassword = Remember.with(context).that(Helper.KEY_HASHED_PASSWORD).was();
        if (hashedPassword == null) return null;
        FirebaseAuth firebaseAuth = FirebaseAuth.getInstance();
        if (firebaseAuth.getCurrentUser() == null) return null;
        String email = firebaseAuth.getCurrentUser().getEmail();
        String uid = firebaseAuth.getUid();
        if (email == null | uid == null) return null;
        String encryptionKey = null;
        try {
            // TODO remove MrCipher.KEY variable from below line; for actual product
            encryptionKey = getSHA256(hashedPassword + email + uid + MrCipher.KEY);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return encryptionKey;
    }

    public static String getFavIconUrl(String website) {
        return "https://api.faviconkit.com/" + website.toLowerCase() + "/144";
    }

    public static boolean isValidDomain(String domain) {
        if (domain == null || domain.trim().isEmpty()) return false;
        String regex = "^((?!-)[A-Za-z0-9-]"
                + "{1,63}(?<!-)\\.)"
                + "+[A-Za-z]{2,6}";
        Pattern pattern = Pattern.compile(regex);
        return pattern.matcher(domain).matches();
    }

    public static String getCurrentDate() {
        String dateFormat = "dd/M/yyyy hh:mm a";
        SimpleDateFormat format = new SimpleDateFormat(dateFormat, Locale.getDefault());
        return format.format(new Date());
    }

    public static Uri drawableToUri(Context context, int drawableId) {
        return Uri.parse(ContentResolver.SCHEME_ANDROID_RESOURCE +
                "://" + context.getResources().getResourcePackageName(drawableId)
                + '/' + context.getResources().getResourceTypeName(drawableId)
                + '/' + context.getResources().getResourceEntryName(drawableId));
    }

    public static void autoHideFAB(RecyclerView recyclerView, FloatingActionButton floatingActionButton) {
        recyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
                super.onScrolled(recyclerView, dx, dy);
                if (dy < 0 && !floatingActionButton.isShown())
                    floatingActionButton.show();
                else if (dy > 0 && floatingActionButton.isShown())
                    floatingActionButton.hide();
            }
        });

    }

}
