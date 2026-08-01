package julianh06.wynnextras.features.wci.service;

import julianh06.wynnextras.features.crafting.data.CraftingDataService;
import julianh06.wynnextras.features.crafting.wynnbuilder.DecodedCraft;
import julianh06.wynnextras.features.crafting.wynnbuilder.WynnBuilderBuildDecoder;
import julianh06.wynnextras.features.wci.model.IngredientRequirement;
import julianh06.wynnextras.features.wci.model.RequirementType;
import julianh06.wynnextras.features.wci.model.WynnBuilderBuild;
import julianh06.wynnextras.features.wci.util.IngredientNormalizer;

import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class WynnBuilderDecoder {
    private final CraftingDataService dataService;

    public WynnBuilderDecoder() {
        this(CraftingDataService.getInstance());
    }

    public WynnBuilderDecoder(CraftingDataService dataService) {
        this.dataService = dataService;
    }

    public WynnBuilderBuild decode(String input) {
        if (input == null || input.isBlank()) {
            throw new IllegalArgumentException("WynnBuilder URL must not be blank");
        }
        if (dataService.getState() != CraftingDataService.State.READY) {
            throw new IllegalStateException(dataService.getStatusMessage());
        }

        String source = input.trim();
        String payload = payload(source);
        List<DecodedCraft> crafts;
        if (WynnBuilderBuildDecoder.isBuildHash(payload)) {
            crafts = WynnBuilderBuildDecoder.decode(payload, dataService);
        } else {
            DecodedCraft craft = julianh06.wynnextras.features.crafting.wynnbuilder.WynnBuilderDecoder.decode(payload);
            crafts = craft == null ? List.of() : List.of(craft);
        }
        if (crafts.isEmpty()) {
            throw new IllegalArgumentException("No supported WynnBuilder crafted items were found");
        }

        List<IngredientRequirement> requirements = new ArrayList<>();
        for (DecodedCraft craft : crafts) {
            requirements.addAll(requirements(craft));
        }
        return new WynnBuilderBuild(source, crafts.size(), requirements);
    }

    private List<IngredientRequirement> requirements(DecodedCraft craft) {
        CraftingDataService.RecipeData recipe = dataService.getRecipeByWynnBuilderId(craft.recipeId());
        if (recipe == null) {
            throw new IllegalArgumentException("Unknown WynnBuilder recipe ID: " + craft.recipeId());
        }

        String source = recipe.type().getDisplayName();
        List<IngredientRequirement> requirements = new ArrayList<>();
        for (int id : craft.ingredientIds()) {
            if (julianh06.wynnextras.features.crafting.wynnbuilder.WynnBuilderDecoder.isNoIngredient(id)) continue;
            String name = dataService.getIngredientNameByWynnBuilderId(id);
            if (name == null) {
                throw new IllegalArgumentException("Unknown WynnBuilder ingredient ID: " + id);
            }
            requirements.add(new IngredientRequirement(
                    RequirementType.INGREDIENT,
                    IngredientNormalizer.key(name),
                    name,
                    1,
                    source,
                    0));
        }

        List<CraftingDataService.Material> materials = recipe.materials();
        for (int index = 0; index < materials.size(); index++) {
            CraftingDataService.Material material = materials.get(index);
            int tier = index == 0 ? craft.mat1Tier() : craft.mat2Tier();
            requirements.add(IngredientRequirement.material(
                    IngredientNormalizer.key(material.item()),
                    material.item(),
                    material.amount(),
                    source,
                    tier));
        }
        return requirements;
    }

    private static String payload(String source) {
        if (!source.contains("://")) return source;

        URI uri;
        try {
            uri = URI.create(source.replace("|", "%7C"));
        } catch (IllegalArgumentException ex) {
            throw new IllegalArgumentException("Malformed WynnBuilder URL", ex);
        }
        if (!allowedHost(uri.getHost(), uri.getPath())) {
            throw new IllegalArgumentException("Unsupported WynnBuilder URL host: " + uri.getHost());
        }

        String raw = uri.getRawFragment();
        if (raw == null || raw.isBlank()) raw = uri.getRawQuery();
        if (raw == null || raw.isBlank()) {
            throw new IllegalArgumentException("WynnBuilder URL has no build payload");
        }
        return URLDecoder.decode(raw.replace("+", "%2B"), StandardCharsets.UTF_8);
    }

    private static boolean allowedHost(String host, String path) {
        if (host == null) return false;
        String normalized = host.toLowerCase(Locale.ROOT);
        if (normalized.equals("wynnbuilder.github.io")
                || normalized.equals("wynnbuilder-beta.github.io")
                || normalized.equals("hppeng-wynn.github.io")) {
            return true;
        }
        return normalized.equals("alex-guha.github.io")
                && path != null
                && path.startsWith("/wynnbuilder-beta.github.io/");
    }
}
