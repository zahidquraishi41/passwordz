package com.zapps.passwordz;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.text.HtmlCompat;

import android.app.AlertDialog;
import android.os.Bundle;
import android.text.Spanned;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;

import com.zapps.passwordz.helper.CToast;
import com.zapps.passwordz.helper.Helper;

public class AboutUsActivity extends AppCompatActivity {
    private TextView tvAbout;
    private View tvVersionInfo;
    private long lastTapTime = System.currentTimeMillis();
    private int count = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_about_us);

        tvAbout = findViewById(R.id.tv_about);
        tvVersionInfo = findViewById(R.id.tv_version_info);

        tvVersionInfo.setOnClickListener(view -> versionInfo());
        String AboutApp = "This Project aim at developing software that can be used for securing their email id’s and passwords. This project is useful because we generally forget our either email id’s or passwords and if we note down on a diary or any other paper anyone can watch it and misuse our email id. It is simple and convenient for security.";
        String Gratitude = "This project was developed for partial fulfillment of MCA degree. We would like to mention our sincere gratitude towards our principle <b>Dr. P. P. Bhattacharya</b> and HOD <i>Dr. Dibyendu Pal</i>, MCA Department, for giving us the opportunity to carry out our project. We would like to express our heart full gratitude to our project guide <b>Mr. Tapan Kumar Das</b>, who took keen interest on our project work and guided us all along, till the completion of our project work by providing all the necessary information for developing a good system. Finally, we take this opportunity to mention our sincere thanks to all team members for the completion of our project.";
        Spanned htmlAboutApp = HtmlCompat.fromHtml(AboutApp, HtmlCompat.FROM_HTML_MODE_LEGACY);
        Spanned htmlGratitude = HtmlCompat.fromHtml(Gratitude, HtmlCompat.FROM_HTML_MODE_LEGACY);
        tvAbout.setText(String.format("%s\n\n%s", htmlAboutApp, htmlGratitude));
    }

    private void showDevDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Add Fake Accounts");
        builder.setMessage("Type 'LOGINS' to add fake logins, Type 'CARDS' to add fake cards.");

        EditText editText = new EditText(this);
        builder.setView(editText);

        builder.setPositiveButton("Ok", (dialog, which) -> {
            String value = editText.getText().toString();
            if (value.equals("LOGINS")) Helper.createDummyLoginsList(this);
            else if (value.equals("CARDS")) Helper.createDummyCardsList(this);
            else CToast.error(this, "Invalid input.");
        });
        builder.setNegativeButton("Cancel", (dialog, which) -> dialog.cancel());
        builder.show();
    }

    private void versionInfo() {
        if (count == 0 || System.currentTimeMillis() - lastTapTime < 800) {
            count += 1;
            if (count == 5) {
                count = 0;
                showDevDialog();
            }
        } else count = 0;
        lastTapTime = System.currentTimeMillis();
    }
}