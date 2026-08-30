package julianh06.wynnextras.features.badges;

import java.util.regex.Pattern;

public class BadgeProfile {
    // Compiled once. normalizeUuid runs per nametag per frame, and String.matches would
    // recompile the pattern on every call.
    private static final Pattern UUID_PATTERN = Pattern.compile("[0-9a-f]{32}");

    public String uuid;
    public String username;
    public String selectedIconId = BadgeCatalog.DEFAULT_ICON_ID;
    public String selectedColorId = BadgeCatalog.DEFAULT_COLOR_ID;
    public String previousIconId;
    public String previousColorId;

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
        if (!isUsableIcon(selectedIconId, enforceUnlocks)) {
            selectedIconId = isUsableIcon(previousIconId, enforceUnlocks)
                    ? previousIconId
                    : BadgeCatalog.DEFAULT_ICON_ID;
        }
        if (!isUsableColor(selectedColorId, enforceUnlocks)) {
            selectedColorId = isUsableColor(previousColorId, enforceUnlocks)
                    ? previousColorId
                    : BadgeCatalog.DEFAULT_COLOR_ID;
        }
        uuid = normalizeUuid(uuid);
    }

    private boolean isUsableIcon(String iconId, boolean enforceUnlocks) {
        return BadgeCatalog.isKnownIcon(iconId)
                && (!enforceUnlocks || BadgeCatalog.isUnlocked(BadgeCatalog.icon(iconId)));
    }

    private boolean isUsableColor(String colorId, boolean enforceUnlocks) {
        if (!BadgeCatalog.isKnownColor(colorId)) return false;
        BadgeCatalog.BadgeColor color = BadgeCatalog.color(colorId);
        return BadgeCatalog.isCompatible(BadgeCatalog.icon(selectedIconId), color)
                && (!enforceUnlocks || BadgeCatalog.isUnlocked(color));
    }

    public static String normalizeUuid(String uuid) {
        if (uuid == null) return null;
        String normalized = uuid.replace("-", "").toLowerCase();
        return UUID_PATTERN.matcher(normalized).matches() ? normalized : null;
    }
}