package br.app.ridecheck

data class Ride(
    val price: Double,
    val pickupMin: Int,
    val pickupKm: Double,
    val tripMin: Int,
    val tripKm: Double,
    val rating: Double?,
    val trips: Int?
)

object RideParser {
    private val priceExact = Regex("""^R\$\s*(\d{1,3}(?:\.\d{3})*,\d{2})$""")
    private val priceAny = Regex("""R\$\s*(\d{1,3}(?:\.\d{3})*,\d{2})(?!\s*/\s*km)""")
    private val leg = Regex("""(\d+)\s*min\s*\(\s*(\d+(?:[.,]\d+)?)\s*(km|m)\s*\)""", RegexOption.IGNORE_CASE)
    private val rating = Regex("""(\d[.,]\d{1,2})[\s|·•]*(\d+)\s*corridas""", RegexOption.IGNORE_CASE)

    private fun money(s: String) = s.replace(".", "").replace(",", ".").toDouble()

    /** texts = todos os textos dos nós da tela, na ordem. A 1ª "perna" é a busca, a 2ª é a corrida. */
    fun parse(texts: List<String>): Ride? {
        val joined = texts.joinToString(" | ")
        val legs = leg.findAll(joined).toList()
        if (legs.size < 2) return null

        val price = texts.firstNotNullOfOrNull { t -> priceExact.find(t.trim())?.let { money(it.groupValues[1]) } }
            ?: priceAny.find(joined)?.let { money(it.groupValues[1]) }
            ?: return null

        fun km(m: MatchResult): Double {
            val v = m.groupValues[2].replace(",", ".").toDouble()
            return if (m.groupValues[3].equals("m", ignoreCase = true)) v / 1000.0 else v
        }

        val r = rating.find(joined)
        return Ride(
            price = price,
            pickupMin = legs[0].groupValues[1].toInt(),
            pickupKm = km(legs[0]),
            tripMin = legs[1].groupValues[1].toInt(),
            tripKm = km(legs[1]),
            rating = r?.groupValues?.get(1)?.replace(",", ".")?.toDoubleOrNull(),
            trips = r?.groupValues?.get(2)?.toIntOrNull()
        )
    }
}

enum class Verdict { GREEN, YELLOW, RED }

data class Result(
    val verdict: Verdict,
    val perKm: Double,
    val perHour: Double,
    val net: Double,
    val netPerHour: Double,
    val totalKm: Double,
    val totalMin: Int,
    val fails: List<String>
)

object Evaluator {
    fun eval(r: Ride, p: Prefs): Result {
        val totalKm = (r.pickupKm + r.tripKm).coerceAtLeast(0.1)
        val totalMin = (r.pickupMin + r.tripMin).coerceAtLeast(1)
        val perKm = r.price / totalKm
        val perHour = r.price / totalMin * 60.0
        val costPerKm = p.fuelPrice / p.kmPerLiter + p.wearKm
        val net = r.price - totalKm * costPerKm
        val netPerHour = net / totalMin * 60.0

        val fails = mutableListOf<String>()
        if (perKm < p.minKm) fails += "R\$/km baixo"
        if (perHour < p.minHour) fails += "R\$/h baixo"
        if (r.pickupKm > p.maxPickup) fails += "busca longa"

        val verdict = when (fails.size) {
            0 -> Verdict.GREEN
            1 -> Verdict.YELLOW
            else -> Verdict.RED
        }
        return Result(verdict, perKm, perHour, net, netPerHour, totalKm, totalMin, fails)
    }
}
