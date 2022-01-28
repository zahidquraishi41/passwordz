package com.zapps.passwordz;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.EditText;

import androidx.appcompat.app.AppCompatActivity;

import com.zapps.passwordz.helper.CToast;
import com.zapps.passwordz.helper.Enabler;
import com.zapps.passwordz.helper.FirebaseHelper;
import com.zapps.passwordz.helper.MToast;
import com.zapps.passwordz.model.CardsModel;

public class AddCardActivity extends AppCompatActivity {
    private static final String TAG = "ZQ";
    private EditText etNameOnCard, etCardNumber, etValidThrough, etCVV;
    private Enabler enabler;
    private AutoCompleteTextView actvCardType;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_card);
        etNameOnCard = findViewById(R.id.etNameOnCard);
        etCardNumber = findViewById(R.id.etCardNumber);
        etValidThrough = findViewById(R.id.etValidThrough);
        etCVV = findViewById(R.id.etCVV);
        actvCardType = findViewById(R.id.actvCardType);

        enabler = new Enabler(etNameOnCard, etCardNumber, etCVV, findViewById(R.id.btnSave));
        enabler.setProgressBar(findViewById(R.id.progressBar));
        String[] items = {"Credit Card", "Debit Card"};
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, items);

        enabler.setProgressBar(findViewById(R.id.progressBar));
        actvCardType.setAdapter(adapter);
        etValidThrough.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {
            }

            @Override
            public void onTextChanged(CharSequence charSequence, int start, int count, int after) {
            }

            @Override
            public void afterTextChanged(Editable editable) {
                etValidThrough.removeTextChangedListener(this);
                String originalString = editable.toString().replaceAll("/", "");
                if (originalString.length() <= 2) {
                    etValidThrough.setText(originalString);
                    etValidThrough.setSelection(originalString.length());
                } else {
                    StringBuilder modifiedString = new StringBuilder(originalString);
                    modifiedString.insert(2, '/');
                    etValidThrough.setText(modifiedString.toString());
                    etValidThrough.setSelection(modifiedString.length());
                }
                etValidThrough.addTextChangedListener(this);
            }
        });
        etCardNumber.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {
            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
            }

            @Override
            public void afterTextChanged(Editable editable) {
                int cursorPosition = etCardNumber.getSelectionStart();
                etCardNumber.removeTextChangedListener(this);
                String modifiedString = CardsModel.formatCardNumber(editable.toString());
                etCardNumber.setText(modifiedString);
                if (cursorPosition == editable.toString().length())
                    etCardNumber.setSelection(modifiedString.length());
                else etCardNumber.setSelection(cursorPosition);
                etCardNumber.addTextChangedListener(this);
            }
        });
    }

    public void save(View view) {
        String nameOnCard, cardNumber, validThrough, cvv, cardType;
        nameOnCard = etNameOnCard.getText().toString();
        cardNumber = etCardNumber.getText().toString();
        validThrough = etValidThrough.getText().toString();
        cvv = etCVV.getText().toString();
        cardType = actvCardType.getText().toString();

        MToast toast = new MToast(this);
        toast.stopAfterFirst(true);

        if (nameOnCard.isEmpty()) toast.warn("Please enter name.");
        if (!nameOnCard.matches("[A-Za-z ]+"))
            toast.warn("Name can only contain alphabets and spaces.");

        if (cardNumber.isEmpty()) toast.warn("Please enter card number.");
        if (cardNumber.length() != 19) toast.warn("Please enter valid card number.");

        if (cardType.isEmpty() || cardType.equals(getString(R.string.card_type)))
            toast.warn("Please select card type.");

        if (validThrough.isEmpty()) toast.warn("Please enter valid through.");
        if (validThrough.length() != 7 || !validThrough.matches("[0-9]{2}/[0-9]{4}"))
            toast.warn("Please enter correct valid through");
        if (validThrough.length() == 7) {
            String[] date = validThrough.split("/");
            String month = date[0];
            String year = date[1];
            try {
                int iMonth = Integer.parseInt(month);
                int iYear = Integer.parseInt(year);
                if (iMonth < 1 || iMonth > 12) toast.warn("Please enter valid month.");
                if (iYear < 2000 || iYear > 3000) toast.warn("Please enter valid year");
            } catch (NumberFormatException e) {
                e.printStackTrace();
                toast.warn("Please enter correct valid through");
            }
        }

        if (cvv.isEmpty()) toast.warn("Please enter cvv.");
        if (cvv.length() != 3 || !cvv.matches("[0-9]+")) toast.warn("Please enter valid cvv.");

        if (toast.resetCount().warningCount > 0) return;

        enabler.disableAll();
        CardsModel model = new CardsModel(cardNumber, validThrough, nameOnCard, cvv, cardType);
        FirebaseHelper.saveCard(this, model, (result, error) -> {
            enabler.enableAll();
            if (result) {
                CToast.success(AddCardActivity.this, "Successfully added");
                finish();
            } else CToast.error(AddCardActivity.this, error);
        });
    }
}