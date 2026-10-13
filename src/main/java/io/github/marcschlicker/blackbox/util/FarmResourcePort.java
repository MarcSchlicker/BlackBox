package io.github.marcschlicker.blackbox.util;

public interface FarmResourcePort {
	MultiFluidTank fluidStorage();

	TrackedEnergyStorage energyStorage();
}
