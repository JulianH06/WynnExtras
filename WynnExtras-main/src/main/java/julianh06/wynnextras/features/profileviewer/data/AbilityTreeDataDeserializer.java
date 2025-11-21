package julianh06.wynnextras.features.profileviewer.data;

import com.google.gson.*;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AbilityTreeDataDeserializer implements JsonDeserializer<AbilityTreeData> {
    @Override
    public AbilityTreeData deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext ctx) throws JsonParseException {
        AbilityTreeData tree = new AbilityTreeData();
        JsonObject root = json.getAsJsonObject();

        // --- Archetypes ---
        tree.archetypes = new HashMap<>();
        if (root.has("archetypes")) {
            JsonObject archetypesObj = root.getAsJsonObject("archetypes");
            for (Map.Entry<String, JsonElement> entry : archetypesObj.entrySet()) {
                String key = entry.getKey();
                JsonObject val = entry.getValue().getAsJsonObject();

                AbilityTreeData.Archetype archetype = new AbilityTreeData.Archetype();
                archetype.name = val.get("name").getAsString();
                archetype.description = val.get("description").getAsString();
                archetype.shortDescription = val.get("shortDescription").getAsString();
                archetype.slot = val.get("slot").getAsInt();
                archetype.icon = parseIcon(val.get("icon"));

                tree.archetypes.put(key, archetype);
            }
        }

        // --- Pages ---
        tree.pages = new HashMap<>();
        if (root.has("pages")) {
            JsonObject pagesObj = root.getAsJsonObject("pages");
            for (Map.Entry<String, JsonElement> pageEntry : pagesObj.entrySet()) {
                int pageNumber = Integer.parseInt(pageEntry.getKey());
                JsonObject abilitiesObj = pageEntry.getValue().getAsJsonObject();

                Map<String, AbilityTreeData.Ability> abilityMap = new HashMap<>();
                for (Map.Entry<String, JsonElement> abilityEntry : abilitiesObj.entrySet()) {
                    String abilityId = abilityEntry.getKey();
                    JsonObject abilityData = abilityEntry.getValue().getAsJsonObject();

                    AbilityTreeData.Ability ability = new AbilityTreeData.Ability();
                    ability.page = pageNumber;
                    ability.name = abilityData.get("name").getAsString();
                    ability.slot = abilityData.has("slot") ? abilityData.get("slot").getAsInt() : 0;

                    // Coordinates
                    JsonObject coord = abilityData.getAsJsonObject("coordinates");
                    ability.coordinates = new AbilityTreeData.Coordinates();
                    ability.coordinates.x = coord.get("x").getAsInt();
                    ability.coordinates.y = coord.get("y").getAsInt();

                    // Description
                    JsonElement desc = abilityData.get("description");
                    if (desc != null && desc.isJsonArray()) {
                        ability.description = new ArrayList<>();
                        for (JsonElement line : desc.getAsJsonArray()) {
                            ability.description.add(line.getAsString());
                        }
                    }


                    // Requirements
                    if (abilityData.has("requirements")) {
                        JsonObject req = abilityData.getAsJsonObject("requirements");
                        AbilityTreeData.Requirements r = new AbilityTreeData.Requirements();
                        if (req.has("ABILITY_POINTS")) r.ABILITY_POINTS = req.get("ABILITY_POINTS").getAsInt();
                        if (req.has("NODE")) r.NODE = req.get("NODE").getAsString();
                        if (req.has("ARCHETYPE") && req.get("ARCHETYPE").isJsonObject()) {
                            JsonObject ar = req.getAsJsonObject("ARCHETYPE");
                            AbilityTreeData.ArchetypeRequirement arq = new AbilityTreeData.ArchetypeRequirement();
                            arq.name = ar.get("name").getAsString();
                            arq.amount = ar.get("amount").getAsInt();
                            r.ARCHETYPE = arq;
                        }
                        ability.requirements = r;
                    }

                    // Links
                    JsonElement links = abilityData.get("links");
                    if (links != null && links.isJsonArray()) {
                        ability.links = new ArrayList<>();
                        for (JsonElement link : links.getAsJsonArray()) {
                            ability.links.add(link.getAsString());
                        }
                    }


                    // Locks
                    JsonElement locks = abilityData.get("locks");
                    if (locks != null && locks.isJsonArray()) {
                        ability.locks = new ArrayList<>();
                        for (JsonElement lock : locks.getAsJsonArray()) {
                            ability.locks.add(lock.getAsString());
                        }
                    }


                    // Icon
                    ability.icon = parseIcon(abilityData.get("icon"));

                    abilityMap.put(abilityId, ability);
                }

                tree.pages.put(pageNumber, abilityMap);
            }
        }


        return tree;
    }

    private AbilityTreeData.Icon parseIcon(JsonElement iconElement) {
        if (!iconElement.isJsonObject()) return null;
        JsonObject iconObj = iconElement.getAsJsonObject();

        AbilityTreeData.Icon icon = new AbilityTreeData.Icon();
        icon.format = iconObj.has("format") ? iconObj.get("format").getAsString() : null;

        JsonElement value = iconObj.get("value");
        if (value.isJsonPrimitive()) {
            icon.value = value.getAsString();
        } else if (value.isJsonObject()) {
            JsonObject valObj = value.getAsJsonObject();
            AbilityTreeData.Icon.IconValue iv = new AbilityTreeData.Icon.IconValue();
            iv.id = valObj.has("id") ? valObj.get("id").getAsString() : null;
            iv.name = valObj.has("name") ? valObj.get("name").getAsString() : null;
            iv.customModelData = valObj.has("customModelData") ? valObj.get("customModelData") : null;
            icon.value = iv;
        }

        return icon;
    }
}

