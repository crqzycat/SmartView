package crqzycat.smartview.mixin.client;

import crqzycat.smartview.client.modules.PacketLossModule;
import net.minecraft.client.network.ClientCommonNetworkHandler;
import net.minecraft.network.packet.s2c.common.KeepAliveS2CPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientCommonNetworkHandler.class)
public class PacketLossMixin {

    /**
     * Each keep-alive from the server = one sent ping.
     * When we receive it and reply, that counts as both sent+received.
     * If a packet is truly lost the server disconnects us – so what we
     * actually track here is server-side keep-alive delivery rate, which
     * correlates with packet loss on the server→client leg.
     */
    @Inject(method = "onKeepAlive", at = @At("HEAD"))
    private void smartview$onKeepAliveReceived(KeepAliveS2CPacket packet, CallbackInfo ci) {
        PacketLossModule.onReceived();
    }

    @Inject(method = "onKeepAlive", at = @At("RETURN"))
    private void smartview$onKeepAliveSent(KeepAliveS2CPacket packet, CallbackInfo ci) {
        PacketLossModule.onSent();
    }
}
