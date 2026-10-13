package io.github.marcschlicker.blackbox.util;

import java.util.List;

/** A simulation machine that can also host a live farm measurement. */
public interface FarmMeasurementHost extends FarmSimulationMachine {
	int getCalculationPhase();

	int getCalculationTicksRemaining();

	int getCalculationTotalTicks();

	int getCalculationItemsPerMinute();

	void setCalculationState(int phase, int ticksRemaining, int totalTicks, int itemsPerMinute);

	MeasurementOutputBuffer measurementOutputBuffer();

	MeasurementOutputBuffer measurementProgress();

	void setMeasurementProgress(List<FarmCoreData.StackAmount> items, List<FarmCoreData.FluidAmount> fluids, long energy);

	void syncToClient();
}
