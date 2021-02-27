package us.talabrek.ultimateskyblock.compat.miniaturepets;

import org.bukkit.Location;
import org.bukkit.entity.EntityType;
import org.bukkit.plugin.Plugin;
import us.talabrek.ultimateskyblock.util.LogUtil;

import java.util.logging.Level;

public class MiniaturePetsAdapterFallback extends MiniaturePetsCompat {

    public EntityType getPetEntityType(String type){
        return null;
    }

    @Override
    public boolean isPetSpawningFor(Location location, EntityType type) {
        return false;
    }

    @Override
    protected void enable(Plugin p) {
        LogUtil.log(Level.INFO, "Enable MiniaturePets Fallback Adapter");
    }

    @Override
    protected void disable() {

    }

    @Override
    public boolean isEnabled() {
        return false;
    }
}
