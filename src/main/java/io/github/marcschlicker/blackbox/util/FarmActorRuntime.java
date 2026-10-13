package io.github.marcschlicker.blackbox.util;

import com.mojang.authlib.GameProfile;

import io.netty.channel.embedded.EmbeddedChannel;

import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;

import io.github.marcschlicker.blackbox.block.entity.FarmActorBlockEntity;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/** Keeps fake players out of the world unless their farm is being built or measured. */
@EventBusSubscriber
public final class FarmActorRuntime {
	private static final int CHECK_INTERVAL_TICKS = 10;
	private static final Set<FarmActorBlockEntity> ACTORS = Collections.newSetFromMap(new IdentityHashMap<>());
	private static final Map<FarmActorBlockEntity, FakePlayer> ACTIVE_PLAYERS = new IdentityHashMap<>();
	private static EmbeddedChannel fakePlayerChannel;

	private FarmActorRuntime() {
	}

	public static void register(FarmActorBlockEntity actor) {
		ACTORS.add(actor);
	}

	public static void unregister(FarmActorBlockEntity actor) {
		ACTORS.remove(actor);
		deactivate(actor);
	}

	/** Farm cells are the user-facing farm worlds, so each one intentionally has one actor anchor. */
	public static boolean hasActorInCell(ServerLevel level, FarmCell cell) {
		for (FarmActorBlockEntity actor : ACTORS) {
			if (!actor.isRemoved() && actor.getLevel() == level && cell.coreId().equals(actor.coreId())) {
				return true;
			}
		}
		return false;
	}

	/** Farm cells are the user-facing farm worlds, so each one intentionally has one actor anchor. */
	public static boolean hasActorInCell(ServerLevel level, FarmCell cell, BlockPos ignoredPos) {
		for (FarmActorBlockEntity actor : ACTORS) {
			if (!actor.isRemoved() && actor.getLevel() == level && !actor.getBlockPos().equals(ignoredPos) && cell.coreId().equals(actor.coreId())) {
				return true;
			}
		}
		return false;
	}

	@SubscribeEvent
	public static void onServerTick(ServerTickEvent.Post event) {
		if (event.getServer().getTickCount() % CHECK_INTERVAL_TICKS != 0) {
			return;
		}
		for (FarmActorBlockEntity actor : new ArrayList<>(ACTORS)) {
			if (actor.isRemoved() || !(actor.getLevel() instanceof ServerLevel level)) {
				unregister(actor);
				continue;
			}
			if (actor.coreId() == null) {
				FarmDimensionRuntime.registeredCellAt(level, actor.getBlockPos()).ifPresent(cell -> actor.bindToCore(cell.coreId()));
			}
			if (FarmDimensionRuntime.shouldActivateFarmActor(level, actor.coreId(), actor.getBlockPos())) {
				activate(level, actor);
			} else {
				deactivate(actor);
			}
		}
	}

	@SubscribeEvent
	public static void onServerStopped(ServerStoppedEvent event) {
		for (FarmActorBlockEntity actor : new ArrayList<>(ACTIVE_PLAYERS.keySet())) {
			deactivate(actor);
		}
		ACTORS.clear();
	}

	private static void activate(ServerLevel level, FarmActorBlockEntity actor) {
		FakePlayer existing = ACTIVE_PLAYERS.get(actor);
		if (existing != null && !existing.isRemoved()) {
			position(existing, actor.getBlockPos());
			return;
		}
		UUID profileId = UUID.nameUUIDFromBytes(("blackbox:farm_actor:" + actor.actorId()).getBytes(StandardCharsets.UTF_8));
		FakePlayer player = new FakePlayer(level, new GameProfile(profileId, "Farm Actor"));
		player.noPhysics = true;
		player.setNoGravity(true);
		player.setInvulnerable(true);
		player.setSilent(true);
		player.setCustomName(Component.translatable("entity.blackbox.farm_actor"));
		player.setCustomNameVisible(true);
		position(player, actor.getBlockPos());
		ensureFakePlayerConnection(player);
		level.addNewPlayer(player);
		ACTIVE_PLAYERS.put(actor, player);
	}

	private static void position(FakePlayer player, BlockPos pos) {
		player.moveTo(pos.getX() + 0.5D, pos.getY() + 1.0D, pos.getZ() + 0.5D, 180.0F, 0.0F);
	}

	/* NeoForge's stock FakePlayer uses a connection without a Netty channel. Large packs often
	 * listen for every ServerPlayer join and inspect that channel, so provide one inert channel. */
	private static void ensureFakePlayerConnection(FakePlayer player) {
		if (player.connection.getConnection().channel() == null) {
			fakePlayerChannel = new EmbeddedChannel(player.connection.getConnection());
		}
	}

	private static void deactivate(FarmActorBlockEntity actor) {
		FakePlayer player = ACTIVE_PLAYERS.remove(actor);
		if (player != null && !player.isRemoved()) {
			player.remove(Entity.RemovalReason.DISCARDED);
		}
	}
}
