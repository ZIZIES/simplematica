package fi.dy.masa.litematica.materials;

import java.util.List;
import javax.annotation.Nullable;

import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;

import net.minecraft.client.Minecraft;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockState;

import fi.dy.masa.litematica.Litematica;
import fi.dy.masa.litematica.config.Configs;
import fi.dy.masa.litematica.data.DataManager;
import fi.dy.masa.litematica.scheduler.TaskScheduler;
import fi.dy.masa.litematica.scheduler.tasks.TaskCountBlocksPlacement;
import fi.dy.masa.litematica.schematic.placement.SchematicPlacement;
import fi.dy.masa.litematica.util.BlockInfoListType;
import fi.dy.masa.malilib.gui.Message.MessageType;
import fi.dy.masa.malilib.util.InfoUtils;
import fi.dy.masa.malilib.util.position.LayerRange;
import fi.dy.masa.malilib.util.StringUtils;

public class MaterialListPlacement extends MaterialListBase
{
    private final SchematicPlacement placement;

    @Nullable private LayerMaterialCounts layerCounts;
    private int lastLayerMin = Integer.MIN_VALUE;
    private int lastLayerMax = Integer.MIN_VALUE;

    public MaterialListPlacement(SchematicPlacement placement)
    {
        this(placement, false);
    }

    public MaterialListPlacement(SchematicPlacement placement, boolean reCreate)
    {
        super();

        this.placement = placement;

        if (reCreate)
        {
            this.reCreateMaterialList();
        }
    }

    @Override
    public boolean supportsRenderLayers()
    {
        return true;
    }

    @Override
    public String getName()
    {
        return this.placement.getName();
    }

    @Override
    public String getTitle()
    {
        BlockInfoListType type = this.getMaterialListType();

        if (type.isPerLayer() && this.isPerLayerUsable())
        {
            LayerRange range = DataManager.getRenderLayerRange();
            String key = type == BlockInfoListType.CURRENT_LAYER
                    ? "litematica.gui.title.material_list.placement.current_layer"
                    : "litematica.gui.title.material_list.placement.layer_and_below";

            return StringUtils.translate(key, this.getName(), range.getMaxLayerBoundary());
        }

        return StringUtils.translate("litematica.gui.title.material_list.placement", this.getName());
    }

    @Override
    public void reCreateMaterialList()
    {
        if (this.getMaterialListType().isPerLayer() && this.isPerLayerUsable())
        {
            this.reCreateFromLayerTally();
            return;
        }

        boolean ignoreState = Configs.Generic.MATERIAL_LIST_IGNORE_STATE.getBooleanValue();
        TaskCountBlocksPlacement task = new TaskCountBlocksPlacement(this.placement, this, ignoreState);
        TaskScheduler.getInstanceClient().scheduleTask(task, 20);
        InfoUtils.showGuiOrInGameMessage(MessageType.INFO, "litematica.message.scheduled_task_added");
    }

    /**
     * Rebuilds the list when the player has moved to a different render layer, so the panel
     * follows the layer instead of waiting for a manual refresh. Cheap enough to call per frame:
     * it does nothing until the layer boundaries actually move.
     */
    public boolean refreshIfLayerChanged()
    {
        if (this.getMaterialListType().isPerLayer() == false || this.isPerLayerUsable() == false)
        {
            return false;
        }

        LayerRange range = DataManager.getRenderLayerRange();

        if (range.getMinLayerBoundary() == this.lastLayerMin && range.getMaxLayerBoundary() == this.lastLayerMax)
        {
            return false;
        }

        this.reCreateFromLayerTally();

        return true;
    }

    /**
     * The tally buckets by Y only, because a placement rotation swaps X and Z but nothing can
     * tip a schematic on its side. An X or Z layer therefore has to go through the counting
     * task, which does the full transform.
     */
    private boolean isPerLayerUsable()
    {
        return DataManager.getRenderLayerRange().getAxis() == Direction.Axis.Y;
    }

    private void reCreateFromLayerTally()
    {
        if (this.layerCounts == null || this.layerCounts.matches(this.placement) == false)
        {
            this.layerCounts = LayerMaterialCounts.createFor(this.placement);
        }

        LayerRange range = DataManager.getRenderLayerRange();
        this.lastLayerMin = range.getMinLayerBoundary();
        this.lastLayerMax = range.getMaxLayerBoundary();

        if (this.layerCounts == null)
        {
            this.setMaterialListEntries(List.of());
            return;
        }

        // CURRENT_LAYER mirrors exactly what the render layer is showing, which covers both a
        // single slice and a layer range. LAYER_AND_BELOW extends the bottom down to the
        // schematic floor, for building upwards.
        final int fromY = this.getMaterialListType() == BlockInfoListType.LAYER_AND_BELOW
                ? this.layerCounts.getMinY()
                : this.lastLayerMin;

        Object2IntOpenHashMap<BlockState> countsTotal = this.layerCounts.getCountsForRange(fromY, this.lastLayerMax);
        Minecraft mc = Minecraft.getInstance();

        // This is a requirements view, not a progress view: it says what the layer needs, and
        // the player's inventory supplies the "available" column. Comparing against the blocks
        // already placed in the world is what the task-backed modes are for.
        this.setMaterialListEntries(MaterialListUtils.getMaterialList(
                countsTotal, countsTotal.clone(), new Object2IntOpenHashMap<>(), mc.player));

        if (Configs.Generic.DEBUG_LOGGING.getBooleanValue())
        {
            this.logLayerTally(fromY);
        }
    }

    /**
     * Dumps the tally and checks the invariant that the per-layer buckets add back up to the
     * whole-schematic total. A mismatch means the container-to-world Y mapping has drifted.
     */
    private void logLayerTally(int fromY)
    {
        long summed = 0;

        for (int y = this.layerCounts.getMinY(); y <= this.layerCounts.getMaxY(); ++y)
        {
            Object2IntOpenHashMap<BlockState> layer = this.layerCounts.getCountsForLayer(y);
            long layerTotal = 0;

            for (BlockState state : layer.keySet())
            {
                layerTotal += layer.getInt(state);
            }

            summed += layerTotal;

            if (layerTotal > 0)
            {
                Litematica.LOGGER.info("material list: y={} -> {} block(s), {} distinct state(s)",
                                       y, layerTotal, layer.size());
            }
        }

        Litematica.LOGGER.info("material list: layers y={}..{} ({} populated), showing y={}..{}",
                               this.layerCounts.getMinY(), this.layerCounts.getMaxY(),
                               this.layerCounts.getLayerCount(), fromY, this.lastLayerMax);

        if (summed != this.layerCounts.getTotalBlocks())
        {
            Litematica.LOGGER.error("material list: per-layer counts sum to {} but the schematic holds {} -- the Y mapping is wrong",
                                    summed, this.layerCounts.getTotalBlocks());
        }
    }
}
