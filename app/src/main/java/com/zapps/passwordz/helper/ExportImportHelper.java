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

import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;

public class ExportImportHelper {
    private static final String TAG = "ExportImportHelper";
    private final Context context;
    public static final String LOGINS_FILENAME = "logins.txt";
    public static final String CARDS_FILENAME = "cards.txt";
    public static final String EXPORT_TYPE_TEXT = "text";
    public static final String EXPORT_TYPE_EXCEL = "excel";
    private static final String EXCEL_FILE = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS) + File.separator + "logins.xls";

    public ExportImportHelper(Context context) {
        this.context = context;
    }

    public void exportLogins() {
        exportLogins(EXPORT_TYPE_EXCEL);
    }

    public void exportLogins(String exportType) {
        FirebaseHelper.getAllLogins(context, new FirebaseHelper.DataRetrieveListener() {
            @Override
            public void onSuccess(@NonNull LoginsModel... loginsModels) {
                if (loginsModels.length == 0) {
                    CToast.info(context, "No login account to save");
                    return;
                }
                if (exportType.equals(EXPORT_TYPE_TEXT)) {
                    LoginsAsyncWriter asyncWriter = new LoginsAsyncWriter((result, error) -> {
                        if (result) CToast.success(context, "Logins saved successfully");
                        else CToast.error(context, error);
                    });
                    asyncWriter.doInBackground(loginsModels);
                } else if (exportType.equals((EXPORT_TYPE_EXCEL))) {
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

    public void exportCards() {
        FirebaseHelper.getAllCards(context, new FirebaseHelper.CardsRetrieverListener() {
            @Override
            public void onSuccess(@NonNull CardsModel... cardsModels) {
                if (cardsModels.length == 0) {
                    CToast.info(context, "No card to save");
                    return;
                }
                CardsAsyncWriter cardsAsyncWriter = new CardsAsyncWriter((result, error) -> {
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

    public void importLogins() {
        CToast.info(context, "Importing...");
        File file = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS) + File.separator + LOGINS_FILENAME);
        ArrayList<LoginsModel> newModels = LoginsModel.fromString(readFile(file));
        ArrayList<String> results = new ArrayList<>();
        if (newModels.isEmpty()) {
            CToast.error(context, Helper.MESSAGE_ERROR_READING_LOGINS);
            return;
        }
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
                    FirebaseHelper.saveWebsite(context, loginsModel, (result, error) -> {
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

    public void importCards() {
        File file = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS) + File.separator + CARDS_FILENAME);
        ArrayList<CardsModel> newModels = CardsModel.fromString(readFile(file));
        ArrayList<String> results = new ArrayList<>();
        if (newModels.isEmpty()) {
            CToast.error(context, Helper.MESSAGE_ERROR_READING_LOGINS);
            return;
        }
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

    public void displayResults(ArrayList<String> results) {
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

    public static class LoginsAsyncWriter extends AsyncTask<LoginsModel, Void, Void> {
        private final FirebaseHelper.CompletionListener listener;

        public LoginsAsyncWriter(FirebaseHelper.CompletionListener listener) {
            this.listener = listener;
        }

        @Override
        protected Void doInBackground(LoginsModel... list) {
            File file = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS) + File.separator + LOGINS_FILENAME);
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

    public static class ExcelLoginsAsyncWriter extends AsyncTask<LoginsModel, Void, Void> {
        private final FirebaseHelper.CompletionListener listener;

        public ExcelLoginsAsyncWriter(FirebaseHelper.CompletionListener listener) {
            this.listener = listener;
        }

        @Override
        protected Void doInBackground(LoginsModel... list) {
            File file = new File(EXCEL_FILE);

            // TODO: test if excel file is created
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

    public static class CardsAsyncWriter extends AsyncTask<CardsModel, Void, Void> {
        private final FirebaseHelper.CompletionListener listener;

        public CardsAsyncWriter(FirebaseHelper.CompletionListener listener) {
            this.listener = listener;
        }

        @Override
        protected Void doInBackground(CardsModel... list) {
            File file = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS) + File.separator + CARDS_FILENAME);
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

    public void test() {
        HSSFWorkbook hssfWorkbook = new HSSFWorkbook();
        HSSFSheet hssfSheet = hssfWorkbook.createSheet();

        HSSFRow hssfRow = hssfSheet.createRow(0);
        HSSFCell hssfCell = hssfRow.createCell(0);
        hssfCell.setCellValue("Something");

//        hssfWorkbook.write(fileWriter);
    }

}
