// © Wynntils (LGPL-3.0-only) — see LICENSE-WYNNTILS
/*
 * WynnExtras — UrlId enum.
 *
 * Faithful to Wynntils' UrlId shape (string id + getId()/from()). Slimmed to the ids the fork
 * actually uses; every constant here MUST have a matching entry in the bundled urls.json.
 * Future phases add constants as needed (and only if present in urls.json).
 */
package julianh06.wynnextras.wtshim.core.net;

import java.util.Optional;

public enum UrlId {
    // Static data (downloadable + cacheable json)
    DATA_STATIC_GEAR("dataStaticGear"),
    DATA_STATIC_ITEM_SETS("dataStaticItemSets"),
    DATA_STATIC_ITEM_OBTAIN_V2("dataStaticItemObtainV2"),
    DATA_STATIC_MODEL_DATA("dataStaticModelData"),
    DATA_STATIC_INGREDIENTS("dataStaticIngredients"),
    DATA_STATIC_MATERIAL_CONVERSION("dataStaticMaterialConversion"),
    DATA_STATIC_MAPS("dataStaticMaps"),
    DATA_STATIC_PLACES("dataStaticPlaces"),
    DATA_STATIC_PLACE_MAPFEATURES("dataStaticPlaceMapFeatures"),
    DATA_ATHENA_TERRITORY_LIST_V2("dataAthenaTerritoryListV2"),
    DATA_WYNNCRAFT_TERRITORY_LIST("dataWynncraftTerritoryListV3"),

    // Links (opened in the user's browser)
    LINK_WYNNCRAFT_PLAYER_STATS("linkWynncraftPlayerStats");

    private final String id;

    UrlId(String id) {
        this.id = id;
    }

    public String getId() {
        return id;
    }

    public static Optional<UrlId> from(String str) {
        for (UrlId urlId : values()) {
            if (urlId.getId().equals(str)) {
                return Optional.of(urlId);
            }
        }
        return Optional.empty();
    }
}
