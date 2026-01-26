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
import org.joml.Vector2d;
import org.joml.Vector2i;
import org.joml.Vector4i;

import java.util.*;

import static julianh06.wynnextras.utils.CraftingUtils.*;

public class Recipe {
    private final IngredientInfo[] ingredients;
    private final int sMaterialTier;
    private final int lMaterialTier;
    private final Vector2i level;
    private final CraftableType station;
    private final Double[] multipliers;
    private boolean isValid;
    private final Vector2i durability;
    private final Vector2i consuDuration;
    private final Vector2i cookingDuration;
    private final Vector2i health;

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
        this.multipliers = updateMultipliers();
        this.isValid = checkValidity();
        Constants.RecipeRange ranges = Constants.getByLevel(this.level);
        if (ranges == null) {
            isValid = false;
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

    public boolean isValid() {
        return isValid;
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
        else if (getType() == CraftableType.POTION || getType() == CraftableType.SCROLL) base = new Vector2d(consuDuration);
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

    public double getMaterialMultiplier() {
        // each digit can be read as the tier of the respective material
        return switch (lMaterialTier * 10 + sMaterialTier) {
            case 11 -> 1;
            case 12 -> switch (getType()) {
                case NECKLACE -> 1.0625;
                case SCROLL, RING -> 1.125;
                default -> 13.0 / 12.0;
            };
            case 13 -> switch (getType()) {
                case NECKLACE -> 1.1;
                case SCROLL, RING -> 1.2;
                default -> 17.0 / 15.0;
            };
            case 21 -> switch (getType()) {
                case NECKLACE -> 1.1875;
                case SCROLL, RING -> 1.125;
                default -> 5.0 / 3.0;
            };
            case 22 -> 1.25;
            case 23 -> switch (getType()) {
                case NECKLACE -> 1.2875;
                case SCROLL, RING -> 1.325;
                default -> 1.3;
            };
            case 31 -> switch (getType()) {
                case NECKLACE -> 1.3;
                case SCROLL, RING -> 1.2;
                default -> 19.0 / 15.0;
            };
            case 32 -> switch (getType()) {
                case NECKLACE -> 1.3625;
                case SCROLL, RING -> 1.325;
                default -> 1.35;
            };
            case 33 -> 1.4;
            default -> {
                System.out.println("invalid Material tier");
                yield 0;
            }
        };
    }

    public Double[] getMultipliers() {
        return multipliers;
    }

    public Double[] updateMultipliers() {
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

        return multipliers;
    }

    private boolean checkValidity() {
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
        if (!this.isValid()) return null;

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
                    StatPossibleValues idData = new StatPossibleValues(entry.key(), entry.value(), entry.value().high(), true);
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
        } else requirements = new GearRequirements(this.getLevel().y, null, new ArrayList<>(), null);

        Vector2i durability = this.getDurability(durabilityModifier);
        Vector2i health = this.getHealth();
        Vector2i duration = getDuration(durationModifier);

        return new CraftingResult(
                this.getType(),
                possibleValues,
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
