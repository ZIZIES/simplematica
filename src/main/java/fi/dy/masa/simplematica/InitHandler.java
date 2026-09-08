package fi.dy.masa.simplematica;

import fi.dy.masa.malilib.command.ClientCommandHandler;
import fi.dy.masa.malilib.config.ConfigManager;
import fi.dy.masa.malilib.event.*;
import fi.dy.masa.malilib.interfaces.IInitializationHandler;
import fi.dy.masa.malilib.interfaces.IRenderer;
import fi.dy.masa.malilib.registry.Registry;
import fi.dy.masa.malilib.util.data.ModInfo;
import fi.dy.masa.malilib.util.i18n.i18nMode;

import net.minecraft.client.Minecraft;

import fi.dy.masa.simplematica.command.PmCommand;
import fi.dy.masa.simplematica.config.Configs;
import fi.dy.masa.simplematica.data.DataManager;
import fi.dy.masa.simplematica.data.EntityDataManager;
import fi.dy.masa.simplematica.event.*;
import fi.dy.masa.simplematica.gui.GuiConfigs;
import fi.dy.masa.simplematica.render.infohud.StatusInfoRenderer;
import fi.dy.masa.simplematica.scheduler.ClientTickHandler;
import fi.dy.masa.simplematica.schematic.placement.PlacementManagerDaemonHandler;

public class InitHandler implements IInitializationHandler
{
    @Override
    public void registerModHandlers()
    {
        ConfigManager.getInstance().registerConfigHandler(Reference.MOD_ID, new Configs());
        Registry.CONFIG_SCREEN.registerConfigScreenFactory(
                new ModInfo(Reference.MOD_ID, Reference.MOD_NAME, GuiConfigs::new)
        );
        Configs.LANG.ifPresent(
                i18nManager ->
                        Registry.TRANSLATION_OVERRIDE_MANAGER.registerTranslationManager(Reference.MOD_ID, i18nManager,
                                                                                         (i18nMode) Configs.Generic.TRANSLATION_MODE.getOptionListValue())
        );
        Configs.Generic.TRANSLATION_MODE.setValueChangeCallback(
                cfg ->
                        Registry.TRANSLATION_OVERRIDE_MANAGER.registerLanguageMode(Reference.MOD_ID, (i18nMode) cfg.getOptionListValue())
        );

        EntityDataManager.getInstance().onGameInit();

        InputEventHandler.getKeybindManager().registerKeybindProvider(InputHandler.getInstance());
        InputEventHandler.getInputManager().registerKeyboardInputHandler(InputHandler.getInstance());
        InputEventHandler.getInputManager().registerMouseInputHandler(InputHandler.getInstance());

        IRenderer renderer = new RenderHandler();
        RenderEventHandler.getInstance().registerInGameGuiRenderer(renderer);
        RenderEventHandler.getInstance().registerWorldPreWeatherRenderer(renderer);
        RenderEventHandler.getInstance().registerWorldLastRenderer(renderer);

        ServerHandler.getInstance().registerServerHandler(new ServerListener());

        TickHandler.getInstance().registerClientTickHandler(new ClientTickHandler());
        TickHandler.getInstance().registerClientTickHandler(EntityDataManager.getInstance());
        TickHandler.getInstance().registerClientTickHandler(PlacementManagerDaemonHandler.INSTANCE);

        WorldLoadListener listener = new WorldLoadListener();
        WorldLoadHandler.getInstance().registerWorldLoadPreHandler(listener);
        WorldLoadHandler.getInstance().registerWorldLoadPostHandler(listener);

        KeyCallbacks.init(Minecraft.getInstance());
        StatusInfoRenderer.init();

        ClientCommandHandler.INSTANCE.registerCommand(new PmCommand());

        DataManager.getAreaSelectionsBaseDirectory();
        DataManager.getSchematicsBaseDirectory();
    }
}
