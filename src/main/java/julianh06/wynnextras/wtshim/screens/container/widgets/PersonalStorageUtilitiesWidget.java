// © Wynntils (LGPL-3.0-only) — see LICENSE-WYNNTILS
/*
 * WynnExtras — PersonalStorageUtilitiesWidget (INERT STAND-IN / type token).
 *
 * WynnExtras' BankOverlay HandledScreenMixin scans the open screen's children for this widget type
 * (`child instanceof PersonalStorageUtilitiesWidget`) to reuse Wynntils' bank page-jump buttons.
 * In the standalone shim nothing ever adds this widget to a screen (the shim's
 * PersonalStorageUtilitiesFeature drives page-jumps directly), so the instanceof never matches and
 * this class is never instantiated — it exists solely so the mixin binds. It extends ClickableWidget
 * so the render/isMouseOver/mouseClicked calls in the mixin resolve against real Yarn widget API.
 */
package julianh06.wynnextras.wtshim.screens.container.widgets;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.narration.NarrationMessageBuilder;
import net.minecraft.client.gui.widget.ClickableWidget;
import net.minecraft.text.Text;

public class PersonalStorageUtilitiesWidget extends ClickableWidget {
    public PersonalStorageUtilitiesWidget(int x, int y, int width, int height) {
        super(x, y, width, height, Text.empty());
    }

    @Override
    protected void renderWidget(DrawContext context, int mouseX, int mouseY, float delta) {
        // inert
    }

    @Override
    protected void appendClickableNarrations(NarrationMessageBuilder builder) {
        // inert
    }
}
