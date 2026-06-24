package crqzycat.smartview.mixin.client;

import crqzycat.smartview.client.modules.PingModule;
import net.minecraft.client.network.ClientCommonNetworkHandler;
import net.minecraft.network.packet.s2c.common.KeepAliveS2CPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientCommonNetworkHandler.class)
public class PingMixin {

    @Inject(method = "onKeepAlive", at = @At("HEAD"))
    private void smartview$onKeepAlive(KeepAliveS2CPacket packet, CallbackInfo ci) {
        PingModule.updatePing();
    }
}
