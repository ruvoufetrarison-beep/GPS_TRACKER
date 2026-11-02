package com.fanofeno.gpstracker

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import org.json.JSONObject

class QueueDb(context: Context) : SQLiteOpenHelper(context, "gps_queue.db", null, 1) {
    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL("""CREATE TABLE points (
            id INTEGER PRIMARY KEY AUTOINCREMENT,
            payload TEXT NOT NULL,
            created_at INTEGER NOT NULL
        )""")
    }
    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {}
    fun add(point: JSONObject) {
        writableDatabase.insert("points", null, ContentValues().apply {
            put("payload", point.toString()); put("created_at", System.currentTimeMillis())
        })
    }
    fun pending(limit: Int = 30): List<Pair<Long, JSONObject>> {
        val out = mutableListOf<Pair<Long, JSONObject>>()
        readableDatabase.rawQuery("SELECT id,payload FROM points ORDER BY id LIMIT ?", arrayOf(limit.toString())).use { c ->
            while (c.moveToNext()) out.add(c.getLong(0) to JSONObject(c.getString(1)))
        }
        return out
    }
    fun delete(id: Long) { writableDatabase.delete("points", "id=?", arrayOf(id.toString())) }
    fun count(): Int {
        readableDatabase.rawQuery("SELECT COUNT(*) FROM points", null).use { c -> return if (c.moveToFirst()) c.getInt(0) else 0 }
    }
}
