package fr.essence.core.data

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

/**
 * Enregistrement du jeu `prix-des-carburants-en-france-flux-instantane-v2`
 * (API Explore v2.1 d'Opendatasoft sur data.economie.gouv.fr).
 * Tous les champs sont optionnels : le flux omet ou met à null ce qui n'est pas publié.
 */
@Serializable
data class StationDto(
    val id: Long,
    val adresse: String? = null,
    val cp: String? = null,
    val ville: String? = null,
    val geom: GeomDto? = null,
    @SerialName("horaires_automate_24_24") val automate24h: String? = null,

    @SerialName("gazole_prix") val gazolePrix: Double? = null,
    @SerialName("gazole_maj") val gazoleMaj: String? = null,
    @SerialName("sp95_prix") val sp95Prix: Double? = null,
    @SerialName("sp95_maj") val sp95Maj: String? = null,
    @SerialName("e10_prix") val e10Prix: Double? = null,
    @SerialName("e10_maj") val e10Maj: String? = null,
    @SerialName("sp98_prix") val sp98Prix: Double? = null,
    @SerialName("sp98_maj") val sp98Maj: String? = null,
    @SerialName("e85_prix") val e85Prix: Double? = null,
    @SerialName("e85_maj") val e85Maj: String? = null,
    @SerialName("gplc_prix") val gplcPrix: Double? = null,
    @SerialName("gplc_maj") val gplcMaj: String? = null,

    // Selon l'export, ces champs arrivent en tableau JSON ou en chaîne séparée par ";".
    @SerialName("carburants_indisponibles") val carburantsIndisponibles: JsonElement? = null,
    @SerialName("carburants_rupture_temporaire") val ruptureTemporaire: JsonElement? = null,
    @SerialName("carburants_rupture_definitive") val ruptureDefinitive: JsonElement? = null,
)

@Serializable
data class GeomDto(val lon: Double, val lat: Double)

@Serializable
data class RecordsResponse(
    @SerialName("total_count") val totalCount: Int = 0,
    val results: List<StationDto> = emptyList(),
)
