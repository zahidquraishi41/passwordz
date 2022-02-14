package com.zapps.passwordz;

import android.hardware.SensorManager;
import android.os.Bundle;
import android.os.Handler;
import android.view.MenuItem;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.biometric.BiometricPrompt;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.navigation.NavigationBarView;
import com.google.firebase.auth.FirebaseAuth;
import com.squareup.seismic.ShakeDetector;
import com.zapps.passwordz.bnv_fragments.CardsListFragment;
import com.zapps.passwordz.bnv_fragments.LoginsListFragment;
import com.zapps.passwordz.bnv_fragments.SettingsFragment;
import com.zapps.passwordz.helper.CToast;
import com.zapps.passwordz.helper.ConnectionObserver;
import com.zapps.passwordz.helper.FirebaseHelper;
import com.zapps.passwordz.helper.Helper;
import com.zapps.passwordz.helper.PasswordGenerator;
import com.zapps.passwordz.model.CardsModel;
import com.zapps.passwordz.model.LoginsModel;

import java.util.concurrent.Executor;

public class MainActivity extends AppCompatActivity implements NavigationBarView.OnItemSelectedListener {
    private boolean isAuthenticated = false;
    private FragmentManager fragmentManager;
    private BottomNavigationView navigationView;
    private FirebaseAuth firebaseAuth;
    private LoginsListFragment loginsListFragment;
    private CardsListFragment cardsListFragment;
    private SettingsFragment settingsFragment;
    private SensorManager sensorManager;
    private ShakeDetector shakeDetector;

    /* TODO
     * Encrypt even names of website.
     *
     * */

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        navigationView = findViewById(R.id.bnvMain);
        navigationView.setOnItemSelectedListener(this);
        firebaseAuth = FirebaseAuth.getInstance();
        fragmentManager = getSupportFragmentManager();
        if (savedInstanceState == null) {
            loginsListFragment = new LoginsListFragment();
            settingsFragment = new SettingsFragment();
            cardsListFragment = new CardsListFragment();
            for (Fragment fragment : fragmentManager.getFragments())
                fragmentManager.beginTransaction().remove(fragment).commit();
            fragmentManager.beginTransaction()
                    .add(R.id.fragmentContainer, loginsListFragment, LoginsListFragment.TAG)
                    .add(R.id.fragmentContainer, cardsListFragment, CardsListFragment.TAG)
                    .add(R.id.fragmentContainer, settingsFragment, SettingsFragment.TAG)
                    .commit();
        } else {
            loginsListFragment = (LoginsListFragment) fragmentManager.findFragmentByTag(LoginsListFragment.TAG);
            cardsListFragment = (CardsListFragment) fragmentManager.findFragmentByTag(CardsListFragment.TAG);
            settingsFragment = (SettingsFragment) fragmentManager.findFragmentByTag(SettingsFragment.TAG);
        }
        fragmentManager.beginTransaction()
                .hide(loginsListFragment)
                .hide(cardsListFragment)
                .hide(settingsFragment)
                .commit();
        sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        shakeDetector = new ShakeDetector(() -> Helper.copyToClipboard(MainActivity.this, Helper.generatePassword(PasswordGenerator.RECOMMENDED_PASSWORD_LENGTH)));
        shakeDetector.setSensitivity(ShakeDetector.SENSITIVITY_LIGHT);
        new ConnectionObserver(this, this, () -> {
            if (isAuthenticated) runOnUiThread(this::displaySelectedFragment);
        });
        /*
        createDummyCardsList();
        createDummyLoginsList();
        */
    }

    private void authenticate() {
        final int[] failCount = {0};
        BiometricPrompt biometricPrompt;
        BiometricPrompt.PromptInfo promptInfo;
        Executor executor = ContextCompat.getMainExecutor(this);

        biometricPrompt = new BiometricPrompt(this, executor, new BiometricPrompt.AuthenticationCallback() {
            @Override
            public void onAuthenticationError(int errorCode, @NonNull CharSequence errString) {
                super.onAuthenticationError(errorCode, errString);
                int delay = 50;
                // errorCode 13 is when user cancels the authentication.
                if (errorCode != 13) {
                    delay = 1000;
                    CToast.error(MainActivity.this, errString.toString());
                }
                new Handler().postDelayed(() -> finish(), delay);
            }

            @Override
            public void onAuthenticationSucceeded(@NonNull BiometricPrompt.AuthenticationResult result) {
                super.onAuthenticationSucceeded(result);
                isAuthenticated = true;
                displaySelectedFragment();
                String username = null;
                if (firebaseAuth.getCurrentUser() != null)
                    username = firebaseAuth.getCurrentUser().getDisplayName();
                if (username == null) username = "";
                CToast.info(MainActivity.this, "Welcome " + username);
            }

            @Override
            public void onAuthenticationFailed() {
                super.onAuthenticationFailed();
                failCount[0] += 1;
                if (failCount[0] == 3) finish();
            }
        });

        promptInfo = new BiometricPrompt.PromptInfo.Builder()
                .setTitle("Authentication Required!")
                .setNegativeButtonText("Cancel")
                .build();
        biometricPrompt.authenticate(promptInfo);
    }

    @Override
    protected void onStart() {
        super.onStart();
        shakeDetector.start(sensorManager);
        if (!Helper.isFingerprintSet(this)) {
            displaySelectedFragment();
            CToast.warn(MainActivity.this, "Please set fingerprint on your device to increase security");
            return;
        }
        if (!isAuthenticated) authenticate();
        else displaySelectedFragment();
    }

    @Override
    protected void onStop() {
        super.onStop();
        shakeDetector.stop();
    }

    /**
     * Issue: whenever activity is recreated the last menu item in
     * bottom navigation is selected by default but always loginsListFragment
     * is displayed. eg, when switching from dark mode (within app setting).
     * There is a direct method for fixing this using
     * navigationView.setSelectedItemId(int)
     * which is not working for this API level.
     * below function is an alternative way to do that.
     */
    private void displaySelectedFragment() {
        int id = navigationView.getSelectedItemId();
        findViewById(id).performClick();
    }

    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
        int itemId = item.getItemId();
        if (itemId == R.id.nav_cards) {
            cardsListFragment.refreshAdapter();
            fragmentManager.beginTransaction()
                    .hide(loginsListFragment).hide(settingsFragment)
                    .show(cardsListFragment)
                    .commit();
        } else if (itemId == R.id.nav_settings) {
            fragmentManager.beginTransaction()
                    .hide(loginsListFragment).hide(cardsListFragment)
                    .show(settingsFragment)
                    .commit();
        } else {
            loginsListFragment.refreshAdapter();
            fragmentManager.beginTransaction()
                    .hide(cardsListFragment).hide(settingsFragment)
                    .show(loginsListFragment)
                    .commit();
        }
        return true;
    }

    /* TODO
     * saveAll(), createDummyWebsiteList(), createDummyCardList() are test function
     * remove from final product
     */

    public void saveAll(LoginsModel... loginsModels) {
        for (LoginsModel loginsModel : loginsModels) FirebaseHelper.saveLogin(this, loginsModel);
    }

    public void saveAll(CardsModel... cardsModels) {
        for (CardsModel model : cardsModels) {
            model.setCardNumber(CardsModel.formatCardNumber(model.getCardNumber()));
            FirebaseHelper.saveCard(this, model);
        }
    }

    public void createDummyLoginsList() {
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
        saveAll(google1, google2, google3, google4, google5, google6, google7, google8, google9, google10, google11, google12, google13, google14, google15, google16, google17, google18, google19, google20);


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
        saveAll(dummy1, dummy2, dummy3, dummy4, dummy5, dummy6, dummy7, dummy8, dummy9, dummy10, dummy11, dummy12, dummy13, dummy14, dummy15, dummy16, dummy17, dummy18, dummy19, dummy20, dummy21, dummy22);

    }

    public void createDummyCardsList() {
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

        saveAll(model0, model1, model2, model3, model4, model5, model6, model7, model8, model9, model10, model11, model12, model13, model14, model15, model16, model17, model18, model19, model20, model21, model22, model23, model24);
    }

}