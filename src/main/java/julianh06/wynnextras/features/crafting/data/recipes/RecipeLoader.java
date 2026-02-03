package julianh06.wynnextras.features.crafting.data.recipes;

import com.google.gson.*;
import com.wynntils.models.profession.type.ProfessionType;
import julianh06.wynnextras.features.crafting.data.CraftableType;
import org.jetbrains.annotations.NotNull;
import org.joml.Vector2i;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class RecipeLoader {
    private static Map<CraftableType, Map<Vector2i, RecipeData>> recipes = new HashMap<>();

    public static RecipeData getRecipe(CraftableType type, Vector2i lvl) {
        Map<Vector2i, RecipeData> allTypeDataMap = recipes.get(type);
        if (allTypeDataMap == null) {
            System.err.println("No recipe data found for type " + type);
            return null;
        }
        return allTypeDataMap.get(lvl);
    }

    public record RecipeData(
            CraftableType type,
            ProfessionType skill,
            List<Material> materials,
            Vector2i healthOrDamage,
            Vector2i durability,
            Vector2i duration,
            Vector2i basicDuration,
            Vector2i lvl,
            String name,
            int id
    ) {


        @Override
        public @NotNull String toString() {
            return "RecipeData{" +
                    "type='" + type + '\'' +
                    ", skill='" + skill + '\'' +
                    ", materials=" + materials +
                    ", healthOrDamage=" + healthOrDamage +
                    ", durability=" + durability +
                    ", duration=" + duration +
                    ", basicDuration=" + basicDuration +
                    ", lvl=" + lvl +
                    ", name='" + name + '\'' +
                    ", id=" + id +
                    '}';
        }

        public static class Material {
            String item;
            int amount;

            @Override
            public String toString() {
                return amount + " " + item;
            }
        }
    }

    private static class JsonRecipeList {
        List<RecipeData> recipes;
        double version;
    }

    public static void loadRecipes() {
        try {
            Gson gson = new GsonBuilder()
                    .registerTypeAdapter(Vector2i.class, (JsonDeserializer<Vector2i>) (json, typeOfT, ctx) -> {
                        Vector2i data = new Vector2i();
                        if (json.isJsonObject()) {
                            JsonObject obj = json.getAsJsonObject();
                            data.x = obj.get("minimum").getAsInt();
                            data.y = obj.get("maximum").getAsInt();
                        } else {
                            throw new JsonParseException("Unexpected JSON for IdentificationData: " + json);
                        }
                        return data;
                    })
                    .registerTypeAdapter(CraftableType.class, (JsonDeserializer<CraftableType>)
                            (json, typeOfT, ctx) -> {
                                try {
                                    return CraftableType.valueOf(json.getAsString());
                                } catch (IllegalArgumentException e) {
                                    throw new JsonParseException("Invalid CraftableType: " + json);
                                }
                            })
                    .registerTypeAdapter(ProfessionType.class, (JsonDeserializer<ProfessionType>)
                            (json, typeOfT, ctx) -> {
                                try {
                                    return ProfessionType.valueOf(json.getAsString());
                                } catch (IllegalArgumentException e) {
                                    throw new JsonParseException("Invalid ProfessionType: " + json);
                                }
                            })
                    .create();


            InputStream input = RecipeLoader.class.getClassLoader().getResourceAsStream("assets/wynnextras/recipes_compress.json");
            Reader reader = new InputStreamReader(input);

            JsonRecipeList jsonRecipes = gson.fromJson(reader, JsonRecipeList.class);
            reader.close();

            double remoteVersion = getRemoteVersion();
            if (remoteVersion == -1) System.err.println("failed to get remote version");
            else if (jsonRecipes.version < remoteVersion)
                System.err.println("new recipe version available current " + jsonRecipes.version + " remote " + remoteVersion);

            for (RecipeData jsonRecipe : jsonRecipes.recipes) {
                Map<Vector2i, RecipeData> levelMap = recipes.computeIfAbsent(jsonRecipe.type, k -> new HashMap<>());
                levelMap.put(jsonRecipe.lvl, jsonRecipe);
            }

            System.out.println("Loaded " + recipes.values().stream().mapToInt(Map::size).sum() + " recipes from file");

        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    public static double getRemoteVersion() {
        String end = "";
        try {
            URL url = new URL("https://raw.githubusercontent.com/wynnbuilder/wynnbuilder.github.io/master/recipes_compress.json");
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();

            // Get file size
            conn.setRequestMethod("HEAD");
            long fileSize = conn.getContentLengthLong();
            conn.disconnect();

            // Request tail bytes
            conn = (HttpURLConnection) url.openConnection();
            long startByte = Math.max(0, fileSize - 20);
            conn.setRequestProperty("Range", "bytes=" + startByte + "-");

            if (conn.getResponseCode() == 206) {
                end = new String(conn.getInputStream().readAllBytes());
            }
            conn.disconnect();
        } catch (Exception e) {
            e.printStackTrace();
        }

        Matcher versionMatcher = Pattern.compile("\"version\": (\\d+)").matcher(end);
        if (versionMatcher.find()) {
            return Double.parseDouble(versionMatcher.group(1));
        }
        return -1;
    }
}
