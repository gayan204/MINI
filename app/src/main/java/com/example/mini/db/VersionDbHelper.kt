package com.example.mini.db

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import android.provider.BaseColumns

object DatabaseContract {
    object FileEntry : BaseColumns {
        const val TABLE_NAME = "files"
        const val COLUMN_NAME_URI = "uri"
        const val COLUMN_NAME_BASE_CONTENT = "base_content"
    }

    object VersionEntry : BaseColumns {
        const val TABLE_NAME = "versions"
        const val COLUMN_NAME_FILE_ID = "file_id"
        const val COLUMN_NAME_TIMESTAMP = "timestamp"
        const val COLUMN_NAME_PATCH = "patch"
        const val COLUMN_NAME_VERSION_NAME = "version_name"
    }
}

private const val SQL_CREATE_FILES =
    "CREATE TABLE ${DatabaseContract.FileEntry.TABLE_NAME} (" +
            "${BaseColumns._ID} INTEGER PRIMARY KEY," +
            "${DatabaseContract.FileEntry.COLUMN_NAME_URI} TEXT UNIQUE," +
            "${DatabaseContract.FileEntry.COLUMN_NAME_BASE_CONTENT} TEXT)"

private const val SQL_CREATE_VERSIONS =
    "CREATE TABLE ${DatabaseContract.VersionEntry.TABLE_NAME} (" +
            "${BaseColumns._ID} INTEGER PRIMARY KEY," +
            "${DatabaseContract.VersionEntry.COLUMN_NAME_FILE_ID} INTEGER," +
            "${DatabaseContract.VersionEntry.COLUMN_NAME_TIMESTAMP} INTEGER," +
            "${DatabaseContract.VersionEntry.COLUMN_NAME_PATCH} TEXT," +
            "${DatabaseContract.VersionEntry.COLUMN_NAME_VERSION_NAME} TEXT," +
            "FOREIGN KEY(${DatabaseContract.VersionEntry.COLUMN_NAME_FILE_ID}) REFERENCES ${DatabaseContract.FileEntry.TABLE_NAME}(${BaseColumns._ID}))"

private const val SQL_DELETE_FILES = "DROP TABLE IF EXISTS ${DatabaseContract.FileEntry.TABLE_NAME}"
private const val SQL_DELETE_VERSIONS = "DROP TABLE IF EXISTS ${DatabaseContract.VersionEntry.TABLE_NAME}"

class VersionDbHelper(context: Context) : SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {
    override fun onConfigure(db: SQLiteDatabase) {
        super.onConfigure(db)
        db.setForeignKeyConstraintsEnabled(true)
    }

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL(SQL_CREATE_FILES)
        db.execSQL(SQL_CREATE_VERSIONS)
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        db.execSQL(SQL_DELETE_VERSIONS)
        db.execSQL(SQL_DELETE_FILES)
        onCreate(db)
    }

    companion object {
        const val DATABASE_VERSION = 1
        const val DATABASE_NAME = "VersionControl.db"
    }
}
