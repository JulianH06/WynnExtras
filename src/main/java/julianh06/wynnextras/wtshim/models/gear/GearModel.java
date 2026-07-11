// © Wynntils (LGPL-3.0-only) — see LICENSE-WYNNTILS
/*
 * WynnExtras — GearModel (slim port of Wynntils' GearInfoRegistry).
 *
 * Downloads Wynncraft's gear database (Reference/gear.json) via the managed net stack and exposes an
 * exact display-name -> GearInfo lookup. This is what makes item recognition exact: ItemModel looks a
 * parsed item up here to get its authoritative GearType/GearTier/requirements/stat-ranges instead of
 * guessing from lore text and name colour.
 *
 * Faithful to Wynntils: registers via CoreComponent#registerDownloads(DownloadRegistry) against
 * UrlId.DATA_STATIC_GEAR; gear.json is a JsonObject keyed by the item display name, with the key
 * injected into each entry as "name" before deserialization.
 *
 * SLIM vs Wynntils GearInfoRegistry:
 *  - No Gson TypeAdapter; a hand-written parser reads only the fields WynnExtras reads
 *    (subType, tier, requirements.{level,classRequirement,<skill>,quest}, identifications.{min,max,raw}).
 *  - Wynntils' Dependency on CustomModel/Set/WynnItem downloads is dropped — those feed icon/set/obtain
 *    metadata this shim does not parse, so gear.json can be read standalone.
 *  - StatType keys are built ad-hoc from the identification apiName (skills become SkillStatType so the
 *    skill-point auto-selector recognizes them); there is no StatModel registry to resolve against.
 */
package julianh06.wynnextras.wtshim.models.gear;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import julianh06.wynnextras.wtshim.core.WynntilsMod;
import julianh06.wynnextras.wtshim.core.components.Model;
import julianh06.wynnextras.wtshim.core.net.DownloadRegistry;
import julianh06.wynnextras.wtshim.core.net.UrlId;
import julianh06.wynnextras.wtshim.models.character.type.ClassType;
import julianh06.wynnextras.wtshim.models.elements.type.Skill;
import julianh06.wynnextras.wtshim.models.gear.type.GearInfo;
import julianh06.wynnextras.wtshim.models.gear.type.GearRequirements;
import julianh06.wynnextras.wtshim.models.gear.type.GearTier;
import julianh06.wynnextras.wtshim.models.gear.type.GearType;
import julianh06.wynnextras.wtshim.models.stats.type.SkillStatType;
import julianh06.wynnextras.wtshim.models.stats.type.StatPossibleValues;
import julianh06.wynnextras.wtshim.models.stats.type.StatType;
import julianh06.wynnextras.wtshim.utils.type.Pair;
import julianh06.wynnextras.wtshim.utils.type.RangedValue;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

public class GearModel extends Model {
    // Keyed by the normalized (colour-stripped, trimmed, lower-cased) display name.
    private final Map<String, GearInfo> gearInfoMap = new ConcurrentHashMap<>();
    private volatile boolean loaded = false;

    @Override
    public void registerDownloads(DownloadRegistry registry) {
        registry.registerDownload(UrlId.DATA_STATIC_GEAR).handleJsonObject(this::handleGearDb);
    }

    public boolean isLoaded() {
        return loaded;
    }

    /** Exact lookup by display name. Name may still carry colour codes — it is normalized here. */
    public Optional<GearInfo> getFromDisplayName(String displayName) {
        if (displayName == null || gearInfoMap.isEmpty()) return Optional.empty();
        GearInfo info = gearInfoMap.get(normalize(displayName));
        return Optional.ofNullable(info);
    }

    public int size() {
        return gearInfoMap.size();
    }

    static String normalize(String name) {
        return name.replaceAll("§[0-9a-fk-orA-FK-OR]", "").trim().toLowerCase(Locale.ROOT);
    }

    // ---- parsing ----

    private void handleGearDb(JsonObject json) {
        Map<String, GearInfo> fresh = new ConcurrentHashMap<>();
        for (Map.Entry<String, JsonElement> entry : json.entrySet()) {
            if (!entry.getValue().isJsonObject()) continue;
            try {
                GearInfo info = parseGear(entry.getKey(), entry.getValue().getAsJsonObject());
                if (info != null) fresh.put(normalize(info.name()), info);
            } catch (Throwable t) {
                // A single malformed entry must never abort the whole DB load.
                WynntilsMod.warn("[GearModel] Skipped malformed gear entry '" + entry.getKey() + "': " + t.getMessage());
            }
        }
        gearInfoMap.clear();
        gearInfoMap.putAll(fresh);
        loaded = true;
        WynntilsMod.info("[GearModel] Loaded {} gear entries from CDN.", gearInfoMap.size());
    }

    private static GearInfo parseGear(String name, JsonObject json) {
        GearType type = json.has("subType") ? GearType.fromString(json.get("subType").getAsString()) : GearType.UNKNOWN;
        GearTier tier = json.has("tier") ? GearTier.fromString(json.get("tier").getAsString()) : GearTier.NORMAL;

        GearRequirements requirements = parseRequirements(type, json);
        List<Pair<StatType, StatPossibleValues>> variableStats = parseVariableStats(json);

        return new GearInfo(name, type, tier, requirements, variableStats);
    }

    private static GearRequirements parseRequirements(GearType type, JsonObject json) {
        int level = 0;
        Optional<ClassType> classType = Optional.empty();
        List<Pair<Skill, Integer>> skills = new ArrayList<>();
        Optional<String> quest = Optional.empty();

        if (json.has("requirements") && json.get("requirements").isJsonObject()) {
            JsonObject reqs = json.getAsJsonObject("requirements");

            if (reqs.has("level")) {
                try { level = reqs.get("level").getAsInt(); } catch (Exception ignored) {}
            }

            // Weapons derive their class from the weapon type; everything else reads classRequirement.
            if (type.isWeapon()) {
                classType = Optional.of(type.getClassReq());
            } else if (reqs.has("classRequirement") && !reqs.get("classRequirement").isJsonNull()) {
                ClassType parsed = classFromName(reqs.get("classRequirement").getAsString());
                if (parsed != null) classType = Optional.of(parsed);
            }

            for (Skill skill : Skill.values()) {
                String key = skillApiName(skill);
                if (reqs.has(key) && !reqs.get(key).isJsonNull()) {
                    try {
                        int val = reqs.get(key).getAsInt();
                        if (val != 0) skills.add(new Pair<>(skill, val));
                    } catch (Exception ignored) {}
                }
            }

            if (reqs.has("quest") && !reqs.get("quest").isJsonNull()) {
                quest = Optional.of(reqs.get("quest").getAsString());
            }
        }

        return new GearRequirements(level, classType, skills, quest);
    }

    private static List<Pair<StatType, StatPossibleValues>> parseVariableStats(JsonObject json) {
        List<Pair<StatType, StatPossibleValues>> result = new ArrayList<>();
        if (!json.has("identifications") || !json.get("identifications").isJsonObject()) return result;

        JsonObject ids = json.getAsJsonObject("identifications");
        for (Map.Entry<String, JsonElement> entry : ids.entrySet()) {
            String apiName = entry.getKey();
            if ("elementalDefense".equals(apiName)) apiName = "elementalDefence";

            JsonElement value = entry.getValue();
            int baseValue;
            RangedValue range;
            boolean preIdentified;

            if (value.isJsonObject()) {
                JsonObject stat = value.getAsJsonObject();
                if (!stat.has("raw")) continue;
                baseValue = stat.get("raw").getAsInt();
                int min = stat.has("min") ? stat.get("min").getAsInt() : baseValue;
                int max = stat.has("max") ? stat.get("max").getAsInt() : baseValue;
                range = RangedValue.of(min, max);
                preIdentified = false;
            } else if (value.isJsonPrimitive()) {
                try { baseValue = value.getAsInt(); } catch (Exception e) { continue; }
                range = RangedValue.of(baseValue, baseValue);
                preIdentified = true;
            } else {
                continue;
            }

            if (baseValue == 0) continue; // Wynntils skips zero-base stats.

            StatType statType = statTypeFor(apiName);
            result.add(new Pair<>(statType, new StatPossibleValues(statType, range, baseValue, preIdentified)));
        }
        return result;
    }

    /** Skill identifications become SkillStatType so callers (skill-point selector) can recognize them. */
    private static StatType statTypeFor(String apiName) {
        Skill skill = skillFromRawApiName(apiName);
        if (skill != null) {
            return new SkillStatType(skill, apiName, prettify(apiName));
        }
        return new StatType(apiName, prettify(apiName));
    }

    private static Skill skillFromRawApiName(String apiName) {
        return switch (apiName) {
            case "rawStrength" -> Skill.STRENGTH;
            case "rawDexterity" -> Skill.DEXTERITY;
            case "rawIntelligence" -> Skill.INTELLIGENCE;
            case "rawDefence" -> Skill.DEFENCE;
            case "rawAgility" -> Skill.AGILITY;
            default -> null;
        };
    }

    private static String skillApiName(Skill skill) {
        return switch (skill) {
            case STRENGTH -> "strength";
            case DEXTERITY -> "dexterity";
            case INTELLIGENCE -> "intelligence";
            case DEFENCE -> "defence";
            case AGILITY -> "agility";
        };
    }

    private static ClassType classFromName(String name) {
        if (name == null) return null;
        String upper = name.trim().toUpperCase(Locale.ROOT);
        for (ClassType t : ClassType.values()) {
            if (t == ClassType.NONE) continue;
            if (t.name().equals(upper)) return t;
            if (t.getActualName(false).equalsIgnoreCase(name)) return t;
            if (t.getActualName(true).equalsIgnoreCase(name)) return t;
        }
        return null;
    }

    /** camelCase apiName -> "Camel Case" display label. */
    private static String prettify(String apiName) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < apiName.length(); i++) {
            char c = apiName.charAt(i);
            if (i == 0) {
                sb.append(Character.toUpperCase(c));
            } else if (Character.isUpperCase(c)) {
                sb.append(' ').append(c);
            } else {
                sb.append(c);
            }
        }
        return sb.toString();
    }
}
