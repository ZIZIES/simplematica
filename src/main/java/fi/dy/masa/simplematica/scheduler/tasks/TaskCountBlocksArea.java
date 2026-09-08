package fi.dy.masa.simplematica.scheduler.tasks;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;

import fi.dy.masa.simplematica.materials.IMaterialList;
import fi.dy.masa.simplematica.selection.AreaSelection;

public class TaskCountBlocksArea extends TaskCountBlocksBase
{
    public TaskCountBlocksArea(AreaSelection selection, IMaterialList materialList)
    {
        super(materialList, "simplematica.gui.label.task_name.area_analyzer");

        this.addPerChunkBoxes(selection.getAllSubRegionBoxes());
    }

    @Override
    protected void countAtPosition(BlockPos pos)
    {
        BlockState stateClient = this.clientWorld.getBlockState(pos);
        this.countsTotal.addTo(stateClient, 1);
    }
}
