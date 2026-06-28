package julianh06.wynnextras.features.qol;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import julianh06.wynnextras.config.WynnExtrasConfig;
import julianh06.wynnextras.core.WynnExtras;
import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.block.entity.BeaconBlockEntityRenderer;
import net.minecraft.client.render.command.OrderedRenderCommandQueue;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.Vec3d;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

public class WarBeacon {
    private static final int BEAM_RGB = 0x32FF32;
    private static final double BEAM_BASE_Y = -200.0;
    private static final String TERRITORY_LIST_URL = "https://api.wynncraft.com/v3/guild/list/territory";
    private static final long TERRITORY_FETCH_RETRY_MS = 60_000L;
    private static final HttpClient HTTP_CLIENT = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(5))
            .build();

    private static boolean loggedRenderFailure = false;
    private static boolean loggedTerritoryFailure = false;
    private static Vec3d testBeaconTarget = null;
    private static final AtomicBoolean fetchingTerritories = new AtomicBoolean(false);
    private static volatile long lastTerritoryFetchAttempt = 0L;
    private static volatile Map<String, TerritoryCenter> territoryCenters = Map.of();

    public static void register() {
        WorldRenderEvents.END_MAIN.register(event -> {
            if (event.commandQueue() == null) return;
            render(event.matrices(), event.gameRenderer().getCamera(), event.commandQueue(), event.worldState().time);
        });
        fetchTerritoriesIfNeeded();
    }

    private static void render(MatrixStack matrices, Camera camera, OrderedRenderCommandQueue orderedRenderCommandQueue, float tickProgress) {
        try {
            if (testBeaconTarget != null) {
                drawBeam(matrices, camera, orderedRenderCommandQueue, tickProgress, testBeaconTarget.x, testBeaconTarget.y, testBeaconTarget.z);
                return;
            }

            if (!WynnExtrasConfig.INSTANCE.warBeaconEnabled) return;
            if (AttackTimer.soonestTerritory == null) return;

            TerritoryCenter center = territoryCenters.get(AttackTimer.soonestTerritory);
            if (center == null) {
                fetchTerritoriesIfNeeded();
                return;
            }

            drawBeam(matrices, camera, orderedRenderCommandQueue, tickProgress, center.x, BEAM_BASE_Y, center.z);
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

    private static void fetchTerritoriesIfNeeded() {
        if (!territoryCenters.isEmpty()) return;
        long now = System.currentTimeMillis();
        if (now - lastTerritoryFetchAttempt < TERRITORY_FETCH_RETRY_MS) return;
        if (!fetchingTerritories.compareAndSet(false, true)) return;
        lastTerritoryFetchAttempt = now;

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(TERRITORY_LIST_URL))
                .timeout(Duration.ofSeconds(8))
                .GET()
                .build();

        HTTP_CLIENT.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenApply(response -> {
                    if (response.statusCode() != 200) {
                        throw new IllegalStateException("Territory API returned " + response.statusCode());
                    }
                    return parseTerritoryCenters(response.body());
                })
                .thenAccept(centers -> {
                    if (!centers.isEmpty()) {
                        territoryCenters = centers;
                        loggedTerritoryFailure = false;
                    }
                })
                .exceptionally(exception -> {
                    logTerritoryFailure(exception);
                    return null;
                })
                .whenComplete((ignored, exception) -> fetchingTerritories.set(false));
    }

    private static Map<String, TerritoryCenter> parseTerritoryCenters(String body) {
        JsonObject territories = JsonParser.parseString(body).getAsJsonObject();
        Map<String, TerritoryCenter> centers = new HashMap<>();

        for (Map.Entry<String, JsonElement> entry : territories.entrySet()) {
            JsonObject location = entry.getValue().getAsJsonObject().getAsJsonObject("location");
            if (location == null) continue;

            JsonArray start = location.getAsJsonArray("start");
            JsonArray end = location.getAsJsonArray("end");
            if (start == null || end == null || start.size() < 2 || end.size() < 2) continue;

            double x = (start.get(0).getAsDouble() + end.get(0).getAsDouble()) / 2.0;
            double z = (start.get(1).getAsDouble() + end.get(1).getAsDouble()) / 2.0;
            centers.put(entry.getKey(), new TerritoryCenter(x, z));
        }

        return centers;
    }

    private static void logRenderFailure(Exception exception) {
        if (loggedRenderFailure) return;

        loggedRenderFailure = true;
        WynnExtras.LOGGER.warn("Failed to render war beacon", exception);
    }

    private static void logTerritoryFailure(Throwable exception) {
        if (loggedTerritoryFailure) return;

        loggedTerritoryFailure = true;
        WynnExtras.LOGGER.warn("Failed to load war beacon territory data", exception);
    }

    private record TerritoryCenter(double x, double z) {}
}