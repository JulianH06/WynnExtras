// © Wynntils (LGPL-3.0-only) — see LICENSE-WYNNTILS
/*
 * WynnExtras — TerritoryProfile (faithful port). Deserialized by TerritoryModel via the
 * bundled TerritoryDeserializer from the Athena/Wynncraft territory list (net stack).
 * DEVIATION: insideArea(Position) dropped (Mojmap net.minecraft.core.Position; no caller
 * in the fork). getCenterLocation() kept (returns PoiLocation).
 */
package julianh06.wynnextras.wtshim.models.territories.profile;

import com.google.gson.JsonArray;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import julianh06.wynnextras.wtshim.utils.DateFormatter;
import julianh06.wynnextras.wtshim.utils.colors.CustomColor;
import julianh06.wynnextras.wtshim.utils.mc.type.PoiLocation;
import java.lang.reflect.Type;
import java.time.Instant;
import java.util.Objects;
import java.util.Optional;
import net.minecraft.util.Formatting;

public class TerritoryProfile {
    private static final DateFormatter DATE_FORMATTER = new DateFormatter(false);

    private final String name;
    private final String friendlyName;

    private final TerritoryLocation territoryLocation;

    private final GuildInfo guildInfo;

    private final Instant acquired;

    public TerritoryProfile(
            String name,
            String friendlyName,
            TerritoryLocation territoryLocation,
            GuildInfo guildInfo,
            Instant acquired) {
        this.name = name;
        this.friendlyName = friendlyName;
        this.territoryLocation = territoryLocation;
        this.guildInfo = guildInfo;
        this.acquired = acquired;
    }

    public GuildInfo getGuildInfo() {
        return guildInfo;
    }

    public String getName() {
        return name;
    }

    public String getFriendlyName() {
        return friendlyName;
    }

    public int getStartX() {
        return territoryLocation.startX();
    }

    public int getStartZ() {
        return territoryLocation.startZ();
    }

    public int getEndX() {
        return territoryLocation.endX();
    }

    public int getEndZ() {
        return territoryLocation.endZ();
    }

    public String getGuild() {
        return guildInfo.name();
    }

    public String getGuildPrefix() {
        return guildInfo.prefix();
    }

    public Instant getAcquired() {
        return acquired;
    }

    public PoiLocation getCenterLocation() {
        int xMiddle = (this.getStartX() + this.getEndX()) / 2;
        int zMiddle = (this.getStartZ() + this.getEndZ()) / 2;
        return new PoiLocation(xMiddle, 0, zMiddle);
    }

    private long getTimeHeldInMillis() {
        if (acquired != null) {
            return System.currentTimeMillis() - acquired.toEpochMilli();
        } else {
            return 0;
        }
    }

    public boolean isOnCooldown() {
        return getTimeHeldInMillis() < 10 * 60 * 1000;
    }

    public String getReadableRelativeTimeAcquired() {
        long difference = getTimeHeldInMillis();
        return DATE_FORMATTER.format(difference);
    }

    public Formatting getTimeAcquiredColor() {
        // 0 - 1 hours > Green
        // 1 hour - 1 day > Yellow
        // 1 day - > Red

        long difference = getTimeHeldInMillis();
        if (difference < 60 * 60 * 1000) {
            return Formatting.GREEN;
        } else if (difference < 24 * 60 * 60 * 1000) {
            return Formatting.YELLOW;
        } else {
            return Formatting.RED;
        }
    }

    public static class TerritoryDeserializer implements JsonDeserializer<TerritoryProfile> {
        @Override
        public TerritoryProfile deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context)
                throws JsonParseException {
            JsonObject territory = json.getAsJsonObject();

            int startX = Integer.MAX_VALUE - 1;
            int startZ = Integer.MAX_VALUE - 1;
            int endX = Integer.MAX_VALUE;
            int endZ = Integer.MAX_VALUE;
            if (territory.has("location")) {
                JsonObject location = territory.getAsJsonObject("location");

                JsonArray start = location.getAsJsonArray("start");
                startX = start.get(0).getAsInt();
                startZ = start.get(1).getAsInt();

                JsonArray end = location.getAsJsonArray("end");
                endX = end.get(0).getAsInt();
                endZ = end.get(1).getAsInt();

                // Sometimes the start and end coordinates are swapped, so we need to check for that
                if (startX > endX) {
                    int temp = startX;
                    startX = endX;
                    endX = temp;
                }

                if (startZ > endZ) {
                    int temp = startZ;
                    startZ = endZ;
                    endZ = temp;
                }
            }

            TerritoryLocation territoryLocation = new TerritoryLocation(startX, startZ, endX, endZ);

            String territoryName = territory.get("name").getAsString();
            String friendlyName = territoryName.replace('’', '\'');

            GuildInfo guild;
            JsonElement guildJson = territory.get("guild");
            if (guildJson.isJsonNull()
                    || !guildJson.isJsonObject()
                    || guildJson.getAsJsonObject().get("name").isJsonNull()
                    || guildJson.getAsJsonObject().get("prefix").isJsonNull()) {
                guild = GuildInfo.NONE;
            } else {
                JsonObject guildObject = guildJson.getAsJsonObject();
                String guildName = guildObject.get("name").getAsString();
                String guildPrefix = guildObject.get("prefix").getAsString();
                Optional<CustomColor> guildColor = guildObject.has("color")
                        ? Optional.of(CustomColor.fromHexString(
                                guildObject.get("color").getAsString()))
                        : Optional.empty();

                guild = new GuildInfo(guildName, guildPrefix, guildColor);
            }

            Instant acquired;
            JsonElement acquiredJson = territory.get("acquired");
            if (acquiredJson.isJsonNull()) {
                acquired = null;
            } else {
                acquired = Instant.parse(acquiredJson.getAsString());
            }

            return new TerritoryProfile(territoryName, friendlyName, territoryLocation, guild, acquired);
        }
    }

    public record GuildInfo(String name, String prefix, Optional<CustomColor> color) {
        public static final GuildInfo NONE = new GuildInfo("No owner", "None", Optional.empty());
    }

    public record TerritoryLocation(int startX, int startZ, int endX, int endZ) {}

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        TerritoryProfile that = (TerritoryProfile) o;
        return Objects.equals(name, that.name)
                && Objects.equals(friendlyName, that.friendlyName)
                && Objects.equals(territoryLocation, that.territoryLocation)
                && Objects.equals(guildInfo, that.guildInfo)
                && Objects.equals(acquired, that.acquired);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, friendlyName, territoryLocation, guildInfo, acquired);
    }
}
