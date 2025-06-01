package net.mcreator.blackbox.procedures;

import net.neoforged.neoforge.event.tick.LevelTickEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.bus.api.Event;

import net.minecraft.world.level.LevelAccessor;

import net.mcreator.blackbox.network.BlackboxModVariables;

import javax.annotation.Nullable;

import java.util.Calendar;

@EventBusSubscriber
public class TimeCalculateProcedure {
	@SubscribeEvent
	public static void onWorldTick(LevelTickEvent.Post event) {
		execute(event, event.getLevel());
	}

	public static void execute(LevelAccessor world) {
		execute(null, world);
	}

	private static void execute(@Nullable Event event, LevelAccessor world) {
		if (BlackboxModVariables.MapVariables.get(world).global_x != Calendar.getInstance().get(Calendar.SECOND)) {
			BlackboxModVariables.MapVariables.get(world).TimeinSec = BlackboxModVariables.MapVariables.get(world).TimeinSec + 1;
			BlackboxModVariables.MapVariables.get(world).syncData(world);
			BlackboxModVariables.MapVariables.get(world).global_x = Calendar.getInstance().get(Calendar.SECOND);
			BlackboxModVariables.MapVariables.get(world).syncData(world);
			if (BlackboxModVariables.MapVariables.get(world).TimeinSec >= 1000000000) {
				BlackboxModVariables.MapVariables.get(world).TimeinSec = 0;
				BlackboxModVariables.MapVariables.get(world).syncData(world);
			}
		}
	}
}
