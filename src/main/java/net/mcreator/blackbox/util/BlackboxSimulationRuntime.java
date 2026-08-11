package net.mcreator.blackbox.util;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;

import net.mcreator.blackbox.block.entity.DimensionalWorkbenchBlockEntity;
import net.mcreator.blackbox.init.BlackboxModItems;

import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;

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
		if (!fundCycle(machine, recipe)) {
			return;
		}
		int before = machine.getSimulationTicks();
		int after = Math.min(recipe.sampleTicks(), before + TICK_INTERVAL);
		ResourceDue due = new ResourceDue(timelineOutputsDue(recipe, before, after),
				after >= recipe.sampleTicks() ? recipe.fluidOutputs() : List.of(),
				after >= recipe.sampleTicks() ? recipe.energyOutput() : 0);
		if (!canInsert(machine, due)) {
			return;
		}
		insert(machine, due);
		finishTick(machine, recipe, after);
	}

	private static void tickStable(FarmSimulationMachine machine, FarmCoreData.Recipe recipe, boolean newCore) {
		if (newCore) {
			machine.setSimulationTicks(0);
		}
		if (!fundCycle(machine, recipe)) {
			return;
		}
		int before = machine.getSimulationTicks();
		int after = Math.min(recipe.sampleTicks(), before + TICK_INTERVAL);
		ResourceDue due = new ResourceDue(outputsDue(recipe.outputs(), recipe.sampleTicks(), before, after),
				fluidOutputsDue(recipe.fluidOutputs(), recipe.sampleTicks(), before, after),
				amountDue(recipe.energyOutput(), recipe.sampleTicks(), before, after));
		if (!canInsert(machine, due)) {
			return;
		}
		insert(machine, due);
		finishTick(machine, recipe, after);
	}

	private static boolean fundCycle(FarmSimulationMachine machine, FarmCoreData.Recipe recipe) {
		if (machine.isStableCycleFunded()) {
			return true;
		}
		if (!canConsume(machine, recipe)) {
			return false;
		}
		consume(machine, recipe);
		machine.setStableCycleFunded(true);
		return true;
	}

	private static void finishTick(FarmSimulationMachine machine, FarmCoreData.Recipe recipe, int after) {
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

	private static List<FarmCoreData.StackAmount> outputsDue(List<FarmCoreData.StackAmount> outputs, int sampleTicks, int before, int after) {
		List<FarmCoreData.StackAmount> due = new ArrayList<>();
		for (FarmCoreData.StackAmount output : outputs) {
			long amount = amountDue(output.amount(), sampleTicks, before, after);
			if (amount > 0) {
				due.add(new FarmCoreData.StackAmount(output.stack(), amount));
			}
		}
		return due;
	}

	private static List<FarmCoreData.FluidAmount> fluidOutputsDue(List<FarmCoreData.FluidAmount> outputs, int sampleTicks, int before, int after) {
		List<FarmCoreData.FluidAmount> due = new ArrayList<>();
		for (FarmCoreData.FluidAmount output : outputs) {
			long amount = amountDue(output.amount(), sampleTicks, before, after);
			if (amount > 0) {
				due.add(new FarmCoreData.FluidAmount(output.stack(), amount));
			}
		}
		return due;
	}

	private static long amountDue(long total, int sampleTicks, int before, int after) {
		long beforeAmount = before * total / sampleTicks;
		long afterAmount = after * total / sampleTicks;
		return Math.max(0, afterAmount - beforeAmount);
	}

	private static boolean canConsume(FarmSimulationMachine machine, FarmCoreData.Recipe recipe) {
		for (FarmCoreData.StackAmount entry : recipe.inputs()) {
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
		for (FarmCoreData.FluidAmount entry : recipe.fluidInputs()) {
			if (machine.resources().inputFluids().amountOf(entry.stack()) < entry.amount()) {
				return false;
			}
		}
		return machine.resources().inputEnergy().getEnergyStored() >= recipe.energyInput();
	}

	private static void consume(FarmSimulationMachine machine, FarmCoreData.Recipe recipe) {
		for (FarmCoreData.StackAmount entry : recipe.inputs()) {
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
		for (FarmCoreData.FluidAmount entry : recipe.fluidInputs()) {
			machine.resources().inputFluids().drainLong(entry.stack(), entry.amount(), IFluidHandler.FluidAction.EXECUTE);
		}
		long energy = recipe.energyInput();
		while (energy > 0) {
			int moved = machine.resources().inputEnergy().extractEnergy((int) Math.min(Integer.MAX_VALUE, energy), false);
			if (moved <= 0) {
				break;
			}
			energy -= moved;
		}
	}

	private static boolean canInsert(FarmSimulationMachine machine, ResourceDue due) {
		return canInsertItems(machine, due.items()) && canInsertFluids(machine.resources().outputFluids(), due.fluids())
				&& due.energy() <= machine.resources().outputEnergy().getMaxEnergyStored() - machine.resources().outputEnergy().getEnergyStored();
	}

	private static boolean canInsertItems(FarmSimulationMachine machine, List<FarmCoreData.StackAmount> produced) {
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

	private static boolean canInsertFluids(MultiFluidTank storage, List<FarmCoreData.FluidAmount> produced) {
		List<FluidStack> tanks = new ArrayList<>();
		List<Integer> capacities = new ArrayList<>();
		for (int tank = 0; tank < storage.getTanks(); tank++) {
			tanks.add(storage.getFluidInTank(tank).copy());
			capacities.add(storage.getTankCapacity(tank));
		}
		for (FarmCoreData.FluidAmount entry : produced) {
			long remaining = entry.amount();
			for (int tank = 0; tank < tanks.size() && remaining > 0; tank++) {
				FluidStack stored = tanks.get(tank);
				if (!stored.isEmpty() && FluidStack.isSameFluidSameComponents(stored, entry.stack())) {
					int moved = (int) Math.min(remaining, capacities.get(tank) - stored.getAmount());
					stored.grow(moved);
					remaining -= moved;
				}
			}
			for (int tank = 0; tank < tanks.size() && remaining > 0; tank++) {
				if (tanks.get(tank).isEmpty()) {
					int moved = (int) Math.min(remaining, capacities.get(tank));
					tanks.set(tank, entry.stack().copyWithAmount(moved));
					remaining -= moved;
				}
			}
			if (remaining > 0) {
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

	private static void insert(FarmSimulationMachine machine, ResourceDue due) {
		insert(machine, due.items());
		for (FarmCoreData.FluidAmount entry : due.fluids()) {
			machine.resources().outputFluids().fillLong(entry.stack(), entry.amount(), IFluidHandler.FluidAction.EXECUTE);
		}
		long energy = due.energy();
		while (energy > 0) {
			int moved = machine.resources().outputEnergy().receiveEnergy((int) Math.min(Integer.MAX_VALUE, energy), false);
			if (moved <= 0) {
				break;
			}
			energy -= moved;
		}
	}

	public static boolean insert(FarmSimulationMachine machine, List<FarmCoreData.StackAmount> produced) {
		if (!canInsertItems(machine, produced)) {
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

	private record ResourceDue(List<FarmCoreData.StackAmount> items, List<FarmCoreData.FluidAmount> fluids, long energy) {
		private ResourceDue {
			items = List.copyOf(items);
			fluids = List.copyOf(fluids);
			energy = Math.max(0, energy);
		}
	}
}
