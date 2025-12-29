package com.donatienthorez.ugandai.chat.ui;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import androidx.annotation.Nullable;

public class DatabaseHelper extends SQLiteOpenHelper {

    public static final String databaseName = "SignLog.db";

    public DatabaseHelper(@Nullable Context context) {
        super(context, "SignLog.db", null, 2);
    }

    @Override
    public void onCreate(SQLiteDatabase MyDatabase) {
        MyDatabase.execSQL("create Table users(email TEXT primary key, password TEXT)");
        MyDatabase.execSQL("create Table farm_activities(" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "user_id TEXT NOT NULL, " +
                "activity_type TEXT NOT NULL, " +
                "date TEXT NOT NULL, " +
                "crop TEXT, " +
                "field TEXT, " +
                "note TEXT, " +
                "created_at INTEGER NOT NULL)");
    }

    @Override
    public void onUpgrade(SQLiteDatabase MyDB, int oldVersion, int newVersion) {
        if (oldVersion < 2) {
            MyDB.execSQL("create Table farm_activities(" +
                    "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    "user_id TEXT NOT NULL, " +
                    "activity_type TEXT NOT NULL, " +
                    "date TEXT NOT NULL, " +
                    "crop TEXT, " +
                    "field TEXT, " +
                    "note TEXT, " +
                    "created_at INTEGER NOT NULL)");
        }
    }

    public Boolean insertData(String email, String password){
        SQLiteDatabase MyDatabase = this.getWritableDatabase();
        ContentValues contentValues = new ContentValues();
        contentValues.put("email", email);
        contentValues.put("password", password);
        long result = MyDatabase.insert("users", null, contentValues);

        if (result == -1) {
            return false;
        } else {
            return true;
        }
    }

    public Boolean checkEmail(String email){
        SQLiteDatabase MyDatabase = this.getWritableDatabase();
        Cursor cursor = MyDatabase.rawQuery("Select * from users where email = ?", new String[]{email});

        if(cursor.getCount() > 0) {
            return true;
        }else {
            return false;
        }
    }
    public Boolean checkEmailPassword(String email, String password){
        SQLiteDatabase MyDatabase = this.getWritableDatabase();
        Cursor cursor = MyDatabase.rawQuery("Select * from users where email = ? and password = ?", new String[]{email, password});

        if (cursor.getCount() > 0) {
            return true;
        }else {
            return false;
        }
    }
}