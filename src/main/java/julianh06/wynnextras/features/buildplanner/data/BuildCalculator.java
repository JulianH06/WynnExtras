package julianh06.wynnextras.features.buildplanner.data;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public final class BuildCalculator {
    private static final String[] ELEMENT_KEYS = {"neutral", "earth", "thunder", "water", "fire", "air"};
    private static final String[] DAMAGE_KEYS = {"nDam", "eDam", "tDam", "wDam", "fDam", "aDam"};
    private static final String[] DEFENSE_KEYS = {"eDef", "tDef", "wDef", "fDef", "aDef"};
    private static final String[] DEFENSE_ID_KEYS = {
            "earthDefence", "thunderDefence", "waterDefence", "fireDefence", "airDefence"
    };
    private static final String[] SKILL_KEYS = {"str", "dex", "int", "def", "agi"};
    private static final String[] RAW_SKILL_KEYS = {
            "rawStrength", "rawDexterity", "rawIntelligence", "rawDefence", "rawAgility"
    };
    private static final String[] REQUIREMENT_KEYS = {"strReq", "dexReq", "intReq", "defReq", "agiReq"};
    private static final double[] ATTACK_SPEED_MULTIPLIERS = {0.51, 0.83, 1.5, 2.05, 2.5, 3.1, 4.3};
    private static final Powder[][] POWDERS = {
            {
                    new Powder(4, 5, 17, 2, 1, 5), new Powder(6, 7, 21, 5, 2, 10),
                    new Powder(7, 9, 25, 9, 3, 20), new Powder(8, 9, 31, 14, 4, 30),
                    new Powder(9, 11, 38, 22, 7, 45), new Powder(11, 12, 46, 29, 7, 60),
                    new Powder(12, 14, 52, 37, 12, 75)
            },
            {
                    new Powder(1, 8, 9, 2, 1, 5), new Powder(1, 12, 11, 4, 1, 10),
                    new Powder(2, 14, 13, 8, 2, 20), new Powder(2, 15, 17, 13, 3, 30),
                    new Powder(3, 17, 22, 20, 5, 45), new Powder(4, 19, 28, 28, 6, 60),
                    new Powder(5, 21, 32, 36, 11, 75)
            },
            {
                    new Powder(3, 4, 13, 3, 1, 5), new Powder(5, 6, 15, 6, 1, 10),
                    new Powder(6, 8, 17, 11, 3, 20), new Powder(7, 8, 21, 16, 4, 30),
                    new Powder(8, 10, 26, 23, 6, 45), new Powder(10, 13, 32, 32, 10, 60),
                    new Powder(11, 15, 38, 40, 15, 75)
            },
            {
                    new Powder(2, 5, 14, 3, 1, 5), new Powder(4, 7, 16, 6, 1, 10),
                    new Powder(5, 9, 19, 10, 2, 20), new Powder(6, 9, 24, 15, 3, 30),
                    new Powder(7, 11, 30, 22, 5, 45), new Powder(9, 14, 37, 31, 9, 60),
                    new Powder(10, 16, 44, 39, 14, 75)
            },
            {
                    new Powder(2, 6, 11, 3, 1, 5), new Powder(3, 9, 14, 6, 2, 10),
                    new Powder(4, 11, 17, 10, 3, 20), new Powder(5, 11, 22, 16, 5, 30),
                    new Powder(7, 12, 28, 23, 7, 45), new Powder(8, 15, 35, 30, 8, 60),
                    new Powder(9, 17, 42, 38, 13, 75)
            }
    };

    private BuildCalculator() {
    }

    public static Result calculate(int level, int[] assignedSkills, List<EquippedItem> equipment) {
        return calculate(level, assignedSkills, equipment, List.of());
    }

    public static Result calculate(
            int level,
            int[] assignedSkills,
            List<EquippedItem> equipment,
            List<WynnTome> tomes
    ) {
        int safeLevel = Math.max(1, Math.min(121, level));
        int[] assigned = Arrays.copyOf(assignedSkills, 5);
        int[] skills = Arrays.copyOf(assigned, 5);
        int[] rawDefenses = new int[5];
        int health = levelToHp(safeLevel);
        Map<String, Integer> ids = new LinkedHashMap<>();
        WynnItem weapon = null;
        String problem = "";

        for (EquippedItem equipped : equipment) {
            WynnItem item = equipped.item();
            if (item == null) {
                continue;
            }
            if (equipped.weapon()) {
                weapon = item;
            }
            health += item.stat("hp");
            for (int i = 0; i < rawDefenses.length; i++) {
                rawDefenses[i] += item.stat(DEFENSE_KEYS[i]);
            }
            for (Map.Entry<String, Identification> entry : item.identifications().entrySet()) {
                ids.merge(entry.getKey(), entry.getValue().max(), Integer::sum);
            }
            if (!equipped.weapon()) {
                for (PowderSelection selection : parsePowders(equipped.powders())) {
                    Powder powder = POWDERS[selection.element()][selection.tier() - 1];
                    health += powder.health();
                    rawDefenses[selection.element()] += powder.defensePlus();
                    rawDefenses[(selection.element() + 4) % 5] -= powder.defenseMinus();
                }
            }
            int requiredLevel = item.stat(item.isCrafted() ? "lvlLow" : "lvl");
            if (requiredLevel > safeLevel && problem.isEmpty()) {
                problem = item.displayName() + " requires level " + requiredLevel;
            }
        }

        for (WynnTome tome : tomes) {
            if (tome == null) {
                continue;
            }
            for (Map.Entry<String, Integer> entry : tome.identifications().entrySet()) {
                ids.merge(entry.getKey(), entry.getValue(), Integer::sum);
            }
            if (tome.level() > safeLevel && problem.isEmpty()) {
                problem = tome.displayName() + " requires level " + tome.level();
            }
        }

        for (int i = 0; i < skills.length; i++) {
            skills[i] += ids.getOrDefault(SKILL_KEYS[i], 0)
                    + ids.getOrDefault(RAW_SKILL_KEYS[i], 0);
        }
        health = Math.max(5, health + ids.getOrDefault("rawHealth", 0));

        if (!hasValidEquipOrder(assigned, equipment, tomeSkillBonuses(tomes)) && problem.isEmpty()) {
            problem = "No valid equip order for the skill requirements";
        }
        int assignedTotal = Arrays.stream(assigned).sum();
        if (Arrays.stream(assigned).anyMatch(value -> value > 100) && problem.isEmpty()) {
            problem = "At most 100 points can be assigned to one skill";
        } else if (assignedTotal > levelToSkillPoints(safeLevel) && problem.isEmpty()) {
            problem = "Too many assigned skill points";
        }

        double[] defenses = new double[5];
        int rainbowDefense = ids.getOrDefault("elementalDefence", 0);
        for (int i = 0; i < defenses.length; i++) {
            double percent = (ids.getOrDefault(DEFENSE_ID_KEYS[i], 0) + rainbowDefense) / 100.0;
            defenses[i] = rawDefenses[i] * (1.0 + percent);
        }

        double defense = skillPercentage(skills[3]) * 0.867;
        double agility = skillPercentage(skills[4]) * 0.951;
        double classFactor = 2.0 - classDefense(weapon);
        double noAgiTaken = (1.0 - defense) * classFactor;
        double withAgiTaken = (0.1 * agility + (1.0 - agility) * (1.0 - defense)) * classFactor;
        double effectiveHpNoAgility = health / Math.max(0.0001, noAgiTaken);
        double effectiveHp = health / Math.max(0.0001, withAgiTaken);

        double rawHealthRegen = ids.getOrDefault("healthRegenRaw", 0);
        double healthRegenPercent = ids.getOrDefault("healthRegen", 0) / 100.0;
        double healthRegen = rawHealthRegen >= 0
                ? Math.max(0, rawHealthRegen * (1.0 + healthRegenPercent))
                : Math.min(0, rawHealthRegen * (1.0 - healthRegenPercent));

        DamageRange[] weaponDamage = weaponDamage(weapon);
        if (weapon != null) {
            String powders = equipment.stream()
                    .filter(EquippedItem::weapon)
                    .map(EquippedItem::powders)
                    .findFirst()
                    .orElse("");
            applyWeaponPowders(weaponDamage, parsePowders(powders), weapon);
        }
        int baseAttackTier = attackSpeedTier(weapon == null ? "" : weapon.attackSpeed());
        int effectiveAttackTier = clamp(baseAttackTier + ids.getOrDefault("rawAttackSpeed", 0), 0, 6);

        Result base = new Result(
                health, effectiveHp, effectiveHpNoAgility, healthRegen,
                skills, defenses, ids, weaponDamage,
                baseAttackTier, effectiveAttackTier, 0,
                null, null, problem.isEmpty(), problem);
        DamageResult melee = calculateDamage(base, SpellPart.mainAttack());
        double critChance = skillPercentage(skills[1]);
        double averageHit = melee.normal().average() * (1.0 - critChance)
                + melee.critical().average() * critChance;
        return new Result(
                health, effectiveHp, effectiveHpNoAgility, healthRegen,
                skills, defenses, ids, weaponDamage,
                baseAttackTier, effectiveAttackTier,
                averageHit * ATTACK_SPEED_MULTIPLIERS[effectiveAttackTier],
                melee.normal(), melee.critical(), problem.isEmpty(), problem);
    }

    public static DamageResult calculateDamage(Result build, SpellPart part) {
        DamageRange[] source = build.weaponDamage();
        double sourceMin = Arrays.stream(source).mapToDouble(DamageRange::min).sum();
        double sourceMax = Arrays.stream(source).mapToDouble(DamageRange::max).sum();
        DamageRange[] converted = new DamageRange[6];
        double neutralConversion = part.multipliers()[0] / 100.0;
        for (int i = 0; i < converted.length; i++) {
            converted[i] = source[i].scale(neutralConversion);
        }
        for (int i = 1; i < converted.length; i++) {
            double conversion = part.multipliers()[i] / 100.0;
            converted[i] = converted[i].add(new DamageRange(sourceMin * conversion, sourceMax * conversion));
        }
        if (part.useAttackSpeed()) {
            double attackSpeed = ATTACK_SPEED_MULTIPLIERS[build.baseAttackTier()];
            for (int i = 0; i < converted.length; i++) {
                converted[i] = converted[i].scale(attackSpeed);
            }
        }

        Map<String, Integer> ids = build.ids();
        boolean melee = part.melee();
        String mode = melee ? "MainAttack" : "Spell";
        int genericPercent = ids.getOrDefault(melee ? "mainAttackDamage" : "spellDamage", 0)
                + ids.getOrDefault("damage", 0);
        int rainbowPercent = ids.getOrDefault("elemental" + mode + "Damage", 0)
                + ids.getOrDefault("elementalDamage", 0);
        int genericRaw = ids.getOrDefault("raw" + mode + "Damage", 0)
                + ids.getOrDefault("rawDamage", 0);
        int rainbowRaw = ids.getOrDefault("rawElemental" + mode + "Damage", 0)
                + ids.getOrDefault("rawElementalDamage", 0);
        double totalConversion = Arrays.stream(part.multipliers()).sum() / 100.0;
        double prePercentTotal = Arrays.stream(converted).mapToDouble(DamageRange::average).sum();
        double elementalPrePercent = Arrays.stream(converted, 1, converted.length)
                .mapToDouble(DamageRange::average).sum();

        DamageRange total = DamageRange.ZERO;
        for (int i = 0; i < converted.length; i++) {
            String element = ELEMENT_KEYS[i];
            boolean present = converted[i].max() > 0;
            int specificPercent = ids.getOrDefault(element + mode + "Damage", 0)
                    + ids.getOrDefault(element + "Damage", 0);
            double skillElementBonus = i == 0 ? 0 : elementalSkillBonus(build.skills(), i - 1);
            double percent = 1.0 + skillElementBonus + (genericPercent + specificPercent) / 100.0;
            if (i > 0) {
                percent += rainbowPercent / 100.0;
            }
            DamageRange value = converted[i].scale(percent);
            if (present && prePercentTotal > 0) {
                double share = converted[i].average() / prePercentTotal;
                value = value.add(DamageRange.flat(genericRaw * totalConversion * share));
            }
            if (i > 0 && present && elementalPrePercent > 0) {
                double share = converted[i].average() / elementalPrePercent;
                value = value.add(DamageRange.flat(rainbowRaw * totalConversion * share));
            }
            if (present) {
                int specificRaw = ids.getOrDefault("raw" + capitalize(element) + mode + "Damage", 0)
                        + ids.getOrDefault("raw" + capitalize(element) + "Damage", 0);
                value = value.add(DamageRange.flat(specificRaw * totalConversion));
            }
            total = total.add(value.nonNegative());
        }

        if (part.hits() != 1.0) {
            total = total.scale(part.hits());
        }
        if (!part.useStrength()) {
            return new DamageResult(total, total);
        }
        double strength = skillPercentage(build.skills()[0]);
        double criticalBonus = 1.0 + ids.getOrDefault("criticalDamageBonus", 0) / 100.0;
        DamageRange normal = total.scale(1.0 + strength);
        DamageRange critical = total.scale(1.0 + strength + criticalBonus);
        return new DamageResult(normal, critical);
    }

    public static SpellPart rootSpell(String weaponSubtype) {
        String subtype = weaponSubtype == null ? "" : weaponSubtype.toLowerCase(Locale.ROOT);
        return switch (subtype) {
            case "bow" -> new SpellPart("Arrow Bomb", new double[]{140, 0, 0, 0, 20, 0}, 1, false, true, false);
            case "spear" -> new SpellPart("Bash", new double[]{170, 30, 0, 0, 0, 0}, 1, false, true, false);
            case "wand" -> new SpellPart("Meteor", new double[]{330, 70, 0, 0, 0, 0}, 1, false, true, false);
            case "dagger" -> new SpellPart("Spin Attack", new double[]{120, 0, 30, 0, 0, 0}, 1, false, true, false);
            case "relik" -> new SpellPart("Totem Tick DPS", new double[]{6, 0, 0, 0, 0, 6}, 2.5, false, true, false);
            default -> null;
        };
    }

    public static double skillPercentage(int points) {
        if (points <= 0) {
            return 0;
        }
        int capped = Math.min(150, points);
        double ratio = 0.9908;
        return (ratio / (1.0 - ratio) * (1.0 - Math.pow(ratio, capped))) / 100.0;
    }

    public static double skillEffectPercentage(int skillIndex, int points) {
        double multiplier = switch (skillIndex) {
            case 2 -> 0.5D / skillPercentage(150);
            case 3 -> 0.867D;
            case 4 -> 0.951D;
            default -> 1.0D;
        };
        return skillPercentage(points) * multiplier * 100.0D;
    }

    public static int levelToSkillPoints(int level) {
        return Math.min(200, Math.max(0, (level - 1) * 2));
    }

    public static int levelToHp(int level) {
        return Math.max(1, level) * 5 + 5;
    }

    public static double attacksPerSecond(int attackTier) {
        return ATTACK_SPEED_MULTIPLIERS[clamp(attackTier, 0, ATTACK_SPEED_MULTIPLIERS.length - 1)];
    }

    public static int armorHealthWithPowders(WynnItem item, String powderCode) {
        int health = item == null ? 0 : item.stat("hp");
        for (PowderSelection selection : parsePowders(powderCode)) {
            health += POWDERS[selection.element()][selection.tier() - 1].health();
        }
        return health;
    }

    public static double weaponBaseDps(WynnItem weapon, String powderCode) {
        if (weapon == null) {
            return 0;
        }
        DamageRange[] damage = weaponDamage(weapon);
        applyWeaponPowders(damage, parsePowders(powderCode), weapon);
        double averageDamage = Arrays.stream(damage).mapToDouble(DamageRange::average).sum();
        return averageDamage * attacksPerSecond(attackSpeedTier(weapon.attackSpeed()));
    }

    public static SkillPointPlan optimizeSkillPoints(int level, List<EquippedItem> equipment) {
        return optimizeSkillPoints(level, equipment, List.of());
    }

    public static SkillPointPlan optimizeSkillPoints(
            int level, List<EquippedItem> equipment, List<WynnTome> tomes
    ) {
        WynnItem weapon = equipment.stream()
                .filter(EquippedItem::weapon)
                .map(EquippedItem::item)
                .filter(item -> item != null)
                .findFirst()
                .orElse(null);
        List<WynnItem> items = equipment.stream()
                .filter(equipped -> !equipped.weapon())
                .map(EquippedItem::item)
                .filter(item -> item != null)
                .toList();

        SkillPointSearch search = new SkillPointSearch(level, items, weapon);
        search.search(
                new boolean[items.size()], new int[5], tomeSkillBonuses(tomes), new ArrayList<>());
        return search.result();
    }

    private static DamageRange[] weaponDamage(WynnItem weapon) {
        DamageRange[] damage = new DamageRange[6];
        for (int i = 0; i < damage.length; i++) {
            damage[i] = weapon == null
                    ? DamageRange.ZERO
                    : new DamageRange(weapon.stat(DAMAGE_KEYS[i] + "Min"), weapon.stat(DAMAGE_KEYS[i] + "Max"));
        }
        return damage;
    }

    private static boolean hasValidEquipOrder(
            int[] assigned, List<EquippedItem> equipment, int[] permanentBonuses
    ) {
        List<WynnItem> items = equipment.stream()
                .map(EquippedItem::item)
                .filter(item -> item != null)
                .toList();
        if (items.size() > 30) {
            return false;
        }
        int stateCount = 1 << items.size();
        boolean[] reachable = new boolean[stateCount];
        reachable[0] = true;
        int[][] bonuses = new int[items.size()][5];
        for (int itemIndex = 0; itemIndex < items.size(); itemIndex++) {
            WynnItem item = items.get(itemIndex);
            if (item.isCrafted()) {
                continue;
            }
            for (int skill = 0; skill < 5; skill++) {
                Identification normalized = item.identifications().get(SKILL_KEYS[skill]);
                Identification raw = item.identifications().get(RAW_SKILL_KEYS[skill]);
                bonuses[itemIndex][skill] = (normalized == null ? 0 : normalized.max())
                        + (raw == null ? 0 : raw.max());
            }
        }
        for (int state = 0; state < stateCount; state++) {
            if (!reachable[state]) {
                continue;
            }
            int[] current = Arrays.copyOf(assigned, 5);
            for (int skill = 0; skill < current.length; skill++) {
                current[skill] += permanentBonuses[skill];
            }
            for (int itemIndex = 0; itemIndex < items.size(); itemIndex++) {
                if ((state & (1 << itemIndex)) == 0) {
                    continue;
                }
                for (int skill = 0; skill < 5; skill++) {
                    current[skill] += bonuses[itemIndex][skill];
                }
            }
            for (int itemIndex = 0; itemIndex < items.size(); itemIndex++) {
                int bit = 1 << itemIndex;
                if ((state & bit) != 0 || !meetsRequirements(items.get(itemIndex), current)) {
                    continue;
                }
                reachable[state | bit] = true;
            }
        }
        return reachable[stateCount - 1];
    }

    private static int[] tomeSkillBonuses(List<WynnTome> tomes) {
        int[] bonuses = new int[5];
        for (WynnTome tome : tomes) {
            if (tome == null) {
                continue;
            }
            for (int skill = 0; skill < bonuses.length; skill++) {
                bonuses[skill] += tome.identifications().getOrDefault(SKILL_KEYS[skill], 0)
                        + tome.identifications().getOrDefault(RAW_SKILL_KEYS[skill], 0);
            }
        }
        return bonuses;
    }

    private static boolean meetsRequirements(WynnItem item, int[] skills) {
        for (int skill = 0; skill < skills.length; skill++) {
            if (item.stat(REQUIREMENT_KEYS[skill]) > 0 && item.stat(REQUIREMENT_KEYS[skill]) > skills[skill]) {
                return false;
            }
        }
        return true;
    }

    private static int[] itemSkillBonuses(WynnItem item) {
        int[] bonuses = new int[5];
        for (int skill = 0; skill < bonuses.length; skill++) {
            Identification normalized = item.identifications().get(SKILL_KEYS[skill]);
            Identification raw = item.identifications().get(RAW_SKILL_KEYS[skill]);
            bonuses[skill] = (normalized == null ? 0 : normalized.max())
                    + (raw == null ? 0 : raw.max());
        }
        return bonuses;
    }

    private static void assignToFit(int[] assigned, int[] current, WynnItem item) {
        for (int skill = 0; skill < current.length; skill++) {
            int requirement = item.stat(REQUIREMENT_KEYS[skill]);
            if (requirement <= 0) {
                continue;
            }
            int deficit = requirement - current[skill];
            if (deficit > 0) {
                assigned[skill] += deficit;
                current[skill] += deficit;
            }
        }
    }

    private static void applyItemSkills(int[] current, WynnItem item) {
        int[] bonuses = itemSkillBonuses(item);
        for (int skill = 0; skill < current.length; skill++) {
            current[skill] += bonuses[skill];
        }
    }

    private static void keepEquipped(int[] assigned, int[] current, WynnItem item) {
        int[] bonuses = itemSkillBonuses(item);
        for (int skill = 0; skill < current.length; skill++) {
            int requirement = item.stat(REQUIREMENT_KEYS[skill]);
            if (requirement <= 0) {
                continue;
            }
            int requiredFinal = requirement + (item.isCrafted() ? 0 : bonuses[skill]);
            int deficit = requiredFinal - current[skill];
            if (deficit > 0) {
                assigned[skill] += deficit;
                current[skill] += deficit;
            }
        }
    }

    private static final class SkillPointSearch {
        private final int available;
        private final List<WynnItem> items;
        private final WynnItem weapon;
        private int[] bestAssigned;
        private int[] bestFinal;
        private List<WynnItem> bestOrder = List.of();
        private int bestTotal = Integer.MAX_VALUE;
        private boolean bestUnderCap;

        private SkillPointSearch(int level, List<WynnItem> items, WynnItem weapon) {
            this.available = levelToSkillPoints(Math.max(1, Math.min(121, level)));
            this.items = items;
            this.weapon = weapon;
        }

        private void search(
                boolean[] equipped,
                int[] assigned,
                int[] current,
                List<WynnItem> order
        ) {
            if (order.size() == items.size()) {
                evaluate(assigned, current, order);
                return;
            }
            for (int index = 0; index < items.size(); index++) {
                if (equipped[index]) {
                    continue;
                }
                WynnItem item = items.get(index);
                int[] nextAssigned = Arrays.copyOf(assigned, assigned.length);
                int[] nextCurrent = Arrays.copyOf(current, current.length);
                assignToFit(nextAssigned, nextCurrent, item);
                if (!item.isCrafted()) {
                    applyItemSkills(nextCurrent, item);
                }
                equipped[index] = true;
                order.add(item);
                search(equipped, nextAssigned, nextCurrent, order);
                order.remove(order.size() - 1);
                equipped[index] = false;
            }
        }

        private void evaluate(int[] assigned, int[] current, List<WynnItem> order) {
            int[] candidateAssigned = Arrays.copyOf(assigned, assigned.length);
            int[] candidateCurrent = Arrays.copyOf(current, current.length);
            if (weapon != null) {
                assignToFit(candidateAssigned, candidateCurrent, weapon);
            }
            for (WynnItem item : items) {
                keepEquipped(candidateAssigned, candidateCurrent, item);
            }

            int total = Arrays.stream(candidateAssigned).sum();
            boolean underCap = Arrays.stream(candidateAssigned).allMatch(value -> value <= 100);
            if (bestAssigned != null
                    && (bestUnderCap && !underCap
                    || bestUnderCap == underCap && total >= bestTotal)) {
                return;
            }

            int[] finalSkills = Arrays.copyOf(candidateCurrent, candidateCurrent.length);
            for (WynnItem item : items) {
                if (item.isCrafted()) {
                    applyItemSkills(finalSkills, item);
                }
            }
            if (weapon != null) {
                applyItemSkills(finalSkills, weapon);
            }
            bestAssigned = candidateAssigned;
            bestFinal = finalSkills;
            bestOrder = List.copyOf(order);
            bestTotal = total;
            bestUnderCap = underCap;
        }

        private SkillPointPlan result() {
            if (bestAssigned == null) {
                bestAssigned = new int[5];
                bestFinal = new int[5];
                bestTotal = 0;
            }
            String problem = "";
            if (!bestUnderCap) {
                problem = "At most 100 points can be assigned to one skill";
            } else if (bestTotal > available) {
                problem = "Build requires " + bestTotal + " skill points but only "
                        + available + " are available";
            }
            return new SkillPointPlan(
                    bestAssigned, bestFinal, bestOrder, bestTotal,
                    bestUnderCap, bestTotal <= available, problem);
        }
    }

    private static void applyWeaponPowders(DamageRange[] damage, List<PowderSelection> selections, WynnItem weapon) {
        Map<Integer, List<PowderContribution>> groups = new LinkedHashMap<>();
        for (PowderSelection selection : selections) {
            Powder powder = POWDERS[selection.element()][selection.tier() - 1];
            groups.computeIfAbsent(selection.element(), ignored -> new ArrayList<>())
                    .add(new PowderContribution(powder.conversion(), powder.minDamage(), powder.maxDamage()));
        }
        if (weapon != null && weapon.isCrafted()) {
            // Powder-master groups come first; ingredient powders add half their usual bonuses.
            for (int pid : weapon.ingredientPowders()) {
                Powder powder = POWDERS[pid / 7][pid % 7];
                groups.computeIfAbsent(pid / 7, ignored -> new ArrayList<>()).add(new PowderContribution(
                        powder.conversion() / 2.0, powder.minDamage() / 2, powder.maxDamage() / 2));
            }
        }
        for (Map.Entry<Integer, List<PowderContribution>> entry : groups.entrySet()) {
            double conversion = entry.getValue().stream().mapToDouble(PowderContribution::conversion).sum();
            double ratio = Math.min(100, conversion) / 100.0;
            DamageRange converted = damage[0].scale(ratio);
            damage[0] = damage[0].scale(1.0 - ratio);
            int min = entry.getValue().stream().mapToInt(PowderContribution::minDamage).sum();
            int max = entry.getValue().stream().mapToInt(PowderContribution::maxDamage).sum();
            damage[entry.getKey() + 1] = damage[entry.getKey() + 1]
                    .add(converted)
                    .add(new DamageRange(min, max));
        }
    }

    static void addIngredientArmorPowder(Map<String, Integer> stats, int pid) {
        Powder powder = POWDERS[pid / 7][pid % 7];
        stats.merge(DEFENSE_KEYS[pid / 7], powder.defensePlus(), Integer::sum);
        stats.merge(DEFENSE_KEYS[(pid / 7 + 4) % 5], -powder.defenseMinus(), Integer::sum);
    }

    private record PowderContribution(double conversion, int minDamage, int maxDamage) {
    }

    private static List<PowderSelection> parsePowders(String code) {
        if (code == null || code.isBlank()) {
            return List.of();
        }
        String normalized = code.toLowerCase(Locale.ROOT);
        List<PowderSelection> result = new ArrayList<>();
        for (int i = 0; i + 1 < normalized.length(); i += 2) {
            int element = "etwfa".indexOf(normalized.charAt(i));
            int tier = Character.digit(normalized.charAt(i + 1), 10);
            if (element >= 0 && tier >= 1 && tier <= 7) {
                result.add(new PowderSelection(element, tier));
            }
        }
        return result;
    }

    private static int attackSpeedTier(String attackSpeed) {
        if (attackSpeed == null) {
            return 3;
        }
        return switch (attackSpeed.toLowerCase(Locale.ROOT)) {
            case "superslow" -> 0;
            case "veryslow" -> 1;
            case "slow" -> 2;
            case "fast" -> 4;
            case "veryfast" -> 5;
            case "superfast" -> 6;
            default -> 3;
        };
    }

    private static double classDefense(WynnItem weapon) {
        if (weapon == null || weapon.subType() == null) {
            return 1.0;
        }
        return switch (weapon.subType().toLowerCase(Locale.ROOT)) {
            case "relik" -> 0.60;
            case "bow" -> 0.70;
            case "wand" -> 0.80;
            default -> 1.0;
        };
    }

    private static double elementalSkillBonus(int[] skills, int elementIndex) {
        double factor = switch (elementIndex) {
            case 3 -> 0.867;
            case 4 -> 0.951;
            default -> 1.0;
        };
        return skillPercentage(skills[elementIndex]) * factor;
    }

    private static String capitalize(String value) {
        return Character.toUpperCase(value.charAt(0)) + value.substring(1);
    }

    private static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

    public record EquippedItem(WynnItem item, String powders, boolean weapon) {
    }

    public record SkillPointPlan(
            int[] assigned,
            int[] finalSkills,
            List<WynnItem> equipOrder,
            int totalAssigned,
            boolean underPerSkillCap,
            boolean withinAvailablePoints,
            String problem
    ) {
        public SkillPointPlan {
            assigned = Arrays.copyOf(assigned, assigned.length);
            finalSkills = Arrays.copyOf(finalSkills, finalSkills.length);
            equipOrder = List.copyOf(equipOrder);
        }
    }

    public record Result(
            int health,
            double effectiveHp,
            double effectiveHpNoAgility,
            double healthRegen,
            int[] skills,
            double[] defenses,
            Map<String, Integer> ids,
            DamageRange[] weaponDamage,
            int baseAttackTier,
            int effectiveAttackTier,
            double meleeDps,
            DamageRange meleeNormal,
            DamageRange meleeCritical,
            boolean valid,
            String problem
    ) {
    }

    public record SpellPart(
            String name,
            double[] multipliers,
            double hits,
            boolean melee,
            boolean useStrength,
            boolean useAttackSpeed
    ) {
        public static SpellPart mainAttack() {
            return new SpellPart("Main Attack", new double[]{100, 0, 0, 0, 0, 0}, 1, true, true, false);
        }
    }

    public record DamageResult(DamageRange normal, DamageRange critical) {
    }

    public record DamageRange(double min, double max) {
        private static final DamageRange ZERO = new DamageRange(0, 0);

        public double average() {
            return (min + max) / 2.0;
        }

        public DamageRange add(DamageRange other) {
            return new DamageRange(min + other.min, max + other.max);
        }

        public DamageRange scale(double multiplier) {
            return new DamageRange(min * multiplier, max * multiplier);
        }

        public DamageRange nonNegative() {
            return new DamageRange(Math.max(0, min), Math.max(0, max));
        }

        private static DamageRange flat(double value) {
            return new DamageRange(value, value);
        }
    }

    private record Powder(
            int minDamage,
            int maxDamage,
            int conversion,
            int defensePlus,
            int defenseMinus,
            int health
    ) {
    }

    private record PowderSelection(int element, int tier) {
    }
}
