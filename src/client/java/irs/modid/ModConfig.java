package irs.modid;

import me.shedaniel.autoconfig.ConfigData;
import me.shedaniel.autoconfig.annotation.Config;
import me.shedaniel.autoconfig.annotation.ConfigEntry;

@Config(name = "interior_rain_sound")
public class ModConfig implements ConfigData {
    @ConfigEntry.Category("general")
    @ConfigEntry.Gui.TransitiveObject
    @ConfigEntry.Gui.Tooltip
    @ConfigEntry.BoundedDiscrete(min = 8, max = 48)
    public int max_search_depth = 24;

    @ConfigEntry.Category("general")
    @ConfigEntry.Gui.Tooltip
    @ConfigEntry.BoundedDiscrete(min = 1, max = 20)
    public int cache_ticks = 10;

    @ConfigEntry.Category("general")
    @ConfigEntry.Gui.Tooltip
    public boolean debug_mode = false;

    @ConfigEntry.Category("biome_settings")
    @ConfigEntry.Gui.CollapsibleObject
    public BiomeSettings biome_settings = new BiomeSettings();

    public static class BiomeSettings {
        @ConfigEntry.Gui.Tooltip
        public boolean enabled = true;

        @ConfigEntry.Gui.Tooltip
        public boolean ignore_deserts = true;
    }
}