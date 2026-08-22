package net.mcreator.blackbox.util;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.vehicle.Boat;
import net.minecraft.world.entity.vehicle.AbstractMinecart;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.portal.DimensionTransition;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import net.mcreator.blackbox.init.BlackboxModItems;

import java.util.List;

public final class FarmMobTransport {
	private static final String COOLDOWN_TAG = "BlackboxMobTransportTick";

	private FarmMobTransport() {
	}

	public static void checkWorkbench(ServerLevel level, BlockPos pos) {
		if (!(level.getBlockEntity(pos) instanceof FarmSimulationMachine machine)) {
			return;
		}
		ItemStack core = machine.getItem(FarmSimulationMachine.CORE_SLOT);
		if (!core.is(BlackboxModItems.DIMENSION_CORE.get()) || FarmDimensionRuntime.isSimulationPaused(level.getServer(), core)) {
			return;
		}
		for (Entity vehicle : nearbyMobVehicles(level, pos)) {
			if (FarmCoreData.isProgrammed(core)) {
				captureRequiredPassengers(level, pos, machine, core, vehicle);
			} else {
				transportIntoFarm(level, pos, machine, core, vehicle);
			}
		}
	}

	public static void checkBlackbox(ServerLevel level, BlockPos pos) {
		if (!(level.getBlockEntity(pos) instanceof FarmSimulationMachine machine)) {
			return;
		}
		ItemStack core = machine.getItem(FarmSimulationMachine.CORE_SLOT);
		if (!core.is(BlackboxModItems.DIMENSION_CORE.get()) || !FarmCoreData.isProgrammed(core) || FarmDimensionRuntime.isSimulationPaused(level.getServer(), core)) {
			return;
		}
		for (Entity vehicle : nearbyMobVehicles(level, pos)) {
			captureRequiredPassengers(level, pos, machine, core, vehicle);
		}
	}

	private static List<Entity> nearbyMobVehicles(ServerLevel level, BlockPos pos) {
		return level.getEntities((Entity) null, new AABB(pos).inflate(1.25D), entity ->
				(entity instanceof Boat || entity instanceof AbstractMinecart)
						&& entity.getPassengers().stream().anyMatch(passenger -> passenger instanceof Mob)
						&& entity.getPersistentData().getLong(COOLDOWN_TAG) < level.getGameTime());
	}

	private static void captureRequiredPassengers(ServerLevel level, BlockPos pos, FarmSimulationMachine machine, ItemStack core, Entity vehicle) {
		FarmCoreData.Recipe recipe = FarmCoreData.read(core, level.registryAccess());
		for (Entity passenger : List.copyOf(vehicle.getPassengers())) {
			if (!(passenger instanceof Mob)) {
				continue;
			}
			var type = BuiltInRegistries.ENTITY_TYPE.getKey(passenger.getType());
			long required = recipe.entityInputs().stream()
					.filter(entry -> entry.entityType().equals(type))
					.mapToLong(FarmCoreData.EntityAmount::amount)
					.sum();
			if (required <= machine.mobInputs().amountOf(type)) {
				continue;
			}
			passenger.stopRiding();
			passenger.discard();
			machine.mobInputs().add(type, 1);
			vehicle.getPersistentData().putLong(COOLDOWN_TAG, level.getGameTime() + 20);
			Component message = Component.translatable("message.blackbox.mob_input.accepted", passenger.getType().getDescription(),
					machine.mobInputs().amountOf(type), required).withStyle(ChatFormatting.GREEN);
			for (Player player : level.players()) {
				if (player.blockPosition().closerThan(pos, 8.0D)) {
					player.sendSystemMessage(message);
				}
			}
		}
	}

	private static void transportIntoFarm(ServerLevel source, BlockPos workbenchPos, FarmSimulationMachine machine, ItemStack core, Entity vehicle) {
		ServerLevel destination = source.getServer().getLevel(FarmCoreData.getEnvironment(core).dimension());
		if (destination == null) {
			return;
		}
		FarmCell cell = FarmCell.fromCoreId(FarmCoreData.ensureCoreId(core), FarmCoreData.getCellSizeChunks(core));
		machine.setItem(FarmSimulationMachine.CORE_SLOT, core);
		List<Entity> passengers = List.copyOf(vehicle.getPassengers());
		for (Entity passenger : passengers) {
			if (passenger instanceof ServerPlayer player) {
				FarmCell assigned = FarmDimensionRuntime.enterFarmDimension(player, destination, workbenchPos, source.dimension());
				if (assigned == null || !assigned.coreId().equals(cell.coreId())) {
					return;
				}
			}
		}
		FarmDimensionRuntime.prepareCell(destination, cell);
		passengers.forEach(passenger -> {
			passenger.stopRiding();
			FarmMobAccounting.markPlayerTransported(passenger);
		});
		Vec3 target = Vec3.atBottomCenterOf(cell.spawnPos().offset(0, 1, 4));
		DimensionTransition transition = new DimensionTransition(destination, target, Vec3.ZERO, vehicle.getYRot(), vehicle.getXRot(), DimensionTransition.DO_NOTHING);
		vehicle.getPersistentData().putLong(COOLDOWN_TAG, source.getGameTime() + 40);
		Entity movedVehicle = vehicle.changeDimension(transition);
		if (movedVehicle == null) {
			return;
		}
		for (Entity passenger : passengers) {
			Entity movedPassenger = passenger.changeDimension(new DimensionTransition(destination, target, Vec3.ZERO,
					passenger.getYRot(), passenger.getXRot(), DimensionTransition.DO_NOTHING));
			if (movedPassenger != null) {
				movedPassenger.startRiding(movedVehicle, true);
			}
		}
		for (Player player : source.players()) {
			if (player.blockPosition().closerThan(workbenchPos, 8.0D)) {
				player.sendSystemMessage(Component.translatable("message.blackbox.mob_transport.sent").withStyle(ChatFormatting.AQUA));
			}
		}
	}
}
