package julianh06.wynnextras.features.buildplanner.data;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class SkillPointWarningTest {
    @Test
    void recommendsTheMatchingTomeForOneToFourPointsAboveEachManualCap() {
        String[] skills = {"Str", "Dex", "Int", "Def", "Agi"};
        for (int skill = 0; skill < 5; skill++) {
            int[] assigned = new int[5];
            assigned[skill] = 100;
            assertTrue(SkillPointWarning.forBuild(120, assigned, List.of()).isEmpty());
            for (int value = 101; value <= 104; value++) {
                assigned[skill] = value;
                var warning = SkillPointWarning.forBuild(120, assigned, List.of()).orElseThrow();
                assertTrue(warning.tomeSuggestion());
                assertEquals("Warning: Build requires a " + skills[skill] + " Tome", warning.message());
            }
            assigned[skill] = 105;
            assertFalse(SkillPointWarning.forBuild(120, assigned, List.of()).orElseThrow().tomeSuggestion());
        }
    }

    @Test
    void rainbowCanCoverOnePointInMultipleSkillsButCannotStackWithAnotherGuildTome() {
        var warning = SkillPointWarning.forBuild(120, new int[]{101, 101, 0, 0, 0}, List.of()).orElseThrow();
        assertEquals("Warning: Build requires a Rainbow Tome", warning.message());
        assertTrue(warning.tomeSuggestion());
        assertFalse(SkillPointWarning.forBuild(120, new int[]{102, 101, 0, 0, 0},
                List.of()).orElseThrow().tomeSuggestion());
        assertFalse(SkillPointWarning.forBuild(120, new int[]{101, 101, 0, 0, 0},
                List.of(guild("dex"))).orElseThrow().tomeSuggestion());
    }

    @Test
    void accountsForTheTomeBeingReplacedWithoutDoubleCountingItsBonus() {
        var warning = SkillPointWarning.forBuild(120, new int[]{0, 104, 0, 0, 0},
                List.of(guild("str"))).orElseThrow();
        assertTrue(warning.tomeSuggestion());
        assertEquals("Warning: Build requires a Dex Tome (replace guild tome)", warning.message());
        assertFalse(SkillPointWarning.forBuild(120, new int[]{0, 101, 0, 0, 0},
                List.of(guild("dex"))).orElseThrow().tomeSuggestion());
        assertFalse(SkillPointWarning.forBuild(120, new int[]{100, 104, 0, 0, 0},
                List.of(guild("str"))).orElseThrow().tomeSuggestion());
        WynnTome rainbow = TomeDatabase.getInstance().get("Assimilator's Tome of Allegiance");
        assertTrue(SkillPointWarning.forBuild(120, new int[]{0, 103, 0, 0, 0},
                List.of(rainbow)).orElseThrow().tomeSuggestion());
        assertFalse(SkillPointWarning.forBuild(120, new int[]{0, 104, 0, 0, 0},
                List.of(rainbow)).orElseThrow().tomeSuggestion());
    }

    @Test
    void rejectsUnavailableTomesAndInsufficientTotalSkillPoints() {
        assertFalse(SkillPointWarning.forBuild(99, new int[]{0, 101, 0, 0, 0},
                List.of()).orElseThrow().tomeSuggestion());
        assertFalse(SkillPointWarning.forBuild(120, new int[]{99, 104, 99, 0, 0},
                List.of()).orElseThrow().tomeSuggestion());
    }

    @Test
    void aSuggestionDoesNotMakeAnOverCapBuildValidUntilTheTomeIsEquipped() {
        WynnItem boots = new WynnItem("Dex boots", "armour", "boots", "normal", "", Map.of(), Map.of("dexReq", 104));
        var equipped = List.of(new BuildCalculator.EquippedItem(boots, "", false));
        var original = BuildCalculator.optimizeSkillPoints(120, equipped);
        assertEquals(104, original.assigned()[1]);
        assertTrue(SkillPointWarning.forBuild(120, original.assigned(), List.of()).orElseThrow().tomeSuggestion());
        assertFalse(BuildCalculator.calculate(120, original.assigned(), equipped).valid());
        var tomes = List.of(guild("dex"));
        var withTome = BuildCalculator.optimizeSkillPoints(120, equipped, tomes);
        assertEquals(100, withTome.assigned()[1]);
        assertTrue(BuildCalculator.calculate(120, withTome.assigned(), equipped, tomes).valid());
    }

    private static WynnTome guild(String skill) {
        return TomeDatabase.getInstance().search("guildTome", "").stream()
                .filter(tome -> tome.identifications().getOrDefault(skill, 0) == 4)
                .findFirst().orElseThrow();
    }
}
