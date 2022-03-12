package com.zapps.passwordz;

import android.hardware.SensorManager;
import android.os.Bundle;
import android.os.Handler;
import android.view.MenuItem;

import androidx.activity.result.ActivityResultLauncher;
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
import com.zapps.passwordz.helper.Helper;
import com.zapps.passwordz.helper.PasswordGenerator;
import com.zapps.passwordz.helper.Remember;

import java.util.concurrent.Executor;

public class MainActivity extends AppCompatActivity implements NavigationBarView.OnItemSelectedListener {
    private static final String TAG = "ZQ-MainActivity";
    private boolean isAuthenticated = false;
    private FragmentManager fragmentManager;
    private BottomNavigationView navigationView;
    private FirebaseAuth firebaseAuth;
    private LoginsListFragment loginsListFragment;
    private CardsListFragment cardsListFragment;
    private SettingsFragment settingsFragment;
    private SensorManager sensorManager;
    private ShakeDetector shakeDetector;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        navigationView = findViewById(R.id.bnv_main);
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
                    .add(R.id.fragment_container, loginsListFragment, LoginsListFragment.TAG)
                    .add(R.id.fragment_container, cardsListFragment, CardsListFragment.TAG)
                    .add(R.id.fragment_container, settingsFragment, SettingsFragment.TAG)
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
        if (!Remember.with(this).that(Helper.KEY_ENABLE_FINGERPRINT).was("true")) {
            displaySelectedFragment();
            return;
        }
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
}