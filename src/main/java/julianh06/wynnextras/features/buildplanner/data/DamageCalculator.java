package julianh06.wynnextras.features.buildplanner.data;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public final class DamageCalculator {
    private static final String RESOURCE =
            "/assets/wynnextras/buildplanner/data/wynnbuilder_atree_2.2.3.0.json";
    private static final String[] ELEMENTS = {"n", "e", "t", "w", "f", "a"};
    private static final double[] ATTACK_SPEED = {0.51, 0.83, 1.5, 2.05, 2.5, 3.1, 4.3};
    private static final double[] SKILL_DAMAGE = {1.0, 1.0, 1.0, 0.867, 0.951};
    private static final Map<String, JsonArray> TREES = loadTrees();

    private DamageCalculator() {
    }

    public static List<SpellResult> calculate(
            BuildCalculator.Result build,
            String weaponSubtype,
            AbilityTreeDefinition definition,
            Set<String> selectedIds
    ) {
        return calculate(build, weaponSubtype, definition, selectedIds, List.of());
    }

    public static List<SpellResult> calculate(
            BuildCalculator.Result build,
            String weaponSubtype,
            AbilityTreeDefinition definition,
            Set<String> selectedIds,
            List<AspectSelection> aspects
    ) {
        AbilityTreeClass abilityClass = AbilityTreeClass.fromWeaponSubtype(weaponSubtype);
        JsonArray source = TREES.get(abilityClass.displayName());
        Map<Integer, Spell> spells = defaultSpells(weaponSubtype);
        Stats stats = new Stats(build);
        if (source != null && definition != null) {
            applyAbilityTree(source, definition, selectedIds, aspects, spells, stats);
        }

        List<SpellResult> results = new ArrayList<>();
        for (int index = 0; index <= 4; index++) {
            Spell spell = spells.get(index);
            if (spell == null) {
                results.add(new SpellResult(index, defaultSpellName(weaponSubtype, index), 0, "", List.of()));
                continue;
            }
            Map<String, PartResult> evaluated = new LinkedHashMap<>();
            for (Part part : spell.parts.values()) {
                evaluatePart(build, stats, spell, part, evaluated, new LinkedHashSet<>());
            }
            List<PartResult> orderedParts = spell.parts.keySet().stream()
                    .map(evaluated::get)
                    .filter(java.util.Objects::nonNull)
                    .toList();
            results.add(new SpellResult(
                    index,
                    spell.name,
                    index == 0 ? 0 : spellCost(stats, build.skills()[2], spell),
                    spell.display,
                    orderedParts));
        }
        return List.copyOf(results);
    }

    public static double mainAttackRange(
            String weaponSubtype,
            AbilityTreeDefinition definition,
            Set<String> selectedIds
    ) {
        AbilityTreeClass abilityClass = AbilityTreeClass.fromWeaponSubtype(weaponSubtype);
        return defaultRange(abilityClass);
    }

    private static void applyAbilityTree(
            JsonArray source,
            AbilityTreeDefinition definition,
            Set<String> selectedIds,
            List<AspectSelection> aspects,
            Map<Integer, Spell> spells,
            Stats stats
    ) {
        Set<String> selectedNames = selectedNames(definition, selectedIds);

        List<JsonObject> active = new ArrayList<>();
        Set<Integer> activeIds = new LinkedHashSet<>();
        Map<Integer, Map<String, Double>> properties = new LinkedHashMap<>();
        activeIds.add(999);
        properties.put(999, new LinkedHashMap<>(
                Map.of("range", defaultRange(definition.abilityClass()))));
        for (JsonElement element : source) {
            JsonObject node = element.getAsJsonObject();
            int id = integer(node, "id", -1);
            Map<String, Double> nodeProperties = new LinkedHashMap<>();
            JsonObject rawProperties = object(node, "properties");
            if (rawProperties != null) {
                for (Map.Entry<String, JsonElement> entry : rawProperties.entrySet()) {
                    if (entry.getValue().isJsonPrimitive()
                            && entry.getValue().getAsJsonPrimitive().isNumber()) {
                        nodeProperties.put(entry.getKey(), entry.getValue().getAsDouble());
                    }
                }
            }
            properties.put(id, nodeProperties);
            if (matchesSelectedName(string(node, "display_name", ""), selectedNames)) {
                active.add(node);
                activeIds.add(id);
            }
        }

        for (JsonObject node : active) {
            int id = integer(node, "id", -1);
            int target = integer(node, "base_abil", id);
            if (!activeIds.contains(target)) {
                target = id;
            }
            if (target != id) {
                Map<String, Double> targetProperties =
                        properties.computeIfAbsent(target, ignored -> new LinkedHashMap<>());
                for (Map.Entry<String, Double> entry
                        : properties.getOrDefault(id, Map.of()).entrySet()) {
                    targetProperties.merge(entry.getKey(), entry.getValue(), Double::sum);
                }
            }
        }
        applyAspects(aspects, activeIds, properties, active);

        for (JsonObject node : active) {
            JsonArray effects = array(node, "effects");
            if (effects == null) {
                continue;
            }
            for (JsonElement element : effects) {
                JsonObject effect = element.getAsJsonObject();
                if (!"raw_stat".equals(string(effect, "type", ""))) {
                    continue;
                }
                if (effect.has("toggle")) {
                    continue;
                }
                JsonArray bonuses = array(effect, "bonuses");
                if (bonuses == null) {
                    continue;
                }
                for (JsonElement bonusElement : bonuses) {
                    JsonObject bonus = bonusElement.getAsJsonObject();
                    if (!"prop".equals(string(bonus, "type", ""))) {
                        continue;
                    }
                    int ability = integer(bonus, "abil", -1);
                    String name = string(bonus, "name", "");
                    double value = resolve(bonus.get("value"), properties);
                    properties.computeIfAbsent(ability, ignored -> new LinkedHashMap<>())
                            .merge(name, value, Double::sum);
                }
            }

        }

        for (JsonObject node : active) {
            JsonArray effects = array(node, "effects");
            if (effects != null) {
                for (JsonElement element : effects) {
                    JsonObject effect = element.getAsJsonObject();
                    if ("replace_spell".equals(string(effect, "type", ""))) {
                        replaceSpell(spells, effect, properties);
                    }
                }
            }
        }
        for (JsonObject node : active) {
            JsonArray effects = array(node, "effects");
            if (effects != null) {
                for (JsonElement element : effects) {
                    JsonObject effect = element.getAsJsonObject();
                    switch (string(effect, "type", "")) {
                        case "add_spell_prop" -> addSpellProperty(spells, effect, properties);
                        case "raw_stat" -> applyRawStats(stats, effect, properties);
                        default -> {
                        }
                    }
                }
            }
        }
        Stats scalingInputs = stats.copy();
        for (JsonObject node : active) {
            JsonArray effects = array(node, "effects");
            if (effects != null) {
                for (JsonElement element : effects) {
                    JsonObject effect = element.getAsJsonObject();
                    if ("stat_scaling".equals(string(effect, "type", ""))) {
                        applyScaling(stats, scalingInputs, effect, properties);
                    }
                }
            }
        }
    }

    private static void applyAspects(
            List<AspectSelection> aspects,
            Set<Integer> activeIds,
            Map<Integer, Map<String, Double>> properties,
            List<JsonObject> active
    ) {
        for (AspectSelection selection : aspects) {
            if (selection == null) {
                continue;
            }
            for (JsonObject ability : selection.selectedTier().abilities()) {
                JsonArray dependencies = array(ability, "dependencies");
                if (dependencies != null) {
                    boolean satisfied = true;
                    for (JsonElement dependency : dependencies) {
                        if (!activeIds.contains(dependency.getAsInt())) {
                            satisfied = false;
                            break;
                        }
                    }
                    if (!satisfied) {
                        continue;
                    }
                }
                int target = integer(ability, "base_abil", integer(ability, "id", -1));
                if (!activeIds.contains(target)) {
                    continue;
                }
                JsonObject rawProperties = object(ability, "properties");
                if (rawProperties != null) {
                    Map<String, Double> targetProperties =
                            properties.computeIfAbsent(target, ignored -> new LinkedHashMap<>());
                    for (Map.Entry<String, JsonElement> entry : rawProperties.entrySet()) {
                        if (entry.getValue().isJsonPrimitive()
                                && entry.getValue().getAsJsonPrimitive().isNumber()) {
                            targetProperties.merge(
                                    entry.getKey(), entry.getValue().getAsDouble(), Double::sum);
                        }
                    }
                }
                active.add(ability);
            }
        }
    }

    private static void replaceSpell(
            Map<Integer, Spell> spells,
            JsonObject effect,
            Map<Integer, Map<String, Double>> properties
    ) {
        int index = integer(effect, "base_spell", -1);
        if (index < 0) {
            return;
        }
        Spell previous = spells.get(index);
        Spell replacement = new Spell(
                string(effect, "name", previous == null ? "Spell " + index : previous.name),
                effect.has("cost") ? effect.get("cost").getAsDouble() : previous == null ? 0 : previous.cost,
                index,
                string(effect, "scaling", previous == null ? "spell" : previous.scaling),
                effect.has("use_atkspd")
                        ? effect.get("use_atkspd").getAsBoolean()
                        : previous == null || previous.useAttackSpeed,
                string(effect, "display", previous == null ? "" : previous.display),
                new LinkedHashMap<>());
        JsonArray parts = array(effect, "parts");
        if (parts == null && previous != null) {
            replacement.parts.putAll(previous.parts);
        } else if (parts != null) {
            for (JsonElement partElement : parts) {
                Part part = parsePart(partElement.getAsJsonObject(), properties);
                replacement.parts.put(part.name, part);
            }
        }
        spells.put(index, replacement);
    }

    private static void addSpellProperty(
            Map<Integer, Spell> spells,
            JsonObject effect,
            Map<Integer, Map<String, Double>> properties
    ) {
        Spell spell = spells.get(integer(effect, "base_spell", -1));
        if (spell == null) {
            return;
        }
        if (effect.has("cost")) {
            spell.cost += effect.get("cost").getAsDouble();
        }
        if (effect.has("display")) {
            spell.display = effect.get("display").getAsString();
        }
        String target = string(effect, "target_part", "");
        if (target.isBlank()) {
            return;
        }
        String behavior = string(effect, "behavior", "merge");
        Part part = spell.parts.get(target);
        if (part == null) {
            if (!"merge".equals(behavior)) {
                return;
            }
            part = parsePart(effect, properties);
            part.name = target;
            spell.parts.put(target, part);
            return;
        }
        if (effect.has("multipliers")) {
            double[] addition = doubles(effect.getAsJsonArray("multipliers"));
            for (int i = 0; i < part.multipliers.length; i++) {
                if ("overwrite".equals(behavior)) {
                    part.multipliers[i] = addition[i];
                } else {
                    part.multipliers[i] += addition[i];
                }
            }
        }
        JsonObject hits = object(effect, "hits");
        if (hits != null) {
            for (Map.Entry<String, JsonElement> entry : hits.entrySet()) {
                double value = resolve(entry.getValue(), properties);
                if ("overwrite".equals(behavior)) {
                    part.hits.put(entry.getKey(), value);
                } else {
                    part.hits.merge(entry.getKey(), value, Double::sum);
                }
            }
        }
        if (effect.has("power")) {
            if ("overwrite".equals(behavior)) {
                part.power = effect.get("power").getAsDouble();
            } else {
                part.power += effect.get("power").getAsDouble();
            }
        }
        if (effect.has("hide")) {
            part.display = !effect.get("hide").getAsBoolean();
        }
    }

    private static Part parsePart(
            JsonObject source,
            Map<Integer, Map<String, Double>> properties
    ) {
        String name = string(source, "name", string(source, "target_part", "Part"));
        double[] multipliers = source.has("multipliers")
                ? doubles(source.getAsJsonArray("multipliers"))
                : new double[6];
        Map<String, Double> hits = new LinkedHashMap<>();
        JsonObject rawHits = object(source, "hits");
        if (rawHits != null) {
            for (Map.Entry<String, JsonElement> entry : rawHits.entrySet()) {
                hits.put(entry.getKey(), resolve(entry.getValue(), properties));
            }
        }
        List<String> ignored = new ArrayList<>();
        JsonArray ignoredSource = array(source, "ignored_mults");
        if (ignoredSource != null) {
            for (JsonElement value : ignoredSource) {
                ignored.add(value.getAsString());
            }
        }
        return new Part(
                name,
                multipliers,
                source.has("power") ? source.get("power").getAsDouble() : 0,
                hits,
                !source.has("use_str") || source.get("use_str").getAsBoolean(),
                !source.has("display") || source.get("display").getAsBoolean(),
                ignored);
    }

    private static void applyRawStats(
            Stats stats,
            JsonObject effect,
            Map<Integer, Map<String, Double>> properties
    ) {
        if (effect.has("toggle")) {
            return;
        }
        JsonArray bonuses = array(effect, "bonuses");
        if (bonuses == null) {
            return;
        }
        for (JsonElement bonusElement : bonuses) {
            JsonObject bonus = bonusElement.getAsJsonObject();
            if ("stat".equals(string(bonus, "type", ""))) {
                stats.add(string(bonus, "name", ""), resolve(bonus.get("value"), properties));
            }
        }
    }

    private static void applyScaling(
            Stats stats,
            Stats inputsSnapshot,
            JsonObject effect,
            Map<Integer, Map<String, Double>> properties
    ) {
        JsonArray scaling = array(effect, "scaling");
        if (!effect.has("output") || scaling == null || scaling.isEmpty()) {
            return;
        }
        double total = 0;
        boolean slider = effect.has("slider") && effect.get("slider").getAsBoolean();
        if (slider) {
            double value = effect.has("slider_default") ? effect.get("slider_default").getAsDouble() : 0;
            double scale = resolve(scaling.get(0), properties);
            total = effect.has("multiplicative") && effect.get("multiplicative").getAsBoolean()
                    ? (Math.pow((100 + scale) / 100, (int) value) - 1) * 100
                    : value * scale;
        } else {
            JsonArray inputs = array(effect, "inputs");
            if (inputs == null) {
                return;
            }
            for (int i = 0; i < inputs.size() && i < scaling.size(); i++) {
                JsonObject input = inputs.get(i).getAsJsonObject();
                double value = "prop".equals(string(input, "type", ""))
                        ? properties.getOrDefault(integer(input, "abil", -1), Map.of())
                                .getOrDefault(string(input, "name", ""), 0.0)
                        : inputsSnapshot.get(string(input, "name", ""));
                total += value * resolve(scaling.get(i), properties);
            }
        }
        boolean round = !effect.has("round") || effect.get("round").getAsBoolean();
        boolean positive = !effect.has("positive") || effect.get("positive").getAsBoolean();
        if (slider) {
            round = false;
            positive = false;
        }
        if (round) {
            total = Math.floor(total + 0.0000001);
        }
        if (positive && total < 0) {
            total = 0;
        }
        if (effect.has("max")) {
            double maximum = resolve(effect.get("max"), properties);
            if (maximum > 0 && total > maximum || maximum < 0 && total < maximum) {
                total = maximum;
            }
        }
        JsonElement output = effect.get("output");
        if (output.isJsonArray()) {
            for (JsonElement item : output.getAsJsonArray()) {
                applyScalingOutput(stats, properties, item.getAsJsonObject(), total);
            }
        } else if (output.isJsonObject()) {
            applyScalingOutput(stats, properties, output.getAsJsonObject(), total);
        }
    }

    private static void applyScalingOutput(
            Stats stats,
            Map<Integer, Map<String, Double>> properties,
            JsonObject output,
            double value
    ) {
        if ("stat".equals(string(output, "type", ""))) {
            stats.add(string(output, "name", ""), value);
        } else if ("prop".equals(string(output, "type", ""))) {
            properties.computeIfAbsent(integer(output, "abil", -1), ignored -> new LinkedHashMap<>())
                    .merge(string(output, "name", ""), value, Double::sum);
        }
    }

    private static PartResult evaluatePart(
            BuildCalculator.Result build,
            Stats stats,
            Spell spell,
            Part part,
            Map<String, PartResult> evaluated,
            Set<String> evaluating
    ) {
        PartResult cached = evaluated.get(part.name);
        if (cached != null) {
            return cached;
        }
        if (!evaluating.add(part.name)) {
            return PartResult.empty(part.name, part.display);
        }
        PartResult result;
        if (!part.hits.isEmpty()) {
            result = PartResult.empty(part.name, part.display);
            for (Map.Entry<String, Double> hit : part.hits.entrySet()) {
                Part source = spell.parts.get(hit.getKey());
                if (source == null) {
                    continue;
                }
                result = result.add(evaluatePart(
                        build, stats, spell, source, evaluated, evaluating).scale(hit.getValue()));
            }
            result = result.named(part.name, part.display);
        } else if (part.power != 0) {
            double multiplier = 1;
            for (double value : stats.healingMultipliers.values()) {
                multiplier *= 1 + value / 100.0;
            }
            result = PartResult.heal(
                    part.name, part.power * build.health() * multiplier, part.display);
        } else {
            result = calculateDamage(build, stats, spell, part);
        }
        evaluating.remove(part.name);
        evaluated.put(part.name, result);
        return result;
    }

    private static PartResult calculateDamage(
            BuildCalculator.Result build,
            Stats stats,
            Spell spell,
            Part part
    ) {
        BuildCalculator.DamageRange[] source = build.weaponDamage();
        double[][] base = new double[6][2];
        boolean[] present = new boolean[6];
        double weaponMin = 0;
        double weaponMax = 0;
        for (int i = 0; i < 6; i++) {
            base[i][0] = source[i].min();
            base[i][1] = source[i].max();
            present[i] = source[i].max() > 0;
            weaponMin += source[i].min();
            weaponMax += source[i].max();
        }

        double[] conversions = Arrays.copyOf(part.multipliers, 6);
        String partFilter = spell.index + "." + part.name;
        for (int i = 0; i < 6; i++) {
            conversions[i] += stats.get(ELEMENTS[i] + "ConvBase")
                    + stats.get(ELEMENTS[i] + "ConvBase:" + partFilter);
        }
        double[][] damage = new double[6][2];
        double neutral = conversions[0] / 100.0;
        if (neutral == 0) {
            Arrays.fill(present, false);
        }
        for (int i = 0; i < 6; i++) {
            damage[i][0] = base[i][0] * neutral;
            damage[i][1] = base[i][1] * neutral;
        }
        double totalConversion = neutral;
        for (int i = 1; i < 6; i++) {
            if (conversions[i] > 0) {
                double conversion = conversions[i] / 100.0;
                damage[i][0] += weaponMin * conversion;
                damage[i][1] += weaponMax * conversion;
                present[i] = true;
                totalConversion += conversion;
            }
        }
        if (spell.useAttackSpeed) {
            double speed = ATTACK_SPEED[build.baseAttackTier()];
            for (double[] range : damage) {
                range[0] *= speed;
                range[1] *= speed;
            }
        }
        for (int i = 0; i < 6; i++) {
            if (present[i]) {
                damage[i][0] += stats.get(ELEMENTS[i] + "DamAddMin");
                damage[i][1] += stats.get(ELEMENTS[i] + "DamAddMax");
            }
        }

        String mode = "spell".equals(spell.scaling) ? "Sd" : "Md";
        double genericPercent = (stats.get(mode.toLowerCase(Locale.ROOT) + "Pct")
                + stats.get("damPct")) / 100.0;
        double[][] proportions = copy(damage);
        double totalMin = 0;
        double totalMax = 0;
        for (double[] range : proportions) {
            totalMin += range[0];
            totalMax += range[1];
        }
        for (int i = 0; i < 6; i++) {
            double skill = i == 0 ? 0
                    : BuildCalculator.skillPercentage(build.skills()[i - 1]) * SKILL_DAMAGE[i - 1];
            double boost = 1 + skill + genericPercent
                    + (stats.get(ELEMENTS[i] + mode + "Pct")
                    + stats.get(ELEMENTS[i] + "DamPct")) / 100.0;
            if (i > 0) {
                boost += (stats.get("r" + mode + "Pct") + stats.get("rDamPct")) / 100.0;
            }
            damage[i][0] *= boost;
            damage[i][1] *= boost;
        }

        double elementalMin = totalMin - proportions[0][0];
        double elementalMax = totalMax - proportions[0][1];
        double proportionalRaw = stats.get(mode.toLowerCase(Locale.ROOT) + "Raw")
                + stats.get("damRaw");
        double rainbowRaw = stats.get("r" + mode + "Raw") + stats.get("rDamRaw");
        for (int i = 0; i < 6; i++) {
            double raw = present[i]
                    ? stats.get(ELEMENTS[i] + mode + "Raw") + stats.get(ELEMENTS[i] + "DamRaw")
                    : 0;
            double minBoost = raw;
            double maxBoost = raw;
            if (totalMax > 0) {
                minBoost += (totalMin == 0
                        ? proportions[i][1] / totalMax
                        : proportions[i][0] / totalMin) * proportionalRaw;
                maxBoost += proportions[i][1] / totalMax * proportionalRaw;
            }
            if (i > 0 && elementalMax > 0) {
                minBoost += (elementalMin == 0
                        ? proportions[i][1] / elementalMax
                        : proportions[i][0] / elementalMin) * rainbowRaw;
                maxBoost += proportions[i][1] / elementalMax * rainbowRaw;
            }
            damage[i][0] += minBoost * totalConversion;
            damage[i][1] += maxBoost * totalConversion;
        }

        double globalMultiplier = 1;
        double[] elementalMultipliers = {1, 1, 1, 1, 1, 1};
        for (Map.Entry<String, Double> entry : stats.multipliers.entrySet()) {
            String key = entry.getKey();
            if (part.ignoredMultipliers.contains(key)) {
                continue;
            }
            if (key.contains(":") && !key.substring(key.indexOf(':') + 1).equals(partFilter)) {
                continue;
            }
            if (key.contains(";")) {
                String element = key.substring(key.indexOf(';') + 1);
                int index = Arrays.asList(ELEMENTS).indexOf(element);
                if (index >= 0) {
                    elementalMultipliers[index] *= 1 + entry.getValue() / 100.0;
                }
            } else {
                globalMultiplier *= 1 + entry.getValue() / 100.0;
            }
        }

        double strength = part.useStrength
                ? BuildCalculator.skillPercentage(build.skills()[0])
                : 0;
        double critical = part.useStrength ? 1 + stats.get("critDamPct") / 100.0 : 0;
        double[][] normal = new double[6][2];
        double[][] crit = new double[6][2];
        double normalMin = 0;
        double normalMax = 0;
        double critMin = 0;
        double critMax = 0;
        for (int i = 0; i < 6; i++) {
            double min = Math.max(0, damage[i][0]) * elementalMultipliers[i] * globalMultiplier;
            double max = Math.max(0, damage[i][1]) * elementalMultipliers[i] * globalMultiplier;
            normal[i][0] = min * (1 + strength);
            normal[i][1] = max * (1 + strength);
            crit[i][0] = min * (1 + strength + critical);
            crit[i][1] = max * (1 + strength + critical);
            normalMin += normal[i][0];
            normalMax += normal[i][1];
            critMin += crit[i][0];
            critMax += crit[i][1];
        }
        return new PartResult(
                part.name, false, part.display, conversions,
                normal, crit,
                new double[]{normalMin, normalMax},
                new double[]{critMin, critMax},
                0);
    }

    private static double spellCost(Stats stats, int intelligence, Spell spell) {
        double intelligenceEffect = BuildCalculator.skillPercentage(intelligence)
                * (0.5 / BuildCalculator.skillPercentage(150));
        double cost = spell.cost * (1 - intelligenceEffect);
        cost += stats.get("spRaw" + spell.index);
        cost *= 1 + stats.get("spPct" + spell.index) / 100.0;
        cost *= 1 + stats.get("spPct" + spell.index + "Final") / 100.0;
        return Math.max(1, cost);
    }

    private static Map<Integer, Spell> defaultSpells(String subtype) {
        String normalized = subtype == null ? "" : subtype.toLowerCase(Locale.ROOT);
        Map<Integer, Spell> spells = new LinkedHashMap<>();
        Spell melee;
        if ("relik".equals(normalized)) {
            melee = new Spell("Relik Melee", 0, 0, "melee", false, "Total", new LinkedHashMap<>());
            melee.parts.put("Single Beam", Part.damage("Single Beam", 33));
            melee.parts.put("Total", Part.total("Total", Map.of("Single Beam", 3.0)));
        } else {
            String name = "bow".equals(normalized) ? "Bow Shot" : "Melee";
            String partName = "bow".equals(normalized) ? "Single Shot" : "Melee";
            melee = new Spell(name, 0, 0, "melee", false, partName, new LinkedHashMap<>());
            melee.parts.put(partName, Part.damage(partName, 100));
        }
        spells.put(0, melee);
        return spells;
    }

    private static String defaultSpellName(String subtype, int index) {
        String normalized = subtype == null ? "" : subtype.toLowerCase(Locale.ROOT);
        String[][] names = {
                {"Bow Shot", "Arrow Storm", "Escape", "Arrow Bomb", "Arrow Shield"},
                {"Melee", "Bash", "Charge", "Uppercut", "War Scream"},
                {"Melee", "Heal", "Teleport", "Meteor", "Ice Snake"},
                {"Melee", "Spin Attack", "Vanish", "Multihit", "Smoke Bomb"},
                {"Relik Melee", "Totem", "Haul", "Aura", "Uproot"}
        };
        int row = switch (normalized) {
            case "spear" -> 1;
            case "wand" -> 2;
            case "dagger" -> 3;
            case "relik" -> 4;
            default -> 0;
        };
        return names[row][index];
    }

    private static double resolve(
            JsonElement value,
            Map<Integer, Map<String, Double>> properties
    ) {
        if (value == null || value.isJsonNull()) {
            return 0;
        }
        if (value.isJsonPrimitive() && value.getAsJsonPrimitive().isNumber()) {
            return value.getAsDouble();
        }
        String reference = value.getAsString();
        int separator = reference.indexOf('.');
        if (separator < 1) {
            try {
                return Double.parseDouble(reference);
            } catch (NumberFormatException ignored) {
                return 0;
            }
        }
        try {
            int id = Integer.parseInt(reference.substring(0, separator));
            return properties.getOrDefault(id, Map.of())
                    .getOrDefault(reference.substring(separator + 1), 0.0);
        } catch (NumberFormatException ignored) {
            return 0;
        }
    }

    private static String normalizeName(String name) {
        return name.toLowerCase(Locale.ROOT)
                .replaceAll("<[^>]+>", "")
                .replaceAll("[^a-z0-9]", "");
    }

    private static Set<String> selectedNames(
            AbilityTreeDefinition definition,
            Set<String> selectedIds
    ) {
        Set<String> result = new LinkedHashSet<>();
        for (String id : selectedIds) {
            AbilityTreeDefinition.Node node = definition.nodes().get(id);
            if (node != null) {
                result.add(normalizeName(node.name()));
            }
        }
        return result;
    }

    private static boolean matchesSelectedName(String name, Set<String> selectedNames) {
        String normalized = normalizeName(name);
        if (selectedNames.contains(normalized)) {
            return true;
        }
        return normalized.endsWith("i") && !normalized.endsWith("ii")
                && selectedNames.contains(normalized.substring(0, normalized.length() - 1));
    }

    private static double defaultRange(AbilityTreeClass abilityClass) {
        return switch (abilityClass) {
            case MAGE -> 5000;
            case WARRIOR, ASSASSIN -> 2;
            case ARCHER, SHAMAN -> 15;
        };
    }

    private static Map<String, JsonArray> loadTrees() {
        try (var stream = DamageCalculator.class.getResourceAsStream(RESOURCE)) {
            if (stream == null) {
                return Map.of();
            }
            JsonObject root = JsonParser.parseReader(
                    new InputStreamReader(stream, StandardCharsets.UTF_8)).getAsJsonObject();
            Map<String, JsonArray> result = new LinkedHashMap<>();
            for (Map.Entry<String, JsonElement> entry : root.entrySet()) {
                result.put(entry.getKey(), entry.getValue().getAsJsonArray());
            }
            return Map.copyOf(result);
        } catch (Exception ignored) {
            return Map.of();
        }
    }

    private static double[] doubles(JsonArray values) {
        double[] result = new double[6];
        for (int i = 0; i < result.length && i < values.size(); i++) {
            result[i] = values.get(i).getAsDouble();
        }
        return result;
    }

    private static double[][] copy(double[][] source) {
        double[][] result = new double[source.length][];
        for (int i = 0; i < source.length; i++) {
            result[i] = Arrays.copyOf(source[i], source[i].length);
        }
        return result;
    }

    private static JsonObject object(JsonObject source, String key) {
        return source.has(key) && source.get(key).isJsonObject() ? source.getAsJsonObject(key) : null;
    }

    private static JsonArray array(JsonObject source, String key) {
        return source.has(key) && source.get(key).isJsonArray() ? source.getAsJsonArray(key) : null;
    }

    private static String string(JsonObject source, String key, String fallback) {
        return source.has(key) && source.get(key).isJsonPrimitive()
                ? source.get(key).getAsString() : fallback;
    }

    private static int integer(JsonObject source, String key, int fallback) {
        return source.has(key) && source.get(key).isJsonPrimitive()
                ? source.get(key).getAsInt() : fallback;
    }

    public record SpellResult(
            int index,
            String name,
            double cost,
            String displayPart,
            List<PartResult> parts
    ) {
        public PartResult summaryPart() {
            for (PartResult part : parts) {
                if (part.name().equalsIgnoreCase(displayPart)) {
                    return part;
                }
            }
            return parts.stream().filter(PartResult::display).findFirst().orElse(null);
        }
    }

    public record PartResult(
            String name,
            boolean healing,
            boolean display,
            double[] multipliers,
            double[][] normal,
            double[][] critical,
            double[] normalTotal,
            double[] criticalTotal,
            double healingAmount
    ) {
        private static PartResult empty(String name, boolean display) {
            return new PartResult(name, false, display, new double[6],
                    new double[6][2], new double[6][2], new double[2], new double[2], 0);
        }

        private static PartResult heal(String name, double amount, boolean display) {
            return new PartResult(name, true, display, new double[6],
                    new double[6][2], new double[6][2], new double[2], new double[2], amount);
        }

        public double normalAverage() {
            return (normalTotal[0] + normalTotal[1]) / 2.0;
        }

        public double criticalAverage() {
            return (criticalTotal[0] + criticalTotal[1]) / 2.0;
        }

        public double average(double criticalChance) {
            return healing ? healingAmount
                    : normalAverage() * (1 - criticalChance) + criticalAverage() * criticalChance;
        }

        private PartResult scale(double multiplier) {
            return new PartResult(name, healing, display, scale(multipliers, multiplier),
                    scale(normal, multiplier), scale(critical, multiplier),
                    scale(normalTotal, multiplier), scale(criticalTotal, multiplier),
                    healingAmount * multiplier);
        }

        private PartResult add(PartResult other) {
            return new PartResult(name, healing || other.healing, display,
                    add(multipliers, other.multipliers),
                    add(normal, other.normal), add(critical, other.critical),
                    add(normalTotal, other.normalTotal), add(criticalTotal, other.criticalTotal),
                    healingAmount + other.healingAmount);
        }

        private PartResult named(String newName, boolean newDisplay) {
            return new PartResult(newName, healing, newDisplay, multipliers,
                    normal, critical, normalTotal, criticalTotal, healingAmount);
        }

        private static double[] scale(double[] values, double multiplier) {
            double[] result = Arrays.copyOf(values, values.length);
            for (int i = 0; i < result.length; i++) result[i] *= multiplier;
            return result;
        }

        private static double[][] scale(double[][] values, double multiplier) {
            double[][] result = copy(values);
            for (double[] range : result) {
                range[0] *= multiplier;
                range[1] *= multiplier;
            }
            return result;
        }

        private static double[] add(double[] left, double[] right) {
            double[] result = Arrays.copyOf(left, left.length);
            for (int i = 0; i < result.length; i++) result[i] += right[i];
            return result;
        }

        private static double[][] add(double[][] left, double[][] right) {
            double[][] result = copy(left);
            for (int i = 0; i < result.length; i++) {
                result[i][0] += right[i][0];
                result[i][1] += right[i][1];
            }
            return result;
        }
    }

    private static final class Spell {
        private String name;
        private double cost;
        private final int index;
        private String scaling;
        private boolean useAttackSpeed;
        private String display;
        private final LinkedHashMap<String, Part> parts;

        private Spell(
                String name,
                double cost,
                int index,
                String scaling,
                boolean useAttackSpeed,
                String display,
                LinkedHashMap<String, Part> parts
        ) {
            this.name = name;
            this.cost = cost;
            this.index = index;
            this.scaling = scaling;
            this.useAttackSpeed = useAttackSpeed;
            this.display = display;
            this.parts = parts;
        }
    }

    private static final class Part {
        private String name;
        private final double[] multipliers;
        private double power;
        private final Map<String, Double> hits;
        private final boolean useStrength;
        private boolean display;
        private final List<String> ignoredMultipliers;

        private Part(
                String name,
                double[] multipliers,
                double power,
                Map<String, Double> hits,
                boolean useStrength,
                boolean display,
                List<String> ignoredMultipliers
        ) {
            this.name = name;
            this.multipliers = multipliers;
            this.power = power;
            this.hits = hits;
            this.useStrength = useStrength;
            this.display = display;
            this.ignoredMultipliers = ignoredMultipliers;
        }

        private static Part damage(String name, double neutral) {
            return new Part(name, new double[]{neutral, 0, 0, 0, 0, 0},
                    0, new LinkedHashMap<>(), true, true, List.of());
        }

        private static Part total(String name, Map<String, Double> hits) {
            return new Part(name, new double[6], 0,
                    new LinkedHashMap<>(hits), true, true, List.of());
        }
    }

    private static final class Stats {
        private final Map<String, Double> values = new LinkedHashMap<>();
        private final Map<String, Double> multipliers = new LinkedHashMap<>();
        private final Map<String, Double> healingMultipliers = new LinkedHashMap<>();

        private Stats(BuildCalculator.Result build) {
            for (Map.Entry<String, Integer> entry : build.ids().entrySet()) {
                String key = alias(entry.getKey());
                values.merge(key, entry.getValue().doubleValue(), Double::sum);
            }
            String[] skills = {"str", "dex", "int", "def", "agi"};
            for (int i = 0; i < skills.length; i++) {
                values.put(skills[i], (double) build.skills()[i]);
            }
        }

        private Stats copy() {
            Stats copy = new Stats();
            copy.values.putAll(values);
            copy.multipliers.putAll(multipliers);
            copy.healingMultipliers.putAll(healingMultipliers);
            return copy;
        }

        private Stats() {
        }

        private double get(String key) {
            return values.getOrDefault(key, 0.0);
        }

        private void add(String key, double value) {
            if (key.startsWith("damMult.")) {
                multipliers.merge(key.substring("damMult.".length()), value, Double::sum);
            } else if (key.startsWith("healMult.")) {
                healingMultipliers.merge(key.substring("healMult.".length()), value, Double::sum);
            } else {
                values.merge(alias(key), value, Double::sum);
            }
        }

        private static String alias(String key) {
            return switch (key) {
                case "spellDamage" -> "sdPct";
                case "rawSpellDamage" -> "sdRaw";
                case "mainAttackDamage" -> "mdPct";
                case "rawMainAttackDamage" -> "mdRaw";
                case "damage" -> "damPct";
                case "rawDamage" -> "damRaw";
                case "elementalSpellDamage" -> "rSdPct";
                case "rawElementalSpellDamage" -> "rSdRaw";
                case "elementalMainAttackDamage" -> "rMdPct";
                case "rawElementalMainAttackDamage" -> "rMdRaw";
                case "elementalDamage" -> "rDamPct";
                case "rawElementalDamage" -> "rDamRaw";
                case "neutralSpellDamage" -> "nSdPct";
                case "rawNeutralSpellDamage" -> "nSdRaw";
                case "neutralMainAttackDamage" -> "nMdPct";
                case "rawNeutralMainAttackDamage" -> "nMdRaw";
                case "neutralDamage" -> "nDamPct";
                case "rawNeutralDamage" -> "nDamRaw";
                case "criticalDamageBonus" -> "critDamPct";
                case "healingEfficiency" -> "healPct";
                case "rawAttackSpeed" -> "atkTier";
                case "1stSpellCost" -> "spPct1";
                case "raw1stSpellCost" -> "spRaw1";
                case "2ndSpellCost" -> "spPct2";
                case "raw2ndSpellCost" -> "spRaw2";
                case "3rdSpellCost" -> "spPct3";
                case "raw3rdSpellCost" -> "spRaw3";
                case "4thSpellCost" -> "spPct4";
                case "raw4thSpellCost" -> "spRaw4";
                default -> elementalAlias(key);
            };
        }

        private static String elementalAlias(String key) {
            String[] names = {"earth", "thunder", "water", "fire", "air"};
            String[] prefixes = {"e", "t", "w", "f", "a"};
            for (int i = 0; i < names.length; i++) {
                String name = names[i];
                String prefix = prefixes[i];
                if (key.equals(name + "SpellDamage")) return prefix + "SdPct";
                if (key.equals("raw" + capitalize(name) + "SpellDamage")) return prefix + "SdRaw";
                if (key.equals(name + "MainAttackDamage")) return prefix + "MdPct";
                if (key.equals("raw" + capitalize(name) + "MainAttackDamage")) return prefix + "MdRaw";
                if (key.equals(name + "Damage")) return prefix + "DamPct";
                if (key.equals("raw" + capitalize(name) + "Damage")) return prefix + "DamRaw";
            }
            return key;
        }

        private static String capitalize(String value) {
            return Character.toUpperCase(value.charAt(0)) + value.substring(1);
        }
    }
}
