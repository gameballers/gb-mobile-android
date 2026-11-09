package com.gameball.gameball.engagement

import android.content.Context
import android.util.Log
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject

/**
 * Received pushes on disk, in their own SharedPreferences file. The app is usually killed
 * between a push arriving and the next open, so memory isn't enough.
 *
 * Written with org.json and fixed field names, so an app's minification can't rename them.
 */
internal class ReceivedPushes(context: Context) : ReceivedPushStore {

    private val prefs = context.applicationContext.getSharedPreferences(FILE, Context.MODE_PRIVATE)

    override fun load(): List<ReceivedPush> = try {
        val array = JSONArray(prefs.getString(KEY, null) ?: return emptyList())
        (0 until array.length()).map { i ->
            val o = array.getJSONObject(i)
            ReceivedPush(
                key = o.getString("key"),
                clickToken = o.optString("clickToken").takeIf { it.isNotEmpty() },
                sentAt = o.optLong("sentAt"),
                receivedAt = o.getLong("receivedAt")
            )
        }
    } catch (e: JSONException) {
        Log.e("GameballApp", "could not read received pushes", e)
        emptyList()
    }

    override fun save(pushes: List<ReceivedPush>) {
        val editor = prefs.edit()
        if (pushes.isEmpty()) {
            editor.remove(KEY)
        } else {
            val array = JSONArray()
            pushes.forEach {
                array.put(JSONObject()
                    .put("key", it.key)
                    .put("clickToken", it.clickToken ?: "")
                    .put("sentAt", it.sentAt)
                    .put("receivedAt", it.receivedAt))
            }
            editor.putString(KEY, array.toString())
        }
        // commit, not apply: a push often arrives in a process that's killed right after
        editor.commit()
    }

    private companion object {
        const val FILE = "gameball_received_pushes"
        const val KEY = "pushes"
    }
}
