package julianh06.wynnextras.features.badges;

public class BadgeProfile {
    public String uuid;
    public String username;
    public String selectedIconId = BadgeCatalog.DEFAULT_ICON_ID;
    public String selectedColorId = BadgeCatalog.DEFAULT_COLOR_ID;

    public BadgeProfile() {}

    public BadgeProfile(String uuid, String username, String selectedIconId, String selectedColorId) {
        this.uuid = normalizeUuid(uuid);
        this.username = username;
        this.selectedIconId = selectedIconId;
        this.selectedColorId = selectedColorId;
    }

    public static BadgeProfile defaultProfile() {
        return new BadgeProfile(null, null, BadgeCatalog.DEFAULT_ICON_ID, BadgeCatalog.DEFAULT_COLOR_ID);
    }

    public void sanitize(boolean enforceUnlocks) {
        if (!BadgeCatalog.isKnownIcon(selectedIconId) || (enforceUnlocks && !BadgeCatalog.isUnlocked(BadgeCatalog.icon(selectedIconId)))) {
            selectedIconId = BadgeCatalog.DEFAULT_ICON_ID;
        }
        if (!BadgeCatalog.isKnownColor(selectedColorId) || (enforceUnlocks && !BadgeCatalog.isUnlocked(BadgeCatalog.color(selectedColorId)))) {
            selectedColorId = BadgeCatalog.DEFAULT_COLOR_ID;
        }
        uuid = normalizeUuid(uuid);
    }

    public static String normalizeUuid(String uuid) {
        if (uuid == null) return null;
        String normalized = uuid.replace("-", "").toLowerCase();
        return normalized.matches("[0-9a-f]{32}") ? normalized : null;
    }
}