package julianh06.wynnextras.features.misc;

import com.wynntils.core.components.Models;
import com.wynntils.models.containers.containers.CharacterInfoContainer;
import com.wynntils.utils.colors.CustomColor;
import com.wynntils.utils.mc.McUtils;
import julianh06.wynnextras.core.WynnExtras;
import julianh06.wynnextras.mixin.Accessor.HandledScreenAccessor;
import julianh06.wynnextras.utils.UI.WEHandledScreen;
import julianh06.wynnextras.utils.UI.Widget;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.text.Text;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


public class CompassMenuOverlay extends WEHandledScreen {
    List<ItemWidget> itemWidgets = new ArrayList<>();
    static ItemStack hoveredItem = Items.AIR.getDefaultStack();

    public CompassMenuOverlay() {
        for (int i = 0; i < 4; i++) {
            ItemWidget itemWidget = new ItemWidget();
            itemWidgets.add(itemWidget);
            rootWidgets.add(itemWidget);
        }
    }

    @Override
    protected void drawBackground(DrawContext ctx, int mouseX, int mouseY, float delta) {
        hoveredItem = Items.AIR.getDefaultStack();
    }

    @Override
    protected void drawContent(DrawContext ctx, int mouseX, int mouseY, float delta) {
        if(!(Models.Container.getCurrentContainer() instanceof CharacterInfoContainer container)) return;
        if(!(McUtils.screen() instanceof HandledScreen<?> screen)) return;

        float xStart = (int) (((HandledScreenAccessor) screen).getX() * ui.getScaleFactor());
        float yStart = (int) ((((HandledScreenAccessor) screen).getY() + ((HandledScreenAccessor) screen).getBackgroundHeight()) * ui.getScaleFactor());
        float backgroundWidth = ((HandledScreenAccessor) screen).getBackgroundWidth() * ui.getScaleFactorF();

        ui.drawCenteredText(WynnExtras.addWynnExtrasPrefix(""), xStart + backgroundWidth / 2f, yStart + 25);
        ui.drawCenteredText("§6Disabled Armor:", xStart + backgroundWidth / 2f, yStart + 60);

        int itemWidth = 50;
        int itemHeight = itemWidth;
        float itemXStart = xStart + 23;
        float itemYStart = yStart + 100;
        backgroundWidth -= 97;

        for(int i = 0; i < 4; i++) {
            ItemStack item = McUtils.player().getEquippedStack(EquipmentSlot.FROM_INDEX.apply(4 - i));
            itemWidgets.get(i).setBounds((int) (itemXStart + i * backgroundWidth / 3f), (int) itemYStart, itemWidth, itemHeight);
            itemWidgets.get(i).setItem(item);
        }
    }

    @Override
    protected void drawForeground(DrawContext ctx, int mouseX, int mouseY, float delta) {
        if(hoveredItem.isEmpty()) return;

        ctx.drawItemTooltip(MinecraftClient.getInstance().textRenderer, hoveredItem, mouseX, mouseY);
    }

    private static class ItemWidget extends Widget {
        ItemStack item;

        @Override
        protected void drawContent(DrawContext ctx, int mouseX, int mouseY, float tickDelta) {
            if(item == null) return;

            ctx.drawItem(item, (int) (x / ui.getScaleFactor()), (int) (y / ui.getScaleFactor()));

            if(hovered) {
                hoveredItem = item;
                ui.drawRect(x - 0.5f, y - 0.25f, width, height, CustomColor.fromHexString("FFFFFF").withAlpha(0.25f));
                //if(true) return;
                int textY = -800;
                //System.out.println("===================");
                String[] order = { "STR", "INT", "AGI", "DEX", "DEF" };

                int index = 0;

                for(Text text : item.getTooltip(Item.TooltipContext.DEFAULT, null, TooltipType.BASIC)) {
                    StringBuilder sb = new StringBuilder();

                    String input = text.getString();
                    for(int codePoint : input.codePoints().toArray()) {
                        //if(String.format("U+%04X ", codePoint).equals("U+D0003 ")) {
                            sb.append(String.format("U+%04X ", codePoint));
                        //}
                    }
                    try {
                        ui.drawText(sb.toString().trim(), 0, textY);
                        ui.drawText(text, 0, textY + 50);

                        textY += 100;
                        //System.out.println(sb);
                    } catch (Exception ignored) {}

                    String raw = text.getString();

                    if (!isRequirementLine(raw))
                        continue;

                    int value = extractNumber(raw);
                    Integer color = findTextColor(text);

                    if(index < 4) System.out.println(order[index] + " = " + value + " | color=" + color);

                    index++;
                }
            }
        }

        public void setItem(ItemStack item) {
            this.item = item;
        }

        private static int extractNumber(String s) {
            Matcher m = Pattern.compile("(\\d+)").matcher(s);
            return m.find() ? Integer.parseInt(m.group(1)) : -1;
        }

        private static Integer findTextColor(Text text) {
            if (text.getStyle().getColor() != null) {
                return text.getStyle().getColor().getRgb();
            }

            for (Text sibling : text.getSiblings()) {
                Integer c = findTextColor(sibling);
                if (c != null) return c;
            }

            return null;
        }

        private static boolean isRequirementLine(String raw) {
            String cleaned = stripPrivateUse(raw);

            // enthält Zahl
            if (!cleaned.matches(".*\\d+.*"))
                return false;

            // darf keine Buchstaben enthalten
            if (cleaned.matches(".*[a-zA-Z].*"))
                return false;

            return true;
        }

        private static String stripPrivateUse(String input) {
            return input
                    .replaceAll("[\\uE000-\\uF8FF]", "")   // Icons
                    .replaceAll("[\\uD000-\\uDFFF]", "")   // Steuersequenzen
                    .replaceAll("[\\uC000-\\uCFFF]", "");  // Farbmarker
        }
    }
}