package com.zapps.passwordz;

import android.hardware.SensorManager;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.EditText;
import android.widget.ProgressBar;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.UserProfileChangeRequest;
import com.nulabinc.zxcvbn.Strength;
import com.nulabinc.zxcvbn.Zxcvbn;
import com.squareup.seismic.ShakeDetector;
import com.zapps.passwordz.helper.CToast;
import com.zapps.passwordz.helper.Enabler;
import com.zapps.passwordz.helper.Helper;
import com.zapps.passwordz.helper.MToast;
import com.zapps.passwordz.helper.PasswordGenerator;
import com.zapps.passwordz.helper.Remember;

public class SignUpActivity extends AppCompatActivity {
    private EditText etFullName, etUsername, etPassword;
    private TextInputLayout tilPassword;
    private SensorManager sensorManager;
    private ShakeDetector shakeDetector;
    private Enabler enabler;
    private FirebaseAuth firebaseAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sign_up);
        etFullName = findViewById(R.id.et_full_name);
        etUsername = findViewById(R.id.et_username);
        etPassword = findViewById(R.id.et_password);
        tilPassword = findViewById(R.id.til_password);
        MaterialButton btnSignUp = findViewById(R.id.btn_sign_up);
        ProgressBar progressBar = findViewById(R.id.progress_bar);

        firebaseAuth = FirebaseAuth.getInstance();
        etPassword.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                String password = etPassword.getText().toString();
                Zxcvbn zxcvbn = new Zxcvbn();
                Strength strength = zxcvbn.measure(password);
                String crackTime = strength.getCrackTimesDisplay().getOnlineNoThrottling10perSecond();
                tilPassword.setHelperText("Estimated time to crack: " + crackTime);
            }

            @Override
            public void afterTextChanged(Editable editable) {

            }
        });
        sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        shakeDetector = new ShakeDetector(() -> etPassword.setText(Helper.generatePassword(PasswordGenerator.RECOMMENDED_PASSWORD_LENGTH)));
        shakeDetector.setSensitivity(ShakeDetector.SENSITIVITY_LIGHT);
        enabler = new Enabler(etFullName, etUsername, etPassword, btnSignUp);
        enabler.setProgressBar(progressBar);
        firebaseAuth.signOut();
    }

    @Override
    protected void onStart() {
        super.onStart();
        shakeDetector.start(sensorManager);
        Helper.promptShake(this);
    }

    @Override
    protected void onStop() {
        super.onStop();
        shakeDetector.stop();
    }

    public void signUp(View view) {
        String fullName, username, password;
        fullName = etFullName.getText().toString();
        username = etUsername.getText().toString();
        password = etPassword.getText().toString();

        MToast toast = new MToast(this);
        toast.stopAfterFirst(true);

        if (fullName.isEmpty()) toast.warn("Full name cannot be empty");
        if (username.isEmpty()) toast.warn("Username cannot be empty");
        if (password.isEmpty()) toast.warn("Password cannot be empty");

        Zxcvbn zxcvbn = new Zxcvbn();
        Strength strength = zxcvbn.measure(password);
        int score = strength.getScore();
        if (score < 3) toast.warn("Password is not strong enough");

        if (toast.resetCount().warningCount > 0) return;
        username = username + "@gmail.com";
        enabler.disableAll();
        firebaseAuth.createUserWithEmailAndPassword(username, password)
                .addOnSuccessListener(authResult -> {
                    CToast.success(SignUpActivity.this, "Account created successfully");
                    UserProfileChangeRequest.Builder requestBuilder = new UserProfileChangeRequest.Builder();
                    requestBuilder.setDisplayName(fullName);
                    try {
                        Remember.with(SignUpActivity.this).that(Helper.KEY_HASHED_PASSWORD).is(Helper.getSHA256(password));
//                        SharedPrefHelper.put(SignUpActivity.this, SharedPrefHelper.KEY_HASHED_PASSWORD, Helper.getSHA256(password));
                    } catch (Exception ignored) {
                        firebaseAuth.signOut();
                    }
                    if (authResult.getUser() != null)
                        authResult.getUser().updateProfile(requestBuilder.build());
                    finish();
                })
                .addOnFailureListener(e -> {
                    enabler.enableAll();
                    String message = "";
                    if (e.getMessage() != null)
                        message = e.getMessage()
                                .replace("email address", "username")
                                .replace("email", "username");
                    CToast.error(SignUpActivity.this, message);
                });
    }
}