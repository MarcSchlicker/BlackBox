package net.mcreator.blackbox.util;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;

import net.mcreator.blackbox.block.entity.DimensionalWorkbenchBlockEntity;
import net.mcreator.blackbox.init.BlackboxModItems;

import java.util.ArrayList;
import java.util.List;

public final class BlackboxSimulationRuntime {
	public static final int TICK_INTERVAL = 20;

	private BlackboxSimulationRuntime() {
	}

	public static void tick(ServerLevel level, BlockPos pos) {
		BlockEntity blockEntity = level.getBlockEntity(pos);
		if (!(blockEntity instanceof FarmSimulationMachine machine)) {
			return;
		}
		ItemStack core = machine.getItem(FarmSimulationMachine.CORE_SLOT);
		if (blockEntity instanceof DimensionalWorkbenchBlockEntity && FarmDimensionRuntime.isMeasurementActive(core)) {
			return;
		}
		if (!core.is(BlackboxModItems.DIMENSION_CORE.get()) || !FarmCoreData.isProgrammed(core)) {
			machine.setSimulationTicks(0);
			machine.setActiveCoreId("");
			machine.setStableCycleFunded(false);
			return;
		}

		FarmCoreData.Recipe recipe = FarmCoreData.read(core, level.registryAccess());
		if (!recipe.isValid()) {
			return;
		}
		String coreId = FarmCoreData.getCoreId(core).map(Object::toString).orElse("legacy-profile");
		boolean newCore = !coreId.equals(machine.getActiveCoreId());
		if (newCore) {
			machine.setActiveCoreId(coreId);
			machine.setStableCycleFunded(false);
		}

		boolean stable = machine.getItem(FarmSimulationMachine.UPGRADE_SLOT).is(BlackboxModItems.STABILITY_UPGRADE.get());
		if (stable) {
			tickStable(machine, recipe, newCore);
		} else {
			tickBatch(machine, recipe, newCore);
		}
	}

	private static void tickBatch(FarmSimulationMachine machine, FarmCoreData.Recipe recipe, boolean newCore) {
		if (newCore) {
			machine.setSimulationTicks(0);
			machine.setStableCycleFunded(false);
		}
		boolean cycleStart = !machine.isStableCycleFunded();
		if (cycleStart) {
			if (!canConsume(machine, recipe.inputs())) {
				return;
			}
			consume(machine, recipe.inputs());
			machine.setStableCycleFunded(true);
		}
		int before = machine.getSimulationTicks();
		int after = Math.min(recipe.sampleTicks(), before + TICK_INTERVAL);
		List<FarmCoreData.StackAmount> due = timelineOutputsDue(recipe, before, after);
		if (!due.isEmpty() && !canInsert(machine, due)) {
			return;
		}
		if (!due.isEmpty()) {
			insert(machine, due);
		}
		if (after >= recipe.sampleTicks()) {
			machine.setSimulationTicks(0);
			machine.setStableCycleFunded(false);
		} else {
			machine.setSimulationTicks(after);
		}
		machine.setChanged();
	}

	private static List<FarmCoreData.StackAmount> timelineOutputsDue(FarmCoreData.Recipe recipe, int before, int after) {
		List<FarmCoreData.StackAmount> due = new ArrayList<>();
		for (FarmCoreData.ProductionEvent event : recipe.timeline()) {
			if ((before == 0 && event.tick() == 0) || event.tick() > before && event.tick() <= after) {
				due.addAll(event.outputs());
			}
		}
		return due;
	}

	private static void tickStable(FarmSimulationMachine machine, FarmCoreData.Recipe recipe, boolean newCore) {
		if (newCore) {
			machine.setSimulationTicks(0);
		}
		if (!machine.isStableCycleFunded()) {
			if (!canConsume(machine, recipe.inputs())) {
				return;
			}
			consume(machine, recipe.inputs());
			machine.setStableCycleFunded(true);
		}

		int before = machine.getSimulationTicks();
		int after = Math.min(recipe.sampleTicks(), before + TICK_INTERVAL);
		List<FarmCoreData.StackAmount> due = outputsDue(recipe, before, after);
		if (!due.isEmpty() && !canInsert(machine, due)) {
			return;
		}
		if (!due.isEmpty()) {
			insert(machine, due);
		}
		if (after >= recipe.sampleTicks()) {
			machine.setSimulationTicks(0);
			machine.setStableCycleFunded(false);
		} else {
			machine.setSimulationTicks(after);
		}
		machine.setChanged();
	}

	private static List<FarmCoreData.StackAmount> outputsDue(FarmCoreData.Recipe recipe, int before, int after) {
		List<FarmCoreData.StackAmount> due = new ArrayList<>();
		for (FarmCoreData.StackAmount output : recipe.outputs()) {
			long beforeCount = before * output.amount() / recipe.sampleTicks();
			long afterCount = after * output.amount() / recipe.sampleTicks();
			if (afterCount > beforeCount) {
				due.add(new FarmCoreData.StackAmount(output.stack(), afterCount - beforeCount));
			}
		}
		return due;
	}

	private static boolean canConsume(FarmSimulationMachine machine, List<FarmCoreData.StackAmount> required) {
		for (FarmCoreData.StackAmount entry : required) {
			long available = 0;
			for (int slot = FarmSimulationMachine.INPUT_START; slot < FarmSimulationMachine.INPUT_END; slot++) {
				ItemStack stored = machine.getItem(slot);
				if (ItemStack.isSameItemSameComponents(stored, entry.stack())) {
					available += stored.getCount();
				}
			}
			if (available < entry.amount()) {
				return false;
			}
		}
		return true;
	}

	private static boolean canInsert(FarmSimulationMachine machine, List<FarmCoreData.StackAmount> produced) {
		List<ItemStack> slots = new ArrayList<>();
		for (int slot = FarmSimulationMachine.OUTPUT_START; slot < FarmSimulationMachine.OUTPUT_END; slot++) {
			slots.add(machine.getItem(slot).copy());
		}
		for (FarmCoreData.StackAmount entry : produced) {
			if (!insertIntoCopies(slots, entry.stack(), entry.amount())) {
				return false;
			}
		}
		return true;
	}

	private static boolean insertIntoCopies(List<ItemStack> slots, ItemStack template, long amount) {
		long remaining = amount;
		for (ItemStack stored : slots) {
			if (!stored.isEmpty() && ItemStack.isSameItemSameComponents(stored, template)) {
				int moved = (int) Math.min(remaining, stored.getMaxStackSize() - stored.getCount());
				stored.grow(moved);
				remaining -= moved;
			}
		}
		for (int slot = 0; slot < slots.size() && remaining > 0; slot++) {
			if (slots.get(slot).isEmpty()) {
				int moved = (int) Math.min(remaining, template.getMaxStackSize());
				slots.set(slot, template.copyWithCount(moved));
				remaining -= moved;
			}
		}
		return remaining == 0;
	}

	private static void consume(FarmSimulationMachine machine, List<FarmCoreData.StackAmount> required) {
		for (FarmCoreData.StackAmount entry : required) {
			long remaining = entry.amount();
			for (int slot = FarmSimulationMachine.INPUT_START; slot < FarmSimulationMachine.INPUT_END && remaining > 0; slot++) {
				ItemStack stored = machine.getItem(slot);
				if (ItemStack.isSameItemSameComponents(stored, entry.stack())) {
					int removed = (int) Math.min(remaining, stored.getCount());
					stored.shrink(removed);
					remaining -= removed;
				}
			}
		}
	}

	public static boolean insert(FarmSimulationMachine machine, List<FarmCoreData.StackAmount> produced) {
		if (!canInsert(machine, produced)) {
			return false;
		}
		for (FarmCoreData.StackAmount entry : produced) {
			long remaining = entry.amount();
			for (int slot = FarmSimulationMachine.OUTPUT_START; slot < FarmSimulationMachine.OUTPUT_END && remaining > 0; slot++) {
				ItemStack stored = machine.getItem(slot);
				if (!stored.isEmpty() && ItemStack.isSameItemSameComponents(stored, entry.stack())) {
					int moved = (int) Math.min(remaining, stored.getMaxStackSize() - stored.getCount());
					stored.grow(moved);
					remaining -= moved;
				}
			}
			for (int slot = FarmSimulationMachine.OUTPUT_START; slot < FarmSimulationMachine.OUTPUT_END && remaining > 0; slot++) {
				if (machine.getItem(slot).isEmpty()) {
					int moved = (int) Math.min(remaining, entry.stack().getMaxStackSize());
					machine.setItem(slot, entry.stack().copyWithCount(moved));
					remaining -= moved;
				}
			}
		}
		return true;
	}
}
