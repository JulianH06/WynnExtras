package julianh06.wynnextras.features.profileviewer;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.authlib.GameProfile;
import com.mojang.authlib.properties.Property;
import com.wynntils.utils.colors.CommonColors;
import com.wynntils.utils.colors.CustomColor;
import com.wynntils.utils.mc.McUtils;
import com.wynntils.utils.render.RenderUtils;
import com.wynntils.utils.render.type.HorizontalAlignment;
import com.wynntils.utils.render.type.VerticalAlignment;
import julianh06.wynnextras.config.WynnExtrasConfig;
import julianh06.wynnextras.config.simpleconfig.SimpleConfig;
import julianh06.wynnextras.features.profileviewer.data.*;
import julianh06.wynnextras.features.profileviewer.tabs.*;
import julianh06.wynnextras.utils.MinecraftUtils;
import julianh06.wynnextras.utils.UI.UIUtils;
import julianh06.wynnextras.utils.UI.WEElement;
import julianh06.wynnextras.utils.UI.Widget;
import julianh06.wynnextras.utils.overlays.EasyButton;
import julianh06.wynnextras.utils.UI.WEScreen;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.EntityRenderDispatcher;
import net.minecraft.client.util.InputUtil;
import net.minecraft.client.util.SkinTextures;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.EntityPose;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerModelPart;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import org.joml.Quaternionf;


import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.DecimalFormat;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.CompletableFuture;

public class PVScreen extends WEScreen {
    public static int mouseX = 0;
    public static int mouseY = 0;

    public enum Rank {NONE, VIP, VIPPLUS, HERO, HEROPLUS, CHAMPION, MEDIA, WYNN, MOD, ADMIN}

    public enum Tab {General, Raids, Rankings, Professions, Dungeons, Quests, Misc}
    public static List<TabButton> tabButtons = new ArrayList<>();
    public static List<CharacterButton> characterButtons = new ArrayList<>();

    public static List<String> WETeam = List.of("JulianH06", "Teslanator", "Mikecraft1224", "LegendaryVirus", "elwood24", "BaltrazYT");

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

    static OpenInBroserButton openInBrowserButton;
    public static Searchbar searchBar;
    public static Searchbar questSearchBar;

    public static Tab currentTab = Tab.General;

    String player;
    public static AbstractClientPlayerEntity dummy;

    public static CharacterData selectedCharacter;

    public static int scrollOffset = 0;
    private static long lastScrollTime = 0;
    private static final long scrollCooldown = 0; // in ms

    BackgroundImageWidget backgroundImageWidget = new BackgroundImageWidget();
    List<TabButtonWidget> tabButtonWidgets = new ArrayList<>();
    static TabWidget currentTabWidget;

    public PVScreen(String name) {
        super(Text.of("Player Viewer"));
        String player;
        if(name == null && McUtils.player() == null) player = "null";
        else if(name == null) player = McUtils.playerName();
        else player = name;
        tabButtons.clear();
        characterButtons.clear();
        openInBrowserButton = null;
        searchBar = null;
        questSearchBar = null;
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
        if(currentTabWidget instanceof GeneralTabWidget) {
            currentTabWidget = null;
        }
        if(currentTabWidget == null) currentTabWidget = new GeneralTabWidget();

        addRootWidget(backgroundImageWidget);
        for(TabButtonWidget tabButtonWidget : tabButtonWidgets) {
            addRootWidget(tabButtonWidget);
        }
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
        int xStart = getLogicalWidth() / 2 - 900;
        int yStart = getLogicalHeight() / 2 - 375;
        backgroundImageWidget.setBounds(xStart, yStart, 1800, 750);
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


        ui.drawBackground();
        backgroundImageWidget.draw(context, mouseX, mouseY, delta, ui);
        updateValues();
        updateVisibleListRange();
        layoutListElements();

        for (Widget w : rootWidgets) {
            if(w instanceof BackgroundImageWidget) continue;
            w.draw(context, mouseX, mouseY, delta, ui);
        }

        // draw only visible range with small buffer for smoothness
        int start = Math.max(0, firstVisibleIndex - 1);
        int end = Math.min(listElements.size() - 1, lastVisibleIndex + 1);
        for (int i = start; i <= end; i++) {
            WEElement<?> e = listElements.get(i);
            e.draw(context, mouseX, mouseY, delta, ui);
        }

        //this still uses the old system, needs to be updated some day

        int xStart = getLogicalWidth() / 2 - 900;
        int yStart = getLogicalHeight() / 2 - 374;
        if(openInBrowserButton == null && PV.currentPlayerData != null) {
            openInBrowserButton = new OpenInBroserButton(-1, -1, (int) (20 * 3 / scaleFactor), (int) (87 * 3 / scaleFactor), "https://wynncraft.com/stats/player/" + PV.currentPlayerData.getUuid());
        }

        if (openInBrowserButton != null) {
            openInBrowserButton.setX((int) (xStart / scaleFactor));
            openInBrowserButton.setY((int) ((yStart + currentTabWidget.getHeight()) / scaleFactor) + 1);
            openInBrowserButton.buttonText = "Open in browser";
            if(SimpleConfig.getInstance(WynnExtrasConfig.class).darkmodeToggle) {
                openInBrowserButton.drawWithTexture(context, openInBrowserButtonTextureDark);
            } else {
                openInBrowserButton.drawWithTexture(context, openInBrowserButtonTexture);
            }
        }

        //Player searchbar
        if(SimpleConfig.getInstance(WynnExtrasConfig.class).darkmodeToggle) {
            drawImage(openInBrowserButtonTextureWDark, xStart + 267, yStart + currentTabWidget.getHeight(), 300, 60);
        } else {
            drawImage(openInBrowserButtonTextureW, xStart + 267, yStart + currentTabWidget.getHeight(), 300, 60);
        }

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
            searchBar.setY((int) ((yStart + currentTabWidget.getHeight() + 7 * 3) / ui.getScaleFactor()));
            searchBar.drawWithoutBackground(context, CustomColor.fromHexString("FFFFFF"));
            //searchBar.draw(context);
        }
    }

    @Override
    public void testDraw(DrawContext context, int mouseX, int mouseY, float delta) {
        if(MinecraftClient.getInstance().getWindow() == null) return;
        super.drawContext = context;
        super.scaleFactor = MinecraftClient.getInstance().getWindow().getScaleFactor();
        scaleFactor = (int) super.scaleFactor;
        if(scaleFactor == 0) return;

        if(openInBrowserButton == null && PV.currentPlayerData != null) {
            openInBrowserButton = new OpenInBroserButton(-1, -1, (int) (20 * 3 / scaleFactor), (int) (87 * 3 / scaleFactor), "https://wynncraft.com/stats/player/" + PV.currentPlayerData.getUuid());
        }

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

        screenWidth = MinecraftClient.getInstance().getWindow().getScaledWidth();
        screenHeight = MinecraftClient.getInstance().getWindow().getScaledHeight();
        width = (int) (600 * 3 / scaleFactor);
        height = (int) (250 * 3 / scaleFactor);
        xStart = screenWidth / 2 - width / 2;
        yStart = screenHeight / 2 - height / 2;

        PVScreen.mouseX = mouseX;
        PVScreen.mouseY = mouseY;

        if(questSearchBar == null && PV.currentPlayerData != null) {
            questSearchBar = new Searchbar( -1, -1, -1, -1);
            questSearchBar.setSearchText("Search...");
        }

        if(currentTab == Tab.General) {
            if(SimpleConfig.getInstance(WynnExtrasConfig.class).darkmodeToggle) ui.drawImage(backgroundTextureDark, (float) 0, (float) 0, (float) (width * scaleFactor), (float) (height * scaleFactor));
            else ui.drawImage(backgroundTexture, 0, 0, width, height);
        } else {
            if(SimpleConfig.getInstance(WynnExtrasConfig.class).darkmodeToggle) ui.drawImage(alsobackgroundTextureDark, (float) 0, (float) 0, (float) (width * scaleFactor), (float) (height * scaleFactor));
            else ui.drawImage(alsobackgroundTexture, 0, 0, (float)(width * scaleFactor),(float)( height * scaleFactor));
        }

        int j = 0;
        int totalXOffset = 0;
        for (Tab tab : Tab.values()) {
            EasyButton tabButton = tabButtons.get(j);
            CustomColor tabStringColor;
            if (tab.equals(currentTab)) {
                tabStringColor = CustomColor.fromHexString("FFFF00");
            } else if (selectedCharacter == null && (tab.equals(Tab.Professions) || tab.equals(Tab.Quests))) {
                tabStringColor = CustomColor.fromHexString("9e9e9e");
            } else {
                tabStringColor = CustomColor.fromHexString("FFFFFF");
            }
            String tabString = tab.toString();
            int signWidth = drawDynamicNameSign(context, tabString, 24 + totalXOffset, -57);
            float centerX = 24 + totalXOffset + (float) signWidth / 2;

            drawText(tabString, centerX - 1, -36, tabStringColor, HorizontalAlignment.CENTER, VerticalAlignment.TOP, 3f);
            tabButton.setX((int) (xStart + 8 * 3 / scaleFactor + totalXOffset / scaleFactor));
            tabButton.setY((int) (yStart - 19 * 3 / scaleFactor));
            tabButton.setWidth((float) ((float) signWidth / scaleFactor));
            tabButton.setHeight((float) ((float) (20 * 3) / scaleFactor));

            totalXOffset += signWidth + 12;
            j++;
        }

        if(PV.currentPlayerData == null) return;

        switch (currentTab) {
            case Tab.General -> {
                Identifier rankBadge = getRankBadge();
                int rankBadgeWidth = getRankBadgeWidth();
                String rankColorHexString;
                if (PV.currentPlayerData.getLegacyRankColour() != null) {
                    rankColorHexString = PV.currentPlayerData.getLegacyRankColour().getMain();
                } else {
                    rankColorHexString = "AAAAAA";
                }
                if (rankBadge != null) {
                    drawImage(rankBadge, 15, 18, rankBadgeWidth, 27);
                }

                drawText(" " + PV.currentPlayerData.getUsername(), 10 + rankBadgeWidth, 21, CustomColor.fromHexString(rankColorHexString), 3f);

                if (PV.currentPlayerData.isOnline()) {
                    if(SimpleConfig.getInstance(WynnExtrasConfig.class).darkmodeToggle) {
                        drawImage(onlineCircleTextureDark, 15, 60, 33, 33);
                    } else {
                        drawImage(onlineCircleTexture, 15, 60, 33, 33);
                    }
                    drawText(PV.currentPlayerData.getServer(), 57, 66, CustomColor.fromHexString("FFFFFF"), 3f);
                } else {
                    if(SimpleConfig.getInstance(WynnExtrasConfig.class).darkmodeToggle) {
                        drawImage(offlineCircleTextureDark, 15, 60, 33, 33);
                    } else {
                        drawImage(offlineCircleTexture, 15, 60, 33, 33);
                    }
                    DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm");
                    String formatted;
                    if (PV.currentPlayerData.getLastJoin() == null) {
                        formatted = "Unknown!";
                    } else {
                        formatted = PV.currentPlayerData.getLastJoin().format(formatter);
                    }
                    drawText("Last seen: " + formatted, 57, 66, CustomColor.fromHexString("FFFFFF"), 3f);
                }
                if (dummy != null) {
                    if (InputUtil.isKeyPressed(MinecraftClient.getInstance().getWindow().getHandle(), InputUtil.GLFW_KEY_LEFT_SHIFT)) {
                        dummy.setPose(EntityPose.CROUCHING);
                        drawPlayer(context, (int) (xStart + 66 / scaleFactor + 216 / scaleFactor), (int) (yStart + 102 / scaleFactor + 387 / scaleFactor), (int) (210 / scaleFactor), mouseX, mouseY, dummy, scaleFactor); //166 178
                    } else if (InputUtil.isKeyPressed(MinecraftClient.getInstance().getWindow().getHandle(), InputUtil.GLFW_KEY_RIGHT_SHIFT)) {
                        dummy.setPose(EntityPose.SLEEPING);
                        drawPlayer(context, (int) (xStart + 66 / scaleFactor), (int) (yStart + 102 / scaleFactor + 357 / scaleFactor), (int) (210 / scaleFactor), mouseX, mouseY, dummy, scaleFactor); //166 178
                    } else {
                        dummy.setPose(EntityPose.STANDING);
                        drawPlayer(context, (int) (xStart + 66 / scaleFactor + 216 / scaleFactor), (int) (yStart + 102 / scaleFactor + 414 / scaleFactor), (int) (210 / scaleFactor), mouseX, mouseY, dummy, scaleFactor); //166 178
                    }
                }

                if (PV.currentPlayerData.getCharacters() != null) {
                    int i = 0;
                    Map<String, CharacterData> map = PV.currentPlayerData.getCharacters();
                    List<CharacterData> sortedCharacterList = new ArrayList<>(map.values());

                    sortedCharacterList.sort(
                        Comparator.comparing(CharacterData::getLevel).thenComparing(CharacterData::getTotalLevel).thenComparing(CharacterData::getContentCompletion).thenComparing(CharacterData::getPlaytime)
                    );

                    for (CharacterData entry : sortedCharacterList.reversed()) {
                        Identifier classTexture;
                        if(entry.getLevel() == 106) {
                            classTexture = getGoldClassTexture(entry.getType());
                        } else {
                             classTexture = getClassTexture(entry.getType());
                        }

                        int entryX = 576 + 411 * (i % 3);
                        int entryY = 15 + 144 * Math.floorDiv(i, 3);
                        characterButtons.get(i).setCharacter(entry);
                        characterButtons.get(i).setX((int) (xStart + 192 * 3 / scaleFactor + (137 * 3 / scaleFactor) * (i % 3)));
                        characterButtons.get(i).setY((int) (yStart + 5 * 3 / scaleFactor + (48 * 3 / scaleFactor) * Math.floorDiv(i, 3)));
                        characterButtons.get(i).setWidth((float) ((float) 390 / scaleFactor));
                        characterButtons.get(i).setHeight((float) ((float) 132 / scaleFactor));
                        if(selectedCharacter == entry) {
                            if(SimpleConfig.getInstance(WynnExtrasConfig.class).darkmodeToggle) {
                                drawImage(classBackgroundTextureActiveDark, entryX, entryY, 390, 132);
                            } else {
                                drawImage(classBackgroundTextureActive, entryX, entryY, 390, 132);
                            }
                        } else if(entry.getTotalLevel() != 1690) {
                            if(SimpleConfig.getInstance(WynnExtrasConfig.class).darkmodeToggle) {
                                drawImage(classBackgroundTextureDark, entryX, entryY, 390, 132);
                            } else {
                                drawImage(classBackgroundTexture, entryX, entryY, 390, 132);
                            }
                        } else {
                            if(SimpleConfig.getInstance(WynnExtrasConfig.class).darkmodeToggle) {
                                drawImage(classBackgroundTextureGoldDark, entryX, entryY, 390, 132);
                            } else {
                                drawImage(classBackgroundTextureGold, entryX, entryY, 390, 132);
                            }
                        }

                        if (classTexture != null) {
                            int level = entry.getLevel();
                            int totalLevel = entry.getTotalLevel();
                            CustomColor levelColor;
                            if (entry.getContentCompletion() == 1133) {
                                levelColor = CommonColors.RAINBOW;
                            } else {
                                levelColor = CustomColor.fromHexString("FFFFFF");
                            }

                            drawImage(classTexture, entryX + 12, entryY + 12, 90, 102);
                            drawText(getClassName(entry), entryX + 111, entryY + 18, levelColor, 2.1f);
                            drawText("Level " + level, entryX + 111, entryY + 42, levelColor, 2.1f);
                            drawText("Total Level " + totalLevel, entryX + 111, entryY + 66, levelColor, 2.1f);
                            drawText("Completion " + (entry.getContentCompletion() * 100/1133) + "%", entryX + 111, entryY + 90, levelColor, 2.1f);
                        }

                        List<String> gamemodes = entry.getGamemode();
                        int k = 0;
                        if(gamemodes != null) {
                            if(gamemodes.contains("ultimate_ironman")) {
                                drawImage(ultimateIronmanTexture, entryX + 350, entryY + 85, 30, 30);
                                k++;
                            } else if (gamemodes.contains("ironman")) {
                                drawImage(ironmanTexture, entryX + 350, entryY + 85, 30, 30);
                                k++;
                            }
                            if(gamemodes.contains("hunted")) {
                                drawImage(huntedTexture, entryX - ((k % 2) * 35) + 350, entryY + 85 - (Math.floorDiv(k, 2) * 35), 30, 30);
                                k++;
                            }
                            if(gamemodes.contains("hardcore")) {
                                if(entry.getDeaths() == 0) {
                                    drawImage(hardcoreTexture, entryX - ((k % 2) * 35) + 350, entryY + 85 - (Math.floorDiv(k, 2) * 35), 30, 30);
                                } else {
                                    drawImage(hardcoreFailedTexture, entryX - ((k % 2) * 35) + 350, entryY + 85 - (Math.floorDiv(k, 2) * 35), 30, 30);
                                }
                                k++;
                            }
                            if(gamemodes.contains("craftsman")) {
                                drawImage(craftsmanTexture, entryX - ((k % 2) * 35) + 350, entryY + 85 - (Math.floorDiv(k, 2) * 35), 30, 30);
                            }
                        }
                        i++;
                    }
                } else {
                    drawText("This player has their classes private.", 900, 345, CustomColor.fromHexString("FF0000"), 3f);
                }

                if (PV.currentPlayerData.getGuild() != null) {
                    String guildString = "[" + PV.currentPlayerData.getGuild().getPrefix() + "] " + PV.currentPlayerData.getGuild().getName();
                    String rankString = PV.currentPlayerData.getGuild().getRankStars() + " " + PV.currentPlayerData.getGuild().getRank() + " of " + PV.currentPlayerData.getGuild().getRankStars();
                    drawCenteredText(rankString, 285, 570, CustomColor.fromHexString("00FFFF"), 3f);
                    drawCenteredText(guildString, 285, 600, CustomColor.fromHexString("FFFFFF"), 3f);
                }

                if (PV.currentPlayerData.getFirstJoin() != null) {
                    DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd.MM.yyyy");
                    String formatted = "First joined: ";
                    formatted += PV.currentPlayerData.getFirstJoin().format(formatter);
                    drawCenteredText(formatted, 285, 630, CustomColor.fromHexString("FFFFFF"), 3f);
                }

                if (PV.currentPlayerData.getPlaytime() != 0) {
                    drawCenteredText("Total Playtime: " + Math.round(PV.currentPlayerData.getPlaytime()) + "h", 285, 660, CustomColor.fromHexString("FFFFFF"), 3f);
                }

                if(selectedCharacter != null) {
                    if(selectedCharacter.getPlaytime() != 0) {
                        drawCenteredText("Class Playtime: " + Math.round(selectedCharacter.getPlaytime()) + "h", 285, 690, CustomColor.fromHexString("FFFFFF"), 3f);
                    }
                }

                if(WETeam != null && PV.currentPlayerData.getUsername() != null) {
                    if (WETeam.contains(PV.currentPlayerData.getUsername())) {
                        drawCenteredText("★★★ WynnExtras Team Member ★★★", 285, 720, CommonColors.SHINE, 3f);
                    }
                }
            }
            case Raids -> {
                DecimalFormat formatter = new DecimalFormat("#,###");
                if(PV.currentPlayerData.getGlobalData() == null) {
                    drawCenteredText("This player has their raid stats private.", 900, 345, CustomColor.fromHexString("FF0000"), 5f);
                    break;
                }

                if(SimpleConfig.getInstance(WynnExtrasConfig.class).darkmodeToggle) {
                    drawImage(raidBackgroundTextureDark, 30, 90, 825, 300);
                    drawImage(raidBackgroundTextureDark, 945, 90, 825, 300);
                    drawImage(raidBackgroundTextureDark, 30, 420, 825, 300);
                    drawImage(raidBackgroundTextureDark, 945, 420, 825, 300);
                } else {
                    drawImage(raidBackgroundTexture, 30, 90, 825, 300);
                    drawImage(raidBackgroundTexture, 945, 90, 825, 300);
                    drawImage(raidBackgroundTexture, 30, 420, 825, 300);
                    drawImage(raidBackgroundTexture, 945, 420, 825, 300);
                }
                drawImage(NOTGTexture, 30, 90, 300, 300);
                drawImage(TCCTexture, 1470, 90, 300, 300);
                drawImage(NOLTexture, 30, 420, 300, 300);
                drawImage(TNATexture, 1470, 420, 300, 300);

                Map<String, Long> ranking = null;
                if(selectedCharacter == null) {
                    ranking = PV.currentPlayerData.getRanking();
                }
                long NOTGRank;
                long NOLRank;
                long TCCRank;
                long TNARank;
                CustomColor notgColor = CustomColor.fromHexString("FFFFFF");
                CustomColor nolColor = CustomColor.fromHexString("FFFFFF");
                CustomColor tccColor = CustomColor.fromHexString("FFFFFF");
                CustomColor tnaColor = CustomColor.fromHexString("FFFFFF");
                if(ranking != null) {

                    NOTGRank = ranking.getOrDefault("grootslangCompletion", -1L);
                    if(NOTGRank <= 100 && NOTGRank > 0) notgColor = CommonColors.RAINBOW;

                    NOLRank = ranking.getOrDefault("orphionCompletion", -1L);
                    if(NOLRank <= 100 && NOLRank > 0) nolColor = CommonColors.RAINBOW;

                    TCCRank = ranking.getOrDefault("colossusCompletion", -1L);
                    if(TCCRank <= 100 && TCCRank > 0) tccColor = CommonColors.RAINBOW;

                    TNARank = ranking.getOrDefault("namelessCompletion", -1L);
                    if(TNARank <= 100 && TNARank > 0) tnaColor = CommonColors.RAINBOW;

                    if(NOTGRank != -1) {
                        drawText("Rank #" + formatter.format(NOTGRank), 345f, 255f, notgColor, 3.9f);
                    }
                    if(NOLRank != -1) {
                        drawText("Rank #" + formatter.format(NOLRank), 345f, 585f, nolColor, 3.9f);
                    }
                    if(TCCRank != -1) {
                        drawText("Rank #" + formatter.format(TCCRank), 1470f, 255f, tccColor, HorizontalAlignment.RIGHT, VerticalAlignment.TOP, 3.9f);
                    }
                    if(TNARank != -1) {
                        drawText("Rank #" + formatter.format(TNARank), 1470f, 585f, tnaColor, HorizontalAlignment.RIGHT, VerticalAlignment.TOP, 3.9f);
                    }
                }

                Raids raids;
                String characterNameString;
                if(selectedCharacter != null && selectedCharacter.getRaids() != null) {
                    characterNameString = " on " + getClassName(selectedCharacter) + ": ";
                    raids = selectedCharacter.getRaids();
                } else {
                    characterNameString = ": ";
                    raids = PV.currentPlayerData.getGlobalData().getRaids();
                }

                drawText("Nest of the Grootslangs", 345f, 165f, notgColor, 3.9f);
                drawText("Orphion's Nexus of Light", 345f, 495f, nolColor, 3.9f);
                drawText("The Canyon Colossus", 1470f, 165f, tccColor, HorizontalAlignment.RIGHT, VerticalAlignment.TOP, 3.9f);
                drawText("The Nameless Anomaly", 1470f, 495f, tnaColor, HorizontalAlignment.RIGHT, VerticalAlignment.TOP, 3.9f);

                if(raids != null) {
                    long NOTGComps = raids.getList().getOrDefault("Nest of the Grootslangs", 0);
                    long NOLComps = raids.getList().getOrDefault("Orphion's Nexus of Light", 0);
                    long TCCComps = raids.getList().getOrDefault("The Canyon Colossus", 0);
                    long TNAComps = raids.getList().getOrDefault("The Nameless Anomaly", 0);
                    long TotalComps = raids.getTotal();

                    drawText(formatter.format(NOTGComps) + " Completions", 345f, 210f, notgColor, 3.9f);
                    drawText(formatter.format(NOLComps) + " Completions", 345f, 540f, nolColor, 3.9f);
                    drawText(formatter.format(TCCComps) + " Completions", 1470f, 210f, tccColor, HorizontalAlignment.RIGHT, VerticalAlignment.TOP, 3.9f);
                    drawText(formatter.format(TNAComps) + " Completions", 1470f, 540f, tnaColor, HorizontalAlignment.RIGHT, VerticalAlignment.TOP, 3.9f);

                    drawCenteredText("Total Completions" + characterNameString + formatter.format(TotalComps), 900f, 48f, CustomColor.fromHexString("FFFFFF"), 3.9f);
                }

            }
            case Rankings -> {
                Map<String, Long> rankings = PV.currentPlayerData.getRanking();
                if(rankings == null) {
                    drawCenteredText("This player has their rankings private.", 900, 345, CustomColor.fromHexString("FF0000"), 5f);
                    break;
                }

                for(int i = 0; i < 20; i++) {
                    if(i == 15) continue;
                    if(i > 18) continue;
                    int xPos = 30 + (435 * (i % 4));
                    int yPos = 30 + (138 * Math.floorDiv(i, 4));

                    Identifier texture = switch (i) {
                        case 0 -> fishingTexture;
                        case 1 -> woodcuttingTexture;
                        case 2 -> miningTexture;
                        case 3 -> farmingTexture;
                        case 4 -> scribingTexture;
                        case 5 -> jewelingTexture;
                        case 6 -> alchemismTexture;
                        case 7 -> cookingTexture;
                        case 8 -> weaponsmithingTexture;
                        case 9 -> tailoringTexture;
                        case 10 -> woodworkingTexture;
                        case 11 -> armouringTexture;
                        case 12 -> warsCompletionTexture;
                        case 13 -> playerContentTexture;
                        case 14 -> globalPlayerContent;
                        case 16 -> combatLevelTexture;
                        case 17 -> totalLevelTexture;
                        case 18 -> professionLevelTexture;
                        default -> null;
                    };

                    String text = switch (i) {
                        case 0 -> "Fishing";
                        case 1 -> "Woodcutting";
                        case 2 -> "Mining";
                        case 3 -> "Farming";
                        case 4 -> "Scribing";
                        case 5 -> "Jeweling";
                        case 6 -> "Alchemism";
                        case 7 -> "Cooking";
                        case 8 -> "Weaponsmithing";
                        case 9 -> "Tailoring";
                        case 10 -> "Woodworking";
                        case 11 -> "Armouring";
                        case 12 -> "Wars completed";
                        case 13 -> "Player content completion";
                        case 14 -> "Global content completion";
                        case 16 -> "Combat level";
                        case 17 -> "Total level";
                        case 18 -> "Profession level";
                        default -> null;
                    };

                    Long globalPlacement = switch (i) {
                        case 0 -> rankings.get("fishingLevel");
                        case 1 -> rankings.get("woodcuttingLevel");
                        case 2 -> rankings.get("miningLevel");
                        case 3 -> rankings.get("farmingLevel");
                        case 4 -> rankings.get("scribingLevel");
                        case 5 -> rankings.get("jewelingLevel");
                        case 6 -> rankings.get("alchemismLevel");
                        case 7 -> rankings.get("cookingLevel");
                        case 8 -> rankings.get("weaponsmithingLevel");
                        case 9 -> rankings.get("tailoringLevel");
                        case 10 -> rankings.get("woodworkingLevel");
                        case 11 -> rankings.get("armouringLevel");
                        case 12 -> rankings.get("warsCompletion");
                        case 13 -> rankings.get("playerContent");
                        case 14 -> rankings.get("globalPlayerContent");
                        case 16 -> rankings.get("combatGlobalLevel");
                        case 17 -> rankings.get("totalGlobalLevel");
                        case 18 -> rankings.get("professionsGlobalLevel");
                        default -> null;
                    };

                    Long soloPlacement = switch (i) {
                        case 16 -> rankings.get("combatSoloLevel");
                        case 17 -> rankings.get("totalSoloLevel");
                        case 18 -> rankings.get("professionsSoloLevel");
                        default -> null;
                    };

                    DecimalFormat formatter = new DecimalFormat("#,###");

                    String globalPlacementString;
                    String soloPlacementString;

                    if(globalPlacement == null) {
                        globalPlacement = -1L;
                        globalPlacementString = "???";
                    } else {
                        globalPlacementString = formatter.format(globalPlacement);
                    }

                    if(soloPlacement == null) {
                        soloPlacement = -1L;
                        soloPlacementString = "???";
                    } else {
                        soloPlacementString = formatter.format(soloPlacement);
                    }


                    CustomColor textColor = CustomColor.fromHexString("FFFFFF");
                    if(globalPlacement <= 100 && globalPlacement > 0) {
                        textColor = CommonColors.RAINBOW;
                    }

                    if(i < 12) {
                        if(SimpleConfig.getInstance(WynnExtrasConfig.class).darkmodeToggle) {
                            drawImage(rankingBackgroundTextureDark, xPos, yPos, 420, 126);
                        } else {
                            drawImage(rankingBackgroundTexture, xPos, yPos, 420, 126);
                        }
                        drawImage(texture, xPos + 12, yPos + 18, 90, 90);
                        drawText(text, xPos + 111, yPos + 36, textColor);
                        drawText("#" + globalPlacementString, xPos + 111, yPos + 66, textColor);
                    } else if(i < 16){
                        xPos += (144 * (i % 3));
                        if(SimpleConfig.getInstance(WynnExtrasConfig.class).darkmodeToggle) {
                            drawImage(rankingBackgroundWideTextureDark, xPos, yPos, 567, 126);
                        } else {
                            drawImage(rankingBackgroundWideTexture, xPos, yPos, 567, 126);
                        }
                        drawImage(texture, xPos + 12, yPos + 18, 90, 90);
                        drawText(text, xPos + 111f, yPos + 36f, textColor);
                        drawText("#" + globalPlacementString, xPos + 111f, yPos + 66f, textColor);
                    } else {
                        xPos += (144 * ((i - 1) % 3));
                        if(SimpleConfig.getInstance(WynnExtrasConfig.class).darkmodeToggle) {
                            drawImage(rankingBackgroundWideTextureDark, xPos, yPos, 567, 126);
                        } else {
                            drawImage(rankingBackgroundWideTexture, xPos, yPos, 567, 126);
                        }
                        drawImage(texture, xPos + 12, yPos + 18, 90, 90);
                        drawText(text, xPos + 111f, yPos + 21f, textColor);
                        drawText("#" + globalPlacementString, xPos + 111f, yPos + 51f, textColor);
                        drawText("Solo #" + soloPlacementString, xPos + 111f, yPos + 81f, textColor);
                    }
                }
            }
            case Professions -> {
                if(selectedCharacter == null) {
                    drawCenteredText("Select a character to view professions.", 900, 345, CustomColor.fromHexString("FF0000"), 5f);
                    break;
                }
                Map<String, Profession> profs = selectedCharacter.getProfessions();
                if(profs == null) {
                    drawCenteredText("This player has their profession stats private.", 900, 345, CustomColor.fromHexString("FF0000"), 5f);
                    break;
                }

                if(SimpleConfig.getInstance(WynnExtrasConfig.class).darkmodeToggle) {
                    drawImage(profBackgroundTextureDark, 30, 30, 1740, 690);
                } else {
                    drawImage(profBackgroundTexture, 30, 30, 1740, 690);
                }

                int i = 0;
                for(Map.Entry<String, Profession> prof : profs.entrySet()) {
                    Identifier profTexture = getProfTexture(prof.getKey());
                    int level = prof.getValue().getLevel();
                    CustomColor levelColor;
                    if(i < 4) {
                        drawImage(profTexture, 210 + i * 408, 60, 192, 192);
                        if(level == 132) {
                            levelColor = CommonColors.RAINBOW;
                        } else if (level >= 110) {
                            levelColor = CommonColors.YELLOW;
                        } else {
                            levelColor = CustomColor.fromHexString("FFFFFF");
                        }
                        drawCenteredText("Level " + level, 306 + i * 408, 300, levelColor, 6f);
                        if(level < 132) {
                            drawCenteredText("Progress to next Level: " + prof.getValue().getXpPercent() + "%", 306 + i * 408, 340, levelColor, 2.4f);
                        }
                    } else {
                        drawImage(profTexture, 132 + (i - 4) * 204, 600, 96, 96);
                        if(level == 132) {
                            levelColor = CommonColors.RAINBOW;
                        } else if (level >= 103) {
                            levelColor = CommonColors.YELLOW;
                        } else {
                            levelColor = CustomColor.fromHexString("FFFFFF");
                        }
                        drawCenteredText("Level " + level, 180 + (i - 4) * 204, 575, levelColor, 3.6f);
                        if(level < 132) {
                            drawCenteredText("Progress to", 180 + (i - 4) * 204, 520, levelColor, 2.4f);
                            drawCenteredText("next Level: " + prof.getValue().getXpPercent() + "%", 180 + (i - 4) * 204, 544, levelColor, 2.4f);
                        }
                    }

                    i++;
                }
            }
            case Dungeons -> {
                if(PV.currentPlayerData.getGlobalData() == null) {
                    drawCenteredText("This player has their dungeon stats private.", 900, 345, CustomColor.fromHexString("FF0000"), 5f);
                    break;
                }
                if (PV.currentPlayerData.getGlobalData().getDungeons() == null) {
                    drawCenteredText("This player has their dungeon stats private.", 900, 345, CustomColor.fromHexString("FF0000"), 5f);
                    break;
                }

                Map<String, Integer> normalComps = new HashMap<>();
                Map<String, Integer> corruptedComps = new HashMap<>();

                Dungeons dungeons;
                if(selectedCharacter == null) {
                    dungeons = PV.currentPlayerData.getGlobalData().getDungeons();
                } else {
                    dungeons = selectedCharacter.getDungeons();

                    if(selectedCharacter.getDungeons() == null) {
                        dungeons = new Dungeons();
                    }
                }

                for (Map.Entry<String, Integer> entry : dungeons.getList().entrySet()) {
                    if (entry.getKey().contains("Corrupted")) {
                        corruptedComps.put(entry.getKey(), entry.getValue());
                    } else {
                        normalComps.put(entry.getKey(), entry.getValue());
                    }
                }

                if(SimpleConfig.getInstance(WynnExtrasConfig.class).darkmodeToggle) {
                    drawImage(dungeonBackgroundTextureDark, 30, 87, 1740, 633);
                } else {
                    drawImage(dungeonBackgroundTexture, 30, 87, 1740, 633);
                }

                int i = 0;

                DecimalFormat formatter = new DecimalFormat("#,###");
                for(Identifier dungeon : dungeonTextures) {
                    int comps = getDungeonComps(i, normalComps);
                    int cComps = getCorruptedComps(i, corruptedComps);
                    int dungeonX = 90 + 345 * (i % 5);
                    int dungeonY = 90 + Math.floorDiv(i, 5) * 350;
                    if(Math.floorDiv(i, 5) > 0) {
                        drawImage(dungeon, dungeonX + 30, dungeonY + 45, 180, 180);
                        drawCenteredText(getDungeonName(i), dungeonX + 120, dungeonY + 250, CustomColor.fromHexString("FFFFFF"), 3f);

                        if(i < 8) {
                            drawImage(dungeonKeyTexture, dungeonX + 60, dungeonY - 15, 60, 60);
                            drawText(formatter.format(comps), dungeonX + 55, dungeonY, CustomColor.fromHexString("FFFFFF"), HorizontalAlignment.RIGHT, VerticalAlignment.TOP, 3f);
                        } else {
                            drawImage(dungeonKeyTexture, dungeonX + 90, dungeonY - 15, 60, 60);
                            drawText(formatter.format(comps), dungeonX + 90, dungeonY, CustomColor.fromHexString("FFFFFF"), HorizontalAlignment.RIGHT, VerticalAlignment.TOP, 3f);
                        }

                        if(i < 8) {
                            drawImage(corruptedDungeonKeyTexture, dungeonX + 120, dungeonY - 15, 60, 60);
                            drawText(formatter.format(cComps), dungeonX + 190, dungeonY, CustomColor.fromHexString("FFFFFF"));
                        }
                    } else {
                        drawImage(dungeon, dungeonX + 30, dungeonY + 45, 180, 180);
                        drawCenteredText(getDungeonName(i), dungeonX + 120, dungeonY + 30, CustomColor.fromHexString("FFFFFF"), 3f);

                        drawImage(dungeonKeyTexture, dungeonX + 60, dungeonY + 230, 60, 60);
                        drawText(formatter.format(comps), dungeonX + 55, dungeonY + 250, CustomColor.fromHexString("FFFFFF"), HorizontalAlignment.RIGHT, VerticalAlignment.TOP, 3f);

                        if(i < 8) {
                            drawImage(corruptedDungeonKeyTexture, dungeonX + 120, dungeonY + 230, 60, 60);
                            drawText(formatter.format(cComps), dungeonX + 190, dungeonY + 250, CustomColor.fromHexString("FFFFFF"));
                        }
                    }
                    i++;

                    long TotalComps = dungeons.getTotal();
                    String characterNameString;
                    if(selectedCharacter != null && selectedCharacter.getRaids() != null) {
                        characterNameString = " on " + getClassName(selectedCharacter) + ": ";
                    } else {
                        characterNameString = ": ";
                    }

                    drawCenteredText("Total Completions" + characterNameString + formatter.format(TotalComps), 900, 45, CustomColor.fromHexString("FFFFFF"), 3.9f);
                }
            }
            case Quests -> {
                if(selectedCharacter == null) {
                    drawCenteredText("Select a character to view quests.", 900, 345, CustomColor.fromHexString("FF0000"), 5f);
                    break;
                }
                List<String> quests = selectedCharacter.getQuests();
                if(quests == null) {
                    drawCenteredText("This player has their quest stats private.", 900, 345, CustomColor.fromHexString("FF0000"), 5f);
                    break;
                }

                String titleString;
                CustomColor textColor;
                if(quests.size() == 262) {
                    textColor = CommonColors.RAINBOW;
                } else {
                    textColor = CustomColor.fromHexString("FFFFFF");
                }
                double value = (quests.size()/262f) * 100;
                double rounded = Math.floor(value * 10) / 10.0;
                titleString = "Completed Quests on " + getClassName(selectedCharacter) + ": " + quests.size() + "/262 (" + rounded + "%)";

                if(SimpleConfig.getInstance(WynnExtrasConfig.class).darkmodeToggle) {
                    drawImage(questBackgroundTextureDark, 30, 90, 1740, 600);
                    drawImage(questSearchbarTextureDark, 600F, (float) (height * scaleFactor), 1200, 60);
                } else {
                    RenderUtils.drawTexturedRect(context.getMatrices(), questBackgroundTexture, (float) (xStart + (float) (10 * 3) / scaleFactor), (float) (yStart + (float) (30 * 3) / scaleFactor), (float) ((float) (580 * 3) / scaleFactor), (float) ((float) (200 * 3) / scaleFactor), (int) (580 * 3 / scaleFactor), (int) (200 * 3 / scaleFactor));
                    drawImage(questBackgroundTexture, 30, 90, 1740, 600);
                    drawImage(questSearchbarTexture, 600F, (float) (height * scaleFactor), 1200, 60);
                }

                questSearchBar.setX((int) (xStart + 200 * 3 / scaleFactor));
                questSearchBar.setY((int) (yStart + height + 7 * 3 / scaleFactor));
                questSearchBar.setWidth((int) (400 * 3 / scaleFactor));
                questSearchBar.setHeight((int) (14 * 3 / scaleFactor));
                questSearchBar.drawWithoutBackgroundButWithSearchtext(context, CustomColor.fromHexString("FFFFFF"));

                int i = 0;
                List<String> allQuestsCopy = new ArrayList<>(List.copyOf(allQuests));
                allQuestsCopy.sort(Comparator.comparing(String::toLowerCase));
                quests.sort(Comparator.comparing(String::toLowerCase));
                for(String quest : quests) {
                    if(!questSearchBar.getInput().isEmpty()) {
                        if(!quest.toLowerCase().contains(questSearchBar.getInput().toLowerCase())) {
                            continue;
                        }
                    }
                    int yPos = 114 + Math.floorDiv(i, 2) * 36- scrollOffset;
                    if(yPos > 690) break;
                    if(yPos > 60) {
                        HorizontalAlignment horizontalAlignment = HorizontalAlignment.LEFT;
                        int rightOffset = 0;
                        if (i % 2 == 1) {
                            rightOffset = 1680;
                            horizontalAlignment = HorizontalAlignment.RIGHT;
                        }

                        drawText(quest, (float) 60 + rightOffset, (float) yPos, textColor, horizontalAlignment, VerticalAlignment.TOP, 3f);
                    }
                    i++;
                }

                {
                    int yPos = 60 + 114 + Math.floorDiv(i, 2) * 36 - scrollOffset;
                    if (yPos > 60 && yPos < 690) {
                        drawText("Missing:", (float) 60, (float) yPos, textColor, 6f);
                        if(i % 2 == 0) i += 6;
                        else i++;
                    }
                }
                allQuestsCopy.removeAll(quests);
                for(String quest : allQuestsCopy) {
                    if(!questSearchBar.getInput().isEmpty()) {
                        if(!quest.toLowerCase().contains(questSearchBar.getInput().toLowerCase())) {
                            continue;
                        }
                    }
                    int yPos = 204 + Math.floorDiv(i, 2) * 36 - scrollOffset;
                    if(yPos > 690) break;
                    if(yPos > 60) {
                        HorizontalAlignment horizontalAlignment = HorizontalAlignment.LEFT;
                        int rightOffset = 0;
                        if (i % 2 == 1) {
                            rightOffset = 1680;
                            horizontalAlignment = HorizontalAlignment.RIGHT;
                        }

                        drawText(quest, (float) 60 + rightOffset, (float) yPos, textColor, horizontalAlignment, VerticalAlignment.TOP, 3f);
                    }
                    i++;
                }

                if(SimpleConfig.getInstance(WynnExtrasConfig.class).darkmodeToggle) {
                    drawImage(questBackgroundBorderTextureDark, 30, 60, 1740, 660);
                } else {
                    drawImage(questBackgroundBorderTexture, 30, 60, 1740, 660);
                }
                drawCenteredText(titleString, 900, 50, textColor, 4.5f);
            }
            case Misc -> {
                Global data = PV.currentPlayerData.getGlobalData();
                if(data == null) {
                    drawCenteredText("This player has their misc stats private.", 900, 345, CustomColor.fromHexString("FF0000"), 5f);
                    break;
                }

                if(SimpleConfig.getInstance(WynnExtrasConfig.class).darkmodeToggle) {
                    drawImage(miscBackgroundTextureDark, 30, 30, 1740, 690);
                } else {
                    drawImage(miscBackgroundTexture, 30, 30, 1740, 690);
                }
                drawText("Wars completed: " + data.getWars(), 60, 60, CustomColor.fromHexString("FFFFFF"),6f);
                drawText("Dungeons completed: " + data.getDungeons().getTotal(), 60, 120, CustomColor.fromHexString("FFFFFF"),6f);
                drawText("Unique Caves completed: " + data.getCaves(), 60, 180, CustomColor.fromHexString("FFFFFF"),6f);
                drawText("Unique Lootrun camps completed: " + data.getLootruns(), 60, 240, CustomColor.fromHexString("FFFFFF"),6f);
                drawText("Unique World events completed: " + data.getWorldEvents(), 60, 300, CustomColor.fromHexString("FFFFFF"),6f);
                drawText("Chests opened: " + data.getChestsFound(), 60, 360, CustomColor.fromHexString("FFFFFF"),6f);
                drawText("Mobs killed: " + data.getMobsKilled(), 60, 420, CustomColor.fromHexString("FFFFFF"),6f);
                drawText("Pvp kills: " + data.getPvp().getKills(), 60, 480, CustomColor.fromHexString("FFFFFF"),6f);
                drawText("Pvp deaths: " + data.getPvp().getDeaths(), 60, 540, CustomColor.fromHexString("FFFFFF"),6f);
            }
        }

        if (openInBrowserButton != null) {
            openInBrowserButton.setX(xStart);
            openInBrowserButton.setY(yStart + height);
            openInBrowserButton.buttonText = "Open in browser";
            if(SimpleConfig.getInstance(WynnExtrasConfig.class).darkmodeToggle) {
                openInBrowserButton.drawWithTexture(context, openInBrowserButtonTextureDark);
            } else {
                openInBrowserButton.drawWithTexture(context, openInBrowserButtonTexture);
            }
        }

        if(SimpleConfig.getInstance(WynnExtrasConfig.class).darkmodeToggle) {
            drawImage(openInBrowserButtonTextureWDark, 267, (float) (height * ui.getScaleFactor()), 300, 60);
        } else {
            drawImage(openInBrowserButtonTextureW, 267, (float) (height * ui.getScaleFactor()), 300, 60);
        }

        if (searchBar != null) {
            searchBar.setX((int) (0));
            searchBar.setY((int) (0));
            searchBar.setWidth(300);
            searchBar.setHeight(60);
//            searchBar.drawWithoutBackground(context, CustomColor.fromHexString("FFFFFF"));
            searchBar.draw(context);
        }

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
//        if(scaleFactor == 0) return;
//        int screenWidth = MinecraftClient.getInstance().getWindow().getScaledWidth();
//        int screenHeight = MinecraftClient.getInstance().getWindow().getScaledHeight();
//        int width = 600 * 3 / scaleFactor;
//        int height = 250 * 3 / scaleFactor;
//        int xStart = screenWidth / 2 - width / 2;
//        int yStart = screenHeight / 2 - height / 2;
//        int dragX = xStart + 23 * 3 / scaleFactor;
//        int dragY = yStart + 33 * 3 / scaleFactor;
//        int dragWidth = 143 * 3 / scaleFactor;
//        int dragHeight = 143 * 3 / scaleFactor;
//
//        if (mouseX >= dragX && mouseX <= dragX + dragWidth &&
//                mouseY >= dragY && mouseY <= dragY + dragHeight) {
//            draggingAllowed = true;
//        } else {
//            draggingAllowed = false;
//        }
//
        if(openInBrowserButton == null || searchBar == null || (currentTab == Tab.Quests && questSearchBar == null)) return;
        if(openInBrowserButton.isClickInBounds(PVScreen.mouseX, PVScreen.mouseY)) {
            openInBrowserButton.click();
        }
        if(searchBar != null) {
            if (searchBar.isClickInBounds(PVScreen.mouseX, PVScreen.mouseY)) {
                searchBar.click();
            } else {
                searchBar.setActive(false);
            }
        }
        if(questSearchBar != null) {
            if (questSearchBar.isClickInBounds(PVScreen.mouseX, PVScreen.mouseY)) {
                questSearchBar.click();
            } else {
                questSearchBar.setActive(false);
            }
        }
//        for(EasyButton button : tabButtons) {
//            if(button.isClickInBounds(PVScreen.mouseX, PVScreen.mouseY)) {
//                button.click();
//            }
//        }
//        if(currentTab == Tab.General) {
//            for (CharacterButton button : characterButtons) {
//                if (button.isClickInBounds(PVScreen.mouseX, PVScreen.mouseY)) {
//                    button.click();
//                }
//            }
//        }
    }

    public int drawDynamicNameSign(DrawContext context, String input, int x, int y) {
        TextRenderer textRenderer = MinecraftClient.getInstance().textRenderer;
        int strWidth = textRenderer.getWidth(input) + 10;
        int strMidWidth = strWidth - 15;
        int amount = Math.max(0, Math.ceilDiv(strMidWidth, 10));
        if(SimpleConfig.getInstance(WynnExtrasConfig.class).darkmodeToggle) {
            ui.drawImage(tabLeftDark, x, y, 30, 60);
        } else {
            ui.drawImage(tabLeft, x, y, 30, 60);
        }
        for (int i = 0; i < amount; i++) {
            if(SimpleConfig.getInstance(WynnExtrasConfig.class).darkmodeToggle) {
                ui.drawImage(tabMidDark, x + 30 * (i + 1), y, 30, 60);
            } else {
                ui.drawImage(tabMid, x + 30 * (i + 1), y, 30, 60);
            }
        }
        if(SimpleConfig.getInstance(WynnExtrasConfig.class).darkmodeToggle) {
            ui.drawImage(tagRightDark, x + 30 * (amount + 1), y, 30, 60);
        } else {
            ui.drawImage(tagRight, x + 30 * (amount + 1), y, 30, 60);
        }
        return 60 + amount * 30;
    }

    private static TabWidget getTabWidget(Tab tab) {
        return switch (tab) {
            case General -> new GeneralTabWidget();
            case Raids -> new RaidsTabWidget();
            case Rankings -> new RankingsTabWidget();
            case Professions -> new ProfessionsTabWidget();
            case Dungeons -> new DungeonsTabWidget();
            case Quests -> new QuestsTabWidget();
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
                if(SimpleConfig.getInstance(WynnExtrasConfig.class).darkmodeToggle) ui.drawImage(backgroundTextureDark, x, y, width, height);
                else ui.drawImage(backgroundTexture, x, y, width, height);
            } else {
                if(SimpleConfig.getInstance(WynnExtrasConfig.class).darkmodeToggle) ui.drawImage(alsobackgroundTextureDark, x, y, width, height);
                else ui.drawImage(alsobackgroundTexture, x, y, width,height);
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

        public TabButtonWidget(int index, Tab tab, WEScreen parent) {
            super(0, 0, 0, 0);
            this.index = index;
            this.tab = tab;
            this.action = () -> {
                if(PV.currentPlayerData == null) return;
                System.out.println("CLICKED " + tab.toString());
                if(tab == currentTab) return;
                currentTab = tab;
                TabWidget tabWidget = getTabWidget(tab);
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
            } else if (selectedCharacter == null && (tab.equals(Tab.Professions) || tab.equals(Tab.Quests))) {
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
        public boolean mouseDragged(double mouseX, double mouseY, int button, double deltaX, double deltaY) {
            return false;
        }

        @Override
        public boolean mouseReleased(double mx, double my, int button) {
            return super.mouseReleased(mx, my, button);
        }
    }
}
