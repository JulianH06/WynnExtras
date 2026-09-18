package julianh06.wynnextras.features.buildplanner.data;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class CraftedItemCodecTest {
    private static final String BOW = "CR-1+W+W+W+W+W+W9b12";
    private static final String MODERN_BOW = "CR-40w3w3w3w3w3wNc080";

    @Test
    void matchesUpstreamForEveryEquipmentTypeMaterialTierPowderAndRandomIngredientLayout() throws Exception {
        try (var stream = getClass().getResourceAsStream("/crafted-item-oracle.json")) {
            JsonArray fixtures = JsonParser.parseReader(
                    new InputStreamReader(stream, StandardCharsets.UTF_8)).getAsJsonArray();
            assertEquals(768, fixtures.size());
            for (JsonElement element : fixtures) {
                JsonObject fixture = element.getAsJsonObject();
                for (String version : List.of("legacy", "modern")) {
                    String code = fixture.get(version).getAsString();
                    WynnItem item = CraftedItemCodec.decode(code);
                    assertTrue(item.isCrafted());
                    assertEquals(code, item.reference());
                    assertEquals(fixture.get("subtype").getAsString(), item.subType(), code);
                    for (var entry : fixture.getAsJsonObject("stats").entrySet()) {
                        String key = entry.getKey().equals("slots") ? "powderSlots" : entry.getKey();
                        assertEquals(entry.getValue().getAsInt(), item.stat(key), code + " " + key);
                    }
                    assertEquals(fixture.getAsJsonArray("durability").get(0).getAsInt(),
                            item.stat("durabilityMin"), code);
                    assertEquals(fixture.getAsJsonArray("durability").get(1).getAsInt(),
                            item.stat("durabilityMax"), code);
                    for (int i = 0; i < 6; i++) {
                        assertEquals(fixture.getAsJsonArray("effectiveness").get(i).getAsInt(),
                                item.stat("ingredientEffectiveness" + i), code);
                    }
                    Map<String, Integer> actualMin = new LinkedHashMap<>();
                    Map<String, Integer> actualMax = new LinkedHashMap<>();
                    item.identifications().forEach((key, roll) -> {
                        if (roll.min() != 0) actualMin.put(key, roll.min());
                        if (roll.max() != 0) actualMax.put(key, roll.max());
                    });
                    assertEquals(ids(fixture.getAsJsonObject("minIds")), actualMin, code);
                    assertEquals(ids(fixture.getAsJsonObject("maxIds")), actualMax, code);
                    if (fixture.has("damage")) {
                        assertDamage(fixture.getAsJsonArray("damage"), item, "");
                        assertDamage(fixture.getAsJsonArray("powderedDamage"), item, "e6t7e6");
                    }
                }
            }
        }
    }

    @Test
    void convertsEveryAccessoryPairWithUpstreamRecipesCodesAndRecalculatedDurability() throws Exception {
        try (var stream = getClass().getResourceAsStream("/crafted-accessory-conversions.json")) {
            JsonArray fixtures = JsonParser.parseReader(
                    new InputStreamReader(stream, StandardCharsets.UTF_8)).getAsJsonArray();
            assertTrue(fixtures.size() > 1000);
            for (JsonElement element : fixtures) {
                JsonObject fixture = element.getAsJsonObject();
                for (String version : List.of("legacy", "modern")) {
                    String source = fixture.get(version.equals("legacy") ? "sourceLegacy" : "sourceModern").getAsString();
                    String slot = fixture.get("target").getAsString();
                    String expected = fixture.get(version).getAsString();
                    WynnItem item = CraftedItemCodec.decodeForSlot(source, slot);
                    assertEquals(slot, item.subType(), source);
                    assertEquals(expected, item.reference(), source);
                    assertEquals(item, ItemDatabase.getInstance().getItem(item.reference()));
                    assertEquals(fixture.getAsJsonArray("durability").get(0).getAsInt(),
                            item.stat("durabilityMin"), source);
                    assertEquals(fixture.getAsJsonArray("durability").get(1).getAsInt(),
                            item.stat("durabilityMax"), source);
                    Map<String, Integer> min = new LinkedHashMap<>();
                    Map<String, Integer> max = new LinkedHashMap<>();
                    item.identifications().forEach((key, roll) -> {
                        if (roll.min() != 0) min.put(key, roll.min());
                        if (roll.max() != 0) max.put(key, roll.max());
                    });
                    assertEquals(ids(fixture.getAsJsonObject("minIds")), min, source);
                    assertEquals(ids(fixture.getAsJsonObject("maxIds")), max, source);
                }
            }
        }
    }

    @Test
    void rejectsUnrelatedSlotConversionsAndKeepsExistingWeaponCodes() {
        assertEquals(BOW, CraftedItemCodec.decodeForSlot(BOW, "weapon").reference());
        assertThrows(IllegalArgumentException.class, () -> CraftedItemCodec.decodeForSlot(BOW, "necklace"));
        WynnItem ordinary = new WynnItem("Ordinary ring", "accessory", "ring", "normal", "", Map.of(), Map.of());
        assertFalse(CraftedItemCodec.matchesSlot(ordinary, "necklace"));
    }

    @Test
    void acceptsCurrentAndLegacyCodesAndCrafterLinksWithoutNetworkAccess() {
        assertEquals(BOW, CraftedItemCodec.decode(" \n" + BOW + "\n ").reference());
        assertEquals(BOW, CraftedItemCodec.decode(BOW.substring(3)).reference());
        assertEquals(MODERN_BOW, CraftedItemCodec.decode(
                "https://wynnbuilder.github.io/crafter/#" + MODERN_BOW.substring(3)).reference());
        assertEquals(BOW, CraftedItemCodec.decode(
                "https://hppeng-wynn.github.io/crafter.html#" + BOW).reference());
        assertEquals("bow", ItemDatabase.getInstance().getItem(BOW).subType());
        assertTrue(CraftedItemCodec.matchesSlot(CraftedItemCodec.decode(BOW), "weapon"));
        assertFalse(CraftedItemCodec.matchesSlot(CraftedItemCodec.decode(BOW), "helmet"));
    }

    @Test
    void rejectsBadCharactersVersionsPaddingMissingIdsTiersAndSpeeds() {
        for (String code : List.of("", "CR-", BOW + "0", BOW.substring(0, BOW.length() - 1),
                BOW.replace('+', '/'), BOW.substring(0, BOW.length() - 2) + "02",
                BOW.substring(0, BOW.length() - 1) + "3",
                BOW.substring(0, 4) + "--" + BOW.substring(6),
                "CR-2" + MODERN_BOW.substring(4),
                MODERN_BOW.substring(0, MODERN_BOW.length() - 1) + "1",
                "https://wynnbuilder.github.io/#" + BOW,
                "https://example.com/crafter/#" + BOW, "X".repeat(2049))) {
            assertThrows(IllegalArgumentException.class, () -> CraftedItemCodec.decode(code), code);
        }
        assertThrows(IllegalArgumentException.class, () -> CraftedItemCodec.decode(null));
    }

    @Test
    void rejectsUncraftableRecipesAndConsumables() throws Exception {
        try (var stream = getClass().getResourceAsStream("/crafted-invalid-oracle.json")) {
            JsonArray invalid = JsonParser.parseReader(
                    new InputStreamReader(stream, StandardCharsets.UTF_8)).getAsJsonArray();
            for (JsonElement element : invalid) {
                JsonObject fixture = element.getAsJsonObject();
                for (String version : List.of("legacy", "modern")) {
                    assertThrows(IllegalArgumentException.class,
                            () -> CraftedItemCodec.decode(fixture.get(version).getAsString()),
                            fixture.get("reason").getAsString());
                }
            }
        }
    }

    @Test
    void craftedWeaponCanBeUsedAtTheLowerEndOfItsRecipeLevelRange() {
        WynnItem bow = CraftedItemCodec.decode(BOW);
        assertTrue(BuildCalculator.calculate(103, new int[5],
                List.of(new BuildCalculator.EquippedItem(bow, "", true))).valid());
        assertFalse(BuildCalculator.calculate(102, new int[5],
                List.of(new BuildCalculator.EquippedItem(bow, "", true))).valid());
    }

    @Test
    void craftedBonusesAffectFinalSkillsButCannotMeetEquipmentRequirements() {
        WynnItem crafted = craftedSkillItem(0, 30);
        WynnItem ordinary = new WynnItem("Requires 50", "armour", "boots", "normal", "",
                Map.of(), Map.of("strReq", 50));
        var equipment = List.of(new BuildCalculator.EquippedItem(crafted, "", false),
                new BuildCalculator.EquippedItem(ordinary, "", false));
        var plan = BuildCalculator.optimizeSkillPoints(120, equipment);
        assertEquals(50, plan.assigned()[0]);
        assertEquals(80, plan.finalSkills()[0]);
        assertFalse(BuildCalculator.calculate(120, new int[]{20, 0, 0, 0, 0}, equipment).valid());
        assertTrue(BuildCalculator.calculate(120, plan.assigned(), equipment).valid());
        var ownRequirement = BuildCalculator.optimizeSkillPoints(120,
                List.of(new BuildCalculator.EquippedItem(craftedSkillItem(50, 30), "", false)));
        assertEquals(50, ownRequirement.assigned()[0]);
        assertEquals(80, ownRequirement.finalSkills()[0]);
    }

    private static WynnItem craftedSkillItem(int requirement, int bonus) {
        return new WynnItem("Crafted helmet", "armour", "helmet", "crafted", "",
                Map.of("str", new Identification(bonus, bonus, bonus)), Map.of("strReq", requirement),
                "", "", 0, "CR-test", List.of());
    }

    private static Map<String, Integer> ids(JsonObject json) {
        Map<String, Integer> result = new LinkedHashMap<>();
        json.entrySet().forEach(entry ->
                result.put(WynnBuilderIdentifications.name(entry.getKey()), entry.getValue().getAsInt()));
        return result;
    }

    private static void assertDamage(JsonArray expected, WynnItem weapon, String powders) {
        var result = BuildCalculator.calculate(120, new int[5],
                List.of(new BuildCalculator.EquippedItem(weapon, powders, true)));
        double expectedAverage = 0;
        for (int i = 0; i < 6; i++) {
            double min = expected.get(i).getAsJsonArray().get(0).getAsDouble();
            double max = expected.get(i).getAsJsonArray().get(1).getAsDouble();
            assertEquals(min, result.weaponDamage()[i].min(), 0.000001, weapon.reference() + " " + powders);
            assertEquals(max, result.weaponDamage()[i].max(), 0.000001, weapon.reference() + " " + powders);
            expectedAverage += (min + max) / 2;
        }
        double speed = switch (weapon.attackSpeed()) {
            case "slow" -> 1.5;
            case "fast" -> 2.5;
            default -> 2.05;
        };
        assertEquals(expectedAverage * speed, BuildCalculator.weaponBaseDps(weapon, powders), 0.000001);
    }
}
