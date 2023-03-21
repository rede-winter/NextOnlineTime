package com.nextplugins.onlinetime.view;

import com.henryfabio.minecraft.inventoryapi.editor.InventoryEditor;
import com.henryfabio.minecraft.inventoryapi.inventory.impl.paged.PagedInventory;
import com.henryfabio.minecraft.inventoryapi.item.InventoryItem;
import com.henryfabio.minecraft.inventoryapi.item.supplier.InventoryItemSupplier;
import com.henryfabio.minecraft.inventoryapi.viewer.Viewer;
import com.henryfabio.minecraft.inventoryapi.viewer.configuration.border.Border;
import com.henryfabio.minecraft.inventoryapi.viewer.impl.paged.PagedViewer;
import com.nextplugins.onlinetime.NextOnlineTime;
import com.nextplugins.onlinetime.api.models.enums.RewardStatus;
import com.nextplugins.onlinetime.api.player.TimedPlayer;
import com.nextplugins.onlinetime.api.reward.Reward;
import com.nextplugins.onlinetime.configuration.values.FeatureValue;
import com.nextplugins.onlinetime.configuration.values.MessageValue;
import com.nextplugins.onlinetime.manager.RewardManager;
import com.nextplugins.onlinetime.manager.TimedPlayerManager;
import com.nextplugins.onlinetime.manager.TopTimedPlayerManager;
import com.nextplugins.onlinetime.registry.InventoryRegistry;
import com.nextplugins.onlinetime.utils.ColorUtil;
import com.nextplugins.onlinetime.utils.ItemBuilder;
import com.nextplugins.onlinetime.utils.TimeUtils;
import lombok.val;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.inventory.ItemFlag;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author Yuhtin
 * Github: https://github.com/Yuhtin
 */
public final class OnlineTimeView extends PagedInventory {

    private final Map<String, Integer> playerRewardFilter = new HashMap<>();
    private final RewardManager rewardManager;
    private final InventoryRegistry inventoryRegistry;
    private final TimedPlayerManager timedPlayerManager;
    private final TopTimedPlayerManager topTimedPlayerManager;

    public OnlineTimeView() {
        super("online-time.main", "Tempo Online", 6 * 9);

        rewardManager = NextOnlineTime.getInstance().getRewardManager();
        inventoryRegistry = NextOnlineTime.getInstance().getInventoryRegistry();
        timedPlayerManager = NextOnlineTime.getInstance().getTimedPlayerManager();
        topTimedPlayerManager = NextOnlineTime.getInstance().getTopTimedPlayerManager();
    }

    @Override
    protected void configureViewer(PagedViewer viewer) {
        val pagedViewer = viewer.getConfiguration();

        pagedViewer.itemPageLimit(21);
        pagedViewer.border(Border.of(1, 1, 2, 1));
    }

    @Override
    protected void configureInventory(Viewer viewer, InventoryEditor editor) {
        val player = viewer.getPlayer();
        val timedPlayer = timedPlayerManager.getByName(player.getName());

        editor.setItem(
                47,
                InventoryItem.of(new ItemBuilder(viewer.getPlayer().getName())
                        .name("&aSeu progresso:")
                        .setLore(
                                "",
                                "&7 Você possui &f" + TimeUtils.format(timedPlayer.getTimeInServer()) + " &7online.",
                                "&7 Você coletou &f"
                                        + timedPlayer.getCollectedRewards().size() + " &7recompensas.")
                        .wrap()));

        editor.setItem(49, changeFilterInventoryItem(viewer));

        editor.setItem(
                51,
                InventoryItem.of(new ItemBuilder(Material.GOLD_INGOT)
                                .name("&aRanking")
                                .setLore("&7Clique para ver os jogadores", "&7com mais tempo.")
                                .wrap())
                        .defaultCallback(callback -> {
                            if (topTimedPlayerManager.checkUpdate()) {
                                callback.getPlayer()
                                        .sendMessage(ColorUtil.colored("&aO ranking está atualizando, aguarde."));
                                return;
                            }

                            inventoryRegistry.getTopInventory().openInventory(callback.getPlayer());
                        }));
    }

    @Override
    protected List<InventoryItemSupplier> createPageItems(PagedViewer viewer) {
        val items = new ArrayList<InventoryItemSupplier>();

        val player = viewer.getPlayer();
        val timedPlayer = timedPlayerManager.getByName(player.getName());

        val rewardFilter = playerRewardFilter.getOrDefault(viewer.getName(), -1);

        for (val reward : rewardManager.getRewards().values()) {
            if (reward.getPermission() != null && !player.hasPermission(reward.getPermission())) continue;

            items.add(() -> {
                val rewardStatus = timedPlayer.canCollect(reward);
                if (rewardFilter != -1 && rewardFilter != rewardStatus.getCode()) return null;

                val replacedLore = rewardLore(reward, rewardStatus, MessageValue.get(MessageValue::rewardLore));
                return rewardInventoryItem(timedPlayer, reward, replacedLore);
            });
        }

        return items;
    }

    @Override
    protected void update(Viewer viewer, InventoryEditor editor) {
        super.update(viewer, editor);
        configureInventory(viewer, viewer.getEditor());
    }

    private InventoryItem rewardInventoryItem(TimedPlayer timedPlayer, Reward reward, List<String> lore) {
        return InventoryItem.of(new ItemBuilder(reward.getIcon().clone())
                        .name(reward.getColoredName())
                        .setLore(lore)
                        .changeItemMeta(itemMeta -> itemMeta.addItemFlags(ItemFlag.values()))
                        .wrap())
                .defaultCallback(callback -> {
                    val player = callback.getPlayer();
                    val rewardStatus = timedPlayer.canCollect(reward);
                    if (!rewardStatus.isCanCollect()) {
                        player.sendMessage(rewardStatus.getMessage());
                        return;
                    }

                    var avaliableSpaces = 0;
                    for (val content : player.getInventory().getContents()) {
                        if (content != null && content.getType() != Material.AIR) continue;
                        ++avaliableSpaces;
                    }

                    if (avaliableSpaces < reward.getCommands().size()) {
                        player.sendMessage(MessageValue.get(MessageValue::noSpace)
                                .replace(
                                        "%spaces%",
                                        String.valueOf(reward.getCommands().size() - avaliableSpaces)));
                        return;
                    }

                    player.sendMessage(MessageValue.get(MessageValue::collectedReward)
                            .replace("%reward%", reward.getColoredName()));

                    timedPlayer.getCollectedRewards().add(reward.getName());

                    if (FeatureValue.get(FeatureValue::type)) {
                        timedPlayer.removeTime(reward.getTime());
                        player.sendMessage(MessageValue.get(MessageValue::usedTime)
                                .replace("%time%", TimeUtils.format(reward.getTime())));
                    }

                    callback.updateInventory();

                    for (val command : reward.getCommands()) {
                        Bukkit.dispatchCommand(
                                Bukkit.getConsoleSender(), command.replace("%player%", player.getName()));
                    }
                });
    }

    private InventoryItem changeFilterInventoryItem(Viewer viewer) {
        val currentFilter = new AtomicInteger(playerRewardFilter.getOrDefault(viewer.getName(), -1));
        return InventoryItem.of(new ItemBuilder(Material.HOPPER)
                        .name("&aFiltro de recompensas")
                        .setLore(
                                "",
                                getColorByFilter(currentFilter.get(), -1) + " Todas as recompensas",
                                getColorByFilter(currentFilter.get(), 0) + " Recompensas liberadas",
                                getColorByFilter(currentFilter.get(), 1) + " Recompensas bloqueadas",
                                getColorByFilter(currentFilter.get(), 2) + " Recompensas coletadas",
                                "",
                                "&aClique para mudar o filtro!")
                        .wrap())
                .defaultCallback(event -> {
                    playerRewardFilter.put(
                            viewer.getName(), currentFilter.incrementAndGet() > 2 ? -1 : currentFilter.get());
                    event.updateInventory();
                });
    }

    private List<String> rewardLore(Reward reward, RewardStatus rewardStatus, List<String> list) {
        val lore = new ArrayList<String>();

        for (val line : list) {
            if (line.contains("%reward_description%")) lore.addAll(reward.getDescription());
            else {
                lore.add(line.replace("%time%", TimeUtils.format(reward.getTime()))
                        .replace("%collect_message%", rewardStatus.getMessage()));
            }
        }

        return lore;
    }

    private String getColorByFilter(int currentFilter, int loopFilter) {
        return currentFilter == loopFilter ? " &7&l▶&7" : "&8";
    }
}
