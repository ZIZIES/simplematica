package fi.dy.masa.simplematica.scheduler;

import fi.dy.masa.malilib.interfaces.IClientTickHandler;
import net.minecraft.client.Minecraft;
import fi.dy.masa.simplematica.config.Configs;
import fi.dy.masa.simplematica.data.DataManager;
import fi.dy.masa.simplematica.selection.SelectionManager;
import fi.dy.masa.simplematica.util.EasyPlaceUtils;
import fi.dy.masa.simplematica.util.LayerUtils;
import fi.dy.masa.simplematica.util.WorldUtils;

public class ClientTickHandler implements IClientTickHandler
{
    @Override
    public void onClientTick(Minecraft mc)
    {
        if (mc.level != null && mc.player != null)
        {
            SelectionManager sm = DataManager.getSelectionManager();

            if (sm.hasGrabbedElement())
            {
                sm.moveGrabbedElement(mc.player);
            }

            if (mc.gui.screen() == null)
            {
                if (Configs.Generic.EASY_PLACE_POST_REWRITE.getBooleanValue())
                {
                    EasyPlaceUtils.easyPlaceOnUseTick();
                }
                else
                {
                    WorldUtils.easyPlaceOnUseTick(mc);
                }
            }

            LayerUtils.onClientTick(mc);
            DataManager.getSchematicPlacementManager().onClientTick(mc);
            TaskScheduler.getInstanceClient().runTasks();
        }
    }
}
