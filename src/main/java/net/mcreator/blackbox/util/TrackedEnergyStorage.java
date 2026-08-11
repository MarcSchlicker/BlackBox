package net.mcreator.blackbox.util;

import net.neoforged.neoforge.energy.EnergyStorage;

public final class TrackedEnergyStorage extends EnergyStorage {
	private final Runnable changed;

	public TrackedEnergyStorage(int capacity, Runnable changed) {
		super(capacity, capacity, capacity);
		this.changed = changed;
	}

	@Override
	public int receiveEnergy(int maxReceive, boolean simulate) {
		int moved = super.receiveEnergy(maxReceive, simulate);
		if (moved > 0 && !simulate) {
			this.changed.run();
		}
		return moved;
	}

	@Override
	public int extractEnergy(int maxExtract, boolean simulate) {
		int moved = super.extractEnergy(maxExtract, simulate);
		if (moved > 0 && !simulate) {
			this.changed.run();
		}
		return moved;
	}

	public void setEnergyStored(int amount) {
		int next = Math.max(0, Math.min(this.capacity, amount));
		if (this.energy != next) {
			this.energy = next;
			this.changed.run();
		}
	}
}
