package net.mcreator.blackbox.command;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.context.CommandContext;

import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;

import net.mcreator.blackbox.BlackboxMod;
import net.mcreator.blackbox.config.BlackboxConfig;

@EventBusSubscriber(modid = BlackboxMod.MODID)
public final class BlackboxCommands {
	private BlackboxCommands() {
	}

	@SubscribeEvent
	public static void register(RegisterCommandsEvent event) {
		event.getDispatcher().register(Commands.literal("blackbox")
				.requires(source -> source.hasPermission(2))
				.then(Commands.literal("measurement")
						.executes(BlackboxCommands::showMeasurementDuration)
						.then(Commands.literal("get").executes(BlackboxCommands::showMeasurementDuration))
						.then(Commands.literal("set")
								.then(Commands.argument("seconds", IntegerArgumentType.integer(10, 3600))
										.executes(BlackboxCommands::setMeasurementDuration)))));
	}

	private static int showMeasurementDuration(CommandContext<CommandSourceStack> context) {
		int seconds = BlackboxConfig.MEASUREMENT_SECONDS.get();
		context.getSource().sendSuccess(() -> Component.translatable("command.blackbox.measurement.current", seconds), false);
		return seconds;
	}

	private static int setMeasurementDuration(CommandContext<CommandSourceStack> context) {
		int seconds = IntegerArgumentType.getInteger(context, "seconds");
		BlackboxConfig.setMeasurementSeconds(seconds);
		context.getSource().sendSuccess(() -> Component.translatable("command.blackbox.measurement.updated", seconds), true);
		return seconds;
	}
}
