# Essence — trouver la station approvisionnée la moins chère

Application Android (Kotlin, Jetpack Compose) qui affiche les stations-service françaises
autour de vous avec prix et ruptures, choisit la meilleure et lance le guidage dans Waze ou Google Maps.

## Données

Flux officiel **« Prix des carburants en France – Flux instantané v2 »** (ministère de l'Économie).

| | |
|---|---|
| Endpoint | `GET https://data.economie.gouv.fr/api/explore/v2.1/catalog/datasets/prix-des-carburants-en-france-flux-instantane-v2/records` |
| Mise à jour | toutes les 10 minutes (source : `donnees.roulez-eco.fr/opendata/instantane_ruptures`) |
| Licence | Licence Ouverte / Etalab 2.0 (mention de la source obligatoire dans l'appli) |
| Clé d'API | aucune |
| Pagination | 100 résultats max par page (`limit` / `offset`) |

Requête utilisée (ODSQL) :

```
where = within_distance(geom, geom'POINT(<lon> <lat>)', 10km) and gazole_prix is not null
```

Champs lus : `id`, `adresse`, `cp`, `ville`, `geom {lon, lat}`, `horaires_automate_24_24`,
`<carburant>_prix` et `<carburant>_maj` pour gazole, sp95, e10, sp98, e85, gplc,
et pour les ruptures `carburants_indisponibles`, `carburants_rupture_temporaire`, `carburants_rupture_definitive`.
Le flux ne contient **pas** l'enseigne (Total, Leclerc…) ; il faudra la croiser plus tard avec une autre source si on la veut.

> Les noms de champs ont été vérifiés via la documentation du jeu et des projets qui l'utilisent,
> mais pas sur une réponse réelle (l'API était injoignable depuis l'environnement de génération).
> Le parsing est tolérant (champs optionnels, tableaux ou chaînes `;`), à confirmer au premier lancement.

## Dans l'appli

- Carte avec une bulle de prix par station, du vert (moins cher du secteur) au rouge (plus cher) ;
  ★ sur la meilleure option, bulles grises « Rupture » pour les stations à sec. Tuiles inversées en mode sombre.
- Panneau glissant : fiche de la station choisie (meilleure par défaut, ou celle touchée sur la carte ou dans la liste)
  avec prix, distance, économie estimée, fraîcheur du prix et boutons Waze / Google Maps, puis la liste classée.
- Choix du carburant et du rayon (5, 10, 20 km), mémorisés entre deux lancements.
- Boutons « me recentrer » et « actualiser » ; rechargement automatique au retour dans l'appli si les prix ont plus de 10 min.
- Prix de plus de 3 jours signalés « à vérifier sur place ».
- Mention des sources (prix-carburants.gouv.fr, OpenStreetMap) en bas de la liste.

## Architecture

```
app-essence/
├── core/   Module Kotlin/JVM pur, testable sans Android
│   ├── model/        Fuel, Station, FuelPrice, GeoPoint
│   ├── data/         StationApi (Ktor + kotlinx.serialization), DTO, mapping
│   ├── domain/       BestStationFinder, PriceStats (tiers de prix), Freshness, distance haversine
│   └── navigation/   Liens Waze / Google Maps
└── app/    Module Android
    ├── MainActivity, Navigator (intents GPS)
    ├── data/         UserPreferences (carburant et rayon mémorisés)
    ├── location/     FusedLocationProvider (Play Services)
    └── ui/           StationsViewModel, StationsScreen (Compose), StationsMap (osmdroid), theme/
```

Stack : Kotlin 2.4, Jetpack Compose + Material 3, Ktor client (moteur Android, sans OkHttp), kotlinx.serialization,
coroutines/StateFlow, osmdroid (carte OpenStreetMap, sans clé ni compte Google), Play Services Location.
minSdk 26, targetSdk 36.

### « La moins chère la plus proche »

`BestStationFinder` ramène prix et distance au coût réel du plein :

```
coût = prix × litres du plein + prix × conso/100 × distance aller-retour
```

(par défaut : plein de 40 L, 6,5 L/100 km, distance à vol d'oiseau × 1,3). Les stations
en rupture pour le carburant choisi sont exclues. Une station 1 centime moins chère à 20 km perd
face à la station du coin ; 20 centimes de moins à 5 km justifient le détour.

### Envoi vers le GPS

- Waze : `https://waze.com/ul?ll=<lat>,<lon>&navigate=yes` (ouvre Waze, ou le web s'il n'est pas installé).
- Google Maps : `google.navigation:q=<lat>,<lon>&mode=d`, repli sur `https://www.google.com/maps/dir/?api=1&destination=…`.

## Lancer

1. Ouvrir le dossier `app-essence` dans Android Studio (il crée `local.properties` avec le SDK).
2. Lancer la configuration `app` sur un téléphone ou un émulateur avec Google Play.
3. Tests de la logique métier : `./gradlew :core:test`.

Sans SDK Android, `settings.gradle.kts` n'inclut que `:core`.

## Pistes suivantes

- Réglages avancés (taille du plein, consommation) pour affiner le calcul de la meilleure station.
- Cache hors-ligne des derniers prix.
- Enseigne des stations (source à croiser).
