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
import julianh06.wynnextras.features.profileviewer.data.*;
import julianh06.wynnextras.utils.overlays.EasyButton;
import net.fabricmc.fabric.api.client.screen.v1.ScreenMouseEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
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

public class PVScreen extends Screen {
    static int mouseX = 0;
    static int mouseY = 0;

    public enum Rank {NONE, VIP, VIPPLUS, HERO, HEROPLUS, CHAMPION}

    public enum Tab {General, Raids, Rankings, Professions, Dungeons, Quests, Misc}
    public static List<TabButton> tabButtons = new ArrayList<>();
    public static List<CharacterButton> characterButtons = new ArrayList<>();

    static List<String> WETeam = List.of("JulianH06", "Teslanator", "Mikecraft1224", "LegendaryVirus", "elwood24");

    List<String> allQuests = Arrays.asList("???", "A Grave Mistake", "A Hunter's Calling", "A Journey Beyond", "A Journey Further", "A Marauder's Dues", "A Sandy Scandal", "Acquiring Credentials", "Aldorei's Secret Part I", "Aldorei's Secret Part II", "All Roads To Peace", "An Iron Heart Part I", "An Iron Heart Part II", "Arachnids' Ascent", "Beneath the Depths", "Beyond the Grave", "Blazing Retribution", "Bob's Lost Soul", "Canyon Condor", "Clearing the Camps", "Cluck Cluck", "Cook Assistant", "Corrupted Betrayal", "Cowfusion", "Creeper Infiltration", "Crop Failure", "Death Whistle", "Deja Vu", "Desperate Metal", "Dwarves and Doguns Part I", "Dwarves and Doguns Part II", "Dwarves and Doguns Part III", "Dwarves and Doguns Part IV", "Dwelling Walls", "Elemental Exercise", "Enter the Dojo", "Enzan's Brother", "Fallen Delivery", "Fantastic Voyage", "Fate of the Fallen", "Flight in Distress", "Forbidden Prison", "From the Bottom", "From the Mountains", "Frost Bite", "General's Orders", "Grand Youth", "Grave Digger", "Green Gloop", "Haven Antiquity", "Heart of Llevigar", "Hollow Serenity", "Hunger of the Gerts Part I", "Hunger of the Gerts Part II", "Ice Nations", "Infested Plants", "Jungle Fever", "King's Recruit", "Kingdom of Sand", "Lava Springs", "Lazarus Pit", "Lexdale Witch Trials", "Lost in the Jungle", "Lost Royalty", "Lost Soles", "Lost Tower", "Maltic's Well", "Master Piece", "Meaningful Holiday", "Memory Paranoia", "Mini-Quest - Gather Acacia Logs", "Mini-Quest - Gather Acacia Logs II", "Mini-Quest - Gather Avo Logs", "Mini-Quest - Gather Avo Logs II", "Mini-Quest - Gather Avo Logs III", "Mini-Quest - Gather Avo Logs IV", "Mini-Quest - Gather Bamboo", "Mini-Quest - Gather Barley", "Mini-Quest - Gather Bass", "Mini-Quest - Gather Bass II", "Mini-Quest - Gather Bass III", "Mini-Quest - Gather Bass IV", "Mini-Quest - Gather Birch Logs", "Mini-Quest - Gather Carp", "Mini-Quest - Gather Carp II", "Mini-Quest - Gather Cobalt", "Mini-Quest - Gather Cobalt II", "Mini-Quest - Gather Cobalt III", "Mini-Quest - Gather Copper", "Mini-Quest - Gather Dark Logs", "Mini-Quest - Gather Dark Logs II", "Mini-Quest - Gather Dark Logs III", "Mini-Quest - Gather Decay Roots", "Mini-Quest - Gather Decay Roots II", "Mini-Quest - Gather Decay Roots III", "Mini-Quest - Gather Diamonds", "Mini-Quest - Gather Diamonds II", "Mini-Quest - Gather Diamonds III", "Mini-Quest - Gather Diamonds IV", "Mini-Quest - Gather Gold", "Mini-Quest - Gather Gold II", "Mini-Quest - Gather Granite", "Mini-Quest - Gather Gudgeon", "Mini-Quest - Gather Gylia Fish", "Mini-Quest - Gather Gylia Fish II", "Mini-Quest - Gather Gylia Fish III", "Mini-Quest - Gather Hops", "Mini-Quest - Gather Hops II", "Mini-Quest - Gather Icefish", "Mini-Quest - Gather Icefish II", "Mini-Quest - Gather Iron", "Mini-Quest - Gather Iron II", "Mini-Quest - Gather Jungle Logs", "Mini-Quest - Gather Jungle Logs II", "Mini-Quest - Gather Kanderstone", "Mini-Quest - Gather Kanderstone II", "Mini-Quest - Gather Kanderstone III", "Mini-Quest - Gather Koi", "Mini-Quest - Gather Koi II", "Mini-Quest - Gather Koi III", "Mini-Quest - Gather Light Logs", "Mini-Quest - Gather Light Logs II", "Mini-Quest - Gather Light Logs III", "Mini-Quest - Gather Malt", "Mini-Quest - Gather Malt II", "Mini-Quest - Gather Millet", "Mini-Quest - Gather Millet II", "Mini-Quest - Gather Millet III", "Mini-Quest - Gather Molten Eel", "Mini-Quest - Gather Molten Eel II", "Mini-Quest - Gather Molten Eel III", "Mini-Quest - Gather Molten Eel IV", "Mini-Quest - Gather Molten Ore", "Mini-Quest - Gather Molten Ore II", "Mini-Quest - Gather Molten Ore III", "Mini-Quest - Gather Molten Ore IV", "Mini-Quest - Gather Oak Logs", "Mini-Quest - Gather Oats", "Mini-Quest - Gather Oats II", "Mini-Quest - Gather Pine Logs", "Mini-Quest - Gather Pine Logs II", "Mini-Quest - Gather Pine Logs III", "Mini-Quest - Gather Piranhas", "Mini-Quest - Gather Piranhas II", "Mini-Quest - Gather Rice", "Mini-Quest - Gather Rice II", "Mini-Quest - Gather Rice III", "Mini-Quest - Gather Rice IV", "Mini-Quest - Gather Rye", "Mini-Quest - Gather Rye II", "Mini-Quest - Gather Salmon", "Mini-Quest - Gather Salmon II", "Mini-Quest - Gather Sandstone", "Mini-Quest - Gather Sandstone II", "Mini-Quest - Gather Silver", "Mini-Quest - Gather Silver II", "Mini-Quest - Gather Sorghum", "Mini-Quest - Gather Sorghum II", "Mini-Quest - Gather Sorghum III", "Mini-Quest - Gather Sorghum IV", "Mini-Quest - Gather Spruce Logs", "Mini-Quest - Gather Spruce Logs II", "Mini-Quest - Gather Trout", "Mini-Quest - Gather Wheat", "Mini-Quest - Gather Willow Logs", "Mini-Quest - Gather Willow Logs II", "Mini-Quest - Slay Ailuropodas", "Mini-Quest - Slay Angels", "Mini-Quest - Slay Astrochelys Manis", "Mini-Quest - Slay Azers", "Mini-Quest - Slay Conures", "Mini-Quest - Slay Coyotes", "Mini-Quest - Slay Creatures of Nesaak Forest", "Mini-Quest - Slay Creatures of the Void", "Mini-Quest - Slay Dead Villagers", "Mini-Quest - Slay Dragonlings", "Mini-Quest - Slay Felrocs", "Mini-Quest - Slay Frosted Guards & Cryostone Golems", "Mini-Quest - Slay Hobgoblins", "Mini-Quest - Slay Idols", "Mini-Quest - Slay Ifrits", "Mini-Quest - Slay Jinkos", "Mini-Quest - Slay Lizardmen", "Mini-Quest - Slay Magma Entities", "Mini-Quest - Slay Mooshrooms", "Mini-Quest - Slay Myconids", "Mini-Quest - Slay Orcs", "Mini-Quest - Slay Pernix Monkeys", "Mini-Quest - Slay Robots", "Mini-Quest - Slay Scarabs", "Mini-Quest - Slay Skeletons", "Mini-Quest - Slay Slimes", "Mini-Quest - Slay Spiders", "Mini-Quest - Slay Weirds", "Mini-Quest - Slay Wraiths & Phantasms", "Misadventure on the Sea", "Mixed Feelings", "Murder Mystery", "Mushroom Man", "One Thousand Meters Under", "Out of my Mind", "Pirate's Trove", "Pit of the Dead", "Point of No Return", "Poisoning the Pest", "Potion Making", "Purple and Blue", "Realm of Light I - The Worm Holes", "Realm of Light II - Taproot", "Realm of Light III - A Headless History", "Realm of Light IV - Finding the Light", "Realm of Light V - The Realm of Light", "Recipe For Disaster", "Reclaiming the House", "Recover the Past", "Redbeard's Booty", "Reincarnation", "Rise of the Quartron", "Royal Trials", "Shattered Minds", "Stable Story", "Star Thief", "Supply and Delivery", "Taking the Tower", "Temple of the Legends", "Tempo Town Trouble", "The Bigger Picture", "The Breaking Point", "The Canary Calls", "The Canyon Guides", "The Corrupted Village", "The Dark Descent", "The Envoy Part I", "The Envoy Part II", "The Feathers Fly Part I", "The Feathers Fly Part II", "The Hero of Gavel", "The Hidden City", "The House of Twain", "The Lost", "The Maiden Tower", "The Mercenary", "The Olmic Rune", "The Order of the Grook", "The Passage", "The Qira Hive", "The Sewers of Ragni", "The Shadow of the Beast", "The Thanos Depository", "The Ultimate Weapon", "Tower of Ascension", "Tribal Aggression", "Troubled Tribesmen", "Tunnel Trouble", "UndericeÀ", "Underwater", "Wrath of the Mummy", "WynnExcavation Site A", "WynnExcavation Site B", "WynnExcavation Site C", "WynnExcavation Site D", "Zhight Island");

    Identifier tabLeft = Identifier.of("wynnextras", "textures/gui/profileviewer/tableft.png");
    Identifier tabMid = Identifier.of("wynnextras", "textures/gui/profileviewer/tabmid.png");
    Identifier tagRight = Identifier.of("wynnextras", "textures/gui/profileviewer/tabright.png");

    Identifier NOTGTexture = Identifier.of("wynnextras", "textures/gui/profileviewer/rankingicons/notg.png");
    Identifier NOLTexture = Identifier.of("wynnextras", "textures/gui/profileviewer/rankingicons/nol.png");
    Identifier TCCTexture = Identifier.of("wynnextras", "textures/gui/profileviewer/rankingicons/tcc.png");
    Identifier TNATexture = Identifier.of("wynnextras", "textures/gui/profileviewer/rankingicons/tna.png");

    Identifier backgroundTexture = Identifier.of("wynnextras", "textures/gui/profileviewer/profileviewerbackground15.png");
    Identifier alsobackgroundTexture = Identifier.of("wynnextras", "textures/gui/profileviewer/alsoprofileviewerbackground.png");
    Identifier raidBackgroundTexture = Identifier.of("wynnextras", "textures/gui/profileviewer/raidbackground2.png");
    Identifier openInBrowserButtonTexture = Identifier.of("wynnextras", "textures/gui/profileviewer/openinbrowserbuttontexture4.png");
    Identifier openInBrowserButtonTextureW = Identifier.of("wynnextras", "textures/gui/profileviewer/openinbrowserbuttontexture4wide.png");
    Identifier classBackgroundTexture = Identifier.of("wynnextras", "textures/gui/profileviewer/classbackground2inactive.png");
    Identifier classBackgroundTextureGold = Identifier.of("wynnextras", "textures/gui/profileviewer/classbackground2inactivegold.png");
    Identifier classBackgroundTextureActive = Identifier.of("wynnextras", "textures/gui/profileviewer/classbackground2active.png");
    Identifier onlineCircleTexture = Identifier.of("wynnextras", "textures/gui/profileviewer/onlinecircle.png");
    Identifier offlineCircleTexture = Identifier.of("wynnextras", "textures/gui/profileviewer/offlinecircle.png");
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
    Identifier dungeonBackgroundTexture = Identifier.of("wynnextras", "textures/gui/profileviewer/dungeons/dungeonpagebackground2.png");

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
    Identifier profBackgroundTexture = Identifier.of("wynnextras", "textures/gui/profileviewer/profs/profbackground4.png");

    Identifier questBackgroundTexture = Identifier.of("wynnextras", "textures/gui/profileviewer/quests/questbackground.png");
    Identifier questBackgroundBorderTexture = Identifier.of("wynnextras", "textures/gui/profileviewer/quests/questbackgroundborders.png");
    Identifier questSearchbarTexture = Identifier.of("wynnextras", "textures/gui/profileviewer/quests/questsearchbar.png");

    Identifier warsCompletionTexture = Identifier.of("wynnextras", "textures/gui/profileviewer/rankingicons/warscompletion.png");
    Identifier playerContentTexture = Identifier.of("wynnextras", "textures/gui/profileviewer/rankingicons/playercontent.png");
    Identifier globalPlayerContent = Identifier.of("wynnextras", "textures/gui/profileviewer/rankingicons/globalplayercontent.png");
    Identifier combatLevelTexture = Identifier.of("wynnextras", "textures/gui/profileviewer/rankingicons/combatlevel.png");
    Identifier totalLevelTexture = Identifier.of("wynnextras", "textures/gui/profileviewer/rankingicons/totallevel.png");
    Identifier professionLevelTexture = Identifier.of("wynnextras", "textures/gui/profileviewer/rankingicons/professionlevel.png");
    Identifier rankingBackgroundTexture = Identifier.of("wynnextras", "textures/gui/profileviewer/rankingicons/rankingbackground.png");
    Identifier rankingBackgroundWideTexture = Identifier.of("wynnextras", "textures/gui/profileviewer/rankingicons/rankingbackgroundwide.png");

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
                scrollOffset -= 10; //Scroll up
            } else {
                scrollOffset += 10; //Scroll down
            }
            if(scrollOffset < 0) {
                scrollOffset = 0;
            }
        });
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        if(openInBrowserButton == null && PV.currentPlayerData != null) {
            openInBrowserButton = new OpenInBroserButton(-1, -1, 20, 87, "https://wynncraft.com/stats/player/" + PV.currentPlayerData.getUuid());
        }

        if(searchBar == null && PV.currentPlayerData != null) {
            searchBar = new Searchbar(-1, -1, 14, 100);
            searchBar.setInput(PV.currentPlayerData.getUsername());
        }

        int screenWidth = MinecraftClient.getInstance().getWindow().getScaledWidth();
        int screenHeight = MinecraftClient.getInstance().getWindow().getScaledHeight();
        int width = 600;
        int height = 250;
        int xStart = screenWidth / 2 - width / 2;
        int yStart = screenHeight / 2 - height / 2;
        PVScreen.mouseX = mouseX;
        PVScreen.mouseY = mouseY;

        if(questSearchBar == null && PV.currentPlayerData != null) {
            questSearchBar = new Searchbar(xStart + 200, yStart + height + 7, 14, 400);
            questSearchBar.setSearchText("Search...");
        }

        RenderUtils.drawRect(context.getMatrices(), CustomColor.fromInt(-804253680), 0, 0, 0, MinecraftClient.getInstance().currentScreen.width, MinecraftClient.getInstance().currentScreen.height);
        if(currentTab == Tab.General) {
            RenderUtils.drawTexturedRect(context.getMatrices(), backgroundTexture, xStart, yStart, width, height, width, height);
        } else {
            RenderUtils.drawTexturedRect(context.getMatrices(), alsobackgroundTexture, xStart, yStart, width, height, width, height);
        }
        if(PV.currentPlayerData != null) {
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
                int signWidth = drawDynamicNameSign(context, tabString, xStart + 8 + totalXOffset, yStart - 19);
                float centerX = xStart + 8 + totalXOffset + (float) signWidth / 2;
                float textX = centerX - (float) textRenderer.getWidth(tabString) / 2;

                context.drawText(MinecraftClient.getInstance().textRenderer, tabString, (int) textX, yStart - 12, tabStringColor.asInt(), true);

                tabButton.setX(xStart + 8 + totalXOffset);
                tabButton.setY(yStart - 19);
                tabButton.setWidth(signWidth);
                tabButton.setHeight(20);

                totalXOffset += signWidth + 4;
                j++;
            }

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
                        RenderUtils.drawTexturedRect(context.getMatrices(), rankBadge, xStart + 5, yStart + 6, (float) rankBadgeWidth / 2, 9, rankBadgeWidth / 2, 9);
                    }
                    context.drawText(MinecraftClient.getInstance().textRenderer, " " + PV.currentPlayerData.getUsername(), xStart + 5 + rankBadgeWidth / 2, yStart + 7, CustomColor.fromHexString(rankColorHexString).asInt(), true);

                    if (PV.currentPlayerData.isOnline()) {
                        RenderUtils.drawTexturedRect(context.getMatrices(), onlineCircleTexture, xStart + 5, yStart + 20, 11, 11, 11, 11);
                        context.drawText(MinecraftClient.getInstance().textRenderer, PV.currentPlayerData.getServer(), xStart + 19, yStart + 22, CustomColor.fromHexString("FFFFFF").asInt(), true);
                    } else {
                        RenderUtils.drawTexturedRect(context.getMatrices(), offlineCircleTexture, xStart + 5, yStart + 20, 11, 11, 11, 11);
                        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm");
                        String formatted;
                        if (PV.currentPlayerData.getLastJoin() == null) {
                            formatted = "Unknown!";
                        } else {
                            formatted = PV.currentPlayerData.getLastJoin().format(formatter);
                        }
                        context.drawText(MinecraftClient.getInstance().textRenderer, "Last seen: " + formatted, xStart + 19, yStart + 22, CustomColor.fromHexString("FFFFFF").asInt(), true);
                    }
                    if (dummy != null) {
                        if (InputUtil.isKeyPressed(MinecraftClient.getInstance().getWindow().getHandle(), InputUtil.GLFW_KEY_LEFT_SHIFT)) {
                            dummy.setPose(EntityPose.CROUCHING);
                            drawPlayer(context, xStart + 22 + 72, yStart + 34 + 129, 70, mouseX, mouseY, dummy); //166 178
                        } else if (InputUtil.isKeyPressed(MinecraftClient.getInstance().getWindow().getHandle(), InputUtil.GLFW_KEY_RIGHT_SHIFT)) {
                            dummy.setPose(EntityPose.SLEEPING);
                            drawPlayer(context, xStart + 22 + 10, yStart + 34 + 119, 70, mouseX, mouseY, dummy); //166 178
                        } else {
                            dummy.setPose(EntityPose.STANDING);
                            drawPlayer(context, xStart + 22 + 72, yStart + 34 + 138, 70, mouseX, mouseY, dummy); //166 178
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
                            //System.out.println(entry.getValue().getType());
                            Identifier classTexture;
                            if(entry.getLevel() == 106) {
                                classTexture = getGoldClassTexture(entry.getType());
                            } else {
                                 classTexture = getClassTexture(entry.getType());
                            }

                            int entryX = xStart + 192 + 137 * (i % 3);
                            int entryY = yStart + 5 + 48 * Math.floorDiv(i, 3);
                            characterButtons.get(i).setCharacter(entry);
                            characterButtons.get(i).setX(entryX);
                            characterButtons.get(i).setY(entryY);
                            characterButtons.get(i).setWidth(130);
                            characterButtons.get(i).setHeight(44);
                            //characterButtons.get(i).draw(context);
                            if(selectedCharacter == entry) {
                                RenderUtils.drawTexturedRect(context.getMatrices(), classBackgroundTextureActive, entryX, entryY, 130, 44, 130, 44);
                            } else if(entry.getTotalLevel() != 1690) {
                                RenderUtils.drawTexturedRect(context.getMatrices(), classBackgroundTexture, entryX, entryY, 130, 44, 130, 44);
                            } else {
                                RenderUtils.drawTexturedRect(context.getMatrices(), classBackgroundTextureGold, entryX, entryY, 130, 44, 130, 44);
                            }


                            //context.drawText(MinecraftClient.getInstance().textRenderer, entry.getValue().getType(), entryX, entryY, CustomColor.fromHexString("FFFFFF").asInt(), true);
                            if (classTexture != null) {
                                int level = entry.getLevel();
                                int totalLevel = entry.getTotalLevel();
                                CustomColor levelColor;
                                if (entry.getContentCompletion() == 1133) {
                                    levelColor = CommonColors.RAINBOW;
                                } else {
                                    levelColor = CustomColor.fromHexString("FFFFFF");
                                }

                                RenderUtils.drawTexturedRect(context.getMatrices(), classTexture, entryX + 4, entryY + 4, 30, 34, 30, 34);
                                FontRenderer.getInstance().renderText(context.getMatrices(), StyledText.fromComponent(Text.of(getClassName(entry))), (float) entryX + 37, (float) entryY + 6, levelColor, HorizontalAlignment.LEFT, VerticalAlignment.TOP, TextShadow.NORMAL, 0.7f);
                                FontRenderer.getInstance().renderText(context.getMatrices(), StyledText.fromComponent(Text.of("Level " + level)), (float) entryX + 37, (float) entryY + 14, levelColor, HorizontalAlignment.LEFT, VerticalAlignment.TOP, TextShadow.NORMAL, 0.7f);
                                FontRenderer.getInstance().renderText(context.getMatrices(), StyledText.fromComponent(Text.of("Total Level " + totalLevel)), (float) entryX + 37, (float) entryY + 22, levelColor, HorizontalAlignment.LEFT, VerticalAlignment.TOP, TextShadow.NORMAL, 0.7f);
                                FontRenderer.getInstance().renderText(context.getMatrices(), StyledText.fromComponent(Text.of("Completion " + (entry.getContentCompletion() * 100/1133) + "%")), (float) entryX + 37, (float) entryY + 30, levelColor, HorizontalAlignment.LEFT, VerticalAlignment.TOP, TextShadow.NORMAL, 0.7f);

                                //context.drawText(MinecraftClient.getInstance().textRenderer, getClassName(entry), entryX + 37, entryY + 4, levelColor.asInt(), true);
                                //context.drawText(MinecraftClient.getInstance().textRenderer, "Level " + level, entryX + 37, entryY + 14, levelColor.asInt(), true);
                                //context.drawText(MinecraftClient.getInstance().textRenderer, "Total Level " + totalLevel, entryX + 37, entryY + 24, levelColor.asInt(), true);
                                //context.drawText(MinecraftClient.getInstance().textRenderer, "Completion " + (entry.getContentCompletion() * 100/1133) + "%", entryX + 37, entryY + 34, levelColor.asInt(), true);
                            }
                            i++;
                        }
                    } else {
                        context.drawText(MinecraftClient.getInstance().textRenderer, "This player has their classes private.", xStart + 300, yStart + 115, CustomColor.fromHexString("ff0000").asInt(), true);
                    }

                    if (PV.currentPlayerData.getGuild() != null) {
                        String guildString = "[" + PV.currentPlayerData.getGuild().getPrefix() + "] " + PV.currentPlayerData.getGuild().getName();
                        String rankString = PV.currentPlayerData.getGuild().getRankStars() + " " + PV.currentPlayerData.getGuild().getRank() + " of " + PV.currentPlayerData.getGuild().getRankStars();
                        context.drawText(MinecraftClient.getInstance().textRenderer, rankString, xStart + 5 + 180 / 2 - MinecraftClient.getInstance().textRenderer.getWidth(rankString) / 2, yStart + 185, CustomColor.fromHexString("00FFFF").asInt(), true);
                        context.drawText(MinecraftClient.getInstance().textRenderer, guildString, xStart + 5 + 180 / 2 - MinecraftClient.getInstance().textRenderer.getWidth(guildString) / 2, yStart + 195, CustomColor.fromHexString("FFFFFF").asInt(), true);
                    }

                    if (PV.currentPlayerData.getPlaytime() != 0) {
                        context.drawText(MinecraftClient.getInstance().textRenderer, "Total Playtime: " + Math.round(PV.currentPlayerData.getPlaytime()) + "h", xStart + 5 + 180 / 2 - textRenderer.getWidth("Total Playtime: " + Math.round(PV.currentPlayerData.getPlaytime()) + "h") / 2, yStart + 215, CustomColor.fromHexString("FFFFFF").asInt(), true);
                    }

                    if(selectedCharacter != null) {
                        if(selectedCharacter.getPlaytime() != 0) {
                            context.drawText(MinecraftClient.getInstance().textRenderer, "Class Playtime: " + Math.round(selectedCharacter.getPlaytime()) + "h", xStart + 5 + 180 / 2 - textRenderer.getWidth("Class Playtime: " + Math.round(selectedCharacter.getPlaytime()) + "h") / 2, yStart + 225, CustomColor.fromHexString("FFFFFF").asInt(), true);
                        }
                    }

                    //System.out.println(WETeam + PV.currentPlayerData.getUsername());
                    if(WETeam.contains(PV.currentPlayerData.getUsername())) {
                        context.drawText(MinecraftClient.getInstance().textRenderer, "★★★ WynnExtras Team Member ★★★", xStart + 5 + 180 / 2 - textRenderer.getWidth("★★★ WynnExtras Team Member ★★★") / 2, yStart + 235, 61460, true);

                    }

//                    System.out.println(PV.currentPlayerData.getGuild().getRankStars());

                    if (PV.currentPlayerData.getFirstJoin() != null) {
                        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd.MM.yyyy");
                        String formatted = "First joined: ";
                        formatted += PV.currentPlayerData.getFirstJoin().format(formatter);
                        context.drawText(MinecraftClient.getInstance().textRenderer, formatted, xStart + 5 + 180 / 2 - textRenderer.getWidth(formatted) / 2, yStart + 205, CustomColor.fromHexString("FFFFFF").asInt(), true);
                    }
                }
                case Raids -> {
                    if(PV.currentPlayerData.getGlobalData() == null) {
                        FontRenderer.getInstance().renderText(context.getMatrices(), StyledText.fromComponent(Text.of("This player has their raid stats private.")), (float) xStart + 300, (float) yStart + 115, CustomColor.fromHexString("ff0000"), HorizontalAlignment.CENTER, VerticalAlignment.MIDDLE, TextShadow.NORMAL, 1.5f);
                        break;
                    }

                    RenderUtils.drawTexturedRect(context.getMatrices(), raidBackgroundTexture, xStart + 10, yStart + 30, 275, 100, 275, 100);
                    RenderUtils.drawTexturedRect(context.getMatrices(), NOTGTexture, xStart + 10, yStart + 30, 100, 100, 100, 100);
                    RenderUtils.drawTexturedRect(context.getMatrices(), raidBackgroundTexture, xStart + 325 - 10, yStart + 30, 275, 100, 275, 100);
                    RenderUtils.drawTexturedRect(context.getMatrices(), TCCTexture, xStart + 500 - 10, yStart + 30, 100, 100, 100, 100);
                    RenderUtils.drawTexturedRect(context.getMatrices(), raidBackgroundTexture, xStart + 10, yStart + 150 - 10, 275, 100, 275, 100);
                    RenderUtils.drawTexturedRect(context.getMatrices(), NOLTexture, xStart + 10, yStart + 150 - 10, 100, 100, 100, 100);
                    RenderUtils.drawTexturedRect(context.getMatrices(), raidBackgroundTexture, xStart + 325 - 10, yStart + 150 - 10, 275, 100, 275, 100);
                    RenderUtils.drawTexturedRect(context.getMatrices(), TNATexture, xStart + 500 - 10, yStart + 150 - 10, 100, 100, 100, 100);

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
                            FontRenderer.getInstance().renderText(context.getMatrices(), StyledText.fromComponent(Text.of("Rank #" + NOTGRank)), xStart + 115, yStart + 85, notgColor, HorizontalAlignment.LEFT, VerticalAlignment.TOP, TextShadow.NORMAL, 1.3f);
                            //context.drawText(MinecraftClient.getInstance().textRenderer, "Rank #" + NOTGRank, xStart + 55, yStart + 42, notgColor.asInt(), true);
                        }
                        if(NOLRank != -1) {
                            FontRenderer.getInstance().renderText(context.getMatrices(), StyledText.fromComponent(Text.of("Rank #" + NOLRank)), xStart + 115, yStart + 195, nolColor, HorizontalAlignment.LEFT, VerticalAlignment.TOP, TextShadow.NORMAL, 1.3f);
                            //context.drawText(MinecraftClient.getInstance().textRenderer, "Rank #" + NOLRank, xStart + 55, yStart + 127, nolColor.asInt(), true);
                        }
                        if(TCCRank != -1) {
                            FontRenderer.getInstance().renderText(context.getMatrices(), StyledText.fromComponent(Text.of("Rank #" + TCCRank)), xStart + 490, yStart + 85, tccColor, HorizontalAlignment.RIGHT, VerticalAlignment.TOP, TextShadow.NORMAL, 1.3f);
                            //context.drawText(MinecraftClient.getInstance().textRenderer, "Rank #" + TCCRank, xStart + 240 + textRenderer.getWidth("The Canyon Colossus") - textRenderer.getWidth("Rank #" + TCCRank), yStart + 42, tccColor.asInt(), true);
                        }
                        if(TNARank != -1) {
                            FontRenderer.getInstance().renderText(context.getMatrices(), StyledText.fromComponent(Text.of("Rank #" + TNARank)), xStart + 490, yStart + 195, tnaColor, HorizontalAlignment.RIGHT, VerticalAlignment.TOP, TextShadow.NORMAL, 1.3f);
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

                    FontRenderer.getInstance().renderText(context.getMatrices(), StyledText.fromComponent(Text.of("Nest of the Grootslangs")), xStart + 115, yStart + 55, notgColor, HorizontalAlignment.LEFT, VerticalAlignment.TOP, TextShadow.NORMAL, 1.3f);
                    //context.drawText(MinecraftClient.getInstance().textRenderer, "Nest of the Grootslangs", xStart + 55, yStart + 22, notgColor.asInt(), true);
                    FontRenderer.getInstance().renderText(context.getMatrices(), StyledText.fromComponent(Text.of("Orphion's Nexus of Light")), xStart + 115, yStart + 165, nolColor, HorizontalAlignment.LEFT, VerticalAlignment.TOP, TextShadow.NORMAL, 1.3f);
                    //context.drawText(MinecraftClient.getInstance().textRenderer, "Orphion's Nexus of Light", xStart + 55, yStart + 107, nolColor.asInt(), true);
                    FontRenderer.getInstance().renderText(context.getMatrices(), StyledText.fromComponent(Text.of("The Canyon Colossus")), xStart + 490, yStart + 55, tccColor, HorizontalAlignment.RIGHT, VerticalAlignment.TOP, TextShadow.NORMAL, 1.3f);
                    //context.drawText(MinecraftClient.getInstance().textRenderer, "The Canyon Colossus", xStart + 240, yStart + 22, tccColor.asInt(), true);
                    FontRenderer.getInstance().renderText(context.getMatrices(), StyledText.fromComponent(Text.of("The Nameless Anomaly")), xStart + 490, yStart + 165, tnaColor, HorizontalAlignment.RIGHT, VerticalAlignment.TOP, TextShadow.NORMAL, 1.3f);
                    //context.drawText(MinecraftClient.getInstance().textRenderer, "The Nameless Anomaly", xStart + 238, yStart + 107, tnaColor.asInt(), true);

                    if(raids != null) {
                        long NOTGComps = raids.getList().getOrDefault("Nest of the Grootslangs", 0);
                        long NOLComps = raids.getList().getOrDefault("Orphion's Nexus of Light", 0);
                        long TCCComps = raids.getList().getOrDefault("The Canyon Colossus", 0);
                        long TNAComps = raids.getList().getOrDefault("The Nameless Anomaly", 0);
                        long TotalComps = raids.getTotal();

                        FontRenderer.getInstance().renderText(context.getMatrices(), StyledText.fromComponent(Text.of(NOTGComps + " Completions")), xStart + 115, yStart + 70, notgColor, HorizontalAlignment.LEFT, VerticalAlignment.TOP, TextShadow.NORMAL, 1.3f);

                        FontRenderer.getInstance().renderText(context.getMatrices(), StyledText.fromComponent(Text.of(NOLComps + " Completions")), xStart + 115, yStart + 180, nolColor, HorizontalAlignment.LEFT, VerticalAlignment.TOP, TextShadow.NORMAL, 1.3f);

                        FontRenderer.getInstance().renderText(context.getMatrices(), StyledText.fromComponent(Text.of(TCCComps + " Completions")), xStart + 490, yStart + 70, tccColor, HorizontalAlignment.RIGHT, VerticalAlignment.TOP, TextShadow.NORMAL, 1.3f);

                        FontRenderer.getInstance().renderText(context.getMatrices(), StyledText.fromComponent(Text.of(TNAComps + " Completions")), xStart + 490, yStart + 180, tnaColor, HorizontalAlignment.RIGHT, VerticalAlignment.TOP, TextShadow.NORMAL, 1.3f);

                        if(TotalComps > 0) {
                            FontRenderer.getInstance().renderText(context.getMatrices(), StyledText.fromComponent(Text.of("Total Completions" + characterNameString + TotalComps)), xStart + 300, yStart + 10, CustomColor.fromHexString("FFFFFF"), HorizontalAlignment.CENTER, VerticalAlignment.TOP, TextShadow.NORMAL, 1.3f);
                            //context.drawText(MinecraftClient.getInstance().textRenderer, "Total Completions: " + TotalComps, xStart + 400 / 2 - textRenderer.getWidth("Total Completions: " + TotalComps) / 2, yStart - 16, CustomColor.fromHexString("FFFFFF").asInt(), true);
                        }
                    }

                }
                case Rankings -> {
                    Map<String, Long> rankings = PV.currentPlayerData.getRanking();
                    if(rankings == null) {
                        FontRenderer.getInstance().renderText(context.getMatrices(), StyledText.fromComponent(Text.of("This player has their Rankings private.")), (float) xStart + 300, (float) yStart + 115, CustomColor.fromHexString("ff0000"), HorizontalAlignment.CENTER, VerticalAlignment.MIDDLE, TextShadow.NORMAL, 1.5f);
                        break;
                    }
                    //RenderUtils.drawTexturedRect(context.getMatrices(), dungeonBackgroundTexture, xStart + 10, yStart + 10, 580, 230, 580, 230);

                    for(int i = 0; i < 20; i++) {
                        if(i == 15) continue;
                        if(i > 18) continue;
                        int xPos = xStart + 10 + (145 * (i % 4));
                        int yPos = yStart + 10 + (46 * Math.floorDiv(i, 4));

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
                            RenderUtils.drawTexturedRect(context.getMatrices(), rankingBackgroundTexture, xPos, yPos, 140, 42, 140, 42);
                            RenderUtils.drawTexturedRect(context.getMatrices(), texture, xPos + 4, yPos + 6, 30, 30, 30, 30);
                            FontRenderer.getInstance().renderText(context.getMatrices(), StyledText.fromComponent(Text.of(text)), xPos + 37, yPos + 12, textColor, HorizontalAlignment.LEFT, VerticalAlignment.TOP, TextShadow.NORMAL, 1f);
                            FontRenderer.getInstance().renderText(context.getMatrices(), StyledText.fromComponent(Text.of("#" + globalPlacementString)), xPos + 37, yPos + 22, textColor, HorizontalAlignment.LEFT, VerticalAlignment.TOP, TextShadow.NORMAL, 1f);
                        } else if(i < 16){
                            xPos += (50 * (i % 3));

                            RenderUtils.drawTexturedRect(context.getMatrices(), rankingBackgroundWideTexture, xPos, yPos, 190, 42, 190, 42);
                            RenderUtils.drawTexturedRect(context.getMatrices(), texture, xPos + 4, yPos + 6, 30, 30, 30, 30);
                            FontRenderer.getInstance().renderText(context.getMatrices(), StyledText.fromComponent(Text.of(text)), xPos + 37, yPos + 12, textColor, HorizontalAlignment.LEFT, VerticalAlignment.TOP, TextShadow.NORMAL, 1f);
                            FontRenderer.getInstance().renderText(context.getMatrices(), StyledText.fromComponent(Text.of("#" + globalPlacementString)), xPos + 37, yPos + 22, textColor, HorizontalAlignment.LEFT, VerticalAlignment.TOP, TextShadow.NORMAL, 1f);

                        } else {
                            xPos += (50 * ((i - 1) % 3));
                            RenderUtils.drawTexturedRect(context.getMatrices(), rankingBackgroundWideTexture, xPos, yPos, 190, 42, 190, 42);
                            RenderUtils.drawTexturedRect(context.getMatrices(), texture, xPos + 4, yPos + 6, 30, 30, 30, 30);
                            FontRenderer.getInstance().renderText(context.getMatrices(), StyledText.fromComponent(Text.of(text)), xPos + 37, yPos + 7, textColor, HorizontalAlignment.LEFT, VerticalAlignment.TOP, TextShadow.NORMAL, 1f);
                            FontRenderer.getInstance().renderText(context.getMatrices(), StyledText.fromComponent(Text.of("Global #" + globalPlacementString)), xPos + 37, yPos + 17, textColor, HorizontalAlignment.LEFT, VerticalAlignment.TOP, TextShadow.NORMAL, 1f);
                            if(i >= 16) {
                                FontRenderer.getInstance().renderText(context.getMatrices(), StyledText.fromComponent(Text.of("Solo #" + soloPlacementString)), xPos + 37, yPos + 27, textColor, HorizontalAlignment.LEFT, VerticalAlignment.TOP, TextShadow.NORMAL, 1f);
                            }}
                    }
                }
                case Professions -> {
                    if(selectedCharacter == null) {
                        FontRenderer.getInstance().renderText(context.getMatrices(), StyledText.fromComponent(Text.of("Select a character to view professions.")), (float) xStart + 300, (float) yStart + 115, CustomColor.fromHexString("ff0000"), HorizontalAlignment.CENTER, VerticalAlignment.MIDDLE, TextShadow.NORMAL, 1.5f);
                        break;
                    }
                    Map<String, Profession> profs = selectedCharacter.getProfessions();
                    if(profs == null) {
                        FontRenderer.getInstance().renderText(context.getMatrices(), StyledText.fromComponent(Text.of("This player has their profession stats private.")), (float) xStart + 300, (float) yStart + 115, CustomColor.fromHexString("ff0000"), HorizontalAlignment.CENTER, VerticalAlignment.MIDDLE, TextShadow.NORMAL, 1.5f);
                        break;
                    }

                    RenderUtils.drawTexturedRect(context.getMatrices(), profBackgroundTexture, xStart + 10, yStart + 10, 580, 230, 580, 230);

                    int i = 0;
                    for(Map.Entry<String, Profession> prof : profs.entrySet()) {
                        Identifier profTexture = getProfTexture(prof.getKey());
                        if(i < 4) {
                            int level = prof.getValue().getLevel();
                            CustomColor levelColor;
                            RenderUtils.drawTexturedRect(context.getMatrices(), profTexture, xStart + 70 + i * 136, yStart + 20, 64, 64, 64, 64);
                            if(level == 132) {
                                levelColor = CommonColors.RAINBOW;
                            } else if (level >= 110) {
                                levelColor = CommonColors.YELLOW;
                            } else {
                                levelColor = CustomColor.fromHexString("FFFFFF");
                            }
                            FontRenderer.getInstance().renderText(context.getMatrices(), StyledText.fromComponent(Text.of("Level " + level)), xStart + 32 + 70 + i * 136, yStart + 90, levelColor, HorizontalAlignment.CENTER, VerticalAlignment.TOP, TextShadow.NORMAL, 2f);
                            if(level < 132) {
                                FontRenderer.getInstance().renderText(context.getMatrices(), StyledText.fromComponent(Text.of("Progress to next Level: " + prof.getValue().getXpPercent() + "%")), xStart + 32 + 70 + i * 136, yStart + 110, levelColor, HorizontalAlignment.CENTER, VerticalAlignment.TOP, TextShadow.NORMAL, 0.8f);
                            }
                        } else {
                            int level = prof.getValue().getLevel();
                            CustomColor levelColor;
                            RenderUtils.drawTexturedRect(context.getMatrices(), profTexture, xStart + 44 + (i - 4) * 68, yStart + 200, 32, 32, 32, 32);
                            if(level == 132) {
                                levelColor = CommonColors.RAINBOW;
                            } else if (level >= 103) {
                                levelColor = CommonColors.YELLOW;
                            } else {
                                levelColor = CustomColor.fromHexString("FFFFFF");
                            }
                            FontRenderer.getInstance().renderText(context.getMatrices(), StyledText.fromComponent(Text.of("Level " + level)), xStart + 44 + (i - 4) * 68 + 16, yStart + 187, levelColor, HorizontalAlignment.CENTER, VerticalAlignment.TOP, TextShadow.NORMAL, 1.2f);
                            if(level < 132) {
                                FontRenderer.getInstance().renderText(context.getMatrices(), StyledText.fromComponent(Text.of("Progress to")), xStart + 44 + (i - 4) * 68 + 16, yStart + 170, levelColor, HorizontalAlignment.CENTER, VerticalAlignment.TOP, TextShadow.NORMAL, 0.8f);
                                FontRenderer.getInstance().renderText(context.getMatrices(), StyledText.fromComponent(Text.of("next Level: " + prof.getValue().getXpPercent() + "%")), xStart + 44 + (i - 4) * 68 + 16, yStart + 178, levelColor, HorizontalAlignment.CENTER, VerticalAlignment.TOP, TextShadow.NORMAL, 0.8f);
                            }
                        }

                        i++;
                    }
                }
                case Dungeons -> {
                    if(PV.currentPlayerData.getGlobalData() == null) {
                        FontRenderer.getInstance().renderText(context.getMatrices(), StyledText.fromComponent(Text.of("This player has their dungeon stats private.")), (float) xStart + 300, (float) yStart + 115, CustomColor.fromHexString("ff0000"), HorizontalAlignment.CENTER, VerticalAlignment.MIDDLE, TextShadow.NORMAL, 1.5f);
                        break;
                    }
                    if (PV.currentPlayerData.getGlobalData().getDungeons() == null) {
                        FontRenderer.getInstance().renderText(context.getMatrices(), StyledText.fromComponent(Text.of("This player has their dungeon stats private.")), (float) xStart + 300, (float) yStart + 115, CustomColor.fromHexString("ff0000"), HorizontalAlignment.CENTER, VerticalAlignment.MIDDLE, TextShadow.NORMAL, 1.5f);
                        break;
                    }

                    Map<String, Integer> normalComps = new HashMap<>();
                    Map<String, Integer> corruptedComps = new HashMap<>();

                    Dungeons dungeons;
                    if(selectedCharacter == null) {
                        dungeons = PV.currentPlayerData.getGlobalData().getDungeons();
                    } else {
                        if(selectedCharacter.getDungeons() != null) {
                            dungeons = selectedCharacter.getDungeons();
                        } else dungeons = PV.currentPlayerData.getGlobalData().getDungeons();
                    }

                    for (Map.Entry<String, Integer> entry : dungeons.getList().entrySet()) {
                        if (entry.getKey().contains("Corrupted")) {
                            corruptedComps.put(entry.getKey(), entry.getValue());
                        } else {
                            normalComps.put(entry.getKey(), entry.getValue());
                        }
                    }


                    RenderUtils.drawTexturedRect(context.getMatrices(), dungeonBackgroundTexture, xStart + 10, yStart + 10, 580, 230, 580, 230);


                    int i = 0;
                    for(Identifier dungeon : dungeonTextures) {
                        int comps = getDungeonComps(i, normalComps);
                        int cComps = getCorruptedComps(i, corruptedComps);
                        int dungeonY = yStart + 11 + Math.floorDiv(i, 5) * 145;
                        int dungeonX = xStart + 30 + 115 * (i % 5);
                        if(Math.floorDiv(i, 5) > 0) {
                            RenderUtils.drawTexturedRect(context.getMatrices(), dungeon, dungeonX + 5, dungeonY - 5, 70, 70, 70, 70);
                            FontRenderer.getInstance().renderText(context.getMatrices(), StyledText.fromComponent(Text.of(getDungeonName(i))), dungeonX + 40, dungeonY + 70, CustomColor.fromHexString("FFFFFF"), HorizontalAlignment.CENTER, VerticalAlignment.TOP, TextShadow.NORMAL);

                            if(i < 8) {
                                RenderUtils.drawTexturedRect(context.getMatrices(), dungeonKeyTexture, dungeonX + 20, dungeonY - 25, 20, 20, 20, 20);
                                FontRenderer.getInstance().renderText(context.getMatrices(), StyledText.fromComponent(Text.of(String.valueOf(comps))), dungeonX + 20, dungeonY - 20, CustomColor.fromHexString("FFFFFF"), HorizontalAlignment.RIGHT, VerticalAlignment.TOP, TextShadow.NORMAL);
                            } else {
                                RenderUtils.drawTexturedRect(context.getMatrices(), dungeonKeyTexture, dungeonX + 30, dungeonY - 25, 20, 20, 20, 20);
                                FontRenderer.getInstance().renderText(context.getMatrices(), StyledText.fromComponent(Text.of(String.valueOf(comps))), dungeonX + 30, dungeonY - 20, CustomColor.fromHexString("FFFFFF"), HorizontalAlignment.RIGHT, VerticalAlignment.TOP, TextShadow.NORMAL);
                            }

                            if(i < 8) {
                                RenderUtils.drawTexturedRect(context.getMatrices(), corruptedDungeonKeyTexture, dungeonX + 40, dungeonY - 25, 20, 20, 20, 20);
                                FontRenderer.getInstance().renderText(context.getMatrices(), StyledText.fromComponent(Text.of(String.valueOf(cComps))), dungeonX + 62, dungeonY - 20, CustomColor.fromHexString("FFFFFF"), HorizontalAlignment.LEFT, VerticalAlignment.TOP, TextShadow.NORMAL);
                            }
                        } else {
                            RenderUtils.drawTexturedRect(context.getMatrices(), dungeon, dungeonX + 5, dungeonY + 15, 70, 70, 70, 70);
                            FontRenderer.getInstance().renderText(context.getMatrices(), StyledText.fromComponent(Text.of(getDungeonName(i))), dungeonX + 40, dungeonY + 5, CustomColor.fromHexString("FFFFFF"), HorizontalAlignment.CENTER, VerticalAlignment.TOP, TextShadow.NORMAL);


                            RenderUtils.drawTexturedRect(context.getMatrices(), dungeonKeyTexture, dungeonX + 20, dungeonY + 85, 20, 20, 20, 20);
                            FontRenderer.getInstance().renderText(context.getMatrices(), StyledText.fromComponent(Text.of(String.valueOf(comps))), dungeonX + 20, dungeonY + 90, CustomColor.fromHexString("FFFFFF"), HorizontalAlignment.RIGHT, VerticalAlignment.TOP, TextShadow.NORMAL);

                            if(i < 8) {
                                RenderUtils.drawTexturedRect(context.getMatrices(), corruptedDungeonKeyTexture, dungeonX + 40, dungeonY + 85, 20, 20, 20, 20);
                                FontRenderer.getInstance().renderText(context.getMatrices(), StyledText.fromComponent(Text.of(String.valueOf(cComps))), dungeonX + 62, dungeonY + 90, CustomColor.fromHexString("FFFFFF"), HorizontalAlignment.LEFT, VerticalAlignment.TOP, TextShadow.NORMAL);
                            }
                        }
                        i++;
                    }
                }
                case Quests -> {
                    if(selectedCharacter == null) {
                        FontRenderer.getInstance().renderText(context.getMatrices(), StyledText.fromComponent(Text.of("Select a character to view quests.")), (float) xStart + 300, (float) yStart + 115, CustomColor.fromHexString("ff0000"), HorizontalAlignment.CENTER, VerticalAlignment.MIDDLE, TextShadow.NORMAL, 1.5f);
                        break;
                    }
                    List<String> quests = selectedCharacter.getQuests();
                    if(quests == null) {
                        FontRenderer.getInstance().renderText(context.getMatrices(), StyledText.fromComponent(Text.of("This player has their quest stats private.")), (float) xStart + 300, (float) yStart + 115, CustomColor.fromHexString("ff0000"), HorizontalAlignment.CENTER, VerticalAlignment.MIDDLE, TextShadow.NORMAL, 1.5f);
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


                    RenderUtils.drawTexturedRect(context.getMatrices(), questBackgroundTexture, xStart + 10, yStart + 30, 580, 200, 580, 200);
                    RenderUtils.drawTexturedRect(context.getMatrices(), questSearchbarTexture, xStart + 200, yStart + height, 400, 20, 400, 20);

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
                        int yPos = yStart + 38 + Math.floorDiv(i, 2) * 12 - scrollOffset;
                        if(yPos > 328) break;
                        if(yPos > yStart + 20) {
                            HorizontalAlignment horizontalAlignment = HorizontalAlignment.LEFT;
                            int rightOffset = 0;
                            if (i % 2 == 1) {
                                rightOffset = 560;
                                horizontalAlignment = HorizontalAlignment.RIGHT;
                            }

                            FontRenderer.getInstance().renderText(context.getMatrices(), StyledText.fromComponent(Text.of(quest)), (float) xStart + 20 + rightOffset, (float) yPos, textColor, horizontalAlignment, VerticalAlignment.TOP, TextShadow.NORMAL, 1f);
                        }
                        i++;
                    }

                    {
                        int yPos = yStart + 20 + 38 + Math.floorDiv(i, 2) * 12 - scrollOffset;
                        if (yPos > yStart + 20 && yPos < 328) {
                            FontRenderer.getInstance().renderText(context.getMatrices(), StyledText.fromComponent(Text.of("Missing:")), (float) xStart + 20, (float) yPos, textColor, HorizontalAlignment.LEFT, VerticalAlignment.TOP, TextShadow.NORMAL, 2f);
                            if(i % 2 == 0) i+= 2;
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
                        int yPos = yStart + 30 + 38 + Math.floorDiv(i, 2) * 12 - scrollOffset;
                        if(yPos > 328) break;
                        if(yPos > yStart + 20) {
                            HorizontalAlignment horizontalAlignment = HorizontalAlignment.LEFT;
                            int rightOffset = 0;
                            if (i % 2 == 1) {
                                rightOffset = 560;
                                horizontalAlignment = HorizontalAlignment.RIGHT;
                            }

                            FontRenderer.getInstance().renderText(context.getMatrices(), StyledText.fromComponent(Text.of(quest)), (float) xStart + 20 + rightOffset, (float) yPos, textColor, horizontalAlignment, VerticalAlignment.TOP, TextShadow.NORMAL, 1f);
                        }
                        i++;
                    }


                    RenderUtils.drawTexturedRect(context.getMatrices(), questBackgroundBorderTexture, xStart + 10, yStart + 20, 580, 220, 580, 220);
                    FontRenderer.getInstance().renderText(context.getMatrices(), StyledText.fromComponent(Text.of(titleString)), (float) xStart + 300, (float) yStart + 10, textColor, HorizontalAlignment.CENTER, VerticalAlignment.TOP, TextShadow.NORMAL, 1.5f);

                }
                case Misc -> {
                    Global data = PV.currentPlayerData.getGlobalData();
                    if(data == null) {
                        FontRenderer.getInstance().renderText(context.getMatrices(), StyledText.fromComponent(Text.of("This player has their misc stats private.")), (float) xStart + 300, (float) yStart + 115, CustomColor.fromHexString("ff0000"), HorizontalAlignment.CENTER, VerticalAlignment.MIDDLE, TextShadow.NORMAL, 1.5f);
                        break;
                    }

                    RenderUtils.drawTexturedRect(context.getMatrices(), dungeonBackgroundTexture, xStart + 10, yStart + 10, 580, 230, 580, 230);
                    FontRenderer.getInstance().renderText(context.getMatrices(), StyledText.fromComponent(Text.of("Wars completed: " + data.getWars())), (float) xStart + 20, (float) yStart + 20, CustomColor.fromHexString("FFFFFF"), HorizontalAlignment.LEFT, VerticalAlignment.TOP, TextShadow.NORMAL, 2f);
                    FontRenderer.getInstance().renderText(context.getMatrices(), StyledText.fromComponent(Text.of("Dungeons completed: " + data.getDungeons().getTotal())), (float) xStart + 20, (float) yStart + 40, CustomColor.fromHexString("FFFFFF"), HorizontalAlignment.LEFT, VerticalAlignment.TOP, TextShadow.NORMAL, 2f);
                    FontRenderer.getInstance().renderText(context.getMatrices(), StyledText.fromComponent(Text.of("Unique Caves completed: " + data.getCaves())), (float) xStart + 20, (float) yStart + 60, CustomColor.fromHexString("FFFFFF"), HorizontalAlignment.LEFT, VerticalAlignment.TOP, TextShadow.NORMAL, 2f);
                    FontRenderer.getInstance().renderText(context.getMatrices(), StyledText.fromComponent(Text.of("Unique Lootrun camps completed: " + data.getLootruns())), (float) xStart + 20, (float) yStart + 80, CustomColor.fromHexString("FFFFFF"), HorizontalAlignment.LEFT, VerticalAlignment.TOP, TextShadow.NORMAL, 2f);
                    FontRenderer.getInstance().renderText(context.getMatrices(), StyledText.fromComponent(Text.of("Unique World events completed: " + data.getWorldEvents())), (float) xStart + 20, (float) yStart + 100, CustomColor.fromHexString("FFFFFF"), HorizontalAlignment.LEFT, VerticalAlignment.TOP, TextShadow.NORMAL, 2f);
                    FontRenderer.getInstance().renderText(context.getMatrices(), StyledText.fromComponent(Text.of("Chests opened: " + data.getChestsFound())), (float) xStart + 20, (float) yStart + 120, CustomColor.fromHexString("FFFFFF"), HorizontalAlignment.LEFT, VerticalAlignment.TOP, TextShadow.NORMAL, 2f);
                    FontRenderer.getInstance().renderText(context.getMatrices(), StyledText.fromComponent(Text.of("Mobs killed: " + data.getMobsKilled())), (float) xStart + 20, (float) yStart + 140, CustomColor.fromHexString("FFFFFF"), HorizontalAlignment.LEFT, VerticalAlignment.TOP, TextShadow.NORMAL, 2f);
                    FontRenderer.getInstance().renderText(context.getMatrices(), StyledText.fromComponent(Text.of("Pvp kills: " + data.getPvp().getKills())), (float) xStart + 20, (float) yStart + 160, CustomColor.fromHexString("FFFFFF"), HorizontalAlignment.LEFT, VerticalAlignment.TOP, TextShadow.NORMAL, 2f);
                    FontRenderer.getInstance().renderText(context.getMatrices(), StyledText.fromComponent(Text.of("Pvp deaths: " + data.getPvp().getDeaths())), (float) xStart + 20, (float) yStart + 180, CustomColor.fromHexString("FFFFFF"), HorizontalAlignment.LEFT, VerticalAlignment.TOP, TextShadow.NORMAL, 2f);

                }
            }

            if (openInBrowserButton != null) {
                openInBrowserButton.setX(xStart);
                openInBrowserButton.setY(yStart + height);
                openInBrowserButton.buttonText = "Open in browser";
                openInBrowserButton.drawWithTexture(context, openInBrowserButtonTexture);
            }

            RenderUtils.drawTexturedRect(context.getMatrices(), openInBrowserButtonTextureW, xStart + 89, yStart + height, 100, 20, 100, 20);

            if (searchBar != null) {
                searchBar.setX(xStart + 89);
                searchBar.setY(yStart + height + 7);
                searchBar.drawWithoutBackground(context, CustomColor.fromHexString("FFFFFF"));
            }



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
            case VIP -> 44;
            case VIPPLUS -> 58;
            case HERO -> 62;
            case HEROPLUS -> 76;
            case CHAMPION -> 106;
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
            sleepOffsetX = 60;
            sleepOffsetY = 10;
        } else {
            sleepOffsetX = 0;
            sleepOffsetY = 0;
        }

        if(PV.currentPlayer.equalsIgnoreCase("teslanator")) {
            rotation.rotateX((float) Math.PI);
            flipOffset = -130;
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
        int screenWidth = MinecraftClient.getInstance().getWindow().getScaledWidth();
        int screenHeight = MinecraftClient.getInstance().getWindow().getScaledHeight();
        int width = 600;
        int height = 250;
        int xStart = screenWidth / 2 - width / 2;
        int yStart = screenHeight / 2 - height / 2;
        int dragX = xStart + 23;
        int dragY = yStart + 33;
        int dragWidth = 143;
        int dragHeight = 143;

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
        RenderUtils.drawTexturedRect(context.getMatrices(), tabLeft, x, y, 10, 20, 10, 20);
        for (int i = 0; i < amount; i++) {
            RenderUtils.drawTexturedRect(context.getMatrices(), tabMid, x + 10 + 10 * i, y, 10, 20, 10, 20);
        }
        RenderUtils.drawTexturedRect(context.getMatrices(), tagRight, x + 10 + 10 * amount, y, 10, 20, 10, 20);
        return 20 + amount * 10;
    }

    //TODO: Use API Key + add ability to rotate skin
}
