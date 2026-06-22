package crqzycat.smartview.client.feature;

import net.minecraft.client.MinecraftClient;

/**
 * Fullbright: sets gamma to 10.0 while active, restores the previous value on disable.
 * Uses the public GameOptions.getGamma().setValue() API available in 1.21.11.
 */
public final class FullbrightFeature {

    private static final double FULLBRIGHT_GAMMA = 10.0;

    private static boolean enabled   = false;
    private static double  savedGamma = 1.0;

    private FullbrightFeature() {}

    public static boolean isEnabled() { return enabled; }

    public static void setEnabled(boolean on) {
        if (on == enabled) return;
        MinecraftClient client = MinecraftClient.getInstance();
        if (on) {
            savedGamma = client.options.getGamma().getValue();
            client.options.getGamma().setValue(FULLBRIGHT_GAMMA);
        } else {
            client.options.getGamma().setValue(savedGamma);
        }
        enabled = on;
    }

    public static void toggle() { setEnabled(!enabled); }
}
