package fi.dy.masa.simplematica.render.schematic.ao;

import fi.dy.masa.simplematica.config.Configs;

public class AOLightmap
{
	public final AOBrightness brightnessCache = new AOBrightness();

	public void enableCache()
	{
		if (Configs.Visuals.RENDER_AO_MODERN_ENABLE.getBooleanValue())
		{
			this.brightnessCache.enable();
		}
	}

	public void disableCache()
	{
		if (Configs.Visuals.RENDER_AO_MODERN_ENABLE.getBooleanValue())
		{
			this.brightnessCache.disable();
		}
	}
}
