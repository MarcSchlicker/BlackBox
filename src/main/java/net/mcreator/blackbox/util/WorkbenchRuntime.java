package net.mcreator.blackbox.util;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;

import net.mcreator.blackbox.block.entity.DimensionalWorkbenchBlockEntity;
import net.mcreator.blackbox.init.BlackboxModItems;
import net.mcreator.blackbox.item.CoreEnvironmentUpgradeItem;
import net.mcreator.blackbox.item.CoreCellSizeUpgradeItem;

public final class WorkbenchRuntime {
	private WorkbenchRuntime() {
	}

	public static void tick(ServerLevel level, BlockPos pos) {
		if (!(level.getBlockEntity(pos) instanceof DimensionalWorkbenchBlockEntity workbench)) {
			return;
		}
		ItemStack core = workbench.getItem(FarmSimulationMachine.CORE_SLOT);
		if (FarmDimensionRuntime.isMeasurementActive(core)) {
			return;
		}

		ItemStack upgrade = workbench.getItem(FarmSimulationMachine.UPGRADE_SLOT);
		if (core.is(BlackboxModItems.DIMENSION_CORE.get()) && FarmCoreData.getCoreId(core).isEmpty()
				&& upgrade.getItem() instanceof CoreEnvironmentUpgradeItem environmentUpgrade) {
			FarmCoreData.setEnvironment(core, environmentUpgrade.environment());
			FarmCoreData.clearProfile(core, level.registryAccess());
			upgrade.shrink(1);
			if (upgrade.isEmpty()) {
				workbench.setItem(FarmSimulationMachine.UPGRADE_SLOT, ItemStack.EMPTY);
			}
			workbench.setActiveCoreId("");
			workbench.setSimulationTicks(0);
			workbench.setChanged();
		} else if (core.is(BlackboxModItems.DIMENSION_CORE.get()) && FarmCoreData.getCoreId(core).isEmpty()
				&& upgrade.getItem() instanceof CoreCellSizeUpgradeItem sizeUpgrade) {
			FarmCoreData.setCellSizeChunks(core, sizeUpgrade.sizeChunks());
			FarmCoreData.clearProfile(core, level.registryAccess());
			upgrade.shrink(1);
			if (upgrade.isEmpty()) {
				workbench.setItem(FarmSimulationMachine.UPGRADE_SLOT, ItemStack.EMPTY);
			}
			workbench.setActiveCoreId("");
			workbench.setSimulationTicks(0);
			workbench.setChanged();
		} else if (core.is(BlackboxModItems.DIMENSION_CORE.get()) && upgrade.is(BlackboxModItems.MOB_SPAWN_UPGRADE.get())) {
			FarmCoreData.setMobSpawningEnabled(core, true);
			FarmCoreData.clearProfile(core, level.registryAccess());
			upgrade.shrink(1);
			if (upgrade.isEmpty()) {
				workbench.setItem(FarmSimulationMachine.UPGRADE_SLOT, ItemStack.EMPTY);
			}
			workbench.setActiveCoreId("");
			workbench.setSimulationTicks(0);
			workbench.setChanged();
		}
		BlackboxSimulationRuntime.tick(level, pos);
	}
}
