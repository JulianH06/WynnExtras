package julianh06.wynnextras.features.profileviewer;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.authlib.GameProfile;
import com.mojang.authlib.properties.Property;
import com.wynntils.utils.colors.CustomColor;
import com.wynntils.utils.mc.McUtils;
import com.wynntils.utils.render.type.HorizontalAlignment;
import com.wynntils.utils.render.type.VerticalAlignment;
import julianh06.wynnextras.config.WynnExtrasConfig;
import julianh06.wynnextras.config.simpleconfig.SimpleConfig;
import julianh06.wynnextras.features.profileviewer.data.*;
import julianh06.wynnextras.features.profileviewer.tabs.*;
import julianh06.wynnextras.utils.UI.UIUtils;
import julianh06.wynnextras.utils.UI.WEElement;
import julianh06.wynnextras.utils.UI.Widget;
import julianh06.wynnextras.utils.UI.WEScreen;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.EntityRenderDispatcher;
import net.minecraft.client.util.SkinTextures;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.EntityPose;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerModelPart;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import org.joml.Quaternionf;


import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.*;
import java.util.concurrent.CompletableFuture;

public class PVScreen extends WEScreen {
    public static int mouseX = 0;
    public static int mouseY = 0;

    public enum Rank {NONE, VIP, VIPPLUS, HERO, HEROPLUS, CHAMPION, MEDIA, WYNN, MOD, ADMIN}

    public enum Tab {General, Raids, Rankings, Professions, Dungeons, Quests, Tree, Aspects, Misc}
    public static List<TabButton> tabButtons = new ArrayList<>();
    public static List<CharacterButton> characterButtons = new ArrayList<>();

    public static List<String> WETeam = List.of("JulianH06", "Teslanator", "Mikecraft1224", "LegendaryVirus", "elwood24", "BaltrazYT", "pat_crafter07");

    List<String> allQuests = Arrays.asList("???", "A Grave Mistake", "A Hunter's Calling", "A Journey Beyond", "A Journey Further", "A Marauder's Dues", "A Sandy Scandal", "Acquiring Credentials", "Aldorei's Secret Part I", "Aldorei's Secret Part II", "All Roads To Peace", "An Iron Heart Part I", "An Iron Heart Part II", "Arachnids' Ascent", "Beneath the Depths", "Beyond the Grave", "Blazing Retribution", "Bob's Lost Soul", "Canyon Condor", "Clearing the Camps", "Cluck Cluck", "Cook Assistant", "Corrupted Betrayal", "Cowfusion", "Creeper Infiltration", "Crop Failure", "Death Whistle", "Deja Vu", "Desperate Metal", "Dwarves and Doguns Part I", "Dwarves and Doguns Part II", "Dwarves and Doguns Part III", "Dwarves and Doguns Part IV", "Dwelling Walls", "Elemental Exercise", "Enter the Dojo", "Enzan's Brother", "Fallen Delivery", "Fantastic Voyage", "Fate of the Fallen", "Flight in Distress", "Forbidden Prison", "From the Bottom", "From the Mountains", "Frost Bite", "General's Orders", "Grand Youth", "Grave Digger", "Green Gloop", "Haven Antiquity", "Heart of Llevigar", "Hollow Serenity", "Hunger of the Gerts Part I", "Hunger of the Gerts Part II", "Ice Nations", "Infested Plants", "Jungle Fever", "King's Recruit", "Kingdom of Sand", "Lava Springs", "Lazarus Pit", "Lexdale Witch Trials", "Lost in the Jungle", "Lost Royalty", "Lost Soles", "Lost Tower", "Maltic's Well", "Master Piece", "Meaningful Holiday", "Memory Paranoia", "Mini-Quest - Gather Acacia Logs", "Mini-Quest - Gather Acacia Logs II", "Mini-Quest - Gather Avo Logs", "Mini-Quest - Gather Avo Logs II", "Mini-Quest - Gather Avo Logs III", "Mini-Quest - Gather Avo Logs IV", "Mini-Quest - Gather Bamboo", "Mini-Quest - Gather Barley", "Mini-Quest - Gather Bass", "Mini-Quest - Gather Bass II", "Mini-Quest - Gather Bass III", "Mini-Quest - Gather Bass IV", "Mini-Quest - Gather Birch Logs", "Mini-Quest - Gather Carp", "Mini-Quest - Gather Carp II", "Mini-Quest - Gather Cobalt", "Mini-Quest - Gather Cobalt II", "Mini-Quest - Gather Cobalt III", "Mini-Quest - Gather Copper", "Mini-Quest - Gather Dark Logs", "Mini-Quest - Gather Dark Logs II", "Mini-Quest - Gather Dark Logs III", "Mini-Quest - Gather Decay Roots", "Mini-Quest - Gather Decay Roots II", "Mini-Quest - Gather Decay Roots III", "Mini-Quest - Gather Diamonds", "Mini-Quest - Gather Diamonds II", "Mini-Quest - Gather Diamonds III", "Mini-Quest - Gather Diamonds IV", "Mini-Quest - Gather Gold", "Mini-Quest - Gather Gold II", "Mini-Quest - Gather Granite", "Mini-Quest - Gather Gudgeon", "Mini-Quest - Gather Gylia Fish", "Mini-Quest - Gather Gylia Fish II", "Mini-Quest - Gather Gylia Fish III", "Mini-Quest - Gather Hops", "Mini-Quest - Gather Hops II", "Mini-Quest - Gather Icefish", "Mini-Quest - Gather Icefish II", "Mini-Quest - Gather Iron", "Mini-Quest - Gather Iron II", "Mini-Quest - Gather Jungle Logs", "Mini-Quest - Gather Jungle Logs II", "Mini-Quest - Gather Kanderstone", "Mini-Quest - Gather Kanderstone II", "Mini-Quest - Gather Kanderstone III", "Mini-Quest - Gather Koi", "Mini-Quest - Gather Koi II", "Mini-Quest - Gather Koi III", "Mini-Quest - Gather Light Logs", "Mini-Quest - Gather Light Logs II", "Mini-Quest - Gather Light Logs III", "Mini-Quest - Gather Malt", "Mini-Quest - Gather Malt II", "Mini-Quest - Gather Millet", "Mini-Quest - Gather Millet II", "Mini-Quest - Gather Millet III", "Mini-Quest - Gather Molten Eel", "Mini-Quest - Gather Molten Eel II", "Mini-Quest - Gather Molten Eel III", "Mini-Quest - Gather Molten Eel IV", "Mini-Quest - Gather Molten Ore", "Mini-Quest - Gather Molten Ore II", "Mini-Quest - Gather Molten Ore III", "Mini-Quest - Gather Molten Ore IV", "Mini-Quest - Gather Oak Logs", "Mini-Quest - Gather Oats", "Mini-Quest - Gather Oats II", "Mini-Quest - Gather Pine Logs", "Mini-Quest - Gather Pine Logs II", "Mini-Quest - Gather Pine Logs III", "Mini-Quest - Gather Piranhas", "Mini-Quest - Gather Piranhas II", "Mini-Quest - Gather Rice", "Mini-Quest - Gather Rice II", "Mini-Quest - Gather Rice III", "Mini-Quest - Gather Rice IV", "Mini-Quest - Gather Rye", "Mini-Quest - Gather Rye II", "Mini-Quest - Gather Salmon", "Mini-Quest - Gather Salmon II", "Mini-Quest - Gather Sandstone", "Mini-Quest - Gather Sandstone II", "Mini-Quest - Gather Silver", "Mini-Quest - Gather Silver II", "Mini-Quest - Gather Sorghum", "Mini-Quest - Gather Sorghum II", "Mini-Quest - Gather Sorghum III", "Mini-Quest - Gather Sorghum IV", "Mini-Quest - Gather Spruce Logs", "Mini-Quest - Gather Spruce Logs II", "Mini-Quest - Gather Trout", "Mini-Quest - Gather Wheat", "Mini-Quest - Gather Willow Logs", "Mini-Quest - Gather Willow Logs II", "Mini-Quest - Slay Ailuropodas", "Mini-Quest - Slay Angels", "Mini-Quest - Slay Astrochelys Manis", "Mini-Quest - Slay Azers", "Mini-Quest - Slay Conures", "Mini-Quest - Slay Coyotes", "Mini-Quest - Slay Creatures of Nesaak Forest", "Mini-Quest - Slay Creatures of the Void", "Mini-Quest - Slay Dead Villagers", "Mini-Quest - Slay Dragonlings", "Mini-Quest - Slay Felrocs", "Mini-Quest - Slay Frosted Guards & Cryostone Golems", "Mini-Quest - Slay Hobgoblins", "Mini-Quest - Slay Idols", "Mini-Quest - Slay Ifrits", "Mini-Quest - Slay Jinkos", "Mini-Quest - Slay Lizardmen", "Mini-Quest - Slay Magma Entities", "Mini-Quest - Slay Mooshrooms", "Mini-Quest - Slay Myconids", "Mini-Quest - Slay Orcs", "Mini-Quest - Slay Pernix Monkeys", "Mini-Quest - Slay Robots", "Mini-Quest - Slay Scarabs", "Mini-Quest - Slay Skeletons", "Mini-Quest - Slay Slimes", "Mini-Quest - Slay Spiders", "Mini-Quest - Slay Weirds", "Mini-Quest - Slay Wraiths & Phantasms", "Misadventure on the Sea", "Mixed Feelings", "Murder Mystery", "Mushroom Man", "One Thousand Meters Under", "Out of my Mind", "Pirate's Trove", "Pit of the Dead", "Point of No Return", "Poisoning the Pest", "Potion Making", "Purple and Blue", "Realm of Light I - The Worm Holes", "Realm of Light II - Taproot", "Realm of Light III - A Headless History", "Realm of Light IV - Finding the Light", "Realm of Light V - The Realm of Light", "Recipe For Disaster", "Reclaiming the House", "Recover the Past", "Redbeard's Booty", "Reincarnation", "Rise of the Quartron", "Royal Trials", "Shattered Minds", "Stable Story", "Star Thief", "Supply and Delivery", "Taking the Tower", "Temple of the Legends", "Tempo Town Trouble", "The Bigger Picture", "The Breaking Point", "The Canary Calls", "The Canyon Guides", "The Corrupted Village", "The Dark Descent", "The Envoy Part I", "The Envoy Part II", "The Feathers Fly Part I", "The Feathers Fly Part II", "The Hero of Gavel", "The Hidden City", "The House of Twain", "The Lost", "The Maiden Tower", "The Mercenary", "The Olmic Rune", "The Order of the Grook", "The Passage", "The Qira Hive", "The Sewers of Ragni", "The Shadow of the Beast", "The Thanos Depository", "The Ultimate Weapon", "Tower of Ascension", "Tribal Aggression", "Troubled Tribesmen", "Tunnel Trouble", "UndericeÀ", "Underwater", "Wrath of the Mummy", "WynnExcavation Site A", "WynnExcavation Site B", "WynnExcavation Site C", "WynnExcavation Site D", "Zhight Island");

    static Identifier tabLeft = Identifier.of("wynnextras", "textures/gui/profileviewer/tableft.png");
    static Identifier tabMid = Identifier.of("wynnextras", "textures/gui/profileviewer/tabmid.png");
    static Identifier tagRight = Identifier.of("wynnextras", "textures/gui/profileviewer/tabright.png");

    static Identifier tabLeftDark = Identifier.of("wynnextras", "textures/gui/profileviewer/tableft_dark.png");
    static Identifier tabMidDark = Identifier.of("wynnextras", "textures/gui/profileviewer/tabmid_dark.png");
    static Identifier tagRightDark = Identifier.of("wynnextras", "textures/gui/profileviewer/tabright_dark.png");

    static Identifier NOTGTexture = Identifier.of("wynnextras", "textures/gui/profileviewer/rankingicons/notg.png");
    static Identifier NOLTexture = Identifier.of("wynnextras", "textures/gui/profileviewer/rankingicons/nol.png");
    static Identifier TCCTexture = Identifier.of("wynnextras", "textures/gui/profileviewer/rankingicons/tcc.png");
    static Identifier TNATexture = Identifier.of("wynnextras", "textures/gui/profileviewer/rankingicons/tna.png");

    static Identifier backgroundTexture = Identifier.of("wynnextras", "textures/gui/profileviewer/profileviewerbackground.png");
    static Identifier alsobackgroundTexture = Identifier.of("wynnextras", "textures/gui/profileviewer/alsoprofileviewerbackground.png");
    static Identifier raidBackgroundTexture = Identifier.of("wynnextras", "textures/gui/profileviewer/raidbackground.png");
    static Identifier openInBrowserButtonTexture = Identifier.of("wynnextras", "textures/gui/profileviewer/openinbrowserbuttontexture.png");
    static Identifier openInBrowserButtonTextureW = Identifier.of("wynnextras", "textures/gui/profileviewer/openinbrowserbuttontexturewide.png");
    static Identifier classBackgroundTexture = Identifier.of("wynnextras", "textures/gui/profileviewer/classbackgroundinactive.png");
    static Identifier classBackgroundTextureGold = Identifier.of("wynnextras", "textures/gui/profileviewer/classbackgroundinactivegold.png");
    static Identifier classBackgroundTextureActive = Identifier.of("wynnextras", "textures/gui/profileviewer/classbackgroundactive.png");
    static Identifier onlineCircleTexture = Identifier.of("wynnextras", "textures/gui/profileviewer/onlinecircle.png");
    static Identifier offlineCircleTexture = Identifier.of("wynnextras", "textures/gui/profileviewer/offlinecircle.png");



    static Identifier backgroundTextureDark = Identifier.of("wynnextras", "textures/gui/profileviewer/profileviewerbackground_dark.png");
    static Identifier alsobackgroundTextureDark = Identifier.of("wynnextras", "textures/gui/profileviewer/alsoprofileviewerbackground_dark.png");
    static Identifier raidBackgroundTextureDark = Identifier.of("wynnextras", "textures/gui/profileviewer/raidbackground_dark.png");
    static Identifier openInBrowserButtonTextureDark = Identifier.of("wynnextras", "textures/gui/profileviewer/openinbrowserbuttontexture_dark.png");
    static Identifier openInBrowserButtonTextureWDark = Identifier.of("wynnextras", "textures/gui/profileviewer/openinbrowserbuttontexturewide_dark.png");
    static Identifier classBackgroundTextureDark = Identifier.of("wynnextras", "textures/gui/profileviewer/classbackgroundinactive_dark.png");
    static Identifier classBackgroundTextureGoldDark = Identifier.of("wynnextras", "textures/gui/profileviewer/classbackgroundinactivegold_dark.png");
    static Identifier classBackgroundTextureActiveDark = Identifier.of("wynnextras", "textures/gui/profileviewer/classbackgroundactive_dark.png");
    static Identifier onlineCircleTextureDark = Identifier.of("wynnextras", "textures/gui/profileviewer/onlinecircle_dark.png");
    static Identifier offlineCircleTextureDark = Identifier.of("wynnextras", "textures/gui/profileviewer/offlinecircle_dark.png");


    static Identifier vip = Identifier.of("wynnextras", "textures/gui/profileviewer/ranks/vip.png");
    static Identifier vipplus = Identifier.of("wynnextras", "textures/gui/profileviewer/ranks/vipplus.png");
    static Identifier hero = Identifier.of("wynnextras", "textures/gui/profileviewer/ranks/hero.png");
    static Identifier heroplus = Identifier.of("wynnextras", "textures/gui/profileviewer/ranks/heroplus.png");
    static Identifier champion = Identifier.of("wynnextras", "textures/gui/profileviewer/ranks/champion.png");
    static Identifier media = Identifier.of("wynnextras", "textures/gui/profileviewer/ranks/media.png");
    static Identifier wynn = Identifier.of("wynnextras", "textures/gui/profileviewer/ranks/wynn.png");
    static Identifier mod = Identifier.of("wynnextras", "textures/gui/profileviewer/ranks/moderator.png");
    static Identifier admin = Identifier.of("wynnextras", "textures/gui/profileviewer/ranks/admin.png");
    static Identifier warriorTexture = Identifier.of("wynnextras", "textures/gui/profileviewer/classes/warrior.png");
    static Identifier warriorGoldTexture = Identifier.of("wynnextras", "textures/gui/profileviewer/classes/warriorgold.png");
    static Identifier shamanTexture = Identifier.of("wynnextras", "textures/gui/profileviewer/classes/shaman.png");
    static Identifier shamanGoldTexture = Identifier.of("wynnextras", "textures/gui/profileviewer/classes/shamangold.png");
    static Identifier mageTexture = Identifier.of("wynnextras", "textures/gui/profileviewer/classes/mage.png");
    static Identifier mageGoldTexture = Identifier.of("wynnextras", "textures/gui/profileviewer/classes/magegold.png");
    static Identifier assassinTexture = Identifier.of("wynnextras", "textures/gui/profileviewer/classes/assassin.png");
    static Identifier assassinGoldTexture = Identifier.of("wynnextras", "textures/gui/profileviewer/classes/assassingold.png");
    static Identifier archerTexture = Identifier.of("wynnextras", "textures/gui/profileviewer/classes/archer.png");
    static Identifier archerGoldTexture = Identifier.of("wynnextras", "textures/gui/profileviewer/classes/archergold.png");


    static Identifier decrepitSewersTexture = Identifier.of("wynnextras", "textures/gui/profileviewer/dungeons/decrepitsewers.png");
    static Identifier infestedPitTexture = Identifier.of("wynnextras", "textures/gui/profileviewer/dungeons/infestedpit.png");
    static Identifier underworldCryptTexture = Identifier.of("wynnextras", "textures/gui/profileviewer/dungeons/underworldcrypt.png");
    static Identifier timelostSanctumTexture = Identifier.of("wynnextras", "textures/gui/profileviewer/dungeons/timelostsanctum.png");
    static Identifier sandSweptTombTexture = Identifier.of("wynnextras", "textures/gui/profileviewer/dungeons/sandswepttomb.png");
    static Identifier iceBarrowsTexture = Identifier.of("wynnextras", "textures/gui/profileviewer/dungeons/icebarrows.png");
    static Identifier undergrowthRuinsTexture = Identifier.of("wynnextras", "textures/gui/profileviewer/dungeons/undergrowthruins.png");
    static Identifier galleonsGraveyardTexture = Identifier.of("wynnextras", "textures/gui/profileviewer/dungeons/galleonsgraveyard.png");
    static Identifier fallenFactoryTexture = Identifier.of("wynnextras", "textures/gui/profileviewer/dungeons/fallenfactory.png");
    static Identifier eldritchOutlookTexture = Identifier.of("wynnextras", "textures/gui/profileviewer/dungeons/eldritchoutlook.png");

    static List<Identifier> dungeonTextures = List.of(decrepitSewersTexture, infestedPitTexture, underworldCryptTexture, timelostSanctumTexture, sandSweptTombTexture, iceBarrowsTexture, undergrowthRuinsTexture, galleonsGraveyardTexture, fallenFactoryTexture, eldritchOutlookTexture);

    static Identifier dungeonKeyTexture = Identifier.of("wynnextras", "textures/gui/profileviewer/dungeons/dungeonkey.png");
    static Identifier corruptedDungeonKeyTexture = Identifier.of("wynnextras", "textures/gui/profileviewer/dungeons/corrupteddungeonkey.png");
    static Identifier dungeonBackgroundTexture = Identifier.of("wynnextras", "textures/gui/profileviewer/dungeons/dungeonpagebackground.png");
    static Identifier miscBackgroundTexture = Identifier.of("wynnextras", "textures/gui/profileviewer/miscpagebackground.png");

    static Identifier dungeonBackgroundTextureDark = Identifier.of("wynnextras", "textures/gui/profileviewer/dungeons/dungeonpagebackground_dark.png");
    static Identifier miscBackgroundTextureDark = Identifier.of("wynnextras", "textures/gui/profileviewer/miscpagebackground_dark.png");

    static Identifier miningTexture = Identifier.of("wynnextras", "textures/gui/profileviewer/rankingicons/mining.png");
    static Identifier woodcuttingTexture = Identifier.of("wynnextras", "textures/gui/profileviewer/rankingicons/woodcutting.png");
    static Identifier farmingTexture = Identifier.of("wynnextras", "textures/gui/profileviewer/rankingicons/farming.png");
    static Identifier fishingTexture = Identifier.of("wynnextras", "textures/gui/profileviewer/rankingicons/fishing.png");
    static Identifier armouringTexture = Identifier.of("wynnextras", "textures/gui/profileviewer/rankingicons/armouring.png");
    static Identifier tailoringTexture = Identifier.of("wynnextras", "textures/gui/profileviewer/rankingicons/tailoring.png");
    static Identifier weaponsmithingTexture = Identifier.of("wynnextras", "textures/gui/profileviewer/rankingicons/weaponsmithing.png");
    static Identifier woodworkingTexture = Identifier.of("wynnextras", "textures/gui/profileviewer/rankingicons/woodworking.png");
    static Identifier jewelingTexture = Identifier.of("wynnextras", "textures/gui/profileviewer/rankingicons/jeweling.png");
    static Identifier alchemismTexture = Identifier.of("wynnextras", "textures/gui/profileviewer/rankingicons/alchemism.png");
    static Identifier scribingTexture = Identifier.of("wynnextras", "textures/gui/profileviewer/rankingicons/scribing.png");
    static Identifier cookingTexture = Identifier.of("wynnextras", "textures/gui/profileviewer/rankingicons/cooking.png");
    static Identifier profBackgroundTexture = Identifier.of("wynnextras", "textures/gui/profileviewer/profs/profbackground.png");
    static Identifier profBackgroundTextureDark = Identifier.of("wynnextras", "textures/gui/profileviewer/profs/profbackground_dark.png");

    static Identifier questBackgroundTexture = Identifier.of("wynnextras", "textures/gui/profileviewer/quests/questbackground.png");
    static Identifier questBackgroundBorderTexture = Identifier.of("wynnextras", "textures/gui/profileviewer/quests/questbackgroundborders.png");
    static Identifier questSearchbarTexture = Identifier.of("wynnextras", "textures/gui/profileviewer/quests/questsearchbar.png");

    static Identifier questBackgroundTextureDark = Identifier.of("wynnextras", "textures/gui/profileviewer/quests/questbackground_dark.png");
    static Identifier questBackgroundBorderTextureDark = Identifier.of("wynnextras", "textures/gui/profileviewer/quests/questbackgroundborders_dark.png");
    static Identifier questSearchbarTextureDark = Identifier.of("wynnextras", "textures/gui/profileviewer/quests/questsearchbar_dark.png");

    static Identifier warsCompletionTexture = Identifier.of("wynnextras", "textures/gui/profileviewer/rankingicons/warscompletion.png");
    static Identifier playerContentTexture = Identifier.of("wynnextras", "textures/gui/profileviewer/rankingicons/playercontent.png");
    static Identifier globalPlayerContent = Identifier.of("wynnextras", "textures/gui/profileviewer/rankingicons/globalplayercontent.png");
    static Identifier combatLevelTexture = Identifier.of("wynnextras", "textures/gui/profileviewer/rankingicons/combatlevel.png");
    static Identifier totalLevelTexture = Identifier.of("wynnextras", "textures/gui/profileviewer/rankingicons/totallevel.png");
    static Identifier professionLevelTexture = Identifier.of("wynnextras", "textures/gui/profileviewer/rankingicons/professionlevel.png");
    static Identifier rankingBackgroundTexture = Identifier.of("wynnextras", "textures/gui/profileviewer/rankingicons/rankingbackground.png");
    static Identifier rankingBackgroundWideTexture = Identifier.of("wynnextras", "textures/gui/profileviewer/rankingicons/rankingbackgroundwide.png");

    static Identifier ironmanTexture = Identifier.of("wynnextras", "textures/gui/profileviewer/gamemodes/ironman.png");
    static Identifier ultimateIronmanTexture = Identifier.of("wynnextras", "textures/gui/profileviewer/gamemodes/ultimateironman.png");
    static Identifier huntedTexture = Identifier.of("wynnextras", "textures/gui/profileviewer/gamemodes/hunted.png");
    static Identifier hardcoreTexture = Identifier.of("wynnextras", "textures/gui/profileviewer/gamemodes/hardcore.png");
    static Identifier hardcoreFailedTexture = Identifier.of("wynnextras", "textures/gui/profileviewer/gamemodes/hardcorefailed.png");
    static Identifier craftsmanTexture = Identifier.of("wynnextras", "textures/gui/profileviewer/gamemodes/craftsman.png");

    static Identifier rankingBackgroundTextureDark = Identifier.of("wynnextras", "textures/gui/profileviewer/rankingicons/rankingbackground_dark.png");
    static Identifier rankingBackgroundWideTextureDark = Identifier.of("wynnextras", "textures/gui/profileviewer/rankingicons/rankingbackgroundwide_dark.png");

    static OpenInBrowserButton openInBrowserButton;
    public static Searchbar searchBar;
    public static Searchbar questSearchBar;
    public static Searchbar treeSearchBar;

    public static Tab currentTab = Tab.General;

    String player;
    public static AbstractClientPlayerEntity dummy;

    public static CharacterData selectedCharacter;

    public static int scrollOffset = 0;
    private static long lastScrollTime = 0;
    private static final long scrollCooldown = 0; // in ms

    BackgroundImageWidget backgroundImageWidget = new BackgroundImageWidget();
    List<TabButtonWidget> tabButtonWidgets = new ArrayList<>();
    public static TabWidget currentTabWidget;

    public static DarkModeToggleWidget darkModeToggleWidget = new DarkModeToggleWidget();

    public static List<String> lastViewedPlayers = new ArrayList<>();
    public static List<PlayerWidget> lastViewedPlayersWidget = new ArrayList<>();
    public static Map<String, Identifier> lastViewedPlayersSkins = new HashMap<>();

    static boolean addedNewest = false;

    public PVScreen(String name) {
        super(Text.of("Player Viewer"));
        String player;
        if(name == null && McUtils.player() == null) player = "null";
        else if(name == null) player = McUtils.playerName();
        else player = name;
        currentTabWidget = null;
        tabButtons.clear();
        characterButtons.clear();
        openInBrowserButton = null;
        searchBar = null;
        questSearchBar = null;
        treeSearchBar = null;
        selectedCharacter = null;
        int j = 0;
        for(Tab tab : Tab.values()) {
            tabButtonWidgets.add(new TabButtonWidget(j, tab, this));
            tabButtons.add(new TabButton(0, 0, 0, 0, tab));
            j++;
        }
        for (int i = 0; i < 15; i++) {
            characterButtons.add(new CharacterButton(-1, -1, 0, 0, null));
        }
        this.player = player;
        currentTab = Tab.General;

        initAsyncPlayerData();
    }

    private void initAsyncPlayerData() {
        CompletableFuture.runAsync(() -> {
            try {
                while (PV.currentPlayerData == null) Thread.sleep(50);

                SkinData skin = fetchSkin(PV.currentPlayerData.getUuid());
                GameProfile profile = createProfileWithSkin(PV.currentPlayerData.getUuid(), player, skin);

                MinecraftClient client = MinecraftClient.getInstance();
                ClientWorld world = client.world;

                if (world != null) {
                    dummy = new AbstractClientPlayerEntity(world, profile) {
                        @Override
                        public SkinTextures getSkinTextures() {
                            return client.getSkinProvider().getSkinTextures(getGameProfile());
                        }

                        @Override
                        public boolean isPartVisible(PlayerModelPart part) {
                            return part != PlayerModelPart.CAPE;
                        }
                    };
                    dummy.getSkinTextures().texture();
                }
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });
    }

    @Override
    public void init() {
        super.init();

        rootWidgets.clear();
        lastViewedPlayersWidget.clear();
        if(currentTabWidget instanceof GeneralTabWidget) {
            currentTabWidget = null;
        }
        if(currentTabWidget == null) currentTabWidget = new GeneralTabWidget(this);

        addRootWidget(backgroundImageWidget);
        addRootWidget(darkModeToggleWidget);
        for(TabButtonWidget tabButtonWidget : tabButtonWidgets) {
            addRootWidget(tabButtonWidget);
        }
        for(int i = 0; i < lastViewedPlayers.size(); i++) {
            PlayerWidget widget = new PlayerWidget(i);
            lastViewedPlayersWidget.add(widget);
            addRootWidget(widget);
        }
        addedNewest = false;
        registerScrolling();
        //addRootWidget(hier jetzt alle verschiedenen tabs);
    }

    @Override
    protected void scrollList(float delta) {
        scrollOffset -= (int) (delta);
        if(scrollOffset < 0) scrollOffset = 0;
    }

    @Override
    public void updateValues() {
        if(dummy != null) {
            Identifier dummyTexture = dummy.getSkinTextures().texture();
            lastViewedPlayersSkins.put(PV.currentPlayerData.getUsername(), dummyTexture);
        }

        int xStart = getLogicalWidth() / 2 - 900 - (getLogicalWidth() - 1800 < 200 ? 50 : 0);
        int yStart = getLogicalHeight() / 2 - 375;

        backgroundImageWidget.setBounds(xStart, yStart, 1800, 750);
        darkModeToggleWidget.setBounds(xStart + 1800 - 120, yStart + 750, 120, 60);
        int totalWidth = 24;
        for(TabButtonWidget tabButtonWidget : tabButtonWidgets) {
            int signWidth = drawDynamicNameSign(drawContext, tabButtonWidget.tab.toString(), xStart + totalWidth, yStart - 57);
            //24; //+ totalXOffset + (float) signWidth / 2
            tabButtonWidget.setBounds(xStart + totalWidth, yStart - 55, signWidth, 55);
            tabButtonWidget.setTextOffset(signWidth / 2, 17);
            totalWidth += signWidth + 12;
        }
        if(currentTabWidget == null) return;
        if(!rootWidgets.contains(currentTabWidget)){
            addRootWidget(currentTabWidget);
        }
        currentTabWidget.setBounds(xStart, yStart, 1800, 750);
        if(!rootWidgets.contains(currentTabWidget)) {
            for (int i = 0; i < lastViewedPlayers.size(); i++) {
                PlayerWidget widget = new PlayerWidget(i);
                lastViewedPlayersWidget.add(widget);
                //addRootWidget(widget);
            }
        }
        for(PlayerWidget playerWidget : lastViewedPlayersWidget) {
            playerWidget.draw(super.drawContext, xStart + currentTabWidget.getWidth(), yStart + 100 * playerWidget.index + 30);
        }
        //System.out.println(rootWidgets);
//        for(int i = 0; i < lastViewedPlayers.size(); i++) {
//            ui.drawText(lastViewedPlayers.get(i),  + 110, yStart + 100 * i + 55);
//
//            ui.drawImage(playerTabTexture, xStart + currentTabWidget.getWidth(), yStart + 100 * i + 25, 100, 80);
//
//            //to only draw the head
//            RenderUtils.drawTexturedRect(
//                    super.drawContext.getMatrices(),
//                    lastViewedPlayersSkins.get(lastViewedPlayers.get(i)),
//                    ui.sx(xStart + currentTabWidget.getWidth() + 25), ui.sy(yStart + 100 * i + 35), 0,
//                    ui.sw(60), ui.sh(60),
//                    8, 8, 8, 8,
//                    64, 64
//            );
//        }
    }

    @Override //im drawing the tab stuff in updateValues so the background has to be rendered first that's why this override exists
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        PVScreen.mouseX = mouseX;
        PVScreen.mouseY = mouseY;
        super.applyBlur();

        this.drawContext = context;
        computeScaleAndOffsets();
        if (ui == null) ui = new UIUtils(context, scaleFactor, xStart, yStart);
        else ui.updateContext(context, scaleFactor, xStart, yStart);

        if(PV.currentPlayerData != null && !addedNewest) {
            if(PV.currentPlayerData.getUsername() != null) {
                if (lastViewedPlayers.contains(PV.currentPlayerData.getUsername())) {
                    lastViewedPlayers.remove(PV.currentPlayerData.getUsername());
                } else if (lastViewedPlayers.size() > 6) {
                    lastViewedPlayers.removeLast();
                }
                lastViewedPlayers.addFirst(PV.currentPlayerData.getUsername());
                if (lastViewedPlayers.size() != lastViewedPlayersWidget.size()) {
                    List<PlayerWidget> toRemove = new ArrayList<>();
                    for (Widget widget : rootWidgets) {
                        if (widget instanceof PlayerWidget) {
                            toRemove.add((PlayerWidget) widget);
                        }
                    }
                    rootWidgets.removeAll(toRemove);

                    for (int i = 0; i < lastViewedPlayers.size(); i++) {
                        PlayerWidget widget = new PlayerWidget(i);
                        lastViewedPlayersWidget.add(widget);
                        addRootWidget(widget);
                    }
                }
                addedNewest = true;
            }
        }

        ui.drawBackground();
        backgroundImageWidget.draw(context, mouseX, mouseY, delta, ui);
        updateValues();
        updateVisibleListRange();
        layoutListElements();



        //this still uses the old system, needs to be updated some day

        int xStart = getLogicalWidth() / 2 - 900 - (getLogicalWidth() - 1800 < 200 ? 50 : 0);
        int yStart = getLogicalHeight() / 2 - 374;
        if(openInBrowserButton == null && PV.currentPlayerData != null) {
            openInBrowserButton = new OpenInBrowserButton(-1, -1, (int) (20 * 3 / scaleFactor), (int) (87 * 3 / scaleFactor), "https://wynncraft.com/stats/player/" + PV.currentPlayerData.getUuid());
        }

        if (openInBrowserButton != null) {
            openInBrowserButton.setX((int) (xStart / scaleFactor));
            openInBrowserButton.setY((int) ((yStart + currentTabWidget.getHeight()) / scaleFactor) + 1);
            openInBrowserButton.buttonText = "Open in browser";
            DarkModeToggleWidget.drawImageWithFade(openInBrowserButtonTextureDark, openInBrowserButtonTexture, xStart, yStart + currentTabWidget.getHeight(), 260, 60, ui);
            openInBrowserButton.drawWithTexture(context, null);
        }

        //Player searchbar
        DarkModeToggleWidget.drawImageWithFade(openInBrowserButtonTextureWDark, openInBrowserButtonTextureW, xStart + 267, yStart + currentTabWidget.getHeight(), 300, 60, ui);

        if(searchBar == null || searchBar.getInput().equals("Unknown user")) {
            searchBar = new Searchbar(-1, -1, (int) (14 * 3 / scaleFactor), (int) (100 * 3 / scaleFactor));
            if(PV.currentPlayerData == null) {
                searchBar.setInput("Unknown user");
            } else if(PV.currentPlayerData.getUsername() == null) {
                searchBar.setInput("Unknown user");
            } else {
                searchBar.setInput(PV.currentPlayerData.getUsername());
            }
        }

        if (searchBar != null) {
            searchBar.setX((int) ((xStart + 89 * 3) / ui.getScaleFactor()));
            searchBar.setY((int) ((yStart + currentTabWidget.getHeight() + 8 * 3) / ui.getScaleFactor()));
            searchBar.drawWithoutBackground(context, CustomColor.fromHexString("FFFFFF"));
            //searchBar.draw(context);
        }

        for (Widget w : rootWidgets) {
            if(w instanceof BackgroundImageWidget || w instanceof PlayerWidget) continue;
            w.draw(context, mouseX, mouseY, delta, ui);
        }

        //its to make the tooltips of the player names always render above the class buttons
        for (PlayerWidget w : lastViewedPlayersWidget) {
            w.draw(context, mouseX, mouseY, delta, ui);
        }

        // draw only visible range with small buffer for smoothness
        int start = Math.max(0, firstVisibleIndex - 1);
        int end = Math.min(listElements.size() - 1, lastVisibleIndex + 1);
        for (int i = start; i <= end; i++) {
            WEElement<?> e = listElements.get(i);
            e.draw(context, mouseX, mouseY, delta, ui);
        }
    }

    @Override
    protected void drawContent(DrawContext ctx, int mouseX, int mouseY, float tickDelta) {

    }

    public static Identifier getProfTexture(String prof) {
        return switch (prof) {
            case "mining" -> miningTexture;
            case "woodcutting" -> woodcuttingTexture;
            case "farming" -> farmingTexture;
            case "fishing" -> fishingTexture;
            case "armouring" -> armouringTexture;
            case "tailoring" -> tailoringTexture;
            case "weaponsmithing" -> weaponsmithingTexture;
            case "woodworking" -> woodworkingTexture;
            case "jeweling" -> jewelingTexture;
            case "alchemism" -> alchemismTexture;
            case "scribing" -> scribingTexture;
            case "cooking" -> cookingTexture;
            default -> null;
        };
    }

    public static int getDungeonComps(int i, Map<String, Integer> map) {
        return switch (i) {
            case 0 -> map.getOrDefault("Decrepit Sewers", 0);
            case 1 -> map.getOrDefault("Infested Pit", 0);
            case 2 -> map.getOrDefault("Underworld Crypt", 0);
            case 3 -> map.getOrDefault("Timelost Sanctum", 0);
            case 4 -> map.getOrDefault("Sand-Swept Tomb", 0);
            case 5 -> map.getOrDefault("Ice Barrows", 0);
            case 6 -> map.getOrDefault("Undergrowth Ruins", 0);
            case 7 -> map.getOrDefault("Galleon's Graveyard", 0);
            case 8 -> map.getOrDefault("Fallen Factory", 0);
            case 9 -> map.getOrDefault("Eldritch Outlook", 0);
            case 10 -> map.getOrDefault("Lost Sanctuary", 0);
            default -> 0;
        };
    }

    public static int getCorruptedComps(int i, Map<String, Integer> map) {
        return switch (i) {
            case 0 -> map.getOrDefault("Corrupted Decrepit Sewers", 0);
            case 1 -> map.getOrDefault("Corrupted Infested Pit", 0);
            case 2 -> map.getOrDefault("Corrupted Underworld Crypt", 0);
            case 3 -> map.getOrDefault("Corrupted Timelost Sanctum", 0);
            case 4 -> map.getOrDefault("Corrupted Sand-Swept Tomb", 0);
            case 5 -> map.getOrDefault("Corrupted Ice Barrows", 0);
            case 6 -> map.getOrDefault("Corrupted Undergrowth Ruins", 0);
            case 7 -> map.getOrDefault("Corrupted Galleon's Graveyard", 0);
            case 8 -> map.getOrDefault("Corrupted Lost Sanctuary", 0);
            default -> 0;
        };
    }

    public static String getDungeonName(int i) {
        return switch (i) {
            case 0 -> "Decrepit Sewers";
            case 1 -> "Infested Pit";
            case 2 -> "Underworld Crypt";
            case 3 -> "Timelost Sanctum";
            case 4 -> "Sand-Swept Tomb";
            case 5 -> "Ice Barrows";
            case 6 -> "Undergrowth Ruins";
            case 7 -> "Galleon's Graveyard";
            case 8 -> "Fallen Factory";
            case 9 -> "Eldritch Outlook";
            default -> "";
        };
    }

    public static String getClassName(CharacterData entry) {
        if(entry == null) return "";
        if (entry.getNickname() != null) {
            return "*§o" + entry.getNickname() + "§r";
        } else {
            return entry.getType().charAt(0) + entry.getType().substring(1).toLowerCase();
        }
    }

    @Override
    public void close() {
        PV.currentPlayer = "";
        PV.currentPlayerData = null;
        currentTabWidget = null;
        dummy = null;
        openInBrowserButton = null;
        searchBar = null;
        questSearchBar = null;
        treeSearchBar = null;
        super.close();
    }

    public static Rank getRank() {
        String rank = PV.currentPlayerData.getRank();
        if(rank == null) return Rank.NONE;
        if(rank.equals("Player")) {
            return switch (PV.currentPlayerData.getSupportRank()) {
                case "player" -> Rank.NONE;
                case "vip" -> Rank.VIP;
                case "vipplus" -> Rank.VIPPLUS;
                case "hero" -> Rank.HERO;
                case "heroplus" -> Rank.HEROPLUS;
                case "champion" -> Rank.CHAMPION;
                case null -> Rank.NONE;
                default -> Rank.WYNN;
            };
        } else {
            return switch (rank) {
                case "Media" -> Rank.MEDIA;
                case "Moderator" -> Rank.MOD;
                case "Administrator" -> Rank.ADMIN;
                default -> Rank.WYNN;
            };
        }
    }

    public static Identifier getRankBadge() {
        Rank rank = getRank();
        return switch (rank) {
            case VIP -> vip;
            case VIPPLUS -> vipplus;
            case HERO -> hero;
            case HEROPLUS -> heroplus;
            case CHAMPION -> champion;
            case MEDIA -> media;
            case MOD -> mod;
            case WYNN -> wynn;
            case ADMIN -> admin;
            case null, default -> null;
        };
    }

    public static int getRankBadgeWidth() {
        Rank rank = getRank();
        return switch (rank) {
            case VIP -> 66;
            case VIPPLUS -> 87;
            case HERO -> 93;
            case HEROPLUS -> 114;
            case CHAMPION -> 159;
            case MEDIA, ADMIN -> 105;
            case WYNN -> 90;
            case MOD -> 183;
            case null, default -> 0;
        };
    }

    public static float playerRotationY = 0;
    private static boolean draggingAllowed = false;


    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double deltaX, double deltaY) {
        if (button == 0 && draggingAllowed) {
            playerRotationY -= (float) deltaX * 1.25f;
            playerRotationY %= 360f;
            return true;
        }
        return super.mouseDragged(mouseX, mouseY, button, deltaX, deltaY);
    }


    public void drawPlayer(
            DrawContext context,
            int x, int y, int scale,
            float mouseX, float mouseY,
            LivingEntity player,
            double scaleFactor
    ) {
        float flipOffset = 0;

        Quaternionf rotation = new Quaternionf().rotateZ((float) Math.PI);
        //rotation.rotateX((float) Math.toRadians(pitch));
        rotation.rotateY((float) Math.toRadians(-20 + playerRotationY));

        float sleepOffsetX;
        float sleepOffsetY;

        if(dummy.getPose() == EntityPose.SLEEPING) {
            rotation.rotateY((float) Math.PI * 0.5f);
            rotation.rotateX((float) Math.PI);
            sleepOffsetX = (float) ((float) (60 * 3) / scaleFactor);
            sleepOffsetY = (float) ((float) (10 * 3) / scaleFactor);
        } else {
            sleepOffsetX = 0;
            sleepOffsetY = 0;
        }

        if(PV.currentPlayer.equalsIgnoreCase("teslanator")) {
            rotation.rotateX((float) Math.PI);
            flipOffset = (float) (-130 * 3 / scaleFactor);
            rotation.rotateY((float) Math.PI);
        }


        EntityRenderDispatcher dispatcher = MinecraftClient.getInstance().getEntityRenderDispatcher();
        dispatcher.setRenderShadows(false);

        float smolScale = 1;
        float thickScale = 1;

        if(PV.currentPlayer.equalsIgnoreCase("legendaryvirus")) {
            smolScale = 0.5f;
            thickScale = 1.5f;
        }

        context.getMatrices().push();
        context.getMatrices().translate(sleepOffsetX + x,  sleepOffsetY + flipOffset + y, 50.0);
        context.getMatrices().scale(thickScale * scale, smolScale * scale, scale);
        context.getMatrices().multiply(rotation);

        VertexConsumerProvider.Immediate buffer = MinecraftClient.getInstance().getBufferBuilders().getEntityVertexConsumers();
        dispatcher.render(player, 0, 0, 0, 1.0F, context.getMatrices(), buffer, 15728880);
        buffer.draw();

        context.getMatrices().pop();
        dispatcher.setRenderShadows(true);
    }

    public record SkinData(String value, String signature) {}

    public static SkinData fetchSkin(UUID uuid) throws IOException {
        String url = "https://sessionserver.mojang.com/session/minecraft/profile/" + uuid + "?unsigned=false";
        HttpURLConnection connection = (HttpURLConnection) new URL(url).openConnection();
        connection.setRequestMethod("GET");

        try (InputStream input = connection.getInputStream()) {
            JsonObject json = JsonParser.parseReader(new InputStreamReader(input)).getAsJsonObject();
            JsonArray properties = json.getAsJsonArray("properties");
            JsonObject skinProperty = properties.get(0).getAsJsonObject();
            String value = skinProperty.get("value").getAsString();
            String signature = skinProperty.get("signature").getAsString();
            return new SkinData(value, signature);
        }
    }

    public static GameProfile createProfileWithSkin(UUID uuid, String name, SkinData skin) {
        GameProfile profile = new GameProfile(uuid, name);
        profile.getProperties().put("textures", new Property("textures", skin.value(), skin.signature()));
        return profile;
    }

    public static Identifier getClassTexture(String className) {
        return switch (className) {
            case "WARRIOR" -> warriorTexture;
            case "SHAMAN" -> shamanTexture;
            case "ARCHER" -> archerTexture;
            case "MAGE" -> mageTexture;
            case "ASSASSIN" -> assassinTexture;
            default -> null;
        };
    }

    public static Identifier getGoldClassTexture(String className) {
        return switch (className) {
            case "WARRIOR" -> warriorGoldTexture;
            case "SHAMAN" -> shamanGoldTexture;
            case "ARCHER" -> archerGoldTexture;
            case "MAGE" -> mageGoldTexture;
            case "ASSASSIN" -> assassinGoldTexture;
            default -> null;
        };
    }

    public static void onClick() {
        if(openInBrowserButton == null || searchBar == null || (currentTab == Tab.Quests && questSearchBar == null) || (currentTab == Tab.Tree && treeSearchBar == null)) return;
        if(openInBrowserButton.isClickInBounds(PVScreen.mouseX, PVScreen.mouseY)) {
            McUtils.playSoundUI(SoundEvents.UI_BUTTON_CLICK.value());
            openInBrowserButton.click();
        }
        if(searchBar != null) {
            if (searchBar.isClickInBounds(PVScreen.mouseX, PVScreen.mouseY)) {
                McUtils.playSoundUI(SoundEvents.UI_BUTTON_CLICK.value());
                searchBar.click();
            } else {
                searchBar.setActive(false);
            }
        }
        if(questSearchBar != null) {
            if (questSearchBar.isClickInBounds(PVScreen.mouseX, PVScreen.mouseY)) {
                McUtils.playSoundUI(SoundEvents.UI_BUTTON_CLICK.value());
                questSearchBar.click();
            } else {
                questSearchBar.setActive(false);
            }
        }

        if(treeSearchBar != null) {
            if (treeSearchBar.isClickInBounds(PVScreen.mouseX, PVScreen.mouseY)) {
                McUtils.playSoundUI(SoundEvents.UI_BUTTON_CLICK.value());
                treeSearchBar.click();
            } else {
                treeSearchBar.setActive(false);
            }
        }
    }

    public int drawDynamicNameSign(DrawContext context, String input, int x, int y) {
        TextRenderer textRenderer = MinecraftClient.getInstance().textRenderer;
        int strWidth = textRenderer.getWidth(input) + 10;
        int strMidWidth = strWidth - 15;
        int amount = Math.max(0, Math.ceilDiv(strMidWidth, 10));
        DarkModeToggleWidget.drawImageWithFade(tabLeftDark, tabLeft, x, y, 30, 60, ui);

        for (int i = 0; i < amount; i++) {
            DarkModeToggleWidget.drawImageWithFade(tabMidDark, tabMid, x + 30 * (i + 1), y, 30, 60, ui);
        }

        DarkModeToggleWidget.drawImageWithFade(tagRightDark, tagRight, x + 30 * (amount + 1), y, 30, 60, ui);
        return 60 + amount * 30;
    }

    private static TabWidget getTabWidget(Tab tab, PVScreen pvScreen) {
        return switch (tab) {
            case General -> new GeneralTabWidget(pvScreen);
            case Raids -> new RaidsTabWidget();
            case Rankings -> new RankingsTabWidget();
            case Professions -> new ProfessionsTabWidget();
            case Dungeons -> new DungeonsTabWidget();
            case Quests -> new QuestsTabWidget();
            case Tree -> new TreeTabWidget();
            case Aspects -> new AspectsTabWidget();
            case Misc -> new MiscTabWidget();
            case null, default -> new TabWidget(0, 0, 0, 0);
        };
    }

    public static class BackgroundImageWidget extends Widget {
        public BackgroundImageWidget() {
            super(0, 0, 0, 0);
        }

        @Override
        protected void drawBackground(DrawContext ctx, int mouseX, int mouseY, float tickDelta) {
            if(currentTab == Tab.General) {
                DarkModeToggleWidget.drawImageWithFade(backgroundTextureDark, backgroundTexture, x, y, width, height, ui);
            } else {
                DarkModeToggleWidget.drawImageWithFade(alsobackgroundTextureDark, alsobackgroundTexture, x, y, width, height, ui);
            }
        }

        @Override
        protected void drawContent(DrawContext ctx, int mouseX, int mouseY, float tickDelta) {}

    }

    public static class TabButtonWidget extends Widget {
        int index;
        Tab tab;
        private Runnable action;
        int textXOffset = 0;
        int textYOffset = 0;

        public TabButtonWidget(int index, Tab tab, PVScreen parent) {
            super(0, 0, 0, 0);
            this.index = index;
            this.tab = tab;
            this.action = () -> {
                if(PV.currentPlayerData == null) return;
                McUtils.playSoundUI(SoundEvents.UI_BUTTON_CLICK.value());
                if(tab == currentTab) return;
                currentTab = tab;
                TabWidget tabWidget = getTabWidget(tab, parent);
                if (tabWidget == null || tabWidget.equals(currentTabWidget)) {
                    parent.removeRootWidget(currentTabWidget);
                    currentTabWidget = null;
                } else {
                    parent.removeRootWidget(currentTabWidget);
                    currentTabWidget = tabWidget;
                    scrollOffset = 0;
                    if (!parent.rootWidgets.contains(tabWidget)) {
                        parent.addRootWidget(tabWidget);
                    }
                }
            };
        }

        protected void setTextOffset(int x, int y) {
            this.textXOffset = x;
            this.textYOffset = y;
        }

        @Override
        protected void drawContent(DrawContext ctx, int mouseX, int mouseY, float tickDelta) {
            CustomColor tabStringColor;
            if (tab.equals(currentTab) || hovered) {
                tabStringColor = CustomColor.fromHexString("FFFF00");
            } else if (selectedCharacter == null && (tab.equals(Tab.Professions) || tab.equals(Tab.Quests) || tab.equals(Tab.Tree))) {
                tabStringColor = CustomColor.fromHexString("9e9e9e");
            } else {
                tabStringColor = CustomColor.fromHexString("FFFFFF");
            }
            String tabString = tab.toString();
            //ui.drawRect(x, y, width, height, CustomColor.fromHexString("FFFFFF"));
            ui.drawText(tabString, x + textXOffset, y + textYOffset, tabStringColor, HorizontalAlignment.CENTER, VerticalAlignment.TOP, 3f);
        }

        @Override
        protected boolean onClick(int button) {
            if (!isEnabled()) return false;
            if (action != null) action.run();
            return true;
        }
    }

    public static class TabWidget extends Widget {
        public TabWidget(int x, int y, int width, int height) {
            super(x, y, width, height);
        }

        @Override
        protected void drawContent(DrawContext ctx, int mouseX, int mouseY, float tickDelta) {

        }

        @Override
        protected void drawForeground(DrawContext ctx, int mouseX, int mouseY, float tickDelta) {

        }

        @Override
        public boolean mouseDragged(double mouseX, double mouseY, int button, double deltaX, double deltaY) {
            return false;
        }

        @Override
        public boolean mouseReleased(double mx, double my, int button) {
            return super.mouseReleased(mx, my, button);
        }
    }

    public static class DarkModeToggleWidget extends Widget {
        static Identifier darkmodeToggleBackground = Identifier.of("wynnextras", "textures/gui/profileviewer/darkmodetogglebackground.png");
        static Identifier darkmodeToggleBackgroundDark = Identifier.of("wynnextras", "textures/gui/profileviewer/darkmodetogglebackground_dark.png");

        static Identifier sun = Identifier.of("wynnextras", "textures/gui/profileviewer/sun.png");
        static Identifier moon = Identifier.of("wynnextras", "textures/gui/profileviewer/moon.png");

        public Runnable action;

        public static float targetX;
        public static float currentX;

        public DarkModeToggleWidget() {
            super(0, 0, 120, 0);
            if(SimpleConfig.getInstance(WynnExtrasConfig.class).pvDarkmodeToggle) {
                targetX = width - 37.5f;
            } else {
                targetX = 7.5f;
            }
            this.action = () -> {
                McUtils.playSoundUI(SoundEvents.UI_BUTTON_CLICK.value());
                SimpleConfig.getInstance(WynnExtrasConfig.class).pvDarkmodeToggle = !SimpleConfig.getInstance(WynnExtrasConfig.class).pvDarkmodeToggle;
                if(SimpleConfig.getInstance(WynnExtrasConfig.class).pvDarkmodeToggle) {
                    targetX = width - 37.5f;
                } else {
                    targetX = 7.5f;
                }
                SimpleConfig.save(WynnExtrasConfig.class);
            };
        }

        @Override
        protected void drawContent(DrawContext ctx, int mouseX, int mouseY, float tickDelta) {
            drawImageWithFade(darkmodeToggleBackgroundDark, darkmodeToggleBackground, x, y, width, height, ui);

            float progress = Math.abs(currentX - 7.5f);
            float maxDistance = width - 37.5f - 7.5f;
            float alpha = (progress / maxDistance);

            ui.drawImage(sun, x + currentX, y + 22.5f, 30, 30, 1 - alpha);
            ui.drawImage(moon, x + currentX, y + 22.5f, 30, 30, alpha);

            if(currentX < targetX) {
                currentX += (10f * tickDelta);
                if(currentX >= targetX) {
                    currentX = targetX;
                }
            }

            if(currentX > targetX) {
                currentX -= (10f * tickDelta);
                if(currentX <= targetX) {
                    currentX = targetX;
                }
            }

        }

        @Override
        protected boolean onClick(int button) {
            if (!isEnabled()) return false;
            if (action != null) action.run();
            return true;
        }

        public static void drawImageWithFade(Identifier dark, Identifier light, float x, float y, float width, float height, UIUtils ui) {
            float progress = Math.abs(currentX - 7.5f);
            float maxDistance = 120 - 37.5f - 7.5f;
            float alpha = (progress / maxDistance);

            ui.drawImage(light, x, y, width, height, 1 - alpha);

            ui.drawImage(dark, x, y, width, height, alpha);
        }
    }
}

//TODO: cleanup