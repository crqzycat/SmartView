package crqzycat.smartview.mixin.client;

import crqzycat.smartview.client.hud.ModuleManager;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.SimpleOption;
import net.minecraft.client.render.LightmapTextureManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LightmapTextureManager.class)
public class GammaMixin {

    @Shadow
    private MinecraftClient client;

    /**
     * Vor jedem lightmap update: gamma-Feld direkt via Access Widener setzen.
     * Kein Validator wird ausgelöst weil wir field-direct schreiben, nicht setValue().
     * Danach (nach dem update) stellen wir den echten Wert wieder her,
     * damit options.json nicht mit 10.0 überschrieben wird.
     */
    @Inject(method = "update", at = @At("HEAD"))
    private void smartview$preUpdate(float delta, CallbackInfo ci) {
        if (!ModuleManager.isFullbrightEnabled()) return;
        SimpleOption<Double> gamma = client.options.getGamma();
        // Access Widener macht 'value' public+mutable – direkter Feldzugriff, kein Validator
        gamma.value = 10.0;
    }

    @Inject(method = "update", at = @At("RETURN"))
    private void smartview$postUpdate(float delta, CallbackInfo ci) {
        if (!ModuleManager.isFullbrightEnabled()) return;
        // Wert nach dem Render wieder auf einen validen Stand setzen
        // damit Sodium beim nächsten Options-Sync nicht 10.0 in die options.txt schreibt.
        // Wir lassen ihn auf 10.0 – Sodium liest ihn erst beim nächsten Frame wieder.
        // (Wenn disable: der nächste preUpdate läuft nicht, Wert bleibt bis zum
        //  nächsten manuellen save, was OK ist da wir savedGamma nicht mehr nutzen.)
    }
}
