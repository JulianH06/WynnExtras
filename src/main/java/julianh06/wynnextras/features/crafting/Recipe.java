package julianh06.wynnextras.features.crafting;

import com.wynntils.models.gear.type.GearRequirements;
import com.wynntils.models.ingredients.type.IngredientInfo;
import com.wynntils.models.ingredients.type.IngredientPosition;
import com.wynntils.models.stats.type.DamageType;
import com.wynntils.models.stats.type.StatPossibleValues;
import com.wynntils.models.stats.type.StatType;
import com.wynntils.utils.type.RangedValue;
import julianh06.wynnextras.features.crafting.data.CraftableType;
import julianh06.wynnextras.features.crafting.data.recipes.RecipeLoader;
import julianh06.wynnextras.utils.CraftingUtils;
import julianh06.wynnextras.utils.Pair;
import org.apache.commons.lang3.tuple.Triple;
import org.joml.Vector2d;
import org.joml.Vector2i;
import org.joml.Vector4i;

import java.util.*;

import static julianh06.wynnextras.utils.CraftingUtils.*;

public class Recipe {
    private IngredientInfo[] ingredients;
    private int sMaterialTier;
    private int lMaterialTier;
    private Vector2i level;
    private final CraftableType type;
    private Double[] multipliers;
    private Vector2i dura;
    private Vector2i healthOrDmg;

    public Recipe(Recipe other) {
        this.ingredients = other.ingredients != null ? Arrays.copyOf(other.ingredients, other.ingredients.length) : null;
        this.sMaterialTier = other.sMaterialTier;
        this.lMaterialTier = other.lMaterialTier;
        this.level = other.level != null ? new Vector2i(other.level) : null;
        this.type = other.type;
        this.multipliers = other.multipliers != null ? Arrays.copyOf(other.multipliers, other.multipliers.length) : null;
        this.dura = other.dura != null ? new Vector2i(other.dura) : null;
        this.healthOrDmg = other.healthOrDmg != null ? new Vector2i(other.healthOrDmg) : null;
    }

    public Recipe(CraftableType type) {
        this.type = type;
        this.ingredients = new IngredientInfo[0];
        this.sMaterialTier = 1;
        this.lMaterialTier = 1;

        multipliers = new Double[]{1.0, 1.0, 1.0, 1.0, 1.0, 1.0};

        this.level = null;
        this.dura = null;
        this.healthOrDmg = null;
    }

    public Recipe(String[] ingredients, int sMaterialTier, int lMaterialTier, Vector2i level, CraftableType type) {
        this(
                Arrays.stream(ingredients).map(CraftingUtils::getIng).toArray(IngredientInfo[]::new),
                sMaterialTier,
                lMaterialTier,
                level,
                type
        );
    }

    public Recipe(IngredientInfo[] ingredients, int sMaterialTier, int lMaterialTier, Vector2i level, CraftableType type) {
        this.type = type;
        this.ingredients = ingredients;
        this.sMaterialTier = sMaterialTier;
        this.lMaterialTier = lMaterialTier;
        this.level = level;
        updateMultipliers();
        RecipeLoader.RecipeData ranges = RecipeLoader.getRecipe(type, level);
        if (ranges == null) {
            dura = null;
            healthOrDmg = null;
            return;
        }
        if (type.hasDurability()) {
            dura = ranges.durability();
        } else if (type.isConsumable()) {
            dura = ranges.duration();
        }
        healthOrDmg = ranges.healthOrDamage();
    }

    public void setIngredients(IngredientInfo[] ingredients) {
        this.ingredients = ingredients;
    }

    public void setFromTriple(Triple<RecipeLoader.RecipeData, Integer, Integer> data) {
        setlMaterialTier(data.getMiddle());
        setsMaterialTier(data.getRight());
        setConstants(data.getLeft());
    }

    public void setsMaterialTier(int sMaterialTier) {
        this.sMaterialTier = sMaterialTier;
    }

    public void setlMaterialTier(int lMaterialTier) {
        this.lMaterialTier = lMaterialTier;
    }

    public void setLevel(Vector2i level) {
        this.level = level;
        RecipeLoader.RecipeData ranges = RecipeLoader.getRecipe(type, level);
        if (ranges == null) {
            System.err.println("cannot set recipe to lvl " + this.level + " no constant found");
            return;
        }
        this.dura = ranges.durability();
        this.healthOrDmg = ranges.healthOrDamage();
    }

    public void setConstants(RecipeLoader.RecipeData data) {
        this.level = data.lvl();

        this.dura = data.durability();
        this.healthOrDmg = data.healthOrDamage();
    }

    public IngredientInfo[] getIngredients() {
        return ingredients;
    }

    public Vector2i getLevel() {
        return level;
    }

    public CraftableType getType() {
        return type;
    }

    public int getBasePowderSlots() {
        return getBaseCharges();
    }

    public int getBaseCharges() {
        // 30 and 70 are technically 1 & 2 respectively
        if (level.y <= 29) return 1;
        else if (level.y <= 69) return 2;
        else return 3; // TODO fruma
    }

    public Vector2i getDurability(int durabilityModifier) {
        if (dura == null || !getType().hasDurability()) return null;

        Vector2d durabilityBase = new Vector2d(this.dura);
        durabilityBase = durabilityBase.mul(getMaterialMultiplier());
        Vector2d finalDura = durabilityBase.add(durabilityModifier, durabilityModifier);
        return new Vector2i((int) finalDura.x, (int) finalDura.y);
    }

    public Vector2i getDuration(int modifier) {
        Vector2d base = new Vector2d(this.dura);
        if (!getType().isConsumable()) return null;
        Vector2d finalDuration = base.add(modifier, modifier);
        return new Vector2i((int) finalDuration.x, (int) finalDuration.y);
    }

    public Vector2i getHealth() {
        if (this.healthOrDmg == null || !getType().isArmour()) return null;
        Vector2d health = new Vector2d(this.healthOrDmg);
        Vector2d finalHealth = health.mul(getMaterialMultiplier()).round();
        return new Vector2i((int) finalHealth.x, (int) finalHealth.y);
    }

    public Map<DamageType, Vector4i> getDamage() {
        if (!getType().isWeapon()) return null;
        HashMap<DamageType, Vector4i> result = new HashMap<>();
        return result;

        // TODO how is dmg calculated
        // TODO apply powders in ing slots
    }

    private static final Map<Pair<Integer, CraftableType>, Double> allMaterialMultipliers = new HashMap<>();

    static {
        allMaterialMultipliers.put(new Pair<>(11, CraftableType.NECKLACE), 1.0);
        allMaterialMultipliers.put(new Pair<>(11, CraftableType.SCROLL), 1.0);
        allMaterialMultipliers.put(new Pair<>(11, CraftableType.RING), 1.0);
        allMaterialMultipliers.put(new Pair<>(11, null), 1.0);

        allMaterialMultipliers.put(new Pair<>(12, CraftableType.NECKLACE), 1.0625);
        allMaterialMultipliers.put(new Pair<>(12, CraftableType.SCROLL), 1.125);
        allMaterialMultipliers.put(new Pair<>(12, CraftableType.RING), 1.125);
        allMaterialMultipliers.put(new Pair<>(12, null), 13.0 / 12.0);

        allMaterialMultipliers.put(new Pair<>(13, CraftableType.NECKLACE), 1.1);
        allMaterialMultipliers.put(new Pair<>(13, CraftableType.SCROLL), 1.2);
        allMaterialMultipliers.put(new Pair<>(13, CraftableType.RING), 1.2);
        allMaterialMultipliers.put(new Pair<>(13, null), 17.0 / 15.0);

        allMaterialMultipliers.put(new Pair<>(21, CraftableType.NECKLACE), 1.1875);
        allMaterialMultipliers.put(new Pair<>(21, CraftableType.SCROLL), 1.125);
        allMaterialMultipliers.put(new Pair<>(21, CraftableType.RING), 1.125);
        allMaterialMultipliers.put(new Pair<>(21, null), 5.0 / 3.0);

        allMaterialMultipliers.put(new Pair<>(22, CraftableType.NECKLACE), 1.25);
        allMaterialMultipliers.put(new Pair<>(22, CraftableType.SCROLL), 1.25);
        allMaterialMultipliers.put(new Pair<>(22, CraftableType.RING), 1.25);
        allMaterialMultipliers.put(new Pair<>(22, null), 1.25);

        allMaterialMultipliers.put(new Pair<>(23, CraftableType.NECKLACE), 1.2875);
        allMaterialMultipliers.put(new Pair<>(23, CraftableType.SCROLL), 1.325);
        allMaterialMultipliers.put(new Pair<>(23, CraftableType.RING), 1.325);
        allMaterialMultipliers.put(new Pair<>(23, null), 1.3);

        allMaterialMultipliers.put(new Pair<>(31, CraftableType.NECKLACE), 1.3);
        allMaterialMultipliers.put(new Pair<>(31, CraftableType.SCROLL), 1.2);
        allMaterialMultipliers.put(new Pair<>(31, CraftableType.RING), 1.2);
        allMaterialMultipliers.put(new Pair<>(31, null), 19.0 / 15.0);

        allMaterialMultipliers.put(new Pair<>(32, CraftableType.NECKLACE), 1.3625);
        allMaterialMultipliers.put(new Pair<>(32, CraftableType.SCROLL), 1.325);
        allMaterialMultipliers.put(new Pair<>(32, CraftableType.RING), 1.325);
        allMaterialMultipliers.put(new Pair<>(32, null), 1.35);

        allMaterialMultipliers.put(new Pair<>(33, CraftableType.NECKLACE), 1.4);
        allMaterialMultipliers.put(new Pair<>(33, CraftableType.SCROLL), 1.4);
        allMaterialMultipliers.put(new Pair<>(33, CraftableType.RING), 1.4);
        allMaterialMultipliers.put(new Pair<>(33, null), 1.4);
    }

    public double getMaterialMultiplier() {
        Set<CraftableType> specialTypes = Set.of(CraftableType.NECKLACE, CraftableType.SCROLL, CraftableType.RING);
        CraftableType localType = specialTypes.contains(getType()) ? getType() : null;
        int value = lMaterialTier * 10 + sMaterialTier;
        return allMaterialMultipliers.get(new Pair<>(value, localType));
    }

    public Double[] getMultipliers() {
        return multipliers;
    }

    public void updateMultipliers() {
        Double[] multipliers = new Double[6];
        Arrays.fill(multipliers, 1.0);

        for (int slot = 0; slot < ingredients.length; slot++) {
            if (ingredients[slot] == null) continue;
            Map<IngredientPosition, Integer> modifierMap = ingredients[slot].positionModifiers();
            Double[] modArray = new IngredientPositionModifiers(modifierMap).getMultipliers(slot);
            for (int i = 0; i < multipliers.length; i++) {
                multipliers[i] += modArray[i];
            }
        }

        for (int i = 0; i < multipliers.length; i++) {
            multipliers[i] = Math.round(multipliers[i] * 100.0) / 100.0;
        }

        this.multipliers = multipliers;
    }

    private boolean checkValidity() {
        if (sMaterialTier < 1 || sMaterialTier > 3 || lMaterialTier < 1 || lMaterialTier > 3) {
            System.out.println("Invalid material tier");
            return false;
        }

        if (dura == null) {
            System.out.println("Invalid dura");
            return false;
        }

        if (ingredients.length != 6) {
            System.err.println("cannot create recipe without 6 ingredients");
            return false;
        }
        for (IngredientInfo ing : ingredients) {
            if (ing == null) {
                continue;
            }

            if (!ing.professions().contains(getType().getStation())) {
                System.out.println("cannot use " + ing.name() + " for " + getType().getStation());
                return false;
            }

            int levelReq = ing.level();
            if (levelReq > getLevel().y) {
                System.out.println("cannot use ingredient " + ing.name() + " for lvl range "
                        + getLevel().x + "-" + getLevel().y
                        + " requires lvl " + levelReq);
                return false;
            }
        }
        return true;
    }

    public CraftingResult craft() {
        if (!checkValidity()) return null;

        // consumables have low duration and heal you when crafted with no ings i dont have the heal numbers
        if (Arrays.stream(ingredients).allMatch(Objects::isNull) && getType().isConsumable()) return null;

        int powderSlots = getBasePowderSlots();
        int charges = getBaseCharges();

        int durationModifier = 0;
        int durabilityModifier = 0;

        GearRequirements requirements = null;
        List<StatPossibleValues> possibleValues = new ArrayList<>();

        Double[] metaMultipliers = this.getMultipliers();
        for (int i = 0; i < this.getIngredients().length; i++) {
            IngredientInfo ing = this.getIngredients()[i];
            if (ing == null) {
                continue;
            }

            // add to running total of ids
            double multiplier = metaMultipliers[i];
            if (ing.variableStats() != null) {
                for (com.wynntils.utils.type.Pair<StatType, RangedValue> entry : ing.variableStats()) {
                    StatPossibleValues idData = new StatPossibleValues(entry.key(), entry.value(), 0, true);
                    StatPossibleValues scaled = applyMultiplier(idData, multiplier);
                    addIds(possibleValues, scaled);
                }
            }

            requirements = addGearRequirements(requirements, ing.skillRequirements(), multiplier);
            durabilityModifier += ing.durabilityModifier();
            charges += ing.charges();
            durationModifier += ing.duration();

        }

        if (requirements != null) {
            requirements = new GearRequirements(this.getLevel().y, requirements.classType(), requirements.skills(), requirements.quest());
        } else requirements = new GearRequirements(this.getLevel().y, Optional.empty(), new ArrayList<>(), Optional.empty());

        Vector2i durability = this.getDurability(durabilityModifier);
        Vector2i health = this.getHealth();
        Vector2i duration = getDuration(durationModifier);

        return new CraftingResult(
                new Recipe(this),
                this.getType(),
                possibleValues.stream().filter(value -> value.range().low() != 0 || value.range().high() != 0).toList(),
                requirements,
                health == null ? null : RangedValue.of(health.x, health.y),
                durability == null ? null : RangedValue.of(durability.x, durability.y),
                powderSlots,
                charges,
                duration == null ? null : RangedValue.of(duration.x, duration.y),
                getDamage()
        );
    }

    @Override
    public String toString() {
        return Arrays.toString(ingredients);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof Recipe other) {
            return Arrays.equals(this.ingredients, other.ingredients);
        }
        return false;
    }

    @Override
    public int hashCode() {
        return Arrays.hashCode(ingredients);
    }

}
