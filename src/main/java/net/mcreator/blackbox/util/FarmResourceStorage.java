package net.mcreator.blackbox.util;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;

public final class FarmResourceStorage {
	public static final int FLUID_TANKS = 4;
	public static final int FLUID_CAPACITY_PER_TANK = 16_000;
	public static final int ENERGY_CAPACITY = 10_000_000;

	private final MultiFluidTank inputFluids;
	private final MultiFluidTank outputFluids;
	private final TrackedEnergyStorage inputEnergy;
	private final TrackedEnergyStorage outputEnergy;

	public FarmResourceStorage(Runnable changed) {
		this.inputFluids = new MultiFluidTank(FLUID_TANKS, FLUID_CAPACITY_PER_TANK, changed);
		this.outputFluids = new MultiFluidTank(FLUID_TANKS, FLUID_CAPACITY_PER_TANK, changed);
		this.inputEnergy = new TrackedEnergyStorage(ENERGY_CAPACITY, changed);
		this.outputEnergy = new TrackedEnergyStorage(ENERGY_CAPACITY, changed);
	}

	public MultiFluidTank inputFluids() {
		return this.inputFluids;
	}

	public MultiFluidTank outputFluids() {
		return this.outputFluids;
	}

	public TrackedEnergyStorage inputEnergy() {
		return this.inputEnergy;
	}

	public TrackedEnergyStorage outputEnergy() {
		return this.outputEnergy;
	}

	public void save(CompoundTag tag, HolderLookup.Provider lookupProvider) {
		tag.put("InputFluids", this.inputFluids.save(lookupProvider));
		tag.put("OutputFluids", this.outputFluids.save(lookupProvider));
		tag.putInt("InputEnergy", this.inputEnergy.getEnergyStored());
		tag.putInt("OutputEnergy", this.outputEnergy.getEnergyStored());
	}

	public void load(CompoundTag tag, HolderLookup.Provider lookupProvider) {
		this.inputFluids.load(lookupProvider, tag.getCompound("InputFluids"));
		this.outputFluids.load(lookupProvider, tag.getCompound("OutputFluids"));
		this.inputEnergy.setEnergyStored(tag.getInt("InputEnergy"));
		this.outputEnergy.setEnergyStored(tag.getInt("OutputEnergy"));
	}
}
