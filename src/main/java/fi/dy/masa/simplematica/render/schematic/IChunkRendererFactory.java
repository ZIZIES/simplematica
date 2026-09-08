package fi.dy.masa.simplematica.render.schematic;

import fi.dy.masa.simplematica.render.IWorldSchematicRenderer;
import fi.dy.masa.simplematica.world.WorldSchematic;

public interface IChunkRendererFactory
{
    ChunkRendererSchematicVbo create(WorldSchematic worldIn, IWorldSchematicRenderer worldRenderer);
}
