package de.ox539.ben.myapplication;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import java.sql.SQLException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;

public class MenuDbAdapter {
    private static final String TAG = "MenuDbAdapter";

    private static final String KEY_ID = "\"id\"";
    private static final String KEY_DATE = "\"date\"";
    private static final String QUERY_WHERE =
            "DATETIME(" + KEY_DATE + ", '1 days', '2 hours', 'utc') > DATETIME('now')";
    private static final String KEY_MEAL1 = "\"meal1\"";
    private static final String KEY_MEAL2 = "\"meal2\"";
    private static final String[] KEYS_DATE_MEALS = {
            KEY_DATE,
            KEY_MEAL1,
            KEY_MEAL2
    };
    private static final String TABLE_NAME = "menu";
    private static final String TABLE_CREATE =
            "CREATE TABLE \"" + TABLE_NAME + "\" (\n"
                    + "  " + KEY_ID + "  INTEGER  PRIMARY KEY  AUTOINCREMENT,\n"
                    + "  " + KEY_DATE + "  STRING UNIQUE,\n"
                    + "  " + KEY_MEAL1 + "  STRING,\n"
                    + "  " + KEY_MEAL2 + "  STRING\n"
                    + ")";
    private static final String DB_NAME = "menu-db";
    private static final int DB_VERSION = 1;
    private Context ctx;
    private DatabaseHelper helper;
    private SQLiteDatabase db;

    public MenuDbAdapter(Context c) {
        ctx = c;
    }

    public MenuDbAdapter open() throws SQLException {
        helper = new DatabaseHelper(ctx);
        db = helper.getWritableDatabase();
        return this;
    }

    public void close() {
        helper.close();
    }

    public void insert(DailyMenu menu) {
        ContentValues values = new ContentValues(3);
        values.put(KEY_DATE, MenuActivity.YYYY_MM_DD_FORMAT.format(menu.date));
        values.put(KEY_MEAL1, menu.meal1);
        values.put(KEY_MEAL2, menu.meal2);
        db.insertWithOnConflict(TABLE_NAME, null, values, SQLiteDatabase.CONFLICT_REPLACE);
    }

    public List<DailyMenu> getCurrentMenus() {
        List<DailyMenu> result = new ArrayList<DailyMenu>();
        Cursor c = db.query(
                TABLE_NAME,
                KEYS_DATE_MEALS,
                /* WHERE */ QUERY_WHERE, null,
                /* GROUP BY */ null, /* HAVING */ null,
                /* ORDER BY */ KEY_DATE + " ASC",
                /* LIMIT */ "7");
        if (c == null) {
            Log.e(TAG, "db returned no cursor");
            return result;
        }
        try {
            c.moveToFirst();
            while (!c.isAfterLast()) {
                DailyMenu menu = new DailyMenu();
                try {
                    menu.date = MenuActivity.YYYY_MM_DD_FORMAT.parse(c.getString(0));
                } catch (ParseException e) {
                    Log.e(TAG, "bad date in db, skipping: " + c.getString(0));
                    continue;
                }
                menu.meal1 = c.getString(1);
                menu.meal2 = c.getString(2);
                c.moveToNext();
                result.add(menu);
            }
        } finally {
            c.close();
        }

        return result;
    }

    private static class DatabaseHelper extends SQLiteOpenHelper {
        DatabaseHelper(Context context) {
            super(context, DB_NAME, null, DB_VERSION);
        }

        @Override
        public void onCreate(SQLiteDatabase db) {

            db.execSQL(TABLE_CREATE);
        }

        @Override
        public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
            Log.w(TAG, "Upgrading database from version " + oldVersion + " to "
                    + newVersion + ", which will destroy all old data");
            db.execSQL("DROP TABLE IF EXISTS " + TABLE_NAME);
            onCreate(db);
        }
    }
}
