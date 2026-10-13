package io.github.marcschlicker.blackbox.block.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import io.github.marcschlicker.blackbox.init.BlackboxModBlockEntities;
import io.github.marcschlicker.blackbox.util.FarmActorRuntime;
import io.github.marcschlicker.blackbox.util.FarmDimensionRuntime;

import java.util.Optional;
import java.util.UUID;

/** Persists which farm cell owns an actor without ever storing a player entity. */
public class FarmActorBlockEntity extends BlockEntity {
	private UUID actorId = UUID.randomUUID();
	private UUID coreId;

	public FarmActorBlockEntity(BlockPos pos, BlockState state) {
		super(BlackboxModBlockEntities.FARM_ACTOR.get(), pos, state);
	}

	@Override
	protected void loadAdditional(CompoundTag tag, HolderLookup.Provider lookupProvider) {
		super.loadAdditional(tag, lookupProvider);
		try {
			this.actorId = UUID.fromString(tag.getString("ActorId"));
		} catch (IllegalArgumentException ignored) {
			this.actorId = UUID.randomUUID();
		}
		try {
			this.coreId = tag.getString("CoreId").isBlank() ? null : UUID.fromString(tag.getString("CoreId"));
		} catch (IllegalArgumentException ignored) {
			this.coreId = null;
		}
	}

	@Override
	protected void saveAdditional(CompoundTag tag, HolderLookup.Provider lookupProvider) {
		super.saveAdditional(tag, lookupProvider);
		tag.putString("ActorId", this.actorId.toString());
		tag.putString("CoreId", this.coreId == null ? "" : this.coreId.toString());
	}

	@Override
	public void onLoad() {
		super.onLoad();
		FarmActorRuntime.register(this);
	}

	@Override
	public void setRemoved() {
		FarmActorRuntime.unregister(this);
		super.setRemoved();
	}

	public UUID actorId() {
		return this.actorId;
	}

	public UUID coreId() {
		return this.coreId;
	}

	public void bindToFarm(ServerPlayer player) {
		Optional<io.github.marcschlicker.blackbox.util.FarmCell> cell = FarmDimensionRuntime.getAssignedCell(player);
		if (cell.isPresent() && cell.get().contains(this.worldPosition)) {
			bindToCore(cell.get().coreId());
		}
	}

	public void bindToCore(UUID newCoreId) {
		if (newCoreId != null && !newCoreId.equals(this.coreId)) {
			this.coreId = newCoreId;
			this.setChanged();
		}
	}
}
