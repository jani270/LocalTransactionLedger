package moe.nea.ledger

import io.github.notenoughupdates.moulconfig.managed.ManagedConfig
import moe.nea.ledger.config.LedgerConfig
import moe.nea.ledger.database.Database
import moe.nea.ledger.events.ChatReceived
import moe.nea.ledger.events.LateWorldLoadEvent
import moe.nea.ledger.modules.AuctionHouseDetection
import moe.nea.ledger.modules.BankDetection
import moe.nea.ledger.modules.BazaarDetection
import moe.nea.ledger.modules.BazaarOrderDetection
import moe.nea.ledger.modules.BitsDetection
import moe.nea.ledger.modules.BitsShopDetection
import moe.nea.ledger.modules.DungeonChestDetection
import moe.nea.ledger.modules.MinionDetection
import moe.nea.ledger.modules.NpcDetection
import moe.nea.ledger.utils.DI
import net.minecraft.client.Minecraft
import net.minecraft.command.ICommand
import net.minecraftforge.client.ClientCommandHandler
import net.minecraftforge.client.event.ClientChatReceivedEvent
import net.minecraftforge.common.MinecraftForge
import net.minecraftforge.event.entity.EntityJoinWorldEvent
import net.minecraftforge.fml.common.Mod
import net.minecraftforge.fml.common.event.FMLInitializationEvent
import net.minecraftforge.fml.common.eventhandler.EventPriority
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import net.minecraftforge.fml.common.gameevent.TickEvent
import net.minecraftforge.fml.common.gameevent.TickEvent.ClientTickEvent
import org.apache.logging.log4j.LogManager
import java.io.File
import java.util.concurrent.ConcurrentLinkedQueue

@Mod(modid = "ledger", useMetadata = true)
class Ledger {
	/*
	You have withdrawn 1M coins! You now have 518M coins in your account!
	You have deposited 519M coins! You now have 519M coins in your account!

	// ORDERS:

	[Bazaar] Buy Order Setup! 160x Wheat for 720.0 coins.
	[Bazaar] Claimed 160x Wheat worth 720.0 coins bought for 4.5 each!

	[Bazaar] Sell Offer Setup! 160x Wheat for 933.4 coins.
	[Bazaar] Claimed 34,236,799 coins from selling 176x Hyper Catalyst at 196,741 each!

	// INSTABUY:

	[Bazaar] Bought 64x Wheat for 377.6 coins!
	[Bazaar] Sold 64x Wheat for 268.8 coins!

	// AUCTION HOUSE:

	You collected 8,712,000 coins from selling Ultimate Carrot Candy Upgrade to [VIP] kodokush in an auction!
	You purchased 2x Walnut for 69 coins!
	You purchased ◆ Ice Rune I for 4,000 coins!

	// NPC

	// You bought Cactus x32 for 465.6 Coins!
	// You sold Cactus x1 for 3 Coins!
	// You bought back Potato x3 for 9 Coins!

	TODO: TRADING, FORGE, COOKIE_EATEN
	*/
	companion object {
		val dataFolder = File("money-ledger").apply { mkdirs() }
		val logger = LogManager.getLogger("MoneyLedger")
		val managedConfig = ManagedConfig.create(File("config/money-ledger/config.json"), LedgerConfig::class.java) {
			checkExpose = false
		}
		private val tickQueue = ConcurrentLinkedQueue<Runnable>()
		fun runLater(runnable: Runnable) {
			tickQueue.add(runnable)
		}
	}

	@Mod.EventHandler
	fun init(event: FMLInitializationEvent) {
		logger.info("Initializing ledger")
		Database.init()

		val di = DI()
		di.registerSingleton(this)
		di.registerInjectableClasses(
			LedgerLogger::class.java,
			ItemIdProvider::class.java,
			BankDetection::class.java,
			BazaarDetection::class.java,
			DungeonChestDetection::class.java,
			BazaarOrderDetection::class.java,
			AuctionHouseDetection::class.java,
			BitsDetection::class.java,
			BitsShopDetection::class.java,
			MinionDetection::class.java,
			NpcDetection::class.java,
			LogChatCommand::class.java,
			ConfigCommand::class.java,
		)
		di.instantiateAll()
		di.getAllInstances().forEach(MinecraftForge.EVENT_BUS::register)
		di.getAllInstances().filterIsInstance<ICommand>()
			.forEach { ClientCommandHandler.instance.registerCommand(it) }
	}

	var lastJoin = -1L

	@SubscribeEvent
	fun worldSwitchEvent(event: EntityJoinWorldEvent) {
		if (event.entity == Minecraft.getMinecraft().thePlayer) {
			lastJoin = System.currentTimeMillis()
		}
	}

	@SubscribeEvent
	fun tickEvent(event: ClientTickEvent) {
		if (event.phase == TickEvent.Phase.END
			&& lastJoin > 0
			&& System.currentTimeMillis() - lastJoin > 10_000
			&& Minecraft.getMinecraft().thePlayer != null
		) {
			lastJoin = -1
			MinecraftForge.EVENT_BUS.post(LateWorldLoadEvent())
		}
		while (true) {
			val queued = tickQueue.poll() ?: break
			queued.run()
		}
	}

	@SubscribeEvent(receiveCanceled = true, priority = EventPriority.HIGHEST)
	fun onChat(event: ClientChatReceivedEvent) {
		if (event.type != 2.toByte())
			MinecraftForge.EVENT_BUS.post(ChatReceived(event))
	}
}
