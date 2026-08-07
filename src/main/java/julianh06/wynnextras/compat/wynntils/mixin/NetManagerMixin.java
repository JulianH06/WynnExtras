package julianh06.wynnextras.compat.wynntils.mixin;

import julianh06.wynnextras.config.WynnExtrasConfig;
import julianh06.wynnextras.features.profileviewer.PV;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Coerce;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Map;

@Pseudo
@Mixin(targets = "com.wynntils.core.net.NetManager", remap = false)
public class NetManagerMixin {
    @Inject(method = "openLink(Lcom/wynntils/core/net/UrlId;Ljava/util/Map;)V", at = @At("HEAD"), cancellable = true, require = 0)
    private void onOpenLink(@Coerce Object urlId, Map<String, String> arguments, CallbackInfo ci) {
        if (!WynnExtrasConfig.INSTANCE.redirectWynntilsViewStatsToPV) return;
        if (!(urlId instanceof Enum<?> value) || !value.name().equals("LINK_WYNNCRAFT_PLAYER_STATS") || arguments == null) return;

        String username = arguments.get("username");
        if (username == null || username.isBlank()) return;

        PV.open(username);
        ci.cancel();
    }
}
