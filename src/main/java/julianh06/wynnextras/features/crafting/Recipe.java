package julianh06.wynnextras.features.crafting;

import com.wynntils.models.gear.type.GearRequirements;
import com.wynntils.models.ingredients.type.IngredientInfo;
import com.wynntils.models.ingredients.type.IngredientPosition;
import com.wynntils.models.stats.type.DamageType;
import com.wynntils.models.stats.type.StatPossibleValues;
import com.wynntils.models.stats.type.StatType;
import com.wynntils.utils.type.RangedValue;
import julianh06.wynnextras.features.crafting.data.Constants;
import julianh06.wynnextras.features.crafting.data.CraftableType;
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
    private final CraftableType station;
    private Double[] multipliers;
    private Vector2i durability;
    private Vector2i consuDuration;
    private Vector2i cookingDuration;
    private Vector2i health;

    public Recipe(Recipe other) {
        this.ingredients = other.ingredients != null ? Arrays.copyOf(other.ingredients, other.ingredients.length) : null;
        this.sMaterialTier = other.sMaterialTier;
        this.lMaterialTier = other.lMaterialTier;
        this.level = other.level != null ? new Vector2i(other.level) : null;
        this.station = other.station;
        this.multipliers = other.multipliers != null ? Arrays.copyOf(other.multipliers, other.multipliers.length) : null;
        this.durability = other.durability != null ? new Vector2i(other.durability) : null;
        this.consuDuration = other.consuDuration != null ? new Vector2i(other.consuDuration) : null;
        this.cookingDuration = other.cookingDuration != null ? new Vector2i(other.cookingDuration) : null;
        this.health = other.health != null ? new Vector2i(other.health) : null;
    }

    public Recipe(CraftableType station) {
        this.station = station;
        this.ingredients = new IngredientInfo[0];
        this.sMaterialTier = 1;
        this.lMaterialTier = 1;

        multipliers = new Double[]{1.0, 1.0, 1.0, 1.0, 1.0, 1.0};

        this.level = null;
        this.durability = null;
        this.consuDuration = null;
        this.cookingDuration = null;
        this.health = null;
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
        this.station = type;
        this.ingredients = ingredients;
        this.sMaterialTier = sMaterialTier;
        this.lMaterialTier = lMaterialTier;
        this.level = new Vector2i(level.x(), Math.max(level.y(), 105)); // TODO fruma
        updateMultipliers();
        Constants.RecipeRange ranges = Constants.getByLevel(this.level);
        if (ranges == null) {
            durability = null;
            consuDuration = null;
            cookingDuration = null;
            health = null;
            return;
        }
        durability = ranges.durability();
        consuDuration = ranges.consuDuration();
        cookingDuration = ranges.cookingDuration();
        health = ranges.health();
    }

    public void setIngredients(IngredientInfo[] ingredients) {
        this.ingredients = ingredients;
    }

    public void setFromTriple(Triple<Constants.RecipeRange, Integer, Integer> data) {
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
        Constants.RecipeRange ranges = Constants.getByLevel(this.level);
        if (ranges == null) {
            System.err.println("cannot set recipe to level " + this.level + " no constant found");
            return;
        }
        this.durability = ranges.durability();
        this.consuDuration = ranges.consuDuration();
        this.cookingDuration = ranges.cookingDuration();
        this.health = ranges.health();
    }

    public void setConstants(Constants.RecipeRange data) {
        this.level = data.level();
        this.durability = data.durability();
        this.consuDuration = data.consuDuration();
        this.cookingDuration = data.cookingDuration();
        this.health = data.health();
    }

    public IngredientInfo[] getIngredients() {
        return ingredients;
    }

    public Vector2i getLevel() {
        return level;
    }

    public CraftableType getType() {
        return station;
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
        if (durability == null || !getType().hasDurability()) return null;

        Vector2d durabilityBase = new Vector2d(this.durability);
        durabilityBase = durabilityBase.mul(getMaterialMultiplier());
        Vector2d finalDura = durabilityBase.add(durabilityModifier, durabilityModifier);
        return new Vector2i((int) finalDura.x, (int) finalDura.y);
    }

    public Vector2i getDuration(int modifier) {
        Vector2d base;
        if (getType() == CraftableType.FOOD) base = new Vector2d(cookingDuration);
        else if (getType() == CraftableType.POTION || getType() == CraftableType.SCROLL)
            base = new Vector2d(consuDuration);
        else return null;
        Vector2d finalDuration = base.add(modifier, modifier);
        return new Vector2i((int) finalDuration.x, (int) finalDuration.y);
    }

    public Vector2i getHealth() {
        if (this.health == null || !getType().isArmour()) return null;
        // in-game is three quarters of the wiki idk if it was nerfed or what but i dont wanna change every entry in constants
        Vector2d health = new Vector2d(this.health).mul(3.0 / 4.0);
        Vector2d finalHealth = health.mul(getMaterialMultiplier()).round();
        return new Vector2i((int) finalHealth.x, (int) finalHealth.y);
    }

    public Map<DamageType, Vector4i> getDamage() {
        if (!getType().isWeapon()) return null;
        HashMap<DamageType, Vector4i> result = new HashMap<>();
        Pair<Vector2i, Vector2i> damage = Constants.getDamage(getType(), level);
        Vector4i finalDmg = new Vector4i();
        finalDmg.x = (int) Math.round(damage.getFirst().x * getMaterialMultiplier());
        finalDmg.y = (int) Math.round(damage.getFirst().y * getMaterialMultiplier());
        finalDmg.z = (int) Math.round(damage.getSecond().x * getMaterialMultiplier());
        finalDmg.w = (int) Math.round(damage.getSecond().y * getMaterialMultiplier());
        result.put(DamageType.NEUTRAL, finalDmg);
        return result;
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

        if (durability == null && cookingDuration == null && consuDuration == null) {
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
                System.out.println("cannot use ingredient " + ing.name() + " for level range "
                        + getLevel().x + "-" + getLevel().y
                        + " requires level " + levelReq);
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
