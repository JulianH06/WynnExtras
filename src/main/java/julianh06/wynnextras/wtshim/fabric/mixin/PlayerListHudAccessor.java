// © Wynntils (LGPL-3.0-only) — see LICENSE-WYNNTILS
/*
 * WynnExtras compat — accessor for PlayerListHud footer/header.
 * Wynncraft publishes player status effects via the tab-list footer; we need to read it.
 */
package julianh06.wynnextras.wtshim.fabric.mixin;

import net.minecraft.client.gui.hud.PlayerListHud;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(PlayerListHud.class)
public interface PlayerListHudAccessor {
    @Accessor("footer")
    Text getFooter();

    @Accessor("header")
    Text getHeader();
}
