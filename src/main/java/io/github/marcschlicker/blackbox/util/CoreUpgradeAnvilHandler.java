package io.github.marcschlicker.blackbox.util;

import net.neoforged.neoforge.event.AnvilUpdateEvent;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

import io.github.marcschlicker.blackbox.init.BlackboxModItems;
import io.github.marcschlicker.blackbox.item.CoreCellSizeUpgradeItem;
import io.github.marcschlicker.blackbox.item.CoreEnvironmentUpgradeItem;

import java.util.ArrayList;
import java.util.List;

/** Applies core-only upgrades through the vanilla anvil instead of machine slots. */
public final class CoreUpgradeAnvilHandler {
	public static final long UPGRADE_COST_LEVELS = 3;

	private CoreUpgradeAnvilHandler() {
	}

	public static void onAnvilUpdate(AnvilUpdateEvent event) {
		if (!(event.getPlayer() instanceof ServerPlayer player) || !event.getOutput().isEmpty()) {
			return;
		}
		ItemStack result = createResult(player, event.getLeft(), event.getRight());
		if (result.isEmpty()) {
			return;
		}
		event.setOutput(result);
		event.setCost(UPGRADE_COST_LEVELS);
		event.setMaterialCost(1);
	}

	public static List<AnvilUpgradeDisplay> jeiDisplays() {
		List<AnvilUpgradeDisplay> displays = new ArrayList<>();
		for (ItemStack upgrade : List.of(
				new ItemStack(BlackboxModItems.STANDARD_ENVIRONMENT_UPGRADE.get()),
				new ItemStack(BlackboxModItems.OVERWORLD_ENVIRONMENT_UPGRADE.get()),
				new ItemStack(BlackboxModItems.NETHER_ENVIRONMENT_UPGRADE.get()),
				new ItemStack(BlackboxModItems.END_ENVIRONMENT_UPGRADE.get()),
				new ItemStack(BlackboxModItems.MEDIUM_CELL_UPGRADE.get()),
				new ItemStack(BlackboxModItems.LARGE_CELL_UPGRADE.get()),
				new ItemStack(BlackboxModItems.MOB_SPAWN_UPGRADE.get()))) {
			ItemStack result = previewResult(upgrade);
			if (!result.isEmpty()) {
				displays.add(new AnvilUpgradeDisplay(new ItemStack(BlackboxModItems.DIMENSION_CORE.get()), upgrade, result, UPGRADE_COST_LEVELS));
			}
		}
		return List.copyOf(displays);
	}

	private static ItemStack createResult(ServerPlayer player, ItemStack core, ItemStack upgrade) {
		if (!core.is(BlackboxModItems.DIMENSION_CORE.get()) || FarmCoreData.isVillageArchiveCore(core)
				|| !FarmCoreData.canManage(core, player)) {
			return ItemStack.EMPTY;
		}
		if ((upgrade.getItem() instanceof CoreEnvironmentUpgradeItem || upgrade.getItem() instanceof CoreCellSizeUpgradeItem)
				&& FarmCoreData.getCoreId(core).isPresent()) {
			return ItemStack.EMPTY;
		}
		if (upgrade.is(BlackboxModItems.MOB_SPAWN_UPGRADE.get()) && FarmCoreData.isMobSpawningEnabled(core)) {
			return ItemStack.EMPTY;
		}
		ItemStack result = core.copyWithCount(1);
		if (!applyUpgrade(result, upgrade)) {
			return ItemStack.EMPTY;
		}
		FarmCoreData.clearProfile(result, player.level().registryAccess());
		return result;
	}

	private static ItemStack previewResult(ItemStack upgrade) {
		ItemStack result = new ItemStack(BlackboxModItems.DIMENSION_CORE.get());
		return applyUpgrade(result, upgrade) ? result : ItemStack.EMPTY;
	}

	private static boolean applyUpgrade(ItemStack core, ItemStack upgrade) {
		if (upgrade.getItem() instanceof CoreEnvironmentUpgradeItem environmentUpgrade) {
			FarmCoreData.setEnvironment(core, environmentUpgrade.environment());
			return true;
		}
		if (upgrade.getItem() instanceof CoreCellSizeUpgradeItem sizeUpgrade) {
			FarmCoreData.setCellSizeChunks(core, sizeUpgrade.sizeChunks());
			return true;
		}
		if (upgrade.is(BlackboxModItems.MOB_SPAWN_UPGRADE.get())) {
			FarmCoreData.setMobSpawningEnabled(core, true);
			return true;
		}
		return false;
	}

	public record AnvilUpgradeDisplay(ItemStack core, ItemStack upgrade, ItemStack result, long experienceCost) {
		public AnvilUpgradeDisplay {
			core = core.copy();
			upgrade = upgrade.copy();
			result = result.copy();
		}
	}
}
