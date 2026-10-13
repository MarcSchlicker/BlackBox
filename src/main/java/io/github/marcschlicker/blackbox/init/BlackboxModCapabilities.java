package io.github.marcschlicker.blackbox.init;

import net.minecraft.core.Direction;

import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;

public final class BlackboxModCapabilities {
	private BlackboxModCapabilities() {
	}

	public static void register(RegisterCapabilitiesEvent event) {
		event.registerBlockEntity(Capabilities.FluidHandler.BLOCK, BlackboxModBlockEntities.DIMENSIONAL_WORKBENCH.get(),
				(workbench, side) -> side == Direction.DOWN ? workbench.resources().outputFluids() : workbench.resources().inputFluids());
		event.registerBlockEntity(Capabilities.EnergyStorage.BLOCK, BlackboxModBlockEntities.DIMENSIONAL_WORKBENCH.get(),
				(workbench, side) -> side == Direction.DOWN ? workbench.resources().outputEnergy() : workbench.resources().inputEnergy());
		event.registerBlockEntity(Capabilities.FluidHandler.BLOCK, BlackboxModBlockEntities.BLACKBOX_BLOCK.get(),
				(blackbox, side) -> blackbox.fluidCapability(side));
		event.registerBlockEntity(Capabilities.EnergyStorage.BLOCK, BlackboxModBlockEntities.BLACKBOX_BLOCK.get(),
				(blackbox, side) -> blackbox.energyCapability(side));
		event.registerBlockEntity(Capabilities.FluidHandler.BLOCK, BlackboxModBlockEntities.SPAWNER_BLACKBOX.get(),
				(blackbox, side) -> blackbox.fluidCapability(side));
		event.registerBlockEntity(Capabilities.EnergyStorage.BLOCK, BlackboxModBlockEntities.SPAWNER_BLACKBOX.get(),
				(blackbox, side) -> blackbox.energyCapability(side));
		event.registerBlockEntity(Capabilities.FluidHandler.BLOCK, BlackboxModBlockEntities.INPUTBLOCK.get(), (port, side) -> port.fluidStorage());
		event.registerBlockEntity(Capabilities.EnergyStorage.BLOCK, BlackboxModBlockEntities.INPUTBLOCK.get(), (port, side) -> port.energyStorage());
		event.registerBlockEntity(Capabilities.FluidHandler.BLOCK, BlackboxModBlockEntities.OUTPUT_BLOCK.get(), (port, side) -> port.fluidStorage());
		event.registerBlockEntity(Capabilities.EnergyStorage.BLOCK, BlackboxModBlockEntities.OUTPUT_BLOCK.get(), (port, side) -> port.energyStorage());
	}
}
