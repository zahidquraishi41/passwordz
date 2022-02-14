package com.zapps.passwordz.helper;

import android.app.AlertDialog;
import android.content.Context;
import android.os.AsyncTask;
import android.os.Environment;

import androidx.annotation.NonNull;

import com.zapps.passwordz.model.CardsModel;
import com.zapps.passwordz.model.LoginsModel;

import org.apache.poi.hssf.usermodel.HSSFCell;
import org.apache.poi.hssf.usermodel.HSSFRow;
import org.apache.poi.hssf.usermodel.HSSFSheet;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;

public class ExportImportHelper {
    public enum FileType {TEXT, EXCEL}

    private static final String TAG = "ExportImportHelper";
    private final Context context;
    private static final String LOGINS_EXCEL = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS) + File.separator + "logins.xls";
    private static final String LOGINS_TEXT = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS) + File.separator + "logins.txt";
    private static final String CARDS_EXCEL = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS) + File.separator + "cards.xls";
    private static final String CARDS_TEXT = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS) + File.separator + "cards.txt";

    public ExportImportHelper(Context context) {
        this.context = context;
    }

    public void exportLogins(FileType fileType) {
        FirebaseHelper.getAllLogins(context, new FirebaseHelper.DataRetrieveListener() {
            @Override
            public void onSuccess(@NonNull LoginsModel... loginsModels) {
                if (loginsModels.length == 0) {
                    CToast.info(context, "No logins to save.");
                    return;
                }
                Arrays.sort(loginsModels);
                if (fileType.equals(FileType.TEXT)) {
                    LoginsAsyncWriter asyncWriter = new LoginsAsyncWriter((result, error) -> {
                        if (result) CToast.success(context, "Logins saved successfully");
                        else CToast.error(context, error);
                    });
                    asyncWriter.doInBackground(loginsModels);
                } else if (fileType.equals(FileType.EXCEL)) {
                    ExcelLoginsAsyncWriter asyncWriter = new ExcelLoginsAsyncWriter((result, error) -> {
                        if (result) CToast.success(context, "Logins saved successfully");
                        else CToast.error(context, error);
                    });
                    asyncWriter.doInBackground(loginsModels);
                }
            }

            @Override
            public void onError(@NonNull String error) {
                CToast.error(context, error);
            }
        });
    }

    public void exportCards(FileType fileType) {
        FirebaseHelper.getAllCards(context, new FirebaseHelper.CardsRetrieverListener() {
            @Override
            public void onSuccess(@NonNull CardsModel... cardsModels) {
                if (cardsModels.length == 0) {
                    CToast.info(context, "No card to save");
                    return;
                }
                Arrays.sort(cardsModels);
                if (fileType.equals(FileType.TEXT)) {
                    CardsAsyncWriter cardsAsyncWriter = new CardsAsyncWriter((result, error) -> {
                        if (result) CToast.success(context, "Cards saved successfully.");
                        else CToast.error(context, error);
                    });
                    cardsAsyncWriter.doInBackground(cardsModels);
                } else if (fileType.equals(FileType.EXCEL)) {
                    ExcelCardsAsyncWriter cardsAsyncWriter = new ExcelCardsAsyncWriter((result, error) -> {
                        if (result) CToast.success(context, "Cards saved successfully.");
                        else CToast.error(context, error);
                    });
                    cardsAsyncWriter.doInBackground(cardsModels);
                }
            }

            @Override
            public void onError(@NonNull String error) {
                CToast.error(context, error);
            }
        });
    }

    public void importLogins(FileType fileType) {
        if (fileType == FileType.EXCEL) importExcelLogins();
        else if (fileType == FileType.TEXT) importLogins();
    }

    public void importCards(FileType fileType) {
        if (fileType == FileType.EXCEL) importExcelCards();
        else if (fileType == FileType.TEXT) importCards();
    }


    /* Functions used by public functions. */
    private String readFile(File file) {
        if (!file.exists()) return null;
        StringBuilder builder = new StringBuilder();
        try {
            FileReader fileReader = new FileReader(file);
            int data = fileReader.read();
            while (data != -1) {
                builder.append((char) data);
                data = fileReader.read();
            }
            fileReader.close();
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
        return builder.toString();
    }

    private void writeLogins(ArrayList<LoginsModel> newModels) {
        /* Skips already existing ones and write new ones to the database. */
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

    private void importExcelLogins() {
        CToast.info(context, "Importing...");
        File file = new File(LOGINS_EXCEL);
        if (!file.exists()) {
            CToast.error(context, "No exported login found.");
            return;
        }
        ArrayList<LoginsModel> newModels = new ArrayList<>();
        try {
            FileInputStream fis = new FileInputStream(file);
            HSSFWorkbook workbook = new HSSFWorkbook(fis);
            HSSFSheet sheet = workbook.getSheetAt(0);

            //Iterate through each rows one by one
            for (Row row : sheet) {
                ArrayList<String> values = new ArrayList<>();
                for (Cell cell : row) values.add(cell.getStringCellValue());
                newModels.add(new LoginsModel(values.get(0), values.get(1), values.get(2), values.get(3)));
            }

            fis.close();
        } catch (Exception e) {
            e.printStackTrace();
            CToast.error(context, "An error occurred while processing excel file.");
            return;
        }
        writeLogins(newModels);
    }

    private void importLogins() {
        CToast.info(context, "Importing...");
        File file = new File(LOGINS_TEXT);
        if (!file.exists()) {
            CToast.error(context, "No exported login found.");
            return;
        }
        ArrayList<LoginsModel> newModels = LoginsModel.fromString(readFile(file));
        if (newModels.isEmpty()) {
            CToast.error(context, Helper.MESSAGE_ERROR_READING_LOGINS);
            return;
        }
        writeLogins(newModels);
    }

    private void importExcelCards() {
        CToast.info(context, "Importing...");
        File file = new File(CARDS_EXCEL);
        if (!file.exists()) {
            CToast.error(context, "No exported login found.");
            return;
        }
        ArrayList<CardsModel> newModels = new ArrayList<>();
        try {
            FileInputStream fis = new FileInputStream(file);
            HSSFWorkbook workbook = new HSSFWorkbook(fis);
            HSSFSheet sheet = workbook.getSheetAt(0);

            //Iterate through each rows one by one
            for (Row row : sheet) {
                ArrayList<String> values = new ArrayList<>();
                for (Cell cell : row) values.add(cell.getStringCellValue());
                newModels.add(new CardsModel(values.get(0), values.get(1), values.get(2), values.get(3), values.get(4)));
            }

            fis.close();
        } catch (Exception e) {
            e.printStackTrace();
            CToast.error(context, "An error occurred while processing excel file.");
            return;
        }
        writeCards(newModels);
    }

    private void importCards() {
        CToast.info(context, "Importing...");
        File file = new File(CARDS_TEXT);
        if (!file.exists()) {
            CToast.error(context, "No exported cards found.");
            return;
        }
        ArrayList<CardsModel> newModels = CardsModel.fromString(readFile(file));
        if (newModels.isEmpty()) {
            CToast.error(context, Helper.MESSAGE_ERROR_READING_LOGINS);
            return;
        }
        writeCards(newModels);

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


    /* Async classes */
    private static class LoginsAsyncWriter extends AsyncTask<LoginsModel, Void, Void> {
        private final FirebaseHelper.CompletionListener listener;

        @SuppressWarnings("deprecation")
        public LoginsAsyncWriter(FirebaseHelper.CompletionListener listener) {
            this.listener = listener;
        }

        @Override
        protected Void doInBackground(LoginsModel... list) {
            File file = new File(LOGINS_TEXT);
            StringBuilder builder = new StringBuilder();


            for (LoginsModel loginsModel : list) builder.append(loginsModel.toString());
            try {
                FileWriter fileWriter = new FileWriter(file);
                fileWriter.write(builder.toString());
                fileWriter.close();
                listener.onCompletion(true, "");
            } catch (IOException e) {
                e.printStackTrace();
                listener.onCompletion(false, e.getMessage());
            }
            return null;
        }

        @Override
        protected void onPostExecute(Void unused) {
            super.onPostExecute(unused);
        }
    }

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

            // creating header row
            HSSFRow headerRow = hssfSheet.createRow(0);
            HSSFCell cell0 = headerRow.createCell(0);
            cell0.setCellValue("Website");
            HSSFCell cell1 = headerRow.createCell(1);
            cell1.setCellValue("Username");
            HSSFCell cell2 = headerRow.createCell(2);
            cell2.setCellValue("Password");
            HSSFCell cell3 = headerRow.createCell(3);
            cell3.setCellValue("Notes");

            // creating data rows
            for (int i = 0; i < list.length; i++) {
                HSSFRow hssfRow = hssfSheet.createRow(i + 1);
                HSSFCell websiteCell = hssfRow.createCell(0);
                websiteCell.setCellValue(list[i].getWebsite());
                HSSFCell usernameCell = hssfRow.createCell(1);
                usernameCell.setCellValue(list[i].getUsername());
                HSSFCell passwordCell = hssfRow.createCell(2);
                passwordCell.setCellValue(list[i].getPassword());
                HSSFCell notesCell = hssfRow.createCell(3);
                notesCell.setCellValue(list[i].getNotes());
            }

            // writing to file
            try {
                FileOutputStream fos = new FileOutputStream(file);
                hssfWorkbook.write(fos);
                fos.close();
                listener.onCompletion(true, "");
            } catch (IOException e) {
                e.printStackTrace();
                listener.onCompletion(false, e.getMessage());
            }
            return null;
        }

        @Override
        protected void onPostExecute(Void unused) {
            super.onPostExecute(unused);
        }
    }

    private static class CardsAsyncWriter extends AsyncTask<CardsModel, Void, Void> {
        private final FirebaseHelper.CompletionListener listener;

        @SuppressWarnings("deprecation")
        public CardsAsyncWriter(FirebaseHelper.CompletionListener listener) {
            this.listener = listener;
        }

        @Override
        protected Void doInBackground(CardsModel... list) {
            File file = new File(CARDS_TEXT);
            StringBuilder builder = new StringBuilder();
            for (CardsModel cardsModel : list) builder.append(cardsModel.toString());
            try {
                FileWriter fileWriter = new FileWriter(file);
                fileWriter.write(builder.toString());
                fileWriter.close();
                listener.onCompletion(true, "");
            } catch (IOException e) {
                e.printStackTrace();
                listener.onCompletion(false, e.getMessage());
            }
            return null;
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
            //    public String cardNumber, validThrough, nameOnCard, cvv, cardType;
            File file = new File(CARDS_EXCEL);

            // Creating excel sheet
            HSSFWorkbook hssfWorkbook = new HSSFWorkbook();
            HSSFSheet hssfSheet = hssfWorkbook.createSheet();

            // creating header row
            HSSFRow headerRow = hssfSheet.createRow(0);
            HSSFCell cell0 = headerRow.createCell(0);
            cell0.setCellValue("Card Number");
            HSSFCell cell1 = headerRow.createCell(1);
            cell1.setCellValue("Valid Through");
            HSSFCell cell2 = headerRow.createCell(2);
            cell2.setCellValue("Name On Card");
            HSSFCell cell3 = headerRow.createCell(3);
            cell3.setCellValue("CVV");
            HSSFCell cell4 = headerRow.createCell(4);
            cell4.setCellValue("Card Type");

            // creating data rows
            for (int i = 0; i < list.length; i++) {
                HSSFRow hssfRow = hssfSheet.createRow(i + 1);
                HSSFCell cardNumberCell = hssfRow.createCell(0);
                cardNumberCell.setCellValue(list[i].getCardNumber());
                HSSFCell validThroughCell = hssfRow.createCell(1);
                validThroughCell.setCellValue(list[i].getValidThrough());
                HSSFCell nameCell = hssfRow.createCell(2);
                nameCell.setCellValue(list[i].getNameOnCard());
                HSSFCell cvvCell = hssfRow.createCell(3);
                cvvCell.setCellValue(list[i].getCvv());
                HSSFCell cardTypeCell = hssfRow.createCell(4);
                cardTypeCell.setCellValue(list[i].getCardType());
            }

            // writing to file
            try {
                FileOutputStream fos = new FileOutputStream(file);
                hssfWorkbook.write(fos);
                fos.close();
                listener.onCompletion(true, "");
            } catch (IOException e) {
                e.printStackTrace();
                listener.onCompletion(false, e.getMessage());
            }
            return null;
        }
    }

}
