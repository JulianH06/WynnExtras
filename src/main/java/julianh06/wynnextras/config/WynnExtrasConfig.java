package julianh06.wynnextras.config;

import julianh06.wynnextras.config.simpleconfig.ConfigData;
import julianh06.wynnextras.config.simpleconfig.ConfigHolder;
import julianh06.wynnextras.config.simpleconfig.SimpleConfig;
import julianh06.wynnextras.config.simpleconfig.annotations.Config;
import julianh06.wynnextras.config.simpleconfig.annotations.ConfigEntry;

import java.util.*;
import java.util.function.BiFunction;

import net.minecraft.util.ActionResult;

@Config(name = "wynnextras/wynnextras", title = "WynnExtras Config")
public class WynnExtrasConfig implements ConfigData {
    public static void registerSave(BiFunction<ConfigHolder<WynnExtrasConfig>, WynnExtrasConfig, ActionResult> saveFunction) {
        SimpleConfig.getConfigHolder(WynnExtrasConfig.class).registerSaveListener(saveFunction::apply);
    }

    public interface Categories {
        String playerHider = "Player Hider";
        String bankOverlay = "Bank Overlay";
        String chat = "Chat";
        String raid = "Raid";
        String crafting = "Crafting";
        String misc = "Misc";
    }


    //PLAYER HIDER

    @ConfigEntry.Name("Playerhider toggle")
    @ConfigEntry.Category(Categories.playerHider)
    public boolean partyMemberHide = true;

    @ConfigEntry.Name("Maximum hide distance")
    @ConfigEntry.Category(Categories.playerHider)
    public int maxHideDistance = 3;

    @ConfigEntry.Name("Hidden players")
    @ConfigEntry.Category(Categories.playerHider)
    public List<String> hiddenPlayers = new ArrayList<>();

    @ConfigEntry.Name("Only hide in NOTG")
    @ConfigEntry.Category(Categories.playerHider)
    @ConfigEntry.Excluded
    public boolean onlyInNotg = false;

    @ConfigEntry.Name("Print debug messages to minecrafts internal console")
    @ConfigEntry.Category(Categories.playerHider)
    @ConfigEntry.Excluded
    public boolean printDebugToConsole = false;

    @ConfigEntry.Category(Categories.playerHider)
    @ConfigEntry.Text
    public String playerHiderCmdInfo = "Run \"/WynnExtras (/we) playerhider\" to add/remove players or to toggle the feature.";


    //CHAT NOTIFIER

    @ConfigEntry.Category(Categories.chat)
    @ConfigEntry.Collapsible
    @ConfigEntry.Name("Premade Notifications")
    public NotificationConfig notificationConfig = new NotificationConfig();

    public static class NotificationConfig implements ConfigData {
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

        public NotificationConfig() {
            premades = new HashMap<>();
        }

        @ConfigEntry.Excluded
        public Map<String, Boolean> premades;
    }

    @ConfigEntry.Category(Categories.chat)
    @ConfigEntry.Name("Notified words")
    public List<String> notifierWords = new ArrayList<>();

    @ConfigEntry.Category(Categories.chat)
    @ConfigEntry.Text
    String notifierInfotext = "The Text needs to be separated by \"|\". (ALT Gr + <)\n" +
            "The Phrase on the left is what needs to be in the Message to trigger and \n" +
            "the one on the right is the text that will be displayed. Example: \"test|worked!\"";

//    @ConfigEntry.Category(Categories.chatNotifier)
//    @ConfigEntry.Name("Text scale")
//    public float TextScale = 5f;
//
//    @ConfigEntry.Category(Categories.chatNotifier)
//    @ConfigEntry.Name("Text offset x")
//    public int TextOffsetX = 75;
//
//    @ConfigEntry.Category(Categories.chatNotifier)
//    @ConfigEntry.Name("Text offset y")
//    public int TextOffsetY = 40;

    @ConfigEntry.Category(Categories.chat)
    @ConfigEntry.Name("Text duration in ms")
    public int TextDurationInMs = 2000;

    @ConfigEntry.Category(Categories.chat)
    @ConfigEntry.Dropdown(values = {
            "WHITE",
            "BLACK",
            "AQUA",
            "RED",
            "YELLOW",
            "BLUE",
            "GREEN",
            "DARK_BLUE",
            "DARK_GREEN",
            "DARK_AQUA",
            "DARK_RED",
            "DARK_PURPLE",
            "LIGHT_PURPLE",
            "GRAY",
            "DARK_GRAY",
            "GOLD"
    })
    @ConfigEntry.Name("Text color")
    public String TextColor = "WHITE";

//    @ConfigEntry.Category(Categories.chatNotifier)
//    @ConfigEntry.Name("Text Preview")
//    public boolean NotifierPreview = false;

    @ConfigEntry.Category(Categories.chat)
    @ConfigEntry.Name("Sound")
    @ConfigEntry.Dropdown(values = {
        "entity.experience_orb.pickup",
        "block.bell.use",
        "entity.player.levelup",
        "block.anvil.place",
        "block.note_block.pling",
        "block.note_block.bell",
        "block.note_block.flute",
        "block.note_block.harp",
        "entity.firework_rocket.launch",
        "entity.item.pickup"
    })
    public String Sound = "entity.experience_orb.pickup";

    @ConfigEntry.Category(Categories.chat)
    @ConfigEntry.Name("Sound volume")
    public float SoundVolume = 0.1f;

    @ConfigEntry.Category(Categories.chat)
    @ConfigEntry.Name("Sound pitch")
    public float SoundPitch = 1;

    @ConfigEntry.Category(Categories.chat)
    @ConfigEntry.Text
    public String notifierCmdInfo = "Run \"/WynnExtras (/we) notifiertest\" to test out the notifier.";


    //CHAT BLOCKER

    @ConfigEntry.Category(Categories.chat)
    @ConfigEntry.Text
    public String empty = " ";

    @ConfigEntry.Category(Categories.chat)
    @ConfigEntry.Name("Blocked words")
    public List<String> blockedWords = new ArrayList<>();


    //BANK OVERLAY

    //    public HashMap<Integer, List<SavedItem>> BankPagesSavedItems = new HashMap<>();

    @ConfigEntry.Category(Categories.bankOverlay)
    @ConfigEntry.Name("Bank overlay toggle")
    public boolean toggleBankOverlay = true;

    @ConfigEntry.Category(Categories.bankOverlay)
    @ConfigEntry.Name("Wynntils item rarity background intensity")
    public int wynntilsItemRarityBackgroundAlpha = 150;

    @ConfigEntry.Category(Categories.bankOverlay)
    @ConfigEntry.Name("Toggle smooth scroll")
    public boolean smoothScrollToggle = true;

    @ConfigEntry.Category(Categories.bankOverlay)
    @ConfigEntry.Name("Show a button to quickly toggle the bank overlay")
    public boolean bankQuickToggle = true;

    @ConfigEntry.Category(Categories.bankOverlay)
    @ConfigEntry.Name("Dark mode for the Bank overlay")
    public boolean darkmodeToggle = false;

    @ConfigEntry.Category(Categories.raid)
    @ConfigEntry.Name("Enable Raid timestamps")
    public boolean toggleRaidTimestamps = true;

    @ConfigEntry.Category(Categories.crafting)
    @ConfigEntry.Name("Crafting helper overlay toggle")
    public boolean craftingHelperOverlay = true;

    //TOTEM VISUALIZER

    @ConfigEntry.Category(Categories.raid)
    @ConfigEntry.Name("Fast requeue toggle (automatically runs /pf when closing a raid chest)")
    public boolean toggleFastRequeue = true;

    @ConfigEntry.Category(Categories.raid)
    @ConfigEntry.Name("Chiropterror spawn timer")
    public boolean chiropTimer = false;

    @ConfigEntry.Category(Categories.misc)
    @ConfigEntry.Name("Show Wynnpool item weights")
    public boolean showWeight = true;

    @ConfigEntry.Category(Categories.misc)
    @ConfigEntry.Name("Show weight of each stat")
    public boolean showScales = true;

    @ConfigEntry.Category(Categories.misc)
    @ConfigEntry.Name("Dark mode for profile viewer")
    public boolean pvDarkmodeToggle = false;

    @ConfigEntry.Category(Categories.misc)
    @ConfigEntry.Text
    public String emptyyy = " ";

    @ConfigEntry.Category(Categories.misc)
    @ConfigEntry.Name("Totem range visualizer toggle")
    public boolean totemRangeVisualizerToggle = true;

    @ConfigEntry.Category(Categories.misc)
    @ConfigEntry.Name("Totem range")
    public float totemRange = 10;

    @ConfigEntry.Category(Categories.misc)
    @ConfigEntry.Dropdown(values = {
            "WHITE",
            "BLACK",
            "AQUA",
            "RED",
            "YELLOW",
            "BLUE",
            "GREEN",
            "DARK_BLUE",
            "DARK_GREEN",
            "DARK_AQUA",
            "DARK_RED",
            "DARK_PURPLE",
            "LIGHT_PURPLE",
            "GRAY",
            "DARK_GRAY",
            "GOLD"
    })
    @ConfigEntry.Name("Totem circle color")
    public String totemColor = "WHITE";

    @ConfigEntry.Category(Categories.misc)
    @ConfigEntry.Name("Eldritchcall range")
    public float eldritchCallRange = 15;

    @ConfigEntry.Category(Categories.misc)
    @ConfigEntry.Dropdown(values = {
            "WHITE",
            "BLACK",
            "AQUA",
            "RED",
            "YELLOW",
            "BLUE",
            "GREEN",
            "DARK_BLUE",
            "DARK_GREEN",
            "DARK_AQUA",
            "DARK_RED",
            "DARK_PURPLE",
            "LIGHT_PURPLE",
            "GRAY",
            "DARK_GRAY",
            "GOLD"
    })
    @ConfigEntry.Name("Eldritchcall circle color")
    public String eldritchCallColor = "WHITE";

    //Provoke Timer

    @ConfigEntry.Category(Categories.raid)
    @ConfigEntry.Name("Toggle Provoke Timer [WIP]")
    public boolean provokeTimerToggle = false;

    @ConfigEntry.Category(Categories.misc)
    @ConfigEntry.Dropdown(values = {
            "WHITE",
            "BLACK",
            "AQUA",
            "RED",
            "YELLOW",
            "BLUE",
            "GREEN",
            "DARK_BLUE",
            "DARK_GREEN",
            "DARK_AQUA",
            "DARK_RED",
            "DARK_PURPLE",
            "LIGHT_PURPLE",
            "GRAY",
            "DARK_GRAY",
            "GOLD"
    })
    @ConfigEntry.Name("Provoke timer color")
    public String provokeTimerColor = "WHITE";

    @ConfigEntry.Category(Categories.misc)
    @ConfigEntry.Name("Use different GUI Scale for menus")
    public boolean differentGUIScale = false;

    @ConfigEntry.Category(Categories.misc)
    @ConfigEntry.Name("GUI Scale")
    public int customGUIScale = 3;

    // Perspective
    @ConfigEntry.Category(Categories.misc)
    @ConfigEntry.Name("Remove front person view")
    public boolean removeFrontPersonView = false;

    @ConfigEntry.Category(Categories.misc)
    @ConfigEntry.Name("Receive smart financial advise in the item identifier menu")
    public boolean sourceOfTruthToggle = false;

    @ConfigEntry.Category(Categories.misc)
    @ConfigEntry.Name("Show territory estimates in the Wynntils guild map")
    public boolean territoryEstimateToggle = false;

    @ConfigEntry.Category(Categories.raid)
    @ConfigEntry.Name("Raid Personal Bests (internal)")
    @ConfigEntry.Excluded
    public Map<String, Long> raidPBs = new HashMap<>();


//    //Hider
//    public boolean partyMemberHide = true;
//    public int maxHideDistance = 3;
//    public boolean onlyInNotg = false;
//    public boolean printDebugToConsole = false;
//    public List<String> playerHiderList = new ArrayList<>();

//    //Chat Notifier Text
//    public List<String> notifierWords = new ArrayList<>();
//    public float TextScale = 5f;
//    public int TextOffsetX = 75;
//    public int TextOffsetY = 40;
//    public int TextDurationInMs = 2000;
//    public Color TextColor = Color.ofRGB(255, 255, 255);
//    public boolean NotifierPreview = false;

//    //Chat Notifier Sound
//    public String Sound = "entity.experience_orb.pickup";
//    public float SoundVolume = 0.1f;
//    public float SoundPitch = 1;
//
//    //Chat Blocker
//    public List<String> blockedWords = new ArrayList<>();
//
//    //Bank overlay
//    public HashMap<Integer, List<ItemStack>> BankPages = new HashMap<>();
////    public HashMap<Integer, List<SavedItem>> BankPagesSavedItems = new HashMap<>();
//    public boolean toggleBankOverlay = true;
//    public HashMap<Integer, String> BankPageNames = new HashMap<>();


//    //Totem Range Visualizer
//    public boolean totemRangeVisualizerToggle = true;
//    public int totemRange = 10;
//    public int eldritchCallRange = 15;

//    private static final Gson GSON = new GsonBuilder()
//            .registerTypeAdapter(ItemStack.class, new ItemStackSerializer())
//            .registerTypeAdapter(ItemStack.class, new ItemStackDeserializer())
////            .registerTypeAdapter(SavedItem.class, new SavedItemSerializer())
//            .setPrettyPrinting()
//            .create();
//    private static final Path CONFIG_PATH = FabricLoader.getInstance()
//            .getConfigDir()
//            .resolve("wynnextras.json");


//    public static WynnExtrasConfig INSTANCE = new WynnExtrasConfig();
//
//    public static void load() {
//        if (Files.exists(CONFIG_PATH)) {
//            try (Reader reader = Files.newBufferedReader(CONFIG_PATH)) {
//                INSTANCE = GSON.fromJson(reader, WynnExtrasConfig.class);
//            } catch (IOException e) {
//                System.err.println("[WynnExtras] Couldn't read the config file:");
//                e.printStackTrace();
//            }
//        }
//    }
//
//    public static void save() {
//        try (Writer writer = Files.newBufferedWriter(CONFIG_PATH)) {
//            GSON.toJson(INSTANCE, writer);
//        } catch (IOException e) {
//            System.err.println("[WynnExtras] Couldn't write the config file:");
//            e.printStackTrace();
//        }
//    }
//
//    public static void openConfigScreen() {
//        if(modMenuApiImpl == null) {
//            modMenuApiImpl = new WynnExtrasModMenuApiImpl();
//        }
//        if(modMenuApiImpl.configScreen == null) {
//            modMenuApiImpl.registerConfig();
//        }
//        MinecraftClient.getInstance().send(() -> MinecraftClient.getInstance().setScreen(modMenuApiImpl.configScreen));
//    }
}
