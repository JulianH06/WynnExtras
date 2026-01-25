package julianh06.wynnextras.features.profileviewer.tabs;

import com.wynntils.core.components.Handlers;
import com.wynntils.core.persisted.config.Config;
import com.wynntils.core.text.StyledText;
import com.wynntils.features.inventory.ItemHighlightFeature;
import com.wynntils.handlers.item.ItemAnnotation;
import com.wynntils.handlers.item.ItemHandler;
import com.wynntils.mc.extension.ItemStackExtension;
import com.wynntils.models.items.WynnItem;
import com.wynntils.utils.colors.CustomColor;
import com.wynntils.utils.mc.McUtils;
import com.wynntils.utils.render.RenderUtils;
import com.wynntils.utils.render.Texture;
import julianh06.wynnextras.config.WynnExtrasConfig;
import julianh06.wynnextras.features.profileviewer.PV;
import julianh06.wynnextras.features.profileviewer.PVScreen;
import julianh06.wynnextras.features.profileviewer.WynncraftApiHandler;
import julianh06.wynnextras.features.profileviewer.data.ApiAspect;
import julianh06.wynnextras.features.profileviewer.data.Aspect;
import julianh06.wynnextras.features.profileviewer.data.User;
import julianh06.wynnextras.mixin.Invoker.ItemHandlerInvoker;
import julianh06.wynnextras.utils.UI.UIUtils;
import julianh06.wynnextras.utils.UI.Widget;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.CustomModelDataComponent;
import net.minecraft.component.type.LoreComponent;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.registry.Registries;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.MutableText;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.text.TextColor;
import net.minecraft.util.Identifier;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static julianh06.wynnextras.features.profileviewer.PV.openedAspectPage;

public class AspectsTabWidget extends PVScreen.TabWidget{
    static Identifier xpbarborder = Identifier.of("wynnextras", "textures/gui/guildviewer/xpbarborder.png");
    static Identifier xpbarborder_dark = Identifier.of("wynnextras", "textures/gui/guildviewer/xpbarborder_dark.png");
    static Identifier xpbarbackground = Identifier.of("wynnextras", "textures/gui/guildviewer/xpbarbackground.png");
    static Identifier xpbarbackground_dark = Identifier.of("wynnextras", "textures/gui/guildviewer/xpbarbackground_dark.png");
    static Identifier xpbarprogress = Identifier.of("wynnextras", "textures/gui/guildviewer/xpbarprogress.png");

    static Identifier rankingBackgroundWideTextureDark = Identifier.of("wynnextras", "textures/gui/profileviewer/rankingicons/rankingbackgroundwide_dark.png");
    static Identifier rankingBackgroundWideTexture = Identifier.of("wynnextras", "textures/gui/profileviewer/rankingicons/rankingbackgroundwide.png");

    static Identifier dungeonBackgroundTexture = Identifier.of("wynnextras", "textures/gui/profileviewer/aspecttabbackground.png");
    static Identifier dungeonBackgroundTextureDark = Identifier.of("wynnextras", "textures/gui/profileviewer/aspecttabbackground_dark.png");

    private static ItemStack currentHovered;

    public static User currentPlayerAspectData;
    public static WynncraftApiHandler.FetchStatus fetchStatus;
    private static Page currentPage;

    private boolean createdPageWidgets = false;
    private List<AspectsTabPageButton> pageWidgets = new ArrayList<>();
    private List<AspectWidget> aspectWidgets = new ArrayList<>();

    private InfoWidget infoWidget = null;

    private static Map<Integer, List<ItemAnnotation>> annotationCache = new HashMap<>();

    public enum Page {Overview, Warrior, Shaman, Mage, Archer, Assassin}

    public AspectsTabWidget() {
        super(0, 0, 0, 0);
    }

    @Override
    protected void drawContent(DrawContext ctx, int mouseX, int mouseY, float tickDelta) {
        currentHovered = null;
        if (PV.currentPlayerData == null) return;

        if (!openedAspectPage) {
            openedAspectPage = true;
            currentPlayerAspectData = null;
            fetchStatus = null;
            currentPage = Page.Overview;

            if(PV.currentPlayerData.getUuid() == null) return;

            WynncraftApiHandler.fetchPlayerAspectData(PV.currentPlayerData.getUuid().toString(), MinecraftClient.getInstance().player.getUuidAsString())
                    .thenAccept(result -> {
                        if (result == null) return;

                        if (result.status() != null) {
                            if (result.status() == WynncraftApiHandler.FetchStatus.OK)
                                currentPlayerAspectData = result.user();
                            fetchStatus = result.status();
                        }
                    })
                    .exceptionally(ex -> {
                        System.err.println("Unexpected error: " + ex.getMessage());
                        return null;
                    });
        }

        if (fetchStatus == null) {
            ui.drawCenteredText("Loading aspects...", x + 900, y + 365, CustomColor.fromHexString("FFFF00"), 4f);
            return;
        }

        switch (fetchStatus) {
            case NOKEYSET -> {
                ui.drawCenteredText("You need to set your api-key to use this feature.", x + 900, y + 350, CustomColor.fromHexString("FF0000"), 4f);
                ui.drawCenteredText("Run \"/we apikey\" for more information.", x + 900, y + 390, CustomColor.fromHexString("FF0000"), 4f);
                return;
            }
            case FORBIDDEN -> {
                ui.drawCenteredText("You need to upload your own aspects first to view other peoples aspects.", x + 900, y + 350, CustomColor.fromHexString("FF0000"), 4f);
                ui.drawCenteredText("Run \"/we ScanAspects\" to upload your Aspects.", x + 900, y + 390, CustomColor.fromHexString("FF0000"), 4f);
                return;
            }
            case UNAUTHORIZED -> {
                ui.drawCenteredText("The api-key you have set is not connected to your minecraft account.", x + 900, y + 350, CustomColor.fromHexString("FF0000"), 4f);
                ui.drawCenteredText("Run \"/we apikey\" for more information.", x + 900, y + 390, CustomColor.fromHexString("FF0000"), 4f);
                return;
            }
            case NOT_FOUND -> {
                ui.drawCenteredText("No data found for this player!", x + 900, y + 365, CustomColor.fromHexString("FF0000"), 4f);
                return;
            }
            case SERVER_UNREACHABLE -> {
                ui.drawCenteredText("Server unreachable. Try again later.", x + 900, y + 365, CustomColor.fromHexString("FF0000"), 4f);
                return;
            }
            case SERVER_ERROR -> {
                ui.drawCenteredText("Server error occurred!", x + 900, y + 365, CustomColor.fromHexString("FF0000"), 4f);
                return;
            }
        }

        if (currentPlayerAspectData == null) {
            ui.drawCenteredText("No data found for this player!", x + 900, y + 365, CustomColor.fromHexString("FF0000"), 4f);
            return;
        }

        List<ApiAspect> aspects = new ArrayList<>(WynncraftApiHandler.fetchAllAspects());
        if(aspects.isEmpty()) {
            ui.drawCenteredText("Loading aspects...", x + 900, y + 365, CustomColor.fromHexString("FFFF00"), 4f);
            return;
        }

        List<ApiAspect> mythicAspects = new ArrayList<>();
        for(ApiAspect aspect : aspects) {
            if(aspect.getRarity().equals("mythic")) {
                mythicAspects.add(aspect);
            }
        }

        List<ApiAspect> fabledAspects = new ArrayList<>();
        for(ApiAspect aspect : aspects) {
            if(aspect.getRarity().equals("fabled")) {
                fabledAspects.add(aspect);
            }
        }

        List<ApiAspect> legendaryAspects = new ArrayList<>();
        for(ApiAspect aspect : aspects) {
            if(aspect.getRarity().equals("legendary")) {
                legendaryAspects.add(aspect);
            }
        }

        List<ApiAspect> warriorAspects = new ArrayList<>();
        for(ApiAspect aspect : aspects) {
            if(aspect.getRequiredClass().equals("warrior")) {
                warriorAspects.add(aspect);
            }
        }

        List<ApiAspect> shamanAspects = new ArrayList<>();
        for(ApiAspect aspect : aspects) {
            if(aspect.getRequiredClass().equals("shaman")) {
                shamanAspects.add(aspect);
            }
        }

        List<ApiAspect> mageAspects = new ArrayList<>();
        for(ApiAspect aspect : aspects) {
            if(aspect.getRequiredClass().equals("mage")) {
                mageAspects.add(aspect);
            }
        }

        List<ApiAspect> archerAspects = new ArrayList<>();
        for(ApiAspect aspect : aspects) {
            if(aspect.getRequiredClass().equals("archer")) {
                archerAspects.add(aspect);
            }
        }

        List<ApiAspect> assassinAspects = new ArrayList<>();
        for(ApiAspect aspect : aspects) {
            if(aspect.getRequiredClass().equals("assassin")) {
                assassinAspects.add(aspect);
            }
        }

        if(!createdPageWidgets) {
            if(PV.currentPlayerData.getCharacters() != null) {
                pageWidgets.clear();
                clearChildren();

                for (Page entry : Page.values()) {
                    pageWidgets.add(new AspectsTabPageButton(entry));
                }

                children.addAll(pageWidgets);

                createdPageWidgets = true;
            };
        }

        for (AspectsTabPageButton pageWidget : pageWidgets) {
            int entryX = x + 10 + 300 * pageWidget.page.ordinal();
            int entryY = y + 15;
            pageWidget.setBounds(entryX, entryY,280, 100);
        }

        switch (currentPage) {
            case Overview -> {
                int i = 0;
                int maxMythic = 0;
                int maxFabled = 0;
                int maxLegendary = 0;
                int maxTotal = 0;

                for(ApiAspect aspect : aspects) {
                    for(Aspect playerAspect : currentPlayerAspectData.getAspects()) {
                        if(!playerAspect.getName().equals(aspect.getName())) continue;
                        if(aspect.getRarity().equals("mythic")) {
                            if(playerAspect.getAmount() >= 15) {
                                maxMythic++;
                                maxTotal++;
                                break;
                            }
                        }
                        if(aspect.getRarity().equals("fabled")) {
                            if(playerAspect.getAmount() >= 75) {
                                maxFabled++;
                                maxTotal++;
                                break;
                            }
                        }
                        if(aspect.getRarity().equals("legendary")) {
                            if(playerAspect.getAmount() >= 150) {
                                maxLegendary++;
                                maxTotal++;
                                break;
                            }
                        }
                    }
                }

                ui.drawCenteredText("Unlocked: " + currentPlayerAspectData.getAspects().size() + "/" + aspects.size(), x + 420, y + 160, CustomColor.fromHexString("FFFFFF"), 5f);
                drawProgressBar(x + 40, y + 190, 800,80, 5f, (float) currentPlayerAspectData.getAspects().size() / aspects.size(), ctx, ui);

                ui.drawCenteredText("Total Maxed: " + maxTotal + "/" + aspects.size(), x + 420, y + 340, CustomColor.fromHexString("FFFFFF"), 5f);
                drawProgressBar(x + 40, y + 380, 800,80, 5f, (float) maxTotal / aspects.size(), ctx, ui);

                if(PV.currentPlayerData.getGlobalData() != null) {
                    PVScreen.DarkModeToggleWidget.drawImageWithFade(rankingBackgroundWideTextureDark, rankingBackgroundWideTexture,  x + 40, y + 510, 800, 160, ui);

                    ui.drawCenteredText("Total Raid Completions: " + PV.currentPlayerData.getGlobalData().getRaids().getTotal(), x + 440, y + 590, CustomColor.fromHexString("FFFFFF"), 5f);
                }

                ui.drawCenteredText("Maxed Mythic Aspects: " + maxMythic + "/" + mythicAspects.size(), x + 960 + 400, y + 160, CustomColor.fromHexString("FFFFFF"), 5f);
                drawProgressBar(x + 60 + width / 2, y + 190, 800,80, 5f, (float) maxMythic / mythicAspects.size(), ctx, ui);

                ui.drawCenteredText("Maxed Fabled Aspects: " + maxFabled + "/" + fabledAspects.size(), x + 960 + 400, y + 340, CustomColor.fromHexString("FFFFFF"), 5f);
                drawProgressBar(x + 60 + width / 2, y + 380, 800,80, 5f, (float) maxFabled / fabledAspects.size(), ctx, ui);

                ui.drawCenteredText("Maxed Legendary Aspects: " + maxLegendary + "/" + legendaryAspects.size(), x + 960 + 400, y + 530, CustomColor.fromHexString("FFFFFF"), 4.5f);
                drawProgressBar(x + 60 + width / 2, y + 570, 800,80, 5f, (float) maxLegendary / legendaryAspects.size(), ctx, ui);

                if(infoWidget == null) {
                    infoWidget = new InfoWidget(currentPlayerAspectData.getUpdatedAt());
                }

                infoWidget.setBounds(x + width - 80, y + height - 80, 50, 50);
                infoWidget.draw(ctx, mouseX, mouseY, tickDelta, ui);
            }
            default -> {
                PVScreen.DarkModeToggleWidget.drawImageWithFade(dungeonBackgroundTextureDark, dungeonBackgroundTexture,  x + 30, y + 87, 1740, 633, ui);

                List<ApiAspect> sorted = new ArrayList<>(List.copyOf(aspects));

                Map<String, Integer> rarityOrder = Map.of(
                        "mythic", 0,
                        "fabled", 1,
                        "legendary", 2
                );

                AtomicReference<ApiAspect> stellar = new AtomicReference<>();

                sorted.sort(
                        Comparator.comparing((ApiAspect a) -> {
                            String rarity = a.getRarity() == null ? "" : a.getRarity().trim().toLowerCase();
                            if(a.getName().equals("Aspect of the Stellar Flurry"))
                                stellar.set(a);

                            return rarityOrder.getOrDefault(rarity, Integer.MAX_VALUE);
                        }).thenComparing(ApiAspect::getName, String.CASE_INSENSITIVE_ORDER)
                );

                if(stellar.get() != null) {
                    sorted.remove(stellar.get());
                    sorted.add(stellar.get());
                    //the assassin stellar aspect is not sorted correctly, i dont know why so i just move it to the end
                }

                if(aspectWidgets.isEmpty()) {
                    int i = 0;
                    for(ApiAspect aspect : sorted) {
                        Aspect playerAspect = null;
                        for(Aspect a : currentPlayerAspectData.getAspects()) {
                            if(aspect.getName().equals(a.getName())) {
                                playerAspect = a;
                                break;
                            }
                        }
                        AspectWidget aspectWidget = new AspectWidget(aspect, playerAspect, warriorAspects, shamanAspects, mageAspects, archerAspects, assassinAspects);
                        aspectWidgets.add(aspectWidget);
                        i++;
                    }
                }

                int i = 0;
                int widgetIndex = 0;
                for(ApiAspect aspect : sorted) {
                    if(!aspect.getRequiredClass().equals(currentPage.toString().toLowerCase())) {
                        widgetIndex++;
                        continue;
                    }
                    int amount = getAspectAmountForClass(currentPage, warriorAspects, shamanAspects, mageAspects, archerAspects, assassinAspects);
                    //ctx.getMatrices().push();

                    int xPos;
                    if(amount % 2 == 0) {
                        xPos = 130 * (i % (amount / 2));
                    } else {
                        if(i < amount / 2f) {
                            xPos = 130 * (i % ((amount + 1) / 2)) - 70;
                        } else {
                            xPos = 130 * (i % (amount / 2));
                        }
                    }
                    int yPos = 315 * Math.floorDiv(i, (amount + 1) / 2);

                    aspectWidgets.get(widgetIndex).setBounds(
                            x + width / 2 - 63 * (amount / 2) + xPos,
                            y + yPos + 240,
                            100,
                            100
                    );

                    aspectWidgets.get(widgetIndex).draw(ctx, mouseX, mouseY, tickDelta, ui);

                    i++;
                    widgetIndex++;
                }
            }
        }

        if(currentHovered == null) return;

        //ctx.getMatrices().push();
        //ctx.getMatrices().translate(0.0F, 0.0F, 8000.0F);
        ctx.drawTooltip(MinecraftClient.getInstance().textRenderer, currentHovered.getTooltip(Item.TooltipContext.DEFAULT, McUtils.player(), TooltipType.BASIC), mouseX, mouseY);
        //ctx.getMatrices().pop();
    }

    private static int getAspectAmountForClass(Page page, List<ApiAspect> warriorAspects, List<ApiAspect> shamanAspects, List<ApiAspect> mageAspects, List<ApiAspect> archerAspects, List<ApiAspect> assassinAspects) {
        return switch (page) {
            case Warrior -> warriorAspects.size();
            case Shaman -> shamanAspects.size();
            case Mage -> mageAspects.size();
            case Archer -> archerAspects.size();
            case Assassin -> assassinAspects.size();
            default -> 0;
        };
    }

    private static void drawProgressBar(int x, int y, int width, int height, float textScale, float progress, DrawContext ctx, UIUtils ui) {
        PVScreen.DarkModeToggleWidget.drawImageWithFade(xpbarbackground_dark, xpbarbackground,  x, y, width, height, ui);

        ctx.enableScissor((int) ui.sx(x), (int) ui.sy(y), (int) ui.sx(x + width * (progress)), (int) ui.sy(y + height));
        ui.drawImage(xpbarprogress, x, y, width, height);
        ctx.disableScissor();

        PVScreen.DarkModeToggleWidget.drawImageWithFade(xpbarborder_dark, xpbarborder,  x, y, width, height, ui);
        ui.drawCenteredText(String.format("%.2f%%", progress * 100), x + width / 2f, y + height / 2f + 2, CustomColor.fromHexString("FFFFFF"), textScale);
    }

    private static ItemStack toItemStack(ApiAspect aspect, boolean max, int tier) {
        ApiAspect.Icon icon = aspect.getIcon();
        if (icon == null) return ItemStack.EMPTY;

        if (icon.getValueString() != null) {
            Identifier id = Identifier.of(icon.getValueString());
            Item item = Registries.ITEM.get(id);
            return new ItemStack(item);
        }

        if (icon.getValueObject() != null) {
            ApiAspect.IconValue iv = icon.getValueObject();
            Identifier id = Identifier.of(iv.getId());
            Item item = Registries.ITEM.get(id);
            ItemStack stack = new ItemStack(item);

            if (iv.getCustomModelData() != null) {
                try {
                    int cmd = iv.getCustomModelData().getRangeDispatch().getFirst() + (max ? 1 : 0);
                    stack.set(DataComponentTypes.CUSTOM_MODEL_DATA, new CustomModelDataComponent(List.of((float) cmd), List.of(), List.of(), List.of()));
                } catch (NumberFormatException ignored) {}
            }

            if(aspect.getName() != null) {
                try {
                    StyledText name = StyledText.fromString(aspect.getName()).withoutFormatting();
                    String color = "";
                    if(aspect.getRarity().equals("mythic")) {
                        color = "§5";
                    }
                    if(aspect.getRarity().equals("fabled")) {
                        color = "§c";
                    }
                    if(aspect.getRarity().equals("legendary")) {
                        color = "§b";
                    }
                    stack.set(DataComponentTypes.CUSTOM_NAME, Text.of(color + name.getString()));
                } catch (NumberFormatException ignored) {}
            }

            if(aspect.getTiers() != null) {
                if(aspect.getTiers().get(String.valueOf(tier)) != null) {
                    List<String> lore = aspect.getTiers().get(String.valueOf(tier)).getDescription();
                    stack.set(DataComponentTypes.LORE, new LoreComponent(WynncraftApiHandler.parseStyledHtml(lore)));
                }
            }

            return stack;
        }

        return ItemStack.EMPTY;
    }

    public static boolean isMaxed(Aspect aspect) {
        switch (aspect.getRarity()) {
            case "Mythic" -> {
                return aspect.getAmount() >= 15;
            }
            case "Fabled" -> {
                return aspect.getAmount() >= 75;
            }
            case "Legendary" -> {
                return aspect.getAmount() >= 150;
            }
            default -> {
                return false;
            }
        }
    }

    private static class AspectsTabPageButton extends Widget {
        static Identifier classBackgroundTexture = Identifier.of("wynnextras", "textures/gui/profileviewer/classbackgroundinactive.png");
        static Identifier classBackgroundTextureDark = Identifier.of("wynnextras", "textures/gui/profileviewer/classbackgroundinactive_dark.png");
        static Identifier classBackgroundTextureHovered = Identifier.of("wynnextras", "textures/gui/profileviewer/classbackgroundhovered.png");
        static Identifier classBackgroundTextureHoveredDark = Identifier.of("wynnextras", "textures/gui/profileviewer/classbackgroundhovered_dark.png");
        Page page;
        private final Runnable action;

        public AspectsTabPageButton(Page page) {
            super(0, 0, 0, 0);
            this.page = page;
            this.action = () -> {
                McUtils.playSoundUI(SoundEvents.UI_BUTTON_CLICK.value());
                currentPage = page;
            };
        }

        @Override
        protected boolean onClick(int button) {
            if (!isEnabled()) return false;
            if (action != null) action.run();
            return true;
        }

        @Override
        protected void drawContent(DrawContext ctx, int mouseX, int mouseY, float tickDelta) {
            if(hovered) {
                PVScreen.DarkModeToggleWidget.drawImageWithFade(classBackgroundTextureHoveredDark, classBackgroundTextureHovered, x, y, width, height, ui);
            } else {
                PVScreen.DarkModeToggleWidget.drawImageWithFade(classBackgroundTextureDark, classBackgroundTexture, x, y, width, height, ui);
            }

            CustomColor textColor = currentPage == page ? CustomColor.fromHexString("FFFF00") : CustomColor.fromHexString("FFFFFF");
            ui.drawCenteredText(page.name(), x + width / 2f, y + height / 2f, textColor, 4f);
        }
    }

    private static class AspectWidget extends Widget {
        public final Config<ItemHighlightFeature.HighlightTexture> highlightTexture = new Config<>(ItemHighlightFeature.HighlightTexture.CIRCLE_TRANSPARENT);
        ApiAspect aspect;
        int i;
        List<ApiAspect> warriorAspects;
        List<ApiAspect> shamanAspects;
        List<ApiAspect> mageAspects;
        List<ApiAspect> archerAspects;
        List<ApiAspect> assassinAspects;
        Aspect playerAspect;

        public AspectWidget(ApiAspect aspect, Aspect playerAspect, List<ApiAspect> warriorAspects, List<ApiAspect> shamanAspects, List<ApiAspect> mageAspects, List<ApiAspect> archerAspects, List<ApiAspect> assassinAspects) {
            super(0, 0, 0, 0);
            this.aspect = aspect;
            this.playerAspect = playerAspect;
            this.warriorAspects = warriorAspects;
            this.shamanAspects = shamanAspects;
            this.mageAspects = mageAspects;
            this.archerAspects = archerAspects;
            this.assassinAspects = assassinAspects;
        }

        public void setI(int i) {
            this.i = i;
        }

        @Override
        protected void drawContent(DrawContext ctx, int mouseX, int mouseY, float tickDelta) {
            if(aspect == null) return;
            if(!aspect.getRequiredClass().equals(currentPage.toString().toLowerCase())) {
                return;
            }

            CustomColor color = CustomColor.NONE;

            int neededForNextLevel = 0;
            int amount = 0;
            int tierInt = 1;
            String progress = "Not unlocked";
            String tier = "";
            if(playerAspect != null) {
                amount = playerAspect.getAmount();
                progress = String.valueOf(playerAspect.getAmount());

                if (aspect.getRarity().equals("mythic")) {
                    color = CustomColor.fromHexString("AA00AA");
                    if (playerAspect.getAmount() >= 15) {
                        progress = "MAX";
                        tierInt = 3;
                    } else if (playerAspect.getAmount() >= 5) {
                        tierInt = 2;
                        tier = "Tier II";
                        amount -= 5;
                        neededForNextLevel = 10;
                    } else if (playerAspect.getAmount() >= 1) {
                        tierInt = 1;
                        tier = "Tier I";
                        amount -= 1;
                        neededForNextLevel = 4;
                    }
                }
                if (aspect.getRarity().equals("fabled")) {
                    color = CustomColor.fromHexString("FF5555");
                    if (playerAspect.getAmount() >= 75) {
                        tierInt = 3;
                        progress = "MAX";
                    } else if (playerAspect.getAmount() >= 15) {
                        tierInt = 2;
                        tier = "Tier II";
                        amount -= 15;
                        neededForNextLevel = 60;
                    } else if (playerAspect.getAmount() >= 1) {
                        tierInt = 1;
                        tier = "Tier I";
                        amount -= 1;
                        neededForNextLevel = 14;
                    }
                }
                if (aspect.getRarity().equals("legendary")) {
                    color = CustomColor.fromHexString("55FFFF");
                    if (playerAspect.getAmount() >= 150) {
                        tierInt = 4;
                        progress = "MAX";
                    } else if (playerAspect.getAmount() >= 30) {
                        tierInt = 3;
                        tier = "Tier III";
                        amount -= 30;
                        neededForNextLevel = 120;
                    } else if (playerAspect.getAmount() >= 5) {
                        tierInt = 2;
                        tier = "Tier II";
                        amount -= 5;
                        neededForNextLevel = 25;
                    } else if (playerAspect.getAmount() >= 1) {
                        tierInt = 1;
                        tier = "Tier I";
                        amount -= 1;
                        neededForNextLevel = 4;
                    }
                }
            }
            if (!Objects.equals(color, CustomColor.NONE)) {
                RenderUtils.drawTexturedRect(
                        ctx,
                        Texture.HIGHLIGHT.identifier(),
                        color.withAlpha(255),
                        x / ui.getScaleFactorF() - 6 / ui.getScaleFactorF(), y / ui.getScaleFactorF() - 6 / ui.getScaleFactorF(), 18 * 6 / ui.getScaleFactorF(), 18 * 6 / ui.getScaleFactorF(),
                        highlightTexture.get().ordinal() * 18 + 18, 0,
                        18, 18,
                        Texture.HIGHLIGHT.width(),
                        Texture.HIGHLIGHT.height()
                );
            }
            if(playerAspect == null) return;
            ItemStack stack = toItemStack(aspect, isMaxed(playerAspect), tierInt);

            ctx.getMatrices().pushMatrix();
            ctx.getMatrices().scale(5 / ui.getScaleFactorF(), 5 / ui.getScaleFactorF());
            ctx.drawItem(stack, x / 5 + 2, y / 5 + 2);
            ctx.getMatrices().popMatrix();

            ui.drawCenteredText((!progress.equals("MAX") ? String.valueOf(amount) : progress) + (!progress.equals("MAX") ? "/" + neededForNextLevel : ""), x + 50, y + 120);
            ui.drawCenteredText(tier, x + 50, y - 25);

            if(hovered) {
                currentHovered = stack;
            }
        }
    }

    private static class InfoWidget extends Widget {
        static Identifier infoIcon = Identifier.of("wynnextras", "textures/gui/profileviewer/infoicon.png");
        static Identifier infoIconDark = Identifier.of("wynnextras", "textures/gui/profileviewer/infoicon_dark.png");

        Long lastUpdatedTimestamp;
        DateTimeFormatter formatter;

        public InfoWidget(Long lastUpdatedTimestamp) {
            super(0, 0, 0, 0);
            this.lastUpdatedTimestamp = lastUpdatedTimestamp;
            formatter = DateTimeFormatter.ofLocalizedDateTime(FormatStyle.SHORT).withLocale(Locale.getDefault()).withZone(ZoneId.systemDefault());
        }

        @Override
        protected void drawContent(DrawContext ctx, int mouseX, int mouseY, float tickDelta) {
            PVScreen.DarkModeToggleWidget.drawImageWithFade(infoIconDark, infoIcon, x, y, width, height, ui);

            if(hovered) {
                String formatted = formatter.format(Instant.ofEpochMilli(lastUpdatedTimestamp));
                ctx.drawTooltip(MinecraftClient.getInstance().textRenderer, List.of(Text.of("Last updated at"), Text.of(formatted)), mouseX, mouseY);
            }
        }
    }
}
