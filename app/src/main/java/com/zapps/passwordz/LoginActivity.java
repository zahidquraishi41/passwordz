package com.zapps.passwordz;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.ProgressBar;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.zapps.passwordz.helper.CToast;
import com.zapps.passwordz.helper.Enabler;
import com.zapps.passwordz.helper.Helper;
import com.zapps.passwordz.helper.MToast;
import com.zapps.passwordz.helper.Messages;
import com.zapps.passwordz.helper.MrCipher;
import com.zapps.passwordz.helper.PermissionHelper;
import com.zapps.passwordz.helper.Remember;

public class LoginActivity extends AppCompatActivity {
    private static final String TAG = "ZQ-LoginActivity";
    private EditText etUsername, etPassword;
    private FirebaseAuth firebaseAuth;
    private Enabler enabler;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);
        etUsername = findViewById(R.id.et_username);
        etPassword = findViewById(R.id.et_password);
        MaterialButton btnLogin = findViewById(R.id.btn_login);
        MaterialButton btnSignUp = findViewById(R.id.btn_sign_up);
        ProgressBar progressBar = findViewById(R.id.progress_bar);

        enabler = new Enabler(etUsername, etPassword, btnLogin, btnSignUp);
        enabler.setProgressBar(progressBar);
        firebaseAuth = FirebaseAuth.getInstance();
        if (PermissionHelper.requiresPermission(this))
            PermissionHelper.requestPermissions(this);
    }

    @Override
    protected void onStart() {
        super.onStart();
        FirebaseUser firebaseUser = firebaseAuth.getCurrentUser();
        if (firebaseUser != null) {
            startActivity(new Intent(this, MainActivity.class));
            finish();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == 101) {
            for (int result : grantResults)
                if (result != PackageManager.PERMISSION_GRANTED) {
                    CToast.warn(this, "Storage permissions are required!");
                    return;
                }
        }
        FirebaseUser firebaseUser = firebaseAuth.getCurrentUser();
        if (firebaseUser != null) {
            startActivity(new Intent(this, MainActivity.class));
            finish();
        }
    }

    public void login(View view) {
        if (PermissionHelper.requiresPermission(this)) {
            PermissionHelper.requestPermissions(this);
            return;
        }
        String username, password;
        username = etUsername.getText().toString();
        password = etPassword.getText().toString();

        MToast toast = new MToast(this);
        toast.stopAfterFirst(true);
        if (username.isEmpty()) toast.warn("Username cannot be empty");
        if (password.isEmpty()) toast.warn("Password cannot be empty");
        if (toast.resetCount().warningCount > 0) return;

        username = username + "@gmail.com";
        enabler.disableAll();

        firebaseAuth.signInWithEmailAndPassword(username, password)
                .addOnSuccessListener(authResult -> {
                    try {
                        Remember.with(LoginActivity.this).that(Helper.KEY_HASHED_PASSWORD).is(MrCipher.getSHA256(password));
                    } catch (Exception e) {
                        CToast.error(LoginActivity.this, e.getMessage());
                        firebaseAuth.signOut();
                        return;
                    }
                    FirebaseUser firebaseUser = authResult.getUser();
                    if (firebaseUser == null) {
                        CToast.error(LoginActivity.this, Messages.FIREBASE_USER_NULL);
                        firebaseAuth.signOut();
                        return;
                    }
                    startActivity(new Intent(LoginActivity.this, MainActivity.class));
                    finish();
                })
                .addOnFailureListener(e -> {
                    enabler.enableAll();
                    String message = "";
                    if (e.getMessage() != null)
                        message = e.getMessage()
                                .replace("email address", "username")
                                .replace("email", "username");
                    if (message.isEmpty()) message = Messages.UNEXPECTED_ERROR;
                    CToast.error(LoginActivity.this, message);
                });
    }

    public void signUp(View view) {
        if (PermissionHelper.requiresPermission(this)) {
            PermissionHelper.requestPermissions(this);
            return;
        }
        startActivity(new Intent(this, SignUpActivity.class));
    }

}
