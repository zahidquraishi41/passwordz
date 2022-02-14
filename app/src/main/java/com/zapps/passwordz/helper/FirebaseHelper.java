package com.zapps.passwordz.helper;

import android.content.Context;

import androidx.annotation.NonNull;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;
import com.zapps.passwordz.model.CardsModel;
import com.zapps.passwordz.model.LoginsModel;

// any firebase related operations are done using this class.
public class FirebaseHelper {
    private static final String TAG = "ZQ";

    public interface CompletionListener {
        void onCompletion(boolean result, String error);
    }

    public interface ExistsListener {
        void onSuccess(boolean exists);

        void onError(String error);
    }

    public interface DataRetrieveListener {
        void onSuccess(@NonNull LoginsModel... loginsModels);

        void onError(@NonNull String error);
    }

    public interface CardsRetrieverListener {
        void onSuccess(@NonNull CardsModel... cardsModels);

        void onError(@NonNull String error);
    }

    // root -> logins -> $uid -> $push -> loginsModel
    public final static DatabaseReference LOGINS_REF = FirebaseDatabase.getInstance().getReference().child("logins");

    // root -> cards -> $uid -> $push -> cardsModel
    public final static DatabaseReference CARDS_REF = FirebaseDatabase.getInstance().getReference().child("cards");

    // requires decrypted LoginsModel; if pushId is not null then updates existing data
    public static void saveLogin(Context context, LoginsModel loginsModel, CompletionListener completionListener) {
        if (!ConnectionObserver.isConnected(context)) {
            completionListener.onCompletion(false, Helper.MESSAGE_NO_INTERNET);
            return;
        }

        LoginsModel encrypted;
        try {
            encrypted = loginsModel.encrypt(context);
        } catch (Exception e) {
            completionListener.onCompletion(false, e.getMessage());
            return;
        }

        FirebaseUser firebaseUser = FirebaseAuth.getInstance().getCurrentUser();
        if (firebaseUser == null) {
            completionListener.onCompletion(false, Helper.MESSAGE_FIREBASE_USER_NULL);
            return;
        }

        DatabaseReference reference;
        if (encrypted.getPushId() == null || encrypted.getPushId().isEmpty()) {
            reference = LOGINS_REF.child(firebaseUser.getUid()).push();
            String pushId = reference.getKey();
            encrypted.setPushId(pushId);
        } else {
            reference = LOGINS_REF.child(firebaseUser.getUid()).child(encrypted.getPushId());
        }
        encrypted.setLastModified(Helper.getCurrentDate());

        reference.setValue(encrypted).addOnCompleteListener(task -> {
            String message;
            if (task.getException() != null && task.getException().getMessage() != null)
                message = task.getException().getMessage();
            else message = null;
            completionListener.onCompletion(task.isSuccessful(), message);
        });
    }

    // TODO made for testing purpose; remove once app is finished
    public static void saveLogin(Context context, LoginsModel loginsModel) {
        saveLogin(context, loginsModel, (result, error) -> {
        });
    }

    // for deleting single login account
    public static void deleteLogin(Context context, String pushId, CompletionListener completionListener) {
        if (!ConnectionObserver.isConnected(context)) {
            completionListener.onCompletion(false, Helper.MESSAGE_NO_INTERNET);
            return;
        }
        FirebaseUser firebaseUser = FirebaseAuth.getInstance().getCurrentUser();
        if (firebaseUser == null) {
            completionListener.onCompletion(false, "User authentication failed");
            return;
        }
        LOGINS_REF.child(firebaseUser.getUid()).child(pushId).removeValue().addOnCompleteListener(task -> {
            String message;
            if (task.getException() == null) message = "";
            else message = task.getException().getMessage();
            completionListener.onCompletion(task.isSuccessful(), message);
        });
    }

    // checks if a username is already used within a website
    // if no match is found then pushId is set to null
    public static void loginExists(Context context, String website, String username, ExistsListener listener) {
        if (!ConnectionObserver.isConnected(context)) {
            listener.onError(Helper.MESSAGE_NO_INTERNET);
            return;
        }
        getAllLogins(context, website, new DataRetrieveListener() {
            @Override
            public void onSuccess(@NonNull LoginsModel[] loginsModels) {
                for (LoginsModel loginsModel : loginsModels)
                    if (loginsModel.getUsername().equals(username)) {
                        listener.onSuccess(true);
                        return;
                    }
                listener.onSuccess(false);
            }

            @Override
            public void onError(@NonNull String error) {
                listener.onError(error);
            }
        });
    }

    private static void getAllLogins(Context context, Query query, DataRetrieveListener listener) {
        if (!ConnectionObserver.isConnected(context)) {
            listener.onError(Helper.MESSAGE_NO_INTERNET);
            return;
        }
        query.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                LoginsModel[] list = new LoginsModel[(int) snapshot.getChildrenCount()];
                int i = 0;
                for (DataSnapshot dataSnapshot : snapshot.getChildren()) {
                    LoginsModel loginsModel = dataSnapshot.getValue(LoginsModel.class);
                    if (loginsModel == null) {
                        listener.onError(Helper.MESSAGE_TYPE_CONVERSION_FAILED);
                        return;
                    }
                    try {
                        LoginsModel decrypted = loginsModel.decrypt(context);
                        list[i] = decrypted;
                    } catch (Exception e) {
                        e.printStackTrace();
                        listener.onError(Helper.MESSAGE_DECRYPTION_FAILED);
                        return;
                    }
                    i += 1;
                }
                listener.onSuccess(list);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                listener.onError(error.getMessage());
            }
        });
    }

    // retrieves all logins
    public static void getAllLogins(Context context, DataRetrieveListener listener) {
        if (!ConnectionObserver.isConnected(context)) {
            listener.onError(Helper.MESSAGE_NO_INTERNET);
            return;
        }
        FirebaseUser firebaseUser = FirebaseAuth.getInstance().getCurrentUser();
        if (firebaseUser == null) {
            listener.onError(Helper.MESSAGE_FIREBASE_USER_NULL);
            return;
        }
        Query query = FirebaseHelper.LOGINS_REF.child(firebaseUser.getUid());
        getAllLogins(context, query, listener);
    }

    // retrieves all logins within a website
    public static void getAllLogins(Context context, String website, DataRetrieveListener listener) {
        if (!ConnectionObserver.isConnected(context)) {
            listener.onError(Helper.MESSAGE_NO_INTERNET);
            return;
        }
        FirebaseUser firebaseUser = FirebaseAuth.getInstance().getCurrentUser();
        if (firebaseUser == null) {
            listener.onError(Helper.MESSAGE_FIREBASE_USER_NULL);
            return;
        }
        Query query = LOGINS_REF.child(firebaseUser.getUid()).orderByChild("website").equalTo(website);
        getAllLogins(context, query, listener);
    }

    // saves cardModel to database; also checks if same card number is used or not
    public static void saveCard(Context context, CardsModel model, CompletionListener listener) {
        if (!ConnectionObserver.isConnected(context)) {
            listener.onCompletion(false, Helper.MESSAGE_NO_INTERNET);
            return;
        }

        CardsModel encrypted;
        try {
            encrypted = model.encrypt(context);
        } catch (Exception e) {
            listener.onCompletion(false, e.getMessage());
            return;
        }

        FirebaseUser firebaseUser = FirebaseAuth.getInstance().getCurrentUser();
        if (firebaseUser == null) {
            listener.onCompletion(false, Helper.MESSAGE_FIREBASE_USER_NULL);
            return;
        }

        cardExists(context, model.getCardNumber(), new ExistsListener() {
            @Override
            public void onSuccess(boolean exists) {
                if (exists) {
                    listener.onCompletion(false, "This card number is already added");
                    return;
                }
                DatabaseReference reference = CARDS_REF.child(firebaseUser.getUid()).push();
                String pushId = reference.getKey();
                encrypted.setPushId(pushId);
                encrypted.setLastModified(Helper.getCurrentDate());
                reference.setValue(encrypted).addOnCompleteListener(task -> {
                    String message;
                    if (task.getException() != null && task.getException().getMessage() != null)
                        message = task.getException().getMessage();
                    else message = null;
                    listener.onCompletion(task.isSuccessful(), message);
                });
            }

            @Override
            public void onError(String error) {
                listener.onCompletion(false, error);
            }
        });
    }

    // TODO made for testing purpose; remove once app is finished
    public static void saveCard(Context context, CardsModel model) {
        saveCard(context, model, (result, error) -> {
        });
    }

    public static void cardExists(Context context, String cardNumber, ExistsListener listener) {
        if (!ConnectionObserver.isConnected(context)) {
            listener.onError(Helper.MESSAGE_NO_INTERNET);
            return;
        }
        FirebaseUser firebaseUser = FirebaseAuth.getInstance().getCurrentUser();
        if (firebaseUser == null) {
            listener.onError(Helper.MESSAGE_FIREBASE_USER_NULL);
            return;
        }
        String key;
        try {
            key = Helper.getEncryptionKey(context);
        } catch (Exception e) {
            e.printStackTrace();
            listener.onError(e.getMessage());
            return;
        }
        CARDS_REF.child(firebaseUser.getUid()).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                for (DataSnapshot dataSnapshot : snapshot.getChildren()) {
                    CardsModel model = dataSnapshot.getValue(CardsModel.class);
                    if (model == null) {
                        listener.onError(Helper.MESSAGE_TYPE_CONVERSION_FAILED);
                        return;
                    }
                    try {
                        String existingCardNumber = MrCipher.decrypt(model.getCardNumber(), key);
                        if (existingCardNumber.equals(cardNumber)) {
                            listener.onSuccess(true);
                            return;
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                        listener.onError(e.getMessage());
                        return;
                    }
                }
                listener.onSuccess(false);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                listener.onError(error.getMessage());
            }
        });
    }

    public static void getAllCards(Context context, CardsRetrieverListener listener) {
        if (!ConnectionObserver.isConnected(context)) {
            listener.onError(Helper.MESSAGE_NO_INTERNET);
            return;
        }

        FirebaseUser firebaseUser = FirebaseAuth.getInstance().getCurrentUser();
        if (firebaseUser == null) {
            listener.onError(Helper.MESSAGE_FIREBASE_USER_NULL);
            return;
        }
        String uid = firebaseUser.getUid();
        CARDS_REF.child(uid).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                CardsModel[] cardsModels = new CardsModel[(int) snapshot.getChildrenCount()];
                int i = 0;
                for (DataSnapshot dataSnapshot : snapshot.getChildren()) {
                    CardsModel model = dataSnapshot.getValue(CardsModel.class);
                    if (model == null) {
                        listener.onError(Helper.MESSAGE_TYPE_CONVERSION_FAILED);
                        return;
                    }
                    try {
                        model = model.decrypt(context);
                        cardsModels[i] = model;
                    } catch (Exception e) {
                        e.printStackTrace();
                        listener.onError(Helper.MESSAGE_DECRYPTION_FAILED);
                        return;
                    }
                    i += 1;
                }
                listener.onSuccess(cardsModels);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                listener.onError(error.getMessage());
            }
        });
    }

    public static void deleteCard(Context context, String pushId, CompletionListener listener) {
        if (!ConnectionObserver.isConnected(context)) {
            listener.onCompletion(false, Helper.MESSAGE_NO_INTERNET);
            return;
        }
        FirebaseUser firebaseUser = FirebaseAuth.getInstance().getCurrentUser();
        if (firebaseUser == null) {
            listener.onCompletion(false, "User authentication failed");
            return;
        }
        CARDS_REF.child(firebaseUser.getUid()).child(pushId).removeValue().addOnCompleteListener(task -> {
            String message;
            if (task.getException() == null) message = "";
            else message = task.getException().getMessage();
            listener.onCompletion(task.isSuccessful(), message);
        });
    }

    public static void deleteAllLogins(Context context, CompletionListener listener) {
        if (!ConnectionObserver.isConnected(context)) {
            listener.onCompletion(false, Helper.MESSAGE_NO_INTERNET);
            return;
        }
        FirebaseUser firebaseUser = FirebaseAuth.getInstance().getCurrentUser();
        if (firebaseUser == null) {
            listener.onCompletion(false, "User authentication failed");
            return;
        }
        LOGINS_REF.child(firebaseUser.getUid())
                .removeValue()
                .addOnSuccessListener(unused -> listener.onCompletion(true, ""))
                .addOnFailureListener(e -> listener.onCompletion(false, e.getMessage()));
    }

    public static void deleteAllCards(Context context, CompletionListener listener) {
        if (!ConnectionObserver.isConnected(context)) {
            listener.onCompletion(false, Helper.MESSAGE_NO_INTERNET);
            return;
        }
        FirebaseUser firebaseUser = FirebaseAuth.getInstance().getCurrentUser();
        if (firebaseUser == null) {
            listener.onCompletion(false, "User authentication failed");
            return;
        }
        CARDS_REF.child(firebaseUser.getUid())
                .removeValue()
                .addOnSuccessListener(unused -> listener.onCompletion(true, ""))
                .addOnFailureListener(e -> listener.onCompletion(false, e.getMessage()));
    }

}
