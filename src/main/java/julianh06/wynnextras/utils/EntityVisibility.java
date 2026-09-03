package julianh06.wynnextras.utils;

import net.minecraft.entity.Entity;

import java.util.Collections;
import java.util.Set;
import java.util.WeakHashMap;

public final class EntityVisibility {
    private static final Set<Entity> HIDDEN = Collections.newSetFromMap(new WeakHashMap<>());

    private EntityVisibility() {}

    public static void setRendered(Entity entity, boolean rendered) {
        if (entity == null) return;
        if (rendered) HIDDEN.remove(entity);
        else HIDDEN.add(entity);
    }

    public static boolean isHidden(Entity entity) {
        return entity != null && HIDDEN.contains(entity);
    }
}
