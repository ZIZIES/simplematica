package fi.dy.masa.simplematica.materials;

import java.util.List;
import fi.dy.masa.simplematica.util.BlockInfoListType;

public interface IMaterialList
{
    BlockInfoListType getMaterialListType();

    void setMaterialListEntries(List<MaterialListEntry> list);
}
