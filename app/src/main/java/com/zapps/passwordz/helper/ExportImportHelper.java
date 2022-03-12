package com.zapps.passwordz.helper;

import android.app.AlertDialog;
import android.content.Context;
import android.os.AsyncTask;
import android.os.Environment;

import androidx.annotation.NonNull;

import com.zapps.passwordz.model.CardsModel;
import com.zapps.passwordz.model.LoginsModel;

import org.apache.poi.hssf.usermodel.HSSFRow;
import org.apache.poi.hssf.usermodel.HSSFSheet;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

public class ExportImportHelper {
    private static final String TAG = "ZQ-ExportImportHelper";
    private final Context context;
    public static final String LOGINS_EXCEL = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS) + File.separator + "logins.xls";
    public static final String CARDS_EXCEL = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS) + File.separator + "cards.xls";

    public ExportImportHelper(Context context) {
        this.context = context;
    }

    public void exportLogins() {
        FirebaseHelper.getAllLogins(context, new FirebaseHelper.DataRetrieveListener() {
            @Override
            public void onSuccess(@NonNull LoginsModel... loginsModels) {
                if (loginsModels.length == 0) {
                    CToast.info(context, "No logins to save.");
                    return;
                }
                Arrays.sort(loginsModels);
                ExcelLoginsAsyncWriter asyncWriter = new ExcelLoginsAsyncWriter((result, error) -> {
                    if (result) CToast.success(context, "Logins saved successfully");
                    else CToast.error(context, error);
                });
                asyncWriter.doInBackground(loginsModels);
            }

            @Override
            public void onError(@NonNull String error) {
                CToast.error(context, error);
            }
        });
    }

    public void exportCards() {

        FirebaseHelper.getAllCards(context, new FirebaseHelper.CardsRetrieverListener() {
            @Override
            public void onSuccess(@NonNull CardsModel... cardsModels) {
                if (cardsModels.length == 0) {
                    CToast.info(context, "No card to save");
                    return;
                }
                Arrays.sort(cardsModels);
                ExcelCardsAsyncWriter cardsAsyncWriter = new ExcelCardsAsyncWriter((result, error) -> {
                    if (result) CToast.success(context, "Cards saved successfully.");
                    else CToast.error(context, error);
                });
                cardsAsyncWriter.doInBackground(cardsModels);
            }

            @Override
            public void onError(@NonNull String error) {
                CToast.error(context, error);
            }
        });
    }

    private void writeLogins(ArrayList<LoginsModel> newModels) {
        /* Skips already existing ones and write new ones to the database. */
        if (newModels.isEmpty()) {
            CToast.warn(context, "Nothing to write");
            return;
        }
        ArrayList<String> results = new ArrayList<>();
        FirebaseHelper.getAllLogins(context, new FirebaseHelper.DataRetrieveListener() {
            @Override
            public void onSuccess(@NonNull LoginsModel... loginsModels) {
                ArrayList<LoginsModel> filteredModels = new ArrayList<>();
                for (LoginsModel newModel : newModels) {
                    filteredModels.add(newModel);
                    for (LoginsModel existingModel : loginsModels) {
                        if (existingModel.getWebsite().equals(newModel.getWebsite())) {
                            results.add("Skipped: " + newModel.getUsername() + " in " + newModel.getWebsite());
                            filteredModels.remove(newModel);
                            break;
                        }
                    }
                }
                if (filteredModels.size() == 0) {
                    displayResults(results);
                    return;
                }
                for (LoginsModel loginsModel : filteredModels)
                    FirebaseHelper.saveLogin(context, loginsModel, (result, error) -> {
                        if (result)
                            results.add("Added: " + loginsModel.getUsername() + " in " + loginsModel.getWebsite());
                        else
                            results.add("Failed to add: " + loginsModel.getUsername() + " in " + loginsModel.getWebsite());
                        if (results.size() == newModels.size()) displayResults(results);
                    });
            }

            @Override
            public void onError(@NonNull String error) {
                CToast.error(context, error);
            }
        });
    }

    private void writeCards(ArrayList<CardsModel> newModels) {
        /* Skips already existing ones and write new ones to the database. */
        if (newModels.isEmpty()) {
            CToast.warn(context, "Nothing to write");
            return;
        }
        ArrayList<String> results = new ArrayList<>();
        FirebaseHelper.getAllCards(context, new FirebaseHelper.CardsRetrieverListener() {
            @Override
            public void onSuccess(@NonNull CardsModel... cardsModels) {
                ArrayList<CardsModel> filteredModels = new ArrayList<>();
                for (CardsModel newModel : newModels) {
                    filteredModels.add(newModel);
                    for (CardsModel existingModel : cardsModels) {
                        if (existingModel.getCardNumber().equals(newModel.getCardNumber())) {
                            results.add("Skipped: " + newModel.getCardNumber());
                            filteredModels.remove(newModel);
                            break;
                        }
                    }
                }
                if (filteredModels.size() == 0) {
                    displayResults(results);
                    return;
                }
                for (CardsModel cardsModel : filteredModels)
                    FirebaseHelper.saveCard(context, cardsModel, (result, error) -> {
                        if (result)
                            results.add("Added: " + cardsModel.getCardNumber());
                        else
                            results.add("Failed to add: " + cardsModel.getCardNumber());
                        if (results.size() == newModels.size()) displayResults(results);
                    });
            }

            @Override
            public void onError(@NonNull String error) {
                CToast.error(context, error);
            }
        });
    }

    public void importLogins() {
        File file = new File(LOGINS_EXCEL);
        if (!file.exists()) {
            CToast.error(context, "No exported login found.");
            return;
        }
        CToast.info(context, "Importing...");
        ArrayList<LoginsModel> newModels = new ArrayList<>();
        FileInputStream fis = null;
        try {
            fis = new FileInputStream(file);
            HSSFWorkbook workbook = new HSSFWorkbook(fis);
            HSSFSheet sheet = workbook.getSheetAt(0);

            //Iterate through each rows one by one
            Iterator<Row> iterator = sheet.iterator();
            if (!iterator.hasNext()) throw new Exception("File is corrupted.");
            // skipping first row
            iterator.next();
            while (iterator.hasNext()) {
                Row row = iterator.next();
                ArrayList<String> values = new ArrayList<>();
                for (Cell cell : row) values.add(cell.getStringCellValue());
                newModels.add(LoginsModel.fromList(values));
            }
        } catch (Exception e) {
            CToast.error(context, "An error occurred while processing excel file.");
            return;
        } finally {
            try {
                if (fis != null) {
                    fis.close();
                }
            } catch (IOException ignore) {
            }
        }
        writeLogins(newModels);
    }

    public void importCards() {
        File file = new File(CARDS_EXCEL);
        if (!file.exists()) {
            CToast.error(context, "No exported login found.");
            return;
        }
        CToast.info(context, "Importing...");
        ArrayList<CardsModel> newModels = new ArrayList<>();
        FileInputStream fis = null;
        try {
            fis = new FileInputStream(file);
            HSSFWorkbook workbook = new HSSFWorkbook(fis);
            HSSFSheet sheet = workbook.getSheetAt(0);

            //Iterate through each rows one by one
            Iterator<Row> iterator = sheet.iterator();
            if (!iterator.hasNext()) throw new Exception("File is corrupted.");
            // skipping first row
            iterator.next();
            while (iterator.hasNext()) {
                Row row = iterator.next();
                ArrayList<String> values = new ArrayList<>();
                for (Cell cell : row) values.add(cell.getStringCellValue());
                newModels.add(CardsModel.fromList(values));
            }
        } catch (Exception e) {
            CToast.error(context, "An error occurred while processing excel file.");
            return;
        } finally {
            try {
                if (fis != null) {
                    fis.close();
                }
            } catch (IOException ignore) {
            }
        }
        writeCards(newModels);
    }

    private void displayResults(ArrayList<String> results) {
        AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(context);
        alertDialogBuilder.setTitle("Result");
        StringBuilder stringBuilder = new StringBuilder();
        for (String line : results)
            stringBuilder.append(line).append("\n\n");
        alertDialogBuilder.setMessage(stringBuilder.toString());
        alertDialogBuilder.setPositiveButton("Ok", (dialogInterface, i) -> {
        });
        alertDialogBuilder.show();
    }

    private static void writeRow(HSSFSheet hssfSheet, int rowNumber, List<String> items) {
        HSSFRow row = hssfSheet.createRow(rowNumber);
        for (int i = 0; i < items.size(); i++) row.createCell(i).setCellValue(items.get(i));
    }

    /* Async classes */
    private static class ExcelLoginsAsyncWriter extends AsyncTask<LoginsModel, Void, Void> {
        private final FirebaseHelper.CompletionListener listener;

        @SuppressWarnings("deprecation")
        public ExcelLoginsAsyncWriter(FirebaseHelper.CompletionListener listener) {
            this.listener = listener;
        }

        @Override
        protected Void doInBackground(LoginsModel... list) {
            File file = new File(LOGINS_EXCEL);

            // Creating excel sheet
            HSSFWorkbook hssfWorkbook = new HSSFWorkbook();
            HSSFSheet hssfSheet = hssfWorkbook.createSheet();

            // writing header row
            List<String> headers = new ArrayList<>();
            headers.add("Website");
            headers.add("Username");
            headers.add("Password");
            headers.add("Notes");
            headers.add("Last Modified");
            writeRow(hssfSheet, 0, headers);

            // creating data rows
            for (int i = 0; i < list.length; i++) writeRow(hssfSheet, i + 1, list[i].toList());

            // writing to file
            FileOutputStream fos = null;
            try {
                fos = new FileOutputStream(file);
                hssfWorkbook.write(fos);
                listener.onCompletion(true, "");
            } catch (IOException e) {
                listener.onCompletion(false, e.getMessage());
            } finally {
                try {
                    if (fos != null) fos.close();
                } catch (Exception ignore) {
                }
            }
            return null;
        }

        @Override
        protected void onPostExecute(Void unused) {
            super.onPostExecute(unused);
        }
    }

    private static class ExcelCardsAsyncWriter extends AsyncTask<CardsModel, Void, Void> {
        private final FirebaseHelper.CompletionListener listener;

        @SuppressWarnings("deprecation")
        public ExcelCardsAsyncWriter(FirebaseHelper.CompletionListener listener) {
            this.listener = listener;
        }

        @Override
        protected Void doInBackground(CardsModel... list) {
            File file = new File(CARDS_EXCEL);

            // creating excel sheet
            HSSFWorkbook hssfWorkbook = new HSSFWorkbook();
            HSSFSheet hssfSheet = hssfWorkbook.createSheet();

            // creating header row
            List<String> headers = new ArrayList<>();
            headers.add("Card Number");
            headers.add("Valid Through");
            headers.add("Name On Card");
            headers.add("CVV");
            headers.add("Card Type");
            writeRow(hssfSheet, 0, headers);

            // creating data rows
            for (int i = 0; i < list.length; i++) writeRow(hssfSheet, i + 1, list[i].toList());

            // writing to file
            FileOutputStream fos = null;
            try {
                fos = new FileOutputStream(file);
                hssfWorkbook.write(fos);
                fos.close();
                listener.onCompletion(true, "");
            } catch (IOException e) {
                listener.onCompletion(false, e.getMessage());
            } finally {
                try {
                    if (fos != null) fos.close();
                } catch (Exception ignore) {
                }
            }
            return null;
        }
    }

}
