package com.github.nthportal.uhc;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.logging.Logger;

public final class UHCPlugin extends JavaPlugin {
    public final Logger logger = getLogger();
    public final FileConfiguration config = getConfig();
    public final Timer timer = new Timer(this);

    @Override
    public void onEnable() {
        Configs.setup(this);

        getCommand(MainCommandExecutor.NAME).setExecutor(new MainCommandExecutor(this));
        getCommand(ConfCommandExecutor.NAME).setExecutor(new ConfCommandExecutor(this));

        logger.info("Enabled UHC Plugin");
    }

    @Override
    public void onDisable() {

    }
}
