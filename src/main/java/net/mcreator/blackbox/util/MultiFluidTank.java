package net.mcreator.blackbox.util;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;

import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.capability.templates.FluidTank;

public final class MultiFluidTank implements IFluidHandler {
	private final FluidTank[] tanks;
	private final Runnable changed;

	public MultiFluidTank(int tankCount, int capacityPerTank, Runnable changed) {
		this.changed = changed;
		this.tanks = new FluidTank[Math.max(1, tankCount)];
		for (int index = 0; index < this.tanks.length; index++) {
			this.tanks[index] = new FluidTank(capacityPerTank) {
				@Override
				protected void onContentsChanged() {
					MultiFluidTank.this.changed.run();
				}
			};
		}
	}

	@Override
	public int getTanks() {
		return this.tanks.length;
	}

	@Override
	public FluidStack getFluidInTank(int tank) {
		return valid(tank) ? this.tanks[tank].getFluid() : FluidStack.EMPTY;
	}

	@Override
	public int getTankCapacity(int tank) {
		return valid(tank) ? this.tanks[tank].getCapacity() : 0;
	}

	@Override
	public boolean isFluidValid(int tank, FluidStack stack) {
		return valid(tank) && this.tanks[tank].isFluidValid(stack);
	}

	@Override
	public int fill(FluidStack resource, FluidAction action) {
		if (resource.isEmpty()) {
			return 0;
		}
		int remaining = resource.getAmount();
		int filled = 0;
		for (FluidTank tank : this.tanks) {
			if (!tank.isEmpty() && FluidStack.isSameFluidSameComponents(tank.getFluid(), resource)) {
				int moved = tank.fill(resource.copyWithAmount(remaining), action);
				remaining -= moved;
				filled += moved;
			}
		}
		for (FluidTank tank : this.tanks) {
			if (remaining <= 0) {
				break;
			}
			if (tank.isEmpty()) {
				int moved = tank.fill(resource.copyWithAmount(remaining), action);
				remaining -= moved;
				filled += moved;
			}
		}
		return filled;
	}

	@Override
	public FluidStack drain(FluidStack resource, FluidAction action) {
		if (resource.isEmpty()) {
			return FluidStack.EMPTY;
		}
		int remaining = resource.getAmount();
		int drained = 0;
		for (FluidTank tank : this.tanks) {
			if (remaining <= 0) {
				break;
			}
			if (!tank.isEmpty() && FluidStack.isSameFluidSameComponents(tank.getFluid(), resource)) {
				FluidStack moved = tank.drain(resource.copyWithAmount(remaining), action);
				remaining -= moved.getAmount();
				drained += moved.getAmount();
			}
		}
		return drained == 0 ? FluidStack.EMPTY : resource.copyWithAmount(drained);
	}

	@Override
	public FluidStack drain(int maxDrain, FluidAction action) {
		if (maxDrain <= 0) {
			return FluidStack.EMPTY;
		}
		for (FluidTank tank : this.tanks) {
			if (!tank.isEmpty()) {
				return drain(tank.getFluid().copyWithAmount(maxDrain), action);
			}
		}
		return FluidStack.EMPTY;
	}

	public long amountOf(FluidStack template) {
		long amount = 0;
		for (FluidTank tank : this.tanks) {
			if (!tank.isEmpty() && FluidStack.isSameFluidSameComponents(tank.getFluid(), template)) {
				amount += tank.getFluidAmount();
			}
		}
		return amount;
	}

	public boolean canFit(FluidStack template, long amount) {
		long space = 0;
		for (FluidTank tank : this.tanks) {
			if (tank.isEmpty() || FluidStack.isSameFluidSameComponents(tank.getFluid(), template)) {
				space += tank.getSpace();
			}
		}
		return space >= amount;
	}

	public long fillLong(FluidStack template, long amount, FluidAction action) {
		long remaining = amount;
		while (remaining > 0) {
			int request = (int) Math.min(Integer.MAX_VALUE, remaining);
			int moved = fill(template.copyWithAmount(request), action);
			remaining -= moved;
			if (moved < request) {
				break;
			}
		}
		return amount - remaining;
	}

	public long drainLong(FluidStack template, long amount, FluidAction action) {
		long remaining = amount;
		while (remaining > 0) {
			int request = (int) Math.min(Integer.MAX_VALUE, remaining);
			FluidStack moved = drain(template.copyWithAmount(request), action);
			remaining -= moved.getAmount();
			if (moved.getAmount() < request) {
				break;
			}
		}
		return amount - remaining;
	}

	public CompoundTag save(HolderLookup.Provider lookupProvider) {
		CompoundTag root = new CompoundTag();
		ListTag list = new ListTag();
		for (FluidTank tank : this.tanks) {
			list.add(tank.writeToNBT(lookupProvider, new CompoundTag()));
		}
		root.put("Tanks", list);
		return root;
	}

	public void load(HolderLookup.Provider lookupProvider, CompoundTag root) {
		ListTag list = root.getList("Tanks", Tag.TAG_COMPOUND);
		for (int index = 0; index < this.tanks.length; index++) {
			this.tanks[index].readFromNBT(lookupProvider, index < list.size() ? list.getCompound(index) : new CompoundTag());
		}
	}

	public void clear() {
		for (FluidTank tank : this.tanks) {
			tank.setFluid(FluidStack.EMPTY);
		}
	}

	private boolean valid(int tank) {
		return tank >= 0 && tank < this.tanks.length;
	}
}
