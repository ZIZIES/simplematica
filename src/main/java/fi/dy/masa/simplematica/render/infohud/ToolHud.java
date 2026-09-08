package fi.dy.masa.simplematica.render.infohud;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import fi.dy.masa.simplematica.data.EntityDataManager;
import fi.dy.masa.simplematica.config.Configs;
import fi.dy.masa.simplematica.data.DataManager;
import fi.dy.masa.simplematica.schematic.placement.SchematicPlacement;
import fi.dy.masa.simplematica.schematic.placement.SubRegionPlacement;
import fi.dy.masa.simplematica.selection.AreaSelection;
import fi.dy.masa.simplematica.selection.Box;
import fi.dy.masa.simplematica.selection.SelectionManager;
import fi.dy.masa.simplematica.selection.SelectionMode;
import fi.dy.masa.simplematica.tool.ToolMode;
import fi.dy.masa.simplematica.util.EntityUtils;
import fi.dy.masa.simplematica.util.PasteLayerBehavior;
import fi.dy.masa.simplematica.util.PositionUtils;
import fi.dy.masa.simplematica.util.ReplaceBehavior;
import fi.dy.masa.malilib.config.HudAlignment;
import fi.dy.masa.malilib.gui.GuiBase;
import fi.dy.masa.malilib.util.game.BlockUtils;
import fi.dy.masa.malilib.util.StringUtils;

public class ToolHud extends InfoHud
{
    private static final ToolHud INSTANCE = new ToolHud();

    public static final Date DATE = new Date();
    public static final SimpleDateFormat SIMPLE_DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

    protected ToolHud()
    {
        super();
    }

    public static ToolHud getInstance()
    {
        return INSTANCE;
    }

    @Override
    protected boolean shouldRender()
    {
        return true;
    }

    protected boolean hasEnabledTool()
    {
        return Configs.Generic.TOOL_ITEM_ENABLED.getBooleanValue() && EntityUtils.hasToolItem(this.mc.player);
    }

    @Override
    protected HudAlignment getHudAlignment()
    {
        return (HudAlignment) Configs.InfoOverlays.TOOL_HUD_ALIGNMENT.getOptionListValue();
    }

    @Override
    protected double getScaleFactor()
    {
        return Configs.InfoOverlays.TOOL_HUD_SCALE.getDoubleValue();
    }

    @Override
    protected int getOffsetX()
    {
        return Configs.InfoOverlays.TOOL_HUD_OFFSET_X.getIntegerValue();
    }

    @Override
    protected int getOffsetY()
    {
        return Configs.InfoOverlays.TOOL_HUD_OFFSET_Y.getIntegerValue();
    }

    @Override
    protected void updateHudText()
    {
        String str;
        String green = GuiBase.TXT_GREEN;
        String rst = GuiBase.TXT_RST;
        boolean hasTool = this.hasEnabledTool();

        List<String> lines = this.lineList;


        ToolMode mode = DataManager.getToolMode();
        String orange = GuiBase.TXT_GOLD;
        String red = GuiBase.TXT_RED;
        String white = GuiBase.TXT_WHITE;
        String aqua = GuiBase.TXT_AQUA;
        String strYes = green + StringUtils.translate("simplematica.label.yes") + rst;
        String strNo = GuiBase.TXT_RED + StringUtils.translate("simplematica.label.no") + rst;

        if (hasTool && mode.getUsesAreaSelection())
        {
            SelectionManager sm = DataManager.getSelectionManager();
            AreaSelection selection = sm.getCurrentSelection();

            if (selection != null)
            {
                String name = green + selection.getName() + rst;

                if (sm.getSelectionMode() == SelectionMode.NORMAL)
                {
                    lines.add(StringUtils.translate("simplematica.hud.area_selection.selected_area_normal", name));
                }
                else
                {
                    lines.add(StringUtils.translate("simplematica.hud.area_selection.selected_area_simple", name));
                }

                String strOr;
                BlockPos o = selection.getExplicitOrigin();

                if (o == null)
                {
                    o = selection.getEffectiveOrigin();
                    strOr = StringUtils.translate("simplematica.gui.label.origin.auto");
                }
                else
                {
                    strOr = StringUtils.translate("simplematica.gui.label.origin.manual");
                }
                int count = selection.getAllSubRegionBoxes().size();

                str = String.format("%d, %d, %d %s[%s%s%s]", o.getX(), o.getY(), o.getZ(), rst, orange, strOr, rst);
                String strOrigin = StringUtils.translate("simplematica.hud.area_selection.origin", green + str + rst);
                String strBoxes = StringUtils.translate("simplematica.hud.area_selection.box_count", green + count + rst);

                lines.add(strOrigin + " - " + strBoxes);

                String subRegionName = selection.getCurrentSubRegionBoxName();
                Box box = selection.getSelectedSubRegionBox();

                if (subRegionName != null && box != null)
                {
                    lines.add(StringUtils.translate("simplematica.hud.area_selection.selected_sub_region", green + subRegionName + rst));
                    BlockPos p1 = box.getPos1();
                    BlockPos p2 = box.getPos2();

                    if (p1 != null && p2 != null)
                    {
                        BlockPos size = PositionUtils.getAreaSizeFromRelativeEndPositionAbs(p2.subtract(p1));
                        String strDim = green + String.format("%dx%dx%d", size.getX(), size.getY(), size.getZ()) + rst;
                        String strp1 = green + String.format("%d, %d, %d", p1.getX(), p1.getY(), p1.getZ()) + rst;
                        String strp2 = green + String.format("%d, %d, %d", p2.getX(), p2.getY(), p2.getZ()) + rst;
                        lines.add(StringUtils.translate("simplematica.hud.area_selection.dimensions_position", strDim, strp1, strp2));
                    }
                }
            }

            str = green + Configs.Generic.SELECTION_CORNERS_MODE.getOptionListValue().getDisplayName() + rst;
            lines.add(StringUtils.translate("simplematica.hud.area_selection.selection_corners_mode", str));
        }
        else if (hasTool && mode.getUsesSchematic())
        {
            SchematicPlacement schematicPlacement = DataManager.getSchematicPlacementManager().getSelectedSchematicPlacement();

            if (schematicPlacement != null)
            {
                str = StringUtils.translate("simplematica.hud.schematic_placement.selected_placement");
                lines.add(String.format("%s: %s%s%s", str, green, schematicPlacement.getName(), rst));

                str = StringUtils.translate("simplematica.hud.schematic_placement.sub_region_count");
                int count = schematicPlacement.getSubRegionCount();
                String strCount = String.format("%s: %s%d%s", str, green, count, rst);

                str = StringUtils.translate("simplematica.hud.schematic_placement.sub_regions_modified");
                String strTmp = schematicPlacement.isRegionPlacementModified() ? strYes : strNo;
                lines.add(strCount + String.format(" - %s: %s", str, strTmp));

                BlockPos or = schematicPlacement.getOrigin();
                str = String.format("%d, %d, %d", or.getX(), or.getY(), or.getZ());

                lines.add(StringUtils.translate("simplematica.hud.area_selection.origin", green + str + rst));

                SubRegionPlacement placement = schematicPlacement.getSelectedSubRegionPlacement();

                if (placement != null)
                {
                    String areaName = placement.getName();
                    str = StringUtils.translate("simplematica.hud.schematic_placement.selected_sub_region");
                    String str2 = StringUtils.translate("simplematica.hud.schematic_placement.sub_region_modified");
                    strTmp = placement.isRegionPlacementModifiedFromDefault() ? strYes : strNo;
                    lines.add(String.format("%s: %s%s%s - %s: %s", str, green, areaName, rst, str2, strTmp));

                    or = placement.getPos();
                    or = PositionUtils.getTransformedBlockPos(or, schematicPlacement.getMirror(), schematicPlacement.getRotation());
                    or = or.offset(schematicPlacement.getOrigin());
                    str = String.format("%d, %d, %d", or.getX(), or.getY(), or.getZ());
                    lines.add(StringUtils.translate("simplematica.hud.schematic_placement.sub_region_origin", green + str + rst));
                }

            }
            else
            {
                String strTmp = "<" + StringUtils.translate("simplematica.label.none_lower") + ">";
                str = StringUtils.translate("simplematica.hud.schematic_placement.selected_placement");
                lines.add(String.format("%s: %s%s%s", str, white, strTmp, rst));
            }
        }

        if (hasTool)
        {
            str = StringUtils.translate("simplematica.hud.selected_mode");
            String modeName = mode.getName();

            lines.add(String.format("%s [%s%d%s/%s%d%s]: %s%s%s", str, green, mode.ordinal() + 1, white,
                    green, ToolMode.values().length, white, green, modeName, rst));
        }
    }

    protected String getBlockString(BlockState state)
    {
        String strBlock;

        String green = GuiBase.TXT_GREEN;
        String rst = GuiBase.TXT_RST;

        strBlock = green + state.getBlock().getName().getString() + rst;
        Optional<Direction> facing = BlockUtils.getFirstPropertyFacingValue(state);

        if (facing.isPresent())
        {
            String gold = GuiBase.TXT_GOLD;
            String strFacing = gold + facing.get().name().toLowerCase() + rst;
            strBlock += " - " + StringUtils.translate("simplematica.tool_hud.facing", strFacing);
        }

        return strBlock;
    }
}
