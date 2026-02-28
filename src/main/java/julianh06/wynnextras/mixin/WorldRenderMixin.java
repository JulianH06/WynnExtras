package julianh06.wynnextras.mixin;

import julianh06.wynnextras.features.spellhider.SpellHider;
import julianh06.wynnextras.features.spellhider.SpellModifier;
import julianh06.wynnextras.features.spellhider.SpellModifiers;
import net.minecraft.client.render.WorldRenderer;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

@Mixin(WorldRenderer.class)
public class WorldRenderMixin {
    @Redirect(
            method = "fillEntityRenderStates",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/world/ClientWorld;getEntities()Ljava/lang/Iterable;"
            )
    )
    private static Iterable<Entity> noRender(ClientWorld instance) {
        return StreamSupport.stream(instance.getEntities().spliterator(), false)
                .filter(entity -> {
                    SpellModifiers modifiers = SpellHider.getModifiers(entity);
                    return (modifiers == null || !Boolean.FALSE.equals(modifiers.get(SpellModifier.VISIBLE)));
                })
                .collect(Collectors.toList());
    }
}
