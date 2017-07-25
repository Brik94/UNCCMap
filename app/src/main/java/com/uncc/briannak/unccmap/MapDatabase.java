package com.uncc.briannak.unccmap;

import android.app.SearchManager;
import android.content.ContentValues;
import android.content.Context;
import android.content.res.Resources;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.database.sqlite.SQLiteQueryBuilder;
import android.provider.BaseColumns;
import android.text.TextUtils;
import android.util.Log;



import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashMap;

/**
 * Logic to return specific search codes or building information from text files, and
 * load the text files to tables in a database when they need to be created.
 */
public class MapDatabase
{
    private static final String TAG = "MapDatabase";

    //The columns we'll include in the academic buildings table
    public static final String SEARCH_CODE = SearchManager.SUGGEST_COLUMN_TEXT_1;
    public static final String BUILDING_NAME = SearchManager.SUGGEST_COLUMN_TEXT_2;
    public static final String LATITUDE = "LATITUDE";
    public static final String LONGITUDE ="LONGITUDE";

    private static final String DATABASE_NAME = "UNCCMAP";
    private static final String FTS_VIRTUAL_TABLE = "FTSACBUILDINGS";
    private static final int DATABASE_VERSION = 2;

    private final MapDBOpenHelper mDatabaseOpenHelper;
    private static final HashMap<String,String> mColumnMap = buildColumnMap();

    /**
     * Constructor
     * @param context The Context within which to work, used to create the DB
     */
    public MapDatabase(Context context)
    {
        mDatabaseOpenHelper = new MapDBOpenHelper(context);
    }

    /**
     * Builds a map for all columns that may be requested, which will be given to the 
     * SQLiteQueryBuilder. This is a good way to define aliases for column names, but must include 
     * all columns, even if the value is the key. This allows the ContentProvider to request
     * columns w/o the need to know real column names and create the alias itself.
     */
    private static HashMap<String,String> buildColumnMap() 
    {
        HashMap<String,String> map = new HashMap<String,String>();
        map.put(SEARCH_CODE, SEARCH_CODE);
        map.put(BUILDING_NAME, BUILDING_NAME);
        map.put(LATITUDE, LATITUDE);
        map.put(LONGITUDE, LONGITUDE);
        map.put(BaseColumns._ID, "rowid AS " + BaseColumns._ID);
        map.put(SearchManager.SUGGEST_COLUMN_INTENT_DATA_ID, "rowid AS " + SearchManager.SUGGEST_COLUMN_INTENT_DATA_ID);
        map.put(SearchManager.SUGGEST_COLUMN_SHORTCUT_ID, "rowid AS " + SearchManager.SUGGEST_COLUMN_SHORTCUT_ID);
        return map;
    }

    /**
     * Returns a Cursor positioned at the word specified by rowId
     *
     * @param rowId id of word to retrieve
     * @param columns The columns to include, if null then all are included
     * @return Cursor positioned to matching word, or null if not found.
     */
    public Cursor getCode(String rowId, String[] columns)
    {
        String selection = "rowid = ?";
        String[] selectionArgs = new String[] {rowId};

        return query(selection, selectionArgs, columns);
    }

    /**
     * Returns a Cursor over all codes that match the given query
     *
     * @param query The string to search for
     * @param columns The columns to include, if null then all are included
     * @return Cursor over all words that match, or null if none found.
     */
    public Cursor getCodeMatches(String query, String[] columns)
    {
        String selection = SEARCH_CODE + " MATCH ?";
        String[] selectionArgs = new String[] {query+"*"};

        return query(selection, selectionArgs, columns);
    }

    /**
     * Performs a database query.
     * @param selection The selection clause
     * @param selectionArgs Selection arguments for "?" components in the selection
     * @param columns The columns to return
     * @return A Cursor over all rows matching the query
     */
    private Cursor query(String selection, String[] selectionArgs, String[] columns) 
    {
        /* The SQLiteBuilder provides a map for all possible columns requested to
         * actual columns in the database, creating a simple column alias mechanism
         * by which the ContentProvider does not need to know the real column names
         */
        SQLiteQueryBuilder builder = new SQLiteQueryBuilder();
        builder.setTables(FTS_VIRTUAL_TABLE);
        builder.setProjectionMap(mColumnMap);

        Cursor cursor = builder.query(mDatabaseOpenHelper.getReadableDatabase(),
                columns, selection, selectionArgs, null, null, null);

        if (cursor == null) 
        {
            return null;
        } 
        else if (!cursor.moveToFirst()) 
        {
            cursor.close();
            return null;
        }
        return cursor;
    }


    /**
     * This creates/opens the database.
     */

    private static class MapDBOpenHelper extends SQLiteOpenHelper {
        private final Context mHelperContext;
        private SQLiteDatabase mDatabase;

        /* Note that FTS3 does not support column constraints and thus, you cannot
         * declare a primary key. However, "rowid" is automatically used as a unique
         * identifier, so when making requests, we will use "_id" as an alias for "rowid"
         */

        private static final String FTS_TABLE_CREATE =
                "CREATE VIRTUAL TABLE " + FTS_VIRTUAL_TABLE +
                        " USING fts3 (" +
                        SEARCH_CODE + ", " +
                        BUILDING_NAME + ", " +
                        LATITUDE + ", " +
                        LONGITUDE + ")";



        MapDBOpenHelper(Context context)
        {
            super(context, DATABASE_NAME, null, DATABASE_VERSION);
            mHelperContext = context;
        }

        @Override
        public void onCreate(SQLiteDatabase db)
        {
            mDatabase = db;
            mDatabase.execSQL(FTS_TABLE_CREATE);
            loadMapDB();
        }

        /**
         * Starts a thread to load the database table with data
         */
        private void loadMapDB()
        {
            new Thread(new Runnable() 
            {
                public void run() 
                {
                    try 
                    {
                        loadWords();
                    }
                    catch (IOException e) 
                    {
                        throw new RuntimeException(e);
                    }
                }
            }).start();
        }


        private void loadWords() throws IOException 
        {
            Log.d(TAG, "Loading txt file data...");
            final Resources resources = mHelperContext.getResources();
            InputStream inputStream = resources.openRawResource(R.raw.acbuildings2);
            BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));

            try 
            {
                String line;
                while ((line = reader.readLine()) != null) 
                {
                    String[] strings = TextUtils.split(line, ",");
                    if (strings.length < 2) continue;
                    long id = addBuilding(strings[0].trim(), strings[1].trim(), strings[2].trim(), strings[3].trim());
                    
                    if (id < 0) 
                    {
                        Log.e(TAG, "unable to add building: " + strings[0].trim());
                    }
                }
            }
            finally 
            {
                reader.close();
            }
            Log.d(TAG, "DONE loading buildings.");
        }

        /**
         * Add a building row to the database.
         * @return rowId or -1 if failed
         */
        public long addBuilding(String searchCode, String building, String lat, String longz)
        {
            ContentValues initialValues = new ContentValues();
            initialValues.put(SEARCH_CODE, searchCode);
            initialValues.put(BUILDING_NAME, building);
            initialValues.put(LATITUDE, lat);
            initialValues.put(LONGITUDE, longz);

            return mDatabase.insert(FTS_VIRTUAL_TABLE, null, initialValues);
        }

        @Override
        public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) 
        {
            Log.w(TAG, "Upgrading database from version " + oldVersion + " to "
                    + newVersion + ", which will destroy all old data");
            db.execSQL("DROP TABLE IF EXISTS " + FTS_VIRTUAL_TABLE);
            onCreate(db);
        }
    }
}