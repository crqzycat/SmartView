package crqzycat.smartview.mixin.client;

import crqzycat.smartview.client.hud.ModuleManager;
import net.minecraft.client.option.GameOptions;
import net.minecraft.client.option.SimpleOption;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.lang.reflect.Field;

@Mixin(GameOptions.class)
public class GammaMixin {

    @Shadow
    public SimpleOption<Double> gamma;

    /**
     * Inject after getGamma() returns. If Fullbright is active, we bypass the
     * SimpleOption validator by writing directly to the private 'value' field
     * via reflection. setValue() is rejected because DoubleSliderCallbacks only
     * accepts [0.0, 1.0] in 1.21.x.
     */
    @Inject(method = "getGamma", at = @At("RETURN"))
    private void smartview$overrideGamma(CallbackInfoReturnable<SimpleOption<Double>> cir) {
        if (!ModuleManager.isFullbrightEnabled()) return;
        try {
            SimpleOption<Double> opt = cir.getReturnValue();
            Field valueField = null;
            // Walk the class hierarchy to find the field (obfuscated name may vary)
            for (Field f : SimpleOption.class.getDeclaredFields()) {
                if (f.getType() == Object.class || f.getGenericType().toString().contains("Object")) {
                    // Skip – too generic
                }
                // The value field holds the current option value as type T
                if (f.getName().equals("value") || f.getName().equals("field_25136")) {
                    valueField = f;
                    break;
                }
            }
            // Fallback: iterate all fields and pick the one whose value is a Double
            if (valueField == null) {
                for (Field f : SimpleOption.class.getDeclaredFields()) {
                    f.setAccessible(true);
                    Object val = f.get(opt);
                    if (val instanceof Double) {
                        valueField = f;
                        break;
                    }
                }
            }
            if (valueField != null) {
                valueField.setAccessible(true);
                valueField.set(opt, 10.0);
            }
        } catch (Exception e) {
            // Silently ignore – worst case fullbright just won't work
        }
    }
}
