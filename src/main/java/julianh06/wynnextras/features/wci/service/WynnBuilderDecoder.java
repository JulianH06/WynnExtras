package julianh06.wynnextras.features.wci.service;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import julianh06.wynnextras.features.wci.model.IngredientRequirement;
import julianh06.wynnextras.features.wci.model.RequirementType;
import julianh06.wynnextras.features.wci.model.WynnBuilderBuild;
import julianh06.wynnextras.features.wci.service.wynnbuilder.WynnBuilderBuildCraftExtractor;
import julianh06.wynnextras.features.wci.service.wynnbuilder.WynnBuilderCraftParser;
import julianh06.wynnextras.features.wci.service.wynnbuilder.WynnBuilderUrlParts;

import java.io.ByteArrayOutputStream;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class WynnBuilderDecoder {
    private static final Pattern LOOSE_COLON = Pattern.compile("^(.+?)\\s*:\\s*(\\d+)$");
    private static final Pattern LOOSE_SUFFIX_X = Pattern.compile("^(.+?)\\s+[xX]\\s*(\\d+)$");
    private static final Pattern LOOSE_PREFIX_X = Pattern.compile("^(\\d+)\\s*[xX]\\s+(.+)$");
    private static final String CRAFT_PREFIX = "CR-";

    private final WynnCraftIngredientService registry;
    private final WynnBuilderCraftParser craftParser;
    private final WynnBuilderBuildCraftExtractor buildCraftExtractor;

    public WynnBuilderDecoder() {
        this(new WynnCraftIngredientService());
    }

    public WynnBuilderDecoder(WynnCraftIngredientService registry) {
        this.registry = registry;
        this.craftParser = new WynnBuilderCraftParser();
        this.buildCraftExtractor = new WynnBuilderBuildCraftExtractor();
    }

    public WynnBuilderBuild decode(String url) {
        DecodeAccumulator decoded = new DecodeAccumulator();
        WynnBuilderUrlParts parts;
        try {
            parts = WynnBuilderUrlParts.parse(url);
        } catch (IllegalArgumentException e) {
            if (url != null && !url.contains("://")) {
                collectPayload(url, true, false, decoded);
                return build(url, decoded);
            }
            throw e;
        }

        boolean useFragment = parts.rawFragment() != null && !parts.rawFragment().isBlank();
        String preferred = useFragment ? parts.rawFragment() : parts.rawQuery();
        if ((preferred == null || preferred.isBlank()) && parts.isBarePayload()) {
            preferred = parts.path();
        }

        collectPayload(preferred, useFragment || parts.isBarePayload(), parts.isBuilderUrl() || parts.isBarePayload(), decoded);
        if (decoded.requirements().isEmpty() && useFragment) {
            collectPayload(parts.rawQuery(), false, false, decoded);
        }
        return build(parts.sourceUrl(), decoded);
    }

    private WynnBuilderBuild build(String sourceUrl, DecodeAccumulator decoded) {
        if (decoded.requirements().isEmpty()) {
            throw new IllegalArgumentException("No supported WynnBuilder ingredients were found in URL");
        }
        return new WynnBuilderBuild(sourceUrl, decoded.craftedItems(), decoded.requirements());
    }

    private void collectPayload(
            String value,
            boolean preserveLiteralPlus,
            boolean allowBinaryBuilder,
            DecodeAccumulator decodedRequirements) {
        if (value == null || value.isBlank()) return;
        String decoded = decodeUrlComponent(value, preserveLiteralPlus).trim();
        if (decoded.isBlank()) return;

        if (allowBinaryBuilder && buildCraftExtractor.isBinaryBuildPayload(decoded)) {
            WynnBuilderBuildCraftExtractor.ExtractedCrafts extracted = buildCraftExtractor.extract(decoded);
            decodedRequirements.addAll(extracted.requirements());
            decodedRequirements.addCraftedItems(extracted.craftedItems());
            return;
        }

        if (tryJson(decoded, decodedRequirements.requirements())) return;
        if (tryBase64Json(decoded, decodedRequirements.requirements())) return;
        decodedRequirements.addCraftedItems(collectCrafts(decoded, decodedRequirements.requirements()));
        if (!decodedRequirements.requirements().isEmpty()) return;
        collectSimplified(decoded, decodedRequirements.requirements());
        if (!decodedRequirements.requirements().isEmpty()) return;
        collectLoose(decoded, decodedRequirements.requirements(), RequirementType.INGREDIENT);
    }

    private int collectCrafts(String decoded, List<IngredientRequirement> requirements) {
        int startingSize = requirements.size();
        int craftedItems = 0;
        int searchFrom = 0;
        while (searchFrom < decoded.length()) {
            int prefixStart = decoded.indexOf(CRAFT_PREFIX, searchFrom);
            if (prefixStart < 0) break;

            int bodyStart = prefixStart + CRAFT_PREFIX.length();
            int bodyEnd = bodyStart;
            while (bodyEnd < decoded.length() && WynnBuilderCraftParser.B64.indexOf(decoded.charAt(bodyEnd)) >= 0) {
                int nextPrefixStart = decoded.indexOf(CRAFT_PREFIX, bodyEnd);
                if (nextPrefixStart == bodyEnd || isCraftSeparatorBeforePrefix(decoded, bodyEnd, nextPrefixStart)) break;
                bodyEnd++;
            }

            if (bodyEnd <= bodyStart) {
                throw new IllegalArgumentException("Malformed WynnBuilder crafted hash in payload");
            }

            if (craftParser.parse(CRAFT_PREFIX + decoded.substring(bodyStart, bodyEnd))
                    .map(requirements::addAll)
                    .orElseThrow(() -> new IllegalArgumentException("Malformed WynnBuilder crafted hash in payload"))) {
                craftedItems++;
            }
            searchFrom = Math.max(bodyEnd, bodyStart);
        }

        if (requirements.size() == startingSize) {
            if (craftParser.parse(decoded).map(requirements::addAll).orElse(false)) {
                craftedItems++;
            }
        }
        return craftedItems;
    }

    private boolean isCraftSeparatorBeforePrefix(String decoded, int bodyEnd, int nextPrefixStart) {
        return nextPrefixStart == bodyEnd + 1
                && (decoded.charAt(bodyEnd) == '-' || decoded.charAt(bodyEnd) == '+');
    }

    private String decodeUrlComponent(String value, boolean preserveLiteralPlus) {
        if (!preserveLiteralPlus) {
            return URLDecoder.decode(value, StandardCharsets.UTF_8);
        }

        ByteArrayOutputStream bytes = new ByteArrayOutputStream(value.length());
        StringBuilder decoded = new StringBuilder(value.length());
        for (int i = 0; i < value.length(); i++) {
            char current = value.charAt(i);
            if (current == '%' && i + 2 < value.length()) {
                int high = Character.digit(value.charAt(i + 1), 16);
                int low = Character.digit(value.charAt(i + 2), 16);
                if (high >= 0 && low >= 0) {
                    bytes.write((high << 4) + low);
                    i += 2;
                    continue;
                }
            }
            flushDecodedBytes(bytes, decoded);
            decoded.append(current);
        }
        flushDecodedBytes(bytes, decoded);
        return decoded.toString();
    }

    private void flushDecodedBytes(ByteArrayOutputStream bytes, StringBuilder decoded) {
        if (bytes.size() == 0) return;
        decoded.append(bytes.toString(StandardCharsets.UTF_8));
        bytes.reset();
    }

    private boolean tryBase64Json(String decoded, List<IngredientRequirement> requirements) {
        String padded = decoded + "=".repeat((4 - decoded.length() % 4) % 4);
        for (Base64.Decoder decoder : List.of(Base64.getUrlDecoder(), Base64.getDecoder())) {
            byte[] bytes;
            try {
                bytes = decoder.decode(padded);
            } catch (IllegalArgumentException ignored) {
                // Not a JSON payload in this encoding.
                continue;
            }
            String json = new String(bytes, StandardCharsets.UTF_8).trim();
            if (tryJson(json, requirements)) return true;
        }
        return false;
    }

    private boolean tryJson(String decoded, List<IngredientRequirement> requirements) {
        String trimmed = decoded.trim();
        if (!trimmed.startsWith("{") && !trimmed.startsWith("[")) return false;
        int before = requirements.size();
        collectJsonRequirementSections(JsonParser.parseString(trimmed), requirements, null);
        return requirements.size() > before;
    }

    private void collectJsonRequirementSections(
            JsonElement element,
            List<IngredientRequirement> requirements,
            RequirementType requirementSection) {
        if (element == null || element.isJsonNull()) return;
        if (requirementSection != null) {
            collectJsonRequirementValue(element, requirements, requirementSection, null);
            return;
        }
        if (element.isJsonArray()) {
            element.getAsJsonArray().forEach(child -> collectJsonRequirementSections(child, requirements, null));
            return;
        }
        if (!element.isJsonObject()) return;

        JsonObject object = element.getAsJsonObject();
        for (var entry : object.entrySet()) {
            RequirementType section = requirementSection(entry.getKey());
            if (section != null) {
                collectJsonRequirementValue(entry.getValue(), requirements, section, null);
            } else {
                collectJsonRequirementSections(entry.getValue(), requirements, null);
            }
        }
    }

    private RequirementType requirementSection(String key) {
        String normalized = key.toLowerCase(Locale.ROOT);
        if (normalized.equals("materials")) return RequirementType.MATERIAL;
        if (normalized.equals("crafted") || normalized.equals("ingredients") || normalized.equals("requirements")) {
            return RequirementType.INGREDIENT;
        }
        return null;
    }

    private void collectJsonRequirementValue(
            JsonElement element,
            List<IngredientRequirement> requirements,
            RequirementType type,
            String defaultName) {
        if (element == null || element.isJsonNull()) return;
        if (element.isJsonArray()) {
            element.getAsJsonArray().forEach(child -> collectJsonRequirementValue(child, requirements, type, defaultName));
            return;
        }
        if (element.isJsonPrimitive() && element.getAsJsonPrimitive().isString()) {
            ParsedLoose parsed = parseLooseToken(element.getAsString());
            if (parsed != null) addRequirement(parsed.name(), parsed.amount(), type, 0, requirements);
            return;
        }
        if (element.isJsonPrimitive() && element.getAsJsonPrimitive().isNumber() && defaultName != null) {
            addRequirement(defaultName, Math.max(1, element.getAsInt()), type, 0, requirements);
            return;
        }
        if (!element.isJsonObject()) return;

        JsonObject object = element.getAsJsonObject();
        String name = firstString(object, "name", "displayName", "ingredient", "material", "item");
        if (name != null || defaultName != null) {
            int amount = firstInt(object, 1, "amount", "count", "quantity", "qty");
            int tier = firstInt(object, 0, "materialTier", "tier");
            addRequirement(name == null ? defaultName : name, amount, type, tier, requirements);
            return;
        }

        for (var entry : object.entrySet()) {
            collectJsonRequirementValue(entry.getValue(), requirements, type, entry.getKey());
        }
    }

    private void collectSimplified(String decoded, List<IngredientRequirement> requirements) {
        for (String part : decoded.split("&")) {
            int separator = part.indexOf('=');
            if (separator < 0) continue;
            String key = part.substring(0, separator);
            String rawIngredientValue = part.substring(separator + 1);
            if (!isIngredientKey(key)) continue;
            collectLoose(rawIngredientValue, requirements, RequirementType.INGREDIENT);
        }
    }

    private void collectLoose(
            String decoded,
            List<IngredientRequirement> requirements,
            RequirementType type) {
        for (String ingredientToken : decoded.split("[,;|]")) {
            ParsedLoose parsed = parseLooseToken(ingredientToken);
            if (parsed != null) addRequirement(parsed.name(), parsed.amount(), type, 0, requirements);
        }
    }

    private void addRequirement(
            String name,
            int amount,
            RequirementType type,
            int materialTier,
            List<IngredientRequirement> requirements) {
        requirements.add(registry.requireRequirement(name, amount, type, materialTier, "WynnBuilder"));
    }

    private boolean isIngredientKey(String key) {
        String normalizedKey = key.toLowerCase(Locale.ROOT).trim();
        return normalizedKey.equals("ingredient")
                || normalizedKey.equals("ingredients")
                || normalizedKey.equals("ing")
                || normalizedKey.equals("ings");
    }

    private String firstString(JsonObject object, String... keys) {
        for (String key : keys) {
            if (object.has(key)
                    && object.get(key).isJsonPrimitive()
                    && object.get(key).getAsJsonPrimitive().isString()) {
                return object.get(key).getAsString();
            }
        }
        return null;
    }

    private int firstInt(JsonObject object, int fallback, String... keys) {
        for (String key : keys) {
            if (object.has(key)
                    && object.get(key).isJsonPrimitive()
                    && object.get(key).getAsJsonPrimitive().isNumber()) {
                return Math.max(fallback == 0 ? 0 : 1, object.get(key).getAsInt());
            }
        }
        return fallback;
    }

    private ParsedLoose parseLooseToken(String raw) {
        if (raw == null) return null;
        String token = raw.trim();
        if (token.isBlank() || token.startsWith("http")) return null;
        Matcher prefix = LOOSE_PREFIX_X.matcher(token);
        if (prefix.matches()) return new ParsedLoose(prefix.group(2).trim(), Integer.parseInt(prefix.group(1)));
        Matcher colon = LOOSE_COLON.matcher(token);
        if (colon.matches()) return new ParsedLoose(colon.group(1).trim(), Integer.parseInt(colon.group(2)));
        Matcher suffix = LOOSE_SUFFIX_X.matcher(token);
        if (suffix.matches()) return new ParsedLoose(suffix.group(1).trim(), Integer.parseInt(suffix.group(2)));
        return new ParsedLoose(token, 1);
    }

    private record ParsedLoose(String name, int amount) {}

    private static final class DecodeAccumulator {
        private final List<IngredientRequirement> requirements = new ArrayList<>();
        private int craftedItems;

        List<IngredientRequirement> requirements() {
            return requirements;
        }

        void addAll(List<IngredientRequirement> added) {
            requirements.addAll(added);
        }

        int craftedItems() {
            return craftedItems;
        }

        void addCraftedItems(int amount) {
            craftedItems += Math.max(0, amount);
        }
    }
}
