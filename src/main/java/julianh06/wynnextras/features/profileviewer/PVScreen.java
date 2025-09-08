package julianh06.wynnextras.features.profileviewer;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.authlib.GameProfile;
import com.mojang.authlib.properties.Property;
import com.wynntils.utils.colors.CommonColors;
import com.wynntils.utils.colors.CustomColor;
import com.wynntils.utils.render.RenderUtils;
import julianh06.wynnextras.annotations.WEModule;
import julianh06.wynnextras.features.profileviewer.data.CharacterData;
import julianh06.wynnextras.features.profileviewer.data.Raids;
import julianh06.wynnextras.features.profileviewer.data.Ranking;
import julianh06.wynnextras.utils.overlays.EasyButton;
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
import org.apache.logging.log4j.core.jackson.MapEntry;
import org.joml.Quaternionf;


import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.CompletableFuture;

import static net.minecraft.client.gui.screen.ingame.InventoryScreen.drawEntity;

@WEModule
public class PVScreen extends Screen {
    static int mouseX = 0;
    static int mouseY = 0;

    public enum Rank {NONE, VIP, VIPPLUS, HERO, HEROPLUS, CHAMPION}

    public enum Tab {General, Raids, Rankings, Professions, Dungeons, Wars, Quests, Skilltree, Misc}
    public static List<TabButton> tabButtons = new ArrayList<>();

    Identifier tabLeft = Identifier.of("wynnextras", "textures/gui/profileviewer/tableft.png");
    Identifier tabMid = Identifier.of("wynnextras", "textures/gui/profileviewer/tabmid.png");
    Identifier tagRight = Identifier.of("wynnextras", "textures/gui/profileviewer/tabright.png");

    Identifier NOTGTexture = Identifier.of("wynnextras", "textures/gui/profileviewer/raidicons/nestofthegrootslangs-small.png");
    Identifier NOLTexture = Identifier.of("wynnextras", "textures/gui/profileviewer/raidicons/orphionsnexusoflight-small.png");
    Identifier TCCTexture = Identifier.of("wynnextras", "textures/gui/profileviewer/raidicons/thecanyoncolossus-small.png");
    Identifier TNATexture = Identifier.of("wynnextras", "textures/gui/profileviewer/raidicons/thenamelessanomaly-small.png");

    Identifier backgroundTexture = Identifier.of("wynnextras", "textures/gui/profileviewer/profileviewerbackground15.png");
    Identifier alsobackgroundTexture = Identifier.of("wynnextras", "textures/gui/profileviewer/alsoprofileviewerbackground.png");
    Identifier raidBackgroundTexture = Identifier.of("wynnextras", "textures/gui/profileviewer/raidbackground2.png");
    Identifier openInBrowserButtonTexture = Identifier.of("wynnextras", "textures/gui/profileviewer/openinbrowserbuttontexture4.png");
    Identifier openInBrowserButtonTextureW = Identifier.of("wynnextras", "textures/gui/profileviewer/openinbrowserbuttontexture4wide.png");
    Identifier classBackgroundTexture = Identifier.of("wynnextras", "textures/gui/profileviewer/classbackground2inactive.png");
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

    static OpenInBroserButton openInBrowserButton;
    public static Searchbar searchBar;

    public static Tab currentTab = Tab.General;

    String player;
    public static AbstractClientPlayerEntity dummy;

    public PVScreen(String player) {
        super(Text.of("Player Viewer"));
        tabButtons.clear();
        for(Tab tab : Tab.values()) {
            tabButtons.add(new TabButton(0, 0, 0, 0, tab));
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

        RenderUtils.drawRect(context.getMatrices(), CustomColor.fromInt(-804253680), 0, 0, 0, MinecraftClient.getInstance().currentScreen.width, MinecraftClient.getInstance().currentScreen.height);
        if(currentTab == Tab.General) {
            RenderUtils.drawTexturedRect(context.getMatrices(), backgroundTexture, xStart, yStart, width, height, width, height);
        } else {
            RenderUtils.drawTexturedRect(context.getMatrices(), alsobackgroundTexture, xStart, yStart, width, height, width, height);
        }
        if(PV.currentPlayerData != null) {
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
                                Comparator.comparing(CharacterData::getLevel).thenComparing(CharacterData::getTotalLevel).thenComparing(CharacterData::getPlaytime)
                        );

                        for (CharacterData entry : sortedCharacterList.reversed()) {
                            //System.out.println(entry.getValue().getType());
                            Identifier classTexture;
                            if(entry.getTotalLevel() == 1690) {
                                classTexture = getGoldClassTexture(entry.getType());
                            } else {
                                 classTexture = getClassTexture(entry.getType());
                            }

                            int entryX = xStart + 192 + 137 * (i % 3);
                            int entryY = yStart + 5 + 48 * Math.floorDiv(i, 3);
                            RenderUtils.drawTexturedRect(context.getMatrices(), classBackgroundTexture, entryX, entryY, 130, 44, 130, 44);
                            //context.drawText(MinecraftClient.getInstance().textRenderer, entry.getValue().getType(), entryX, entryY, CustomColor.fromHexString("FFFFFF").asInt(), true);
                            if (classTexture != null) {
                                int level = entry.getLevel();
                                int totalLevel = entry.getTotalLevel();
                                CustomColor levelColor;
                                if (level == 106) {
                                    levelColor = CommonColors.RAINBOW;
                                } else if (level == 105) {
                                    levelColor = CommonColors.YELLOW;
                                } else {
                                    levelColor = CustomColor.fromHexString("FFFFFF");
                                }

                                RenderUtils.drawTexturedRect(context.getMatrices(), classTexture, entryX + 4, entryY + 4, 30, 34, 30, 34);
                                context.drawText(MinecraftClient.getInstance().textRenderer, getClassName(entry), entryX + 37, entryY + 8, levelColor.asInt(), true);
                                context.drawText(MinecraftClient.getInstance().textRenderer, "Level " + level, entryX + 37, entryY + 18, levelColor.asInt(), true);
                                context.drawText(MinecraftClient.getInstance().textRenderer, "Total Level " + totalLevel, entryX + 37, entryY + 28, levelColor.asInt(), true);
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
                        context.drawText(MinecraftClient.getInstance().textRenderer, "Playtime: " + PV.currentPlayerData.getPlaytime() + "h", xStart + 5 + 180 / 2 - textRenderer.getWidth("Playtime: " + PV.currentPlayerData.getPlaytime() + "h") / 2, yStart + 205, CustomColor.fromHexString("FFFFFF").asInt(), true);
                    }

                    if (PV.currentPlayerData.getFirstJoin() != null) {
                        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd.MM.yyyy");
                        String formatted = "First joined: ";
                        formatted += PV.currentPlayerData.getFirstJoin().format(formatter);
                        context.drawText(MinecraftClient.getInstance().textRenderer, formatted, xStart + 5 + 180 / 2 - textRenderer.getWidth(formatted) / 2, yStart + 215, CustomColor.fromHexString("FFFFFF").asInt(), true);
                    }
                }
                case Raids -> {
                    if(PV.currentPlayerData.getGlobalData() == null) {
                        context.drawText(MinecraftClient.getInstance().textRenderer, "This player has their raid stats private.", xStart + 200, yStart + 115, CustomColor.fromHexString("ff0000").asInt(), true);
                    } else {
                        RenderUtils.drawTexturedRect(context.getMatrices(), raidBackgroundTexture, xStart + 10, yStart + 30, 275, 100, 275, 100);
                        RenderUtils.drawTexturedRect(context.getMatrices(), NOTGTexture, xStart + 10, yStart + 30, 100, 100, 100, 100);
                        RenderUtils.drawTexturedRect(context.getMatrices(), raidBackgroundTexture, xStart + 325 - 10, yStart + 30, 275, 100, 275, 100);
                        RenderUtils.drawTexturedRect(context.getMatrices(), TCCTexture, xStart + 500 - 10, yStart + 30, 100, 100, 100, 100);
                        RenderUtils.drawTexturedRect(context.getMatrices(), raidBackgroundTexture, xStart + 10, yStart + 150 - 10, 275, 100, 275, 100);
                        RenderUtils.drawTexturedRect(context.getMatrices(), NOLTexture, xStart + 10, yStart + 150 - 10, 100, 100, 100, 100);
                        RenderUtils.drawTexturedRect(context.getMatrices(), raidBackgroundTexture, xStart + 325 - 10, yStart + 150 - 10, 275, 100, 275, 100);
                        RenderUtils.drawTexturedRect(context.getMatrices(), TNATexture, xStart + 500 - 10, yStart + 150 - 10, 100, 100, 100, 100);


                        context.getMatrices().push();
                        context.getMatrices().scale(1.3f, 1.3f, 1.3f);
                        Raids raids = PV.currentPlayerData.getGlobalData().getRaids();

                        context.drawText(MinecraftClient.getInstance().textRenderer, "Nest of the Grootslangs", xStart + 55, yStart + 22, CustomColor.fromHexString("FFFFFF").asInt(), true);
                        context.drawText(MinecraftClient.getInstance().textRenderer, "Orphion's Nexus of Light", xStart + 55, yStart + 107, CustomColor.fromHexString("FFFFFF").asInt(), true);
                        context.drawText(MinecraftClient.getInstance().textRenderer, "The Canyon Colossus", xStart + 240, yStart + 22, CustomColor.fromHexString("FFFFFF").asInt(), true);
                        context.drawText(MinecraftClient.getInstance().textRenderer, "The Nameless Anomaly", xStart + 238, yStart + 107, CustomColor.fromHexString("FFFFFF").asInt(), true);

                        if(raids != null) {
                            long NOTGComps = raids.getList().getOrDefault("Nest of the Grootslangs", -1);
                            long NOLComps = raids.getList().getOrDefault("Orphion's Nexus of Light", -1);
                            long TCCComps = raids.getList().getOrDefault("The Canyon Colossus", -1);
                            long TNAComps = raids.getList().getOrDefault("The Nameless Anomaly", -1);
                            long TotalComps = raids.getTotal();
                            if(NOTGComps != -1) {
                                context.drawText(MinecraftClient.getInstance().textRenderer, NOTGComps + " Completions", xStart + 55, yStart + 32, CustomColor.fromHexString("FFFFFF").asInt(), true);
                            }
                            if(NOLComps != -1) {
                                context.drawText(MinecraftClient.getInstance().textRenderer, NOLComps + " Completions", xStart + 55, yStart + 117, CustomColor.fromHexString("FFFFFF").asInt(), true);
                            }
                            if(NOTGComps != -1) {
                                context.drawText(MinecraftClient.getInstance().textRenderer, TCCComps + " Completions", xStart + 240 + textRenderer.getWidth("The Canyon Colossus") - textRenderer.getWidth(TCCComps + " Completions"), yStart + 32, CustomColor.fromHexString("FFFFFF").asInt(), true);
                            }
                            if(NOTGComps != -1) {
                                context.drawText(MinecraftClient.getInstance().textRenderer, TNAComps + " Completions", xStart + 238 + textRenderer.getWidth("The Nameless Anomaly") - textRenderer.getWidth(TNAComps + " Completions"), yStart + 117, CustomColor.fromHexString("FFFFFF").asInt(), true);
                            }
                            if(TotalComps > 0) {
                                context.drawText(MinecraftClient.getInstance().textRenderer, "Total Completions: " + TotalComps, xStart + 400 / 2 - textRenderer.getWidth("Total Completions: " + TotalComps) / 2, yStart - 16, CustomColor.fromHexString("FFFFFF").asInt(), true);
                                //System.out.println(textRenderer.getWidth("Total Completions: " + TotalComps + 10000)); //
                            }
                        }
                        Map<String, Long> ranking = PV.currentPlayerData.getRanking();
                        if(ranking != null) {
                            long NOTGRank = ranking.getOrDefault("grootslangCompletion", -1L);
                            long NOLRank = ranking.getOrDefault("orphionCompletion", -1L);
                            long TCCRank = ranking.getOrDefault("colossusCompletion", -1L);
                            long TNARank = ranking.getOrDefault("namelessCompletion", -1L);
                            if(NOTGRank != -1) {
                                context.drawText(MinecraftClient.getInstance().textRenderer, "Rank #" + NOTGRank, xStart + 55, yStart + 42, CustomColor.fromHexString("FFFFFF").asInt(), true);
                            }
                            if(NOLRank != -1) {
                                context.drawText(MinecraftClient.getInstance().textRenderer, "Rank #" + NOLRank, xStart + 55, yStart + 127, CustomColor.fromHexString("FFFFFF").asInt(), true);
                            }
                            if(TCCRank != -1) {
                                context.drawText(MinecraftClient.getInstance().textRenderer, "Rank #" + TCCRank, xStart + 240 + textRenderer.getWidth("The Canyon Colossus") - textRenderer.getWidth("Rank #" + TCCRank), yStart + 42, CustomColor.fromHexString("FFFFFF").asInt(), true);
                            }
                            if(TNARank != -1) {
                                context.drawText(MinecraftClient.getInstance().textRenderer, "Rank #" + TNARank, xStart + 238 + textRenderer.getWidth("The Nameless Anomaly") - textRenderer.getWidth("Rank #" + TNARank), yStart + 127, CustomColor.fromHexString("FFFFFF").asInt(), true);
                            }
                        }
                        context.getMatrices().pop();
                    }
                }
                case Dungeons -> {
                    if(PV.currentPlayerData.getGlobalData() == null) {
                        context.drawText(MinecraftClient.getInstance().textRenderer, "This player has their dungeon stats private.", xStart + 200, yStart + 115, CustomColor.fromHexString("ff0000").asInt(), true);
                    } else {
                        if (PV.currentPlayerData.getGlobalData().getDungeons() == null) {
                            context.drawText(MinecraftClient.getInstance().textRenderer, "This player has their dungeon stats private.", xStart + 200, yStart + 115, CustomColor.fromHexString("ff0000").asInt(), true);
                        } else {
                            Map<String, Integer> normalComps = PV.currentPlayerData.getGlobalData().getDungeons().getList();
                            Map<String, Integer> corruptedComps;
                            int i = 0;
                            int j = 0;
                            for(Map.Entry<String, Integer> entry : PV.currentPlayerData.getGlobalData().getDungeons().getList().entrySet()) {
                                if(entry.getKey().contains("Corrupted")) {
                                    context.drawText(MinecraftClient.getInstance().textRenderer, entry.getKey() + ": " + entry.getValue() + " completions", xStart, yStart + 10 * i, CommonColors.GRADIENT_2.asInt(), true);

                                    i++;
                                } else {
                                    //context.drawText(MinecraftClient.getInstance().textRenderer, "Rank #" + TNARank, xStart + 238 + textRenderer.getWidth("The Nameless Anomaly") - textRenderer.getWidth("Rank #" + TNARank), yStart + 127, CustomColor.fromHexString("FFFFFF").asInt(), true);
                                    context.drawText(MinecraftClient.getInstance().textRenderer, entry.getKey() + ": " + entry.getValue() + " completions", xStart + 600 - textRenderer.getWidth(entry.getKey() + ": " + entry.getValue() + " completions"), yStart + 10 * j, CustomColor.fromHexString("FFFFFF").asInt(), true);
                                    j++;
                                }
                            }
                        }
                    }
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


            int j = 0;
            int totalXOffset = 0;
            for (Tab tab : Tab.values()) {
                EasyButton tabButton = tabButtons.get(j);
                CustomColor tabStringColor;
                if (tab.equals(currentTab)) {
                    tabStringColor = CustomColor.fromHexString("FFFF00");
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
        }
    }

    public String getClassName(CharacterData entry) {
        if (entry.getNickname() != null) {
            return "*§o" + entry.getNickname();
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

    public static void drawPlayer(
            DrawContext context,
            int x, int y, int scale,
            float mouseX, float mouseY,
            LivingEntity player
    ) {
        float yaw = mouseX - x;
        float pitch = mouseY - y;

        Quaternionf rotation = new Quaternionf().rotateZ((float) Math.PI);
        //rotation.rotateX((float) Math.toRadians(pitch));
        rotation.rotateY((float) Math.toRadians(-20));

        EntityRenderDispatcher dispatcher = MinecraftClient.getInstance().getEntityRenderDispatcher();
        dispatcher.setRenderShadows(false);

        context.getMatrices().push();
        context.getMatrices().translate(x, y, 50.0);
        context.getMatrices().scale(scale, scale, scale);
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
        if(openInBrowserButton == null || searchBar == null) return;
        if(openInBrowserButton.isClickInBounds(PVScreen.mouseX, PVScreen.mouseY)) {
            openInBrowserButton.click();
        }
        if(searchBar.isClickInBounds(PVScreen.mouseX, PVScreen.mouseY)) {
            searchBar.click();
        } else {
            searchBar.setActive(false);
        }
        for(EasyButton button : tabButtons) {
            if(button.isClickInBounds(PVScreen.mouseX, PVScreen.mouseY)) {
                button.click();
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
}
