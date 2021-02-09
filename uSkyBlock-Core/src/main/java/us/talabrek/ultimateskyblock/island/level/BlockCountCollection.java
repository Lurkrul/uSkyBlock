package us.talabrek.ultimateskyblock.island.level;

import org.bukkit.Material;
import org.bukkit.entity.EntityType;
import us.talabrek.ultimateskyblock.api.model.BlockScore;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.LongAdder;
import java.util.stream.Collectors;

/**
 * Mutable collection for storing counts of blocks
 */
public class BlockCountCollection {
    private BlockLevelConfigMap configMap;
    private Map<BlockMatch, LongAdder> countMap;

    public BlockCountCollection(BlockLevelConfigMap configMap) {
        this.configMap = configMap;
        countMap = new ConcurrentHashMap<>();
    }

    public int add(Material type, byte dataValue, int blockCount) {
        BlockLevelConfig blockLevelConfig = configMap.get(type, dataValue);
        BlockMatch key = blockLevelConfig.getKey();
        LongAdder count = countMap.computeIfAbsent(key, k -> new LongAdder());
        count.add(blockCount);
        return count.intValue();
    }

    public int add(Material type, byte dataValue) {
        return add(type, dataValue, 1);
    }

    public List<BlockScore> calculateScore(double pointsPerLevel) {
        List<BlockScore> collect = countMap.entrySet().stream()
                //todo pull in the entities and add to count
                .map(e -> configMap.get(e.getKey()).calculateScore(e.getValue().intValue(), pointsPerLevel))
                //.filter(f -> f.getScore() != 0)
                //todo filter scores that are 0 AND DO NOT HAVE LIMITS!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!
                .sorted(new BlockScoreComparator()).collect(Collectors.toList());
        return collect;
    }
}
