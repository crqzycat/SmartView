package crqzycat.smartview.mixin.client;

import crqzycat.smartview.client.modules.PingModule;
import net.minecraft.client.network.ClientCommonNetworkHandler;
import net.minecraft.network.packet.s2c.common.KeepAliveS2CPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientCommonNetworkHandler.class)
public class PingMixin {

    /** Timestamp (ms) when we last sent a keep-alive pong to the server. */
    @Unique
    private static long smartview$lastPongSentAt = 0L;

    /**
     * onKeepAlive() is called when the server's ping arrives.
     * The client will immediately send its pong reply after this method.
     * We record "now" as our send time, then on the NEXT call we know the
     * full round-trip: server sent → client received → client sent pong →
     * server received → server sent next ping → client received.
     *
     * That is ~1 RTT + server processing (~0 ms) ≈ RTT.
     */
    @Inject(method = "onKeepAlive", at = @At("HEAD"))
    private void smartview$onKeepAlive(KeepAliveS2CPacket packet, CallbackInfo ci) {
        long now = System.currentTimeMillis();
        if (smartview$lastPongSentAt > 0) {
            // Elapsed since we last replied = server interval + RTT
            // Server sends keep-alive every 1000 ms, so subtract that.
            long elapsed = now - smartview$lastPongSentAt;
            int rtt = (int) Math.max(0, elapsed - 1000L);
            PingModule.updatePing(rtt);
        }
        smartview$lastPongSentAt = now;
    }
}
