package julianh06.wynnextras.features.crafting.data;

import julianh06.wynnextras.core.WynnExtras;
import julianh06.wynnextras.config.WynnExtrasConfig;
import julianh06.wynnextras.config.simpleconfig.SimpleConfig;
import net.fabricmc.fabric.api.resource.ResourceManagerHelper;
import net.fabricmc.fabric.api.resource.SimpleSynchronousResourceReloadListener;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.texture.NativeImageBackedTexture;
import net.minecraft.resource.Resource;
import net.minecraft.resource.ResourceManager;
import net.minecraft.resource.ResourceType;
import net.minecraft.util.Identifier;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public final class MaterialTextureResolver implements SimpleSynchronousResourceReloadListener {
    private static final Identifier RELOAD_ID = Identifier.of(WynnExtras.MOD_ID, "material_textures");
    private static final MaterialTextureResolver INSTANCE = new MaterialTextureResolver();

    private final Map<Identifier, Identifier> resolved = new HashMap<>();
    private ResourceManager manager;

    private MaterialTextureResolver() {
    }

    public static void register() {
        ResourceManagerHelper.get(ResourceType.CLIENT_RESOURCES).registerReloadListener(INSTANCE);
    }

    public static Identifier resolve(Identifier serverTexture, Identifier fallbackTexture) {
        return INSTANCE.resolveInternal(serverTexture, fallbackTexture);
    }

    @Override
    public Identifier getFabricId() {
        return RELOAD_ID;
    }

    @Override
    public void reload(ResourceManager manager) {
        this.manager = manager;
        resolved.clear();
    }

    private Identifier resolveInternal(Identifier serverTexture, Identifier fallbackTexture) {
        if (!isDynamicEnabled()) {
            return fallbackTexture != null ? fallbackTexture : serverTexture;
        }
        if (serverTexture == null) {
            return fallbackTexture;
        }
        Identifier cached = resolved.get(serverTexture);
        if (cached != null) {
            return cached;
        }
        ResourceManager activeManager = getActiveManager();
        if (activeManager == null) {
            return fallbackTexture != null ? fallbackTexture : serverTexture;
        }
        Optional<Resource> resource = activeManager.getResource(serverTexture);
        if (resource.isEmpty()) {
            return fallbackTexture != null ? fallbackTexture : serverTexture;
        }
        Identifier resolvedTexture = normalizeIfNeeded(serverTexture, resource.get());
        resolved.put(serverTexture, resolvedTexture);
        return resolvedTexture;
    }

    private Identifier normalizeIfNeeded(Identifier serverTexture, Resource resource) {
        try (InputStream inputStream = resource.getInputStream()) {
            NativeImage image = NativeImage.read(inputStream);
            if (image == null) {
                return serverTexture;
            }
            int width = image.getWidth();
            int height = image.getHeight();
            if (height <= width || width <= 0) {
                image.close();
                return serverTexture;
            }
            NativeImage cropped = new NativeImage(width, width, false);
            for (int y = 0; y < width; y++) {
                for (int x = 0; x < width; x++) {
                    int color = image.getColorArgb(x, y);
                    cropped.setColorArgb(x, y, color);
                }
            }
            image.close();

            MinecraftClient client = MinecraftClient.getInstance();
            if (client == null) {
                return serverTexture;
            }
            Identifier dynamicId = buildDynamicId(serverTexture);
            NativeImageBackedTexture texture = new NativeImageBackedTexture(cropped);
            texture.setFilter(false, false);
            client.getTextureManager().registerTexture(dynamicId, texture);
            return dynamicId;
        } catch (IOException | RuntimeException ignored) {
            return serverTexture;
        }
    }

    private static Identifier buildDynamicId(Identifier serverTexture) {
        String sanitized = serverTexture.getPath().replace('/', '_');
        return Identifier.of(WynnExtras.MOD_ID,
                "dynamic/materials/" + serverTexture.getNamespace() + "/" + sanitized);
    }

    private ResourceManager getActiveManager() {
        if (manager != null) {
            return manager;
        }
        MinecraftClient client = MinecraftClient.getInstance();
        if (client != null) {
            return client.getResourceManager();
        }
        return null;
    }

    private boolean isDynamicEnabled() {
        WynnExtrasConfig config = SimpleConfig.getInstance(WynnExtrasConfig.class);
        return config == null || config.craftingDynamicTextures;
    }
}
