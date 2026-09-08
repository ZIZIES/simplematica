package fi.dy.masa.simplematica.util;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;
import java.util.function.BiPredicate;
import javax.annotation.Nullable;
import com.google.common.annotations.VisibleForTesting;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;

import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Vec3i;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

import fi.dy.masa.malilib.gui.GuiBase;
import fi.dy.masa.malilib.gui.GuiTextInput;
import fi.dy.masa.malilib.gui.GuiTextInputStackedMultiLine;
import fi.dy.masa.malilib.gui.Message.MessageType;
import fi.dy.masa.malilib.interfaces.IStringDualConsumerFeedback;
import fi.dy.masa.malilib.util.GuiUtils;
import fi.dy.masa.malilib.util.InfoUtils;
import fi.dy.masa.malilib.util.data.tag.BaseData;
import fi.dy.masa.malilib.util.data.tag.CompoundData;
import fi.dy.masa.malilib.util.data.tag.ListData;
import fi.dy.masa.malilib.util.log.AnsiLogger;
import fi.dy.masa.malilib.util.position.LayerRange;
import fi.dy.masa.malilib.util.position.SubChunkPos;
import fi.dy.masa.simplematica.config.Configs;
import fi.dy.masa.simplematica.data.DataManager;
import fi.dy.masa.simplematica.data.SchematicHolder;
import fi.dy.masa.simplematica.gui.GuiSchematicSave;
import fi.dy.masa.simplematica.gui.GuiSchematicSave.InMemorySchematicCreator;
import fi.dy.masa.simplematica.mixin.entity.IMixinEntity;
import fi.dy.masa.simplematica.scheduler.TaskScheduler;
import fi.dy.masa.simplematica.scheduler.tasks.*;
import fi.dy.masa.simplematica.schematic.LitematicaSchematic;
import fi.dy.masa.simplematica.schematic.SchematicMetadata;
import fi.dy.masa.simplematica.schematic.container.LitematicaBlockStateContainer;
import fi.dy.masa.simplematica.schematic.placement.SchematicPlacement;
import fi.dy.masa.simplematica.schematic.placement.SchematicPlacementManager;
import fi.dy.masa.simplematica.schematic.placement.SchematicPlacementManager.PlacementPart;
import fi.dy.masa.simplematica.schematic.placement.SubRegionPlacement;
import fi.dy.masa.simplematica.schematic.placement.SubRegionPlacement.RequiredEnabled;
import fi.dy.masa.simplematica.selection.AreaSelection;
import fi.dy.masa.simplematica.selection.Box;
import fi.dy.masa.simplematica.selection.SelectionManager;
import fi.dy.masa.simplematica.tool.ToolMode;
import fi.dy.masa.simplematica.util.RayTraceUtils.RayTraceWrapper;
import fi.dy.masa.simplematica.world.SchematicWorldHandler;
import fi.dy.masa.simplematica.world.WorldSchematic;

public class SchematicUtils
{
    private static long areaMovedTime;

    public static boolean saveSchematic(boolean inMemoryOnly)
    {
        SelectionManager sm = DataManager.getSelectionManager();
        AreaSelection area = sm.getCurrentSelection();

        if (area != null)
        {
            if (inMemoryOnly)
            {
                String title = "simplematica.gui.title.create_in_memory_schematic";
                GuiTextInput gui = new GuiTextInput(512, title, area.getName(), GuiUtils.getCurrentScreen(), new InMemorySchematicCreator(area));
                GuiBase.openGui(gui);
            }
            else
            {
                GuiSchematicSave gui = new GuiSchematicSave();
                gui.setParent(GuiUtils.getCurrentScreen());
                GuiBase.openGui(gui);
            }

            return true;
        }

        return false;
    }

    public static void unloadCurrentlySelectedSchematic()
    {
        SchematicPlacement placement = DataManager.getSchematicPlacementManager().getSelectedSchematicPlacement();

        if (placement != null)
        {
            SchematicHolder.getInstance().removeSchematic(placement.getSchematic());
        }
        else
        {
            InfoUtils.showGuiOrInGameMessage(MessageType.ERROR, "simplematica.message.error.no_placement_selected");
        }
    }

    @VisibleForTesting
    public static void dumpDataBase(AnsiLogger LOGGER, BaseData tag)
    {
        LOGGER = LOGGER != null ? LOGGER : new AnsiLogger(SchematicUtils.class, true, true);

        ListData ld = tag.asList().orElse(new ListData());

        if (!ld.isEmpty())
        {
            dumpDataList(LOGGER, ld);
            return;
        }

        CompoundData cd = tag.asCompound().orElse(new CompoundData());

        if (!cd.isEmpty())
        {
            dumpDataCompound(LOGGER, cd);
            return;
        }

        LOGGER.debug("dumpEntityDataBase:: type: {}, value: {}", tag.getType(), tag.toString());
    }

    @VisibleForTesting
    public static void dumpDataList(AnsiLogger LOGGER, ListData tag)
    {
        LOGGER = LOGGER != null ? LOGGER : new AnsiLogger(SchematicUtils.class, true, true);

        if (tag.isEmpty())
        {
            LOGGER.warn("dumpEntityDataList:EMPTY: type: {}, value: {}", tag.getType(), tag.toString());
        }
        else
        {
            for (int i = 0; i < tag.size(); i++)
            {
                LOGGER.warn("dumpEntityDataList:{}: -->", i);
                BaseData d = tag.get(i);
                dumpDataBase(LOGGER, d);
            }
        }
    }

    @VisibleForTesting
    public static void dumpDataCompound(AnsiLogger LOGGER, CompoundData tag)
    {
        LOGGER = LOGGER != null ? LOGGER : new AnsiLogger(SchematicUtils.class, true, true);

        if (tag.isEmpty())
        {
            LOGGER.warn("dumpDataCompound:EMPTY: type: {}, value: {}", tag.getType(), tag.toString());
        }
        else
        {
            for (Map.Entry<String, BaseData> entry : tag.entrySet())
            {
                String key = entry.getKey();
                BaseData d = entry.getValue();

                LOGGER.error("dumpDataCompound:{}: -->", key);

                dumpDataBase(LOGGER, d);
            }
        }
    }
}
