package julianh06.wynnextras.mixin;

import julianh06.wynnextras.features.qol.StackDuplicateMessages;
import net.minecraft.client.gui.hud.ChatHud;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(ChatHud.class)
public class ChatHudAddMessageMixin {
    @ModifyVariable(method = "addMessage(Lnet/minecraft/text/Text;)V", at = @At("HEAD"), argsOnly = true)
    private Text modifyMessage(Text message) {
        return StackDuplicateMessages.process(message);
    }
}
