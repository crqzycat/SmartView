package crqzycat.smartview.mixin.client;

import crqzycat.smartview.client.hud.ModuleManager;
import net.minecraft.client.option.SimpleOption;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.lang.reflect.Field;

@Mixin(SimpleOption.class)
public class GammaMixin {

    @Unique
    private static Field smartview$keyField = null;

    @Unique
    private static boolean smartview$keyFieldResolved = false;

    /**
     * Intercepts SimpleOption#getValue().
     * Only returns 10.0 for the gamma option (key = "options.gamma").
     */
    @SuppressWarnings("unchecked")
    @Inject(method = "getValue", at = @At("RETURN"), cancellable = true)
    private void smartview$interceptGetValue(CallbackInfoReturnable<Object> cir) {
        if (!ModuleManager.isFullbrightEnabled()) return;

        // Resolve the 'key' field once via reflection
        if (!smartview$keyFieldResolved) {
            smartview$keyFieldResolved = true;
            for (Field f : SimpleOption.class.getDeclaredFields()) {
                if (f.getType() == String.class) {
                    f.setAccessible(true);
                    smartview$keyField = f;
                    break;
                }
            }
        }

        if (smartview$keyField == null) return;

        try {
            String key = (String) smartview$keyField.get(this);
            if ("options.gamma".equals(key)) {
                cir.setReturnValue(10.0);
            }
        } catch (Exception ignored) {}
    }
}
