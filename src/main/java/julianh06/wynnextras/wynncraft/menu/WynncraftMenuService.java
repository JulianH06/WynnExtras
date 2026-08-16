package julianh06.wynnextras.wynncraft.menu;

import julianh06.wynnextras.utils.enums.WEProfessionType;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.ingame.HandledScreen;

import java.util.Locale;
import java.util.Optional;

public final class WynncraftMenuService {
    private static final String ACCOUNT_BANK_TITLE = "\uDAFF\uDFF0\uE00F\uDAFF\uDF68\uF000";
    private static final String CHARACTER_BANK_TITLE = "\uDAFF\uDFF0\uE00F\uDAFF\uDF68\uF001";
    private static final String MISC_BUCKET_TITLE = "\uDAFF\uDFF0\uE00F\uDAFF\uDF68\uF004";
    private static final String BOOKSHELF_TITLE = "\uDAFF\uDFF0\uE00F\uDAFF\uDF68\uF005";
    private static final String CHARACTER_INFO_TITLE = "\uDAFF\uDFDC\uE003";
    private static final String TOME_TITLE = "\uDAFF\uDFDB\uE005";
    private static final String CHARACTER_SELECTION_TITLE = "\uDAFF\uDFD5\uE01F";
    private static final String ITEM_IDENTIFIER_TITLE = "\uDAFF\uDFF8\uE018";
    private static final String CRAFTING_STATION_TITLE_PREFIX = "\uDAFF\uDFF8\uE053\uDAFF\uDF80";

    private WynncraftMenuService() {}

    public static MenuType currentType() {
        try {
            MinecraftClient client = MinecraftClient.getInstance();
            if (client == null || !(client.currentScreen instanceof HandledScreen<?> screen)) return MenuType.NONE;
            return detect(screen.getTitle().getString());
        } catch (Throwable ignored) {
            return MenuType.UNKNOWN;
        }
    }

    public static boolean isCurrent(MenuType type) {
        return type != null && currentType() == type;
    }

    public static boolean isCurrentAny(MenuType... types) {
        MenuType current = currentType();
        if (types == null) return false;
        for (MenuType type : types) if (current == type) return true;
        return false;
    }

    public static Optional<WEProfessionType> currentCraftingProfession() {
        try {
            String title = currentTitle();
            return title == null ? Optional.empty() : Optional.ofNullable(craftingProfession(title));
        } catch (Throwable ignored) {
            return Optional.empty();
        }
    }

    static MenuType detect(String title) {
        if (title == null || title.isEmpty()) return MenuType.UNKNOWN;
        if (ACCOUNT_BANK_TITLE.equals(title)) return MenuType.ACCOUNT_BANK;
        if (CHARACTER_BANK_TITLE.equals(title)) return MenuType.CHARACTER_BANK;
        if (MISC_BUCKET_TITLE.equals(title)) return MenuType.MISC_BUCKET;
        if (BOOKSHELF_TITLE.equals(title)) return MenuType.BOOKSHELF;
        if (CHARACTER_INFO_TITLE.equals(title)) return MenuType.CHARACTER_INFO;
        if (TOME_TITLE.equals(title)) return MenuType.TOME;
        if (CHARACTER_SELECTION_TITLE.equals(title)) return MenuType.CLASS_SELECTION;
        if (ITEM_IDENTIFIER_TITLE.equals(title)) return MenuType.ITEM_IDENTIFIER;
        if (craftingProfession(title) != null) return MenuType.CRAFTING_STATION;

        String normalized = title.replaceAll("§[0-9a-fk-or]", "").trim().toLowerCase(Locale.ROOT);
        if (normalized.equals("trade market")) return MenuType.TRADE_MARKET;
        if (normalized.equals("party finder")) return MenuType.PARTY_FINDER;
        if (normalized.contains("raid rewards")) return MenuType.RAID_REWARD;
        if (normalized.equals("ability tree")) return MenuType.ABILITY_TREE;
        return MenuType.UNKNOWN;
    }

    private static String currentTitle() {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client == null || !(client.currentScreen instanceof HandledScreen<?> screen)) return null;
        return screen.getTitle().getString();
    }

    private static WEProfessionType craftingProfession(String title) {
        if (!title.startsWith(CRAFTING_STATION_TITLE_PREFIX)
                || title.length() != CRAFTING_STATION_TITLE_PREFIX.length() + 1) return null;
        return switch (title.charAt(CRAFTING_STATION_TITLE_PREFIX.length())) {
            case '\uF041' -> WEProfessionType.ALCHEMISM;
            case '\uF042' -> WEProfessionType.ARMOURING;
            case '\uF043' -> WEProfessionType.COOKING;
            case '\uF044' -> WEProfessionType.JEWELING;
            case '\uF045' -> WEProfessionType.SCRIBING;
            case '\uF046' -> WEProfessionType.TAILORING;
            case '\uF047' -> WEProfessionType.WEAPONSMITHING;
            case '\uF048' -> WEProfessionType.WOODWORKING;
            default -> null;
        };
    }
}
