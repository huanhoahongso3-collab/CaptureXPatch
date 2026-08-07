package dhp.thl.tpl.capturexpatch.hookers

import android.content.Context
import android.database.Cursor
import android.database.MatrixCursor
import android.net.Uri

/*
    Detection/filtering logic shared between the modern libxposed-API hooker
    (ScreenCaptureDetectionHooker, loaded via META-INF/xposed/java_init.list) and the legacy
    IXposedHookLoadPackage hooker (LegacyScreenCaptureDetectionHooker, loaded via
    assets/xposed_init). Some LSPatch/NPatch manager builds only expose modules to dynamic/live
    scope through the legacy loading path, so both entry points must produce identical behavior.
 */
object ScreenCaptureFilterUtils {

    // Set while we issue our own verification query (e.g. from isScreenshotUri) so our own
    // ContentResolver.query hooks don't recursively filter/consume that internal lookup.
    private val inInternalQuery = ThreadLocal.withInitial { false }

    fun rowIsScreenshot(cursor: Cursor, log: (String) -> Unit): Boolean {
        for (colName in cursor.columnNames) {
            val index = cursor.getColumnIndex(colName)
            if (index >= 0) {
                try {
                    val value = cursor.getString(index)
                    if (value != null && value.contains("screenshot", ignoreCase = true)) {
                        log("Found screenshot keyword in column $colName: $value")
                        return true
                    }
                } catch (e: Throwable) {
                    // Not a string column, ignore
                }
            }
        }
        return false
    }

    // Filters both single-row cursors (query by item id) and multi-row cursors (apps that scan
    // the N most recent images looking for a screenshot among them). Screenshot rows are
    // stripped out instead of nulling the whole cursor, so callers that don't null-check don't
    // crash or fall back to another detection path.
    fun filterCursor(result: Any?, uri: Uri?, log: (String) -> Unit): Any? {
        if (inInternalQuery.get()) return result
        val cursor = result as? Cursor ?: return result
        try {
            val columnNames = cursor.columnNames
            val matrix = MatrixCursor(columnNames)
            var removedAny = false
            if (cursor.moveToFirst()) {
                do {
                    if (rowIsScreenshot(cursor, log)) {
                        removedAny = true
                        continue
                    }
                    val row = arrayOfNulls<Any>(columnNames.size)
                    for (i in columnNames.indices) {
                        row[i] = when (cursor.getType(i)) {
                            Cursor.FIELD_TYPE_INTEGER -> cursor.getLong(i)
                            Cursor.FIELD_TYPE_FLOAT -> cursor.getDouble(i)
                            Cursor.FIELD_TYPE_BLOB -> cursor.getBlob(i)
                            Cursor.FIELD_TYPE_NULL -> null
                            else -> cursor.getString(i)
                        }
                    }
                    matrix.addRow(row)
                } while (cursor.moveToNext())
            }
            if (!removedAny) {
                cursor.moveToFirst()
                return cursor
            }
            log("Filtered screenshot row(s) from query result for URI: $uri")
            return matrix
        } catch (e: Throwable) {
            log("Error filtering cursor: $e")
        }
        return cursor
    }

    // android.app.ActivityThread is a hidden API: not present in the public SDK stubs used for
    // compilation, so it must be reached via reflection rather than a direct class reference.
    fun currentApplicationContext(log: (String) -> Unit): Context? {
        return try {
            val activityThreadClass = Class.forName("android.app.ActivityThread")
            val currentApplicationMethod = activityThreadClass.getMethod("currentApplication")
            currentApplicationMethod.invoke(null) as? Context
        } catch (e: Throwable) {
            log("Could not obtain application context: $e")
            null
        }
    }

    fun isScreenshotUri(uri: Uri?, log: (String) -> Unit): Boolean {
        if (uri == null) return false
        try {
            val context = currentApplicationContext(log) ?: return false
            val resolver = context.contentResolver
            inInternalQuery.set(true)
            try {
                val cursor: Cursor? = resolver.query(uri, null, null, null, null)
                cursor?.use {
                    if (it.moveToFirst()) {
                        return rowIsScreenshot(it, log)
                    }
                }
            } finally {
                inInternalQuery.set(false)
            }
        } catch (e: Throwable) {
            log("Error checking dispatchChange URI: $e")
        }
        return false
    }
}
