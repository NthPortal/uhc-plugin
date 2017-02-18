package com.github.nthportal.uhc;

import com.github.nthportal.uhc.commands.ConfCommandExecutor;
import com.github.nthportal.uhc.commands.ConfCommandTabCompleter;
import com.github.nthportal.uhc.commands.MainCommandExecutor;
import com.github.nthportal.uhc.commands.MainCommandTabCompleter;
import com.github.nthportal.uhc.core.Config;
import com.github.nthportal.uhc.core.Context;
import com.github.nthportal.uhc.core.CustomListener;
import com.github.nthportal.uhc.core.Timer;
import com.github.nthportal.uhc.events.MainListener;
import com.google.common.eventbus.EventBus;
import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.logging.Logger;

public final class UHCPlugin extends JavaPlugin {
    private final Logger logger = getLogger();
    private final EventBus eventBus = new EventBus("UHC-Plugin");
    private final Context context = new Context(this, logger, eventBus);
    private final Timer timer = new Timer(context);

    @Override
    public void onEnable() {
        Config.setup(this);

        PluginCommand mainCommand = getCommand(MainCommandExecutor.NAME);
        mainCommand.setExecutor(new MainCommandExecutor(timer));
        mainCommand.setTabCompleter(new MainCommandTabCompleter());

        PluginCommand confCommand = getCommand(ConfCommandExecutor.NAME);
        confCommand.setExecutor(new ConfCommandExecutor(this));
        confCommand.setTabCompleter(new ConfCommandTabCompleter());

        getServer().getPluginManager().registerEvents(new CustomListener(context, timer), this);

        eventBus.register(new MainListener(context));
    }

    @Override
    public void onDisable() {
    }
}
