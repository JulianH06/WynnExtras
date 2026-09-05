package julianh06.wynnextras.features.badges;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import julianh06.wynnextras.core.WynnExtras;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.MinecraftClient;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;

public final class BadgeProfileData {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static BadgeProfile localProfile;

    private BadgeProfileData() {}

    public static BadgeProfile getLocalProfile() {
        if (localProfile == null) load();
        String oldIcon = localProfile.selectedIconId;
        String oldColor = localProfile.selectedColorId;
        localProfile.sanitize(true);
        if (!Objects.equals(localProfile.selectedIconId, oldIcon)
                || !Objects.equals(localProfile.selectedColorId, oldColor)) {
            saveLocalProfile();
        }
        return localProfile;
    }

    public static void setIcon(String iconId) {
        BadgeCatalog.BadgeIcon icon = BadgeCatalog.icon(iconId);
        if (!BadgeCatalog.isUnlocked(icon)) return;
        BadgeProfile profile = getLocalProfile();
        if (icon.id().equals(profile.selectedIconId)) return;
        profile.previousIconId = profile.selectedIconId;
        profile.selectedIconId = icon.id();
        profile.sanitize(true);
        save();
    }

    public static void setColor(String colorId) {
        BadgeCatalog.BadgeColor color = BadgeCatalog.color(colorId);
        if (!BadgeCatalog.isUnlocked(color)) return;
        BadgeProfile profile = getLocalProfile();
        if (!BadgeCatalog.isCompatible(BadgeCatalog.icon(profile.selectedIconId), color)) return;
        if (color.id().equals(profile.selectedColorId)) return;
        profile.previousColorId = profile.selectedColorId;
        profile.selectedColorId = color.id();
        save();
    }

    public static void load() {
        Path path = profilePath();
        if (path == null) {
            localProfile = BadgeProfile.defaultProfile();
            return;
        }
        try {
            if (Files.exists(path)) {
                localProfile = GSON.fromJson(Files.readString(path), BadgeProfile.class);
            }
        } catch (Exception e) {
            WynnExtras.LOGGER.error("[WynnExtras] Failed to load badge profile from {}, using defaults.", path, e);
            localProfile = BadgeProfile.defaultProfile();
        }
        if (localProfile == null) localProfile = BadgeProfile.defaultProfile();
        localProfile.uuid = currentUuid();
        localProfile.username = currentUsername();
        String oldIcon = localProfile.selectedIconId;
        String oldColor = localProfile.selectedColorId;
        localProfile.sanitize(true);
        if (!Objects.equals(localProfile.selectedIconId, oldIcon)
                || !Objects.equals(localProfile.selectedColorId, oldColor)) {
            saveLocalProfile();
        }
    }

    public static void save() {
        if (localProfile == null) load();
        saveLocalProfile();
    }

    private static void saveLocalProfile() {
        Path path = profilePath();
        if (path == null) return;
        try {
            Files.createDirectories(path.getParent());
            localProfile.uuid = currentUuid();
            localProfile.username = currentUsername();
            Files.writeString(path, GSON.toJson(localProfile));
        } catch (IOException e) {
            WynnExtras.LOGGER.error("[WynnExtras] Failed to save badge profile: " + e.getMessage());
        }
    }

    private static Path profilePath() {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc == null || mc.player == null) return null;
        String uuid = mc.player.getUuid().toString();
        return FabricLoader.getInstance().getConfigDir()
                .resolve("wynnextras")
                .resolve(uuid)
                .resolve("badge_profile.json");
    }

    private static String currentUuid() {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc == null || mc.player == null) return null;
        return mc.player.getUuidAsString().replace("-", "").toLowerCase();
    }

    private static String currentUsername() {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc == null || mc.player == null) return null;
        return mc.player.getGameProfile().name();
    }
}