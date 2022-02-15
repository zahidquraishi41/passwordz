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
import com.zapps.passwordz.model.CardsModel;
import com.zapps.passwordz.model.LoginsModel;

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

    public static String getEncryptionKey(Context context) {
        String hashedPassword = Remember.with(context).that(Helper.KEY_HASHED_PASSWORD).was();
        if (hashedPassword == null) return null;
        FirebaseAuth firebaseAuth = FirebaseAuth.getInstance();
        if (firebaseAuth.getCurrentUser() == null) return null;
        String email = firebaseAuth.getCurrentUser().getEmail();
        String uid = firebaseAuth.getUid();
        if (email == null | uid == null) return null;
        String encryptionKey = null;
        try {
            encryptionKey = getSHA256(hashedPassword + email + uid);
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


    /* TODO
     * saveAll(), createDummyWebsiteList(), createDummyCardList() are test function
     * remove from final product
     */

    private static void saveAll(Context context, LoginsModel... loginsModels) {
        for (LoginsModel loginsModel : loginsModels) FirebaseHelper.saveLogin(context, loginsModel);
    }

    private static void saveAll(Context context, CardsModel... cardsModels) {
        for (CardsModel model : cardsModels) {
            model.setCardNumber(CardsModel.formatCardNumber(model.getCardNumber()));
            FirebaseHelper.saveCard(context, model);
        }
    }

    public static void createDummyLoginsList(Context context) {
        LoginsModel google1 = new LoginsModel("Google.com", "elmer.leonard@gmail.com", "()JhuU(H!@(#H98");
        LoginsModel google2 = new LoginsModel("Google.com", "craig.snyder@gmail.com", "3H!@(#H98");
        LoginsModel google3 = new LoginsModel("Google.com", "kendrick.ball@gmail.com", "(61hus!@(#H98");
        LoginsModel google4 = new LoginsModel("Google.com", "archer.hoffman@gmail.com", "TFT22ij1jIJ");
        LoginsModel google5 = new LoginsModel("Google.com", "jerry.fairbank@gmail.com", "$%^&*()PfF");
        LoginsModel google6 = new LoginsModel("Google.com", "gresham.salvage@gmail.com", "NUSoi2()@@");
        LoginsModel google7 = new LoginsModel("Google.com", "godfrey.kain@gmail.com", "7915H&@(H");
        LoginsModel google8 = new LoginsModel("Google.com", "miles.neel@gmail.com", "MIM!@R+909");
        LoginsModel google9 = new LoginsModel("Google.com", "wilbur.hoffman@gmail.com", "KFI_DPK!)(");
        LoginsModel google10 = new LoginsModel("Google.com", "ronald.elledge@gmail.com", "JOI((@!11");
        LoginsModel google11 = new LoginsModel("Google.com", "baldwin.brown@gmail.com", "(SF_+AFOj1oi2j1");
        LoginsModel google12 = new LoginsModel("Google.com", "herb.geis@gmail.com", "asO(OFK11");
        LoginsModel google13 = new LoginsModel("Google.com", "marty.pascall@gmail.com", "KAS_@!$O9921");
        LoginsModel google14 = new LoginsModel("Google.com", "austin.robbins@gmail.com", "OK!!_@#!$k)(*");
        LoginsModel google15 = new LoginsModel("Google.com", "warwick.hancock@gmail.com", "ijGS***@!#!");
        LoginsModel google16 = new LoginsModel("Google.com", "johnny.fields@gmail.com", "kIS_K1AOF(99");
        LoginsModel google17 = new LoginsModel("Google.com", "gregory.goodwin@gmail.com", "PIJ(@!!214");
        LoginsModel google18 = new LoginsModel("Google.com", "lovell.baldwin@gmail.com", "strong");
        LoginsModel google19 = new LoginsModel("Google.com", "kennard.fletcher@gmail.com", "password_+");
        LoginsModel google20 = new LoginsModel("Google.com", "jerry.fennimore@gmail.com", "KP(@#K#");
        saveAll(context, google1, google2, google3, google4, google5, google6, google7, google8, google9, google10, google11, google12, google13, google14, google15, google16, google17, google18, google19, google20);


        LoginsModel dummy1 = new LoginsModel("Amazon.com", "elmer.leonard@gmail.com", "()JhuU(H!@(#H98");
        LoginsModel dummy2 = new LoginsModel("Amazon.com", "craig.snyder@gmail.com", "3H!@(#H98");
        LoginsModel dummy3 = new LoginsModel("Flipkart.com", "kendrick.ball@gmail.com", "(61hus!@(#H98");
        LoginsModel dummy4 = new LoginsModel("YouTube.com", "archer.hoffman@gmail.com", "TFT22ij1jIJ");
        LoginsModel dummy5 = new LoginsModel("Twitch.tv", "jerry.fairbank@gmail.com", "$%^&*()PfF");
        LoginsModel dummy6 = new LoginsModel("Nykaa.com", "gresham.salvage@gmail.com", "NUSoi2()@@");
        LoginsModel dummy7 = new LoginsModel("AirIndia.in", "godfrey.kain@gmail.com", "7915H&@(H");
        LoginsModel dummy8 = new LoginsModel("Wikipedia.org", "miles.neel@gmail.com", "MIM!@R+909");
        LoginsModel dummy9 = new LoginsModel("Facebook.com", "wilbur.hoffman@gmail.com", "KFI_DPK!)(");
        LoginsModel dummy10 = new LoginsModel("Instagram.com", "ronald.elledge@gmail.com", "JOI((@!11");
        LoginsModel dummy11 = new LoginsModel("Twitter.com", "baldwin.brown@gmail.com", "(SF_+AFOj1oi2j1");
        LoginsModel dummy12 = new LoginsModel("Ebay.com", "herb.geis@gmail.com", "asO(OFK11");
        LoginsModel dummy13 = new LoginsModel("Weather.com", "marty.pascall@gmail.com", "KAS_@!$O9921");
        LoginsModel dummy14 = new LoginsModel("IMDB.com", "austin.robbins@gmail.com", "OK!!_@#!$k)(*");
        LoginsModel dummy15 = new LoginsModel("Netflix.com", "warwick.hancock@gmail.com", "ijGS***@!#!");
        LoginsModel dummy16 = new LoginsModel("Reddit.com", "johnny.fields@gmail.com", "kIS@KAOF(99");
        LoginsModel dummy17 = new LoginsModel("LinkedIn.com", "gregory.goodwin@gmail.com", "PIJ(@!!214");
        LoginsModel dummy18 = new LoginsModel("Zoom.us", "lovell.baldwin@gmail.com", "strongPassword");
        LoginsModel dummy19 = new LoginsModel("duckduckgo.com", "kennard.fletcher@gmail.com", "password_+");
        LoginsModel dummy20 = new LoginsModel("Apple.com", "jerry.fennimore@gmail.com", "KP(@#K#");
        LoginsModel dummy21 = new LoginsModel("Apple.com", "george.skeldon@gmail.com", "KP(@#K#");
        LoginsModel dummy22 = new LoginsModel("Apple.com", "gerry.smith@gmail.com", "KP(@#K#");
        saveAll(context, dummy1, dummy2, dummy3, dummy4, dummy5, dummy6, dummy7, dummy8, dummy9, dummy10, dummy11, dummy12, dummy13, dummy14, dummy15, dummy16, dummy17, dummy18, dummy19, dummy20, dummy21, dummy22);

    }

    public static void createDummyCardsList(Context context) {
        CardsModel model0 = new CardsModel("374245455400126", "05/2023", "Adam Abraham", "643", "Credit Card");
        CardsModel model1 = new CardsModel("378282246310005", "05/2023", "Adrian Allan", "245", "Credit Card");
        CardsModel model2 = new CardsModel("7250941006528599", "06/2023", "Alan Alsop", "231", "Debit Card");
        CardsModel model3 = new CardsModel("60115564485789458", "12/2023", "Alexander Anderson", "421", "Debit Card");
        CardsModel model4 = new CardsModel("6011000991300009", "12/2023", "Andrew Arnold", "521", "Credit Card");
        CardsModel model5 = new CardsModel("8566000020000410", "02/2023", "Anthony Avery", "325", "Credit Card");
        CardsModel model6 = new CardsModel("3530111333300000", "03/2023", "Austin Bailey", "521", "Credit Card");
        CardsModel model7 = new CardsModel("5425233430109903", "04/2023", "Benjamin Baker", "646", "Credit Card");
        CardsModel model8 = new CardsModel("5425233430109903", "12/2004", "Blake Ball", "765", "Debit Card");
        CardsModel model9 = new CardsModel("2222420000001113", "08/2020", "Boris Bell", "334", "Debit Card");
        CardsModel model10 = new CardsModel("2223000048410010", "09/2020", "Brandon Berry", "574", "Debit Card");
        CardsModel model11 = new CardsModel("4263982640269299", "02/2023", "Brian Black", "342", "Debit Card");
        CardsModel model12 = new CardsModel("4263982640269299", "04/2023", "Cameron Blake", "632", "Debit Card");
        CardsModel model13 = new CardsModel("4263982640269299", "02/2023", "Carl Bond", "723", "Credit Card");
        CardsModel model14 = new CardsModel("4917484589897107", "01/2023", "Charles Bower", "756", "Debit Card");
        CardsModel model15 = new CardsModel("1001919257537193", "09/2023", "Christian Brown", "532", "Credit Card");
        CardsModel model16 = new CardsModel("4007702835532454", "10/2023", "Christopher Davis", "123", "Debit Card");
        CardsModel model17 = new CardsModel("6362970000457013", "8/2023", "Colin Burgess", "321", "Credit Card");
        CardsModel model18 = new CardsModel("6062826786276634", "09/2023", "Connor Butler", "432", "Debit Card");
        CardsModel model19 = new CardsModel("5011054488597827", "12/2023", "Dan Cameron", "421", "Credit Card");
        CardsModel model20 = new CardsModel("6271701225979642", "03/2023", "David Campbell", "442", "Debit Card");
        CardsModel model21 = new CardsModel("6034932528973614", "06/2023", "Dominic Carr", "121", "Debit Card");
        CardsModel model22 = new CardsModel("9895626746595650", "11/2023", "Dylan Chapman", "190", "Credit Card");
        CardsModel model23 = new CardsModel("5200533989557118", "11/2023", "Edward Churchill", "127", "Credit Card");
        CardsModel model24 = new CardsModel("6034883265619896", "09/2023", "Eric Clark", "184", "Debit Card");

        saveAll(context, model0, model1, model2, model3, model4, model5, model6, model7, model8, model9, model10, model11, model12, model13, model14, model15, model16, model17, model18, model19, model20, model21, model22, model23, model24);
    }


}
