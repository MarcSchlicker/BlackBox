package net.mcreator.blackbox.util;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;

import java.util.LinkedHashMap;
import java.util.Map;

public final class MobInputStorage {
	private static final long MAX_PER_TYPE = 1024;
	private final Map<ResourceLocation, Long> amounts = new LinkedHashMap<>();
	private final Runnable onChanged;

	public MobInputStorage(Runnable onChanged) {
		this.onChanged = onChanged;
	}

	public long amountOf(ResourceLocation entityType) {
		return this.amounts.getOrDefault(entityType, 0L);
	}

	public long add(ResourceLocation entityType, long amount) {
		if (entityType == null || amount <= 0) {
			return 0;
		}
		long stored = amountOf(entityType);
		long accepted = Math.min(amount, MAX_PER_TYPE - stored);
		if (accepted > 0) {
			this.amounts.put(entityType, stored + accepted);
			this.onChanged.run();
		}
		return accepted;
	}

	public boolean consume(ResourceLocation entityType, long amount) {
		long stored = amountOf(entityType);
		if (amount <= 0 || stored < amount) {
			return amount <= 0;
		}
		long remaining = stored - amount;
		if (remaining == 0) {
			this.amounts.remove(entityType);
		} else {
			this.amounts.put(entityType, remaining);
		}
		this.onChanged.run();
		return true;
	}

	public ListTag save() {
		ListTag list = new ListTag();
		this.amounts.forEach((type, amount) -> {
			if (amount > 0) {
				CompoundTag entry = new CompoundTag();
				entry.putString("Type", type.toString());
				entry.putLong("Amount", amount);
				list.add(entry);
			}
		});
		return list;
	}

	public void load(ListTag list) {
		this.amounts.clear();
		for (int index = 0; index < list.size(); index++) {
			CompoundTag entry = list.getCompound(index);
			ResourceLocation type = ResourceLocation.tryParse(entry.getString("Type"));
			long amount = Math.min(MAX_PER_TYPE, Math.max(0, entry.getLong("Amount")));
			if (type != null && amount > 0) {
				this.amounts.put(type, amount);
			}
		}
	}
}
