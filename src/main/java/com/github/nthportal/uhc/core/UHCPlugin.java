package com.github.nthportal.uhc.core;

import com.github.nthportal.uhc.commands.ConfCommandExecutor;
import com.github.nthportal.uhc.commands.ConfCommandTabCompleter;
import com.github.nthportal.uhc.commands.MainCommandExecutor;
import com.github.nthportal.uhc.commands.MainCommandTabCompleter;
import com.github.nthportal.uhc.events.MainListener;
import com.github.nthportal.uhc.util.CommandExecutor;
import com.google.common.eventbus.EventBus;
import lombok.val;
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

        val mainCommand = getCommand(MainCommandExecutor.NAME);
        mainCommand.setExecutor(new MainCommandExecutor(timer));
        mainCommand.setTabCompleter(new MainCommandTabCompleter());

        val confCommand = getCommand(ConfCommandExecutor.NAME);
        confCommand.setExecutor(new ConfCommandExecutor(this));
        confCommand.setTabCompleter(new ConfCommandTabCompleter());

        getServer().getPluginManager().registerEvents(new DeathListener(context, timer), this);

        eventBus.register(new MainListener(new CommandExecutor(context)));
    }

    @Override
    public void onDisable() {
    }
}
