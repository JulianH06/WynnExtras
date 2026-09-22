package julianh06.wynnextras.features.leaderboardviewer;

import com.mojang.authlib.GameProfile;
import julianh06.wynnextras.features.profileviewer.PVScreen;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.util.DefaultSkinHelper;
import net.minecraft.util.Identifier;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

public final class LeaderboardPlayerSkinService {
    private static final Map<UUID, CompletableFuture<Identifier>> CACHE = new ConcurrentHashMap<>();

    private LeaderboardPlayerSkinService() {
    }

    public static CompletableFuture<Identifier> fetchSkin(String uuid, String name) {
        UUID profileId;
        try {
            profileId = UUID.fromString(uuid);
        } catch (Exception ignored) {
            return CompletableFuture.completedFuture(DefaultSkinHelper.getSteve().body().texturePath());
        }

        return CACHE.computeIfAbsent(profileId, id -> CompletableFuture
                .supplyAsync(() -> {
                    try {
                        PVScreen.SkinData skin = PVScreen.fetchSkin(id);
                        return PVScreen.createProfileWithSkin(id, name == null ? id.toString() : name, skin);
                    } catch (Exception exception) {
                        throw new RuntimeException(exception);
                    }
                })
                .thenCompose(LeaderboardPlayerSkinService::loadTexture)
                .exceptionally(error -> DefaultSkinHelper.getSteve().body().texturePath()));
    }

    private static CompletableFuture<Identifier> loadTexture(GameProfile profile) {
        return MinecraftClient.getInstance().getSkinProvider().fetchSkinTextures(profile)
                .thenApply(skin -> skin
                        .map(textures -> textures.body().texturePath())
                        .orElseGet(() -> DefaultSkinHelper.getSteve().body().texturePath()));
    }
}
