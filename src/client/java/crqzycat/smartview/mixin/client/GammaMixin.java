package crqzycat.smartview.mixin.client;

import crqzycat.smartview.client.hud.ModuleManager;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.SimpleOption;
import net.minecraft.client.render.LightmapTextureManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LightmapTextureManager.class)
public class GammaMixin {

    @Shadow
    private MinecraftClient client;

    @Unique
    private double smartview$savedGamma = -1.0;

    @Unique
    private boolean smartview$wasEnabled = false;

    @Inject(method = "update", at = @At("HEAD"))
    private void smartview$preUpdate(float delta, CallbackInfo ci) {
        boolean enabled = ModuleManager.isFullbrightEnabled();
        SimpleOption<Double> gamma = client.options.getGamma();

        if (enabled && !smartview$wasEnabled) {
            // Fullbright wird gerade eingeschaltet: aktuellen Wert speichern
            smartview$savedGamma = gamma.value;
        } else if (!enabled && smartview$wasEnabled) {
            // Fullbright wird gerade ausgeschaltet: gespeicherten Wert wiederherstellen
            gamma.value = smartview$savedGamma;
            smartview$savedGamma = -1.0;
        }

        if (enabled) {
            gamma.value = 10.0;
        }

        smartview$wasEnabled = enabled;
    }
}
