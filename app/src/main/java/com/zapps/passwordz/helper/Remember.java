package com.zapps.passwordz.helper;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.annotation.Nullable;

public class Remember {
    private static final String FILENAME = "remember.log";
    private static final int MODE = Context.MODE_PRIVATE;
    private final Context context;

    private Remember(Context context) {
        this.context = context;
    }

    public static Remember with(Context context) {
        /* Same as instantiating with constructor but i like instantiating this way.*/
        return new Remember(context);
    }

    public Z that(String key) {
        return new Z(context, key);
    }

    public static class Z {

        private final Context context;
        private final String key;

        private Z(Context context, String key) {
            this.context = context;
            this.key = key;
        }

        /**
         * If any value other than null is provided then it will be stored.
         * If null is provided as the value, then key will be removed.
         */
        public void is(String value) {
            if (value == null) forget();
            else remember(value);
        }

        /**
         * Returns the value of key; Returns null if not found
         */
        @Nullable
        public String was() {
            SharedPreferences sharedPreferences = context.getSharedPreferences(FILENAME, MODE);
            return sharedPreferences.getString(key, null);
        }

        /**
         * Matches the given value with stored value.
         * If both are null then returns true.
         */
        public boolean was(String value) {
            String storedValue = was();
            if (storedValue == null)
                return value == null;
            if (value == null) return false;
            return value.equals(storedValue);
        }

        /**
         * Removes provided key
         */
        private void forget() {
            SharedPreferences sharedPreferences = context.getSharedPreferences(FILENAME, Context.MODE_PRIVATE);
            SharedPreferences.Editor editor = sharedPreferences.edit();
            editor.remove(key);
            editor.apply();
        }

        /**
         * stores 'value'
         */
        private void remember(String value) {
            SharedPreferences sharedPreferences = context.getSharedPreferences(FILENAME, MODE);
            SharedPreferences.Editor editor = sharedPreferences.edit();
            editor.putString(key, value);
            editor.apply();
        }

    }

}
