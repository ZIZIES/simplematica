package fi.dy.masa.simplematica.util.invoker;

public interface IWorldUpdateSuppressor
{
    boolean litematica_getShouldPreventBlockUpdates();

    void litematica_setShouldPreventBlockUpdates(boolean preventUpdates);
}
