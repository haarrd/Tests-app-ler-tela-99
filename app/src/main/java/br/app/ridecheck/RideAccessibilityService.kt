package br.app.ridecheck

import android.accessibilityservice.AccessibilityService
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.view.Gravity
import android.view.WindowManager
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import android.widget.LinearLayout
import android.widget.TextView
import java.util.Locale

/** Só LÊ a tela do app 99 e mostra um selo. Não clica, não aceita corrida. */
class RideAccessibilityService : AccessibilityService() {

    private lateinit var prefs: Prefs
    private lateinit var wm: WindowManager
    private val handler = Handler(Looper.getMainLooper())
    private var overlay: LinearLayout? = null
    private var lastRun = 0L
    private var lastMatchAt = 0L
    private var lastDumpAt = 0L
    private var lastSig = ""

    private val safetyHide = Runnable { removeOverlay() }

    override fun onServiceConnected() {
        prefs = Prefs(this)
        wm = getSystemService(WINDOW_SERVICE) as WindowManager
    }

    override fun onInterrupt() {}

    override fun onDestroy() {
        removeOverlay()
        super.onDestroy()
    }

    override fun onAccessibilityEvent(e: AccessibilityEvent?) {
        val pkg = e?.packageName?.toString() ?: return
        if (prefs.debug && prefs.lastPkg != pkg) prefs.lastPkg = pkg
        if (!pkg.contains("99")) return

        val now = SystemClock.uptimeMillis()
        if (now - lastRun < 250) return
        lastRun = now

        val texts = ArrayList<String>()
        val dump = if (prefs.debug) StringBuilder() else null
        for (root in roots99()) collect(root, texts, 0, dump)

        if (dump != null && now - lastDumpAt > 2000 && texts.any { it.contains("R$") }) {
            lastDumpAt = now
            prefs.lastDump = dump.toString().take(60000)
        }

        val ride = RideParser.parse(texts)
        if (ride != null) {
            lastMatchAt = now
            val sig = "${ride.price}|${ride.pickupKm}|${ride.tripKm}"
            if (sig != lastSig || overlay == null) {
                lastSig = sig
                showOverlay(Evaluator.eval(ride, prefs), ride)
            }
            handler.removeCallbacks(safetyHide)
            handler.postDelayed(safetyHide, 15000)
        } else if (overlay != null && now - lastMatchAt > 700) {
            removeOverlay()
        }
    }

    private fun roots99(): List<AccessibilityNodeInfo> {
        val list = ArrayList<AccessibilityNodeInfo>()
        try {
            for (w in windows) w.root?.let { list += it }
        } catch (_: Exception) {
        }
        if (list.isEmpty()) rootInActiveWindow?.let { list += it }
        return list.filter { it.packageName?.toString()?.contains("99") == true }
    }

    private fun collect(n: AccessibilityNodeInfo?, out: MutableList<String>, depth: Int, dump: StringBuilder?) {
        if (n == null || depth > 40) return
        val t = (n.text ?: n.contentDescription)?.toString()?.trim()
        if (!t.isNullOrEmpty()) out += t
        if (dump != null) {
            dump.append("  ".repeat(depth))
                .append(n.className)
                .append(" | text=").append(n.text)
                .append(" | desc=").append(n.contentDescription)
                .append(" | id=").append(n.viewIdResourceName)
                .append('\n')
        }
        for (i in 0 until n.childCount) collect(n.getChild(i), out, depth + 1, dump)
    }

    private fun dp(v: Int) = (v * resources.displayMetrics.density).toInt()

    private fun fmt(f: String, vararg a: Any) = String.format(Locale("pt", "BR"), f, *a)

    private fun showOverlay(res: Result, r: Ride) {
        removeOverlay()
        val (color, title) = when (res.verdict) {
            Verdict.GREEN -> "#E6188F4A" to "✔ BOA"
            Verdict.YELLOW -> "#E6C98A00" to "⚠ ATENÇÃO"
            Verdict.RED -> "#E6C0262D" to "✖ RUIM"
        }

        fun line(text: String, size: Float, bold: Boolean = false) = TextView(this).apply {
            this.text = text
            textSize = size
            setTextColor(Color.WHITE)
            if (bold) setTypeface(null, Typeface.BOLD)
        }

        val box = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(16), dp(10), dp(16), dp(10))
            background = GradientDrawable().apply {
                setColor(Color.parseColor(color))
                cornerRadius = dp(14).toFloat()
            }
            addView(line(title, 20f, true))
            addView(line(fmt("R\$ %.2f/km  ·  R\$ %.0f/h", res.perKm, res.perHour), 16f, true))
            addView(line(fmt("Líquido R\$ %.2f  (R\$ %.0f/h)", res.net, res.netPerHour), 14f))
            addView(line(fmt("Busca %.1f km · total %.1f km / %d min", r.pickupKm, res.totalKm, res.totalMin), 13f))
            if (res.fails.isNotEmpty()) addView(line(res.fails.joinToString(" · "), 13f, true))
        }

        val lp = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE or
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT
        )
        lp.gravity = Gravity.TOP or Gravity.CENTER_HORIZONTAL
        lp.y = dp(90)

        try {
            wm.addView(box, lp)
            overlay = box
        } catch (_: Exception) {
        }
    }

    private fun removeOverlay() {
        handler.removeCallbacks(safetyHide)
        overlay?.let { try { wm.removeView(it) } catch (_: Exception) {} }
        overlay = null
    }
}
