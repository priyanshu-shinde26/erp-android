package com.campussync.erp.academics;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class AcademicDBHelper extends SQLiteOpenHelper {

    private static final String DB_NAME = "academic_db";
    private static final int DB_VERSION = 1;

    public AcademicDBHelper(Context context) {
        super(context, DB_NAME, null, DB_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(
                "CREATE TABLE student_results (" +
                        "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                        "subject TEXT," +
                        "test TEXT," +
                        "marks INTEGER," +
                        "maxMarks INTEGER" +
                        ")"
        );
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS student_results");
        onCreate(db);
    }
}
