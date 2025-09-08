package julianh06.wynnextras.features.profileviewer;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.authlib.GameProfile;
import com.mojang.authlib.properties.Property;
import com.wynntils.utils.colors.CustomColor;
import com.wynntils.utils.render.RenderUtils;
import julianh06.wynnextras.annotations.WEModule;
import julianh06.wynnextras.features.profileviewer.data.CharacterData;
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

    Identifier tabLeft = Identifier.of("wynnextras", "textures/gui/profileviewer/tableft.png");
    Identifier tabMid = Identifier.of("wynnextras", "textures/gui/profileviewer/tabmid.png");
    Identifier tagRight = Identifier.of("wynnextras", "textures/gui/profileviewer/tabright.png");

    Identifier backgroundTexture = Identifier.of("wynnextras", "textures/gui/profileviewer/profileviewerbackground15.png");
    Identifier tabBackgroundTexture = Identifier.of("wynnextras", "textures/gui/profileviewer/tabbackground2.png");
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
    Identifier shamanTexture = Identifier.of("wynnextras", "textures/gui/profileviewer/classes/shaman.png");
    Identifier mageTexture = Identifier.of("wynnextras", "textures/gui/profileviewer/classes/mage.png");
    Identifier assassinTexture = Identifier.of("wynnextras", "textures/gui/profileviewer/classes/assassin.png");
    Identifier archerTexture = Identifier.of("wynnextras", "textures/gui/profileviewer/classes/archer.png");

    static OpenInBroserButton openInBrowserButton;
    public static Searchbar searchBar;

    Tab currentTab = Tab.General;

    String player;
    public static AbstractClientPlayerEntity dummy;

    public PVScreen(String player) {
        super(Text.of("Player Viewer"));
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
        RenderUtils.drawTexturedRect(context.getMatrices(), backgroundTexture, xStart, yStart, width, height, width, height);
        if(PV.currentPlayerData != null) {
            Identifier rankBadge = getRankBadge();
            int rankBadgeWidth = getRankBadgeWidth();
            String rankColorHexString;
            if(PV.currentPlayerData.getLegacyRankColour() != null) {
                rankColorHexString = PV.currentPlayerData.getLegacyRankColour().getMain();
            } else {
                rankColorHexString = "AAAAAA";
            }
            if(rankBadge != null) {
                RenderUtils.drawTexturedRect(context.getMatrices(), rankBadge, xStart + 5, yStart + 6, (float) rankBadgeWidth / 2, 9, rankBadgeWidth / 2, 9);
            }
            context.drawText(MinecraftClient.getInstance().textRenderer, " " + PV.currentPlayerData.getUsername(), xStart + 5 + rankBadgeWidth / 2, yStart + 7, CustomColor.fromHexString(rankColorHexString).asInt(), true);

            if(PV.currentPlayerData.isOnline()) {
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
            if(dummy != null) {
                if(InputUtil.isKeyPressed(MinecraftClient.getInstance().getWindow().getHandle(), InputUtil.GLFW_KEY_LEFT_SHIFT)) {
                    dummy.setPose(EntityPose.CROUCHING);
                    drawPlayer(context, xStart + 22 + 72, yStart + 34 + 129, 70, mouseX, mouseY, dummy); //166 178
                }else if(InputUtil.isKeyPressed(MinecraftClient.getInstance().getWindow().getHandle(), InputUtil.GLFW_KEY_RIGHT_SHIFT)) {
                    dummy.setPose(EntityPose.SLEEPING);
                    drawPlayer(context, xStart + 22 + 10, yStart + 34 + 119, 70, mouseX, mouseY, dummy); //166 178
                } else {
                    dummy.setPose(EntityPose.STANDING);
                    drawPlayer(context, xStart + 22 + 72, yStart + 34 + 138, 70, mouseX, mouseY, dummy); //166 178
                }
            }

            if(PV.currentPlayerData.getCharacters() != null) {
                int i = 0;
                Map<String, CharacterData> map = PV.currentPlayerData.getCharacters();
                List<CharacterData> sortedCharacterList = new ArrayList<>(map.values());

                sortedCharacterList.sort(
                        Comparator.comparing(CharacterData::getTotalLevel).thenComparing(CharacterData::getPlaytime)
                );

                for(CharacterData entry : sortedCharacterList.reversed()) {
                    //System.out.println(entry.getValue().getType());
                    Identifier classTexture = getClassTexture(entry.getType());

                    int entryX = xStart + 192 + 137 * (i % 3);
                    int entryY = yStart + 5 + 48 * Math.floorDiv(i, 3);
                    RenderUtils.drawTexturedRect(context.getMatrices(), classBackgroundTexture, entryX, entryY, 130, 44, 130, 44);
                    //context.drawText(MinecraftClient.getInstance().textRenderer, entry.getValue().getType(), entryX, entryY, CustomColor.fromHexString("FFFFFF").asInt(), true);
                    if(classTexture != null) {
                        RenderUtils.drawTexturedRect(context.getMatrices(), classTexture, entryX + 4, entryY + 4, 30, 34, 30, 34);
                        context.drawText(MinecraftClient.getInstance().textRenderer, getClassName(entry), entryX + 37, entryY + 12, CustomColor.fromHexString("FFFFFF").asInt(), true);

                        CustomColor levelColor;

                        context.drawText(MinecraftClient.getInstance().textRenderer, "Level " + entry.getLevel(), entryX + 37, entryY + 23, CustomColor.fromHexString("FFFFFF").asInt(), true);
                    }
                    i++;
                }
            } else {
                context.drawText(MinecraftClient.getInstance().textRenderer, "This player has their classes private.", xStart + 300, yStart + 115, CustomColor.fromHexString("ff0000").asInt(), true);
            }

            if(PV.currentPlayerData.getGuild() != null) {
                String guildString = "[" + PV.currentPlayerData.getGuild().getPrefix() + "] " + PV.currentPlayerData.getGuild().getName();
                String rankString = PV.currentPlayerData.getGuild().getRankStars() + " " + PV.currentPlayerData.getGuild().getRank() + " of " + PV.currentPlayerData.getGuild().getRankStars();
                context.drawText(MinecraftClient.getInstance().textRenderer, rankString , xStart + 5 + 180 / 2 - MinecraftClient.getInstance().textRenderer.getWidth(rankString) / 2, yStart + 185, CustomColor.fromHexString("00FFFF").asInt(), true);
                context.drawText(MinecraftClient.getInstance().textRenderer, guildString, xStart + 5 + 180 / 2 - MinecraftClient.getInstance().textRenderer.getWidth(guildString) / 2, yStart + 195, CustomColor.fromHexString("FFFFFF").asInt(), true);
            }

            if(PV.currentPlayerData.getPlaytime() != 0) {
                context.drawText(MinecraftClient.getInstance().textRenderer, "Playtime: " + PV.currentPlayerData.getPlaytime() + "h", xStart + 5 + 180 / 2 - textRenderer.getWidth("Playtime: " + PV.currentPlayerData.getPlaytime() + "h") / 2, yStart + 205, CustomColor.fromHexString("FFFFFF").asInt(), true);
            }

            if(PV.currentPlayerData.getFirstJoin() != null) {
                DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd.MM.yyyy");
                String formatted = "First joined: ";
                formatted += PV.currentPlayerData.getFirstJoin().format(formatter);
                context.drawText(MinecraftClient.getInstance().textRenderer, formatted, xStart + 5 + 180 / 2 - textRenderer.getWidth(formatted) / 2, yStart + 215, CustomColor.fromHexString("FFFFFF").asInt(), true);
            }

            //TODO: first join

            if(openInBrowserButton != null) {
                openInBrowserButton.setX(xStart);
                openInBrowserButton.setY(yStart + height);
                openInBrowserButton.buttonText = "Open in browser";
                openInBrowserButton.drawWithTexture(context, openInBrowserButtonTexture);
            }

            RenderUtils.drawTexturedRect(context.getMatrices(), openInBrowserButtonTextureW, xStart + 89, yStart + height, 100, 20, 100, 20);

            if(searchBar != null) {
                searchBar.setX(xStart + 89);
                searchBar.setY(yStart + height + 7);
                searchBar.drawWithoutBackground(context, CustomColor.fromHexString("FFFFFF"));
            }



            int j = 0;
            int totalXOffset = 0;
            for(Tab tab : Tab.values()) {
                CustomColor tabStringColor;
                if(tab.equals(currentTab)) {
                    tabStringColor = CustomColor.fromHexString("FFFF00");
                } else {
                    tabStringColor = CustomColor.fromHexString("FFFFFF");
                }
                String tabString = tab.toString();
                int signWidth = drawDynamicNameSign(context, tabString, xStart + 8 + totalXOffset, yStart - 19);
                float centerX = xStart + 8 + totalXOffset + (float) signWidth /2;
                float textX = centerX - (float) textRenderer.getWidth(tabString) /2;

                context.drawText(MinecraftClient.getInstance().textRenderer, tabString, (int) textX, yStart - 12, tabStringColor.asInt(), true);

                totalXOffset += signWidth + 4;
                j++;
            }

//                RenderUtils.drawTexturedRect(context.getMatrices(), tabBackgroundTexture, xStart + 8, yStart - 19, 40, 20, 40, 20);
                //context.drawText(MinecraftClient.getInstance().textRenderer, "General", xStart + 8 + 3, yStart - 11, CustomColor.fromHexString("FFFFFF").asInt(), true);
//                RenderUtils.drawTexturedRect(context.getMatrices(), tabBackgroundTexture, xStart + 50, yStart - 19, 40, 20, 40, 20);
                //context.drawText(MinecraftClient.getInstance().textRenderer, "Raid", xStart + 50 + 6, yStart - 13, CustomColor.fromHexString("FFFFFF").asInt(), true);
//                RenderUtils.drawTexturedRect(context.getMatrices(), tabBackgroundTexture, xStart + 92, yStart - 19, 40, 20, 40, 20);
               //context.drawText(MinecraftClient.getInstance().textRenderer, "War", xStart + 92 + 6, yStart - 13, CustomColor.fromHexString("FFFFFF").asInt(), true);
//                RenderUtils.drawTexturedRect(context.getMatrices(), tabBackgroundTexture, xStart + 134, yStart - 19, 40, 20, 40, 20);


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
    }

    public int drawDynamicNameSign(DrawContext context, String input, int x, int y) {
        TextRenderer textRenderer = MinecraftClient.getInstance().textRenderer;
        int strWidth = textRenderer.getWidth(input);
        int strMidWidth = strWidth - 15;
        int amount = Math.max(0, Math.ceilDiv(strMidWidth, 10));
        RenderUtils.drawTexturedRect(context.getMatrices(), tabLeft, x, y, 15, 20, 15, 20);
        for (int i = 0; i < amount; i++) {
            RenderUtils.drawTexturedRect(context.getMatrices(), tabMid, x + 15 + 10 * i, y, 10, 20, 10, 20);
        }
        RenderUtils.drawTexturedRect(context.getMatrices(), tagRight, x + 15 + 10 * amount, y, 15, 20, 15, 20);
        return 30 + amount * 10;
    }
}
