package com.example.cs205;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class HighScoreDatabaseHelper extends SQLiteOpenHelper {

    private static final String DATABASE_NAME = "counter.db";
    private static final int DATABASE_VERSION = 1;
    private static final String TABLE_NAME = "counter";
    private static final String COLUMN_ID = "id";
    private static final String COLUMN_VALUE = "value";

    public HighScoreDatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        String createTable = "CREATE TABLE " + TABLE_NAME + " (" +
                COLUMN_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                COLUMN_VALUE + " INTEGER)";
        db.execSQL(createTable);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_NAME);
        onCreate(db);
    }

    public void saveHighestCounter(SQLiteDatabase db, int counter) {
        Cursor cursor = db.rawQuery("SELECT MAX(" + COLUMN_VALUE + ") FROM " + TABLE_NAME, null);
        int highest = 0;
        if (cursor.moveToFirst()) {
            highest = cursor.getInt(0);
        }
        cursor.close();

        if (counter > highest) {
            db.execSQL("INSERT INTO " + TABLE_NAME + " (" + COLUMN_VALUE + ") VALUES (" + counter + ")");
        }
    }

    public int getHighestCounter(SQLiteDatabase db) {
        Cursor cursor = db.rawQuery("SELECT MAX(" + COLUMN_VALUE + ") FROM " + TABLE_NAME, null);
        int highest = 0;
        if (cursor.moveToFirst()) {
            highest = cursor.getInt(0);
        }
        cursor.close();
        return highest;
    }
}
