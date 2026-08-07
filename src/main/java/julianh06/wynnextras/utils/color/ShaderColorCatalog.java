package julianh06.wynnextras.utils.color;

public class ShaderColorCatalog {
    public static final ShaderColor OCEAN_FADE = new ShaderColor()
            .add(0x00FFFF)
            .add(0x9BD8FF)
            .add(0x4B0082)
            .fadeSpeed(0.6f);

    public static final ShaderColor BLACK_WHITE = new ShaderColor()
            .add(0x000000)
            .add(0xFFFFFF)
            .fadeSpeed(0.4f);

    public static final ShaderColor SUNSET = new ShaderColor()
            .add(0xFFBF69)
            .add(0xFF6B6B)
            .add(0xC95CFF)
            .fadeSpeed(0.45f);

    public static final ShaderColor AURORA = new ShaderColor()
            .add(0x6DFFB8)
            .add(0x00FFFF)
            .add(0x4B0082)
            .fadeSpeed(0.5f);

    public static final ShaderColor COTTON_CANDY = new ShaderColor()
            .add(0xFFC5D3)
            .add(0x9BD8FF)
            .add(0xC95CFF)
            .fadeSpeed(0.4f);

    public static final ShaderColor EMBER = new ShaderColor()
            .add(0xFFF06A)
            .add(0xFF6B00)
            .add(0xBB1111)
            .fadeSpeed(0.7f);

    public static final ShaderColor FOREST = new ShaderColor()
            .add(0x96db25)
            .add(0x105c10)
            .fadeSpeed(0.35f);
}