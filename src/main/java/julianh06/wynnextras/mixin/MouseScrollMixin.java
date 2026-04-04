//package julianh06.wynnextras.mixin;
//
//import julianh06.wynnextras.features.chat.ChatPeek;
//import net.minecraft.client.Mouse;
//import org.spongepowered.asm.mixin.Mixin;
//import org.spongepowered.asm.mixin.injection.At;
//import org.spongepowered.asm.mixin.injection.Inject;
//import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
//
//@Mixin(Mouse.class)
//public class MouseScrollMixin {
//    @Inject(
//            method = "onMouseScroll",
//            at = @At("HEAD"),
//            cancellable = true
//    )
//    private void onMouseScroll(long window, double horizontal, double vertical, CallbackInfo ci) {
//        if (ChatPeek.onScroll(vertical)) {
//            ci.cancel();
//        }
//    }
//}