package us.talabrek.ultimateskyblock.chat;

import dk.lockfuglsang.minecraft.po.I18nUtil;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import us.talabrek.ultimateskyblock.api.IslandInfo;
import us.talabrek.ultimateskyblock.api.event.IslandChatEvent;
import us.talabrek.ultimateskyblock.handler.WorldGuardHandler;
import us.talabrek.ultimateskyblock.handler.placeholder.PlaceholderHandler;
import us.talabrek.ultimateskyblock.uSkyBlock;
import dk.lockfuglsang.minecraft.util.FormatUtil;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Matcher;

/**
 * The primary logic of uSkyBlocks chat-handling
 */
public class ChatLogic {
    private final List<String> ALONE = Arrays.asList(
            I18nUtil.tr("But you are ALLLLLLL ALOOOOONE!"),
            I18nUtil.tr("But you are Yelling in the wind!"),
            I18nUtil.tr("But your fantasy friends are gone!"),
            I18nUtil.tr("But you are Talking to your self!")
    );
    private final uSkyBlock plugin;
    private final Map<IslandChatEvent.Type, String> formats = new HashMap<>();
    private final Map<UUID, IslandChatEvent.Type> toggled = new HashMap<>();
    private String spyFormat;

    public ChatLogic(uSkyBlock plugin) {
        this.plugin = plugin;
        spyFormat = plugin.getConfig().getString("options.admin.spyFormat", "&9SKY &r{SENDER} -> {RECEIVERS} &f>&b {MESSAGE}");
        formats.put(IslandChatEvent.Type.PARTY, plugin.getConfig().getString("options.party.chat-format", "&9PARTY &r{DISPLAYNAME} &f>&d {MESSAGE}"));
        formats.put(IslandChatEvent.Type.ISLAND, plugin.getConfig().getString("options.island.chat-format", "&9SKY &r{DISPLAYNAME} &f>&b {MESSAGE}"));
    }

    public List<Player> getRecipients(Player player, IslandChatEvent.Type chatType) {
        if (chatType == IslandChatEvent.Type.PARTY) {
            IslandInfo islandInfo = plugin.getIslandInfo(player);
            return islandInfo != null ? islandInfo.getOnlineMembers() : Collections.singletonList(player);
        } else if (chatType == IslandChatEvent.Type.ISLAND) {
            if (plugin.getWorldManager().isSkyAssociatedWorld(player.getWorld())) {
                return WorldGuardHandler.getPlayersInRegion(plugin.getWorldManager().getWorld(),
                        WorldGuardHandler.getIslandRegionAt(player.getLocation()));
            }
            return Collections.emptyList();
        }
        return Collections.emptyList();
    }

    public void sendMessage(Player player, IslandChatEvent.Type type, String message) {
        String format = getFormat(type);
        format = FormatUtil.normalize(format);
        format = format.replaceAll("\\{DISPLAYNAME\\}", Matcher.quoteReplacement(player.getDisplayName()));
        String msg = format.replaceAll("\\{MESSAGE\\}", Matcher.quoteReplacement(message));
        //&9SKY &r{SENDER} -> {RECEIVERS} &f>&b {MESSAGE}
        msg = PlaceholderHandler.replacePlaceholders(player, msg);
        List<Player> onlineMembers = getRecipients(player, type);
        //todo filter out the invisible players

        String isSpyFormat = getSpyFormat(type);
        isSpyFormat = FormatUtil.normalize(isSpyFormat);
        isSpyFormat = isSpyFormat.replace("{SENDER}", player.getDisplayName());
        isSpyFormat = isSpyFormat.replace("{MESSAGE}", message);
        StringBuilder builder = new StringBuilder();
        for(Player p : onlineMembers) {
            if(player.equals(p)) continue;
            if(!builder.toString().equals("")) builder.append(", ");
            builder.append(p.getDisplayName());
        }
        isSpyFormat = isSpyFormat.replace("{RECEIVERS}", builder.toString());
        String spyMessage = PlaceholderHandler.replacePlaceholders(player, isSpyFormat);
        List<Player> spyMembers = new ArrayList<>();
        //only add players that have chatspy perms
        for(Player p : Bukkit.getOnlinePlayers()) {
            if(p.hasPermission("usb.admin.istalkspy") && !onlineMembers.contains(p)) {
                spyMembers.add(p);
            }
        }

        if (onlineMembers.size() <= 1) {
            player.sendMessage(I18nUtil.tr("\u00a7cSorry! {0}", "\u00a79" + ALONE.get(((int) Math.round(Math.random() * ALONE.size())) % ALONE.size())));
        } else {
            for (Player member : onlineMembers) {
                member.sendMessage(msg);
            }
            for(Player staff : spyMembers) {
                staff.sendMessage(spyMessage);
            }
        }
    }

    public String getFormat(IslandChatEvent.Type type) {
        return formats.get(type);
    }

    public String getSpyFormat(IslandChatEvent.Type type) {
        return spyFormat;
    }

    /**
     * Toggles the chat-type on or off, returns <code>true</code> if it was toggled on.
     * @param player
     * @param type
     * @return
     */
    public synchronized boolean toggle(Player player, IslandChatEvent.Type type) {
        IslandChatEvent.Type oldType = toggled.get(player.getUniqueId());
        if (oldType == type) {
            toggled.remove(player.getUniqueId());
            return false;
        } else {
            toggled.put(player.getUniqueId(), type);
        }
        return true;
    }

    /**
     * Returns the current toggle, or <code>null</code> if none exists.
     * @param player
     * @return
     */
    public synchronized IslandChatEvent.Type getToggle(Player player) {
        return toggled.get(player.getUniqueId());
    }
}
