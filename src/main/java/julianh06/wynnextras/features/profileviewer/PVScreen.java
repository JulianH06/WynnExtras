package julianh06.wynnextras.features.profileviewer;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.authlib.GameProfile;
import com.mojang.authlib.properties.Property;
import com.wynntils.core.text.StyledText;
import com.wynntils.utils.colors.CommonColors;
import com.wynntils.utils.colors.CustomColor;
import com.wynntils.utils.render.FontRenderer;
import com.wynntils.utils.render.RenderUtils;
import com.wynntils.utils.render.type.HorizontalAlignment;
import com.wynntils.utils.render.type.TextShadow;
import com.wynntils.utils.render.type.VerticalAlignment;
import julianh06.wynnextras.config.WynnExtrasConfig;
import julianh06.wynnextras.config.simpleconfig.SimpleConfig;
import julianh06.wynnextras.features.profileviewer.data.*;
import julianh06.wynnextras.utils.overlays.EasyButton;
import julianh06.wynnextras.utils.render.WEScreen;
import net.fabricmc.fabric.api.client.screen.v1.ScreenMouseEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.ingame.InventoryScreen;
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

import static julianh06.wynnextras.features.waypoints.WaypointScreen.scaleFactor;

public class PVScreen extends WEScreen {
    static int mouseX = 0;
    static int mouseY = 0;

    public enum Rank {NONE, VIP, VIPPLUS, HERO, HEROPLUS, CHAMPION}

    public enum Tab {General, Raids, Rankings, Professions, Dungeons, Quests, Misc}
    public static List<TabButton> tabButtons = new ArrayList<>();
    public static List<CharacterButton> characterButtons = new ArrayList<>();

    static List<String> WETeam = List.of("JulianH06", "Teslanator", "Mikecraft1224", "LegendaryVirus", "elwood24", "BaltrazYT");

    List<String> allQuests = Arrays.asList("???", "A Grave Mistake", "A Hunter's Calling", "A Journey Beyond", "A Journey Further", "A Marauder's Dues", "A Sandy Scandal", "Acquiring Credentials", "Aldorei's Secret Part I", "Aldorei's Secret Part II", "All Roads To Peace", "An Iron Heart Part I", "An Iron Heart Part II", "Arachnids' Ascent", "Beneath the Depths", "Beyond the Grave", "Blazing Retribution", "Bob's Lost Soul", "Canyon Condor", "Clearing the Camps", "Cluck Cluck", "Cook Assistant", "Corrupted Betrayal", "Cowfusion", "Creeper Infiltration", "Crop Failure", "Death Whistle", "Deja Vu", "Desperate Metal", "Dwarves and Doguns Part I", "Dwarves and Doguns Part II", "Dwarves and Doguns Part III", "Dwarves and Doguns Part IV", "Dwelling Walls", "Elemental Exercise", "Enter the Dojo", "Enzan's Brother", "Fallen Delivery", "Fantastic Voyage", "Fate of the Fallen", "Flight in Distress", "Forbidden Prison", "From the Bottom", "From the Mountains", "Frost Bite", "General's Orders", "Grand Youth", "Grave Digger", "Green Gloop", "Haven Antiquity", "Heart of Llevigar", "Hollow Serenity", "Hunger of the Gerts Part I", "Hunger of the Gerts Part II", "Ice Nations", "Infested Plants", "Jungle Fever", "King's Recruit", "Kingdom of Sand", "Lava Springs", "Lazarus Pit", "Lexdale Witch Trials", "Lost in the Jungle", "Lost Royalty", "Lost Soles", "Lost Tower", "Maltic's Well", "Master Piece", "Meaningful Holiday", "Memory Paranoia", "Mini-Quest - Gather Acacia Logs", "Mini-Quest - Gather Acacia Logs II", "Mini-Quest - Gather Avo Logs", "Mini-Quest - Gather Avo Logs II", "Mini-Quest - Gather Avo Logs III", "Mini-Quest - Gather Avo Logs IV", "Mini-Quest - Gather Bamboo", "Mini-Quest - Gather Barley", "Mini-Quest - Gather Bass", "Mini-Quest - Gather Bass II", "Mini-Quest - Gather Bass III", "Mini-Quest - Gather Bass IV", "Mini-Quest - Gather Birch Logs", "Mini-Quest - Gather Carp", "Mini-Quest - Gather Carp II", "Mini-Quest - Gather Cobalt", "Mini-Quest - Gather Cobalt II", "Mini-Quest - Gather Cobalt III", "Mini-Quest - Gather Copper", "Mini-Quest - Gather Dark Logs", "Mini-Quest - Gather Dark Logs II", "Mini-Quest - Gather Dark Logs III", "Mini-Quest - Gather Decay Roots", "Mini-Quest - Gather Decay Roots II", "Mini-Quest - Gather Decay Roots III", "Mini-Quest - Gather Diamonds", "Mini-Quest - Gather Diamonds II", "Mini-Quest - Gather Diamonds III", "Mini-Quest - Gather Diamonds IV", "Mini-Quest - Gather Gold", "Mini-Quest - Gather Gold II", "Mini-Quest - Gather Granite", "Mini-Quest - Gather Gudgeon", "Mini-Quest - Gather Gylia Fish", "Mini-Quest - Gather Gylia Fish II", "Mini-Quest - Gather Gylia Fish III", "Mini-Quest - Gather Hops", "Mini-Quest - Gather Hops II", "Mini-Quest - Gather Icefish", "Mini-Quest - Gather Icefish II", "Mini-Quest - Gather Iron", "Mini-Quest - Gather Iron II", "Mini-Quest - Gather Jungle Logs", "Mini-Quest - Gather Jungle Logs II", "Mini-Quest - Gather Kanderstone", "Mini-Quest - Gather Kanderstone II", "Mini-Quest - Gather Kanderstone III", "Mini-Quest - Gather Koi", "Mini-Quest - Gather Koi II", "Mini-Quest - Gather Koi III", "Mini-Quest - Gather Light Logs", "Mini-Quest - Gather Light Logs II", "Mini-Quest - Gather Light Logs III", "Mini-Quest - Gather Malt", "Mini-Quest - Gather Malt II", "Mini-Quest - Gather Millet", "Mini-Quest - Gather Millet II", "Mini-Quest - Gather Millet III", "Mini-Quest - Gather Molten Eel", "Mini-Quest - Gather Molten Eel II", "Mini-Quest - Gather Molten Eel III", "Mini-Quest - Gather Molten Eel IV", "Mini-Quest - Gather Molten Ore", "Mini-Quest - Gather Molten Ore II", "Mini-Quest - Gather Molten Ore III", "Mini-Quest - Gather Molten Ore IV", "Mini-Quest - Gather Oak Logs", "Mini-Quest - Gather Oats", "Mini-Quest - Gather Oats II", "Mini-Quest - Gather Pine Logs", "Mini-Quest - Gather Pine Logs II", "Mini-Quest - Gather Pine Logs III", "Mini-Quest - Gather Piranhas", "Mini-Quest - Gather Piranhas II", "Mini-Quest - Gather Rice", "Mini-Quest - Gather Rice II", "Mini-Quest - Gather Rice III", "Mini-Quest - Gather Rice IV", "Mini-Quest - Gather Rye", "Mini-Quest - Gather Rye II", "Mini-Quest - Gather Salmon", "Mini-Quest - Gather Salmon II", "Mini-Quest - Gather Sandstone", "Mini-Quest - Gather Sandstone II", "Mini-Quest - Gather Silver", "Mini-Quest - Gather Silver II", "Mini-Quest - Gather Sorghum", "Mini-Quest - Gather Sorghum II", "Mini-Quest - Gather Sorghum III", "Mini-Quest - Gather Sorghum IV", "Mini-Quest - Gather Spruce Logs", "Mini-Quest - Gather Spruce Logs II", "Mini-Quest - Gather Trout", "Mini-Quest - Gather Wheat", "Mini-Quest - Gather Willow Logs", "Mini-Quest - Gather Willow Logs II", "Mini-Quest - Slay Ailuropodas", "Mini-Quest - Slay Angels", "Mini-Quest - Slay Astrochelys Manis", "Mini-Quest - Slay Azers", "Mini-Quest - Slay Conures", "Mini-Quest - Slay Coyotes", "Mini-Quest - Slay Creatures of Nesaak Forest", "Mini-Quest - Slay Creatures of the Void", "Mini-Quest - Slay Dead Villagers", "Mini-Quest - Slay Dragonlings", "Mini-Quest - Slay Felrocs", "Mini-Quest - Slay Frosted Guards & Cryostone Golems", "Mini-Quest - Slay Hobgoblins", "Mini-Quest - Slay Idols", "Mini-Quest - Slay Ifrits", "Mini-Quest - Slay Jinkos", "Mini-Quest - Slay Lizardmen", "Mini-Quest - Slay Magma Entities", "Mini-Quest - Slay Mooshrooms", "Mini-Quest - Slay Myconids", "Mini-Quest - Slay Orcs", "Mini-Quest - Slay Pernix Monkeys", "Mini-Quest - Slay Robots", "Mini-Quest - Slay Scarabs", "Mini-Quest - Slay Skeletons", "Mini-Quest - Slay Slimes", "Mini-Quest - Slay Spiders", "Mini-Quest - Slay Weirds", "Mini-Quest - Slay Wraiths & Phantasms", "Misadventure on the Sea", "Mixed Feelings", "Murder Mystery", "Mushroom Man", "One Thousand Meters Under", "Out of my Mind", "Pirate's Trove", "Pit of the Dead", "Point of No Return", "Poisoning the Pest", "Potion Making", "Purple and Blue", "Realm of Light I - The Worm Holes", "Realm of Light II - Taproot", "Realm of Light III - A Headless History", "Realm of Light IV - Finding the Light", "Realm of Light V - The Realm of Light", "Recipe For Disaster", "Reclaiming the House", "Recover the Past", "Redbeard's Booty", "Reincarnation", "Rise of the Quartron", "Royal Trials", "Shattered Minds", "Stable Story", "Star Thief", "Supply and Delivery", "Taking the Tower", "Temple of the Legends", "Tempo Town Trouble", "The Bigger Picture", "The Breaking Point", "The Canary Calls", "The Canyon Guides", "The Corrupted Village", "The Dark Descent", "The Envoy Part I", "The Envoy Part II", "The Feathers Fly Part I", "The Feathers Fly Part II", "The Hero of Gavel", "The Hidden City", "The House of Twain", "The Lost", "The Maiden Tower", "The Mercenary", "The Olmic Rune", "The Order of the Grook", "The Passage", "The Qira Hive", "The Sewers of Ragni", "The Shadow of the Beast", "The Thanos Depository", "The Ultimate Weapon", "Tower of Ascension", "Tribal Aggression", "Troubled Tribesmen", "Tunnel Trouble", "UndericeÀ", "Underwater", "Wrath of the Mummy", "WynnExcavation Site A", "WynnExcavation Site B", "WynnExcavation Site C", "WynnExcavation Site D", "Zhight Island");

    Identifier tabLeft = Identifier.of("wynnextras", "textures/gui/profileviewer/tableft.png");
    Identifier tabMid = Identifier.of("wynnextras", "textures/gui/profileviewer/tabmid.png");
    Identifier tagRight = Identifier.of("wynnextras", "textures/gui/profileviewer/tabright.png");

    Identifier tabLeftDark = Identifier.of("wynnextras", "textures/gui/profileviewer/tableft_dark.png");
    Identifier tabMidDark = Identifier.of("wynnextras", "textures/gui/profileviewer/tabmid_dark.png");
    Identifier tagRightDark = Identifier.of("wynnextras", "textures/gui/profileviewer/tabright_dark.png");

    Identifier NOTGTexture = Identifier.of("wynnextras", "textures/gui/profileviewer/rankingicons/notg.png");
    Identifier NOLTexture = Identifier.of("wynnextras", "textures/gui/profileviewer/rankingicons/nol.png");
    Identifier TCCTexture = Identifier.of("wynnextras", "textures/gui/profileviewer/rankingicons/tcc.png");
    Identifier TNATexture = Identifier.of("wynnextras", "textures/gui/profileviewer/rankingicons/tna.png");

    Identifier backgroundTexture = Identifier.of("wynnextras", "textures/gui/profileviewer/profileviewerbackground.png");
    Identifier alsobackgroundTexture = Identifier.of("wynnextras", "textures/gui/profileviewer/alsoprofileviewerbackground.png");
    Identifier raidBackgroundTexture = Identifier.of("wynnextras", "textures/gui/profileviewer/raidbackground.png");
    Identifier openInBrowserButtonTexture = Identifier.of("wynnextras", "textures/gui/profileviewer/openinbrowserbuttontexture.png");
    Identifier openInBrowserButtonTextureW = Identifier.of("wynnextras", "textures/gui/profileviewer/openinbrowserbuttontexturewide.png");
    Identifier classBackgroundTexture = Identifier.of("wynnextras", "textures/gui/profileviewer/classbackgroundinactive.png");
    Identifier classBackgroundTextureGold = Identifier.of("wynnextras", "textures/gui/profileviewer/classbackgroundinactivegold.png");
    Identifier classBackgroundTextureActive = Identifier.of("wynnextras", "textures/gui/profileviewer/classbackgroundactive.png");
    Identifier onlineCircleTexture = Identifier.of("wynnextras", "textures/gui/profileviewer/onlinecircle.png");
    Identifier offlineCircleTexture = Identifier.of("wynnextras", "textures/gui/profileviewer/offlinecircle.png");

    Identifier backgroundTextureDark = Identifier.of("wynnextras", "textures/gui/profileviewer/profileviewerbackground_dark.png");
    Identifier alsobackgroundTextureDark = Identifier.of("wynnextras", "textures/gui/profileviewer/alsoprofileviewerbackground_dark.png");
    Identifier raidBackgroundTextureDark = Identifier.of("wynnextras", "textures/gui/profileviewer/raidbackground_dark.png");
    Identifier openInBrowserButtonTextureDark = Identifier.of("wynnextras", "textures/gui/profileviewer/openinbrowserbuttontexture_dark.png");
    Identifier openInBrowserButtonTextureWDark = Identifier.of("wynnextras", "textures/gui/profileviewer/openinbrowserbuttontexturewide_dark.png");
    Identifier classBackgroundTextureDark = Identifier.of("wynnextras", "textures/gui/profileviewer/classbackgroundinactive_dark.png");
    Identifier classBackgroundTextureGoldDark = Identifier.of("wynnextras", "textures/gui/profileviewer/classbackgroundinactivegold_dark.png");
    Identifier classBackgroundTextureActiveDark = Identifier.of("wynnextras", "textures/gui/profileviewer/classbackgroundactive_dark.png");
    Identifier onlineCircleTextureDark = Identifier.of("wynnextras", "textures/gui/profileviewer/onlinecircle_dark.png");
    Identifier offlineCircleTextureDark = Identifier.of("wynnextras", "textures/gui/profileviewer/offlinecircle_dark.png");


    Identifier vip = Identifier.of("wynnextras", "textures/gui/profileviewer/ranks/vip.png");
    Identifier vipplus = Identifier.of("wynnextras", "textures/gui/profileviewer/ranks/vipplus.png");
    Identifier hero = Identifier.of("wynnextras", "textures/gui/profileviewer/ranks/hero.png");
    Identifier heroplus = Identifier.of("wynnextras", "textures/gui/profileviewer/ranks/heroplus.png");
    Identifier champion = Identifier.of("wynnextras", "textures/gui/profileviewer/ranks/champion.png");
    Identifier warriorTexture = Identifier.of("wynnextras", "textures/gui/profileviewer/classes/warrior.png");
    Identifier warriorGoldTexture = Identifier.of("wynnextras", "textures/gui/profileviewer/classes/warriorgold.png");
    Identifier shamanTexture = Identifier.of("wynnextras", "textures/gui/profileviewer/classes/shaman.png");
    Identifier shamanGoldTexture = Identifier.of("wynnextras", "textures/gui/profileviewer/classes/shamangold.png");
    Identifier mageTexture = Identifier.of("wynnextras", "textures/gui/profileviewer/classes/mage.png");
    Identifier mageGoldTexture = Identifier.of("wynnextras", "textures/gui/profileviewer/classes/magegold.png");
    Identifier assassinTexture = Identifier.of("wynnextras", "textures/gui/profileviewer/classes/assassin.png");
    Identifier assassinGoldTexture = Identifier.of("wynnextras", "textures/gui/profileviewer/classes/assassingold.png");
    Identifier archerTexture = Identifier.of("wynnextras", "textures/gui/profileviewer/classes/archer.png");
    Identifier archerGoldTexture = Identifier.of("wynnextras", "textures/gui/profileviewer/classes/archergold.png");


    Identifier decrepitSewersTexture = Identifier.of("wynnextras", "textures/gui/profileviewer/dungeons/decrepitsewers.png");
    Identifier infestedPitTexture = Identifier.of("wynnextras", "textures/gui/profileviewer/dungeons/infestedpit.png");
    Identifier underworldCryptTexture = Identifier.of("wynnextras", "textures/gui/profileviewer/dungeons/underworldcrypt.png");
    Identifier timelostSanctumTexture = Identifier.of("wynnextras", "textures/gui/profileviewer/dungeons/timelostsanctum.png");
    Identifier sandSweptTombTexture = Identifier.of("wynnextras", "textures/gui/profileviewer/dungeons/sandswepttomb.png");
    Identifier iceBarrowsTexture = Identifier.of("wynnextras", "textures/gui/profileviewer/dungeons/icebarrows.png");
    Identifier undergrowthRuinsTexture = Identifier.of("wynnextras", "textures/gui/profileviewer/dungeons/undergrowthruins.png");
    Identifier galleonsGraveyardTexture = Identifier.of("wynnextras", "textures/gui/profileviewer/dungeons/galleonsgraveyard.png");
    Identifier fallenFactoryTexture = Identifier.of("wynnextras", "textures/gui/profileviewer/dungeons/fallenfactory.png");
    Identifier eldritchOutlookTexture = Identifier.of("wynnextras", "textures/gui/profileviewer/dungeons/eldritchoutlook.png");

    List<Identifier> dungeonTextures = List.of(decrepitSewersTexture, infestedPitTexture, underworldCryptTexture, timelostSanctumTexture, sandSweptTombTexture, iceBarrowsTexture, undergrowthRuinsTexture, galleonsGraveyardTexture, fallenFactoryTexture, eldritchOutlookTexture);

    Identifier dungeonKeyTexture = Identifier.of("wynnextras", "textures/gui/profileviewer/dungeons/dungeonkey.png");
    Identifier corruptedDungeonKeyTexture = Identifier.of("wynnextras", "textures/gui/profileviewer/dungeons/corrupteddungeonkey.png");
    Identifier dungeonBackgroundTexture = Identifier.of("wynnextras", "textures/gui/profileviewer/dungeons/dungeonpagebackground.png");
    Identifier miscBackgroundTexture = Identifier.of("wynnextras", "textures/gui/profileviewer/miscpagebackground.png");

    Identifier dungeonBackgroundTextureDark = Identifier.of("wynnextras", "textures/gui/profileviewer/dungeons/dungeonpagebackground_dark.png");
    Identifier miscBackgroundTextureDark = Identifier.of("wynnextras", "textures/gui/profileviewer/miscpagebackground_dark.png");

    Identifier miningTexture = Identifier.of("wynnextras", "textures/gui/profileviewer/rankingicons/mining.png");
    Identifier woodcuttingTexture = Identifier.of("wynnextras", "textures/gui/profileviewer/rankingicons/woodcutting.png");
    Identifier farmingTexture = Identifier.of("wynnextras", "textures/gui/profileviewer/rankingicons/farming.png");
    Identifier fishingTexture = Identifier.of("wynnextras", "textures/gui/profileviewer/rankingicons/fishing.png");
    Identifier armouringTexture = Identifier.of("wynnextras", "textures/gui/profileviewer/rankingicons/armouring.png");
    Identifier tailoringTexture = Identifier.of("wynnextras", "textures/gui/profileviewer/rankingicons/tailoring.png");
    Identifier weaponsmithingTexture = Identifier.of("wynnextras", "textures/gui/profileviewer/rankingicons/weaponsmithing.png");
    Identifier woodworkingTexture = Identifier.of("wynnextras", "textures/gui/profileviewer/rankingicons/woodworking.png");
    Identifier jewelingTexture = Identifier.of("wynnextras", "textures/gui/profileviewer/rankingicons/jeweling.png");
    Identifier alchemismTexture = Identifier.of("wynnextras", "textures/gui/profileviewer/rankingicons/alchemism.png");
    Identifier scribingTexture = Identifier.of("wynnextras", "textures/gui/profileviewer/rankingicons/scribing.png");
    Identifier cookingTexture = Identifier.of("wynnextras", "textures/gui/profileviewer/rankingicons/cooking.png");
    Identifier profBackgroundTexture = Identifier.of("wynnextras", "textures/gui/profileviewer/profs/profbackground.png");

    Identifier profBackgroundTextureDark = Identifier.of("wynnextras", "textures/gui/profileviewer/profs/profbackground_dark.png");

    Identifier questBackgroundTexture = Identifier.of("wynnextras", "textures/gui/profileviewer/quests/questbackground.png");
    Identifier questBackgroundBorderTexture = Identifier.of("wynnextras", "textures/gui/profileviewer/quests/questbackgroundborders.png");
    Identifier questSearchbarTexture = Identifier.of("wynnextras", "textures/gui/profileviewer/quests/questsearchbar.png");

    Identifier questBackgroundTextureDark = Identifier.of("wynnextras", "textures/gui/profileviewer/quests/questbackground_dark.png");
    Identifier questBackgroundBorderTextureDark = Identifier.of("wynnextras", "textures/gui/profileviewer/quests/questbackgroundborders_dark.png");
    Identifier questSearchbarTextureDark = Identifier.of("wynnextras", "textures/gui/profileviewer/quests/questsearchbar_dark.png");

    Identifier warsCompletionTexture = Identifier.of("wynnextras", "textures/gui/profileviewer/rankingicons/warscompletion.png");
    Identifier playerContentTexture = Identifier.of("wynnextras", "textures/gui/profileviewer/rankingicons/playercontent.png");
    Identifier globalPlayerContent = Identifier.of("wynnextras", "textures/gui/profileviewer/rankingicons/globalplayercontent.png");
    Identifier combatLevelTexture = Identifier.of("wynnextras", "textures/gui/profileviewer/rankingicons/combatlevel.png");
    Identifier totalLevelTexture = Identifier.of("wynnextras", "textures/gui/profileviewer/rankingicons/totallevel.png");
    Identifier professionLevelTexture = Identifier.of("wynnextras", "textures/gui/profileviewer/rankingicons/professionlevel.png");
    Identifier rankingBackgroundTexture = Identifier.of("wynnextras", "textures/gui/profileviewer/rankingicons/rankingbackground.png");
    Identifier rankingBackgroundWideTexture = Identifier.of("wynnextras", "textures/gui/profileviewer/rankingicons/rankingbackgroundwide.png");

    Identifier rankingBackgroundTextureDark = Identifier.of("wynnextras", "textures/gui/profileviewer/rankingicons/rankingbackground_dark.png");
    Identifier rankingBackgroundWideTextureDark = Identifier.of("wynnextras", "textures/gui/profileviewer/rankingicons/rankingbackgroundwide_dark.png");

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

    static int scaleFactor;

    public PVScreen(String player) {
        super(Text.of("Player Viewer"));
        tabButtons.clear();
        characterButtons.clear();
        openInBrowserButton = null;
        searchBar = null;
        questSearchBar = null;
        selectedCharacter = null;
        for(Tab tab : Tab.values()) {
            tabButtons.add(new TabButton(0, 0, 0, 0, tab));
        }
        for (int i = 0; i < 15; i++) {
            characterButtons.add(new CharacterButton(-1, -1, 0, 0, null));
        }
        this.player = player;
        currentTab = Tab.General;

        CompletableFuture.runAsync(() -> {
            try {
                while (PV.currentPlayerData == null) {
                    Thread.sleep(50);
                }
                SkinData skin = fetchSkin(PV.currentPlayerData.getUuid());
                GameProfile profile = createProfileWithSkin(PV.currentPlayerData.getUuid(), player, skin);

                MinecraftClient client = MinecraftClient.getInstance();
                ClientWorld world = client.world;

                if (world != null) {
                    dummy = new AbstractClientPlayerEntity(world, profile) {
                        @Override
                        public SkinTextures getSkinTextures() {
                            return MinecraftClient.getInstance()
                                    .getSkinProvider()
                                    .getSkinTextures(this.getGameProfile());
                        }

                        @Override
                        public boolean isPartVisible(PlayerModelPart part) {
                            if(part == PlayerModelPart.CAPE) return false;
                            return true;
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
        ScreenMouseEvents.afterMouseScroll(this).register((
                screen,
                mX,
                mY,
                horizontalAmount,
                verticalAmount
        ) -> {
            long now = System.currentTimeMillis();
            if (now - lastScrollTime < scrollCooldown) {
                return;
            }
            lastScrollTime = now;

            if (verticalAmount > 0) {
                scrollOffset -= 30 / scaleFactor; //Scroll up
            } else {
                scrollOffset += 30 / scaleFactor; //Scroll down
            }
            if(scrollOffset < 0) {
                scrollOffset = 0;
            }
        });
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        if(MinecraftClient.getInstance().getWindow() == null) return;
        super.drawContext = context;
        super.scaleFactor = MinecraftClient.getInstance().getWindow().getScaleFactor();
        scaleFactor = (int) MinecraftClient.getInstance().getWindow().getScaleFactor();

        if(openInBrowserButton == null && PV.currentPlayerData != null) {
            openInBrowserButton = new OpenInBroserButton(-1, -1, 20 * 3 / scaleFactor, 87 * 3 / scaleFactor, "https://wynncraft.com/stats/player/" + PV.currentPlayerData.getUuid());
        }

        if(searchBar == null || searchBar.getInput().equals("Unknown user")) {
            searchBar = new Searchbar(-1, -1, 14 * 3 / scaleFactor, 100 * 3 / scaleFactor);
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
        width = 600 * 3 / scaleFactor;
        height = 250 * 3 / scaleFactor;
        xStart = screenWidth / 2 - width / 2;
        yStart = screenHeight / 2 - height / 2;

        PVScreen.mouseX = mouseX;
        PVScreen.mouseY = mouseY;

        if(questSearchBar == null && PV.currentPlayerData != null) {
            questSearchBar = new Searchbar( -1, -1, -1, -1);
            questSearchBar.setSearchText("Search...");
        }

        drawBackground();
        if(currentTab == Tab.General) {
            if(SimpleConfig.getInstance(WynnExtrasConfig.class).darkmodeToggle) drawImage(backgroundTextureDark, 0, 0, width * scaleFactor, height * scaleFactor);
            else drawImage(backgroundTexture, 0, 0, width * scaleFactor, height * scaleFactor);
        } else {
            if(SimpleConfig.getInstance(WynnExtrasConfig.class).darkmodeToggle) drawImage(alsobackgroundTextureDark, 0, 0, width * scaleFactor, height * scaleFactor);
            else drawImage(alsobackgroundTexture, 0, 0, width * scaleFactor, height * scaleFactor);
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
            tabButton.setX(xStart + 24 + totalXOffset);
            tabButton.setY(yStart - 57);
            tabButton.setWidth(signWidth);
            tabButton.setHeight(60);
            //tabButton.draw(context);

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
                        drawPlayer(context, xStart + 66 / scaleFactor + 216 / scaleFactor, yStart + 102 / scaleFactor + 387 / scaleFactor, 210 / scaleFactor, mouseX, mouseY, dummy); //166 178
                    } else if (InputUtil.isKeyPressed(MinecraftClient.getInstance().getWindow().getHandle(), InputUtil.GLFW_KEY_RIGHT_SHIFT)) {
                        dummy.setPose(EntityPose.SLEEPING);
                        drawPlayer(context, xStart + 66 / scaleFactor, yStart + 102 / scaleFactor + 357 / scaleFactor, 210 / scaleFactor, mouseX, mouseY, dummy); //166 178
                    } else {
                        dummy.setPose(EntityPose.STANDING);
                        drawPlayer(context, xStart + 66 / scaleFactor + 216 / scaleFactor, yStart + 102 / scaleFactor + 414 / scaleFactor, 210 / scaleFactor, mouseX, mouseY, dummy); //166 178
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
                        characterButtons.get(i).setX(xStart + 192 * 3 / scaleFactor + (137 * 3 / scaleFactor) * (i % 3));
                        characterButtons.get(i).setY(yStart + 5 * 3 / scaleFactor + (48 * 3 / scaleFactor) * Math.floorDiv(i, 3));
                        characterButtons.get(i).setWidth(390 / scaleFactor);
                        characterButtons.get(i).setHeight(132 / scaleFactor);
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
                                drawImage(classBackgroundTexture, entryX, entryY, 390, 132);
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
                    RenderUtils.drawTexturedRect(context.getMatrices(), raidBackgroundTextureDark, xStart + (float) (10 * 3) / scaleFactor, yStart + (float) (30 * 3) / scaleFactor, (float) (275 * 3) / scaleFactor, (float) (100 * 3) / scaleFactor, 275 * 3 / scaleFactor, 100 * 3 / scaleFactor);
                    RenderUtils.drawTexturedRect(context.getMatrices(), NOTGTexture, xStart + (float) (10 * 3) / scaleFactor, yStart + (float) (30 * 3) / scaleFactor, (float) (100 * 3) / scaleFactor, (float) (100 * 3) / scaleFactor, 100 * 3 / scaleFactor, 100 * 3 / scaleFactor);
                    RenderUtils.drawTexturedRect(context.getMatrices(), raidBackgroundTextureDark, xStart + (float) (315 * 3) / scaleFactor, yStart + (float) (30 * 3) / scaleFactor, (float) (275 * 3) / scaleFactor, (float) (100 * 3) / scaleFactor, 275 * 3 / scaleFactor, 100 * 3 / scaleFactor);
                    RenderUtils.drawTexturedRect(context.getMatrices(), TCCTexture, xStart + (float) (490 * 3) / scaleFactor, yStart + (float) (30 * 3) / scaleFactor, (float) (100 * 3) / scaleFactor, (float) (100 * 3) / scaleFactor, 100 * 3 / scaleFactor, 100 * 3 / scaleFactor);
                    RenderUtils.drawTexturedRect(context.getMatrices(), raidBackgroundTextureDark, xStart + (float) (10 * 3) / scaleFactor, yStart + (float) (140 * 3) / scaleFactor, (float) (275 * 3) / scaleFactor, (float) (100 * 3) / scaleFactor, 275 * 3 / scaleFactor, 100 * 3 / scaleFactor);
                    RenderUtils.drawTexturedRect(context.getMatrices(), NOLTexture, xStart + (float) (10 * 3) / scaleFactor, yStart + (float) (140 * 3) / scaleFactor, (float) (100 * 3) / scaleFactor, (float) (100 * 3) / scaleFactor, 100 * 3 / scaleFactor, 100 * 3 / scaleFactor);
                    RenderUtils.drawTexturedRect(context.getMatrices(), raidBackgroundTextureDark, xStart + (float) (315 * 3) / scaleFactor, yStart + (float) (140 * 3) / scaleFactor, (float) (275 * 3) / scaleFactor, (float) (100 * 3) / scaleFactor, 275 * 3 / scaleFactor, 100 * 3 / scaleFactor);
                    RenderUtils.drawTexturedRect(context.getMatrices(), TNATexture, xStart + (float) (490 * 3) / scaleFactor, yStart + (float) (140 * 3) / scaleFactor, (float) (100 * 3) / scaleFactor, (float) (100 * 3) / scaleFactor, 100 * 3 / scaleFactor, 100 * 3 / scaleFactor);
                } else {
                    RenderUtils.drawTexturedRect(context.getMatrices(), raidBackgroundTexture, xStart + (float) (10 * 3) / scaleFactor, yStart + (float) (30 * 3) / scaleFactor, (float) (275 * 3) / scaleFactor, (float) (100 * 3) / scaleFactor, 275 * 3 / scaleFactor, 100 * 3 / scaleFactor);
                    RenderUtils.drawTexturedRect(context.getMatrices(), NOTGTexture, xStart + (float) (10 * 3) / scaleFactor, yStart + (float) (30 * 3) / scaleFactor, (float) (100 * 3) / scaleFactor, (float) (100 * 3) / scaleFactor, 100 * 3 / scaleFactor, 100 * 3 / scaleFactor);
                    RenderUtils.drawTexturedRect(context.getMatrices(), raidBackgroundTexture, xStart + (float) (315 * 3) / scaleFactor, yStart + (float) (30 * 3) / scaleFactor, (float) (275 * 3) / scaleFactor, (float) (100 * 3) / scaleFactor, 275 * 3 / scaleFactor, 100 * 3 / scaleFactor);
                    RenderUtils.drawTexturedRect(context.getMatrices(), TCCTexture, xStart + (float) (490 * 3) / scaleFactor, yStart + (float) (30 * 3) / scaleFactor, (float) (100 * 3) / scaleFactor, (float) (100 * 3) / scaleFactor, 100 * 3 / scaleFactor, 100 * 3 / scaleFactor);
                    RenderUtils.drawTexturedRect(context.getMatrices(), raidBackgroundTexture, xStart + (float) (10 * 3) / scaleFactor, yStart + (float) (140 * 3) / scaleFactor, (float) (275 * 3) / scaleFactor, (float) (100 * 3) / scaleFactor, 275 * 3 / scaleFactor, 100 * 3 / scaleFactor);
                    RenderUtils.drawTexturedRect(context.getMatrices(), NOLTexture, xStart + (float) (10 * 3) / scaleFactor, yStart + (float) (140 * 3) / scaleFactor, (float) (100 * 3) / scaleFactor, (float) (100 * 3) / scaleFactor, 100 * 3 / scaleFactor, 100 * 3 / scaleFactor);
                    RenderUtils.drawTexturedRect(context.getMatrices(), raidBackgroundTexture, xStart + (float) (315 * 3) / scaleFactor, yStart + (float) (140 * 3) / scaleFactor, (float) (275 * 3) / scaleFactor, (float) (100 * 3) / scaleFactor, 275 * 3 / scaleFactor, 100 * 3 / scaleFactor);
                    RenderUtils.drawTexturedRect(context.getMatrices(), TNATexture, xStart + (float) (490 * 3) / scaleFactor, yStart + (float) (140 * 3) / scaleFactor, (float) (100 * 3) / scaleFactor, (float) (100 * 3) / scaleFactor, 100 * 3 / scaleFactor, 100 * 3 / scaleFactor);
                }

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
                        FontRenderer.getInstance().renderText(context.getMatrices(), StyledText.fromComponent(Text.of("Rank #" + formatter.format(NOTGRank))), xStart + (float) (115 * 3) / scaleFactor, yStart + (float) (85 * 3) / scaleFactor, notgColor, HorizontalAlignment.LEFT, VerticalAlignment.TOP, TextShadow.NORMAL, 1.3f * 3 / scaleFactor);
                        //context.drawText(MinecraftClient.getInstance().textRenderer, "Rank #" + NOTGRank, xStart + 55, yStart + 42, notgColor.asInt(), true);
                    }
                    if(NOLRank != -1) {
                        FontRenderer.getInstance().renderText(context.getMatrices(), StyledText.fromComponent(Text.of("Rank #" + formatter.format(NOLRank))), xStart + (float) (115 * 3) / scaleFactor, yStart + (float) (195 * 3) / scaleFactor, nolColor, HorizontalAlignment.LEFT, VerticalAlignment.TOP, TextShadow.NORMAL, 1.3f * 3 / scaleFactor);
                        //context.drawText(MinecraftClient.getInstance().textRenderer, "Rank #" + NOLRank, xStart + 55, yStart + 127, nolColor.asInt(), true);
                    }
                    if(TCCRank != -1) {
                        FontRenderer.getInstance().renderText(context.getMatrices(), StyledText.fromComponent(Text.of("Rank #" + formatter.format(TCCRank))), xStart + (float) (490 * 3) / scaleFactor, yStart + (float) (85 * 3) / scaleFactor, tccColor, HorizontalAlignment.RIGHT, VerticalAlignment.TOP, TextShadow.NORMAL, 1.3f * 3 / scaleFactor);
                        //context.drawText(MinecraftClient.getInstance().textRenderer, "Rank #" + TCCRank, xStart + 240 + textRenderer.getWidth("The Canyon Colossus") - textRenderer.getWidth("Rank #" + TCCRank), yStart + 42, tccColor.asInt(), true);
                    }
                    if(TNARank != -1) {
                        FontRenderer.getInstance().renderText(context.getMatrices(), StyledText.fromComponent(Text.of("Rank #" + formatter.format(TNARank))), xStart + (float) (490 * 3) / scaleFactor, yStart + (float) (195 * 3) / scaleFactor, tnaColor, HorizontalAlignment.RIGHT, VerticalAlignment.TOP, TextShadow.NORMAL, 1.3f * 3 / scaleFactor);
                        //context.drawText(MinecraftClient.getInstance().textRenderer, "Rank #" + TNARank, xStart + 238 + textRenderer.getWidth("The Nameless Anomaly") - textRenderer.getWidth("Rank #" + TNARank), yStart + 127, tnaColor.asInt(), true);
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

                FontRenderer.getInstance().renderText(context.getMatrices(), StyledText.fromComponent(Text.of("Nest of the Grootslangs")), xStart + (float) (115 * 3) / scaleFactor, yStart + (float) (55 * 3) / scaleFactor, notgColor, HorizontalAlignment.LEFT, VerticalAlignment.TOP, TextShadow.NORMAL, 1.3f * 3 / scaleFactor);
                //context.drawText(MinecraftClient.getInstance().textRenderer, "Nest of the Grootslangs", xStart + 55, yStart + 22, notgColor.asInt(), true);
                FontRenderer.getInstance().renderText(context.getMatrices(), StyledText.fromComponent(Text.of("Orphion's Nexus of Light")), xStart + (float) (115 * 3) / scaleFactor, yStart + (float) (165 * 3) / scaleFactor, nolColor, HorizontalAlignment.LEFT, VerticalAlignment.TOP, TextShadow.NORMAL, 1.3f * 3 / scaleFactor);
                //context.drawText(MinecraftClient.getInstance().textRenderer, "Orphion's Nexus of Light", xStart + 55, yStart + 107, nolColor.asInt(), true);
                FontRenderer.getInstance().renderText(context.getMatrices(), StyledText.fromComponent(Text.of("The Canyon Colossus")), xStart + (float) (490 * 3) / scaleFactor, yStart + (float) (55 * 3) / scaleFactor, tccColor, HorizontalAlignment.RIGHT, VerticalAlignment.TOP, TextShadow.NORMAL, 1.3f * 3 / scaleFactor);
                //context.drawText(MinecraftClient.getInstance().textRenderer, "The Canyon Colossus", xStart + 240, yStart + 22, tccColor.asInt(), true);
                FontRenderer.getInstance().renderText(context.getMatrices(), StyledText.fromComponent(Text.of("The Nameless Anomaly")), xStart + (float) (490 * 3) / scaleFactor, yStart + (float) (165 * 3) / scaleFactor, tnaColor, HorizontalAlignment.RIGHT, VerticalAlignment.TOP, TextShadow.NORMAL, 1.3f * 3 / scaleFactor);
                //context.drawText(MinecraftClient.getInstance().textRenderer, "The Nameless Anomaly", xStart + 238, yStart + 107, tnaColor.asInt(), true);

                if(raids != null) {
                    long NOTGComps = raids.getList().getOrDefault("Nest of the Grootslangs", 0);
                    long NOLComps = raids.getList().getOrDefault("Orphion's Nexus of Light", 0);
                    long TCCComps = raids.getList().getOrDefault("The Canyon Colossus", 0);
                    long TNAComps = raids.getList().getOrDefault("The Nameless Anomaly", 0);
                    long TotalComps = raids.getTotal();

                    FontRenderer.getInstance().renderText(context.getMatrices(), StyledText.fromComponent(Text.of(formatter.format(NOTGComps) + " Completions")), xStart + (float) (115 * 3) / scaleFactor, yStart + (float) (70 * 3) / scaleFactor, notgColor, HorizontalAlignment.LEFT, VerticalAlignment.TOP, TextShadow.NORMAL, 1.3f * 3 / scaleFactor);

                    FontRenderer.getInstance().renderText(context.getMatrices(), StyledText.fromComponent(Text.of(formatter.format(NOLComps) + " Completions")), xStart + (float) (115 * 3) / scaleFactor, yStart + (float) (180 * 3) / scaleFactor, nolColor, HorizontalAlignment.LEFT, VerticalAlignment.TOP, TextShadow.NORMAL, 1.3f * 3 / scaleFactor);

                    FontRenderer.getInstance().renderText(context.getMatrices(), StyledText.fromComponent(Text.of(formatter.format(TCCComps) + " Completions")), xStart + (float) (490 * 3) / scaleFactor, yStart + (float) (70 * 3) / scaleFactor, tccColor, HorizontalAlignment.RIGHT, VerticalAlignment.TOP, TextShadow.NORMAL, 1.3f * 3 / scaleFactor);

                    FontRenderer.getInstance().renderText(context.getMatrices(), StyledText.fromComponent(Text.of(formatter.format(TNAComps) + " Completions")), xStart + (float) (490 * 3) / scaleFactor, yStart + (float) (180 * 3) / scaleFactor, tnaColor, HorizontalAlignment.RIGHT, VerticalAlignment.TOP, TextShadow.NORMAL, 1.3f * 3 / scaleFactor);

                    FontRenderer.getInstance().renderText(context.getMatrices(), StyledText.fromComponent(Text.of("Total Completions" + characterNameString + formatter.format(TotalComps))), xStart + (float) (300 * 3) / scaleFactor, yStart + (float) (10 * 3) / scaleFactor, CustomColor.fromHexString("FFFFFF"), HorizontalAlignment.CENTER, VerticalAlignment.TOP, TextShadow.NORMAL, 1.3f * 3 / scaleFactor);
                }

            }
            case Rankings -> {
                Map<String, Long> rankings = PV.currentPlayerData.getRanking();
                if(rankings == null) {
                    drawCenteredText("This player has their rankings private.", 900, 345, CustomColor.fromHexString("FF0000"), 5f);
                    break;
                }
                //RenderUtils.drawTexturedRect(context.getMatrices(), dungeonBackgroundTexture, xStart + 10, yStart + 10, 580, 230, 580, 230);

                for(int i = 0; i < 20; i++) {
                    if(i == 15) continue;
                    if(i > 18) continue;
                    int xPos = xStart + 10 * 3 / scaleFactor + (145 * 3 / scaleFactor * (i % 4));
                    int yPos = yStart + 10 * 3 / scaleFactor + (46 * 3 / scaleFactor * Math.floorDiv(i, 4));

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
                            RenderUtils.drawTexturedRect(context.getMatrices(), rankingBackgroundTextureDark, xPos, yPos, (float) (140 * 3) / scaleFactor, (float) (42 * 3) / scaleFactor, 140 * 3 / scaleFactor, 42 * 3 / scaleFactor);
                        } else {
                            RenderUtils.drawTexturedRect(context.getMatrices(), rankingBackgroundTexture, xPos, yPos, (float) (140 * 3) / scaleFactor, (float) (42 * 3) / scaleFactor, 140 * 3 / scaleFactor, 42 * 3 / scaleFactor);
                        }
                        RenderUtils.drawTexturedRect(context.getMatrices(), texture, xPos + (float) (4 * 3) / scaleFactor, yPos + (float) (6 * 3) / scaleFactor, (float) (30 * 3) / scaleFactor, (float) (30 * 3) / scaleFactor, 30 * 3 / scaleFactor, 30 * 3 / scaleFactor);
                        FontRenderer.getInstance().renderText(context.getMatrices(), StyledText.fromComponent(Text.of(text)), xPos + (float) (37 * 3) / scaleFactor, yPos + (float) (12 * 3) / scaleFactor, textColor, HorizontalAlignment.LEFT, VerticalAlignment.TOP, TextShadow.NORMAL, 1f * 3 / scaleFactor);
                        FontRenderer.getInstance().renderText(context.getMatrices(), StyledText.fromComponent(Text.of("#" + globalPlacementString)), xPos + (float) (37 * 3) / scaleFactor, yPos + (float) (22 * 3) / scaleFactor, textColor, HorizontalAlignment.LEFT, VerticalAlignment.TOP, TextShadow.NORMAL, 1f * 3 / scaleFactor);
                    } else if(i < 16){
                        xPos += (48 * 3 / scaleFactor * (i % 3));
                        if(SimpleConfig.getInstance(WynnExtrasConfig.class).darkmodeToggle) {
                            RenderUtils.drawTexturedRect(context.getMatrices(), rankingBackgroundWideTextureDark, xPos, yPos, (float) (189 * 3) / scaleFactor, (float) (42 * 3) / scaleFactor, 189 * 3 / scaleFactor, 42 * 3 / scaleFactor);
                        } else {
                            RenderUtils.drawTexturedRect(context.getMatrices(), rankingBackgroundWideTexture, xPos, yPos, (float) (189 * 3) / scaleFactor, (float) (42 * 3) / scaleFactor, 189 * 3 / scaleFactor, 42 * 3 / scaleFactor);
                        }
                        RenderUtils.drawTexturedRect(context.getMatrices(), texture, xPos + (float) (4 * 3) / scaleFactor, yPos + (float) (6 * 3) / scaleFactor, (float) (30 * 3) / scaleFactor, (float) (30 * 3) / scaleFactor, 30 * 3 / scaleFactor, 30 * 3 / scaleFactor);
                        FontRenderer.getInstance().renderText(context.getMatrices(), StyledText.fromComponent(Text.of(text)), xPos + (float) (37 * 3) / scaleFactor, yPos + (float) (12 * 3) / scaleFactor, textColor, HorizontalAlignment.LEFT, VerticalAlignment.TOP, TextShadow.NORMAL, 1f * 3 / scaleFactor);
                        FontRenderer.getInstance().renderText(context.getMatrices(), StyledText.fromComponent(Text.of("#" + globalPlacementString)), xPos + (float) (37 * 3) / scaleFactor, yPos + (float) (22 * 3) / scaleFactor, textColor, HorizontalAlignment.LEFT, VerticalAlignment.TOP, TextShadow.NORMAL, 1f * 3 / scaleFactor);

                    } else {
                        xPos += (48 * 3 / scaleFactor * ((i - 1) % 3));
                        if(SimpleConfig.getInstance(WynnExtrasConfig.class).darkmodeToggle) {
                            RenderUtils.drawTexturedRect(context.getMatrices(), rankingBackgroundWideTextureDark, xPos, yPos, (float) (189 * 3) / scaleFactor, (float) (42 * 3) / scaleFactor, 189 * 3 / scaleFactor, 42 * 3 / scaleFactor);
                        } else {
                            RenderUtils.drawTexturedRect(context.getMatrices(), rankingBackgroundWideTexture, xPos, yPos, (float) (189 * 3) / scaleFactor, (float) (42 * 3) / scaleFactor, 189 * 3 / scaleFactor, 42 * 3 / scaleFactor);
                        }
                        RenderUtils.drawTexturedRect(context.getMatrices(), texture, xPos + (float) (4 * 3) / scaleFactor, yPos + (float) (6 * 3) / scaleFactor, (float) (30 * 3) / scaleFactor, (float) (30 * 3) / scaleFactor, 30 * 3 / scaleFactor, 30 * 3 / scaleFactor);
                        FontRenderer.getInstance().renderText(context.getMatrices(), StyledText.fromComponent(Text.of(text)), xPos + (float) (37 * 3) / scaleFactor, yPos + (float) (7 * 3) / scaleFactor, textColor, HorizontalAlignment.LEFT, VerticalAlignment.TOP, TextShadow.NORMAL, 1f * 3 / scaleFactor);
                        FontRenderer.getInstance().renderText(context.getMatrices(), StyledText.fromComponent(Text.of("Global #" + globalPlacementString)), xPos + (float) (37 * 3) / scaleFactor, yPos + (float) (17 * 3) / scaleFactor, textColor, HorizontalAlignment.LEFT, VerticalAlignment.TOP, TextShadow.NORMAL, 1f * 3 / scaleFactor);
                        if(i >= 16) {
                            FontRenderer.getInstance().renderText(context.getMatrices(), StyledText.fromComponent(Text.of("Solo #" + soloPlacementString)), xPos + (float) (37 * 3) / scaleFactor, yPos + (float) (27 * 3) / scaleFactor, textColor, HorizontalAlignment.LEFT, VerticalAlignment.TOP, TextShadow.NORMAL, 1f * 3 / scaleFactor);
                        }}
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
                    RenderUtils.drawTexturedRect(context.getMatrices(), profBackgroundTextureDark, xStart + (float) (10 * 3) / scaleFactor, yStart + (float) (10 * 3) / scaleFactor, (float) (580 * 3) / scaleFactor, (float) (230 * 3) / scaleFactor, 580 * 3 / scaleFactor, 230 * 3 / scaleFactor);
                } else {
                    RenderUtils.drawTexturedRect(context.getMatrices(), profBackgroundTexture, xStart + (float) (10 * 3) / scaleFactor, yStart + (float) (10 * 3) / scaleFactor, (float) (580 * 3) / scaleFactor, (float) (230 * 3) / scaleFactor, 580 * 3 / scaleFactor, 230 * 3 / scaleFactor);
                }

                int i = 0;
                for(Map.Entry<String, Profession> prof : profs.entrySet()) {
                    Identifier profTexture = getProfTexture(prof.getKey());
                    if(i < 4) {
                        int level = prof.getValue().getLevel();
                        CustomColor levelColor;
                        RenderUtils.drawTexturedRect(context.getMatrices(), profTexture, xStart + (float) (70 * 3) / scaleFactor + (float) (i * 136 * 3) / scaleFactor, yStart + (float) (20 * 3) / scaleFactor, (float) (64 * 3) / scaleFactor, (float) (64 * 3) / scaleFactor, 64 * 3 / scaleFactor, 64 * 3 / scaleFactor);
                        if(level == 132) {
                            levelColor = CommonColors.RAINBOW;
                        } else if (level >= 110) {
                            levelColor = CommonColors.YELLOW;
                        } else {
                            levelColor = CustomColor.fromHexString("FFFFFF");
                        }
                        FontRenderer.getInstance().renderText(context.getMatrices(), StyledText.fromComponent(Text.of("Level " + level)), xStart + (float) (102 * 3) / scaleFactor + (float) (i * 136 * 3) / scaleFactor, yStart + (float) (90 * 3) / scaleFactor, levelColor, HorizontalAlignment.CENTER, VerticalAlignment.TOP, TextShadow.NORMAL, 2f * 3 / scaleFactor);
                        if(level < 132) {
                            FontRenderer.getInstance().renderText(context.getMatrices(), StyledText.fromComponent(Text.of("Progress to next Level: " + prof.getValue().getXpPercent() + "%")), xStart + (float) (102 * 3) / scaleFactor + (float) (i * 136 * 3) / scaleFactor, yStart + (float) (110 * 3) / scaleFactor, levelColor, HorizontalAlignment.CENTER, VerticalAlignment.TOP, TextShadow.NORMAL, 0.8f * 3 / scaleFactor);
                        }
                    } else {
                        int level = prof.getValue().getLevel();
                        CustomColor levelColor;
                        RenderUtils.drawTexturedRect(context.getMatrices(), profTexture, xStart + (float) (44 * 3) / scaleFactor + (float) ((i - 4) * 68 * 3) / scaleFactor, yStart + (float) (200 * 3) / scaleFactor, (float) (32 * 3) / scaleFactor, (float) (32 * 3) / scaleFactor, 32 * 3 / scaleFactor, 32 * 3 / scaleFactor);
                        if(level == 132) {
                            levelColor = CommonColors.RAINBOW;
                        } else if (level >= 103) {
                            levelColor = CommonColors.YELLOW;
                        } else {
                            levelColor = CustomColor.fromHexString("FFFFFF");
                        }
                        FontRenderer.getInstance().renderText(context.getMatrices(), StyledText.fromComponent(Text.of("Level " + level)), xStart + (float) (44 * 3) / scaleFactor + (float) ((i - 4) * 68 * 3) / scaleFactor + (float) (16 * 3) / scaleFactor, yStart + (float) (187 * 3) / scaleFactor, levelColor, HorizontalAlignment.CENTER, VerticalAlignment.TOP, TextShadow.NORMAL, 1.2f * 3 / scaleFactor);
                        if(level < 132) {
                            FontRenderer.getInstance().renderText(context.getMatrices(), StyledText.fromComponent(Text.of("Progress to")), xStart + (float) (44 * 3) / scaleFactor + (float) ((i - 4) * 68 * 3) / scaleFactor + (float) (16 * 3) / scaleFactor, yStart + (float) (170 * 3) / scaleFactor, levelColor, HorizontalAlignment.CENTER, VerticalAlignment.TOP, TextShadow.NORMAL, 0.8f * 3 / scaleFactor);
                            FontRenderer.getInstance().renderText(context.getMatrices(), StyledText.fromComponent(Text.of("next Level: " + prof.getValue().getXpPercent() + "%")), xStart + (float) (44 * 3) / scaleFactor + (float) ((i - 4) * 68 * 3) / scaleFactor + (float) (16 * 3) / scaleFactor, yStart + (float) (178 * 3) / scaleFactor, levelColor, HorizontalAlignment.CENTER, VerticalAlignment.TOP, TextShadow.NORMAL, 0.8f * 3 / scaleFactor);
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
                    RenderUtils.drawTexturedRect(context.getMatrices(), dungeonBackgroundTextureDark, xStart + (float) (10 * 3) / scaleFactor, yStart + (float) (29 * 3) / scaleFactor, (float) (580 * 3) / scaleFactor, (float) (211 * 3) / scaleFactor, 580 * 3 / scaleFactor, 211 * 3 / scaleFactor);
                } else {
                    RenderUtils.drawTexturedRect(context.getMatrices(), dungeonBackgroundTexture, xStart + (float) (10 * 3) / scaleFactor, yStart + (float) (29 * 3) / scaleFactor, (float) (580 * 3) / scaleFactor, (float) (211 * 3) / scaleFactor, 580 * 3 / scaleFactor, 211 * 3 / scaleFactor);
                }

                int i = 0;

                DecimalFormat formatter = new DecimalFormat("#,###");
                for(Identifier dungeon : dungeonTextures) {
                    int comps = getDungeonComps(i, normalComps);
                    int cComps = getCorruptedComps(i, corruptedComps);
                    int dungeonY = yStart + 11 * 3 / scaleFactor + Math.floorDiv(i, 5) * 145 * 3 / scaleFactor;
                    int dungeonX = xStart + 30 * 3 / scaleFactor + 115 * 3 / scaleFactor * (i % 5);
                    if(Math.floorDiv(i, 5) > 0) {
                        RenderUtils.drawTexturedRect(context.getMatrices(), dungeon, dungeonX + (float) (10 * 3) / scaleFactor, dungeonY + (float) (5 * 3) / scaleFactor, (float) (60 * 3) / scaleFactor, (float) (60 * 3) / scaleFactor, 60 * 3 / scaleFactor, 60 * 3 / scaleFactor);
                        FontRenderer.getInstance().renderText(context.getMatrices(), StyledText.fromComponent(Text.of(getDungeonName(i))), dungeonX + (float) (40 * 3) / scaleFactor, dungeonY + (float) (70 * 3) / scaleFactor, CustomColor.fromHexString("FFFFFF"), HorizontalAlignment.CENTER, VerticalAlignment.TOP, TextShadow.NORMAL, 1f * 3 / scaleFactor);

                        if(i < 8) {
                            RenderUtils.drawTexturedRect(context.getMatrices(), dungeonKeyTexture, dungeonX + (float) (20 * 3) / scaleFactor, dungeonY - (float) (15 * 3) / scaleFactor, (float) (20 * 3) / scaleFactor, (float) (20 * 3) / scaleFactor, 20 * 3 / scaleFactor, 20 * 3 / scaleFactor);
                            FontRenderer.getInstance().renderText(context.getMatrices(), StyledText.fromComponent(Text.of(formatter.format(comps))), dungeonX + (float) (20 * 3) / scaleFactor, dungeonY - (float) (10 * 3) / scaleFactor, CustomColor.fromHexString("FFFFFF"), HorizontalAlignment.RIGHT, VerticalAlignment.TOP, TextShadow.NORMAL, 1f * 3 / scaleFactor);
                        } else {
                            RenderUtils.drawTexturedRect(context.getMatrices(), dungeonKeyTexture, dungeonX + (float) (30 * 3) / scaleFactor, dungeonY - (float) (15 * 3) / scaleFactor, (float) (20 * 3) / scaleFactor, (float) (20 * 3) / scaleFactor, 20 * 3 / scaleFactor, 20 * 3 / scaleFactor);
                            FontRenderer.getInstance().renderText(context.getMatrices(), StyledText.fromComponent(Text.of(formatter.format(comps))), dungeonX + (float) (30 * 3) / scaleFactor, dungeonY - (float) (10 * 3) / scaleFactor, CustomColor.fromHexString("FFFFFF"), HorizontalAlignment.RIGHT, VerticalAlignment.TOP, TextShadow.NORMAL, 1f * 3 / scaleFactor);
                        }

                        if(i < 8) {
                            RenderUtils.drawTexturedRect(context.getMatrices(), corruptedDungeonKeyTexture, dungeonX + (float) (40 * 3) / scaleFactor, dungeonY - (float) (15 * 3) / scaleFactor, (float) (20 * 3) / scaleFactor, (float) (20 * 3) / scaleFactor, 20 * 3 / scaleFactor, 20 * 3 / scaleFactor);
                            FontRenderer.getInstance().renderText(context.getMatrices(), StyledText.fromComponent(Text.of(formatter.format(cComps))), dungeonX + (float) (62 * 3) / scaleFactor, dungeonY - (float) (10 * 3) / scaleFactor, CustomColor.fromHexString("FFFFFF"), HorizontalAlignment.LEFT, VerticalAlignment.TOP, TextShadow.NORMAL, 1f * 3 / scaleFactor);
                        }
                    } else {
                        RenderUtils.drawTexturedRect(context.getMatrices(), dungeon, dungeonX + (float) (10 * 3) / scaleFactor, dungeonY + (float) (35 * 3) / scaleFactor, (float) (60 * 3) / scaleFactor, (float) (60 * 3) / scaleFactor, 60 * 3 / scaleFactor, 60 * 3 / scaleFactor);
                        FontRenderer.getInstance().renderText(context.getMatrices(), StyledText.fromComponent(Text.of(getDungeonName(i))), dungeonX + (float) (40 * 3) / scaleFactor, dungeonY + (float) (25 * 3) / scaleFactor, CustomColor.fromHexString("FFFFFF"), HorizontalAlignment.CENTER, VerticalAlignment.TOP, TextShadow.NORMAL, 1f * 3 / scaleFactor);


                        RenderUtils.drawTexturedRect(context.getMatrices(), dungeonKeyTexture, dungeonX + (float) (20 * 3) / scaleFactor, dungeonY + (float) (95 * 3) / scaleFactor, (float) (20 * 3) / scaleFactor, (float) (20 * 3) / scaleFactor, 20 * 3 / scaleFactor, 20 * 3 / scaleFactor);
                        FontRenderer.getInstance().renderText(context.getMatrices(), StyledText.fromComponent(Text.of(String.valueOf(comps))), dungeonX + (float) (20 * 3) / scaleFactor, dungeonY + (float) (100 * 3) / scaleFactor, CustomColor.fromHexString("FFFFFF"), HorizontalAlignment.RIGHT, VerticalAlignment.TOP, TextShadow.NORMAL, 1f * 3 / scaleFactor);

                        if(i < 8) {
                            RenderUtils.drawTexturedRect(context.getMatrices(), corruptedDungeonKeyTexture, dungeonX + (float) (40 * 3) / scaleFactor, dungeonY + (float) (95 * 3) / scaleFactor, (float) (20 * 3) / scaleFactor, (float) (20 * 3) / scaleFactor, 20 * 3 / scaleFactor, 20 * 3 / scaleFactor);
                            FontRenderer.getInstance().renderText(context.getMatrices(), StyledText.fromComponent(Text.of(formatter.format(cComps))), dungeonX + (float) (62 * 3) / scaleFactor, dungeonY + (float) (100 * 3) / scaleFactor, CustomColor.fromHexString("FFFFFF"), HorizontalAlignment.LEFT, VerticalAlignment.TOP, TextShadow.NORMAL, 1f * 3 / scaleFactor);
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

                    FontRenderer.getInstance().renderText(context.getMatrices(), StyledText.fromComponent(Text.of("Total Completions" + characterNameString + formatter.format(TotalComps))), xStart + (float) (300 * 3) / scaleFactor, yStart + (float) (10 * 3) / scaleFactor, CustomColor.fromHexString("FFFFFF"), HorizontalAlignment.CENTER, VerticalAlignment.TOP, TextShadow.NORMAL, 1.3f * 3 / scaleFactor);

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
                    RenderUtils.drawTexturedRect(context.getMatrices(), questBackgroundTextureDark, xStart + (float) (10 * 3) / scaleFactor, yStart + (float) (30 * 3) / scaleFactor, (float) (580 * 3) / scaleFactor, (float) (200 * 3) / scaleFactor, 580 * 3 / scaleFactor, 200 * 3 / scaleFactor);
                    RenderUtils.drawTexturedRect(context.getMatrices(), questSearchbarTextureDark, xStart + (float) (200 * 3) / scaleFactor, yStart + height, (float) (400 * 3) / scaleFactor, (float) (20 * 3) / scaleFactor, 400 * 3 / scaleFactor, 20 * 3 / scaleFactor);
                } else {
                    RenderUtils.drawTexturedRect(context.getMatrices(), questBackgroundTexture, xStart + (float) (10 * 3) / scaleFactor, yStart + (float) (30 * 3) / scaleFactor, (float) (580 * 3) / scaleFactor, (float) (200 * 3) / scaleFactor, 580 * 3 / scaleFactor, 200 * 3 / scaleFactor);
                    RenderUtils.drawTexturedRect(context.getMatrices(), questSearchbarTexture, xStart + (float) (200 * 3) / scaleFactor, yStart + height, (float) (400 * 3) / scaleFactor, (float) (20 * 3) / scaleFactor, 400 * 3 / scaleFactor, 20 * 3 / scaleFactor);
                }

                questSearchBar.setX(xStart + 200 * 3 / scaleFactor);
                questSearchBar.setY(yStart + height + 7 * 3 / scaleFactor);
                questSearchBar.setWidth(400 * 3 / scaleFactor);
                questSearchBar.setHeight(14 * 3 / scaleFactor);
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
                    int yPos = yStart + 38 * 3 / scaleFactor + Math.floorDiv(i, 2) * 12 * 3 / scaleFactor - scrollOffset * 3 / scaleFactor;
                    if(yPos > yStart + 230 * 3 / scaleFactor) break;
                    if(yPos > yStart + 20 * 3 / scaleFactor) {
                        HorizontalAlignment horizontalAlignment = HorizontalAlignment.LEFT;
                        int rightOffset = 0;
                        if (i % 2 == 1) {
                            rightOffset = 560 * 3 / scaleFactor;
                            horizontalAlignment = HorizontalAlignment.RIGHT;
                        }

                        FontRenderer.getInstance().renderText(context.getMatrices(), StyledText.fromComponent(Text.of(quest)), (float) xStart + (float) (20 * 3) / scaleFactor + rightOffset, (float) yPos, textColor, horizontalAlignment, VerticalAlignment.TOP, TextShadow.NORMAL, 1f * 3 / scaleFactor);
                    }
                    i++;
                }

                {
                    int yPos = yStart + 20 * 3 / scaleFactor + 38 * 3 / scaleFactor + Math.floorDiv(i, 2) * 12 * 3 / scaleFactor - scrollOffset * 3 / scaleFactor;
                    if (yPos > yStart + 20 * 3 / scaleFactor && yPos < yStart + 230 * 3 / scaleFactor) {
                        FontRenderer.getInstance().renderText(context.getMatrices(), StyledText.fromComponent(Text.of("Missing:")), (float) xStart + (float) (20 * 3) / scaleFactor, (float) yPos, textColor, HorizontalAlignment.LEFT, VerticalAlignment.TOP, TextShadow.NORMAL, 2f * 3 / scaleFactor);
                        if(i % 2 == 0) i += 2 * 3 / scaleFactor;
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
                    int yPos = yStart + 68 * 3 / scaleFactor + Math.floorDiv(i, 2) * 12 * 3 / scaleFactor - scrollOffset * 3 / scaleFactor;
                    if(yPos > yStart + 230 * 3 / scaleFactor) break;
                    if(yPos > yStart + 20 * 3 / scaleFactor) {
                        HorizontalAlignment horizontalAlignment = HorizontalAlignment.LEFT;
                        int rightOffset = 0;
                        if (i % 2 == 1) {
                            rightOffset = 560 * 3 / scaleFactor;
                            horizontalAlignment = HorizontalAlignment.RIGHT;
                        }

                        FontRenderer.getInstance().renderText(context.getMatrices(), StyledText.fromComponent(Text.of(quest)), (float) xStart + (float) (20 * 3) / scaleFactor + rightOffset, (float) yPos, textColor, horizontalAlignment, VerticalAlignment.TOP, TextShadow.NORMAL, 1f * 3 / scaleFactor);
                    }
                    i++;
                }

                if(SimpleConfig.getInstance(WynnExtrasConfig.class).darkmodeToggle) {
                    RenderUtils.drawTexturedRect(context.getMatrices(), questBackgroundBorderTextureDark, xStart + (float) (10 * 3) / scaleFactor, yStart + (float) (20 * 3) / scaleFactor, (float) (580 * 3) / scaleFactor, (float) (220 * 3) / scaleFactor, 580 * 3 / scaleFactor, 220 * 3 / scaleFactor);
                } else {
                    RenderUtils.drawTexturedRect(context.getMatrices(), questBackgroundBorderTexture, xStart + (float) (10 * 3) / scaleFactor, yStart + (float) (20 * 3) / scaleFactor, (float) (580 * 3) / scaleFactor, (float) (220 * 3) / scaleFactor, 580 * 3 / scaleFactor, 220 * 3 / scaleFactor);
                }
                FontRenderer.getInstance().renderText(context.getMatrices(), StyledText.fromComponent(Text.of(titleString)), (float) xStart + (float) (300 * 3) / scaleFactor, (float) yStart + (float) (10 * 3) / scaleFactor, textColor, HorizontalAlignment.CENTER, VerticalAlignment.TOP, TextShadow.NORMAL, 1.5f * 3 / scaleFactor);

            }
            case Misc -> {
                Global data = PV.currentPlayerData.getGlobalData();
                if(data == null) {
                    drawCenteredText("This player has their misc stats private.", 900, 345, CustomColor.fromHexString("FF0000"), 5f);
                    break;
                }

                if(SimpleConfig.getInstance(WynnExtrasConfig.class).darkmodeToggle) {
                    RenderUtils.drawTexturedRect(context.getMatrices(), miscBackgroundTextureDark, xStart + (float) (10 * 3) / scaleFactor, yStart + (float) (10 * 3) / scaleFactor, (float) (580 * 3) / scaleFactor, (float) (230 * 3) / scaleFactor, 580 * 3 / scaleFactor, 230 * 3 / scaleFactor);
                } else {
                    RenderUtils.drawTexturedRect(context.getMatrices(), miscBackgroundTexture, xStart + (float) (10 * 3) / scaleFactor, yStart + (float) (10 * 3) / scaleFactor, (float) (580 * 3) / scaleFactor, (float) (230 * 3) / scaleFactor, 580 * 3 / scaleFactor, 230 * 3 / scaleFactor);
                }
                FontRenderer.getInstance().renderText(context.getMatrices(), StyledText.fromComponent(Text.of("Wars completed: " + data.getWars())), (float) xStart + (float) (20 * 3) / scaleFactor, (float) yStart + (float) (20 * 3) / scaleFactor, CustomColor.fromHexString("FFFFFF"), HorizontalAlignment.LEFT, VerticalAlignment.TOP, TextShadow.NORMAL, 2f * 3 / scaleFactor);
                FontRenderer.getInstance().renderText(context.getMatrices(), StyledText.fromComponent(Text.of("Dungeons completed: " + data.getDungeons().getTotal())), (float) xStart + (float) (20 * 3) / scaleFactor, (float) yStart + (float) (40 * 3) / scaleFactor, CustomColor.fromHexString("FFFFFF"), HorizontalAlignment.LEFT, VerticalAlignment.TOP, TextShadow.NORMAL, 2f * 3 / scaleFactor);
                FontRenderer.getInstance().renderText(context.getMatrices(), StyledText.fromComponent(Text.of("Unique Caves completed: " + data.getCaves())), (float) xStart + (float) (20 * 3) / scaleFactor, (float) yStart + (float) (60 * 3) / scaleFactor, CustomColor.fromHexString("FFFFFF"), HorizontalAlignment.LEFT, VerticalAlignment.TOP, TextShadow.NORMAL, 2f * 3 / scaleFactor);
                FontRenderer.getInstance().renderText(context.getMatrices(), StyledText.fromComponent(Text.of("Unique Lootrun camps completed: " + data.getLootruns())), (float) xStart + (float) (20 * 3) / scaleFactor, (float) yStart + (float) (80 * 3) / scaleFactor, CustomColor.fromHexString("FFFFFF"), HorizontalAlignment.LEFT, VerticalAlignment.TOP, TextShadow.NORMAL, 2f * 3 / scaleFactor);
                FontRenderer.getInstance().renderText(context.getMatrices(), StyledText.fromComponent(Text.of("Unique World events completed: " + data.getWorldEvents())), (float) xStart + (float) (20 * 3) / scaleFactor, (float) yStart + (float) (100 * 3) / scaleFactor, CustomColor.fromHexString("FFFFFF"), HorizontalAlignment.LEFT, VerticalAlignment.TOP, TextShadow.NORMAL, 2f * 3 / scaleFactor);
                FontRenderer.getInstance().renderText(context.getMatrices(), StyledText.fromComponent(Text.of("Chests opened: " + data.getChestsFound())), (float) xStart + (float) (20 * 3) / scaleFactor, (float) yStart + (float) (120 * 3) / scaleFactor, CustomColor.fromHexString("FFFFFF"), HorizontalAlignment.LEFT, VerticalAlignment.TOP, TextShadow.NORMAL, 2f * 3 / scaleFactor);
                FontRenderer.getInstance().renderText(context.getMatrices(), StyledText.fromComponent(Text.of("Mobs killed: " + data.getMobsKilled())), (float) xStart + (float) (20 * 3) / scaleFactor, (float) yStart + (float) (140 * 3) / scaleFactor, CustomColor.fromHexString("FFFFFF"), HorizontalAlignment.LEFT, VerticalAlignment.TOP, TextShadow.NORMAL, 2f * 3 / scaleFactor);
                FontRenderer.getInstance().renderText(context.getMatrices(), StyledText.fromComponent(Text.of("Pvp kills: " + data.getPvp().getKills())), (float) xStart + (float) (20 * 3) / scaleFactor, (float) yStart + (float) (160 * 3) / scaleFactor, CustomColor.fromHexString("FFFFFF"), HorizontalAlignment.LEFT, VerticalAlignment.TOP, TextShadow.NORMAL, 2f * 3 / scaleFactor);
                FontRenderer.getInstance().renderText(context.getMatrices(), StyledText.fromComponent(Text.of("Pvp deaths: " + data.getPvp().getDeaths())), (float) xStart + (float) (20 * 3) / scaleFactor, (float) yStart + (float) (180 * 3) / scaleFactor, CustomColor.fromHexString("FFFFFF"), HorizontalAlignment.LEFT, VerticalAlignment.TOP, TextShadow.NORMAL, 2f * 3 / scaleFactor);

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
            RenderUtils.drawTexturedRect(context.getMatrices(), openInBrowserButtonTextureWDark, xStart + 89 * 3 / scaleFactor, yStart + height, 100 * 3 / scaleFactor, 20 * 3 / scaleFactor, 100 * 3 / scaleFactor, 20 * 3 / scaleFactor);
        } else {
            RenderUtils.drawTexturedRect(context.getMatrices(), openInBrowserButtonTextureW, xStart + 89 * 3 / scaleFactor, yStart + height, 100 * 3 / scaleFactor, 20 * 3 / scaleFactor, 100 * 3 / scaleFactor, 20 * 3 / scaleFactor);
        }

        if (searchBar != null) {
            searchBar.setX(xStart + 89 * 3 / scaleFactor);
            searchBar.setY(yStart + height + 7 * 3 / scaleFactor);
            searchBar.drawWithoutBackground(context, CustomColor.fromHexString("FFFFFF"));
        }


    }

    public Identifier getProfTexture(String prof) {
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

    public int getDungeonComps(int i, Map<String, Integer> map) {
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

    public int getCorruptedComps(int i, Map<String, Integer> map) {
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

    public String getDungeonName(int i) {
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

    public String getClassName(CharacterData entry) {
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
        dummy = null;
        openInBrowserButton = null;
        searchBar = null;
        questSearchBar = null;
        super.close();
    }

    public Rank getRank() {
        return switch (PV.currentPlayerData.getSupportRank()) {
            case "vip" -> Rank.VIP;
            case "vipplus" -> Rank.VIPPLUS;
            case "hero" -> Rank.HERO;
            case "heroplus" -> Rank.HEROPLUS;
            case "champion" -> Rank.CHAMPION;
            case null, default -> Rank.NONE;
        };
    }

    public Identifier getRankBadge() {
        Rank rank = getRank();
        return switch (rank) {
            case VIP -> vip;
            case VIPPLUS -> vipplus;
            case HERO -> hero;
            case HEROPLUS -> heroplus;
            case CHAMPION -> champion;
            default -> null;
        };
    }

    public int getRankBadgeWidth() {
        Rank rank = getRank();
        return switch (rank) {
            case VIP -> 66;
            case VIPPLUS -> 87;
            case HERO -> 93;
            case HEROPLUS -> 114;
            case CHAMPION -> 159;
            default -> 0;
        };
    }

    static float playerRotationY = 0;
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


    public static void drawPlayer(
            DrawContext context,
            int x, int y, int scale,
            float mouseX, float mouseY,
            LivingEntity player
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
            sleepOffsetX = (float) (60 * 3) / scaleFactor;
            sleepOffsetY = (float) (10 * 3) / scaleFactor;
        } else {
            sleepOffsetX = 0;
            sleepOffsetY = 0;
        }

        if(PV.currentPlayer.equalsIgnoreCase("teslanator")) {
            rotation.rotateX((float) Math.PI);
            flipOffset = -130 * 3 / scaleFactor;
            rotation.rotateY((float) Math.PI);
        }


        EntityRenderDispatcher dispatcher = MinecraftClient.getInstance().getEntityRenderDispatcher();
        dispatcher.setRenderShadows(false);

        float smolScale = 1;
        float thickScale = 1;

//        System.out.println(PV.currentPlayer);
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

    public Identifier getClassTexture(String className) {
        return switch (className) {
            case "WARRIOR" -> warriorTexture;
            case "SHAMAN" -> shamanTexture;
            case "ARCHER" -> archerTexture;
            case "MAGE" -> mageTexture;
            case "ASSASSIN" -> assassinTexture;
            default -> null;
        };
    }

    public Identifier getGoldClassTexture(String className) {
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
        if(scaleFactor == 0) return;
        int screenWidth = MinecraftClient.getInstance().getWindow().getScaledWidth();
        int screenHeight = MinecraftClient.getInstance().getWindow().getScaledHeight();
        int width = 600 * 3 / scaleFactor;
        int height = 250 * 3 / scaleFactor;
        int xStart = screenWidth / 2 - width / 2;
        int yStart = screenHeight / 2 - height / 2;
        int dragX = xStart + 23 * 3 / scaleFactor;
        int dragY = yStart + 33 * 3 / scaleFactor;
        int dragWidth = 143 * 3 / scaleFactor;
        int dragHeight = 143 * 3 / scaleFactor;

        if (mouseX >= dragX && mouseX <= dragX + dragWidth &&
                mouseY >= dragY && mouseY <= dragY + dragHeight) {
            draggingAllowed = true;
        } else {
            draggingAllowed = false;
        }

        if(openInBrowserButton == null || searchBar == null || (currentTab == Tab.Quests && questSearchBar == null)) return;
        if(openInBrowserButton.isClickInBounds(PVScreen.mouseX, PVScreen.mouseY)) {
            openInBrowserButton.click();
        }
        if(searchBar.isClickInBounds(PVScreen.mouseX, PVScreen.mouseY)) {
            searchBar.click();
        } else {
            searchBar.setActive(false);
        }
        if(questSearchBar.isClickInBounds(PVScreen.mouseX, PVScreen.mouseY)) {
            questSearchBar.click();
        } else {
            questSearchBar.setActive(false);
        }
        for(EasyButton button : tabButtons) {
            if(button.isClickInBounds(PVScreen.mouseX, PVScreen.mouseY)) {
                button.click();
            }
        }
        if(currentTab == Tab.General) {
            for (CharacterButton button : characterButtons) {
                if (button.isClickInBounds(PVScreen.mouseX, PVScreen.mouseY)) {
                    button.click();
                }
            }
        }
    }

    public int drawDynamicNameSign(DrawContext context, String input, int x, int y) {
        TextRenderer textRenderer = MinecraftClient.getInstance().textRenderer;
        int strWidth = textRenderer.getWidth(input) + 10;
        int strMidWidth = strWidth - 15;
        int amount = Math.max(0, Math.ceilDiv(strMidWidth, 10));
        if(SimpleConfig.getInstance(WynnExtrasConfig.class).darkmodeToggle) {
            drawImage(tabLeftDark, x, y, 30, 60);
        } else {
            drawImage(tabLeft, x, y, 30, 60);
        }
        for (int i = 0; i < amount; i++) {
            if(SimpleConfig.getInstance(WynnExtrasConfig.class).darkmodeToggle) {
                drawImage(tabMidDark, x + 30 * (i + 1), y, 30, 60);
            } else {
                drawImage(tabMid, x + 30 * (i + 1), y, 30, 60);
            }
        }
        if(SimpleConfig.getInstance(WynnExtrasConfig.class).darkmodeToggle) {
            drawImage(tagRightDark, x + 30 * (amount + 1), y, 30, 60);
        } else {
            drawImage(tagRight, x + 30 * (amount + 1), y, 30, 60);
        }
        return 60 + amount * 30;
    }
}
