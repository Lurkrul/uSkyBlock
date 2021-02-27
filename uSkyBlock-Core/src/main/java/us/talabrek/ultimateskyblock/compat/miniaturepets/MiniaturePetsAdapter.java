package us.talabrek.ultimateskyblock.compat.miniaturepets;

import com.kirelcodes.miniaturepets.MiniaturePets;
import com.kirelcodes.miniaturepets.api.events.pets.PetFinishedSpawnEvent;
import com.kirelcodes.miniaturepets.api.events.pets.PetSpawnEvent;
import com.kirelcodes.miniaturepets.loader.PetLoader;
import dk.lockfuglsang.minecraft.util.TimeUtil;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitTask;
import us.talabrek.ultimateskyblock.util.LogUtil;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;

public class MiniaturePetsAdapter extends MiniaturePetsCompat {

    private MiniaturePets mpets;
    private Map<Player, SpawnInfo> currentSpawns = new ConcurrentHashMap<>();
    private BukkitTask updateTask;

    @Override
    public EntityType getPetEntityType(String type) {
        return PetLoader.getPet(type).getAnchor();
    }

    @Override
    public boolean isPetSpawningFor(Location location, EntityType type) {
        long timestamp = System.currentTimeMillis();
        for (SpawnInfo info : currentSpawns.values()) {
            double distance;
            try {
                distance = location.distance(info.location);
            } catch (IllegalArgumentException e) {
                return false;
            }

            return info.type == type && distance < 5 && info.timestamp - timestamp < 3000;
        }
        return false;
    }

    @Override
    protected void enable(Plugin p) {
        LogUtil.log(Level.INFO, "Enable MiniaturePets Adapter");
        mpets = (MiniaturePets) p;
        updateTask = Bukkit.getScheduler().runTaskTimerAsynchronously(plugin, () -> {
                    // In case pet spawning was initiated but not successfully completed -> finish event never called
                    long timestamp = System.currentTimeMillis();
                    currentSpawns.forEach((key, info) -> {
                        if (Math.abs(info.timestamp - timestamp) > 3000) currentSpawns.remove(key);
                    });
                },
                TimeUtil.secondsAsTicks(300),
                TimeUtil.secondsAsTicks(300));
    }

    @Override
    protected void disable() {
        if (updateTask == null) return;
        updateTask.cancel();
    }

    @Override
    public boolean isEnabled() {
        return true;
    }

    @EventHandler
    public void onPetSpawn(PetSpawnEvent event) {
        Location loc = event.getOwner().getLocation();
        EntityType type = getPetEntityType(event.getType());
        currentSpawns.put(event.getOwner(), new SpawnInfo(loc, type, System.currentTimeMillis()));
    }

    @EventHandler
    public void onPetSpawned(PetFinishedSpawnEvent event) {
        currentSpawns.remove(event.getOwner());
    }

    static class SpawnInfo {
        Location location;
        EntityType type;
        Long timestamp;

        public SpawnInfo(Location location, EntityType type, Long timestamp) {
            this.location = location;
            this.type = type;
            this.timestamp = timestamp;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            SpawnInfo spawnInfo = (SpawnInfo) o;
            return location.equals(spawnInfo.location) && type == spawnInfo.type;
        }
    }

}
