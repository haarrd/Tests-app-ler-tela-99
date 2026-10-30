package br.app.ridecheck

import android.app.Activity
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.text.InputType
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.Switch
import android.widget.TextView
import android.widget.Toast

class MainActivity : Activity() {

    private lateinit var prefs: Prefs
    private lateinit var info: TextView
    private val inputs = LinkedHashMap<String, EditText>()

    private fun dp(v: Int) = (v * resources.displayMetrics.density).toInt()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        prefs = Prefs(this)

        val col = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(16), dp(32), dp(16), dp(24))
        }

        fun field(label: String, key: String, value: Float) {
            col.addView(TextView(this).apply { text = label; setPadding(0, dp(10), 0, 0) })
            val e = EditText(this).apply {
                inputType = InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_DECIMAL
                setText(value.toString())
            }
            inputs[key] = e
            col.addView(e)
        }

        col.addView(TextView(this).apply { text = "RideCheck (99)"; textSize = 24f })
        field("Mínimo R$/km", "minKm", prefs.minKm)
        field("Mínimo R$/hora", "minHour", prefs.minHour)
        field("Busca máxima (km)", "maxPickup", prefs.maxPickup)
        field("Preço do combustível (R$/L)", "fuelPrice", prefs.fuelPrice)
        field("Consumo do carro (km/L)", "kmPerLiter", prefs.kmPerLiter)
        field("Desgaste por km (R$)", "wearKm", prefs.wearKm)

        col.addView(Button(this).apply {
            text = "Salvar critérios"
            setOnClickListener {
                fun f(k: String, d: Float) = inputs[k]!!.text.toString().replace(",", ".").toFloatOrNull() ?: d
                prefs.minKm = f("minKm", prefs.minKm)
                prefs.minHour = f("minHour", prefs.minHour)
                prefs.maxPickup = f("maxPickup", prefs.maxPickup)
                prefs.fuelPrice = f("fuelPrice", prefs.fuelPrice)
                prefs.kmPerLiter = f("kmPerLiter", prefs.kmPerLiter)
                prefs.wearKm = f("wearKm", prefs.wearKm)
                Toast.makeText(this@MainActivity, "Salvo", Toast.LENGTH_SHORT).show()
            }
        })

        col.addView(Button(this).apply {
            text = "Ativar o serviço (Acessibilidade)"
            setOnClickListener { startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)) }
        })

        col.addView(Switch(this).apply {
            text = "Modo debug (grava a estrutura da tela da 99)"
            isChecked = prefs.debug
            setPadding(0, dp(16), 0, dp(8))
            setOnCheckedChangeListener { _, c -> prefs.debug = c }
        })

        info = TextView(this)
        col.addView(info)

        col.addView(Button(this).apply {
            text = "Copiar último dump"
            setOnClickListener {
                val cm = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                cm.setPrimaryClip(ClipData.newPlainText("dump", prefs.lastDump))
                Toast.makeText(this@MainActivity, "Copiado", Toast.LENGTH_SHORT).show()
            }
        })

        setContentView(ScrollView(this).apply { addView(col) })
    }

    override fun onResume() {
        super.onResume()
        info.text = "Último pacote visto: ${prefs.lastPkg.ifEmpty { "-" }}\nDump salvo: ${prefs.lastDump.length} caracteres"
    }
}
