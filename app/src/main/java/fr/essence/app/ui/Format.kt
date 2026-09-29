package fr.essence.app.ui

import java.util.Locale

internal fun formatPrice(price: Double): String = String.format(Locale.FRANCE, "%.3f €", price)

internal fun formatDistance(km: Double): String =
    if (km < 1) String.format(Locale.FRANCE, "%d m", (km * 1000).toInt() / 10 * 10)
    else String.format(Locale.FRANCE, "%.1f km", km)

internal fun formatEuros(amount: Double): String = String.format(Locale.FRANCE, "%.2f €", amount)
