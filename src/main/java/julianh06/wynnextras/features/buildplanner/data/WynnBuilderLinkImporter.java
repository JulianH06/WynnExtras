package julianh06.wynnextras.features.buildplanner.data;

import julianh06.wynnextras.wynncraft.state.CharacterClass;

import julianh06.wynnextras.features.buildplanner.config.SavedBuild;
import julianh06.wynnextras.wynncraft.codec.WynnBuilderBuild;
import julianh06.wynnextras.wynncraft.codec.WynnBuilderBuildCodec;
import julianh06.wynnextras.wynncraft.codec.WynnBuilderVersions;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Converts the shared link model into the build planner's current persistence model. */
public final class WynnBuilderLinkImporter {
    private static final char[] POWDERS = {'e', 't', 'w', 'f', 'a'};

    private WynnBuilderLinkImporter() {}

    public static Result importLink(String input) {
        WynnBuilderItemIndex index = WynnBuilderItemIndex.getInstance();
        if (!index.isReady()) {
            throw new IllegalStateException(index.isLoading()
                    ? "WynnBuilder item IDs are still loading"
                    : index.error().isBlank() ? "WynnBuilder item IDs are unavailable" : index.error());
        }
        WynnBuilderBuild decoded = WynnBuilderBuildCodec.decode(input, CraftedItemCodec::isWeaponRecipe);
        String version = WynnBuilderVersions.byIndex(decoded.dataVersionIndex());
        if (version == null) {
            throw new IllegalArgumentException("This link uses a newer WynnBuilder data version");
        }

        List<String> warnings = new ArrayList<>();
        Map<String, String> equipment = new LinkedHashMap<>();
        for (Map.Entry<WynnBuilderBuild.EquipmentSlot, WynnBuilderBuild.Equipment> entry
                : decoded.equipment().entrySet()) {
            String slot = entry.getKey().name();
            switch (entry.getValue()) {
                case WynnBuilderBuild.Equipment.None ignored -> { }
                case WynnBuilderBuild.Equipment.Normal normal -> {
                    String name = index.name(normal.itemId());
                    if (name == null || ItemDatabase.getInstance().getItem(name) == null) {
                        warnings.add("Skipped unknown item ID " + normal.itemId() + " in " + readable(slot));
                    } else {
                        equipment.put(slot, name);
                    }
                }
                case WynnBuilderBuild.Equipment.Crafted crafted -> {
                    try {
                        equipment.put(slot, CraftedItemCodec.encode(crafted.craft()));
                    } catch (IllegalArgumentException exception) {
                        warnings.add("Skipped crafted item in " + readable(slot) + ": " + exception.getMessage());
                    }
                }
                case WynnBuilderBuild.Equipment.Custom ignored ->
                        warnings.add("Skipped unsupported custom item in " + readable(slot));
            }
        }

        Map<String, String> powders = new LinkedHashMap<>();
        decoded.powders().forEach((slot, values) -> {
            StringBuilder code = new StringBuilder();
            for (WynnBuilderBuild.Powder powder : values) {
                if (powder.element() >= 0 && powder.element() < POWDERS.length
                        && powder.tier() >= 1 && powder.tier() <= 7) {
                    code.append(POWDERS[powder.element()]).append(powder.tier());
                } else {
                    warnings.add("Skipped invalid powder in " + readable(slot.name()));
                }
            }
            if (!code.isEmpty()) powders.put(slot.name(), code.toString());
        });

        WynnItem weapon = equipment.containsKey("WEAPON")
                ? ItemDatabase.getInstance().getItem(equipment.get("WEAPON")) : null;
        CharacterClass abilityClass = weapon == null
                ? CharacterClass.ARCHER : CharacterClass.fromWeaponSubtype(weapon.subType());
        if (weapon == null && (!decoded.aspects().isEmpty() || decoded.abilityTree().bitLength() > 0)) {
            warnings.add("Could not determine the class; class-specific data may be skipped");
        }

        Map<String, String> tomes = new LinkedHashMap<>();
        List<WynnTome> selectedTomes = new ArrayList<>();
        for (int i = 0; i < Math.min(WynnBuilderTomeSlots.size(), decoded.tomeIds().size()); i++) {
            Integer id = decoded.tomeIds().get(i);
            if (id == null) continue;
            WynnTome tome = TomeDatabase.getInstance().get(id);
            if (tome == null) warnings.add("Skipped unknown tome ID " + id);
            else {
                tomes.put(WynnBuilderTomeSlots.slot(i), tome.displayName());
                selectedTomes.add(tome);
            }
        }

        Map<String, SavedBuild.SavedAspect> aspects = new LinkedHashMap<>();
        for (int i = 0; i < decoded.aspects().size(); i++) {
            WynnBuilderBuild.Aspect encoded = decoded.aspects().get(i);
            if (encoded == null) continue;
            WynnAspect aspect = AspectDatabase.getInstance().get(abilityClass, encoded.id());
            if (aspect == null) warnings.add("Skipped unknown " + abilityClass.displayName() + " aspect ID " + encoded.id());
            else aspects.put("aspect" + (i + 1), new SavedBuild.SavedAspect(
                    aspect.displayName(), Math.clamp(encoded.tier(), 1, aspect.tiers().size())));
        }

        Map<String, Set<String>> trees = new LinkedHashMap<>();
        if (decoded.abilityTree().bitLength() > 0) {
            AbilityTreeDefinition definition = AbilityTreeDatabase.getInstance().getCachedOrLoadAsync(abilityClass);
            if (definition == null) {
                warnings.add("Ability tree is loading; paste the link again to import it");
            } else {
                try {
                    WynnBuilderTreeCodec.DecodeResult tree = WynnBuilderTreeCodec.decodeBuildPayloadBestEffort(
                            definition, decoded.abilityTree().base64(), decoded.abilityTree().bitLength());
                    trees.put(abilityClass.apiName(), new LinkedHashSet<>(tree.selected()));
                    if (!tree.skippedAbilities().isEmpty()) {
                        warnings.add("Skipped incompatible abilities: "
                                + String.join(", ", tree.skippedAbilities()));
                    }
                } catch (IllegalArgumentException exception) {
                    warnings.add("Skipped ability tree: " + exception.getMessage());
                }
            }
        }

        List<BuildCalculator.EquippedItem> equippedItems = new ArrayList<>();
        decoded.equipment().keySet().forEach(slot -> {
            String reference = equipment.get(slot.name());
            if (reference == null) return;
            WynnItem item = ItemDatabase.getInstance().getItem(reference);
            if (item != null) equippedItems.add(new BuildCalculator.EquippedItem(
                    item, powders.getOrDefault(slot.name(), ""), slot == WynnBuilderBuild.EquipmentSlot.WEAPON));
        });
        BuildCalculator.SkillPointPlan automatic = BuildCalculator.optimizeSkillPoints(
                decoded.level(), equippedItems, selectedTomes);
        int[] skills = automatic.assigned().clone();
        Integer[] encodedSkills = decoded.assignedSkillPoints();
        for (int i = 0; i < skills.length; i++) {
            if (encodedSkills[i] != null) skills[i] += encodedSkills[i] - automatic.finalSkills()[i];
        }
        SavedBuild build = new SavedBuild("Imported Build", abilityClass.apiName(), decoded.level(), equipment,
                powders, List.of(), skills, tomes, aspects, Map.of(), trees, 0);
        return new Result(version, build, List.copyOf(warnings));
    }

    private static String readable(String slot) {
        return slot.toLowerCase(java.util.Locale.ROOT).replace('_', ' ');
    }

    public record Result(String dataVersion, SavedBuild build, List<String> warnings) {}
}
