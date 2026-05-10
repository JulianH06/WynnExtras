package julianh06.wynnextras.features.misc;

import julianh06.wynnextras.core.WynnExtras;
import com.wynntils.utils.colors.CommonColors;
import com.wynntils.utils.render.RenderUtils;
import julianh06.wynnextras.config.WynnExtrasConfig;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.texture.NativeImageBackedTexture;
import net.minecraft.util.Identifier;

import java.io.FileInputStream;
import java.io.InputStream;

public class ClassSelectionPngOverlay {

    private static final double NPC_X = 18590;
    private static final double NPC_Y = 68.5;
    private static final double NPC_Z = -155;
    private static final double RANGE = 10;

    private static Identifier cachedTexture = null;
    private static String cachedPath = "";
    private static int imgWidth = 1;
    private static int imgHeight = 1;

    public static void register() {
        HudRenderCallback.EVENT.register(ClassSelectionPngOverlay::renderHud);
    }

    private static void renderHud(DrawContext ctx, RenderTickCounter tickCounter) {
        if (!WynnExtrasConfig.INSTANCE.customClassSelectionEnabled) return;

        String pngPath = WynnExtrasConfig.INSTANCE.customClassPngPath;
        if (pngPath == null || pngPath.isEmpty()) return;

        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null || mc.options.hudHidden) return;

        // Check position near NPC
        double px = mc.player.getX();
        double py = mc.player.getY();
        double pz = mc.player.getZ();

        if (Math.abs(px - NPC_X) > RANGE || Math.abs(py - NPC_Y) > RANGE || Math.abs(pz - NPC_Z) > RANGE) {
            return;
        }

        // Load or reload texture
        if (cachedTexture == null || !pngPath.equals(cachedPath)) {
            loadTexture(pngPath);
        }

        if (cachedTexture == null) return;

        // Render full-screen
        int width = mc.getWindow().getScaledWidth();
        int height = mc.getWindow().getScaledHeight();

        RenderUtils.drawTexturedRect(
                ctx, cachedTexture, CommonColors.WHITE,
                0, 0, width, height,
                0, 0, imgWidth, imgHeight,
                imgWidth, imgHeight
        );
    }

    private static void loadTexture(String path) {
        try {
            // Clean up old texture
            if (cachedTexture != null) {
                MinecraftClient.getInstance().getTextureManager().destroyTexture(cachedTexture);
                cachedTexture = null;
            }

            NativeImage image;
            try (InputStream stream = new FileInputStream(path)) {
                image = NativeImage.read(stream);
            }

            imgWidth = image.getWidth();
            imgHeight = image.getHeight();

            NativeImageBackedTexture texture = new NativeImageBackedTexture(() -> "wynnextras_custom_class_png", image);
            cachedTexture = Identifier.of("wynnextras", "custom_class_png_" + System.currentTimeMillis());
            MinecraftClient.getInstance().getTextureManager().registerTexture(cachedTexture, texture);
            cachedPath = path;
        } catch (Exception e) {
            WynnExtras.LOGGER.error("[WynnExtras] Failed to load custom class PNG: " + e.getMessage());
            cachedTexture = null;
            cachedPath = path; // Don't retry the same broken path every frame
        }
    }
}
