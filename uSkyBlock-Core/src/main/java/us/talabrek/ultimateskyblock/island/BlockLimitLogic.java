package us.talabrek.ultimateskyblock.island;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;
import us.talabrek.ultimateskyblock.api.model.BlockScore;
import us.talabrek.ultimateskyblock.island.level.IslandScore;
import us.talabrek.ultimateskyblock.uSkyBlock;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

public class BlockLimitLogic {
    public enum CanPlace { YES, UNCERTAIN, NO};

    private static final Logger log = Logger.getLogger(BlockLimitLogic.class.getName());
    private final uSkyBlock plugin;

    private final NavigableMap<Double, Map<Material, Integer>> blockLimits = new TreeMap<>();
    private final Set<Material> scope = new HashSet<>();

    // TODO: R4zorax - 13-07-2018: Persist this somehow - and use a guavacache
    private Map<Location, Map<Material,Integer>> blockCounts = new HashMap<>();

    private final boolean limitsEnabled;

    public BlockLimitLogic(uSkyBlock plugin) {
        this.plugin = plugin;
        FileConfiguration config = plugin.getConfig();
        limitsEnabled = config.getBoolean("options.island.block-limits.enabled", false);
        if (limitsEnabled) {
            List<HashMap<String, ?>> section = (List<HashMap<String, ?>>) config.getList("options.island.block-limits.entries");
            for (HashMap<String, ?> entry : section) {
                Map<Material, Integer> limits = new HashMap<>();
                Integer level = (Integer) entry.get("level");
                Set<String> keys = entry.keySet();
                keys.remove("level");
                for (String key : keys) {
                    Material material = Material.getMaterial(key.toUpperCase());
                    int limit = (Integer) entry.get(key);
                    if (material != null && limit >= 0) {
                        limits.put(material, limit);
                        scope.add(material);
                    } else {
                        log.warning("Unknown material " + key + " supplied for block-limit, or value not an integer");
                    }
                }
                blockLimits.put(level.doubleValue(), limits);
                plugin.getLogger().info("uSkyBlock loaded limits for ["+level+"]: "+limits);
            }
        }
    }

    public int getLimit(double islandLevel, Material type) {
        return blockLimits.getOrDefault(
                blockLimits.floorKey(islandLevel),
                Collections.emptyMap()
        ).getOrDefault(type, Integer.MAX_VALUE);
    }

    public Map<Material,Integer> getLimits(double islandLevel) {
        return Collections.unmodifiableMap(
                blockLimits.getOrDefault(blockLimits.floorKey(islandLevel), Collections.emptyMap())
        );
    }

    public void updateBlockCount(Location islandLocation, IslandScore score) {
        if (!limitsEnabled) {
            return;
        }
        Map<Material, Integer> countMap = asBlockCount(score);
        plugin.getLogger().info("Update Block Count for "+islandLocation+": "+countMap);
        blockCounts.put(islandLocation, countMap);
    }

    private Map<Material,Integer> asBlockCount(IslandScore score) {
        Map<Material, Integer> countMap = new ConcurrentHashMap<>();
        for (BlockScore blockScore : score.getTop()) {
            Material type = blockScore.getBlock().getType();
            if (scope.contains(type)) {
                countMap.put(type, (countMap.containsKey(type) ? countMap.get(type) + blockScore.getCount() : blockScore.getCount()));
            }
        }
        return countMap;
    }

    public int getCount(Material type, Location islandLocation) {
        if (type == Material.HOPPER)
            plugin.getLogger().info("Get hoppercount for "+islandLocation);
        if (!limitsEnabled || !scope.contains(type)) {
            return -1;
        }
        Map<Material, Integer> islandCount = blockCounts.getOrDefault(islandLocation, null);
        if (islandCount == null) {
            return -2;
        }
        if (type == Material.HOPPER)
            plugin.getLogger().info("Result: "+islandCount.getOrDefault(type, 0));
        return islandCount.getOrDefault(type, 0);
    }

    public CanPlace canPlace(Material type, IslandInfo islandInfo) {
        int count = getCount(type, islandInfo.getIslandLocation());
        if (count == -1) {
            if (type == Material.HOPPER)
                plugin.getLogger().info("Check if hopper can place on "+islandInfo+": YES");
            return CanPlace.YES;
        } else if (count == -2) {
            if (type == Material.HOPPER)
                plugin.getLogger().info("Check if hopper can place on "+islandInfo+": UNCERTAIN");
            return CanPlace.UNCERTAIN;
        }
        if (type == Material.HOPPER){
            plugin.getLogger().info("Check if hopper can place on "+islandInfo+"? Count: "+count+", Limit: "+ getLimit(islandInfo.getLevel(), type));
        }
        return count < getLimit(islandInfo.getLevel(), type) ? CanPlace.YES : CanPlace.NO;
    }

    public void incBlockCount(Location islandLocation, Material type) {
        if (!limitsEnabled || !scope.contains(type)) {
            return;
        }
        Map<Material, Integer> islandCount = blockCounts.getOrDefault(islandLocation, new ConcurrentHashMap<>());
        int blockCount = islandCount.getOrDefault(type, 0);
        islandCount.put(type, blockCount + 1);
        blockCounts.put(islandLocation, islandCount);
    }

    public void decBlockCount(Location islandLocation, Material type) {
        if (!limitsEnabled || !scope.contains(type)) {
            return;
        }
        Map<Material, Integer> islandCount = blockCounts.getOrDefault(islandLocation, new ConcurrentHashMap<>());
        int blockCount = islandCount.getOrDefault(type, 0);
        islandCount.put(type, blockCount - 1);
        blockCounts.put(islandLocation, islandCount);
    }
}
