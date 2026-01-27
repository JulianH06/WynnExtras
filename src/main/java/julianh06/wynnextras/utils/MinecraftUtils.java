package julianh06.wynnextras.utils;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.Entity;

public class MinecraftUtils {
    public static ClientPlayerEntity localPlayer() {
        ClientPlayerEntity localPlayer = localPlayerOrNull();
        if (localPlayer == null) {
            throw new IllegalStateException("Local player is null, this should not happen!");
        }
        return localPlayer;
    }

    public static ClientPlayerEntity localPlayerOrNull() {
        return MinecraftClient.getInstance().player;
    }

    public static boolean isLocalPlayer(Entity entity) {
        return entity != null && entity.equals(localPlayerOrNull());
    }

    public static boolean localPlayerExists() {
        return localPlayerOrNull() != null;
    }

    public static ClientWorld localWorld() {
        ClientWorld localWorld = localWorldOrNull();
        if (localWorld == null) {
            throw new IllegalStateException("Local world is null, this should not happen!");
        }
        return localWorld;
    }

    public static ClientWorld localWorldOrNull() {
        return MinecraftClient.getInstance().world;
    }

    public static boolean localWorldExists() {
        return localWorldOrNull() != null;
    }

    // TODO: This is temporary and should be replaced with a wrapper around the network handler
    public static ClientPlayNetworkHandler localNetworkHandler() {
        ClientPlayNetworkHandler localNetworkHandler = localNetworkHandlerOrNull();
        if (localNetworkHandler == null) {
            throw new IllegalStateException("Local network handler is null, this should not happen!");
        }
        return localNetworkHandler;
    }

    public static ClientPlayNetworkHandler localNetworkHandlerOrNull() {
        return MinecraftClient.getInstance().getNetworkHandler();
    }

    // TODO: This is temporary and should be replaced with a wrapper around the MinecraftClient instance
    public static MinecraftClient mc() {
        return MinecraftClient.getInstance();
    }
}
