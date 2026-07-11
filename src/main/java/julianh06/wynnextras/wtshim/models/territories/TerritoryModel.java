// © Wynntils (LGPL-3.0-only) — see LICENSE-WYNNTILS
/*
 * WynnExtras — TerritoryModel (faithful port of Wynntils' advancement + Athena/API flow).
 *
 * Data flow (matches Wynntils):
 *   - Guild-territory ADVANCEMENT (AdvancementUpdateEvent) -> full TerritoryInfo (routes,
 *     storage, generation, defences, treasury, HQ) -> territoryPoiMap.
 *   - Athena/Wynncraft-API poll via the net stack (Managers.Net.download(UrlId)) -> TerritoryProfile
 *     -> territoryProfileMap. Athena is primary; after MAX_ERRORS it falls back to the Wynncraft API.
 *
 * Yarn/shim adaptations (all marked below):
 *   - super() (shim Model is no-arg); Handlers.WrappedScreen + TerritoryManagementHolder dropped.
 *   - The poll is self-started from the constructor on a daemon executor (the shim has no
 *     reloadData() lifecycle wiring). Guild-membership throttle (Models.Guild.isInGuild) dropped
 *     — the fork has no GuildModel — so the poll always runs on the fixed 15s delay.
 *   - getTerritoryProfileForPosition(Position) / getTerritoryConnections(List<TerritoryItem>) /
 *     getTerritoryProfileFromShortName / getTerritoryNames / getTerritoryPois dropped (no caller;
 *     they pull Mojmap Position and the un-ported TerritoryItem gui-item).
 *   - Mojmap advancement API mapped to Yarn: AdvancementHolder->AdvancementEntry,
 *     DisplayInfo->AdvancementDisplay, getType()/AdvancementType.CHALLENGE->getFrame()/AdvancementFrame.CHALLENGE.
 */
package julianh06.wynnextras.wtshim.models.territories;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import julianh06.wynnextras.wtshim.core.WynntilsMod;
import julianh06.wynnextras.wtshim.core.components.Managers;
import julianh06.wynnextras.wtshim.core.components.Model;
import julianh06.wynnextras.wtshim.core.net.Download;
import julianh06.wynnextras.wtshim.core.net.UrlId;
import julianh06.wynnextras.wtshim.core.text.StyledText;
import julianh06.wynnextras.wtshim.mc.event.AdvancementUpdateEvent;
import julianh06.wynnextras.wtshim.models.territories.profile.TerritoryProfile;
import julianh06.wynnextras.wtshim.services.map.pois.TerritoryPoi;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import net.minecraft.advancement.Advancement;
import net.minecraft.advancement.AdvancementDisplay;
import net.minecraft.advancement.AdvancementEntry;
import net.minecraft.advancement.AdvancementFrame;
import net.neoforged.bus.api.SubscribeEvent;

public final class TerritoryModel extends Model {
    private static final int IN_GUILD_TERRITORY_UPDATE_MS = 15000;
    private static final Gson TERRITORY_PROFILE_GSON = new GsonBuilder()
            .registerTypeHierarchyAdapter(TerritoryProfile.class, new TerritoryProfile.TerritoryDeserializer())
            .create();

    // This is territory POIs as returned by the advancement from Wynncraft
    private final Map<String, TerritoryPoi> territoryPoiMap = new ConcurrentHashMap<>();

    // This is the profiles as downloaded from Athena / the Wynncraft API
    private volatile Map<String, TerritoryProfile> territoryProfileMap = new HashMap<>();

    private final ScheduledExecutorService timerExecutor = Executors.newSingleThreadScheduledExecutor(r -> {
        Thread t = new Thread(r, "WynnExtras-TerritoryPoll");
        t.setDaemon(true);
        return t;
    });

    // Use Athena by default for territories, but after 3 failures switch to the API
    private static final int MAX_ERRORS = 3;
    private int athenaCheckErrors = 0;
    private UrlId lookupUrl = UrlId.DATA_ATHENA_TERRITORY_LIST_V2;

    public TerritoryModel() {
        super();
        timerExecutor.scheduleWithFixedDelay(
                this::updateTerritoryProfileMap, 0, IN_GUILD_TERRITORY_UPDATE_MS, TimeUnit.MILLISECONDS);
    }

    public TerritoryProfile getTerritoryProfile(String name) {
        return territoryProfileMap.get(name);
    }

    /* Yarn adaptation of Wynntils' getTerritoryProfileForPosition(Position): returns the profile
     * whose XZ bounds contain the position, or null. (Wynntils' insideArea(Position) is not ported;
     * the bounds are already normalized start<=end in TerritoryProfile.TerritoryDeserializer.) */
    public TerritoryProfile getTerritoryProfileForPosition(net.minecraft.util.math.Vec3d position) {
        if (position == null) return null;
        double x = position.getX();
        double z = position.getZ();
        for (TerritoryProfile profile : territoryProfileMap.values()) {
            if (x >= profile.getStartX() && x <= profile.getEndX()
                    && z >= profile.getStartZ() && z <= profile.getEndZ()) {
                return profile;
            }
        }
        return null;
    }

    public List<TerritoryPoi> getTerritoryPoisFromAdvancement() {
        return new ArrayList<>(territoryPoiMap.values());
    }

    public TerritoryPoi getTerritoryPoiFromAdvancement(String name) {
        return territoryPoiMap.get(name);
    }

    @SubscribeEvent
    public void onAdvancementUpdate(AdvancementUpdateEvent event) {
        Map<String, TerritoryInfo> tempMap = new HashMap<>();

        for (AdvancementEntry added : event.getAdded()) {
            Advancement advancement = added.value();

            Optional<AdvancementDisplay> displayOpt = advancement.display();
            if (displayOpt.isEmpty()) continue;

            AdvancementDisplay displayInfo = displayOpt.get();
            String territoryName = StyledText.fromComponent(displayInfo.getTitle())
                    .replaceAll("\\[", "")
                    .replaceAll("\\]", "")
                    .trim()
                    .getStringWithoutFormatting();

            // Do not parse same thing twice
            if (tempMap.containsKey(territoryName)) continue;

            // ignore empty display texts they are used to generate the "lines"
            if (territoryName.isEmpty()) continue;

            // headquarters frame is challenge
            boolean headquarters = displayInfo.getFrame() == AdvancementFrame.CHALLENGE;

            // description is a raw string with \n, so we have to split
            StyledText description = StyledText.fromComponent(displayInfo.getDescription());
            StyledText[] colored = description.split("\n");
            String[] raw = description.getStringWithoutFormatting().split("\n");

            TerritoryInfo container = new TerritoryInfo(raw, colored, headquarters);
            tempMap.put(territoryName, container);
        }

        for (Map.Entry<String, TerritoryInfo> entry : tempMap.entrySet()) {
            String territoryName = entry.getKey();
            TerritoryProfile territoryProfile = getTerritoryProfile(territoryName);

            if (territoryProfile == null) continue;

            territoryPoiMap.put(
                    territoryName, new TerritoryPoi(() -> getTerritoryProfile(territoryName), entry.getValue()));
        }
    }

    private void updateTerritoryProfileMap() {
        Download dl = Managers.Net.download(lookupUrl);
        dl.handleJsonObject(
                json -> {
                    Map<String, TerritoryProfile> tempMap = new HashMap<>();
                    for (Map.Entry<String, JsonElement> entry : json.entrySet()) {
                        JsonObject territoryObject = entry.getValue().getAsJsonObject();

                        // Inject back the name for the deserializer
                        territoryObject.addProperty("name", entry.getKey());

                        TerritoryProfile territoryProfile =
                                TERRITORY_PROFILE_GSON.fromJson(territoryObject, TerritoryProfile.class);
                        tempMap.put(entry.getKey(), territoryProfile);
                    }

                    territoryProfileMap = tempMap;
                },
                onError -> {
                    WynntilsMod.warn("Failed to update territory data.");

                    if (lookupUrl == UrlId.DATA_ATHENA_TERRITORY_LIST_V2) {
                        athenaCheckErrors++;
                        if (athenaCheckErrors >= MAX_ERRORS) {
                            WynntilsMod.warn(
                                    "Reached maximum errors for Athena territory lookup, switching to Wynncraft API.");
                            lookupUrl = UrlId.DATA_WYNNCRAFT_TERRITORY_LIST;
                        }
                    }
                });
    }
}
