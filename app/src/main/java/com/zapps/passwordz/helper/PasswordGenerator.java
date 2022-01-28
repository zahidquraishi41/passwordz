package com.zapps.passwordz.helper;

import java.util.Random;

public class PasswordGenerator {
    public static final int RECOMMENDED_PASSWORD_LENGTH = 15;
    // selects a random string from s then returns it
    private static String choice(String s) {
        int random = new Random().nextInt(s.length());
        return s.charAt(random) + "";
    }

    // shuffles the characters in string s
    private static String shuffle(String s) {
        char[] chars = s.toCharArray();
        for (int i = 0; i < 4; i++) {
            int random = new Random().nextInt(chars.length);
            char temp = chars[i];
            chars[i] = chars[random];
            chars[random] = temp;
        }
        return new String(chars);
    }

    public static String generate(int passwordLength) {
        String caps_alphabets = "ABCDEFGHIJKLMNPQRSTUVWXYZ";
        String small_alphabets = "abcdefghijklmnpqrstuvwxyz";
        String numbers = "123456789";
        String symbols = "`~!@#$%^&*()-_=+]}|[{;:,<.>?";
        StringBuilder passwordBuffer = new StringBuilder();
        passwordBuffer.append(choice(caps_alphabets));
        passwordBuffer.append(choice(small_alphabets));
        passwordBuffer.append(choice(numbers));
        passwordBuffer.append(choice(symbols));
        for (int i = 4; i < passwordLength; i++) {
            int random = new Random().nextInt(4);
            switch (random) {
                case 0:
                    passwordBuffer.append(choice(caps_alphabets));
                    break;
                case 1:
                    passwordBuffer.append(choice(small_alphabets));
                    break;
                case 2:
                    passwordBuffer.append(choice(numbers));
                    break;
                case 3:
                    passwordBuffer.append(choice(symbols));
                    break;
            }
        }
        return shuffle(passwordBuffer.toString());
    }

}
