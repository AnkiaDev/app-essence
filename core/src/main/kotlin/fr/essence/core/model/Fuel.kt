package fr.essence.core.model

/**
 * Carburants publiés par le flux officiel prix-carburants.
 * [apiKey] est le préfixe des champs du jeu de données (`gazole_prix`, `sp98_maj`...),
 * [apiName] le libellé utilisé dans les listes `carburants_disponibles` / ruptures.
 */
enum class Fuel(val apiKey: String, val apiName: String, val label: String) {
    GAZOLE("gazole", "Gazole", "Gazole"),
    SP95("sp95", "SP95", "SP95"),
    E10("e10", "E10", "SP95-E10"),
    SP98("sp98", "SP98", "SP98"),
    E85("e85", "E85", "E85"),
    GPLC("gplc", "GPLc", "GPL");

    companion object {
        fun fromApiName(name: String): Fuel? =
            entries.firstOrNull { it.apiName.equals(name.trim(), ignoreCase = true) }
    }
}
