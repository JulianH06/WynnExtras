package julianh06.wynnextras.mixin;

import julianh06.wynnextras.duck.EntityRenderStateAccess;
import net.minecraft.client.render.entity.state.EntityRenderState;
import net.minecraft.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

@Mixin(EntityRenderState.class)
public class EntityRenderStateMixin implements EntityRenderStateAccess {
    @Unique
    private Entity wynnExtras$entity;

    @Override
    public Entity wynnExtras$getEntity() {
        return wynnExtras$entity;
    }

    @Override
    public void wynnExtras$setEntity(Entity entity) {
        wynnExtras$entity = entity;
    }
}
