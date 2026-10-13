package io.github.marcschlicker.blackbox;

import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;

import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import net.neoforged.neoforge.network.handling.IPayloadHandler;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.fml.util.thread.SidedThreadGroups;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.bus.api.IEventBus;

import net.minecraft.server.TickTask;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.FriendlyByteBuf;

import io.github.marcschlicker.blackbox.network.BlackboxModVariables;
import io.github.marcschlicker.blackbox.init.BlackboxModTabs;
import io.github.marcschlicker.blackbox.init.BlackboxModMenus;
import io.github.marcschlicker.blackbox.init.BlackboxModItems;
import io.github.marcschlicker.blackbox.init.BlackboxModBlocks;
import io.github.marcschlicker.blackbox.init.BlackboxModBlockEntities;
import io.github.marcschlicker.blackbox.init.BlackboxModChunkGenerators;
import io.github.marcschlicker.blackbox.init.BlackboxModCapabilities;
import io.github.marcschlicker.blackbox.init.BlackboxModConditions;
import io.github.marcschlicker.blackbox.init.BlackboxModVillagers;
import io.github.marcschlicker.blackbox.config.BlackboxConfig;
import io.github.marcschlicker.blackbox.util.CoreUpgradeAnvilHandler;
import io.github.marcschlicker.blackbox.util.FarmDimensionRuntime;

import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.Queue;
import java.util.PriorityQueue;
import java.util.Map;
import java.util.HashMap;
import java.util.Comparator;

import it.unimi.dsi.fastutil.ints.IntObjectPair;
import it.unimi.dsi.fastutil.ints.IntObjectImmutablePair;

@Mod("blackbox")
public class BlackboxMod {
	public static final Logger LOGGER = LogManager.getLogger(BlackboxMod.class);
	public static final String MODID = "blackbox";

	public BlackboxMod(IEventBus modEventBus, ModContainer modContainer) {
		// Start of user code block mod constructor
		// End of user code block mod constructor
		NeoForge.EVENT_BUS.register(this);
		NeoForge.EVENT_BUS.addListener(CoreUpgradeAnvilHandler::onAnvilUpdate);
		modEventBus.addListener(this::registerNetworking);
		modEventBus.addListener(BlackboxModCapabilities::register);
		modEventBus.addListener(FarmDimensionRuntime::registerTicketController);
		BlackboxModBlocks.REGISTRY.register(modEventBus);
		BlackboxModBlockEntities.REGISTRY.register(modEventBus);
		BlackboxModChunkGenerators.REGISTRY.register(modEventBus);
		BlackboxModItems.REGISTRY.register(modEventBus);
		BlackboxModTabs.REGISTRY.register(modEventBus);
		BlackboxModVariables.ATTACHMENT_TYPES.register(modEventBus);
		BlackboxModMenus.REGISTRY.register(modEventBus);
		BlackboxModConditions.REGISTRY.register(modEventBus);
		BlackboxModVillagers.REGISTRY.register(modEventBus);
		modContainer.registerConfig(ModConfig.Type.SERVER, BlackboxConfig.SPEC);
		// Start of user code block mod init
		// End of user code block mod init
	}

	// Start of user code block mod methods
	// End of user code block mod methods
	private static boolean networkingRegistered = false;
	private static final Map<CustomPacketPayload.Type<?>, NetworkMessage<?>> MESSAGES = new HashMap<>();

	private record NetworkMessage<T extends CustomPacketPayload>(StreamCodec<? extends FriendlyByteBuf, T> reader, IPayloadHandler<T> handler) {
	}

	public static <T extends CustomPacketPayload> void addNetworkMessage(CustomPacketPayload.Type<T> id, StreamCodec<? extends FriendlyByteBuf, T> reader, IPayloadHandler<T> handler) {
		if (networkingRegistered)
			throw new IllegalStateException("Cannot register new network messages after networking has been registered");
		MESSAGES.put(id, new NetworkMessage<>(reader, handler));
	}

	@SuppressWarnings({"rawtypes", "unchecked"})
	private void registerNetworking(final RegisterPayloadHandlersEvent event) {
		final PayloadRegistrar registrar = event.registrar(MODID);
		MESSAGES.forEach((id, networkMessage) -> registrar.playBidirectional(id, ((NetworkMessage) networkMessage).reader(), ((NetworkMessage) networkMessage).handler()));
		networkingRegistered = true;
	}

	private static final Queue<IntObjectPair<Runnable>> workToBeScheduled = new ConcurrentLinkedQueue<>();
	private static final PriorityQueue<TickTask> workQueue = new PriorityQueue<>(Comparator.comparingInt(TickTask::getTick));

	public static void queueServerWork(int delay, Runnable action) {
		if (Thread.currentThread().getThreadGroup() == SidedThreadGroups.SERVER)
			workToBeScheduled.add(new IntObjectImmutablePair<>(delay, action));
	}

	@SubscribeEvent
	public void tick(ServerTickEvent.Post event) {
		int currentTick = event.getServer().getTickCount();
		IntObjectPair<Runnable> work;
		while ((work = workToBeScheduled.poll()) != null) {
			workQueue.add(new TickTask(currentTick + work.leftInt(), work.right()));
		}
		while (!workQueue.isEmpty() && currentTick >= workQueue.peek().getTick()) {
			workQueue.poll().run();
		}
	}
}
