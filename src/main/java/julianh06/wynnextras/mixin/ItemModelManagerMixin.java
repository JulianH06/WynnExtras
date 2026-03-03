package julianh06.wynnextras.mixin;

import julianh06.wynnextras.config.SpellHiderConfig;
import julianh06.wynnextras.core.WynnExtras;
import julianh06.wynnextras.features.spellhider.ModelDataLogger;
import julianh06.wynnextras.features.spellhider.SpellHider;
import julianh06.wynnextras.features.spellhider.SpellNamespace;
import julianh06.wynnextras.mixin.Accessor.ItemRenderStateAccessor;
import julianh06.wynnextras.utils.ItemUtils;
import net.minecraft.client.item.ItemModelManager;
import net.minecraft.client.render.item.ItemRenderState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.decoration.DisplayEntity;
import net.minecraft.item.ItemDisplayContext;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.util.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.HashSet;
import java.util.Set;

@Mixin(ItemModelManager.class)
public class ItemModelManagerMixin {
    @Inject(method = "updateForNonLivingEntity", at = @At("TAIL"))
    public void updateModelsForNonLiving(ItemRenderState renderState, ItemStack stack, ItemDisplayContext displayContext, Entity entity, CallbackInfo ci) {
        if (!(entity instanceof DisplayEntity.ItemDisplayEntity)) return;
        if (stack.getItem() != Items.OAK_BOAT) return;
        Float customModel = ItemUtils.getFirsCustomModelDataFloat(stack); // changes on texture pack updates
        if (customModel == null) return;
        SpellNamespace nameForModel = SpellHider.getNameForModel(customModel); // in memory cache
        // null is if it hasn't even tried to check the hash,
        // empty is if it has checked hash and didn't find any mapping.
        if (nameForModel != null) {
            if (nameForModel.isEmpty()) {
                ModelDataLogger.handleUnknownModel(customModel, getFileNames(renderState));
            }
        } else checkMappingsAndCache(renderState, customModel);
    }

    @Unique
    private static void checkMappingsAndCache(ItemRenderState renderState, Float customModel) {
        Set<Identifier> names = getFileNames(renderState);
        if (names == null || names.size() > 1) {
            WynnExtras.LOGGER.warn("multiple paths for model {} {}", customModel, names);
            return;
        }
        for (Identifier id : names) {
            SpellNamespace nameFromHash = SpellHiderConfig.INSTANCE.getSpellMapping(id);
            // if not found cache as empty string to indicate that we have checked the hash mappings
            if (nameFromHash == null) nameFromHash = new SpellNamespace("");
            SpellHider.addModel(customModel, nameFromHash);
        }
    }

    @Unique
    private static Set<Identifier> getFileNames(ItemRenderState renderState) {
        ItemRenderStateAccessor renderStateAccess = (ItemRenderStateAccessor) renderState;
        int layerCount = renderStateAccess.getLayerCount();
        ItemRenderState.LayerRenderState[] layers = renderStateAccess.getLayers();
        if (layers == null || layerCount == 0) {
            WynnExtras.LOGGER.warn("No item layer found in ItemRenderStateAccessor");
            return null;
        }

        Set<Identifier> names = new HashSet<>();
        for (int i = 0; i < layerCount; i++)
            layers[i].getQuads().forEach(q -> names.add(q.sprite().getContents().getId()));
        return names;
    }

}
