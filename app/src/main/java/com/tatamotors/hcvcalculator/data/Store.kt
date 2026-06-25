package com.tatamotors.hcvcalculator.data

import android.content.Context
import android.content.SharedPreferences
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Offline persistence via SharedPreferences + JSON (no extra dependencies).
 *
 * Two kinds of data:
 *  - DRAFTS: the live working state of each calculator, auto-saved on every change
 *    so closing the app without saving still restores the last entered values.
 *  - ESTIMATES: explicitly saved snapshots, listed on the dashboard; tapping one
 *    loads it back into the calculator.
 */
object Store {
    private const val PREFS = "hcv_store"
    private const val KEY_ESTIMATES = "estimates"
    private lateinit var prefs: SharedPreferences

    fun init(context: Context) {
        if (!::prefs.isInitialized)
            prefs = context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
    }

    // ---------- Drafts ----------
    fun saveDraft(key: String, json: String) = prefs.edit().putString("draft_$key", json).apply()
    fun loadDraft(key: String): String? = prefs.getString("draft_$key", null)

    // ---------- Saved estimates ----------
    data class Estimate(
        val id: Long,
        val type: String,          // "FEMAX" | "HPT" | "BRT"
        val customerName: String,
        val location: String,
        val route: String,
        val savedAt: Long,
        val payload: String        // calculator state JSON
    ) {
        val savedAtText: String
            get() = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale("en", "IN")).format(Date(savedAt))
        val typeLabel: String
            get() = when (type) {
                "FEMAX" -> "FE MaX"
                "HPT" -> "Revenue MaX"
                else -> "BRT"
            }
    }

    private fun readAll(): JSONArray =
        try { JSONArray(prefs.getString(KEY_ESTIMATES, "[]")) } catch (_: Exception) { JSONArray() }

    private fun writeAll(arr: JSONArray) =
        prefs.edit().putString(KEY_ESTIMATES, arr.toString()).apply()

    fun saveEstimate(type: String, customerName: String, location: String, route: String, payload: String): Long {
        val id = System.currentTimeMillis()
        val o = JSONObject()
            .put("id", id).put("type", type)
            .put("customerName", customerName).put("location", location).put("route", route)
            .put("savedAt", id).put("payload", payload)
        val arr = readAll()
        arr.put(o)
        writeAll(arr)
        return id
    }

    fun listEstimates(): List<Estimate> {
        val arr = readAll()
        val out = ArrayList<Estimate>(arr.length())
        for (i in 0 until arr.length()) {
            val o = arr.optJSONObject(i) ?: continue
            out.add(
                Estimate(
                    id = o.optLong("id"),
                    type = o.optString("type"),
                    customerName = o.optString("customerName"),
                    location = o.optString("location"),
                    route = o.optString("route"),
                    savedAt = o.optLong("savedAt"),
                    payload = o.optString("payload")
                )
            )
        }
        return out.sortedByDescending { it.savedAt }
    }

    fun deleteEstimate(id: Long) {
        val arr = readAll()
        val keep = JSONArray()
        for (i in 0 until arr.length()) {
            val o = arr.optJSONObject(i) ?: continue
            if (o.optLong("id") != id) keep.put(o)
        }
        writeAll(keep)
    }
}
