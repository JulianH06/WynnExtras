package julianh06.wynnextras.features.qol;

import com.wynntils.core.components.Models;
import com.wynntils.models.territories.profile.TerritoryProfile;
import julianh06.wynnextras.config.WynnExtrasConfig;
import julianh06.wynnextras.event.RenderWorldEvent;
import julianh06.wynnextras.event.api.WEEventBus;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.block.entity.BeaconBlockEntityRenderer;
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
        if (AttackTimerMenu.soonestTerritory == null) return;

        try {
            TerritoryProfile profile = Models.Territory.getTerritoryProfile(AttackTimerMenu.soonestTerritory);
            if (profile == null) return;

            double mx = (profile.getStartX() + profile.getEndX()) / 2.0;
            double mz = (profile.getStartZ() + profile.getEndZ()) / 2.0;
            drawBeam(event.matrices, event.camera, event.vertexConsumerProvider, mx, mz, 0x3296FF32, AttackTimerMenu.soonestTerritory);
        } catch (Exception ignored) {}
    }

    private static void drawBeam(MatrixStack matrices, Camera camera, VertexConsumerProvider consumers,
                                  double targetX, double targetZ, int color, String title) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (camera == null || consumers == null || mc.player == null) return;

        double beaconDX = targetX - camera.getCameraPos().x;
        double beaconDY = -300;
        double beaconDZ = targetZ - camera.getCameraPos().z;
        double titleDX = beaconDX, titleDZ = beaconDZ;
        double distSq = beaconDX * beaconDX + beaconDZ * beaconDZ;
        double dist = Math.sqrt(distSq);

        int maxDistance = mc.options.getClampedViewDistance() * 15;
        if (dist > maxDistance) {
            double scale = maxDistance / dist;
            beaconDX *= scale;
            beaconDZ *= scale;
        }
        if (distSq > 144) {
            titleDX *= 12 / dist;
            titleDZ *= 12 / dist;
        }

        matrices.push();
        matrices.translate(beaconDX, beaconDY, beaconDZ);
        try {
            BeaconBlockEntityRenderer.renderBeam(matrices, null, BeaconBlockEntityRenderer.BEAM_TEXTURE,
                    camera.getLastTickProgress(), 1.0F, camera.getFocusedEntity().age, 0,
                    BeaconBlockEntityRenderer.MAX_BEAM_HEIGHT, color, 0.2F);
        } catch (Exception ignored) {}
        matrices.pop();

        matrices.push();
        matrices.translate(titleDX, 0, titleDZ);
        matrices.multiply(camera.getRotation());
        matrices.scale(-0.025F, -0.025F, 0.025F);

        Matrix4f matrix4f = matrices.peek().getPositionMatrix();
        TextRenderer tr = mc.textRenderer;
        int bgColor = 500_000_000;
        float titleXOffset = -tr.getWidth(title) / 2f;
        tr.draw(title, titleXOffset, 0f, 0xffffff, false, matrix4f, consumers, TextRenderer.TextLayerType.SEE_THROUGH, bgColor, 255);
        String distText = "§e" + Math.round(dist) + "m";
        float distXOffset = -tr.getWidth(distText) / 2f;
        tr.draw(distText, distXOffset, -10f, 0xffffff, false, matrix4f, consumers, TextRenderer.TextLayerType.SEE_THROUGH, bgColor, 255);
        matrices.pop();
    }
}
