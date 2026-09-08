package fi.dy.masa.simplematica.event;

import fi.dy.masa.malilib.interfaces.IServerListener;
import net.minecraft.client.server.IntegratedServer;
import fi.dy.masa.simplematica.data.DataManager;

public class ServerListener implements IServerListener
{
    @Override
    public void onServerIntegratedSetup(IntegratedServer server)
    {
        DataManager.getInstance().setHasIntegratedServer(true);
    }
}
