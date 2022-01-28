package com.zapps.passwordz;

import android.hardware.SensorManager;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;
import com.nulabinc.zxcvbn.Strength;
import com.nulabinc.zxcvbn.Zxcvbn;
import com.squareup.seismic.ShakeDetector;
import com.zapps.passwordz.helper.CToast;
import com.zapps.passwordz.helper.Enabler;
import com.zapps.passwordz.helper.FirebaseHelper;
import com.zapps.passwordz.helper.Helper;
import com.zapps.passwordz.helper.PasswordGenerator;
import com.zapps.passwordz.model.LoginsModel;

public class AddOrEditLoginsActivity extends AppCompatActivity {
    public static final String PARAM_PUSH_ID = "push_id";
    public static final String PARAM_WEBSITE = "website";
    private EditText etWebsite, etUsername, etPassword, etNotes;
    private LoginsModel originalLoginsModel, modifiedLoginsModel;
    private Enabler enabler;
    private SensorManager sensorManager;
    private ShakeDetector shakeDetector;
    private TextInputLayout tilPassword;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_or_edit_logins);

        ProgressBar progressBar = findViewById(R.id.progressBar);
        etWebsite = findViewById(R.id.etWebsite);
        etUsername = findViewById(R.id.etUsername);
        etPassword = findViewById(R.id.etPassword);
        tilPassword = findViewById(R.id.tilPassword);
        etNotes = findViewById(R.id.etNotes);
        TextView tvTitle = findViewById(R.id.tvTitle);
        sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        shakeDetector = new ShakeDetector(() -> etPassword.setText(Helper.generatePassword(PasswordGenerator.RECOMMENDED_PASSWORD_LENGTH)));
        shakeDetector.setSensitivity(ShakeDetector.SENSITIVITY_LIGHT);
        enabler = new Enabler(etWebsite, etUsername, etPassword, etNotes, findViewById(R.id.btnSave));
        enabler.setProgressBar(progressBar);
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

        String website = getIntent().getStringExtra(PARAM_WEBSITE);
        tvTitle.setText(R.string.add_login);
        if (website != null && !website.isEmpty()) {
            enabler.removeView(etWebsite);
            etWebsite.setEnabled(false);
            etWebsite.setText(website);
            return;
        }

        String pushId = getIntent().getStringExtra(PARAM_PUSH_ID);
        if (pushId == null || pushId.isEmpty()) return;
        tvTitle.setText(R.string.edit_login);
        enabler.removeView(etWebsite);
        etWebsite.setEnabled(false);
        enabler.disableAll();
        FirebaseUser firebaseUser = FirebaseAuth.getInstance().getCurrentUser();
        if (firebaseUser == null) {
            CToast.error(this, Helper.MESSAGE_FIREBASE_USER_NULL);
            finish();
            return;
        }
        Query query = FirebaseHelper.LOGINS_REF.child(firebaseUser.getUid()).child(pushId);
        query.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                query.removeEventListener(this);
                originalLoginsModel = snapshot.getValue(LoginsModel.class);
                if (originalLoginsModel == null) {
                    CToast.error(AddOrEditLoginsActivity.this, Helper.MESSAGE_TYPE_CONVERSION_FAILED);
                    finish();
                    return;
                }
                try {
                    originalLoginsModel = originalLoginsModel.decrypt(AddOrEditLoginsActivity.this);
                    modifiedLoginsModel = originalLoginsModel.copy();
                } catch (Exception e) {
                    CToast.error(AddOrEditLoginsActivity.this, e.getMessage());
                    e.printStackTrace();
                    finish();
                    return;
                }
                etWebsite.setText(originalLoginsModel.getWebsite());
                etUsername.setText(originalLoginsModel.getUsername());
                etPassword.setText(originalLoginsModel.getPassword());
                etNotes.setText(originalLoginsModel.getNotes());
                enabler.enableAll();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                query.removeEventListener(this);
                CToast.error(AddOrEditLoginsActivity.this, "Error: " + error.getMessage());
                finish();
            }
        });
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

    public void save(View view) {
        String website, username, password, notes;
        website = etWebsite.getText().toString().trim();
        username = etUsername.getText().toString().trim();
        password = etPassword.getText().toString();
        notes = etNotes.getText().toString();

        if (website.isEmpty() || username.isEmpty() || password.isEmpty()) {
            CToast.warn(AddOrEditLoginsActivity.this, "Website, Username & Password are mandatory fields");
            return;
        }
        if (!Helper.isValidDomain(website)) {
            CToast.warn(this, "Please enter a valid domain name");
            return;
        }
        if (modifiedLoginsModel == null) modifiedLoginsModel = new LoginsModel();
        modifiedLoginsModel.setWebsite(website);
        modifiedLoginsModel.setUsername(username);
        modifiedLoginsModel.setPassword(password);
        modifiedLoginsModel.setNotes(notes);

        if (originalLoginsModel != null) {
            if (modifiedLoginsModel.equals(originalLoginsModel)) {
                finish();
                return;
            }
            if (modifiedLoginsModel.getUsername().equals(originalLoginsModel.getUsername())) {
                saveToFirebase();
                return;
            }
        }

        enabler.disableAll();
        FirebaseHelper.loginExists(this, website, username, new FirebaseHelper.ExistsListener() {
            @Override
            public void onSuccess(boolean exists) {
                if (exists) {
                    enabler.enableAll();
                    CToast.error(AddOrEditLoginsActivity.this, Helper.MESSAGE_USERNAME_ALREADY_EXISTS);
                } else saveToFirebase();
            }

            @Override
            public void onError(String error) {
                enabler.enableAll();
                CToast.error(AddOrEditLoginsActivity.this, error);
            }
        });
    }

    // saves modifiedLoginsModel to database
    public void saveToFirebase() {
        enabler.disableAll();
        FirebaseHelper.saveWebsite(AddOrEditLoginsActivity.this, modifiedLoginsModel, (result, error) -> {
            enabler.enableAll();
            if (!result) CToast.error(AddOrEditLoginsActivity.this, error);
            else {
                CToast.success(AddOrEditLoginsActivity.this, "Saved successfully");
                finish();
            }
        });
    }

}