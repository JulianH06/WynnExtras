package julianh06.wynnextras.features.spellhider;

import com.google.common.reflect.TypeToken;
import com.google.gson.*;
import com.wynntils.mc.extension.EntityExtension;
import julianh06.wynnextras.annotations.WEModule;
import julianh06.wynnextras.core.WynnExtras;
import julianh06.wynnextras.event.SetEntityDataEvent;
import julianh06.wynnextras.utils.EntityUtils;
import julianh06.wynnextras.utils.ItemUtils;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.entity.Entity;
import net.minecraft.entity.decoration.DisplayEntity;
import net.minecraft.item.Items;
import net.neoforged.bus.api.SubscribeEvent;
import org.jetbrains.annotations.NotNull;
import org.joml.Vector3f;

import java.io.IOException;
import java.lang.reflect.Type;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

@WEModule
public class SpellHider {
    public static final String RESOURCES_PATH = "assets/wynnextras/default_packages/spell_hider/";
    public static final Gson GSON = new GsonBuilder()
            .setPrettyPrinting()
            .registerTypeAdapter(SpellModifiers.class, (JsonDeserializer<SpellModifiers>) (json, typeOfT, ctx) -> {
                if (json.isJsonObject()) {
                    JsonObject obj = json.getAsJsonObject();
                    SpellModifiers modifiers = new SpellModifiers();
                    JsonObject valuesMap = obj.get("values").getAsJsonObject();

                    for (Map.Entry<String, JsonElement> entry : valuesMap.entrySet()) {
                        String key = entry.getKey();
                        JsonElement value = entry.getValue();

                        SpellModifier modifier = SpellModifier.from(key);
                        if (modifier == null) continue;

                        // Parse value based on modifier type
                        if (modifier == SpellModifier.VISIBLE) {
                            modifiers.set(modifier, value.getAsBoolean());
                        } else if (modifier == SpellModifier.SCALE) {
                            String vecStr = value.getAsString();
                            modifiers.set(modifier, modifier.parseValue(vecStr, Vector3f.class));
                        }
                    }

                    return modifiers;
                } else {
                    throw new JsonParseException("Unexpected JSON for IdentificationData: " + json);
                }
            })
            .registerTypeAdapter(SpellNamespace.class, (JsonDeserializer<SpellNamespace>) (json, typeOfT, ctx) -> {
                if (json.isJsonObject()) {
                    return SpellNamespace.from(json.getAsString());
                } else {
                    throw new JsonParseException("Unexpected JSON for IdentificationData: " + json);
                }
            })
            .registerTypeAdapter(new TypeToken<@NotNull Map<SpellNamespace, SpellModifiers>>(){}.getType(),
                    (JsonDeserializer<Map<SpellNamespace, SpellModifiers>>) (json, typeOfT, ctx) -> {
                        Map<SpellNamespace, SpellModifiers> map = new HashMap<>();
                        JsonObject obj = json.getAsJsonObject();

                        for (Map.Entry<String, JsonElement> entry : obj.entrySet()) {
                            SpellNamespace namespace = SpellNamespace.from(entry.getKey());
                            SpellModifiers modifiers = ctx.deserialize(entry.getValue(), SpellModifiers.class);
                            map.put(namespace, modifiers);
                        }

                        return map;
                    })
            .create();

    private static final Path MODIFIERS_PATH = FabricLoader.getInstance()
            .getConfigDir()
            .resolve("wynnextras")
            .resolve("spell_modifiers.json");


    // first path and hash are added when the game launches as textures are loaded
    // then when the entity is first seen in-game its model is added based on the path
    //      and its namespace is added from the hash
    // that mean we have model -> namespace ability
    private static final Map<Integer, SpellData> byHash = new HashMap<>();
    private static final Map<String, SpellData> byPath = new HashMap<>();
    private static final Map<Integer, SpellData> byModel = new HashMap<>();
    private static final Map<String, Set<SpellData>> byName = new HashMap<>();
    public static final Map<SpellNamespace, SpellModifiers> modifiersMap = new HashMap<>();

    public static void putHash(String filePath, int hash) {
        SpellData data = new SpellData(filePath, hash);
        byHash.put(hash, data);
        byPath.put(filePath, data);
    }

    public static SpellData getFromPath(String filePath) {
        return byPath.get(filePath);
    }

    public static SpellData getFromHash(int hash) {
        return byHash.get(hash);
    }

    public static SpellData getFromModel(int model) {
        return byModel.get(model);
    }

    public static Set<SpellData> getFromName(String name) {
        return byName.get(name);
    }

    public static void addModel(String path, Float modelF) {
        if (byModel.containsKey(modelF.intValue())) return;
        SpellData existing = byPath.get(path);
        if (existing != null) {
            existing.setCustomModelData(modelF.intValue());
            byModel.put(modelF.intValue(), existing);
        }
    }

    public static void editNameOfPath(String path, SpellNamespace namespace) {
        String newName = namespace.getFQName();
        SpellData existing = byPath.get(path);
        if (existing != null) {
            String oldName = existing.getFQName();
            byName.get(oldName).remove(existing);

            existing.setFQName(newName);
            byName.computeIfAbsent(newName, (k) -> new HashSet<>()).add(existing);
        }
    }

    public static void addName(String path, String FQName) {
        SpellData existing = byPath.get(path);
        if (existing != null) {
            existing.setFQName(FQName);
            byName.computeIfAbsent(FQName, (k) -> new HashSet<>()).add(existing);
        }
    }

    public static SpellModifiers getModifiers(DisplayEntity.ItemDisplayEntity display) {
        if (display.getItemStack().getItem() != Items.OAK_BOAT) return null;
        Float model = ItemUtils.getFirsCustomModelDataFloat(display.getItemStack());
        if (model == null) return null;
        SpellData data = SpellHider.byModel.get(model.intValue());
        if (data == null) return null;
        SpellNamespace nameSpace = data.getNamespace();
        if (nameSpace == null || nameSpace.isEmpty()) return null;
        SpellModifiers modifiers = modifiersMap.get(nameSpace);
        if (modifiers == null) modifiers = SpellProfiles.getModifiers(nameSpace);
        return modifiers;
    }

    public static boolean modify(SpellNamespace nameSpace, SpellModifier type, Object value) {
        SpellModifiers modifiers = modifiersMap.compute(nameSpace, (k, v) -> v == null ? new SpellModifiers() : v);
        return modifiers.set(type, value);
    }

    public static Map<SpellNamespace, SpellModifiers> getAllModifiers() {
        return modifiersMap;
    }

    public static String getAllModifiersAsDisplay() {
        StringBuilder sb = new StringBuilder();
        for (SpellNamespace namespace : modifiersMap.keySet()) {
            sb.append(namespace.getFQName()).append("\n");
            SpellModifiers modifiers = modifiersMap.get(namespace);
            for (Map.Entry<SpellModifier, Object> entry : modifiers.getAll().entrySet()) {
                SpellModifier modifier = entry.getKey();
                Object value = entry.getValue();
                sb.append("   ").append(modifier.name()).append(" -> ").append(value == null ? "null" : value.toString()).append("\n");
            }
        }
        if (sb.isEmpty()) sb.append("no modifiers");
        return sb.toString();
    }

    @SubscribeEvent
    public void onEntitySetData(SetEntityDataEvent event) {
        if (MinecraftClient.getInstance().world == null) return;
        Entity entity = MinecraftClient.getInstance().world.getEntityById(event.getId());
        if (entity instanceof DisplayEntity.ItemDisplayEntity display) {
            SpellModifiers modifiers = getModifiers(display);

            boolean hide = SpellProfiles.isEverythingInvisible();
            if (modifiers != null) {
                if (Boolean.FALSE.equals(modifiers.get(SpellModifier.VISIBLE))) {
                    hide = true;
                }

                Vector3f scale = modifiers.get(SpellModifier.SCALE);
                if (scale != null) {
                    EntityUtils.setScale(display, scale);
                }
            }

            if (hide) {
                ((EntityExtension) entity).setRendered(false);
            }
        }
    }

    public static int hashNativeImage(NativeImage image) {
        if (image == null) return 0;

        int result = 1;
        int width = image.getWidth();
        int height = image.getHeight();

        // Include dimensions
        result = 31 * result + width;
        result = 31 * result + height;

        // Hash ALL pixels
        int[] pixels = image.copyPixelsArgb();
        for (int pixel : pixels) {
            result = 31 * result + pixel;
        }

        return result;
    }

    public static void loadModifiers() {
        try {
            if (Files.exists(MODIFIERS_PATH)) {
                String json = Files.readString(MODIFIERS_PATH);
                Type mapType = new TypeToken<@NotNull Map<SpellNamespace, SpellModifiers>>() {}.getType();
                Map<SpellNamespace, SpellModifiers> modifiers = GSON.fromJson(json, mapType);
                modifiersMap.putAll(modifiers);
            }
        } catch (IOException e) {
            WynnExtras.LOGGER.error("[WynnExtras] Failed to load spell modifiers: " + e.getMessage());
        }
    }

    public static void resetRenderedState() {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.world == null) return;
        client.world.getEntities().forEach(entity -> {
            if (entity instanceof DisplayEntity.ItemDisplayEntity) {
                ((EntityExtension) entity).setRendered(true);
            }
        });
    }

    public static void saveModifiers() {
        try {
            Files.createDirectories(MODIFIERS_PATH.getParent());
            Files.writeString(MODIFIERS_PATH, GSON.toJson(SpellHider.modifiersMap));
        } catch (IOException e) {
            WynnExtras.LOGGER.error("[WynnExtras] Failed to save spell modifiers: " + e.getMessage());
        }
    }
}
