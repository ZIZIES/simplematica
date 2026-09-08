package fi.dy.masa.litematica.tool;

import javax.annotation.Nonnull;
import net.minecraft.util.StringRepresentable;
import com.google.common.collect.ImmutableList;

import com.mojang.serialization.Codec;
import fi.dy.masa.malilib.util.StringUtils;

public enum ToolMode implements StringRepresentable
{
    AREA_SELECTION      ("area_selection",      "litematica.tool_mode.name.area_selection",        false),
    SCHEMATIC_PLACEMENT ("schematic_placement", "litematica.tool_mode.name.schematic_placement",   true);

    public static final StringRepresentable.EnumCodec<ToolMode> CODEC = StringRepresentable.fromEnum(ToolMode::values);
    public static final ImmutableList<ToolMode> VALUES = ImmutableList.copyOf(values());

    private final String configString;
    private final String unlocName;
    private final boolean usesSchematic;

    ToolMode(String configName, String unlocName, boolean usesSchematic)
    {
        this.configString = configName;
        this.unlocName = unlocName;
        this.usesSchematic = usesSchematic;
    }

    public Codec<ToolMode> codec()
    {
        return CODEC;
    }

    @Override
    public @Nonnull String getSerializedName()
    {
        return this.configString;
    }

    public boolean getUsesSchematic()
    {
        return this.usesSchematic;
    }

    public boolean getUsesAreaSelection()
    {
        return this.getUsesSchematic() == false;
    }

    public String getName()
    {
        return StringUtils.translate(this.unlocName);
    }

    public ToolMode cycle(boolean forward)
    {
        ToolMode[] values = ToolMode.values();
        int nextId = this.ordinal() + (forward ? 1 : -1);

        if (nextId < 0)
        {
            nextId = values.length - 1;
        }
        else if (nextId >= values.length)
        {
            nextId = 0;
        }

        return values[nextId];
    }
}
