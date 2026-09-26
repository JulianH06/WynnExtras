package julianh06.wynnextras.features.buildplanner.data;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.Test;

class AspectDatabaseTest {
    @Test
    void loadsCurrentWynnBuilderAspectDatabase() {
        AspectDatabase database = AspectDatabase.getInstance();

        assertEquals(25, database.search(AbilityTreeClass.ARCHER, "").size());
        assertEquals(27, database.search(AbilityTreeClass.WARRIOR, "").size());
        assertEquals(25, database.search(AbilityTreeClass.MAGE, "").size());
        assertEquals(26, database.search(AbilityTreeClass.ASSASSIN, "").size());
        assertEquals(25, database.search(AbilityTreeClass.SHAMAN, "").size());
        WynnAspect aspect = database.get(
                AbilityTreeClass.ARCHER, "Aspect of Fragmentation Rounds");
        assertNotNull(aspect);
        assertEquals(3, aspect.tiers().size());
    }
}
