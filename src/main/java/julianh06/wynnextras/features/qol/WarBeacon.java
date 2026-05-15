package julianh06.wynnextras.features.qol;

import com.wynntils.core.components.Models;
import com.wynntils.models.territories.profile.TerritoryProfile;
import com.wynntils.utils.colors.CustomColor;
import julianh06.wynnextras.config.WynnExtrasConfig;
import julianh06.wynnextras.event.RenderWorldEvent;
import julianh06.wynnextras.event.api.WEEventBus;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.block.entity.BeaconBlockEntityRenderer;
import net.minecraft.client.render.command.OrderedRenderCommandQueue;
import net.minecraft.client.util.math.MatrixStack;
import net.neoforged.bus.api.SubscribeEvent;
import org.joml.Matrix4f;

public class WarBeacon {

    public static void register() {
        WEEventBus.registerEventListener(new WarBeacon());
    }

    @SubscribeEvent
    public void onRenderWorld(RenderWorldEvent event) {
        if (!WynnExtrasConfig.INSTANCE.warBeaconEnabled) return;
        if (AttackTimer.soonestTerritory == null) return;

        try {
            TerritoryProfile profile = Models.Territory.getTerritoryProfile(AttackTimer.soonestTerritory);
            if (profile == null) return;

            double mx = (profile.getStartX() + profile.getEndX()) / 2.0;
            double mz = (profile.getStartZ() + profile.getEndZ()) / 2.0;
            drawBeam(event.matrices, event.camera, event.orderedRenderCommandQueue, mx, mz, 0x3296FF32, AttackTimer.soonestTerritory);
        } catch (Exception ignored) {}
    }

    private static void drawBeam(MatrixStack matrices, Camera camera, OrderedRenderCommandQueue orderedRenderCommandQueue,
                                  double targetX, double targetZ, int color, String title) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (camera == null || orderedRenderCommandQueue == null || mc.player == null) return;

        double realDX = targetX - camera.getCameraPos().x;
// Wir nehmen die Spielerhöhe als Basis oder einen festen Wert (z.B. 64),
// damit der Strahl nicht im Nirvana startet.
        double targetY = 64.0;
        double realDY = targetY - camera.getCameraPos().y;
        double realDZ = targetZ - camera.getCameraPos().z;

        double dist = Math.sqrt(realDX * realDX + realDZ * realDZ);

// Distanz-Skalierung für weit entfernte Beacons (wie in Wynntils)
        double beaconDX = realDX;
        double beaconDZ = realDZ;
        int maxDistance = mc.options.getClampedViewDistance() * 16;
        if (dist > maxDistance) {
            double scale = maxDistance / dist;
            beaconDX *= scale;
            beaconDZ *= scale;
        }

// Alpha berechnen (Fade-out wenn nah dran)
        float alpha = 1.0f;
        if (dist <= 7.0) {
            alpha = (float) Math.max(0.0, (dist - 2.0) / 5.0);
        }

// Farbe mit Alpha kombinieren
// color ist 0xRRGGBB, wir müssen den Alpha-Wert oben drauf packen
        int finalColor = CustomColor.fromHexString("FFFFFF").asInt();

        matrices.push();
// Wir verschieben den Beacon an seine Position
        matrices.translate(beaconDX, realDY, beaconDZ);

        try {
            float partialTick = camera.getLastTickProgress();
            float animationTime = (mc.world.getTime() % 40) + partialTick;

            BeaconBlockEntityRenderer.renderBeam(matrices, orderedRenderCommandQueue, BeaconBlockEntityRenderer.BEAM_TEXTURE,
                    partialTick, 1.0F, (int) animationTime, 10000,
                    1024, finalColor, 10.15F); // 0.15F ist der Radius des Strahls
        } catch (Exception ignored) {}
        matrices.pop();
    }
}
