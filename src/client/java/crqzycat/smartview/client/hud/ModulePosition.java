package crqzycat.smartview.client.hud;

/**
 * Saved state for a single module: where it sits on screen and whether it's enabled.
 * Plain data holder, gets (de)serialized by Gson - keep field names stable.
 */
public class ModulePosition {

    public int x;
    public int y;
    public boolean enabled;
    /** Render scale applied via matrix transform. 1.0 = default size. */
    public float scale;
    /** Alpha of the module background (0 = invisible, 255 = fully opaque). */
    public int backgroundAlpha;

    public ModulePosition() {
        this(0, 0, true);
    }

    public ModulePosition(int x, int y, boolean enabled) {
        this.x = x;
        this.y = y;
        this.enabled = enabled;
        this.scale = 1.0f;
        this.backgroundAlpha = 128;
    }
}
