package julianh06.wynnextras.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import julianh06.wynnextras.config.gui.WynnExtrasConfigScreen;
import julianh06.wynnextras.sound.ModSounds;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.util.Formatting;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.function.Consumer;

public class WynnExtrasConfig {
    private static final Path CONFIG_PATH = FabricLoader.getInstance()
            .getConfigDir()
            .resolve("wynnextras")
            .resolve("wynnextras.json");

    private static final Gson GSON = new GsonBuilder()
            .setPrettyPrinting()
            .create();

    public static WynnExtrasConfig INSTANCE = new WynnExtrasConfig();

    private static final List<Consumer<WynnExtrasConfig>> saveListeners = new ArrayList<>();

    // ==================== PLAYER HIDER ====================
    public boolean playerHiderToggle = true;
    public int maxHideDistance = 3;
    public boolean hideAllPlayers = false;
    public boolean hideAllPlayersInWar = false;
    public List<String> hiddenPlayers = new ArrayList<>();

    // ==================== CHAT NOTIFIER ====================
    public List<String> notifierWords = new ArrayList<>();
    public int textDurationInMs = 2000;
    public TextColor textColor = TextColor.WHITE;
    public NotificationSound notificationSound = NotificationSound.EXPERIENCE_ORB;
    public float soundVolume = 10f;
    public float soundPitch = 100f;

    // ==================== CHAT NOTIFIER PREMADES ====================

    public Map<String, Boolean> premades;
    public boolean lostEye = true;
    public boolean oneGoo = true;
    public boolean twoGoo = true;
    public boolean soul = true;
    public boolean voidMatter = true;
    public boolean fourOutOfFiveVoidMatter = true;
    public boolean oneLightCrystal = true;
    public boolean twoLightCrystal = true;
    public boolean notgUpperPlatform = true;
    public boolean notgLowerPlatform = true;

    public void syncPremades() {
        if(premades == null) premades = new HashMap<>();

        premades.put("You feel like thousands of eyes|LOST EYE", lostEye);
        premades.put("+1 Slimey Goo|+1 Goo", oneGoo);
        premades.put("+2 Slimey Goo|+2 Goos", twoGoo);
        premades.put("Another Soul must be given!|NEXT SOUL", soul);
        premades.put("+1 Void Matter|+1 Void Matter", voidMatter);
        premades.put("The Void Holes have begun to desetabilize!|KILL THE VOID HOLES", fourOutOfFiveVoidMatter);
        premades.put("+1 Light Crystal|+1 Crystal", oneLightCrystal);
        premades.put("+2 Light Crystal|+2 Crystals", twoLightCrystal);
        premades.put("The players on the|UPPER PLATFORM SPAWNED", notgUpperPlatform);
        premades.put("A new platform has|LOWER PLATFORM SPAWNED", notgLowerPlatform);
    }

    // ==================== CHAT BLOCKER ====================
    public List<String> blockedWords = new ArrayList<>();

    // ==================== INVENTORY ====================
    public boolean toggleBankOverlay = true;
    public int wynntilsItemRarityBackgroundAlpha = 150;
    public boolean smoothScrollToggle = true;
    public boolean bankQuickToggle = true;
    public boolean darkmodeToggle = false;
    public int bankOverlayMaxRows = 3;
    public int bankOverlayMaxColumns = 3;
    public boolean showWeight = true;
    public boolean showScales = true;
    public boolean craftingHelperOverlay = true;
    public Boolean craftingPreviewOverlay = true;
    public boolean wynnventoryOverlay = true;

    // ==================== RAID ====================
    public boolean toggleRaidTimestamps = true;
    public boolean toggleRaidLootTracker = true;
    public boolean raidLootTrackerRenderInHud = true;
    public boolean raidLootTrackerRenderInInventory = true;
    public boolean raidLootTrackerRenderInChat = true;
    public boolean raidLootTrackerOnlyNearChest = true;
    public boolean raidLootTrackerCompact = false;
    public boolean raidLootTrackerShowSession = false;
    public int raidLootTrackerX = 5;
    public int raidLootTrackerY = 5;
    public List<String> raidLootTrackerHiddenLines = new ArrayList<>();
    public boolean raidLootTrackerBackground = true;
    public int tradeMarketOverlayX = 10;
    public int tradeMarketOverlayY = 10;
    public boolean tradeMarketOverlayBackground = true;
    public boolean toggleFastRequeue = true;
    public boolean provokeTimerToggle = false;
    public Map<String, Long> raidPBs = new HashMap<>();
    public boolean chiropTimer = false;

    // ==================== CHAT CLICK ====================
    public boolean chatClickOpensPV = true;

    // ==================== MISC ====================
    public boolean pvDarkmodeToggle = false;
    public boolean totemRangeVisualizerToggle = true;
    public float totemRange = 10f;
    public TextColor totemColor = TextColor.WHITE;
    public float eldritchCallRange = 15f;
    public TextColor eldritchCallColor = TextColor.WHITE;
    public TextColor provokeTimerColor = TextColor.WHITE;
    public boolean differentGUIScale = false;
    public int customGUIScale = 3;
    public boolean removeFrontPersonView = false;
    public boolean sourceOfTruthToggle = false;
    public boolean territoryEstimateToggle = false;

    // ==================== ENUMS ====================
    public enum TextColor {
        WHITE(Formatting.WHITE),
        BLACK(Formatting.BLACK),
        AQUA(Formatting.AQUA),
        RED(Formatting.RED),
        YELLOW(Formatting.YELLOW),
        BLUE(Formatting.BLUE),
        GREEN(Formatting.GREEN),
        DARK_BLUE(Formatting.DARK_BLUE),
        DARK_GREEN(Formatting.DARK_GREEN),
        DARK_AQUA(Formatting.DARK_AQUA),
        DARK_RED(Formatting.DARK_RED),
        DARK_PURPLE(Formatting.DARK_PURPLE),
        LIGHT_PURPLE(Formatting.LIGHT_PURPLE),
        GRAY(Formatting.GRAY),
        DARK_GRAY(Formatting.DARK_GRAY),
        GOLD(Formatting.GOLD);

        private final Formatting formatting;

        TextColor(Formatting formatting) {
            this.formatting = formatting;
        }

        public Formatting getFormatting() {
            return formatting;
        }

        public int getRGB() {
            Integer color = formatting.getColorValue();
            return color != null ? color : 0xFFFFFF;
        }
    }

    public enum NotificationSound {
        EXPERIENCE_ORB("entity.experience_orb.pickup", "Experience Orb"),
        BELL("block.bell.use", "Bell"),
        LEVEL_UP("entity.player.levelup", "Level Up"),
        ANVIL("block.anvil.place", "Anvil"),
        NOTE_PLING("block.note_block.pling", "Note Pling"),
        NOTE_BELL("block.note_block.bell", "Note Bell"),
        NOTE_FLUTE("block.note_block.flute", "Note Flute"),
        NOTE_HARP("block.note_block.harp", "Note Harp"),
        FIREWORK("entity.firework_rocket.launch", "Firework"),
        ITEM_PICKUP("entity.item.pickup", "Item Pickup"),
        SKELETON("wynnextras:skeleton", "Skeleton"),
        AMOGUS("wynnextras:amogus", "Amogus");

        private final String soundId;
        private final String displayName;

        NotificationSound(String soundId, String displayName) {
            this.soundId = soundId;
            this.displayName = displayName;
        }

        public String getSoundId() {
            return soundId;
        }

        @Override
        public String toString() {
            return displayName;
        }
    }

    // ==================== SAVE/LOAD ====================
    public static void load() {
        try {
            if (Files.exists(CONFIG_PATH)) {
                String json = Files.readString(CONFIG_PATH);
                INSTANCE = GSON.fromJson(json, WynnExtrasConfig.class);
                if (INSTANCE == null) {
                    INSTANCE = new WynnExtrasConfig();
                }
                // Ensure lists are not null
                if (INSTANCE.hiddenPlayers == null) INSTANCE.hiddenPlayers = new ArrayList<>();
                if (INSTANCE.notifierWords == null) INSTANCE.notifierWords = new ArrayList<>();
                if (INSTANCE.blockedWords == null) INSTANCE.blockedWords = new ArrayList<>();
                if (INSTANCE.raidLootTrackerHiddenLines == null) INSTANCE.raidLootTrackerHiddenLines = new ArrayList<>();
                if (INSTANCE.raidPBs == null) INSTANCE.raidPBs = new HashMap<>();
            }
        } catch (IOException e) {
            System.err.println("[WynnExtras] Failed to load config: " + e.getMessage());
            INSTANCE = new WynnExtrasConfig();
        }
    }

    public static void save() {
        try {
            Files.createDirectories(CONFIG_PATH.getParent());
            Files.writeString(CONFIG_PATH, GSON.toJson(INSTANCE));
            for (Consumer<WynnExtrasConfig> listener : saveListeners) {
                listener.accept(INSTANCE);
            }
        } catch (IOException e) {
            System.err.println("[WynnExtras] Failed to save config: " + e.getMessage());
        }
    }

    public static void registerSaveListener(Consumer<WynnExtrasConfig> listener) {
        saveListeners.add(listener);
    }

    // ==================== CONFIG SCREEN ====================
    public static Screen createConfigScreen(Screen parent) {
        return new WynnExtrasConfigScreen(parent);
    }
}
