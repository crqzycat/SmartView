package crqzycat.smartview.mixin.client;

import crqzycat.smartview.client.hud.ModuleManager;
import net.minecraft.client.option.GameOptions;
import net.minecraft.client.option.SimpleOption;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(GameOptions.class)
public class GammaMixin {

    @Inject(method = "getGamma", at = @At("RETURN"), cancellable = true)
    private void smartview$overrideGamma(CallbackInfoReturnable<SimpleOption<Double>> cir) {
        if (ModuleManager.isFullbrightEnabled()) {
            cir.getReturnValue().setValue(10.0);
        }
    }
}
