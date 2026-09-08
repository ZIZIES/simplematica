package fi.dy.masa.simplematica.util;

import com.google.common.collect.ImmutableList;

import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.ChunkPos;

import fi.dy.masa.malilib.interfaces.IRangeChangeListener;
import fi.dy.masa.simplematica.data.DataManager;
import fi.dy.masa.simplematica.materials.MaterialListBase;
import fi.dy.masa.simplematica.materials.MaterialListPlacement;
import fi.dy.masa.simplematica.world.SchematicWorldHandler;
import fi.dy.masa.simplematica.world.WorldSchematic;

public class SchematicWorldRefresher implements IRangeChangeListener
{
    public static final SchematicWorldRefresher INSTANCE = new SchematicWorldRefresher();

    private final Minecraft mc = Minecraft.getInstance();

    /**
     * Keeps a per-layer material list in step with the render layer. This listener already
     * fires on every layer change, so the list follows the layer instead of needing a manual
     * refresh. It is a no-op unless a per-layer list is actually open.
     */
    private void refreshPerLayerMaterialList()
    {
        MaterialListBase materialList = DataManager.getMaterialList();

        if (materialList instanceof MaterialListPlacement placementList)
        {
            placementList.refreshIfLayerChanged();
        }
    }

    @Override
    public void updateAll()
    {
        this.refreshPerLayerMaterialList();
        WorldSchematic world = SchematicWorldHandler.getSchematicWorld();

        if (world != null && this.mc.level != null)
        {
//            PlacementManagerDaemonHandler.INSTANCE.clearAllTasks();
            DataManager.getSchematicPlacementManager().setVisibleSubChunksNeedsUpdate();
            final int minY = world.getMinY();
            final int maxY = world.getMaxY() - 1;
            this.updateBetweenY(minY, maxY);
        }
    }

    @Override
    public void updateBetweenX(int minX, int maxX)
    {
        WorldSchematic world = SchematicWorldHandler.getSchematicWorld();

        if (world != null && this.mc.level != null)
        {
            DataManager.getSchematicPlacementManager().setVisibleSubChunksNeedsUpdate();
            ImmutableList<ChunkPos> keySet = world.getChunkSource().getLoadedNonEmptyChunkPosSet();

            final int cxMin = (Math.min(minX, maxX) >> 4);
            final int cxMax = (Math.max(minX, maxX) >> 4);

            for (ChunkPos pos : keySet)
            {
                // Only mark chunks that are actually rendered (if the schematic world contains more chunks)
                if (pos.x() >= cxMin && pos.x() <= cxMax &&
                    WorldUtils.isClientChunkLoaded(this.mc.level, pos.x(), pos.z()))
                {
                    world.scheduleChunkRenders(pos.x(), pos.z());
                }
            }
        }
    }

    @Override
    public void updateBetweenY(int minY, int maxY)
    {
        this.refreshPerLayerMaterialList();

        WorldSchematic world = SchematicWorldHandler.getSchematicWorld();

        if (world != null && this.mc.level != null)
        {
            DataManager.getSchematicPlacementManager().setVisibleSubChunksNeedsUpdate();
            ImmutableList<ChunkPos> keySet = world.getChunkSource().getLoadedNonEmptyChunkPosSet();

            for (ChunkPos pos : keySet)
            {
                // Only mark chunks that are actually rendered (if the schematic world contains more chunks)
                if (WorldUtils.isClientChunkLoaded(this.mc.level, pos.x(), pos.z()))
                {
                    world.scheduleChunkRenders(pos.x(), pos.z());
                }
            }
        }
    }

    @Override
    public void updateBetweenZ(int minZ, int maxZ)
    {
        WorldSchematic world = SchematicWorldHandler.getSchematicWorld();

        if (world != null && this.mc.level != null)
        {
            DataManager.getSchematicPlacementManager().setVisibleSubChunksNeedsUpdate();
            ImmutableList<ChunkPos> keySet = world.getChunkSource().getLoadedNonEmptyChunkPosSet();
            final int czMin = (Math.min(minZ, maxZ) >> 4);
            final int czMax = (Math.max(minZ, maxZ) >> 4);

            for (ChunkPos pos : keySet)
            {
                // Only mark chunks that are actually rendered (if the schematic world contains more chunks)
                if (pos.z() >= czMin && pos.z() <= czMax &&
                    WorldUtils.isClientChunkLoaded(this.mc.level, pos.x(), pos.z()))
                {
                    world.scheduleChunkRenders(pos.x(), pos.z());
                }
            }
        }
    }

    public void markSchematicChunksForRenderUpdate(int chunkX, int chunkZ)
    {
        WorldSchematic world = SchematicWorldHandler.getSchematicWorld();

        if (world != null && this.mc.level != null)
        {
            if (world.getChunkSource().hasChunk(chunkX, chunkZ) &&
                WorldUtils.isClientChunkLoaded(this.mc.level, chunkX, chunkZ))
            {
                world.scheduleChunkRenders(chunkX, chunkZ);
            }
        }
    }

    public void markSchematicChunkForRenderUpdate(BlockPos pos)
    {
        WorldSchematic world = SchematicWorldHandler.getSchematicWorld();

        if (world != null && this.mc.level != null)
        {
            int chunkX = pos.getX() >> 4;
            int chunkZ = pos.getZ() >> 4;
            //Simplematica.debugLog("SchematicWorldRefresher#markSchematicChunkForRenderUpdate({}, {})", chunkX, chunkZ);

            if (world.getChunkSource().hasChunk(chunkX, chunkZ) &&
                WorldUtils.isClientChunkLoaded(this.mc.level, chunkX, chunkZ))
            {
                world.scheduleChunkRenders(chunkX, chunkZ);
            }
        }
    }
}
