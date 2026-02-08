package julianh06.wynnextras.features.crafting;

import com.wynntils.models.character.type.ClassType;
import com.wynntils.models.elements.type.Skill;
import com.wynntils.models.gear.type.GearAttackSpeed;
import com.wynntils.models.gear.type.GearRequirements;
import com.wynntils.models.ingredients.type.IngredientInfo;
import com.wynntils.models.ingredients.type.IngredientPosition;
import com.wynntils.models.stats.type.DamageType;
import com.wynntils.models.stats.type.StatPossibleValues;
import com.wynntils.models.stats.type.StatType;
import com.wynntils.utils.type.Pair;
import com.wynntils.utils.type.RangedValue;
import julianh06.wynnextras.features.crafting.data.CraftableType;
import julianh06.wynnextras.features.crafting.data.recipes.RecipeLoader;
import julianh06.wynnextras.utils.CraftingUtils;
import org.joml.Vector2d;
import org.joml.Vector2i;
import org.joml.Vector4i;

import java.util.*;

import static julianh06.wynnextras.utils.CraftingUtils.*;

public class Recipe {
    private IngredientInfo[] ingredients;
    private Materials materials;
    private Vector2i level;
    private final CraftableType type;
    private Double[] multipliers;
    private Vector2i dura;
    private Vector2i healthOrDmg;
    private GearAttackSpeed attackSpeed = GearAttackSpeed.NORMAL;

    private static final Double[] tierToMult = {0.0, 1.0, 1.25, 1.4};

    public static class Materials {
        int mat1Tier;
        int mat1Count;
        int mat2Tier;
        int mat2Count;

        public Materials(int mat1Tier, int mat1Count, int mat2Tier, int mat2Count) {
            this.mat1Tier = mat1Tier;
            this.mat1Count = mat1Count;
            this.mat2Tier = mat2Tier;
            this.mat2Count = mat2Count;
        }

        public double getMultiplier() {
            try {
                return (tierToMult[mat1Tier] * mat1Count + tierToMult[mat2Tier] * mat2Count) / (mat1Count + mat2Count);
            } catch (Exception e) {
                return 0;
            }
        }
    }

    public Recipe(Recipe other) {
        this.ingredients = other.ingredients != null ? Arrays.copyOf(other.ingredients, other.ingredients.length) : null;
        this.materials = other.materials;
        this.level = other.level != null ? new Vector2i(other.level) : null;
        this.type = other.type;
        this.multipliers = other.multipliers != null ? Arrays.copyOf(other.multipliers, other.multipliers.length) : null;
        this.dura = other.dura != null ? new Vector2i(other.dura) : null;
        this.healthOrDmg = other.healthOrDmg != null ? new Vector2i(other.healthOrDmg) : null;
        this.attackSpeed = other.attackSpeed;
    }

    public Recipe(CraftableType type) {
        this.type = type;
        this.ingredients = new IngredientInfo[0];

        multipliers = new Double[]{1.0, 1.0, 1.0, 1.0, 1.0, 1.0};

        this.level = null;
        this.dura = null;
        this.healthOrDmg = null;
    }

    public Recipe(String[] ingredients, Materials materials, Vector2i level, CraftableType type) {
        this(
                Arrays.stream(ingredients).map(CraftingUtils::getIng).toArray(IngredientInfo[]::new),
                materials,
                level,
                type
        );
    }

    public Recipe(IngredientInfo[] ingredients, Materials materials, Vector2i level, CraftableType type) {
        this.type = type;
        this.ingredients = ingredients;
        this.materials = materials;
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

    public void setAttackSpeed(GearAttackSpeed attackSpeed) {
        if (attackSpeed.ordinal() < 2 || attackSpeed.ordinal() > 4) return;
        this.attackSpeed = attackSpeed;
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
        durabilityBase = durabilityBase.mul(materials.getMultiplier());
        Vector2d finalDura = durabilityBase.add(durabilityModifier, durabilityModifier);
        return new Vector2i((int) Math.round(finalDura.x), (int) Math.round(finalDura.y));
    }

    public Vector2i getDuration(int modifier) {
        Vector2d base = new Vector2d(this.dura);
        if (!getType().isConsumable()) return null;
        Vector2d finalDuration = base.add(modifier, modifier);
        return new Vector2i((int) finalDuration.x, (int) finalDuration.y);
    }

    public Vector2i getHealth() {
        if (this.healthOrDmg == null) return null;
        Vector2d health = new Vector2d(this.healthOrDmg);
        Vector2d finalHealth = health.mul(materials.getMultiplier());
        return new Vector2i((int) Math.floor(finalHealth.x), (int) Math.floor(finalHealth.y));
    }

    public Map<DamageType, Vector4i> getDamage() {
        if (!getType().isWeapon()) return null;
        HashMap<DamageType, Vector4i> result = new HashMap<>();

        double ratio = 2.05;
        switch (attackSpeed) {
            case SLOW -> ratio /= 1.5;
            case NORMAL -> ratio = 1;
            case FAST -> ratio /= 2.5;
        }
        double multiplier = materials.getMultiplier();
        int nDamBaseLow = (int) Math.floor(healthOrDmg.x * multiplier);
        int nDamBaseHigh = (int) Math.floor(healthOrDmg.y * multiplier);
        nDamBaseLow = (int) Math.floor(nDamBaseLow * ratio);
        nDamBaseHigh = (int) Math.floor(nDamBaseHigh * ratio);

        // apply powders here wynntills doesnt count them as being ingredients so i cant be bothered to do it rn

        int low1 = (int) Math.floor(nDamBaseLow * 0.9);
        int low2 = (int) Math.floor(nDamBaseLow * 1.1);
        int high1 = (int) Math.floor(nDamBaseHigh * 0.9);
        int high2 = (int) Math.floor(nDamBaseHigh * 1.1);
        result.put(DamageType.NEUTRAL, new Vector4i(low1, low2, high1, high2));

        return result;
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
        if (this.materials == null) {
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

        Vector2i health = this.getHealth();

        // consumables have low duration and heal you when crafted with no ings i dont have the heal numbers
        if (getType().isConsumable()) {
            boolean basic = true;
            for (IngredientInfo ing : ingredients) {
                if (ing != null && ing.variableStats() != null && !ing.variableStats().isEmpty()) {
                    basic = false;
                    break;
                }
            }
            if (basic) {
                RecipeLoader.RecipeData data = RecipeLoader.getRecipe(getType(), getLevel());
                if (data == null) return null;
                return new CraftingResult(
                        new Recipe(this),
                        this.getType(),
                        new ArrayList<>(),
                        new GearRequirements(this.level.y, Optional.of(ClassType.NONE), new ArrayList<>(), Optional.empty()),
                        health == null ? null : RangedValue.of(health.x, health.y),
                        null,
                        null,
                        getBaseCharges(),
                        RangedValue.of(data.basicDuration().x, data.basicDuration().y),
                        getDamage()
                );
            }
        }
        if (getType().isConsumable()) health = null;
        if (Arrays.stream(ingredients).allMatch(Objects::isNull) && getType().isConsumable()) return null;

        int powderSlots = getBasePowderSlots();
        int charges = getBaseCharges();

        int durationModifier = 0;
        int durabilityModifier = 0;

        List<Pair<Skill, Double>> requirements = null;
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
        GearRequirements gearReqs;

        if (requirements != null) {
            List<Pair<Skill, Integer>> skillReqs = requirements.stream()
                    .map(pair -> new Pair<>(pair.a(), (int) Math.copySign(Math.round(Math.abs(pair.b())), pair.b())))
                    .toList();
            gearReqs = new GearRequirements(this.getLevel().y, Optional.of(getType().getClassType()), skillReqs, Optional.empty());
        } else
            gearReqs = new GearRequirements(this.getLevel().y, Optional.of(getType().getClassType()), new ArrayList<>(), Optional.empty());

        Vector2i durability = this.getDurability(durabilityModifier);
        Vector2i duration = getDuration(durationModifier);

        return new CraftingResult(
                new Recipe(this),
                this.getType(),
                possibleValues.stream().filter(value -> value.range().low() != 0 || value.range().high() != 0).toList(),
                gearReqs,
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
