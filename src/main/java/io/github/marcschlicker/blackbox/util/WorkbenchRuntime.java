package io.github.marcschlicker.blackbox.util;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;

import io.github.marcschlicker.blackbox.init.BlackboxModItems;

public final class WorkbenchRuntime {
	private WorkbenchRuntime() {
	}

	public static void tick(ServerLevel level, BlockPos pos) {
		if (!(level.getBlockEntity(pos) instanceof FarmMeasurementHost workbench)) {
			return;
		}
		// A direct-workbench BlackBox can have an output filter. Drop disabled
		// pending measurement results before they reach its visible outputs.
		BlackboxSimulationRuntime.discardFilteredPendingOutputs(workbench, level.registryAccess());
		workbench.measurementOutputBuffer().flushInto(workbench);
		ItemStack core = workbench.getItem(FarmSimulationMachine.CORE_SLOT);
		if (FarmDimensionRuntime.isSimulationPaused(level.getServer(), core)) {
			return;
		}

		BlackboxSimulationRuntime.tick(level, pos);
	}
}
