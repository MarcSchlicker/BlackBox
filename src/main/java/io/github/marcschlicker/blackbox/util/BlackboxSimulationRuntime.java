package io.github.marcschlicker.blackbox.util;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;

import io.github.marcschlicker.blackbox.init.BlackboxModItems;
import io.github.marcschlicker.blackbox.block.entity.BlackboxBlockBlockEntity;

import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

public final class BlackboxSimulationRuntime {
	public static final int TICK_INTERVAL = 20;
	private static final int MINUTE_TICKS = 20 * 60;

	private BlackboxSimulationRuntime() {
	}

	public static void tick(ServerLevel level, BlockPos pos) {
		BlockEntity blockEntity = level.getBlockEntity(pos);
		if (!(blockEntity instanceof FarmSimulationMachine machine)) {
			return;
		}
		ItemStack core = machine.getItem(FarmSimulationMachine.CORE_SLOT);
		if (FarmDimensionRuntime.isSimulationPaused(level.getServer(), core)) {
			machine.outputBuffer().flushInto(machine);
			return;
		}
		if (!core.is(BlackboxModItems.DIMENSION_CORE.get()) || !FarmCoreData.isProgrammed(core)) {
			machine.outputBuffer().flushInto(machine);
			machine.setSimulationTicks(0);
			machine.setActiveCoreId("");
			machine.setStableCycleFunded(false);
			machine.setSimulationCycleSeed(0L);
			machine.durabilityOutputs().clear();
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
		machine.durabilityOutputs().resetForCore(coreId);
		discardFilteredDurability(machine, level.registryAccess());
		discardFilteredPendingOutputs(machine, level.registryAccess());
		// Pending outputs are persistent, so a full visible tank or inventory never
		// freezes the farm profile. They are delivered as soon as downstream space exists.
		machine.outputBuffer().flushInto(machine);

		boolean stable = machine.getItem(FarmSimulationMachine.UPGRADE_SLOT).is(BlackboxModItems.STABILITY_UPGRADE.get());
		if (stable) {
			tickStable(machine, recipe, newCore, level.registryAccess());
		} else {
			tickBatch(machine, recipe, newCore, level.registryAccess());
		}
	}

	private static void tickBatch(FarmSimulationMachine machine, FarmCoreData.Recipe recipe, boolean newCore, net.minecraft.core.HolderLookup.Provider lookupProvider) {
		if (newCore) {
			machine.setSimulationTicks(0);
			machine.setStableCycleFunded(false);
			machine.setSimulationCycleSeed(0L);
		}
		if (!beginCycle(machine, recipe)) {
			return;
		}
		int before = machine.getSimulationTicks();
		int after = Math.min(recipe.sampleTicks(), before + TICK_INTERVAL);
		if (!consumeInputsDue(machine, recipe, before, after)) {
			return;
		}
		List<FarmCoreData.DurabilityAmount> durabilityDue = after >= recipe.sampleTicks()
				? filterDurabilityOutputs(machine, recipe.durabilityOutputs(), lookupProvider) : List.of();
		List<FarmCoreData.StackAmount> itemDue = new ArrayList<>(filterItemOutputs(machine, timelineOutputsDue(recipe, before, after, machine.getSimulationCycleSeed()), lookupProvider));
		itemDue.addAll(machine.durabilityOutputs().previewCompleted(durabilityDue));
		ResourceDue due = new ResourceDue(itemDue,
				filterFluidOutputs(machine, minuteFluidOutputsDue(recipe.fluidOutputs(), recipe.sampleTicks(), before, after), lookupProvider),
				isEnergyOutputEnabled(machine) ? minuteAmountDue(recipe.energyOutput(), recipe.sampleTicks(), before, after) : 0);
		queueOutputs(machine, due);
		machine.durabilityOutputs().accept(durabilityDue);
		finishTick(machine, recipe, after);
	}

	private static void tickStable(FarmSimulationMachine machine, FarmCoreData.Recipe recipe, boolean newCore, net.minecraft.core.HolderLookup.Provider lookupProvider) {
		if (newCore) {
			machine.setSimulationTicks(0);
		}
		if (!beginCycle(machine, recipe)) {
			return;
		}
		int before = machine.getSimulationTicks();
		int after = Math.min(recipe.sampleTicks(), before + TICK_INTERVAL);
		if (!consumeInputsDue(machine, recipe, before, after)) {
			return;
		}
		List<FarmCoreData.DurabilityAmount> durabilityDue = filterDurabilityOutputs(machine,
				durabilityOutputsDue(recipe.durabilityOutputs(), recipe.sampleTicks(), before, after), lookupProvider);
		List<FarmCoreData.StackAmount> itemDue = new ArrayList<>(filterItemOutputs(machine, outputsDue(recipe.outputs(), recipe.sampleTicks(), before, after), lookupProvider));
		itemDue.addAll(machine.durabilityOutputs().previewCompleted(durabilityDue));
		ResourceDue due = new ResourceDue(itemDue,
				filterFluidOutputs(machine, fluidOutputsDue(recipe.fluidOutputs(), recipe.sampleTicks(), before, after), lookupProvider),
				isEnergyOutputEnabled(machine) ? amountDue(recipe.energyOutput(), recipe.sampleTicks(), before, after) : 0);
		queueOutputs(machine, due);
		machine.durabilityOutputs().accept(durabilityDue);
		finishTick(machine, recipe, after);
	}

	/**
	 * Starts a cycle without demanding its complete fluid, item, or FE cost up
	 * front. A 10-minute profile may legitimately need more than the BlackBox's
	 * 500 kFE tank, provided it is supplied while it runs.
	 */
	private static boolean beginCycle(FarmSimulationMachine machine, FarmCoreData.Recipe recipe) {
		if (machine.isStableCycleFunded()) {
			return true;
		}
		if (!canConsumeEntityInputs(machine, recipe)) {
			return false;
		}
		consumeEntityInputs(machine, recipe);
		machine.setStableCycleFunded(true);
		machine.setSimulationCycleSeed(ThreadLocalRandom.current().nextLong());
		return true;
	}

	/** Consumes the exact fraction required by the next simulation interval. */
	private static boolean consumeInputsDue(FarmSimulationMachine machine, FarmCoreData.Recipe recipe, int before, int after) {
		for (FarmCoreData.StackAmount entry : recipe.inputs()) {
			long due = amountDue(entry.amount(), recipe.sampleTicks(), before, after);
			if (due > 0L && availableItems(machine, entry.stack()) < due) {
				return false;
			}
		}
		for (FarmCoreData.FluidAmount entry : recipe.fluidInputs()) {
			long due = amountDue(entry.amount(), recipe.sampleTicks(), before, after);
			if (due > 0L && machine.resources().inputFluids().amountOf(entry.stack()) < due) {
				return false;
			}
		}
		long energyDue = amountDue(recipe.energyInput(), recipe.sampleTicks(), before, after);
		if (machine.resources().inputEnergy().getEnergyStored() < energyDue) {
			return false;
		}

		for (FarmCoreData.StackAmount entry : recipe.inputs()) {
			long remaining = amountDue(entry.amount(), recipe.sampleTicks(), before, after);
			for (int slot = FarmSimulationMachine.INPUT_START; slot < FarmSimulationMachine.INPUT_END && remaining > 0L; slot++) {
				ItemStack stored = machine.getItem(slot);
				if (ItemStack.isSameItemSameComponents(stored, entry.stack())) {
					int removed = (int) Math.min(remaining, stored.getCount());
					stored.shrink(removed);
					remaining -= removed;
				}
			}
		}
		for (FarmCoreData.FluidAmount entry : recipe.fluidInputs()) {
			long due = amountDue(entry.amount(), recipe.sampleTicks(), before, after);
			if (due > 0L) {
				machine.resources().inputFluids().drainLong(entry.stack(), due, IFluidHandler.FluidAction.EXECUTE);
			}
		}
		long remainingEnergy = energyDue;
		while (remainingEnergy > 0L) {
			int removed = machine.resources().inputEnergy().extractEnergy((int) Math.min(Integer.MAX_VALUE, remainingEnergy), false);
			if (removed <= 0) {
				break;
			}
			remainingEnergy -= removed;
		}
		return true;
	}

	private static void finishTick(FarmSimulationMachine machine, FarmCoreData.Recipe recipe, int after) {
		if (after >= recipe.sampleTicks()) {
			machine.setSimulationTicks(0);
			machine.setStableCycleFunded(false);
			machine.setSimulationCycleSeed(0L);
		} else {
			machine.setSimulationTicks(after);
		}
		machine.setChanged();
	}

	private static List<FarmCoreData.StackAmount> timelineOutputsDue(FarmCoreData.Recipe recipe, int before, int after, long cycleSeed) {
		List<FarmCoreData.StackAmount> due = new ArrayList<>();
		for (int index = 0; index < recipe.timeline().size(); index++) {
			FarmCoreData.ProductionEvent event = recipe.timeline().get(index);
			int eventTick = randomizedEventTick(event.tick(), recipe.sampleTicks(), cycleSeed, index);
			if ((before == 0 && eventTick == 0) || eventTick > before && eventTick <= after) {
				due.addAll(event.outputs());
			}
		}
		return due;
	}

	private static int randomizedEventTick(int measuredTick, int sampleTicks, long cycleSeed, int eventIndex) {
		int jitter = Math.max(TICK_INTERVAL, sampleTicks / 12);
		long mixed = cycleSeed + 0x9E3779B97F4A7C15L * (eventIndex + 1L);
		mixed = (mixed ^ (mixed >>> 30)) * 0xBF58476D1CE4E5B9L;
		mixed = (mixed ^ (mixed >>> 27)) * 0x94D049BB133111EBL;
		mixed ^= mixed >>> 31;
		int offset = (int) Math.floorMod(mixed, jitter * 2L + 1L) - jitter;
		int shifted = Math.max(0, Math.min(sampleTicks, measuredTick + offset));
		return shifted / TICK_INTERVAL * TICK_INTERVAL;
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

	private static List<FarmCoreData.FluidAmount> minuteFluidOutputsDue(List<FarmCoreData.FluidAmount> outputs, int sampleTicks, int before, int after) {
		List<FarmCoreData.FluidAmount> due = new ArrayList<>();
		for (FarmCoreData.FluidAmount output : outputs) {
			long amount = minuteAmountDue(output.amount(), sampleTicks, before, after);
			if (amount > 0) {
				due.add(new FarmCoreData.FluidAmount(output.stack(), amount));
			}
		}
		return due;
	}

	private static long minuteAmountDue(long total, int sampleTicks, int before, int after) {
		long due = 0;
		int completedMinutes = sampleTicks / MINUTE_TICKS;
		for (int minute = 1; minute <= completedMinutes; minute++) {
			int tick = minute * MINUTE_TICKS;
			if (tick > before && tick <= after) {
				int previousTick = (minute - 1) * MINUTE_TICKS;
				due += tick * total / sampleTicks - previousTick * total / sampleTicks;
			}
		}
		if (sampleTicks % MINUTE_TICKS != 0 && sampleTicks > before && sampleTicks <= after) {
			int previousTick = completedMinutes * MINUTE_TICKS;
			due += total - previousTick * total / sampleTicks;
		}
		return Math.max(0, due);
	}

	private static List<FarmCoreData.DurabilityAmount> durabilityOutputsDue(List<FarmCoreData.DurabilityAmount> outputs, int sampleTicks, int before, int after) {
		List<FarmCoreData.DurabilityAmount> due = new ArrayList<>();
		for (FarmCoreData.DurabilityAmount output : outputs) {
			long amount = amountDue(output.durability(), sampleTicks, before, after);
			if (amount > 0) {
				due.add(new FarmCoreData.DurabilityAmount(output.stack(), amount));
			}
		}
		return due;
	}

	private static long amountDue(long total, int sampleTicks, int before, int after) {
		long beforeAmount = before * total / sampleTicks;
		long afterAmount = after * total / sampleTicks;
		return Math.max(0, afterAmount - beforeAmount);
	}

	private static List<FarmCoreData.StackAmount> filterItemOutputs(FarmSimulationMachine machine, List<FarmCoreData.StackAmount> outputs,
			net.minecraft.core.HolderLookup.Provider lookupProvider) {
		List<FarmCoreData.StackAmount> enabled = new ArrayList<>();
		for (FarmCoreData.StackAmount output : outputs) {
			if (isItemOutputEnabled(machine, output.stack(), lookupProvider)) {
				enabled.add(output);
			}
		}
		return enabled;
	}

	private static List<FarmCoreData.DurabilityAmount> filterDurabilityOutputs(FarmSimulationMachine machine, List<FarmCoreData.DurabilityAmount> outputs,
			net.minecraft.core.HolderLookup.Provider lookupProvider) {
		List<FarmCoreData.DurabilityAmount> enabled = new ArrayList<>();
		for (FarmCoreData.DurabilityAmount output : outputs) {
			if (isItemOutputEnabled(machine, output.stack(), lookupProvider)) {
				enabled.add(output);
			}
		}
		return enabled;
	}

	private static List<FarmCoreData.FluidAmount> filterFluidOutputs(FarmSimulationMachine machine, List<FarmCoreData.FluidAmount> outputs,
			net.minecraft.core.HolderLookup.Provider lookupProvider) {
		List<FarmCoreData.FluidAmount> enabled = new ArrayList<>();
		for (FarmCoreData.FluidAmount output : outputs) {
			if (isFluidOutputEnabled(machine, output.stack(), lookupProvider)) {
				enabled.add(output);
			}
		}
		return enabled;
	}

	private static void discardFilteredDurability(FarmSimulationMachine machine, net.minecraft.core.HolderLookup.Provider lookupProvider) {
		machine.durabilityOutputs().discardIf(output -> !isItemOutputEnabled(machine, output, lookupProvider));
	}

	/** Removes already-buffered outputs that the BlackBox's installed filter now rejects. */
	public static void discardFilteredPendingOutputs(FarmSimulationMachine machine, net.minecraft.core.HolderLookup.Provider lookupProvider) {
		machine.outputBuffer().discardItemsIf(output -> !isItemOutputEnabled(machine, output, lookupProvider));
		machine.outputBuffer().discardFluidsIf(output -> !isFluidOutputEnabled(machine, output, lookupProvider));
		if (!isEnergyOutputEnabled(machine)) {
			machine.outputBuffer().discardEnergy();
		}
	}

	public static boolean isItemOutputEnabled(FarmSimulationMachine machine, ItemStack output, net.minecraft.core.HolderLookup.Provider lookupProvider) {
		if (!(machine instanceof BlackboxBlockBlockEntity blackbox)) {
			return true;
		}
		ItemStack filter = blackbox.getItem(BlackboxBlockBlockEntity.FILTER_UPGRADE_SLOT);
		return !filter.is(BlackboxModItems.OUTPUT_FILTER_UPGRADE.get()) || OutputFilterData.isEnabled(filter, output, lookupProvider);
	}

	public static boolean isFluidOutputEnabled(FarmSimulationMachine machine, FluidStack output, net.minecraft.core.HolderLookup.Provider lookupProvider) {
		if (!(machine instanceof BlackboxBlockBlockEntity blackbox)) {
			return true;
		}
		ItemStack filter = blackbox.getItem(BlackboxBlockBlockEntity.FILTER_UPGRADE_SLOT);
		return !filter.is(BlackboxModItems.OUTPUT_FILTER_UPGRADE.get()) || OutputFilterData.isFluidEnabled(filter, output, lookupProvider);
	}

	public static boolean isEnergyOutputEnabled(FarmSimulationMachine machine) {
		if (!(machine instanceof BlackboxBlockBlockEntity blackbox)) {
			return true;
		}
		ItemStack filter = blackbox.getItem(BlackboxBlockBlockEntity.FILTER_UPGRADE_SLOT);
		return !filter.is(BlackboxModItems.OUTPUT_FILTER_UPGRADE.get()) || OutputFilterData.isEnergyEnabled(filter);
	}

	private static boolean canConsumeEntityInputs(FarmSimulationMachine machine, FarmCoreData.Recipe recipe) {
		for (FarmCoreData.EntityAmount entry : recipe.entityInputs()) {
			if (machine.mobInputs().amountOf(entry.entityType()) < entry.amount()) {
				return false;
			}
		}
		return true;
	}

	private static long availableItems(FarmSimulationMachine machine, ItemStack template) {
		long available = 0L;
		for (int slot = FarmSimulationMachine.INPUT_START; slot < FarmSimulationMachine.INPUT_END; slot++) {
			ItemStack stored = machine.getItem(slot);
			if (ItemStack.isSameItemSameComponents(stored, template)) {
				available += stored.getCount();
			}
		}
		return available;
	}

	private static void consumeEntityInputs(FarmSimulationMachine machine, FarmCoreData.Recipe recipe) {
		for (FarmCoreData.EntityAmount entry : recipe.entityInputs()) {
			machine.mobInputs().consume(entry.entityType(), entry.amount());
		}
	}

	private static void queueOutputs(FarmSimulationMachine machine, ResourceDue due) {
		for (FarmCoreData.StackAmount entry : due.items()) {
			machine.outputBuffer().addItem(entry.stack(), entry.amount());
		}
		for (FarmCoreData.FluidAmount entry : due.fluids()) {
			machine.outputBuffer().addFluid(entry.stack(), entry.amount());
		}
		machine.outputBuffer().addEnergy(due.energy());
		machine.outputBuffer().flushInto(machine);
	}

	private record ResourceDue(List<FarmCoreData.StackAmount> items, List<FarmCoreData.FluidAmount> fluids, long energy) {
		private ResourceDue {
			items = List.copyOf(items);
			fluids = List.copyOf(fluids);
			energy = Math.max(0, energy);
		}
	}
}
