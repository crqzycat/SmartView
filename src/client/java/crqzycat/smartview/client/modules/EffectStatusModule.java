package crqzycat.smartview.client.modules;

import crqzycat.smartview.client.hud.HudModule;
import crqzycat.smartview.client.hud.ModulePosition;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.entity.effect.StatusEffectInstance;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class EffectStatusModule implements HudModule {

    private static final int PAD        = 4;
    private static final int ROW_HEIGHT = 14;
    private static final int WIDTH      = 130;

    @Override public String getId() { return "effect_status"; }
    @Override public String getDisplayName() { return "Effect Status"; }
    @Override public int getDefaultX() { return 10; }
    @Override public int getDefaultY() { return 130; }
    @Override public int getBaseWidth(MinecraftClient c) { return WIDTH; }

    @Override
    public int getBaseHeight() {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null) return ROW_HEIGHT;
        int count = client.player.getStatusEffects().size();
        return Math.max(1, count) * ROW_HEIGHT + PAD;
    }

    @Override
    public void render(DrawContext context, MinecraftClient client, int x, int y, ModulePosition pos) {
        if (client.player == null) return;
        Collection<StatusEffectInstance> effects = client.player.getStatusEffects();
        if (effects.isEmpty()) return;

        int h = effects.size() * ROW_HEIGHT + PAD;
        context.fill(x, y, x + WIDTH, y + h, pos.backgroundAlpha << 24);

        List<StatusEffectInstance> list = new ArrayList<>(effects);
        for (int i = 0; i < list.size(); i++) {
            StatusEffectInstance effect = list.get(i);
            int rowY = y + PAD / 2 + i * ROW_HEIGHT;

            // Effect name + amplifier
            String name = effect.getEffectType().value().getName().getString();
            int amp = effect.getAmplifier();
            if (amp > 0) name += " " + toRoman(amp + 1);

            // Duration
            String duration = effect.isInfinite() ? "∞" : formatDuration(effect.getDuration());

            // Colour from the effect's category
            int nameColor = switch (effect.getEffectType().value().getCategory()) {
                case BENEFICIAL -> 0xFF55FF55;
                case HARMFUL    -> 0xFFFF5555;
                default         -> 0xFFFFFFFF;
            };

            context.drawTextWithShadow(client.textRenderer, name, x + PAD, rowY, nameColor);
            int durationWidth = client.textRenderer.getWidth(duration);
            context.drawTextWithShadow(client.textRenderer, duration,
                x + WIDTH - durationWidth - PAD, rowY, 0xFFAAAAAA);
        }
    }

    private static String formatDuration(int ticks) {
        int secs = ticks / 20;
        return String.format("%d:%02d", secs / 60, secs % 60);
    }

    private static String toRoman(int n) {
        return switch (n) {
            case 2 -> "II"; case 3 -> "III"; case 4 -> "IV";
            case 5 -> "V";  case 6 -> "VI";  case 7 -> "VII";
            case 8 -> "VIII"; case 9 -> "IX"; case 10 -> "X";
            default -> String.valueOf(n);
        };
    }
    @Override public boolean enabledByDefault() { return false; }
}
