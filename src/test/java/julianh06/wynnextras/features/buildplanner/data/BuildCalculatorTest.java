package julianh06.wynnextras.features.buildplanner.data;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class BuildCalculatorTest {
    @Test
    void appliesEachTomeExactlyOnce() {
        WynnTome tome = new WynnTome(
                "Test Tome", "", "armorTome", "Fabled", 1,
                Map.of("rawHealth", 300, "spellDamage", 7));

        BuildCalculator.Result result = BuildCalculator.calculate(
                1, new int[5], List.of(), List.of(tome));

        assertEquals(310, result.health());
        assertEquals(7, result.ids().get("spellDamage"));
        assertEquals(
                "Test Tome (Health, Spell Damage)",
                tome.nameWithStats());
    }

    @Test
    void summarizesAllFiveSkillPointsAsRainbow() {
        WynnTome tome = new WynnTome(
                "Assimilator's Tome of Allegiance", "", "guildTome", "Fabled", 100,
                Map.of("str", 2, "dex", 2, "int", 2, "def", 2, "agi", 2));

        assertEquals("Assimilator's Tome of Allegiance (Rainbow)", tome.nameWithStats());
    }

    @Test
    void loadsAllWynnBuilderTomeCategories() {
        TomeDatabase database = TomeDatabase.getInstance();

        assertFalse(database.search("weaponTome", "").isEmpty());
        assertFalse(database.search("armorTome", "").isEmpty());
        assertFalse(database.search("gatherXpTome", "").isEmpty());
        assertFalse(database.search("dungeonXpTome", "").isEmpty());
        assertFalse(database.search("mobXpTome", "").isEmpty());
        assertFalse(database.search("guildTome", "").isEmpty());
        assertFalse(database.search("lootrunTome", "").isEmpty());
    }

    @Test
    void usesMaximumRollsAndEffectiveAttackSpeedForMeleeDps() {
        WynnItem weapon = item(
                "Test Wand", "weapon", "wand", "normal",
                Map.of(
                        "mainAttackDamage", new Identification(10, 15, 20),
                        "rawAttackSpeed", new Identification(1, 1, 1)),
                Map.of("nDamMin", 100, "nDamMax", 200));

        BuildCalculator.Result result = BuildCalculator.calculate(
                120, new int[5],
                List.of(new BuildCalculator.EquippedItem(weapon, "", true)));

        assertEquals(120, result.meleeNormal().min(), 0.0001);
        assertEquals(240, result.meleeNormal().max(), 0.0001);
        assertEquals(4, result.effectiveAttackTier());
        assertEquals(450, result.meleeDps(), 0.0001);
    }

    @Test
    void appliesGroupedWeaponPowdersToRemainingNeutralDamage() {
        WynnItem weapon = item(
                "Powder Wand", "weapon", "wand", "normal",
                Map.of(), Map.of("nDamMin", 100, "nDamMax", 200));

        BuildCalculator.Result result = BuildCalculator.calculate(
                120, new int[5],
                List.of(new BuildCalculator.EquippedItem(weapon, "e7", true)));

        assertEquals(48, result.weaponDamage()[0].min(), 0.0001);
        assertEquals(96, result.weaponDamage()[0].max(), 0.0001);
        assertEquals(64, result.weaponDamage()[1].min(), 0.0001);
        assertEquals(118, result.weaponDamage()[1].max(), 0.0001);
    }

    @Test
    void appliesArmorPowderHealthAndCyclicDefenses() {
        WynnItem helmet = item(
                "Powder Helmet", "armour", "helmet", "",
                Map.of("rawHealth", new Identification(70, 100, 130)),
                Map.of("hp", 100));

        BuildCalculator.Result result = BuildCalculator.calculate(
                120, new int[5],
                List.of(new BuildCalculator.EquippedItem(helmet, "e7", false)));

        assertEquals(910, result.health());
        assertArrayEquals(new double[]{37, 0, 0, 0, -12}, result.defenses(), 0.0001);
    }

    @Test
    void exposesPowderAdjustedEquipmentRowValues() {
        WynnItem weapon = item(
                "Powder Bow", "weapon", "bow", "normal",
                Map.of(), Map.of(
                        "nDamMin", 135, "nDamMax", 185,
                        "wDamMin", 170, "wDamMax", 310));
        WynnItem helmet = item(
                "Powder Helmet", "armour", "helmet", "",
                Map.of(), Map.of("hp", 3200));

        assertEquals(899.95, BuildCalculator.weaponBaseDps(weapon, "w7w7w7"), 0.0001);
        assertEquals(3425, BuildCalculator.armorHealthWithPowders(helmet, "e7w7a7"));
    }

    @Test
    void validatesRequirementsUsingARealEquipOrder() {
        WynnItem strengthItem = item(
                "Strength Item", "armour", "helmet", "",
                Map.of("dex", new Identification(20, 20, 20)),
                Map.of("strReq", 50));
        WynnItem dexterityItem = item(
                "Dexterity Item", "armour", "boots", "",
                Map.of(), Map.of("dexReq", 20));

        BuildCalculator.Result valid = BuildCalculator.calculate(
                120, new int[]{50, 0, 0, 0, 0},
                List.of(
                        new BuildCalculator.EquippedItem(dexterityItem, "", false),
                        new BuildCalculator.EquippedItem(strengthItem, "", false)));
        BuildCalculator.Result invalid = BuildCalculator.calculate(
                120, new int[5],
                List.of(new BuildCalculator.EquippedItem(strengthItem, "", false)));

        assertTrue(valid.valid());
        assertFalse(invalid.valid());
    }

    @Test
    void preservesNegativeHealthRegenWithSignAwarePercentage() {
        WynnItem item = item(
                "Negative Regen", "armour", "helmet", "",
                Map.of(
                        "healthRegenRaw", new Identification(-100, -100, -100),
                        "healthRegen", new Identification(50, 50, 50)),
                Map.of());

        BuildCalculator.Result result = BuildCalculator.calculate(
                120, new int[5],
                List.of(new BuildCalculator.EquippedItem(item, "", false)));

        assertEquals(-50, result.healthRegen(), 0.0001);
    }

    @Test
    void addsModernItemSkillPointIdentificationsToAssignedSkills() {
        WynnItem item = item(
                "Skill Item", "armour", "helmet", "",
                Map.of(
                        "rawStrength", new Identification(4, 4, 4),
                        "rawDexterity", new Identification(5, 5, 5),
                        "rawIntelligence", new Identification(6, 6, 6),
                        "rawDefence", new Identification(7, 7, 7),
                        "rawAgility", new Identification(8, 8, 8)),
                Map.of());

        BuildCalculator.Result result = BuildCalculator.calculate(
                120, new int[]{10, 20, 30, 40, 50},
                List.of(new BuildCalculator.EquippedItem(item, "", false)));

        assertArrayEquals(new int[]{14, 25, 36, 47, 58}, result.skills());
    }

    @Test
    void automaticallyAssignsRequirementsWithoutChargingItemSkillPoints() {
        WynnItem nexus = item(
                "Nexus", "armour", "helmet", "",
                Map.of(
                        "int", new Identification(8, 8, 8),
                        "agi", new Identification(8, 8, 8)),
                Map.of("intReq", 60, "agiReq", 60));

        BuildCalculator.SkillPointPlan plan = BuildCalculator.optimizeSkillPoints(
                120, List.of(new BuildCalculator.EquippedItem(nexus, "", false)));

        assertArrayEquals(new int[]{0, 0, 60, 0, 60}, plan.assigned());
        assertArrayEquals(new int[]{0, 0, 68, 0, 68}, plan.finalSkills());
        assertEquals(120, plan.totalAssigned());
        assertTrue(plan.problem().isEmpty());
    }

    @Test
    void reportsRequirementsAboveThePerSkillAssignmentCap() {
        WynnItem warp = item(
                "Warp", "weapon", "wand", "superFast",
                Map.of(), Map.of("agiReq", 125));

        BuildCalculator.SkillPointPlan plan = BuildCalculator.optimizeSkillPoints(
                120, List.of(new BuildCalculator.EquippedItem(warp, "", true)));

        assertArrayEquals(new int[]{0, 0, 0, 0, 125}, plan.assigned());
        assertFalse(plan.underPerSkillCap());
        assertEquals("At most 100 points can be assigned to one skill", plan.problem());
    }

    @Test
    void usesArmorBonusesToBringAWeaponRequirementUnderTheCap() {
        WynnItem agilityArmor = item(
                "Agility Armor", "armour", "boots", "",
                Map.of("agi", new Identification(30, 30, 30)), Map.of());
        WynnItem weapon = item(
                "Demanding Weapon", "weapon", "wand", "fast",
                Map.of(), Map.of("agiReq", 125));

        BuildCalculator.SkillPointPlan plan = BuildCalculator.optimizeSkillPoints(
                120, List.of(
                        new BuildCalculator.EquippedItem(agilityArmor, "", false),
                        new BuildCalculator.EquippedItem(weapon, "", true)));

        assertArrayEquals(new int[]{0, 0, 0, 0, 95}, plan.assigned());
        assertArrayEquals(new int[]{0, 0, 0, 0, 125}, plan.finalSkills());
        assertTrue(plan.underPerSkillCap());
        assertTrue(plan.problem().isEmpty());
    }

    @Test
    void assignsEnoughPointsToKeepNegativeSkillItemsEquipped() {
        WynnItem drainingHelmet = item(
                "Draining Helmet", "armour", "helmet", "",
                Map.of("agi", new Identification(-20, -20, -20)),
                Map.of("agiReq", 50));

        BuildCalculator.SkillPointPlan plan = BuildCalculator.optimizeSkillPoints(
                120, List.of(new BuildCalculator.EquippedItem(drainingHelmet, "", false)));

        assertArrayEquals(new int[]{0, 0, 0, 0, 50}, plan.assigned());
        assertArrayEquals(new int[]{0, 0, 0, 0, 30}, plan.finalSkills());
        assertEquals(List.of(drainingHelmet), plan.equipOrder());
    }

    @Test
    void doesNotCompensateNegativeBonusesWithoutARequirement() {
        WynnItem drainingHelmet = item(
                "Draining Helmet", "armour", "helmet", "",
                Map.of("dex", new Identification(-80, -80, -80)),
                Map.of());
        WynnItem drainingBoots = item(
                "Draining Boots", "armour", "boots", "",
                Map.of("dex", new Identification(-80, -80, -80)),
                Map.of());

        BuildCalculator.SkillPointPlan plan = BuildCalculator.optimizeSkillPoints(
                120, List.of(
                        new BuildCalculator.EquippedItem(drainingHelmet, "", false),
                        new BuildCalculator.EquippedItem(drainingBoots, "", false)));

        assertArrayEquals(new int[]{0, 0, 0, 0, 0}, plan.assigned());
        assertArrayEquals(new int[]{0, -160, 0, 0, 0}, plan.finalSkills());
        assertTrue(plan.problem().isEmpty());
    }

    @Test
    void appliesWynnBuilderSkillEffectMultipliers() {
        assertEquals(50.0, BuildCalculator.skillEffectPercentage(2, 150), 0.0001);
        assertEquals(
                BuildCalculator.skillPercentage(150) * 86.7,
                BuildCalculator.skillEffectPercentage(3, 150),
                0.0001);
        assertEquals(
                BuildCalculator.skillPercentage(150) * 95.1,
                BuildCalculator.skillEffectPercentage(4, 150),
                0.0001);
    }

    private static WynnItem item(
            String name,
            String type,
            String subtype,
            String attackSpeed,
            Map<String, Identification> ids,
            Map<String, Integer> stats
    ) {
        return new WynnItem(name, type, subtype, "legendary", attackSpeed, ids, stats);
    }
}
