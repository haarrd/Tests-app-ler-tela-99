package br.app.ridecheck

import android.content.Context

class Prefs(ctx: Context) {
    private val sp = ctx.getSharedPreferences("ridecheck", Context.MODE_PRIVATE)

    var minKm: Float
        get() = sp.getFloat("minKm", 1.80f)
        set(v) { sp.edit().putFloat("minKm", v).apply() }
    var minHour: Float
        get() = sp.getFloat("minHour", 45f)
        set(v) { sp.edit().putFloat("minHour", v).apply() }
    var maxPickup: Float
        get() = sp.getFloat("maxPickup", 3f)
        set(v) { sp.edit().putFloat("maxPickup", v).apply() }
    var fuelPrice: Float
        get() = sp.getFloat("fuelPrice", 6.20f)
        set(v) { sp.edit().putFloat("fuelPrice", v).apply() }
    var kmPerLiter: Float
        get() = sp.getFloat("kmPerLiter", 11f)
        set(v) { sp.edit().putFloat("kmPerLiter", v).apply() }
    var wearKm: Float
        get() = sp.getFloat("wearKm", 0.25f)
        set(v) { sp.edit().putFloat("wearKm", v).apply() }

    var debug: Boolean
        get() = sp.getBoolean("debug", false)
        set(v) { sp.edit().putBoolean("debug", v).apply() }
    var lastPkg: String
        get() = sp.getString("lastPkg", "") ?: ""
        set(v) { sp.edit().putString("lastPkg", v).apply() }
    var lastDump: String
        get() = sp.getString("lastDump", "") ?: ""
        set(v) { sp.edit().putString("lastDump", v).apply() }
}
