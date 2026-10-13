package io.github.marcschlicker.blackbox.util;

import net.neoforged.neoforge.energy.IEnergyStorage;

/** Exposes only energy receive, extraction, or both as selected by a BlackBox side. */
public final class BlackboxSidedEnergyStorage implements IEnergyStorage {
	private final TrackedEnergyStorage input;
	private final TrackedEnergyStorage output;
	private final boolean allowReceive;
	private final boolean allowExtract;

	public BlackboxSidedEnergyStorage(TrackedEnergyStorage input, TrackedEnergyStorage output, boolean allowReceive, boolean allowExtract) {
		this.input = input;
		this.output = output;
		this.allowReceive = allowReceive;
		this.allowExtract = allowExtract;
	}

	@Override
	public int receiveEnergy(int maxReceive, boolean simulate) {
		return allowReceive ? input.receiveEnergy(maxReceive, simulate) : 0;
	}

	@Override
	public int extractEnergy(int maxExtract, boolean simulate) {
		return allowExtract ? output.extractEnergy(maxExtract, simulate) : 0;
	}

	@Override
	public int getEnergyStored() {
		if (allowReceive && allowExtract) {
			return input.getEnergyStored() + output.getEnergyStored();
		}
		return allowReceive ? input.getEnergyStored() : output.getEnergyStored();
	}

	@Override
	public int getMaxEnergyStored() {
		if (allowReceive && allowExtract) {
			return input.getMaxEnergyStored() + output.getMaxEnergyStored();
		}
		return allowReceive ? input.getMaxEnergyStored() : output.getMaxEnergyStored();
	}

	@Override
	public boolean canExtract() {
		return allowExtract;
	}

	@Override
	public boolean canReceive() {
		return allowReceive;
	}
}
