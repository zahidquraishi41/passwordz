package com.zapps.passwordz.bnv_fragments;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.biometric.BiometricPrompt;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;

import com.google.android.material.switchmaterial.SwitchMaterial;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.squareup.picasso.Picasso;
import com.zapps.passwordz.AboutUsActivity;
import com.zapps.passwordz.LoginActivity;
import com.zapps.passwordz.R;
import com.zapps.passwordz.dialogs.DeleteConfirmDialog;
import com.zapps.passwordz.dialogs.ProfilePicFragment;
import com.zapps.passwordz.helper.CToast;
import com.zapps.passwordz.helper.ExportImportHelper;
import com.zapps.passwordz.helper.Helper;
import com.zapps.passwordz.helper.Messages;
import com.zapps.passwordz.helper.Remember;

import java.util.concurrent.Executor;

public class SettingsFragment extends Fragment implements CompoundButton.OnCheckedChangeListener, ProfilePicFragment.ProfilePicChangeListener {
    public static final String TAG = "ZQ-SettingsFragment";
    private Context context;
    private ImageView ivProfilePic;

    private interface FileSelectionListener {
        void onSelected(ExportImportHelper.FileType fileType);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_settings, container, false);
        ivProfilePic = view.findViewById(R.id.iv_profile_pic);
        SwitchMaterial smNightMode = view.findViewById(R.id.sm_night_mode);
        smNightMode.setOnCheckedChangeListener(this);
        smNightMode.setChecked(AppCompatDelegate.getDefaultNightMode() != AppCompatDelegate.MODE_NIGHT_NO);
        view.findViewById(R.id.tv_export_logins).setOnClickListener(view12 -> export("logins"));
        view.findViewById(R.id.tv_export_cards).setOnClickListener(view12 -> export("cards"));
        view.findViewById(R.id.tv_about).setOnClickListener(view12 -> about());
        view.findViewById(R.id.tv_logout).setOnClickListener(view12 -> logout());
        view.findViewById(R.id.tv_import_logins).setOnClickListener(view14 -> _import("logins"));
        view.findViewById(R.id.tv_import_cards).setOnClickListener(view14 -> _import("cards"));
        view.findViewById(R.id.tv_remove_all_logins).setOnClickListener(view13 -> {
            DeleteConfirmDialog dialog = new DeleteConfirmDialog(
                    DeleteConfirmDialog.ACTION_DELETE_ALL_LOGINS,
                    "",
                    getString(R.string.confirm_delete_all_logins),
                    "I UNDERSTAND"
            );
            dialog.show(getChildFragmentManager(), null);
        });
        view.findViewById(R.id.tv_remove_all_cards).setOnClickListener(view13 -> {
            DeleteConfirmDialog dialog = new DeleteConfirmDialog(
                    DeleteConfirmDialog.ACTION_DELETE_ALL_CARDS,
                    "",
                    getString(R.string.confirm_delete_all_cards),
                    "I UNDERSTAND"
            );
            dialog.show(getChildFragmentManager(), null);
        });

        SwitchMaterial smShowShakePrompt = view.findViewById(R.id.sm_show_shake_prompt);
        Remember.Z shakePrompt = Remember.with(context).that(Helper.KEY_SHOW_SHAKE_PROMPT);
        smShowShakePrompt.setChecked(shakePrompt.was(null) || shakePrompt.was("true"));
        smShowShakePrompt.setOnCheckedChangeListener((compoundButton, checked) -> {
            if (checked)
                Remember.with(context).that(Helper.KEY_SHOW_SHAKE_PROMPT).is("true");
            else Remember.with(context).that(Helper.KEY_SHOW_SHAKE_PROMPT).is("false");
        });

        TextView tvFullName = view.findViewById(R.id.tv_full_name);
        TextView tvUsername = view.findViewById(R.id.tv_username);

        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) {
            CToast.error(context, Messages.FIREBASE_USER_NULL);
            ((Activity) context).finish();
            return view;
        }

        if (user.getEmail() != null)
            tvUsername.setText(user.getEmail().replace("@gmail.com", ""));
        if (user.getDisplayName() != null)
            tvFullName.setText(user.getDisplayName());
        else {
            // After signup display name takes few seconds to register which leaves makes it null.
            // using a delay of 3 seconds fixes this issue.
            Handler handler = new Handler();
            Runnable runnable = new Runnable() {
                @Override
                public void run() {
                    if (user.getDisplayName() == null) handler.postDelayed(this, 3000);
                    else tvFullName.setText(user.getDisplayName());
                }
            };
            handler.postDelayed(runnable, 3000);
        }
        ivProfilePic.setOnClickListener(view1 -> {
            ProfilePicFragment fragment = new ProfilePicFragment(this);
            fragment.show(getChildFragmentManager(), null);
        });
        return view;
    }

    private void selectFileDialog(String title, String message, FileSelectionListener listener) {
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        /* using builder.setMessage(message); hides options. Create custom dialog for fancy interface.*/
        builder.setTitle(title);
        String[] options = {"Text File", "Excel File"};
        builder.setItems(options, (dialogInterface, i) -> {
            ExportImportHelper.FileType fileType = ExportImportHelper.FileType.TEXT;
            switch (i) {
                case 0:
                    fileType = ExportImportHelper.FileType.TEXT;
                    break;
                case 1:
                    fileType = ExportImportHelper.FileType.EXCEL;
                    break;
            }
            listener.onSelected(fileType);
        });
        builder.create().show();
    }

    /*  other version of export function that require fingerprint before import/export.
    private void export(String which) {
        if (!Helper.isFingerprintSet(context)) {
            CToast.warn(context, "Please set fingerprint protection on your device to use this feature");
            return;
        }
        new BiometricAuth().prompt(context, () -> {
            selectFileDialog("Save As", "Select file type to save.", fileType -> {
                if (which.equals("cards"))
                    new ExportImportHelper(context).exportCards(fileType);
                else if (which.equals("logins"))
                    new ExportImportHelper(context).exportLogins(fileType);
            });
        });
    }
    */

    private void export(String which) {
        selectFileDialog("Save As", "Select file type to save.", fileType -> {
            if (which.equals("cards"))
                new ExportImportHelper(context).exportCards(fileType);
            else if (which.equals("logins"))
                new ExportImportHelper(context).exportLogins(fileType);
        });
    }

    private void _import(String which) {
        selectFileDialog("Import From", "Select exported file type.", fileType -> {
            if (which.equals("cards"))
                new ExportImportHelper(context).importCards(fileType);
            else if (which.equals("logins"))
                new ExportImportHelper(context).importLogins(fileType);
        });
    }

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        this.context = context;
    }

    @Override
    public void onCheckedChanged(CompoundButton compoundButton, boolean isActivated) {
        AppCompatDelegate.setDefaultNightMode(isActivated ? AppCompatDelegate.MODE_NIGHT_YES : AppCompatDelegate.MODE_NIGHT_NO);
    }

    public void about() {
        startActivity(new Intent(context, AboutUsActivity.class));
    }

    public void logout() {
        FirebaseAuth.getInstance().signOut();
        Remember.with(context).that(Helper.KEY_HASHED_PASSWORD).is(null);
        startActivity(new Intent(context, LoginActivity.class));
        ((Activity) context).finish();
    }

    @Override
    public void onStart() {
        super.onStart();
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null) {
            CToast.error(context, Messages.FIREBASE_USER_NULL);
            return;
        }
        Uri profileUri = currentUser.getPhotoUrl();
        if (profileUri == null) Picasso.get().load(R.drawable.ic_cat).into(ivProfilePic);
        else Picasso.get().load(profileUri).into(ivProfilePic);
    }

    @Override
    public void onProfileChanged(int imgId) {
        Picasso.get().load(imgId).into(ivProfilePic);
    }

    private static class BiometricAuth {
        // Q: why it exists?
        // A: because it's impossible to cancel an biometric auth from inside of default callback function
        //    so I created this to separate parameters which will now allow me to cancel auth prompt.
        //    also this specific class will automatically close auth prompt if user auth fails even once.
        private BiometricPrompt biometricPrompt;

        public interface AuthCallback {
            void onSuccess();
        }

        public void prompt(Context context, AuthCallback authCallback) {
            BiometricPrompt.PromptInfo promptInfo;
            Executor executor = ContextCompat.getMainExecutor(context);

            final BiometricPrompt.AuthenticationCallback callback = new BiometricPrompt.AuthenticationCallback() {
                @Override
                public void onAuthenticationError(int errorCode, @NonNull CharSequence errString) {
                    super.onAuthenticationError(errorCode, errString);
                    // errorCode is 13 when user presses cancel button.
                    if (errorCode != 13)
                        CToast.error(context, errString.toString());
                }

                @Override
                public void onAuthenticationSucceeded(@NonNull BiometricPrompt.AuthenticationResult result) {
                    super.onAuthenticationSucceeded(result);
                    authCallback.onSuccess();
                }

                @Override
                public void onAuthenticationFailed() {
                    super.onAuthenticationFailed();
                    CToast.error(context, "Authentication failed.");
                    cancel();
                }
            };
            biometricPrompt = new BiometricPrompt((FragmentActivity) context, executor, callback);

            promptInfo = new BiometricPrompt.PromptInfo.Builder()
                    .setTitle("Authentication Required!")
                    .setNegativeButtonText("Cancel")
                    .build();
            biometricPrompt.authenticate(promptInfo);
        }

        public void cancel() {
            if (biometricPrompt != null) biometricPrompt.cancelAuthentication();
        }

    }

}
