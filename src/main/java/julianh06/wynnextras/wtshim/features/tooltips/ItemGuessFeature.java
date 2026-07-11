// © Wynntils (LGPL-3.0-only) — see LICENSE-WYNNTILS
/*
 * WynnExtras — ItemGuessFeature.
 * Mixin target: ItemGuessFeatureAccessor#callGetTooltipAddon.
 */
package julianh06.wynnextras.wtshim.features.tooltips;

import julianh06.wynnextras.wtshim.core.consumers.features.Feature;
import julianh06.wynnextras.wtshim.models.items.items.game.GearBoxItem;
import java.util.List;
import net.minecraft.text.Text;

public class ItemGuessFeature extends Feature {
    /** Mixin target. Stub: empty extra tooltip lines — full gear-box guessing requires gear.json. */
    private List<Text> getTooltipAddon(GearBoxItem gearBoxItem) {
        return List.of();
    }
}
