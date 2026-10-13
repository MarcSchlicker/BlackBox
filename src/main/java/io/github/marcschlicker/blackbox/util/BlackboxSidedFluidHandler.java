package io.github.marcschlicker.blackbox.util;

import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;

/** Exposes only fluid insertion, extraction, or both as selected by a BlackBox side. */
public final class BlackboxSidedFluidHandler implements IFluidHandler {
	private final MultiFluidTank input;
	private final MultiFluidTank output;
	private final boolean allowFill;
	private final boolean allowDrain;

	public BlackboxSidedFluidHandler(MultiFluidTank input, MultiFluidTank output, boolean allowFill, boolean allowDrain) {
		this.input = input;
		this.output = output;
		this.allowFill = allowFill;
		this.allowDrain = allowDrain;
	}

	@Override
	public int getTanks() {
		if (allowFill && allowDrain) {
			return input.getTanks() + output.getTanks();
		}
		return allowFill ? input.getTanks() : output.getTanks();
	}

	@Override
	public FluidStack getFluidInTank(int tank) {
		if (allowFill && allowDrain) {
			return tank < input.getTanks() ? input.getFluidInTank(tank) : output.getFluidInTank(tank - input.getTanks());
		}
		return allowFill ? input.getFluidInTank(tank) : output.getFluidInTank(tank);
	}

	@Override
	public int getTankCapacity(int tank) {
		if (allowFill && allowDrain) {
			return tank < input.getTanks() ? input.getTankCapacity(tank) : output.getTankCapacity(tank - input.getTanks());
		}
		return allowFill ? input.getTankCapacity(tank) : output.getTankCapacity(tank);
	}

	@Override
	public boolean isFluidValid(int tank, FluidStack stack) {
		if (!allowFill) {
			return false;
		}
		return input.isFluidValid(Math.min(tank, input.getTanks() - 1), stack);
	}

	@Override
	public int fill(FluidStack resource, FluidAction action) {
		return allowFill ? input.fill(resource, action) : 0;
	}

	@Override
	public FluidStack drain(FluidStack resource, FluidAction action) {
		return allowDrain ? output.drain(resource, action) : FluidStack.EMPTY;
	}

	@Override
	public FluidStack drain(int maxDrain, FluidAction action) {
		return allowDrain ? output.drain(maxDrain, action) : FluidStack.EMPTY;
	}
}
