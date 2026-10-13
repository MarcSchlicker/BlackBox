package io.github.marcschlicker.blackbox.util;

import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

/** Converts damaged equipment measured in a farm into pristine item templates and durability units. */
public final class DurableOutputNormalizer {
	private static final int EQUIPMENT_STACK_SIZE = 16;

	private DurableOutputNormalizer() {
	}

	public static NormalizedOutputs normalize(List<FarmCoreData.StackAmount> rawOutputs) {
		List<FarmCoreData.StackAmount> items = new ArrayList<>();
		List<FarmCoreData.DurabilityAmount> durability = new ArrayList<>();
		for (FarmCoreData.StackAmount output : rawOutputs) {
			if (usesDurability(output.stack())) {
				long remaining = Math.max(0, output.stack().getMaxDamage() - output.stack().getDamageValue());
				addDurability(durability, fullStack(output.stack()), saturatedMultiply(remaining, output.amount()));
			} else {
				addItem(items, stackableEquipmentTemplate(output.stack()), output.amount());
			}
		}
		return new NormalizedOutputs(items, durability);
	}

	public static List<FarmCoreData.ProductionEvent> normalizeTimeline(List<FarmCoreData.ProductionEvent> rawTimeline) {
		List<FarmCoreData.ProductionEvent> result = new ArrayList<>();
		for (FarmCoreData.ProductionEvent event : rawTimeline) {
			List<FarmCoreData.StackAmount> items = new ArrayList<>();
			for (FarmCoreData.StackAmount output : event.outputs()) {
				if (!usesDurability(output.stack())) {
					addItem(items, stackableEquipmentTemplate(output.stack()), output.amount());
				}
			}
			if (!items.isEmpty()) {
				result.add(new FarmCoreData.ProductionEvent(event.tick(), items));
			}
		}
		return result;
	}

	public static boolean usesDurability(ItemStack stack) {
		return !stack.isEmpty() && stack.isDamageableItem() && stack.getMaxDamage() > 0;
	}

	public static ItemStack fullOutputTemplate(ItemStack stack) {
		return usesDurability(stack) ? fullStack(stack) : stackableEquipmentTemplate(stack);
	}

	public static long durabilityUnits(ItemStack stack, long count) {
		if (!usesDurability(stack) || count <= 0) {
			return 0;
		}
		long remaining = Math.max(0, stack.getMaxDamage() - stack.getDamageValue());
		return saturatedMultiply(remaining, count);
	}

	private static ItemStack fullStack(ItemStack stack) {
		ItemStack result = stack.copyWithCount(1);
		result.remove(DataComponents.DAMAGE);
		result.remove(DataComponents.MAX_STACK_SIZE);
		result.setDamageValue(0);
		return stackableEquipmentTemplate(result);
	}

	private static ItemStack stackableEquipmentTemplate(ItemStack stack) {
		ItemStack result = stack.copyWithCount(1);
		if (isStackableDurableOutput(result) && !result.isDamaged()) {
			result.set(DataComponents.MAX_STACK_SIZE, EQUIPMENT_STACK_SIZE);
		}
		return result;
	}

	private static boolean isStackableDurableOutput(ItemStack stack) {
		return stack.getMaxDamage() > 0;
	}

	private static void addItem(List<FarmCoreData.StackAmount> entries, ItemStack stack, long amount) {
		if (amount <= 0 || stack.isEmpty()) {
			return;
		}
		for (int index = 0; index < entries.size(); index++) {
			FarmCoreData.StackAmount existing = entries.get(index);
			if (ItemStack.isSameItemSameComponents(existing.stack(), stack)) {
				entries.set(index, new FarmCoreData.StackAmount(existing.stack(), saturatedAdd(existing.amount(), amount)));
				return;
			}
		}
		entries.add(new FarmCoreData.StackAmount(stack, amount));
	}

	private static void addDurability(List<FarmCoreData.DurabilityAmount> entries, ItemStack stack, long amount) {
		if (amount <= 0 || stack.isEmpty()) {
			return;
		}
		for (int index = 0; index < entries.size(); index++) {
			FarmCoreData.DurabilityAmount existing = entries.get(index);
			if (ItemStack.isSameItemSameComponents(existing.stack(), stack)) {
				entries.set(index, new FarmCoreData.DurabilityAmount(existing.stack(), saturatedAdd(existing.durability(), amount)));
				return;
			}
		}
		entries.add(new FarmCoreData.DurabilityAmount(stack, amount));
	}

	private static long saturatedAdd(long first, long second) {
		return Long.MAX_VALUE - first < second ? Long.MAX_VALUE : first + second;
	}

	private static long saturatedMultiply(long first, long second) {
		if (first <= 0 || second <= 0) {
			return 0;
		}
		return Long.MAX_VALUE / first < second ? Long.MAX_VALUE : first * second;
	}

	public record NormalizedOutputs(List<FarmCoreData.StackAmount> items, List<FarmCoreData.DurabilityAmount> durability) {
		public NormalizedOutputs {
			items = List.copyOf(items);
			durability = List.copyOf(durability);
		}
	}
}
