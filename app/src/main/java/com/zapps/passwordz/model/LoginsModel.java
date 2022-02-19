package com.zapps.passwordz.model;

import android.content.Context;

import androidx.annotation.NonNull;

import com.zapps.passwordz.helper.Helper;
import com.zapps.passwordz.helper.Messages;
import com.zapps.passwordz.helper.MrCipher;

import java.util.ArrayList;

public class LoginsModel implements Cloneable, Comparable<LoginsModel> {
    private static final String TAG = "ZQ-LoginsModel";
    private String website, username, password, notes, lastModified;
    private String pushId;
    private static final String DELIMITER = "\n\t\n";

    public LoginsModel() {
    }

    public LoginsModel(String website, String username, String password, String notes, String lastModified) {
        this.website = website;
        this.username = username;
        this.password = password;
        this.notes = notes;
        this.lastModified = lastModified;
    }

    public LoginsModel(String website, String username, String password) {
        this.website = website;
        this.username = username;
        this.password = password;
        this.notes = "";
        this.pushId = "";
        this.lastModified = "";
    }

    public LoginsModel encrypt(Context context) throws Exception {
        String key = Helper.getEncryptionKey(context);
        if (key == null) throw new Exception(Messages.KEY_GENERATION_FAILED);
        LoginsModel encryptedModel;
        try {
            encryptedModel = (LoginsModel) this.clone();
            encryptedModel.setWebsite(MrCipher.encrypt(getWebsite(), key));
            encryptedModel.setUsername(MrCipher.encrypt(getUsername(), key));
            encryptedModel.setPassword(MrCipher.encrypt(getPassword(), key));
            encryptedModel.setNotes(MrCipher.encrypt(getNotes(), key));
            encryptedModel.setLastModified(MrCipher.encrypt(getLastModified(), key));
        } catch (Exception e) {
            throw new Exception(Messages.ENCRYPTION_FAILED);
        }
        return encryptedModel;
    }

    public LoginsModel decrypt(Context context) throws Exception {
        String key = Helper.getEncryptionKey(context);
        if (key == null) throw new Exception(Messages.KEY_GENERATION_FAILED);
        LoginsModel decryptedModel;
        try {
            decryptedModel = (LoginsModel) this.clone();
            decryptedModel.setWebsite(MrCipher.decrypt(getWebsite(), key));
            decryptedModel.setUsername(MrCipher.decrypt(getUsername(), key));
            decryptedModel.setPassword(MrCipher.decrypt(getPassword(), key));
            decryptedModel.setNotes(MrCipher.decrypt(getNotes(), key));
            decryptedModel.setLastModified(MrCipher.decrypt(getLastModified(), key));
        } catch (Exception e) {
            throw new Exception(Messages.DECRYPTION_FAILED);
        }
        return decryptedModel;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getWebsite() {
        return website;
    }

    public void setWebsite(String website) {
        this.website = website;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    public String getPushId() {
        return pushId;
    }

    public void setPushId(String pushId) {
        this.pushId = pushId;
    }

    public String getLastModified() {
        return lastModified;
    }

    public void setLastModified(String lastModified) {
        this.lastModified = lastModified;
    }

    public boolean equals(LoginsModel loginsModel) {
        if (loginsModel == null) return false;
        return getPushId().equals(loginsModel.getPushId())
                && getWebsite().equals(loginsModel.getWebsite())
                && getUsername().equals(loginsModel.getUsername())
                && getPassword().equals(loginsModel.getPassword())
                && getLastModified().equals(loginsModel.getLastModified())
                && getNotes().equals(loginsModel.getNotes());
    }

    @NonNull
    @Override
    public String toString() {
        return "[" + getWebsite() + "]" +
                "\n" +
                "Username: " + getUsername() +
                "\n" +
                "Password: " + getPassword() +
                "\n" +
                "Last Modified: " + getLastModified() +
                "\n" +
                "Notes: " + getNotes() +
                DELIMITER;
    }

    public static ArrayList<LoginsModel> fromString(String s) {
        ArrayList<LoginsModel> loginsModels = new ArrayList<>();
        if (s == null || s.isEmpty()) return loginsModels;
        String[] strCardModels = s.split(DELIMITER);
        for (String strLoginModel : strCardModels) {
            String[] lines = strLoginModel.split("\n", 5);
            String website = lines[0].substring(1, lines[0].length() - 1);
            String username = lines[1].replaceFirst("Username: ", "");
            String password = lines[2].replaceFirst("Password: ", "");
            String lastModified = lines[3].replaceFirst("Last Modified: ", "");
            String notes = lines[4].replaceFirst("Notes: ", "");
            loginsModels.add(new LoginsModel(website, username, password, notes, lastModified));
        }
        return loginsModels;
    }

    @NonNull
    public Object clone() throws CloneNotSupportedException {
        return super.clone();
    }

    @Override
    public int compareTo(LoginsModel other) {
        if (getWebsite().equals(other.getWebsite()))
            return getUsername().compareTo(other.getUsername());
        return getWebsite().compareTo(other.getWebsite());
    }

}
