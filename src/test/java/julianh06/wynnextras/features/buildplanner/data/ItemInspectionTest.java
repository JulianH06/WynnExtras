package julianh06.wynnextras.features.buildplanner.data;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.gson.JsonParser;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class ItemInspectionTest {
    @Test
    void matchesTheThunderSanctuaryReferenceCardContentAndOrdering() throws Exception {
        WynnItem item;
        try (var stream = getClass().getResourceAsStream("/api-masterwork-items.json")) {
            ItemDatabase database = new ItemDatabase();
            item = database.parseItems(JsonParser.parseReader(new InputStreamReader(stream, StandardCharsets.UTF_8)))
                    .stream().filter(entry -> entry.displayName().equals("Thunder Sanctuary")).findFirst().orElseThrow();
        }
        List<ItemInspection.Row> rows = ItemInspection.rows(item, "");
        assertEquals(List.of("Earth Defense: -70", "Thunder Defense: 320", "Combat Level Min: 80",
                "Dexterity Min: 70", "Dexterity: 5", "Mana Steal: 12/3s", "Thunder Damage %: 90%",
                "Powder Slots: 2 []", item.lore(), "untradable", "Legendary leggings"),
                rows.stream().filter(row -> row.kind() != ItemInspection.Kind.GAP)
                        .map(row -> row.label() + (row.value().isEmpty() ? "" : ": " + row.value())).toList());
        assertEquals(0, rows.get(0).element());
        assertEquals(1, rows.get(1).element());
        assertTrue(rows.stream().filter(row -> row.kind() == ItemInspection.Kind.SKILL
                || row.kind() == ItemInspection.Kind.IDENTIFICATION).allMatch(ItemInspection.Row::beneficial));
        assertEquals(ItemInspection.Kind.LORE, rows.get(rows.size() - 3).kind());
    }

    @Test
    void showsMaximumRollsAndColorsNegativeSpellCostsAsBeneficial() {
        WynnItem item = new WynnItem("Example", "accessory", "ring", "rare", "",
                Map.of("manaRegen", new Identification(3, 10, 13),
                        "raw1stSpellCost", new Identification(-10, -8, -6),
                        "walkSpeed", new Identification(-20, -15, -10)),
                Map.of("lvl", 100));
        List<ItemInspection.Row> ids = ItemInspection.rows(item, "").stream()
                .filter(row -> row.kind() == ItemInspection.Kind.IDENTIFICATION).toList();
        assertEquals("13/5s", ids.get(0).value());
        assertEquals("-6", ids.get(1).value());
        assertTrue(ids.get(1).beneficial());
        assertFalse(ids.get(2).beneficial());
        assertFalse(ItemInspection.rows(item, "").stream().anyMatch(row -> row.label().equals("Powder Slots")));
    }

    @Test
    void preservesCraftingDetailsPowdersAndMajorIdLimitations() {
        WynnItem craft = CraftedItemCodec.decode("CR-1+W+W+W+W+W+W9b12");
        List<ItemInspection.Row> rows = ItemInspection.rows(craft, "w6w6");
        assertTrue(rows.stream().anyMatch(row -> row.label().equals("Code") && row.value().equals(craft.reference())));
        assertTrue(rows.stream().anyMatch(row -> row.label().equals("Combat Level Min") && row.value().equals("103")));
        assertTrue(rows.stream().anyMatch(row -> row.label().equals("Powder Slots") && row.value().equals("3 [w6w6]")));
        assertTrue(rows.stream().anyMatch(row -> row.label().equals("Durability")));
        WynnItem major = new WynnItem("Major", "weapon", "bow", "mythic", "fast", Map.of(),
                Map.of("lvl", 100), "", "", 0, "", List.of(), Map.of("Major power", "Description"));
        assertTrue(ItemInspection.rows(major, "").stream().anyMatch(row -> row.kind() == ItemInspection.Kind.WARNING
                && row.label().contains("not simulated")));
    }
}
