package us.talabrek.ultimateskyblock.compat;

import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;
import us.talabrek.ultimateskyblock.compat.miniaturepets.MiniaturePetsAdapter;
import us.talabrek.ultimateskyblock.compat.miniaturepets.MiniaturePetsAdapterFallback;
import us.talabrek.ultimateskyblock.util.LogUtil;

import java.util.*;
import java.util.logging.Level;

public class CompatHandler {

    // Keys of all Plugins to load compats for
    public enum Key {
        MiniaturePets("MiniaturePets");

        public String id;

        Key(String id){
            this.id = id;
        }
    }

    protected JavaPlugin plugin;

    private Map<Key, Compat> resolvedCompats = new HashMap<>();

    public CompatHandler(JavaPlugin instance) {
        this.plugin = instance;
    }

    // Load and define adapter classes for each plugin
    public void load(){
        loadCompat(Key.MiniaturePets, Arrays.asList(MiniaturePetsAdapter.class, MiniaturePetsAdapterFallback.class));
    }

    public void disable(Key key) {
        Compat c = resolvedCompats.get(key);
        if (c == null) return;
        c.disable();
    }

    public void disableAll(){
        resolvedCompats.forEach((name, compat) -> compat.disable());
    }

    public <T extends Compat> T getCompat(Key key){
        return (T) resolvedCompats.get(key);
    }

    private <T extends Compat> T loadCompat(Key key, List<Class<? extends T>> options){
        return (T) resolvedCompats.computeIfAbsent(key, s -> tryLoadCompat(s, options));
    }

    private <T extends Compat> T tryLoadCompat(Key key, List<Class<? extends T>> options){
        Plugin dependency = plugin.getServer().getPluginManager().getPlugin(key.id);
        if (dependency ==  null){
            LogUtil.log(Level.WARNING, "No plugin named " + key + " found.");
            return null;
        }

        for (Class<? extends T> option : options){
            try{
                T compat = option.newInstance();
                compat.init(plugin, dependency);
                LogUtil.log(Level.INFO, "Successfully loaded compat " + key);
                return compat;
            } catch (IllegalAccessException | InstantiationException e) {
                e.printStackTrace();
            } catch (NoClassDefFoundError ignored){
            }
        }

        LogUtil.log(Level.SEVERE, "Failed to load compat for " + key);
        return null;
    }
}
