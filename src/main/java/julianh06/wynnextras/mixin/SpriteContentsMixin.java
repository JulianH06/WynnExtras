package julianh06.wynnextras.mixin;

import julianh06.wynnextras.features.spellhider.SpellHider;
import net.minecraft.client.resource.metadata.AnimationResourceMetadata;
import net.minecraft.client.resource.metadata.TextureResourceMetadata;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.texture.SpriteContents;
import net.minecraft.client.texture.SpriteDimensions;
import net.minecraft.resource.metadata.ResourceMetadataSerializer;
import net.minecraft.util.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;
import java.util.Optional;

@Mixin(SpriteContents.class)
public class SpriteContentsMixin {
    @Inject(
            method = "<init>(Lnet/minecraft/util/Identifier;Lnet/minecraft/client/texture/SpriteDimensions;Lnet/minecraft/client/texture/NativeImage;)V",
            at = @At("RETURN")
    )
    private void onCreate(Identifier id, SpriteDimensions dimensions, NativeImage image, CallbackInfo ci) {
        addHash(id, image);
    }

    @Inject(
            method = "<init>(Lnet/minecraft/util/Identifier;Lnet/minecraft/client/texture/SpriteDimensions;Lnet/minecraft/client/texture/NativeImage;Ljava/util/Optional;Ljava/util/List;Ljava/util/Optional;)V",
            at = @At("RETURN")
    )
    private void onCreate(Identifier id,
                          SpriteDimensions dimensions,
                          NativeImage image,
                          Optional<AnimationResourceMetadata> animationResourceMetadata,
                          List<ResourceMetadataSerializer.Value<?>> additionalMetadata,
                          Optional<TextureResourceMetadata> metadata,
                          CallbackInfo ci
    ) {
        addHash(id, image);
    }

    @Unique private static void addHash(Identifier id, NativeImage image) {
        if (!id.getPath().startsWith("item/w")) return;
        int hash = SpellHider.hashNativeImage(image);
        SpellHider.hashMap.put(id.getPath(), hash);
    }
}
