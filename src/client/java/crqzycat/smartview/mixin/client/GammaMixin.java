package crqzycat.smartview.mixin.client;

import crqzycat.smartview.client.hud.ModuleManager;
import net.minecraft.client.render.LightmapTextureManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(LightmapTextureManager.class)
public class GammaMixin {

    /**
     * In LightmapTextureManager.update() the gamma value is read from
     * GameOptions and stored in a local double variable. We intercept that
     * variable and replace it with 10.0 when Fullbright is active.
     *
     * The target INVOKE is the call to SimpleOption.getValue() for gamma
     * inside update(). ModifyVariable captures the return value as it is
     * assigned to the local variable – bypassing the SimpleOption validator
     * entirely and also bypassing Sodium's own lightmap pipeline.
     */
    @ModifyVariable(
        method = "update",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/option/SimpleOption;getValue()Ljava/lang/Object;",
            ordinal = 0,
            shift = At.Shift.AFTER
        ),
        ordinal = 0
    )
    private double smartview$overrideGamma(double original) {
        return ModuleManager.isFullbrightEnabled() ? 10.0 : original;
    }
}
