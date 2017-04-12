/*
 *  Copyright 2017 Roy Van Liew
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package org.sosuml.sosumlapp;

import android.content.ContentValues;
import android.content.Context;
import android.content.pm.PackageInfo;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import org.sosuml.sosumlapp.MealDbDate;

/**
 * This class carries out the CRUD operations for all local SQLite databases
 * used in the app. This class really should be called AppDbHandler.
 *
 * @author Roy Van Liew
 */
public class MealDbHandler extends SQLiteOpenHelper {

    // All Static variables
    // Database Version
    private static final int DATABASE_VERSION = 1;

    // Database Name
    private static final String DATABASE_NAME = "MealDbDatesManager";

    // MealDbDates table names
    private static final String TABLE_MEALDBDATES = "MealDbDates";
    private static final String TABLE_REGISTER = "Register";
    private static final String TABLE_CALSIGN = "CalSign";

    // MealDbDates Table Columns names
    private static final String KEY_ID = "id";
    private static final String KEY_MONTH = "month";
    private static final String KEY_DAY = "day";
    private static final String KEY_YEAR = "year";

    // Register Table Columns names
    private static final String REGISTER_KEY_ID = "id";
    private static final String REGISTER_KEY_NAME = "name";
    private static final String REGISTER_KEY_STUDENT_EMAIL = "student_email";
    private static final String REGISTER_KEY_EMAIL = "email";

    // Register Table Columns names
    private static final String CALSIGN_KEY_ID = "id";
    private static final String CALSIGN_KEY_TITLE = "title";
    private static final String CALSIGN_KEY_TIME = "time";

    public MealDbHandler(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
        //3rd argument to be passed is CursorFactory instance
    }

    // Creating Tables
    @Override
    public void onCreate(SQLiteDatabase db) {

        // Create the Meal Db Dates Table
        Log.d("MealDbHandler: ", "onCreate: Creating Table " + TABLE_MEALDBDATES);
        String CREATE_MEALDBDATES_TABLE = "CREATE TABLE " + TABLE_MEALDBDATES + "("
                + KEY_ID + " INTEGER PRIMARY KEY," + KEY_MONTH + " INTEGER,"
                + KEY_DAY + " INTEGER," + KEY_YEAR + " INTEGER" + ")";
        db.execSQL(CREATE_MEALDBDATES_TABLE);

        // Create the Registration Dates Table
        Log.d("MealDbHandler: ", "onCreate: Creating Table " + TABLE_REGISTER);
        String CREATE_REGISTER_TABLE = "CREATE TABLE " + TABLE_REGISTER + "("
                + REGISTER_KEY_ID + " INTEGER PRIMARY KEY,"
                + REGISTER_KEY_NAME + " VARCHAR(100),"
                + REGISTER_KEY_STUDENT_EMAIL + " VARCHAR(100),"
                + REGISTER_KEY_EMAIL + " VARCHAR(100)" + ")";
        db.execSQL(CREATE_REGISTER_TABLE);

        // Create Calendar Signup Table
        Log.d("MealDbHandler: ", "onCreate: Creating Table " + TABLE_CALSIGN);
        String CREATE_CALSIGN_TABLE = "CREATE TABLE " + TABLE_CALSIGN + "("
                + CALSIGN_KEY_ID + " INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,"
                + CALSIGN_KEY_TITLE + " VARCHAR(100),"
                + CALSIGN_KEY_TIME + " VARCHAR(100)" + ")";
        db.execSQL(CREATE_CALSIGN_TABLE);
    }

    // Upgrading database
    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        // Drop older table if existed
        Log.d("MealDbHandler: ", "onUpgrade: Dropping and recreating tables");
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_MEALDBDATES);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_REGISTER);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_CALSIGN);

        // Create tables again
        onCreate(db);
    }

    /**
     * All CRUD(Create, Read, Update, Delete) Operations
     * Delete does not exist yet, not needed for the time being with just one entry
     */

    /* MEAL DONATION ENTRIES */

    // Adding new MealDbDate
    public void addMealDbDate(MealDbDate mealDbDate) {
        SQLiteDatabase db = this.getWritableDatabase();

        // Add the values in this entry we want to enter into the database
        ContentValues values = new ContentValues();
        values.put(KEY_MONTH, mealDbDate.getMonth()); // Month of most recent meal donation
        values.put(KEY_DAY, mealDbDate.getDay()); // Day of most recent meal donation
        values.put(KEY_YEAR, mealDbDate.getYear()); // Year of most recent meal donation

        // Inserting Row
        db.insert(TABLE_MEALDBDATES, null, values);
        //2nd argument is String containing nullColumnHack
        db.close(); // Closing database connection
    }

    // Getting the most recent Meal Donation
    public MealDbDate getMealDbDate(int id) {
        SQLiteDatabase db = this.getReadableDatabase();
        String[] columns = { KEY_ID, KEY_MONTH, KEY_DAY, KEY_YEAR };
        String[] selectionArgs = { String.valueOf(id) };

        // Query the database for our entry
        Cursor cursor = db.query(TABLE_MEALDBDATES, columns, KEY_ID + "=?",
                selectionArgs, null, null, null, null);
        if (cursor != null) {
            cursor.moveToFirst();
        }

        // In order we retrieve: id, month, day, and year for the constructor.
        MealDbDate mealDbDate = new MealDbDate(Integer.parseInt(cursor.getString(0)),
                Integer.parseInt(cursor.getString(1)), Integer.parseInt(cursor.getString(2)),
                Integer.parseInt(cursor.getString(3)));
        return mealDbDate; // Return the most recent Meal Donation
    }

    // Updating single MealDbDate
    public int updateMealDbDate(MealDbDate mealDbDate) {
        SQLiteDatabase db = this.getWritableDatabase();

        ContentValues values = new ContentValues();
        values.put(KEY_MONTH, mealDbDate.getMonth()); // Month of most recent meal donation
        values.put(KEY_DAY, mealDbDate.getDay()); // Day of most recent meal donation
        values.put(KEY_YEAR, mealDbDate.getYear()); // Year of most recent meal donation

        // Updating row entry
        return db.update(TABLE_MEALDBDATES, values, KEY_ID + " = ?",
                new String[] { String.valueOf(mealDbDate.getID()) });
    }

    /* REGISTRATION ENTRIES */

    // Adding new Register entry
    public void addRegister(RegisterDbEntry register) {
        SQLiteDatabase db = this.getWritableDatabase();

        // Add the values in this entry we want to enter into the database
        ContentValues values = new ContentValues();
        values.put(REGISTER_KEY_NAME, register.getName());
        values.put(REGISTER_KEY_STUDENT_EMAIL, register.getStudentEmail());
        values.put(REGISTER_KEY_EMAIL, register.getEmail());

        // Inserting Row
        db.insert(TABLE_REGISTER, null, values);
        //2nd argument is String containing nullColumnHack
        db.close(); // Closing database connection
    }

    // Getting the Register entry
    public RegisterDbEntry getRegister(int id) {
        SQLiteDatabase db = this.getReadableDatabase();
        String[] columns = { REGISTER_KEY_ID, REGISTER_KEY_NAME,
                REGISTER_KEY_STUDENT_EMAIL, REGISTER_KEY_EMAIL };
        String[] selectionArgs = { String.valueOf(id) };

        // Query the database for our entry
        Cursor cursor = db.query(TABLE_REGISTER, columns, REGISTER_KEY_ID + "=?",
                selectionArgs, null, null, null, null);
        if (cursor != null) {
            cursor.moveToFirst();
        }

        // In order we retrieve: id, month, day, and year for the constructor.
        RegisterDbEntry register = new RegisterDbEntry(Integer.parseInt(cursor.getString(0)),
                cursor.getString(1), cursor.getString(2), cursor.getString(3));
        return register; // Return the most recent Meal Donation
    }

    // Updating single Register
    public int updateRegister(RegisterDbEntry register) {
        SQLiteDatabase db = this.getWritableDatabase();

        ContentValues values = new ContentValues();
        values.put(REGISTER_KEY_NAME, register.getName());
        values.put(REGISTER_KEY_STUDENT_EMAIL, register.getStudentEmail());
        values.put(REGISTER_KEY_EMAIL, register.getEmail());

        // Updating row entry
        return db.update(TABLE_REGISTER, values, REGISTER_KEY_ID + " = ?",
                new String[] { String.valueOf(register.getID()) });
    }

    /* CALENDAR SIGNUP ENTRIES */

    public void addCalSign(CalSignupDbEntry calSignup) {
        SQLiteDatabase db = this.getWritableDatabase();

//        // Add the values in this entry we want to enter into the database
//        ContentValues values = new ContentValues();
//        values.put(CALSIGN_KEY_TITLE, calSignup.getTitle());
//        values.put(CALSIGN_KEY_TIME, calSignup.getTime());
//
//        // Inserting Row
//        db.insert(TABLE_CALSIGN, null, values);
//        //2nd argument is String containing nullColumnHack

        String sql = "INSERT INTO " + TABLE_CALSIGN + "("
                + CALSIGN_KEY_TITLE + ", " + CALSIGN_KEY_TIME + ") VALUES(\""
                + calSignup.getTitle() + "\",\"" + calSignup.getTime() + "\")";
        Log.d("addCalSign: ", "SQL statement is " + sql);
        db.execSQL(sql);
        db.close(); // Closing database connection
    }

    // Getting the Register entry
    public CalSignupDbEntry getCalSignById(int id) {
        SQLiteDatabase db = this.getReadableDatabase();
        String[] columns = { CALSIGN_KEY_ID, CALSIGN_KEY_TITLE, CALSIGN_KEY_TIME };
        String[] selectionArgs = { String.valueOf(id) };

        // Query the database for our entry
        Cursor cursor = db.query(TABLE_CALSIGN, columns, CALSIGN_KEY_ID + "=?",
                selectionArgs, null, null, null, null);
        if (cursor != null) {
            cursor.moveToFirst();
        }

        // In order we retrieve: id, month, day, and year for the constructor.
        CalSignupDbEntry calSign = new CalSignupDbEntry(Integer.parseInt(cursor.getString(0)),
                cursor.getString(1), cursor.getString(2));
        db.close(); // Closing database connection
        return calSign; // Return the most recent Meal Donation
    }

    // Updating single Calendar Signup Entry
    public int updateCalSign(CalSignupDbEntry calSign) {
        SQLiteDatabase db = this.getWritableDatabase();

        ContentValues values = new ContentValues();
        values.put(CALSIGN_KEY_ID, calSign.getID());
        values.put(CALSIGN_KEY_TITLE, calSign.getTitle());
        values.put(CALSIGN_KEY_TIME, calSign.getTime());

        // Updating row entry
        return db.update(TABLE_CALSIGN, values, CALSIGN_KEY_ID + " = ?",
                new String[] { String.valueOf(calSign.getID()) });
    }

    // Query for a specific event that the user might have signed up for.
    public CalSignupDbEntry getCalSignByDetails(String eventTitle, String eventTime) {
        SQLiteDatabase db = this.getReadableDatabase();
//        String[] returnColumns = {CALSIGN_KEY_TITLE, CALSIGN_KEY_TIME};
//        String selection = CALSIGN_KEY_TITLE + "=?" + " and "
//                + CALSIGN_KEY_TIME + "=?";
//        String[] selectionArgs = {eventTitle, eventTime};
//
//        // Query the database for our entry
//        Cursor cursor = db.query(TABLE_CALSIGN, returnColumns, selection,
//                selectionArgs, null, null, null, null);
        String sql = "SELECT * FROM " + TABLE_CALSIGN + " WHERE "
                + CALSIGN_KEY_TITLE + "=\"" + eventTitle + "\" AND "
                + CALSIGN_KEY_TIME + "=\"" + eventTime + "\";";
        Cursor cursor = db.rawQuery(sql, null);
        if (cursor != null) {
            cursor.moveToFirst();
        }

        // In order we retrieve: id, month, day, and year for the constructor.
        CalSignupDbEntry calSign = new CalSignupDbEntry(Integer.parseInt(cursor.getString(0)),
                cursor.getString(1), cursor.getString(2));
        db.close(); // Closing database connection
        return calSign; // Return the entry
    }

}
