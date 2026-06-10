package net.mcreator.simpleenhancement;

import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;

import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import net.neoforged.neoforge.network.handling.IPayloadHandler;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.fml.util.thread.SidedThreadGroups;
import net.neoforged.fml.common.Mod;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.bus.api.IEventBus;

import net.minecraft.server.TickTask;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.FriendlyByteBuf;

import net.mcreator.simpleenhancement.init.SimpleEnhancementModTabs;
import net.mcreator.simpleenhancement.init.SimpleEnhancementModItems;
import net.mcreator.simpleenhancement.init.SimpleEnhancementModEntities;
import net.mcreator.simpleenhancement.giant.GiantEventHandler;
import net.mcreator.simpleenhancement.giant.GiantAttackHandler;

import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.Queue;
import java.util.PriorityQueue;
import java.util.Map;
import java.util.HashMap;
import java.util.Comparator;

import it.unimi.dsi.fastutil.ints.IntObjectPair;
import it.unimi.dsi.fastutil.ints.IntObjectImmutablePair;

@Mod("simple_enhancement")
public class SimpleEnhancementMod {
	public static final Logger LOGGER = LogManager.getLogger(SimpleEnhancementMod.class);
	public static final String MODID = "simple_enhancement";

	public SimpleEnhancementMod(IEventBus modEventBus) {
		// Start of user code block mod constructor
		NeoForge.EVENT_BUS.register(this);
		// 手动注册巨人相关事件处理器（确保模型缩放和碰撞箱生效）
		NeoForge.EVENT_BUS.register(GiantEventHandler.class);
		// 注册网络包
		modEventBus.addListener(GiantAttackHandler::registerPackets);
		// 手动注册客户端事件监听器（可选，如果自动注册不生效）
		NeoForge.EVENT_BUS.register(GiantAttackHandler.ClientEvents.class);
		SimpleEnhancementModifiers.register(modEventBus);
		ModVillagerTrades.register();
		// 注册升级宝珠自定义配方序列化器
		net.mcreator.simpleenhancement.recipe.OrbUpgradeRecipe.Serializer.register(modEventBus);
		// End of user code block mod constructor
		NeoForge.EVENT_BUS.register(this);
		modEventBus.addListener(this::registerNetworking);
		SimpleEnhancementModItems.REGISTRY.register(modEventBus);
		SimpleEnhancementModEntities.REGISTRY.register(modEventBus);
		SimpleEnhancementModTabs.REGISTRY.register(modEventBus);
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