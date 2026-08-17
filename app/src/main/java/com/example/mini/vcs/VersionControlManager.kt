package com.example.mini.vcs

import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.provider.BaseColumns
import com.example.mini.db.DatabaseContract
import com.example.mini.db.VersionDbHelper
import com.github.difflib.DiffUtils
import com.github.difflib.UnifiedDiffUtils

data class VersionInfo(val timestamp: Long, val patchString: String, val versionName: String)

class VersionControlManager(context: Context) {
    private val dbHelper = VersionDbHelper(context)

    fun saveVersion(uri: Uri, content: String, versionName: String) {
        val db = dbHelper.writableDatabase
        
        // 1. Check if file exists in DB
        val projection = arrayOf(
            BaseColumns._ID,
            DatabaseContract.FileEntry.COLUMN_NAME_BASE_CONTENT
        )
        val selection = "${DatabaseContract.FileEntry.COLUMN_NAME_URI} = ?"
        val selectionArgs = arrayOf(uri.toString())
        
        val cursor = db.query(
            DatabaseContract.FileEntry.TABLE_NAME,
            projection,
            selection,
            selectionArgs,
            null,
            null,
            null
        )
        
        var fileId: Long = -1
        var previousState = ""

        if (cursor.moveToFirst()) {
            fileId = cursor.getLong(cursor.getColumnIndexOrThrow(BaseColumns._ID))
            // Reconstruct the latest state to diff against
            previousState = getLatestState(fileId)
        } else {
            // New file, insert base content
            val values = ContentValues().apply {
                put(DatabaseContract.FileEntry.COLUMN_NAME_URI, uri.toString())
                put(DatabaseContract.FileEntry.COLUMN_NAME_BASE_CONTENT, content)
            }
            fileId = db.insert(DatabaseContract.FileEntry.TABLE_NAME, null, values)
            previousState = content
        }
        cursor.close()

        // 2. Generate Diff Patch if it's a subsequent save, or an empty patch if it's the base
        val previousLines = previousState.split("\n")
        val currentLines = content.split("\n")
        val patch = DiffUtils.diff(previousLines, currentLines)
        val unifiedDiff = UnifiedDiffUtils.generateUnifiedDiff("original", "revised", previousLines, patch, 3)
        val patchString = unifiedDiff.joinToString("\n")

        // 3. Store the version
        val versionValues = ContentValues().apply {
            put(DatabaseContract.VersionEntry.COLUMN_NAME_FILE_ID, fileId)
            put(DatabaseContract.VersionEntry.COLUMN_NAME_TIMESTAMP, System.currentTimeMillis())
            put(DatabaseContract.VersionEntry.COLUMN_NAME_PATCH, patchString)
            put(DatabaseContract.VersionEntry.COLUMN_NAME_VERSION_NAME, versionName)
        }
        db.insert(DatabaseContract.VersionEntry.TABLE_NAME, null, versionValues)
    }

    private fun getLatestState(fileId: Long): String {
        val db = dbHelper.readableDatabase
        
        // Get base content
        var baseContent = ""
        val cursorBase = db.query(
            DatabaseContract.FileEntry.TABLE_NAME,
            arrayOf(DatabaseContract.FileEntry.COLUMN_NAME_BASE_CONTENT),
            "${BaseColumns._ID} = ?",
            arrayOf(fileId.toString()),
            null, null, null
        )
        if (cursorBase.moveToFirst()) {
            baseContent = cursorBase.getString(cursorBase.getColumnIndexOrThrow(DatabaseContract.FileEntry.COLUMN_NAME_BASE_CONTENT))
        }
        cursorBase.close()

        // Get all patches
        val cursorPatches = db.query(
            DatabaseContract.VersionEntry.TABLE_NAME,
            arrayOf(DatabaseContract.VersionEntry.COLUMN_NAME_PATCH),
            "${DatabaseContract.VersionEntry.COLUMN_NAME_FILE_ID} = ?",
            arrayOf(fileId.toString()),
            null, null, "${DatabaseContract.VersionEntry.COLUMN_NAME_TIMESTAMP} ASC"
        )

        var currentStateLines = baseContent.split("\n").toMutableList()

        while (cursorPatches.moveToNext()) {
            val patchString = cursorPatches.getString(cursorPatches.getColumnIndexOrThrow(DatabaseContract.VersionEntry.COLUMN_NAME_PATCH))
            if (patchString.isNotEmpty()) {
                val patchLines = patchString.split("\n")
                val parsedPatch = UnifiedDiffUtils.parseUnifiedDiff(patchLines)
                currentStateLines = DiffUtils.patch(currentStateLines, parsedPatch).toMutableList()
            }
        }
        cursorPatches.close()

        return currentStateLines.joinToString("\n")
    }

    fun getVersions(uri: Uri): List<VersionInfo> {
        val db = dbHelper.readableDatabase
        val versions = mutableListOf<VersionInfo>()

        // Get fileId
        var fileId: Long = -1
        val cursorBase = db.query(
            DatabaseContract.FileEntry.TABLE_NAME,
            arrayOf(BaseColumns._ID),
            "${DatabaseContract.FileEntry.COLUMN_NAME_URI} = ?",
            arrayOf(uri.toString()),
            null, null, null
        )
        if (cursorBase.moveToFirst()) {
            fileId = cursorBase.getLong(cursorBase.getColumnIndexOrThrow(BaseColumns._ID))
        }
        cursorBase.close()

        if (fileId == -1L) return versions

        // Get versions
        val cursorVersions = db.query(
            DatabaseContract.VersionEntry.TABLE_NAME,
            arrayOf(
                DatabaseContract.VersionEntry.COLUMN_NAME_TIMESTAMP,
                DatabaseContract.VersionEntry.COLUMN_NAME_PATCH,
                DatabaseContract.VersionEntry.COLUMN_NAME_VERSION_NAME
            ),
            "${DatabaseContract.VersionEntry.COLUMN_NAME_FILE_ID} = ?",
            arrayOf(fileId.toString()),
            null, null, "${DatabaseContract.VersionEntry.COLUMN_NAME_TIMESTAMP} ASC"
        )

        while (cursorVersions.moveToNext()) {
            val timestamp = cursorVersions.getLong(cursorVersions.getColumnIndexOrThrow(DatabaseContract.VersionEntry.COLUMN_NAME_TIMESTAMP))
            val patch = cursorVersions.getString(cursorVersions.getColumnIndexOrThrow(DatabaseContract.VersionEntry.COLUMN_NAME_PATCH))
            val name = cursorVersions.getString(cursorVersions.getColumnIndexOrThrow(DatabaseContract.VersionEntry.COLUMN_NAME_VERSION_NAME))
            versions.add(VersionInfo(timestamp, patch, name))
        }
        cursorVersions.close()

        return versions
    }

    fun getVersionState(uri: Uri, targetTimestamp: Long): String? {
        val db = dbHelper.readableDatabase
        
        var fileId: Long = -1
        var baseContent = ""
        val cursorBase = db.query(
            DatabaseContract.FileEntry.TABLE_NAME,
            arrayOf(BaseColumns._ID, DatabaseContract.FileEntry.COLUMN_NAME_BASE_CONTENT),
            "${DatabaseContract.FileEntry.COLUMN_NAME_URI} = ?",
            arrayOf(uri.toString()),
            null, null, null
        )
        if (cursorBase.moveToFirst()) {
            fileId = cursorBase.getLong(cursorBase.getColumnIndexOrThrow(BaseColumns._ID))
            baseContent = cursorBase.getString(cursorBase.getColumnIndexOrThrow(DatabaseContract.FileEntry.COLUMN_NAME_BASE_CONTENT))
        }
        cursorBase.close()

        if (fileId == -1L) return null

        val cursorPatches = db.query(
            DatabaseContract.VersionEntry.TABLE_NAME,
            arrayOf(DatabaseContract.VersionEntry.COLUMN_NAME_PATCH),
            "${DatabaseContract.VersionEntry.COLUMN_NAME_FILE_ID} = ? AND ${DatabaseContract.VersionEntry.COLUMN_NAME_TIMESTAMP} <= ?",
            arrayOf(fileId.toString(), targetTimestamp.toString()),
            null, null, "${DatabaseContract.VersionEntry.COLUMN_NAME_TIMESTAMP} ASC"
        )

        var currentStateLines = baseContent.split("\n").toMutableList()

        while (cursorPatches.moveToNext()) {
            val patchString = cursorPatches.getString(cursorPatches.getColumnIndexOrThrow(DatabaseContract.VersionEntry.COLUMN_NAME_PATCH))
            if (patchString.isNotEmpty()) {
                val patchLines = patchString.split("\n")
                val parsedPatch = UnifiedDiffUtils.parseUnifiedDiff(patchLines)
                currentStateLines = DiffUtils.patch(currentStateLines, parsedPatch).toMutableList()
            }
        }
        cursorPatches.close()

        return currentStateLines.joinToString("\n")
    }
}
