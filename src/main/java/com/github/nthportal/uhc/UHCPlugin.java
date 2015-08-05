package com.github.nthportal.uhc;

import com.github.nthportal.uhc.commands.ConfCommandExecutor;
import com.github.nthportal.uhc.commands.ConfCommandTabCompleter;
import com.github.nthportal.uhc.commands.MainCommandExecutor;
import com.github.nthportal.uhc.commands.MainCommandTabCompleter;
import com.github.nthportal.uhc.core.Config;
import com.github.nthportal.uhc.core.CustomListener;
import com.github.nthportal.uhc.core.Timer;
import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.logging.Logger;

public final class UHCPlugin extends JavaPlugin {
    public final Logger logger = getLogger();
    public final Timer timer = new Timer(this);

    @Override
    public void onEnable() {
        Config.setup(this);

        PluginCommand mainCommand = getCommand(MainCommandExecutor.NAME);
        mainCommand.setExecutor(new MainCommandExecutor(this));
        mainCommand.setTabCompleter(new MainCommandTabCompleter(this));

        PluginCommand confCommand = getCommand(ConfCommandExecutor.NAME);
        confCommand.setExecutor(new ConfCommandExecutor(this));
        confCommand.setTabCompleter(new ConfCommandTabCompleter(this));

        getServer().getPluginManager().registerEvents(new CustomListener(this), this);
    }

    @Override
    public void onDisable() {

    }


}
