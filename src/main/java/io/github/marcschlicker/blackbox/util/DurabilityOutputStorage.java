package io.github.marcschlicker.blackbox.util;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

/** Persistent machine-local remainders for outputs that are measured as partial durability. */
public final class DurabilityOutputStorage {
	private final Runnable changed;
	private final List<FarmCoreData.DurabilityAmount> remainders = new ArrayList<>();
	private String coreId = "";

	public DurabilityOutputStorage(Runnable changed) {
		this.changed = changed;
	}

	public void resetForCore(String coreId) {
		String safeCoreId = coreId == null ? "" : coreId;
		if (!this.coreId.equals(safeCoreId)) {
			this.coreId = safeCoreId;
			this.remainders.clear();
			this.changed.run();
		}
	}

	public void clear() {
		if (!this.coreId.isEmpty() || !this.remainders.isEmpty()) {
			this.coreId = "";
			this.remainders.clear();
			this.changed.run();
		}
	}

	public void discardIf(Predicate<ItemStack> discard) {
		if (this.remainders.removeIf(entry -> discard.test(entry.stack()))) {
			this.changed.run();
		}
	}

	public List<FarmCoreData.StackAmount> previewCompleted(List<FarmCoreData.DurabilityAmount> produced) {
		List<FarmCoreData.StackAmount> result = new ArrayList<>();
		for (FarmCoreData.DurabilityAmount entry : produced) {
			long total = saturatedAdd(amountOf(entry.stack()), entry.durability());
			addItem(result, entry.stack(), total / maxDamage(entry.stack()));
		}
		return result;
	}

	public void accept(List<FarmCoreData.DurabilityAmount> produced) {
		boolean changedAny = false;
		for (FarmCoreData.DurabilityAmount entry : produced) {
			if (entry.durability() <= 0 || entry.stack().isEmpty()) {
				continue;
			}
			int index = indexOf(entry.stack());
			long total = saturatedAdd(index < 0 ? 0 : this.remainders.get(index).durability(), entry.durability());
			long remainder = total % maxDamage(entry.stack());
			if (index >= 0 && remainder == 0) {
				this.remainders.remove(index);
			} else if (index >= 0) {
				this.remainders.set(index, new FarmCoreData.DurabilityAmount(entry.stack(), remainder));
			} else if (remainder > 0) {
				this.remainders.add(new FarmCoreData.DurabilityAmount(entry.stack(), remainder));
			}
			changedAny = true;
		}
		if (changedAny) {
			this.changed.run();
		}
	}

	public CompoundTag save(HolderLookup.Provider lookupProvider) {
		CompoundTag root = new CompoundTag();
		root.putString("CoreId", this.coreId);
		ListTag entries = new ListTag();
		for (FarmCoreData.DurabilityAmount entry : this.remainders) {
			CompoundTag tag = new CompoundTag();
			tag.put("Stack", entry.stack().saveOptional(lookupProvider));
			tag.putLong("Durability", entry.durability());
			entries.add(tag);
		}
		root.put("Remainders", entries);
		return root;
	}

	public void load(CompoundTag root, HolderLookup.Provider lookupProvider) {
		this.coreId = root.getString("CoreId");
		this.remainders.clear();
		ListTag entries = root.getList("Remainders", Tag.TAG_COMPOUND);
		for (int index = 0; index < entries.size(); index++) {
			CompoundTag tag = entries.getCompound(index);
			ItemStack stack = ItemStack.parseOptional(lookupProvider, tag.getCompound("Stack"));
			long remainder = tag.getLong("Durability");
			if (!stack.isEmpty() && stack.getMaxDamage() > 0 && remainder > 0) {
				this.remainders.add(new FarmCoreData.DurabilityAmount(stack, remainder % maxDamage(stack)));
			}
		}
	}

	private long amountOf(ItemStack stack) {
		int index = indexOf(stack);
		return index < 0 ? 0 : this.remainders.get(index).durability();
	}

	private int indexOf(ItemStack stack) {
		for (int index = 0; index < this.remainders.size(); index++) {
			if (ItemStack.isSameItemSameComponents(this.remainders.get(index).stack(), stack)) {
				return index;
			}
		}
		return -1;
	}

	private static int maxDamage(ItemStack stack) {
		return Math.max(1, stack.getMaxDamage());
	}

	private static long saturatedAdd(long first, long second) {
		return Long.MAX_VALUE - first < second ? Long.MAX_VALUE : first + second;
	}

	private static void addItem(List<FarmCoreData.StackAmount> entries, ItemStack stack, long amount) {
		if (amount <= 0) {
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
}
