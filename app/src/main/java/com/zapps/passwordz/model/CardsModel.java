package com.zapps.passwordz.model;

import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;

import androidx.annotation.NonNull;

import com.zapps.passwordz.helper.Helper;
import com.zapps.passwordz.helper.Messages;
import com.zapps.passwordz.helper.MrCipher;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Random;

public class CardsModel implements Cloneable, Comparable<CardsModel> {
    private static final String TAG = "ZQ-CardsModel";
    private String pushId;
    public String cardNumber, validThrough, nameOnCard, cvv, cardType;
    private static final String DELIMITER = "\n\t\n";

    public CardsModel() {
    }

    public CardsModel(String cardNumber, String validThrough, String nameOnCard, String cvv, String cardType) {
        this.cardNumber = cardNumber;
        this.validThrough = validThrough;
        this.nameOnCard = nameOnCard;
        this.cvv = cvv;
        this.cardType = cardType;
    }

    public CardsModel encrypt(Context context) throws Exception {
        String key = Helper.getEncryptionKey(context);
        if (key == null) throw new Exception(Messages.KEY_GENERATION_FAILED);
        CardsModel encrypted;
        try {
            encrypted = (CardsModel) this.clone();
            encrypted.setCardNumber(MrCipher.encrypt(encrypted.getCardNumber(), key));
            encrypted.setValidThrough(MrCipher.encrypt(encrypted.getValidThrough(), key));
            encrypted.setNameOnCard(MrCipher.encrypt(encrypted.getNameOnCard(), key));
            encrypted.setCvv(MrCipher.encrypt(encrypted.getCvv(), key));
        } catch (Exception e) {
            throw new Exception(Messages.ENCRYPTION_FAILED);
        }
        return encrypted;
    }

    public CardsModel decrypt(Context context) throws Exception {
        String key = Helper.getEncryptionKey(context);
        if (key == null) throw new Exception(Messages.KEY_GENERATION_FAILED);
        CardsModel decrypted;
        try {
            decrypted = (CardsModel) this.clone();
            decrypted.setCardNumber(MrCipher.decrypt(decrypted.getCardNumber(), key));
            decrypted.setValidThrough(MrCipher.decrypt(decrypted.getValidThrough(), key));
            decrypted.setNameOnCard(MrCipher.decrypt(decrypted.getNameOnCard(), key));
            decrypted.setCvv(MrCipher.decrypt(decrypted.getCvv(), key));
        } catch (Exception e) {
            throw new Exception(Messages.ENCRYPTION_FAILED);
        }
        return decrypted;
    }

    public String getPushId() {
        return pushId;
    }

    public void setPushId(String pushId) {
        this.pushId = pushId;
    }

    public String getCardNumber() {
        return cardNumber;
    }

    public void setCardNumber(String cardNumber) {
        this.cardNumber = cardNumber;
    }

    public String getValidThrough() {
        return validThrough;
    }

    public void setValidThrough(String validThrough) {
        this.validThrough = validThrough;
    }

    public String getNameOnCard() {
        return nameOnCard;
    }

    public void setNameOnCard(String nameOnCard) {
        this.nameOnCard = nameOnCard;
    }

    public String getCvv() {
        return cvv;
    }

    public void setCvv(String cvv) {
        this.cvv = cvv;
    }

    public static String formatCardNumber(String cardNumber) {
        String originalString = cardNumber.replaceAll(" ", "");
        if (originalString.length() <= 4) return originalString;
        StringBuilder builder = new StringBuilder(originalString.substring(0, 4));
        for (int i = 4; i < originalString.length(); i += 4) {
            int e = i + 4;
            if (e > originalString.length()) e = originalString.length();
            builder.append(' ').append(originalString.substring(i, e));
        }
        return builder.toString();
    }

    public String getCardType() {
        return cardType;
    }

    public void setCardType(String cardType) {
        this.cardType = cardType;
    }

    private GradientDrawable gradient(String start, String end) {
        GradientDrawable gradientDrawable = new GradientDrawable(GradientDrawable.Orientation.LEFT_RIGHT, new int[]{Color.parseColor(start), Color.parseColor(end)});
        gradientDrawable.setCornerRadius(8f);
        return gradientDrawable;
    }

    public GradientDrawable cardBackground() {
        GradientDrawable gradientDrawable = gradient("#f857a6", "#ff5858");
        if (cardNumber == null || cardNumber.isEmpty() || cardNumber.startsWith("1"))
            gradientDrawable = gradient("#f857a6", "#ff5858");
        if (cardNumber.startsWith("2")) gradientDrawable = gradient("#4b6cb7", "#182848");
        if (cardNumber.startsWith("3")) gradientDrawable = gradient("#1F1C2C", "#928DAB");
        if (cardNumber.startsWith("4")) gradientDrawable = gradient("#a044ff", "#1F1C18");
        if (cardNumber.startsWith("5")) gradientDrawable = gradient("#f46b45", "#eea849");
        if (cardNumber.startsWith("6")) gradientDrawable = gradient("#4CB8C4", "#3CD3AD");
        if (cardNumber.startsWith("7")) gradientDrawable = gradient("#6a3093", "#a044ff");
        if (cardNumber.startsWith("8")) gradientDrawable = gradient("#AA076B", "#61045F");
        if (cardNumber.startsWith("9")) gradientDrawable = gradient("#76b852", "#8DC26F");
        return gradientDrawable;
    }

    public boolean equals(CardsModel cardsModel) {
        if (this == cardsModel) return true;
        if (cardsModel == null) return false;
        return Objects.equals(pushId, cardsModel.pushId)
                && Objects.equals(cardNumber, cardsModel.cardNumber)
                && Objects.equals(validThrough, cardsModel.validThrough)
                && Objects.equals(nameOnCard, cardsModel.nameOnCard)
                && Objects.equals(cvv, cardsModel.cvv)
                && Objects.equals(cardType, cardsModel.cardType);
    }

    public boolean isIn(CardsModel[] cardsModels) {
        for (CardsModel cardsModel : cardsModels)
            if (cardsModel.equals(this)) return true;
        return false;
    }

    @Override
    public int compareTo(CardsModel model) {
        Long thisNumber = Long.parseLong(getCardNumber().replaceAll(" ", ""));
        Long otherNumber = Long.parseLong(model.getCardNumber().replaceAll(" ", ""));
        return thisNumber.compareTo(otherNumber);
    }

    @NonNull
    @Override
    public String toString() {
        return "Card Number: " + getCardNumber() + "\n"
                + "Name on Card: " + getNameOnCard() + "\n"
                + "Card Type: " + getCardType() + "\n"
                + "Valid Through: " + getValidThrough() + "\n"
                + "CVV: " + getCvv() + "\n"
                + DELIMITER;
    }

    public static ArrayList<CardsModel> fromString(String s) {
        ArrayList<CardsModel> cardsModels = new ArrayList<>();
        if (s == null || s.isEmpty()) return cardsModels;
        String[] strCardModels = s.split(DELIMITER);
        for (String line : strCardModels) {
            String[] lines = line.split("\n", 5);
            String cardNumber = lines[0].replaceFirst("Card Number: ", "");
            String nameOnCard = lines[1].replaceFirst("Name on Card: ", "");
            String cardType = lines[2].replaceFirst("Card Type: ", "");
            String validThrough = lines[3].replaceFirst("Valid Through: ", "");
            String cvv = lines[4].replaceFirst("CVV: ", "");
            cardsModels.add(new CardsModel(cardNumber, validThrough, nameOnCard, cvv, cardType));
        }
        return cardsModels;
    }

    public List<String> toList() {
        List<String> columns = new ArrayList<>();
        columns.add(getCardNumber());
        columns.add(getValidThrough());
        columns.add(getNameOnCard());
        columns.add(getCvv());
        columns.add(getCardType());
        return columns;
    }

    public static CardsModel fromList(List<String> data) {
        return new CardsModel(data.get(0), data.get(1), data.get(2), data.get(3), data.get(4));
    }

    @NonNull
    public Object clone() throws CloneNotSupportedException {
        return super.clone();
    }

}
