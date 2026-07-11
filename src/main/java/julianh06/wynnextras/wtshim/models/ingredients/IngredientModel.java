// © Wynntils (LGPL-3.0-only) — see LICENSE-WYNNTILS
/*
 * WynnExtras — IngredientModel.
 *
 * Loads the ingredient database from cdn.wynntils.com (one-time fetch at mod init) and exposes
 * a Stream<IngredientInfo> to callers.
 *
 * URL source: Wynntils urls.json id "dataStaticIngredients".
 * Only the minimum fields are populated — name, tier, level, professions — which is what
 * WynnExtras' CraftingUtils currently keys on.
 */
package julianh06.wynnextras.wtshim.models.ingredients;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import julianh06.wynnextras.wtshim.core.WynntilsMod;
import julianh06.wynnextras.wtshim.core.components.Model;
import julianh06.wynnextras.wtshim.models.ingredients.type.IngredientInfo;
import julianh06.wynnextras.wtshim.models.profession.type.ProfessionType;
import julianh06.wynnextras.wtshim.models.wynnitem.type.ItemMaterial;
import net.minecraft.item.ItemStack;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Executors;
import java.util.stream.Stream;

public class IngredientModel extends Model {
    private static final String INGREDIENTS_URL = "https://cdn.wynntils.com/static/Reference/ingredients.json";

    private final List<IngredientInfo> ingredients = new CopyOnWriteArrayList<>();
    private volatile boolean loaded = false;

    public IngredientModel() {
        Executors.newSingleThreadExecutor(r -> {
            Thread t = new Thread(r, "WynnExtras-IngredientLoad");
            t.setDaemon(true);
            return t;
        }).submit(this::fetch);
    }

    public Stream<IngredientInfo> getAllIngredientInfos() {
        return ingredients.stream();
    }

    public boolean isLoaded() { return loaded; }

    private void fetch() {
        try {
            HttpClient http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(15)).build();
            HttpRequest req = HttpRequest.newBuilder(URI.create(INGREDIENTS_URL))
                    .timeout(Duration.ofSeconds(30))
                    .header("Accept", "application/json")
                    .header("User-Agent", "WynnExtras/" + WynntilsMod.MOD_ID)
                    .GET()
                    .build();
            HttpResponse<String> resp = http.send(req, HttpResponse.BodyHandlers.ofString());
            if (resp.statusCode() != 200) {
                WynntilsMod.warn("Ingredient DB HTTP " + resp.statusCode());
                return;
            }

            JsonElement parsed = JsonParser.parseString(resp.body());
            List<IngredientInfo> fresh = new ArrayList<>();
            if (parsed.isJsonObject()) {
                for (var entry : parsed.getAsJsonObject().entrySet()) {
                    if (!entry.getValue().isJsonObject()) continue;
                    IngredientInfo info = parse(entry.getKey(), entry.getValue().getAsJsonObject());
                    if (info != null) fresh.add(info);
                }
            }

            ingredients.clear();
            ingredients.addAll(fresh);
            loaded = true;
            WynntilsMod.info("Loaded " + ingredients.size() + " ingredients from CDN.");
        } catch (Throwable t) {
            WynntilsMod.warn("Ingredient DB fetch failed: " + t.getMessage());
        }
    }

    private static IngredientInfo parse(String key, JsonObject json) {
        String name = json.has("name") ? json.get("name").getAsString() : key;

        int tier = 0;
        if (json.has("tier")) {
            try { tier = json.get("tier").getAsInt(); } catch (Exception ignored) {}
        }

        int level = 0;
        if (json.has("level")) {
            try { level = json.get("level").getAsInt(); } catch (Exception ignored) {}
        } else if (json.has("requirements") && json.get("requirements").isJsonObject()) {
            JsonObject reqs = json.getAsJsonObject("requirements");
            if (reqs.has("level")) {
                try { level = reqs.get("level").getAsInt(); } catch (Exception ignored) {}
            }
        }

        List<ProfessionType> professions = new ArrayList<>();
        if (json.has("requirements") && json.get("requirements").isJsonObject()) {
            JsonObject reqs = json.getAsJsonObject("requirements");
            if (reqs.has("skills") && reqs.get("skills").isJsonArray()) {
                for (JsonElement el : reqs.getAsJsonArray("skills")) {
                    ProfessionType type = ProfessionType.fromString(el.getAsString());
                    if (type != null) professions.add(type);
                }
            }
        }

        // Superseded loader (CraftingDataService is authoritative) — populate only the fields this
        // parser reads; the rest default. Faithful 13-component ctor.
        return new IngredientInfo(name, tier, level, java.util.Optional.empty(),
                new ItemMaterial(ItemStack.EMPTY), professions, List.of(), java.util.Map.of(),
                List.of(), 0, 0, 0, List.of());
    }
}
