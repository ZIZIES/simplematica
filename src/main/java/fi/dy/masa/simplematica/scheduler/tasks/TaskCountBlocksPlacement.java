package fi.dy.masa.simplematica.scheduler.tasks;

import java.util.Collection;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;

import fi.dy.masa.simplematica.config.Configs;
import fi.dy.masa.simplematica.data.DataManager;
import fi.dy.masa.simplematica.materials.IMaterialList;
import fi.dy.masa.simplematica.schematic.placement.SchematicPlacement;
import fi.dy.masa.simplematica.schematic.placement.SubRegionPlacement.RequiredEnabled;
import fi.dy.masa.simplematica.selection.Box;
import fi.dy.masa.simplematica.util.BlockInfoListType;
import fi.dy.masa.simplematica.util.BlockUtils;

public class TaskCountBlocksPlacement extends TaskCountBlocksBase
{
    protected final SchematicPlacement schematicPlacement;
    protected final boolean ignoreState;

    public TaskCountBlocksPlacement(SchematicPlacement schematicPlacement, IMaterialList materialList)
    {
        this(schematicPlacement, materialList, false);
    }

    public TaskCountBlocksPlacement(SchematicPlacement schematicPlacement, IMaterialList materialList, boolean ignoreState)
    {
        super(materialList, "simplematica.gui.label.task_name.material_list");

        this.schematicPlacement = schematicPlacement;
        this.ignoreState = ignoreState;
        Collection<Box> boxes = schematicPlacement.getSubRegionBoxes(RequiredEnabled.PLACEMENT_ENABLED).values();

        // Filter/clamp the boxes to intersect with the render layer
        if (materialList.getMaterialListType() == BlockInfoListType.RENDER_LAYERS)
        {
            this.addPerChunkBoxes(boxes, DataManager.getRenderLayerRange());
        }
        else
        {
            this.addPerChunkBoxes(boxes);
        }

    }

    @Override
    public boolean canExecute()
    {
        return super.canExecute() && this.schematicWorld != null;
    }

    @Override
    protected void countAtPosition(BlockPos pos)
    {
        BlockState stateSchematic = this.schematicWorld.getBlockState(pos);

        if (stateSchematic.isAir() == false)
        {
            BlockState stateClient = this.clientWorld.getBlockState(pos);

            this.countsTotal.addTo(stateSchematic, 1);

            if (stateClient.isAir())
            {
                this.countsMissing.addTo(stateSchematic, 1);
            }
            else if (stateClient != stateSchematic &&
                    (this.ignoreState == false || stateClient.getBlock() != stateSchematic.getBlock()))
            {
                if (Configs.Visuals.IGNORE_CROP_AGE.getBooleanValue() == false ||
                    BlockUtils.areStatesEqualIgnoringAge(stateSchematic, stateClient) == false)
                {
                    this.countsMissing.addTo(stateSchematic, 1);
                    this.countsMismatch.addTo(stateSchematic, 1);
                }
            }
        }
    }
}
