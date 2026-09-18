package julianh06.wynnextras.features.buildplanner.data;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;

class DamageCalculatorTest {
    @Test
    void appliesSelectedAspectTierToSpellDamage() {
        WynnItem bow = new WynnItem(
                "Test Bow", "weapon", "bow", "normal", "normal",
                Map.of(), Map.of("nDamMin", 100, "nDamMax", 200));
        BuildCalculator.Result build = BuildCalculator.calculate(
                120, new int[5],
                List.of(new BuildCalculator.EquippedItem(bow, "", true)));
        AbilityTreeDefinition.Node arrowBomb = node("arrowBomb", "Arrow Bomb");
        AbilityTreeDefinition.Node grapeBomb = node("grapeBomb", "Grape Bomb");
        AbilityTreeDefinition tree = new AbilityTreeDefinition(
                AbilityTreeClass.ARCHER, Map.of(),
                Map.of("arrowBomb", arrowBomb, "grapeBomb", grapeBomb),
                List.of(), Map.of("arrowBomb", Set.of(), "grapeBomb", Set.of()),
                "arrowBomb", 1);
        WynnAspect aspect = AspectDatabase.getInstance().get(
                AbilityTreeClass.ARCHER, "Aspect of Fragmentation Rounds");

        DamageCalculator.SpellResult without = DamageCalculator.calculate(
                        build, "bow", tree, Set.of("arrowBomb", "grapeBomb")).stream()
                .filter(result -> result.index() == 3)
                .findFirst()
                .orElseThrow();
        DamageCalculator.SpellResult with = DamageCalculator.calculate(
                        build, "bow", tree, Set.of("arrowBomb", "grapeBomb"),
                        List.of(new AspectSelection(aspect, 1))).stream()
                .filter(result -> result.index() == 3)
                .findFirst()
                .orElseThrow();
        DamageCalculator.SpellResult inactive = DamageCalculator.calculate(
                        build, "bow", tree, Set.of("arrowBomb"),
                        List.of(new AspectSelection(aspect, 1))).stream()
                .filter(result -> result.index() == 3)
                .findFirst()
                .orElseThrow();

        DamageCalculator.PartResult withoutGrape = without.parts().stream()
                .filter(part -> part.name().equals("Grape Bomb"))
                .findFirst()
                .orElseThrow();
        DamageCalculator.PartResult withGrape = with.parts().stream()
                .filter(part -> part.name().equals("Grape Bomb"))
                .findFirst()
                .orElseThrow();
        assertEquals(50, withoutGrape.multipliers()[0], 0.0001);
        assertEquals(75, withGrape.multipliers()[0], 0.0001);
        assertEquals(
                0,
                inactive.parts().stream()
                        .filter(part -> part.name().equals("Grape Bomb"))
                        .count());
    }

    @Test
    void calculatesSelectedArrowBombFromWynnBuilderData() {
        WynnItem bow = new WynnItem(
                "Test Bow", "weapon", "bow", "normal", "normal",
                Map.of(), Map.of("nDamMin", 100, "nDamMax", 200));
        BuildCalculator.Result build = BuildCalculator.calculate(
                120, new int[5],
                List.of(new BuildCalculator.EquippedItem(bow, "", true)));
        AbilityTreeDefinition.Node arrowBomb = new AbilityTreeDefinition.Node(
                "arrowBomb", "Arrow Bomb", List.of(), 1, 1, 1,
                1, 1, List.of(), null, List.of(), "", 0xFFFFFFFF, "");
        AbilityTreeDefinition tree = new AbilityTreeDefinition(
                AbilityTreeClass.ARCHER, Map.of(), Map.of("arrowBomb", arrowBomb),
                List.of(), Map.of("arrowBomb", Set.of()), "arrowBomb", 1);

        DamageCalculator.SpellResult spell = DamageCalculator.calculate(
                        build, "bow", tree, Set.of("arrowBomb")).stream()
                .filter(result -> result.index() == 3)
                .findFirst()
                .orElseThrow();

        assertEquals("Arrow Bomb", spell.name());
        assertEquals(45, spell.cost(), 0.0001);
        DamageCalculator.PartResult total = spell.summaryPart();
        assertNotNull(total);
        assertEquals(328, total.normalTotal()[0], 0.0001);
        assertEquals(656, total.normalTotal()[1], 0.0001);
        assertEquals(492, total.average(0), 0.0001);
    }

    @Test
    void firstTierNameDoesNotActivateLaterRomanNumeralTiers() {
        WynnItem bow = new WynnItem(
                "Test Bow", "weapon", "bow", "normal", "normal",
                Map.of(), Map.of("nDamMin", 100, "nDamMax", 200));
        BuildCalculator.Result build = BuildCalculator.calculate(
                120, new int[5],
                List.of(new BuildCalculator.EquippedItem(bow, "", true)));
        AbilityTreeDefinition.Node arrowBomb = node("arrowBomb", "Arrow Bomb");
        AbilityTreeDefinition.Node cheaper = node("cheaper", "Cheaper Arrow Bomb");
        AbilityTreeDefinition tree = new AbilityTreeDefinition(
                AbilityTreeClass.ARCHER, Map.of(),
                Map.of("arrowBomb", arrowBomb, "cheaper", cheaper),
                List.of(), Map.of("arrowBomb", Set.of(), "cheaper", Set.of()),
                "arrowBomb", 1);

        DamageCalculator.SpellResult spell = DamageCalculator.calculate(
                        build, "bow", tree, Set.of("arrowBomb", "cheaper")).stream()
                .filter(result -> result.index() == 3)
                .findFirst()
                .orElseThrow();

        assertEquals(35, spell.cost(), 0.0001);
    }

    @Test
    void usesClassBaseRangeAndSelectedMainAttackRangeAbilities() {
        assertEquals(15, DamageCalculator.mainAttackRange("relik", null, Set.of()), 0.0001);

        AbilityTreeDefinition.Node proficiency = node("proficiency", "Bow Proficiency");
        AbilityTreeDefinition tree = new AbilityTreeDefinition(
                AbilityTreeClass.ARCHER, Map.of(), Map.of("proficiency", proficiency),
                List.of(), Map.of("proficiency", Set.of()), "proficiency", 1);

        assertEquals(
                15,
                DamageCalculator.mainAttackRange("bow", tree, Set.of("proficiency")),
                0.0001);
    }

    @Test
    void matchesCurrentWynnBuilderForSuppliedSpringBuild() {
        Map<String, Integer> ids = Map.ofEntries(
                Map.entry("earthDamage", 20),
                Map.entry("thunderDamage", -35),
                Map.entry("waterDamage", 78),
                Map.entry("airDamage", 20),
                Map.entry("spellDamage", 145),
                Map.entry("rawDamage", 265),
                Map.entry("rawAttackSpeed", -7),
                Map.entry("2ndSpellCost", 11),
                Map.entry("raw3rdSpellCost", -4),
                Map.entry("mainAttackRange", 30));
        BuildCalculator.Result build = new BuildCalculator.Result(
                13960, 0, 0, 0,
                new int[]{55, -160, 141, -70, 140},
                new double[5],
                ids,
                new BuildCalculator.DamageRange[]{
                        new BuildCalculator.DamageRange(135, 185),
                        new BuildCalculator.DamageRange(0, 0),
                        new BuildCalculator.DamageRange(0, 0),
                        new BuildCalculator.DamageRange(170, 310),
                        new BuildCalculator.DamageRange(0, 0),
                        new BuildCalculator.DamageRange(0, 0)
                },
                3, 0, 0, null, null, true, "");

        String[] names = {
                "Arrow Bomb", "Bow Proficiency", "Cheaper Arrow Bomb I", "Heart Shatter",
                "Escape", "Double Shots", "Arrow Storm", "Cheaper Escape I", "Arrow Shield",
                "Windy Feet", "Water Mastery", "Thunder Mastery", "Nimble String",
                "Triple Shots", "Guardian Angels", "Arrow Wall", "Implosion",
                "Cheaper Arrow Storm I", "Leap", "Fierce Stomp", "Grape Bomb", "Windstorm",
                "Frenzy", "Shrapnel Bomb", "Divine Intervention", "Bounding Stride",
                "Recycling", "Vigilant Sentinels", "Cheaper Arrow Storm II", "Bouncing Bomb",
                "Arrow Hurricane", "Transience", "Shrieking Arrows", "Pyrotechnics",
                "Elusive", "Stormy Feet"
        };
        Map<String, AbilityTreeDefinition.Node> nodes = new LinkedHashMap<>();
        for (int i = 0; i < names.length; i++) {
            nodes.put("n" + i, node("n" + i, names[i]));
        }
        AbilityTreeDefinition tree = new AbilityTreeDefinition(
                AbilityTreeClass.ARCHER, Map.of(), nodes, List.of(), Map.of(), "n0", 1);

        List<DamageCalculator.SpellResult> spells = DamageCalculator.calculate(
                build, "bow", tree, nodes.keySet());

        assertEquals(1312.40, spells.get(0).summaryPart().average(0) * 0.51, 0.01);
        assertEquals(78820.60, spells.get(1).summaryPart().average(0), 0.01);
        assertEquals(11156.42, spells.get(2).summaryPart().average(0), 0.01);
        assertEquals(15815.83, spells.get(3).summaryPart().average(0), 0.01);
        assertEquals(16354.07, spells.get(4).summaryPart().average(0), 0.01);
        assertEquals(19.5, DamageCalculator.mainAttackRange("bow", tree, nodes.keySet())
                * (1 + ids.get("mainAttackRange") / 100.0), 0.0001);
    }

    private static AbilityTreeDefinition.Node node(String id, String name) {
        return new AbilityTreeDefinition.Node(
                id, name, List.of(), 1, 1, 1,
                1, 1, List.of(), null, List.of(), "", 0xFFFFFFFF, "");
    }
}
