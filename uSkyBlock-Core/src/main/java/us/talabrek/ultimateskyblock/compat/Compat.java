package us.talabrek.ultimateskyblock.compat;

import org.bukkit.event.Listener;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;

public abstract class Compat implements Listener {
    protected JavaPlugin plugin;

    public final void init(JavaPlugin plugin, Plugin dependency){
        this.plugin = plugin;
        enable(dependency);
        registerListeners();
    }

    protected abstract void enable(Plugin p);

    protected abstract void disable();

    public abstract boolean isEnabled();

    protected void registerListeners(){
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

}
