package julianh06.wynnextras.features.misc;

import com.wynntils.utils.colors.CustomColor;
import com.wynntils.utils.mc.McUtils;
import julianh06.wynnextras.utils.UI.WEScreen;
import julianh06.wynnextras.utils.UI.Widget;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.screen.slot.Slot;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

import java.util.ArrayList;
import java.util.List;

import static julianh06.wynnextras.features.profileviewer.PVScreen.mouseX;
import static julianh06.wynnextras.features.profileviewer.PVScreen.mouseY;

public class CustomClassSelectionOverlay extends WEScreen {
    public static final CustomClassSelectionOverlay INSTANCE =
            new CustomClassSelectionOverlay();

    List<ClassWidget> classWidgets = new ArrayList<>();

    protected CustomClassSelectionOverlay() {
        super(Text.of("Overlay"));
    }

    @Override
    protected void drawContent(DrawContext ctx, int mouseX, int mouseY, float tickDelta) {

        int xStart = getLogicalWidth() / 2 - 500 - 200;
        int yStart = 100;

        if(classWidgets.isEmpty()) {
            int i = 0;
            for (Slot slot : McUtils.containerMenu().slots) {
                if (slot.getIndex() == 7 || slot.getIndex() == 25 || slot.getIndex() == 51 || slot.getIndex() == 53)
                    continue;
                if (slot.getStack() == null || slot.getStack().getCustomName() == null)
                    continue;

                ClassWidget widget = new ClassWidget(slot.getStack().getCustomName().getString(), slot.getIndex());
                classWidgets.add(widget);
                addRootWidget(widget);
                i++;
            }
        }

        int i = 0;
        for (ClassWidget widget : classWidgets) {
            if(widget.dragging) continue;

            int baseX = xStart + (i % 3) * 500;
            int baseY = yStart + (i / 3) * 250;

            widget.setBounds(
                    baseX + widget.xDrag,
                    baseY + widget.yDrag,
                    400,
                    200
            );
            i++;
        }
    }


    @Override
    public boolean mouseDragged(Click click, double dx, double dy) {
        double mouseX = click.x();
        double mouseY = click.y();
        int button = click.button();

        if(ui == null) return false;
        for (int i = rootWidgets.size() - 1; i >= 0; i--) {
            Widget w = rootWidgets.get(i);
            if (w.isHovered()) {
                w.mouseDragged(mouseX, mouseY, button, dx, dy);
                return true;
            }
        }
        return super.mouseDragged(click, dx, dy);
    }

    private static class ClassWidget extends Widget {
        static Identifier classBackgroundTexture = Identifier.of("wynnextras", "textures/gui/profileviewer/classbackgroundinactive.png");
        static Identifier classBackgroundTextureHovered = Identifier.of("wynnextras", "textures/gui/profileviewer/classbackgroundhovered.png");

        final String className;
        final int slotIndex;

        public int xDrag = 0;
        public int yDrag = 0;

        public boolean dragging = false;
        double lastMouseX = 0;
        double lastMouseY = 0;

        public ClassWidget(String className, int slotIndex) {
            super(0, 0, 0, 0);
            this.className = className;
            this.slotIndex = slotIndex;
        }

        @Override
        protected void drawContent(DrawContext ctx, int mouseX, int mouseY, float tickDelta) {
            if(dragging) return;
            ui.drawImage(
                    hovered ? classBackgroundTextureHovered : classBackgroundTexture,
                    x,
                    y,
                    width,
                    height
            );

            ui.drawText(className, x + 5,  y + 5);
            ui.drawText("Load",      x + 5, y + 50);
        }

        // -------- CLICK START DRAG --------
        @Override
        protected boolean onClick(int button) {
            if (button != 0) return false;

            if (hovered) {
                dragging = true;
                lastMouseX = mouseX;
                lastMouseY = mouseY;
                return true;
            }
            return false;
        }

        @Override
        public boolean mouseReleased(double mouseX, double mouseY, int button) {
            if (button == 0) dragging = false;
            return super.mouseReleased(mouseX, mouseY, button);
        }

        // -------- DRAGGING --------
        @Override
        public boolean mouseDragged(double mouseX, double mouseY, int button, double deltaX, double deltaY) {
            this.x = (int) (mouseX * ui.getScaleFactor());
            this.y = (int) (mouseY * ui.getScaleFactor());
            System.out.println("mousex:" + mouseX);
            ui.drawImage(
                    hovered ? classBackgroundTextureHovered : classBackgroundTexture,
                    x,
                    y,
                    width,
                    height
            );

            ui.drawText(className, x + 5,  y + 5);
            ui.drawText("Load",      x + 5, y + 50);

//            double dx = mouseX - lastMouseX;
//            double dy = mouseY - lastMouseY;
//
//            xDrag += dx;
//            yDrag += dy;
//
//            lastMouseX = mouseX;
//            lastMouseY = mouseY;

            return true;
        }
    }


}

