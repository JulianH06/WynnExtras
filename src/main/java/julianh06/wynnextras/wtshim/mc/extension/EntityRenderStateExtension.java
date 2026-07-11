// © Wynntils (LGPL-3.0-only) — see LICENSE-WYNNTILS
/* WynnExtras — EntityRenderStateExtension marker. */
package julianh06.wynnextras.wtshim.mc.extension;

import net.minecraft.entity.Entity;

public interface EntityRenderStateExtension {
    Entity getEntity();
    void setEntity(Entity entity);
}
