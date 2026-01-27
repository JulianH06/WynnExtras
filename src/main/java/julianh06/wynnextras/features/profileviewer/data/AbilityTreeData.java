package julianh06.wynnextras.features.profileviewer.data;

import java.util.List;
import java.util.Map;

public class AbilityTreeData {
    public Map<String, Archetype> archetypes;
    public Map<Integer, Map<String, Ability>> pages;

    public static class Archetype {
        public String name;
        public String description;
        public String shortDescription;
        public Icon icon;
        public int slot;
    }

    public static class Ability {
        public String name;
        public Icon icon;
        public int slot;
        public Coordinates coordinates;
        public List<String> description;
        public Requirements requirements;
        public List<String> links;
        public List<String> locks;
        public int page;
    }

    public static class Requirements {
        public Integer ABILITY_POINTS;
        public String NODE;
        public ArchetypeRequirement ARCHETYPE;
    }

    public static class ArchetypeRequirement {
        public String name;
        public int amount;
    }

    public static class Coordinates {
        public int x;
        public int y;
    }

    public static class Icon {
        public String format;
        public Object value; // String or IconValue

        public static class IconValue {
            public String id;
            public String name;
            public Object customModelData; // String or nested object
        }
    }
}

