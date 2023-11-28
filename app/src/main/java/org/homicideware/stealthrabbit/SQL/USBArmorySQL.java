package org.homicideware.stealthrabbit.SQL;

import android.annotation.SuppressLint;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.os.Environment;
import android.util.Log;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.nio.channels.FileChannel;
import java.util.ArrayList;

import org.homicideware.stealthrabbit.models.USBArmorySwitchModel;

public class USBArmorySQL extends SQLiteOpenHelper {

    private static final String DATABASE_NAME = "USBArmoryFragment";
    private static final String TAG = "USBArmorySQL";
    private static final String TABLE_NAME = DATABASE_NAME;
    private static final ArrayList<String> COLUMNS_USBSWITCH = new ArrayList<>();
    private static USBArmorySQL instance;

    private USBArmorySQL(Context context) {
        super(context, DATABASE_NAME, null, 3);
        // Add your default column here;
        COLUMNS_USBSWITCH.add("target");
        COLUMNS_USBSWITCH.add("functions");
        COLUMNS_USBSWITCH.add("idVendor");
        COLUMNS_USBSWITCH.add("idProduct");
        COLUMNS_USBSWITCH.add("manufacturer");
        COLUMNS_USBSWITCH.add("product");
        COLUMNS_USBSWITCH.add("serialnumber");
    }

    public static synchronized USBArmorySQL getInstance(Context context) {
        if (instance == null) {
            instance = new USBArmorySQL(context);
        }
        return instance;
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        final String[][] USBSwitchData = {
                {"Windows", "reset", "1234", "5678", "", "", ""},
                {"Windows", "hid", "046d", "c316", "", "", ""},
                {"Windows", "hid,adb", "046d", "c317", "", "", ""},
                {"Windows", "mass_storage", "9051", "168a", "", "", ""},
                {"Windows", "mass_storage,adb", "9051", "168b", "", "", ""},
                {"Windows", "rndis", "0525", "a4a2", "", "", ""},
                {"Windows", "rndis,adb", "0525", "a4a3", "", "", ""},
                {"Windows", "hid,mass_storage", "046d", "c318", "", "", ""},
                {"Windows", "hid,mass_storage,adb", "046d", "c319", "", "", ""},
                {"Windows", "hid,rndis", "0525", "a4a6", "", "", ""},
                {"Windows", "hid,rndis,adb", "0525", "a4a7", "", "", ""},
                {"Windows", "mass_storage,rndis", "0525", "a4a4", "", "", ""},
                {"Windows", "mass_storage,rndis,adb", "0525", "a4a5", "", "", ""},
                {"Windows", "hid,mass_storage,rndis", "0525", "a4a8", "", "", ""},
                {"Windows", "hid,mass_storage,rndis,adb", "0525", "a4a9", "", "", ""},
                {"Linux", "reset", "1234", "5678", "", "", ""},
                {"Linux", "hid", "046d", "c316", "", "", ""},
                {"Linux", "hid,adb", "046d", "c317", "", "", ""},
                {"Linux", "mass_storage", "9051", "168a", "", "", ""},
                {"Linux", "mass_storage,adb", "9051", "168b", "", "", ""},
                {"Linux", "rndis", "0525", "a4a2", "", "", ""},
                {"Linux", "rndis,adb", "0525", "a4a3", "", "", ""},
                {"Linux", "hid,mass_storage", "046d", "c318", "", "", ""},
                {"Linux", "hid,mass_storage,adb", "046d", "c319", "", "", ""},
                {"Linux", "hid,rndis", "0525", "a4a6", "", "", ""},
                {"Linux", "hid,rndis,adb", "0525", "a4a7", "", "", ""},
                {"Linux", "mass_storage,rndis", "0525", "a4a4", "", "", ""},
                {"Linux", "mass_storage,rndis,adb", "0525", "a4a5", "", "", ""},
                {"Linux", "hid,mass_storage,rndis", "0525", "a4a8", "", "", ""},
                {"Linux", "hid,mass_storage,rndis,adb", "0525", "a4a9", "", "", ""},
                {"Mac OS", "reset", "1234", "5678", "", "", ""},
                {"Mac OS", "hid", "05ac", "0201", "", "", ""},
                {"Mac OS", "hid,adb", "05ac", "0201", "", "", ""},
                {"Mac OS", "mass_storage", "0930", "6545", "", "", ""},
                {"Mac OS", "mass_storage,adb", "0930", "6545", "", "", ""},
                {"Mac OS", "acm,ecm", "1d6b", "0105", "", "", ""},
                {"Mac OS", "acm,ecm,adb", "1d6b", "0105", "", "", ""},
                {"Mac OS", "hid,mass_storage", "05ac", "0201", "", "", ""},
                {"Mac OS", "hid,mass_storage,adb", "05ac", "0201", "", "", ""},
                {"Mac OS", "hid,acm,ecm", "05ac", "0201", "", "", ""},
                {"Mac OS", "hid,acm,ecm,adb", "05ac", "0201", "", "", ""},
                {"Mac OS", "mass_storage,acm,ecm", "1d6b", "0105", "", "", ""},
                {"Mac OS", "mass_storage,acm,ecm,adb", "1d6b", "0105", "", "", ""},
                {"Mac OS", "hid,mass_storage,acm,ecm", "05ac", "0201", "", "", ""},
                {"Mac OS", "hid,mass_storage,acm,ecm,adb", "05ac", "0201", "", "", ""}
        };
        db.execSQL(
                "CREATE TABLE "
                        + TABLE_NAME
                        + " ("
                        + COLUMNS_USBSWITCH.get(0)
                        + " TEXT, "
                        + COLUMNS_USBSWITCH.get(1)
                        + " TEXT, "
                        + COLUMNS_USBSWITCH.get(2)
                        + " TEXT, "
                        + COLUMNS_USBSWITCH.get(3)
                        + " TEXT, "
                        + COLUMNS_USBSWITCH.get(4)
                        + " TEXT, "
                        + COLUMNS_USBSWITCH.get(5)
                        + " TEXT, "
                        + COLUMNS_USBSWITCH.get(6)
                        + " TEXT)");
        ContentValues initialValues = new ContentValues();
        db.beginTransaction();
        for (String[] data : USBSwitchData) {
            initialValues.put(COLUMNS_USBSWITCH.get(0), data[0]);
            initialValues.put(COLUMNS_USBSWITCH.get(1), data[1]);
            initialValues.put(COLUMNS_USBSWITCH.get(2), data[2]);
            initialValues.put(COLUMNS_USBSWITCH.get(3), data[3]);
            initialValues.put(COLUMNS_USBSWITCH.get(4), data[4]);
            initialValues.put(COLUMNS_USBSWITCH.get(5), data[5]);
            initialValues.put(COLUMNS_USBSWITCH.get(6), data[6]);
            db.insert(TABLE_NAME, null, initialValues);
            initialValues.clear();
        }
        db.setTransactionSuccessful();
        db.endTransaction();
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
    }

    @SuppressLint("Range")
    public USBArmorySwitchModel getUSBSwitchColumnData(
            String targetOSName, String functionName) {
        USBArmorySwitchModel USBArmorySwitchModel = new USBArmorySwitchModel();
        SQLiteDatabase db = this.getWritableDatabase();
        Cursor cursor =
                db.rawQuery(
                        "SELECT * FROM "
                                + TABLE_NAME
                                + " WHERE "
                                + COLUMNS_USBSWITCH.get(0)
                                + "='"
                                + targetOSName
                                + "'"
                                + " AND "
                                + COLUMNS_USBSWITCH.get(1)
                                + "='"
                                + functionName
                                + "';",
                        null);
        if (cursor.moveToFirst()) {
            USBArmorySwitchModel.setIdVendor(
                    cursor.getString(cursor.getColumnIndex(COLUMNS_USBSWITCH.get(2))));
            USBArmorySwitchModel.setIdProduct(
                    cursor.getString(cursor.getColumnIndex(COLUMNS_USBSWITCH.get(3))));
            USBArmorySwitchModel.setManufacturer(
                    cursor.getString(cursor.getColumnIndex(COLUMNS_USBSWITCH.get(4))));
            USBArmorySwitchModel.setProduct(
                    cursor.getString(cursor.getColumnIndex(COLUMNS_USBSWITCH.get(5))));
            USBArmorySwitchModel.setSerialNumber(
                    cursor.getString(cursor.getColumnIndex(COLUMNS_USBSWITCH.get(6))));
        }
        cursor.close();
        return USBArmorySwitchModel;
    }

    public boolean setUSBSwitchColumnData(
            String functionName, int targetColumnIndex, String targetOSName, String content) {
        try {
            SQLiteDatabase db = this.getWritableDatabase();
            db.execSQL(
                    "UPDATE "
                            + TABLE_NAME
                            + " SET "
                            + COLUMNS_USBSWITCH.get(targetColumnIndex)
                            + " = '"
                            + content
                            + "'"
                            + " WHERE "
                            + COLUMNS_USBSWITCH.get(0)
                            + " = '"
                            + targetOSName
                            + "'"
                            + " AND "
                            + COLUMNS_USBSWITCH.get(1)
                            + " = '"
                            + functionName
                            + "';");
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }

    public boolean resetData() {
        try {
            SQLiteDatabase db = this.getWritableDatabase();
            db.execSQL("DROP TABLE IF EXISTS " + TABLE_NAME);
            this.onCreate(db);
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }

    public String backupData(String storedDBpath) {
        try {
            String currentDBPath =
                    Environment.getDataDirectory()
                            + "/data/org.homicideware.stealthrabbit/databases/"
                            + getDatabaseName();
            if (Environment.getExternalStorageDirectory().canWrite()) {
                File currentDB = new File(currentDBPath);
                File backupDB = new File(storedDBpath);
                if (currentDB.exists()) {
                    FileChannel src = new FileInputStream(currentDB).getChannel();
                    FileChannel dst = new FileOutputStream(backupDB).getChannel();
                    dst.transferFrom(src, 0, src.size());
                    src.close();
                    dst.close();
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            return e.toString();
        }
        return null;
    }

    public String restoreData(String storedDBpath) {
        if (!new File(storedDBpath).exists()) {
            return "db file not found.";
        }

        SQLiteDatabase tempDB =
                SQLiteDatabase.openDatabase(storedDBpath, null, SQLiteDatabase.OPEN_READONLY);
        if (tempDB.getVersion() > this.getReadableDatabase().getVersion()) {
            tempDB.close();
            return "db cannot be restored.\n"
                    + "Reason: the db version of your backup db is newer than the current db"
                    + " version.";
        }
        tempDB.close();

        try {
            String currentDBPath =
                    Environment.getDataDirectory()
                            + "/data/org.homicideware.stealthrabbit/databases/"
                            + getDatabaseName();
            if (Environment.getExternalStorageDirectory().canWrite()) {
                File currentDB = new File(currentDBPath);
                File backupDB = new File(storedDBpath);
                if (backupDB.exists()) {
                    FileChannel src = new FileInputStream(backupDB).getChannel();
                    FileChannel dst = new FileOutputStream(currentDB).getChannel();
                    dst.transferFrom(src, 0, src.size());
                    src.close();
                    dst.close();
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            Log.e(TAG, e.toString());
            return e.toString();
        }
        return null;
    }
}
