package net.mcreator.blackbox.util;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.item.ItemStack;

import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;

import java.util.ArrayList;
import java.util.List;

public final class MeasurementOutputBuffer {
	private final List<FarmCoreData.StackAmount> items = new ArrayList<>();
	private final List<FarmCoreData.FluidAmount> fluids = new ArrayList<>();
	private final Runnable changed;
	private long energy;

	public MeasurementOutputBuffer(Runnable changed) {
		this.changed = changed;
	}

	public void addItem(ItemStack stack, long amount) {
		if (stack.isEmpty() || amount <= 0) {
			return;
		}
		for (int index = 0; index < this.items.size(); index++) {
			FarmCoreData.StackAmount entry = this.items.get(index);
			if (ItemStack.isSameItemSameComponents(entry.stack(), stack)) {
				this.items.set(index, new FarmCoreData.StackAmount(entry.stack(), saturatedAdd(entry.amount(), amount)));
				this.changed.run();
				return;
			}
		}
		this.items.add(new FarmCoreData.StackAmount(stack.copyWithCount(1), amount));
		this.changed.run();
	}

	public void addFluid(FluidStack stack, long amount) {
		if (stack.isEmpty() || amount <= 0) {
			return;
		}
		for (int index = 0; index < this.fluids.size(); index++) {
			FarmCoreData.FluidAmount entry = this.fluids.get(index);
			if (FluidStack.isSameFluidSameComponents(entry.stack(), stack)) {
				this.fluids.set(index, new FarmCoreData.FluidAmount(entry.stack(), saturatedAdd(entry.amount(), amount)));
				this.changed.run();
				return;
			}
		}
		this.fluids.add(new FarmCoreData.FluidAmount(stack.copyWithAmount(1), amount));
		this.changed.run();
	}

	public void addEnergy(long amount) {
		if (amount > 0) {
			this.energy = saturatedAdd(this.energy, amount);
			this.changed.run();
		}
	}

	public void flushInto(FarmSimulationMachine machine) {
		boolean moved = flushItems(machine);
		moved |= flushFluids(machine.resources().outputFluids());
		long beforeEnergy = this.energy;
		while (this.energy > 0) {
			int accepted = machine.resources().outputEnergy().receiveEnergy((int) Math.min(Integer.MAX_VALUE, this.energy), false);
			if (accepted <= 0) {
				break;
			}
			this.energy -= accepted;
		}
		if (moved || beforeEnergy != this.energy) {
			this.changed.run();
		}
	}

	public CompoundTag save(HolderLookup.Provider lookupProvider) {
		CompoundTag root = new CompoundTag();
		ListTag itemList = new ListTag();
		for (FarmCoreData.StackAmount entry : this.items) {
			CompoundTag tag = new CompoundTag();
			tag.put("Stack", entry.stack().saveOptional(lookupProvider));
			tag.putLong("Amount", entry.amount());
			itemList.add(tag);
		}
		root.put("Items", itemList);
		ListTag fluidList = new ListTag();
		for (FarmCoreData.FluidAmount entry : this.fluids) {
			CompoundTag tag = new CompoundTag();
			tag.put("Stack", entry.stack().saveOptional(lookupProvider));
			tag.putLong("Amount", entry.amount());
			fluidList.add(tag);
		}
		root.put("Fluids", fluidList);
		root.putLong("Energy", this.energy);
		return root;
	}

	public void load(CompoundTag root, HolderLookup.Provider lookupProvider) {
		this.items.clear();
		ListTag itemList = root.getList("Items", Tag.TAG_COMPOUND);
		for (int index = 0; index < itemList.size(); index++) {
			CompoundTag tag = itemList.getCompound(index);
			ItemStack stack = ItemStack.parseOptional(lookupProvider, tag.getCompound("Stack"));
			long amount = Math.max(0, tag.getLong("Amount"));
			if (!stack.isEmpty() && amount > 0) {
				this.items.add(new FarmCoreData.StackAmount(stack.copyWithCount(1), amount));
			}
		}
		this.fluids.clear();
		ListTag fluidList = root.getList("Fluids", Tag.TAG_COMPOUND);
		for (int index = 0; index < fluidList.size(); index++) {
			CompoundTag tag = fluidList.getCompound(index);
			FluidStack stack = FluidStack.parseOptional(lookupProvider, tag.getCompound("Stack"));
			long amount = Math.max(0, tag.getLong("Amount"));
			if (!stack.isEmpty() && amount > 0) {
				this.fluids.add(new FarmCoreData.FluidAmount(stack.copyWithAmount(1), amount));
			}
		}
		this.energy = Math.max(0, root.getLong("Energy"));
	}

	private boolean flushItems(FarmSimulationMachine machine) {
		boolean movedAny = false;
		for (int index = 0; index < this.items.size();) {
			FarmCoreData.StackAmount entry = this.items.get(index);
			long remaining = entry.amount();
			for (int slot = FarmSimulationMachine.OUTPUT_START; slot < FarmSimulationMachine.OUTPUT_END && remaining > 0; slot++) {
				ItemStack stored = machine.getItem(slot);
				if (!stored.isEmpty() && ItemStack.isSameItemSameComponents(stored, entry.stack())) {
					int amount = (int) Math.min(remaining, stored.getMaxStackSize() - stored.getCount());
					if (amount > 0) {
						stored.grow(amount);
						remaining -= amount;
						movedAny = true;
					}
				}
			}
			for (int slot = FarmSimulationMachine.OUTPUT_START; slot < FarmSimulationMachine.OUTPUT_END && remaining > 0; slot++) {
				if (machine.getItem(slot).isEmpty()) {
					int amount = (int) Math.min(remaining, entry.stack().getMaxStackSize());
					machine.setItem(slot, entry.stack().copyWithCount(amount));
					remaining -= amount;
					movedAny = true;
				}
			}
			if (remaining == 0) {
				this.items.remove(index);
			} else {
				this.items.set(index, new FarmCoreData.StackAmount(entry.stack(), remaining));
				index++;
			}
		}
		return movedAny;
	}

	private boolean flushFluids(MultiFluidTank target) {
		boolean movedAny = false;
		for (int index = 0; index < this.fluids.size();) {
			FarmCoreData.FluidAmount entry = this.fluids.get(index);
			long moved = target.fillLong(entry.stack(), entry.amount(), IFluidHandler.FluidAction.EXECUTE);
			long remaining = entry.amount() - moved;
			movedAny |= moved > 0;
			if (remaining == 0) {
				this.fluids.remove(index);
			} else {
				this.fluids.set(index, new FarmCoreData.FluidAmount(entry.stack(), remaining));
				index++;
			}
		}
		return movedAny;
	}

	private static long saturatedAdd(long first, long second) {
		return Long.MAX_VALUE - first < second ? Long.MAX_VALUE : first + second;
	}
}
