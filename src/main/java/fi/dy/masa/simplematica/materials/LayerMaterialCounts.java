package fi.dy.masa.simplematica.materials;

import javax.annotation.Nullable;

import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

import fi.dy.masa.simplematica.schematic.LitematicaSchematic;
import fi.dy.masa.simplematica.schematic.container.LitematicaBlockStateContainer;
import fi.dy.masa.simplematica.schematic.placement.SchematicPlacement;
import fi.dy.masa.simplematica.schematic.placement.SubRegionPlacement;
import fi.dy.masa.simplematica.util.PositionUtils;

/**
 * A per-Y-layer tally of the blocks in a schematic placement.
 *
 * <p>The stock material list is one flat total for the whole schematic, so it cannot answer
 * "what do I need for the layer I am standing on". This walks each sub-region's block state
 * container once and buckets the counts by world Y, which makes slicing a layer afterwards a
 * map lookup rather than a re-scan.
 *
 * <p>Only Y is bucketed. Render layers can also run along X or Z, but a placement rotation
 * remaps those two onto each other, whereas nothing in a Minecraft placement can tip a
 * schematic on its side: rotations are all about the Y axis and both mirrors are in vertical
 * planes. Y therefore survives the transform untouched and the container-to-world mapping
 * stays plain arithmetic. Callers wanting an X or Z layer should fall back to the block
 * counting task, which does the full transform.
 */
public class LayerMaterialCounts
{
    private final Int2ObjectOpenHashMap<Object2IntOpenHashMap<BlockState>> countsByLayer = new Int2ObjectOpenHashMap<>();
    private final Signature signature;
    private int minY = Integer.MAX_VALUE;
    private int maxY = Integer.MIN_VALUE;
    private long totalBlocks;

    private LayerMaterialCounts(Signature signature)
    {
        this.signature = signature;
    }

    /**
     * Builds the tally for the given placement. Returns null if the placement has no schematic
     * or no enabled sub-region with readable block data.
     */
    @Nullable
    public static LayerMaterialCounts createFor(SchematicPlacement placement)
    {
        LitematicaSchematic schematic = placement != null ? placement.getSchematic() : null;

        if (schematic == null)
        {
            return null;
        }

        LayerMaterialCounts counts = new LayerMaterialCounts(Signature.of(placement));
        final int originY = placement.getOrigin().getY();

        for (String regionName : schematic.getAreaPositions().keySet())
        {
            SubRegionPlacement subRegion = placement.getRelativeSubRegionPlacement(regionName);

            if (subRegion == null || subRegion.isEnabled() == false)
            {
                continue;
            }

            LitematicaBlockStateContainer container = schematic.getSubRegionContainer(regionName);
            Vec3i regionSize = schematic.getAreaSizeAsVec3i(regionName);

            if (container == null || regionSize == null)
            {
                continue;
            }

            // A sub-region's stored position is whichever corner it was selected from, so the
            // size can be negative on any axis. Resolve the minimum corner the same way the
            // placement code does, so layer 0 of the container lands on the same world Y that
            // the hologram puts it at.
            BlockPos regionPos = subRegion.getPos();
            BlockPos regionEnd = new BlockPos(PositionUtils.getRelativeEndPositionFromAreaSize(regionSize)).offset(regionPos);
            final int baseY = originY + PositionUtils.getMinCorner(regionPos, regionEnd).getY();

            counts.addRegion(container, baseY);
        }

        return counts.totalBlocks > 0 ? counts : null;
    }

    private void addRegion(LitematicaBlockStateContainer container, int baseY)
    {
        Vec3i size = container.getSize();
        final int sizeX = size.getX();
        final int sizeY = size.getY();
        final int sizeZ = size.getZ();
        final BlockState structureVoid = Blocks.STRUCTURE_VOID.defaultBlockState();

        for (int y = 0; y < sizeY; ++y)
        {
            final int worldY = baseY + y;
            Object2IntOpenHashMap<BlockState> layer = null;

            for (int z = 0; z < sizeZ; ++z)
            {
                for (int x = 0; x < sizeX; ++x)
                {
                    BlockState state = container.get(x, y, z);

                    // Air and structure void are not materials anyone has to carry.
                    if (state.isAir() || state == structureVoid)
                    {
                        continue;
                    }

                    if (layer == null)
                    {
                        layer = this.countsByLayer.computeIfAbsent(worldY, k -> new Object2IntOpenHashMap<>());
                    }

                    layer.addTo(state, 1);
                    ++this.totalBlocks;
                }
            }

            if (layer != null)
            {
                this.minY = Math.min(this.minY, worldY);
                this.maxY = Math.max(this.maxY, worldY);
            }
        }
    }

    /** Counts for a single world Y. Never null; an empty layer returns an empty map. */
    public Object2IntOpenHashMap<BlockState> getCountsForLayer(int worldY)
    {
        Object2IntOpenHashMap<BlockState> layer = this.countsByLayer.get(worldY);

        return layer != null ? layer : new Object2IntOpenHashMap<>();
    }

    /** Counts summed over an inclusive world Y range, for the "this layer and everything below" view. */
    public Object2IntOpenHashMap<BlockState> getCountsForRange(int fromY, int toY)
    {
        Object2IntOpenHashMap<BlockState> summed = new Object2IntOpenHashMap<>();
        final int start = Math.max(Math.min(fromY, toY), this.minY);
        final int end = Math.min(Math.max(fromY, toY), this.maxY);

        for (int y = start; y <= end; ++y)
        {
            Object2IntOpenHashMap<BlockState> layer = this.countsByLayer.get(y);

            if (layer != null)
            {
                for (Object2IntOpenHashMap.Entry<BlockState> entry : layer.object2IntEntrySet())
                {
                    summed.addTo(entry.getKey(), entry.getIntValue());
                }
            }
        }

        return summed;
    }

    /** Lowest world Y holding a non-air block, or 0 when the tally is empty. */
    public int getMinY()
    {
        return this.minY != Integer.MAX_VALUE ? this.minY : 0;
    }

    /** Highest world Y holding a non-air block, or 0 when the tally is empty. */
    public int getMaxY()
    {
        return this.maxY != Integer.MIN_VALUE ? this.maxY : 0;
    }

    /** Non-air blocks across every layer. Summing each layer must give this back. */
    public long getTotalBlocks()
    {
        return this.totalBlocks;
    }

    public int getLayerCount()
    {
        return this.countsByLayer.size();
    }

    /** True when this tally still describes the given placement. */
    public boolean matches(SchematicPlacement placement)
    {
        return this.signature.equals(Signature.of(placement));
    }

    /**
     * The parts of a placement that move blocks between layers. Rotation and mirror are
     * deliberately absent: neither can change a block's Y, so including them would throw the
     * tally away on edits that cannot affect it.
     */
    private record Signature(String schematicName, int originY, int enabledRegions, int regionYHash)
    {
        static Signature of(SchematicPlacement placement)
        {
            LitematicaSchematic schematic = placement.getSchematic();
            int enabled = 0;
            int yHash = 1;

            if (schematic != null)
            {
                for (String regionName : schematic.getAreaPositions().keySet())
                {
                    SubRegionPlacement subRegion = placement.getRelativeSubRegionPlacement(regionName);

                    if (subRegion != null && subRegion.isEnabled())
                    {
                        ++enabled;
                        yHash = 31 * yHash + subRegion.getPos().getY();
                    }
                }
            }

            return new Signature(placement.getName(), placement.getOrigin().getY(), enabled, yHash);
        }
    }
}
