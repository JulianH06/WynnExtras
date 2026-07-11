package julianh06.wynnextras.mixin.Accessor;

import net.minecraft.network.packet.s2c.play.BossBarS2CPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.UUID;

@Mixin(BossBarS2CPacket.class)
public interface BossBarS2CPacketAccessor {
    @Accessor("uuid")
    UUID getUuid();
}