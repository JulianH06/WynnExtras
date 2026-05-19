package julianh06.wynnextras.mixin.Accessor;

import net.neoforged.bus.api.Event;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(Event.class)
public interface EventAccessor {
    @Accessor("isCanceled")
    public void setCanceled(boolean canceled);
}
