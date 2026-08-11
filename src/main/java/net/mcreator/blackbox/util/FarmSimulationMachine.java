package net.mcreator.blackbox.util;

import net.minecraft.world.Container;

public interface FarmSimulationMachine extends Container {
	int CORE_SLOT = 0;
	int INPUT_START = 1;
	int INPUT_END = 10;
	int OUTPUT_START = 10;
	int OUTPUT_END = 28;
	int UPGRADE_SLOT = 28;
	int SLOT_COUNT = 29;

	int getSimulationTicks();

	void setSimulationTicks(int ticks);

	String getActiveCoreId();

	void setActiveCoreId(String coreId);

	boolean isStableCycleFunded();

	void setStableCycleFunded(boolean funded);
}
