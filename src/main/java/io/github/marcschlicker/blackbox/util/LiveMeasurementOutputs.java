package io.github.marcschlicker.blackbox.util;

import net.neoforged.neoforge.fluids.FluidStack;

import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

/** Canonical, live output view used by the filter while a farm is still being measured. */
public final class LiveMeasurementOutputs {
	private LiveMeasurementOutputs() {
	}

	public static List<FarmCoreData.StackAmount> items(FarmMeasurementHost host) {
		if (host == null) {
			return List.of();
		}
		List<FarmCoreData.StackAmount> result = new ArrayList<>();
		for (FarmCoreData.StackAmount entry : host.measurementProgress().items()) {
			ItemStack template = DurableOutputNormalizer.usesDurability(entry.stack())
					? DurableOutputNormalizer.fullOutputTemplate(entry.stack()) : entry.stack();
			addItem(result, template, entry.amount());
		}
		return List.copyOf(result);
	}

	public static List<FarmCoreData.FluidAmount> fluids(FarmMeasurementHost host) {
		return host == null ? List.of() : host.measurementProgress().fluids();
	}

	public static long energy(FarmMeasurementHost host) {
		return host == null ? 0 : host.measurementProgress().energy();
	}

	private static void addItem(List<FarmCoreData.StackAmount> entries, ItemStack stack, long amount) {
		if (stack.isEmpty() || amount <= 0) {
			return;
		}
		for (int index = 0; index < entries.size(); index++) {
			FarmCoreData.StackAmount existing = entries.get(index);
			if (ItemStack.isSameItemSameComponents(existing.stack(), stack)) {
				long total = Long.MAX_VALUE - existing.amount() < amount ? Long.MAX_VALUE : existing.amount() + amount;
				entries.set(index, new FarmCoreData.StackAmount(existing.stack(), total));
				return;
			}
		}
		entries.add(new FarmCoreData.StackAmount(stack, amount));
	}
}
