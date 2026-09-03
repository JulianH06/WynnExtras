package julianh06.wynnextras.mixin;

import julianh06.wynnextras.features.inventory.ScaleBackgroundRenderState;
import net.minecraft.component.ComponentHolder;
import net.minecraft.component.ComponentType;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.CustomModelDataComponent;
import net.minecraft.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;

@Mixin(ComponentHolder.class)
public interface ScaleRarityBackgroundMixin {
    @Inject(
            method = "get(Lnet/minecraft/component/ComponentType;)Ljava/lang/Object;",
            at = @At("RETURN"),
            cancellable = true
    )
    private void removeRarityModelWhileDrawingScale(
            ComponentType<?> type,
            CallbackInfoReturnable<Object> cir
    ) {
        if (type != DataComponentTypes.CUSTOM_MODEL_DATA) return;
        if (!((Object) this instanceof ItemStack stack)) return;
        if (!ScaleBackgroundRenderState.isActive(stack)) return;
        if (!(cir.getReturnValue() instanceof CustomModelDataComponent modelData)) return;

        List<String> strings = modelData.strings();
        List<String> withoutRarity = strings.stream()
                .map(value -> value.startsWith("item_tier") ? "" : value)
                .toList();
        if (withoutRarity.equals(strings)) return;

        cir.setReturnValue(new CustomModelDataComponent(
                modelData.floats(),
                modelData.flags(),
                withoutRarity,
                modelData.colors()
        ));
    }
}
