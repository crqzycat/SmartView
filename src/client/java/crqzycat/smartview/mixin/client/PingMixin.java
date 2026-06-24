package crqzycat.smartview.mixin.client;

import crqzycat.smartview.client.modules.PingModule;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.network.packet.s2c.play.KeepAliveS2CPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientPlayNetworkHandler.class)
public class PingMixin {

    /**
     * Every time the server sends a keep-alive packet we record the current
     * time. When onKeepAlive() is called the round-trip is complete, so we
     * calculate the elapsed time and store it in PingModule.
     */
    @Inject(method = "onKeepAlive", at = @At("HEAD"))
    private void smartview$onKeepAlive(KeepAliveS2CPacket packet, CallbackInfo ci) {
        PingModule.updatePing();
    }
}
