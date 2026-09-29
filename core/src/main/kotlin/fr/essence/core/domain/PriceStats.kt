package fr.essence.core.domain

/** Position d'un prix par rapport aux autres stations du secteur, pour colorer la carte. */
enum class PriceTier { CHEAP, MEDIUM, EXPENSIVE }

/** Statistiques des prix d'un carburant sur les stations approvisionnées affichées. */
data class PriceStats(
    val min: Double,
    val max: Double,
    val average: Double,
    val count: Int,
) {
    /** Tiers du prix entre le minimum et le maximum locaux ; tout est « moyen » si les prix sont identiques. */
    fun tierOf(price: Double): PriceTier {
        val spread = max - min
        if (spread < 0.001) return PriceTier.MEDIUM
        val position = (price - min) / spread
        return when {
            position < 1.0 / 3 -> PriceTier.CHEAP
            position < 2.0 / 3 -> PriceTier.MEDIUM
            else -> PriceTier.EXPENSIVE
        }
    }

    /** Économie (en euros, négative si plus cher) d'un plein de [liters] par rapport au prix moyen. */
    fun savingsVsAverage(price: Double, liters: Double = 40.0): Double = (average - price) * liters

    companion object {
        fun of(prices: Collection<Double>): PriceStats? =
            if (prices.isEmpty()) null
            else PriceStats(prices.min(), prices.max(), prices.average(), prices.size)
    }
}
