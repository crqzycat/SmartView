package crqzycat.smartview.client.hud;

public class ModulePosition {

    public int     x;
    public int     y;
    public boolean enabled;
    public float   scale;
    public int     backgroundAlpha;
    /** ARGB text color for this module. Default = opaque white. */
    public int     textColor = 0xFFFFFFFF;

    public ModulePosition() {
        this(0, 0, true);
    }

    public ModulePosition(int x, int y, boolean enabled) {
        this.x = x;
        this.y = y;
        this.enabled = enabled;
        this.scale = 1.0f;
        this.backgroundAlpha = 0;
        this.textColor = 0xFFFFFFFF;
    }
}
