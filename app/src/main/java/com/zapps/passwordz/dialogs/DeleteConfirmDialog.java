package com.zapps.passwordz.dialogs;

import android.content.Context;
import android.os.Bundle;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.zapps.passwordz.R;
import com.zapps.passwordz.helper.CToast;
import com.zapps.passwordz.helper.FirebaseHelper;
import com.zapps.passwordz.model.CardsModel;

public class DeleteConfirmDialog extends BottomSheetDialogFragment {
    private Button btnDelete;
    private Context context;
    private DeleteItemListener listener;
    public static final int ACTION_DELETE_LOGIN = 0;
    public static final int ACTION_DELETE_CARD = 1;
    public static final int ACTION_DELETE_ALL_LOGINS = 2;
    public static final int ACTION_DELETE_ALL_CARDS = 3;
    private final int action;
    private final String pushId, message, confirmCode;

    public interface DeleteItemListener {
        void onItemDeleted(String confirmCode);
    }

    public DeleteConfirmDialog(int action, String pushId, String message, String confirmCode) {
        this.action = action;
        this.pushId = pushId;
        this.message = message;
        this.confirmCode = confirmCode;
    }

    public DeleteConfirmDialog(int action, String pushId, String message, String confirmCode, DeleteItemListener listener) {
        this(action, pushId, message, confirmCode);
        this.listener = listener;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.layout_delete_confirmation, container, false);
        btnDelete = view.findViewById(R.id.btnDelete);
        EditText etConfirmationCode = view.findViewById(R.id.etConfirmationCode);
        TextView tvConfirmationText = view.findViewById(R.id.tvConfirmationText);

        tvConfirmationText.setText(message);
        btnDelete.setEnabled(false);
        btnDelete.setOnClickListener(view12 -> {
            if (action == ACTION_DELETE_LOGIN) deleteLogin();
            if (action == ACTION_DELETE_CARD) deleteCard();
            if (action == ACTION_DELETE_ALL_LOGINS) deleteAllLogins();
            if (action == ACTION_DELETE_ALL_CARDS) deleteAllCards();
        });
        if (action == ACTION_DELETE_CARD) {
            etConfirmationCode.setInputType(InputType.TYPE_CLASS_NUMBER);
            etConfirmationCode.addTextChangedListener(new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

                }

                @Override
                public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                }

                @Override
                public void afterTextChanged(Editable editable) {
                    int cursorPosition = etConfirmationCode.getSelectionStart();
                    etConfirmationCode.removeTextChangedListener(this);
                    String modifiedString = CardsModel.formatCardNumber(editable.toString());
                    etConfirmationCode.setText(modifiedString);
                    if (cursorPosition == editable.toString().length())
                        etConfirmationCode.setSelection(modifiedString.length());
                    else etConfirmationCode.setSelection(cursorPosition);
                    btnDelete.setEnabled(modifiedString.equals(confirmCode));
                    etConfirmationCode.addTextChangedListener(this);
                }
            });

        } else {
            etConfirmationCode.addTextChangedListener(new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

                }

                @Override
                public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                    btnDelete.setEnabled(etConfirmationCode.getText().toString().equals(confirmCode));
                }

                @Override
                public void afterTextChanged(Editable editable) {

                }
            });
        }
        return view;
    }

    private void deleteLogin() {
        FirebaseHelper.deleteWebsite(context, pushId, (result, error) -> {
            if (result) {
                CToast.success(context, "Removed successfully");
                listener.onItemDeleted(confirmCode);
            } else CToast.error(context, error);
            dismiss();
        });
    }

    private void deleteCard() {
        FirebaseHelper.deleteCard(context, pushId, (result, error) -> {
            if (result) {
                if (listener != null)
                    listener.onItemDeleted(confirmCode);
                CToast.success(context, "Removed successfully");
            } else CToast.error(context, error);
            dismiss();
        });
    }

    private void deleteAllLogins() {
        FirebaseHelper.deleteAllLogins(context, (result, error) -> {
            if (result)
                CToast.success(context, "Removed Successfully");
            else CToast.error(context, error);
            dismiss();
        });
    }

    private void deleteAllCards() {
        FirebaseHelper.deleteAllCards(context, (result, error) -> {
            if (result)
                CToast.success(context, "Removed Successfully");
            else CToast.error(context, error);
            dismiss();
        });

    }

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        this.context = context;
        if (context instanceof DeleteItemListener) {
            listener = (DeleteItemListener) context;
        }
    }
}
