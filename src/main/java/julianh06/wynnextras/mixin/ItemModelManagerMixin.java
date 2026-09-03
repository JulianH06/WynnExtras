package julianh06.wynnextras.mixin;

import julianh06.wynnextras.features.spellhider.*;
import julianh06.wynnextras.core.WynnExtras;
import julianh06.wynnextras.features.inventory.MountColorBackground;
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
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.HashSet;
import java.util.Set;

@Mixin(ItemModelManager.class)
public class ItemModelManagerMixin {
    @ModifyVariable(method = "update", at = @At("HEAD"), argsOnly = true)
    private ItemStack addMountColorBackground(ItemStack stack) {
        return MountColorBackground.apply(stack);
    }

    @Inject(method = "updateForNonLivingEntity", at = @At("TAIL"))
    public void updateModelsForNonLiving(ItemRenderState renderState, ItemStack stack, ItemDisplayContext displayContext, Entity entity, CallbackInfo ci) {
        if (!(entity instanceof DisplayEntity.ItemDisplayEntity)) return;
        if (stack.getItem() != Items.OAK_BOAT) return;

        Set<Identifier> fileNames = getFileNames(renderState);
        if (fileNames == null) {
            return;
        }
        for (Identifier fileName : fileNames) {
            Float modelData = ItemUtils.getFirsCustomModelDataFloat(stack);
            if (modelData != null) {
                SpellHider.addModel(fileName.getPath(), modelData);
            }

            SpellNamespace spellMapping = SpellHiderMappings.INSTANCE.getSpellMapping(fileName);
            if (spellMapping == null || spellMapping.isEmpty()) {
                SpellData fromPath = SpellHider.getFromPath(fileName.getPath());
                if (fromPath == null) {
                    return;
                }
                ModelDataLogger.addTextToRender(fromPath.getHash(), entity.getEntityPos());
                ModelDataLogger.handleUnknownModel(modelData, fileNames);
            } else {
                SpellHider.addName(fileName.getPath(), spellMapping.getFQName());
                ModelDataLogger.addTextToRender(spellMapping.getFQName(), entity.getEntityPos());
            }
        }
    }

    @Unique
    private static Set<Identifier> getFileNames(ItemRenderState renderState) {
        ItemRenderStateAccessor renderStateAccess = (ItemRenderStateAccessor) renderState;
        int layerCount = renderStateAccess.getLayerCount();
        ItemRenderState.LayerRenderState[] layers = renderStateAccess.getLayers();
        if (layers == null || layerCount == 0) {
            return null;
        }

        Set<Identifier> names = new HashSet<>();
        for (int i = 0; i < layerCount; i++)
            layers[i].getQuads().forEach(q -> names.add(q.sprite().getContents().getId()));
        return names;
    }

}
