package julianh06.wynnextras.features.buildplanner.data;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class IdentificationUnitsTest {
    @Test
    void formatsPercentageAndRawDamageDifferently() {
        assertEquals("+65%", IdentificationUnits.format("spellDamage", 65));
        assertEquals("+250", IdentificationUnits.format("rawSpellDamage", 250));
        assertEquals("-12%", IdentificationUnits.format("fireDamage", -12));
    }

    @Test
    void formatsRateAndRangeUnits() {
        assertEquals("+7/5s", IdentificationUnits.format("manaRegen", 7));
        assertEquals("+120/3s", IdentificationUnits.format("lifeSteal", 120));
        assertEquals("+4 blocks", IdentificationUnits.format("mainAttackRange", 4));
    }
}
