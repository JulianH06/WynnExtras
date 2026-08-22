package julianh06.wynnextras.features.qol;

import julianh06.wynnextras.config.WynnExtrasConfig;
import julianh06.wynnextras.core.WynnExtras;
import julianh06.wynnextras.wynncraft.state.TerritoryState;
import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.block.entity.BeaconBlockEntityRenderer;
import net.minecraft.client.render.command.OrderedRenderCommandQueue;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.Vec3d;

public class WarBeacon {
    private static final int BEAM_RGB = 0x32FF32;
    private static final double BEAM_BASE_Y = -200.0;

    private static boolean loggedRenderFailure = false;
    private static Vec3d testBeaconTarget = null;

    public static void register() {
        WorldRenderEvents.END_MAIN.register(event -> {
            if (event.commandQueue() == null) return;
            render(event.matrices(), event.gameRenderer().getCamera(), event.commandQueue(), event.worldState().time);
        });
        TerritoryState.initialize();
    }

    private static void render(MatrixStack matrices, Camera camera, OrderedRenderCommandQueue orderedRenderCommandQueue, float tickProgress) {
        try {
            if (testBeaconTarget != null) {
                drawBeam(matrices, camera, orderedRenderCommandQueue, tickProgress, testBeaconTarget.x, testBeaconTarget.y, testBeaconTarget.z);
                return;
            }

            if (!WynnExtrasConfig.INSTANCE.warBeaconEnabled) return;
            if (AttackTimer.soonestTerritory == null) return;

            TerritoryState.TerritoryCenter center = TerritoryState.center(AttackTimer.soonestTerritory).orElse(null);
            if (center == null) return;

            drawBeam(matrices, camera, orderedRenderCommandQueue, tickProgress, center.x(), BEAM_BASE_Y, center.z());
        } catch (Exception exception) {
            logRenderFailure(exception);
        }
    }

    private static void drawBeam(MatrixStack matrices, Camera camera, OrderedRenderCommandQueue orderedRenderCommandQueue,
                                 float tickProgress, double targetX, double targetY, double targetZ) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null || mc.world == null) return;
        if (camera == null || orderedRenderCommandQueue == null) return;

        Vec3d cameraPos = camera.getCameraPos();

        double dx = targetX - cameraPos.x;
        double dy = targetY - cameraPos.y;
        double dz = targetZ - cameraPos.z;

        double distance = Math.sqrt(dx * dx + dz * dz);
        int maxDistance = mc.options.getViewDistance().getValue() * 16;
        if (distance > maxDistance) {
            double scale = maxDistance / distance;
            dx *= scale;
            dz *= scale;
        }

        float alpha = 1.0f;
        if (distance <= 7.0) {
            alpha = (float) Math.max(0.0, Math.min(1.0, (distance - 2.0) / 5.0));
        }

        int color = ((int) (alpha * 255.0f) << 24) | BEAM_RGB;
        float animationTime = (mc.world.getTime() % 40) + tickProgress;

        matrices.push();
        try {
            matrices.translate(dx, dy, dz);
            BeaconBlockEntityRenderer.renderBeam(matrices, orderedRenderCommandQueue, BeaconBlockEntityRenderer.BEAM_TEXTURE,
                    tickProgress, animationTime, 0, 2048, color, 0.166F, 0.33F);
        } finally {
            matrices.pop();
        }
    }

    private static void logRenderFailure(Exception exception) {
        if (loggedRenderFailure) return;

        loggedRenderFailure = true;
        WynnExtras.LOGGER.warn("Failed to render war beacon", exception);
    }

}
