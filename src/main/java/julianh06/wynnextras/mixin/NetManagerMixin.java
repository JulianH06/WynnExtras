package julianh06.wynnextras.mixin;

import com.wynntils.core.net.NetManager;
import com.wynntils.core.net.UrlId;
import julianh06.wynnextras.config.WynnExtrasConfig;
import julianh06.wynnextras.features.profileviewer.PV;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Map;

@Mixin(NetManager.class)
public class NetManagerMixin {
    @Inject(method = "openLink(Lcom/wynntils/core/net/UrlId;Ljava/util/Map;)V", at = @At("HEAD"), cancellable = true)
    private void onOpenLink(UrlId urlId, Map<String, String> arguments, CallbackInfo ci) {
        if (!WynnExtrasConfig.INSTANCE.redirectWynntilsViewStatsToPV) return;
        if (urlId != UrlId.LINK_WYNNCRAFT_PLAYER_STATS || arguments == null) return;

        String username = arguments.get("username");
        if (username == null || username.isBlank()) return;

        PV.open(username);
        ci.cancel();
    }
}