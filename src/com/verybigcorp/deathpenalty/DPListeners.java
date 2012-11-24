package com.verybigcorp.deathpenalty;

import java.sql.SQLException;
import java.util.Timer;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityTargetEvent;
import org.bukkit.event.entity.FoodLevelChangeEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.inventory.CraftItemEvent;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerBucketEvent;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerExpChangeEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerLoginEvent;
import org.bukkit.event.player.PlayerLoginEvent.Result;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerPickupItemEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.event.player.PlayerTeleportEvent.TeleportCause;
import org.bukkit.util.Vector;

import com.verybigcorp.deathpenalty.timing.DelayedHide;

public class DPListeners implements Listener {
		DeathPenalty plugin;
		public DPListeners(DeathPenalty plugin){
			this.plugin = plugin;
		}

		@EventHandler
		public void onPlayerJoin(final PlayerJoinEvent e){
				try {
					plugin.db.addPlayer(e.getPlayer().getName());
					if(plugin.db.isGhost(e.getPlayer())){
						e.getPlayer().sendMessage("You are a ghost.");
						if(plugin.db.getTimeLeft(e.getPlayer().getName()) <= 0)
							e.getPlayer().sendMessage(ChatColor.GRAY + "Your time is up. Move to be resurrected. " + plugin.resAppend(e.getPlayer().getName()));
						if(plugin.getConfig().getBoolean("ghostsFly"))
							e.getPlayer().setGameMode(GameMode.CREATIVE);
					}
					new Timer().schedule(new DelayedHide(plugin), 2000);
				} catch (SQLException e1) {

				}
		}

		@EventHandler
		public void onPlayerLogin(PlayerLoginEvent e){
			try {
				if(plugin.db.isBanned(e.getPlayer().getName())){
					e.disallow(Result.KICK_OTHER, "You are still banned for " + plugin.formatSeconds(plugin.db.banTimeLeft(e.getPlayer().getName())));
				}
			} catch (SQLException e1) {

			}
		}

		@EventHandler
		public void onPlayerDeath(PlayerDeathEvent e){
			final PlayerDeathEvent e1 = e;
			new Thread(new Runnable(){

				@Override
				public void run() {
					if(plugin.db.isGhost(e1.getEntity()))
						return;
					if(plugin.hasPermission(e1.getEntity(), "deathpenalty.ignore", false))
						return;
		            try {

						if(plugin.db.decrementLives(e1.getEntity().getName()) <= 0){
							plugin.db.addGhost(e1.getEntity().getName());
							plugin.ghosts.hideGhost(e1.getEntity().getName());
				            e1.getEntity().sendMessage(ChatColor.GRAY + "You have become a ghost of your former self. To see how much time you have left, type in /timeleft");
				            if(plugin.getConfig().getBoolean("banOnGhostLifeDepletion"))
				            	plugin.db.increaseGhostTimes(e1.getEntity().getName());
						} else {
							int lives = plugin.db.nLives(e1.getEntity().getName());
							String life = " lives";
							if(lives == 1)
								life = " life";
							e1.getEntity().sendMessage(ChatColor.GRAY + "You have "+ lives + life + " left.");
						}


						if(plugin.db.getGhostTimesLeft(e1.getEntity().getName()) > plugin.getConfig().getInt("maxGhostTimes") && plugin.getConfig().getBoolean("banOnGhostLifeDepletion")){
							plugin.ghosts.revealGhost(e1.getEntity().getName());
							plugin.db.ban(e1.getEntity().getName());
							e1.getEntity().kickPlayer("You have died. You may rejoin in " + plugin.formatSeconds(plugin.db.banTimeLeft(e1.getEntity().getName())));
							plugin.db.resetPlayer(e1.getEntity().getName());
						}
					} catch (SQLException e1) {
						plugin.log("sql error!" + e1.getMessage());
					}

				}

			}).start();

		}

		@EventHandler
		public void onEntityTarget(EntityTargetEvent e){
			if(e.getTarget() instanceof Player){
				if(plugin.db.isGhost((Player)e.getTarget()))
					e.setTarget(null);
			}
		}

		@EventHandler(priority = EventPriority.HIGHEST)
		public void onPlayerRespawn(final PlayerRespawnEvent e){
			if(plugin.db.isGhost(e.getPlayer()) && !plugin.isGhostSpawnLocationNotSet){
				e.setRespawnLocation(((Vector) plugin.spawn.get("ghostSpawn")).toLocation(plugin.getServer().getWorld(plugin.spawn.getString("world"))));
				if(plugin.getConfig().getBoolean("ghostsFly"))
					e.getPlayer().setGameMode(GameMode.CREATIVE);
			}
			if(plugin.db.getGhostTimesLeft(e.getPlayer().getName()) > plugin.getConfig().getInt("maxGhostTimes") || !plugin.getConfig().getBoolean("banOnGhostLifeDepletion"))
				plugin.db.resetLives(e.getPlayer().getName());
		}


		@EventHandler
		public void onPlayerExpChange(PlayerExpChangeEvent e){
			if(plugin.db.isGhost(e.getPlayer())){
				e.setAmount(0);
			}
		}

		@EventHandler
		public void onPlayerDropItem(PlayerDropItemEvent e) {
			if(plugin.db.isGhost(e.getPlayer())){
				e.setCancelled(true);
			}
		}

		@EventHandler
		public void onBlockPlace(BlockPlaceEvent e){
			if(plugin.db.isGhost(e.getPlayer())){
				e.setCancelled(true);
			}
		}

		@EventHandler
		public void onFoodLevelChange(FoodLevelChangeEvent e){
			if(plugin.db.isGhost((Player)e.getEntity())){
				e.setCancelled(true);
			}
		}

		@EventHandler
		public void onBlockBreak(BlockBreakEvent e){
			if(plugin.db.isGhost(e.getPlayer())){
				e.setCancelled(true);
			}
		}

		@EventHandler
		public void onPlayerInteract(final PlayerInteractEvent e){
				try {
					if(plugin.db.isGhost(e.getPlayer())){
						if(e.getAction() == Action.RIGHT_CLICK_BLOCK){
							if(e.getClickedBlock().getType().equals(Material.CAKE_BLOCK) && !plugin.db.hasEaten(e.getPlayer().getName()) && plugin.getConfig().getInt("cakeAmount") > 0 && !plugin.getConfig().getBoolean("permaGhost")){
								plugin.db.setHasEaten(e.getPlayer().getName());
								e.getPlayer().sendMessage(ChatColor.GRAY + "Your ghost time has decreased by " + plugin.formatSeconds(plugin.getConfig().getInt("cakeAmount")) + " seconds!");
								if(plugin.db.decrementTime(e.getPlayer().getName(), plugin.getConfig().getInt("cakeAmount")) <= 0){
									if(plugin.getPlayer(e.getPlayer().getName()) != null){
										plugin.getPlayer(e.getPlayer().getName()).sendMessage(ChatColor.GRAY + "Your time is up. Move to be resurrected. " + plugin.resAppend(e.getPlayer().getName()));
									}
								}
							}
						}
						e.setCancelled(true);
					}
				} catch (SQLException e1) {

				}
		}

		@EventHandler
		public void onPlayerInteractEntity(PlayerInteractEntityEvent e){
			if(plugin.db.isGhost(e.getPlayer()))
				e.setCancelled(true);
		}

		@EventHandler
		public void onPlayerBucketEvent(PlayerBucketEvent e){
				if(plugin.db.isGhost(e.getPlayer())){
					e.setCancelled(true);
				}
		}

		@EventHandler
		public void onPlayerMove(final PlayerMoveEvent e){
			Bukkit.getScheduler().scheduleSyncDelayedTask(plugin, (new Runnable(){

				@Override
				public void run() {
					try {
						if(plugin.db.isGhost(e.getPlayer()) && (plugin.db.getTimeLeft(e.getPlayer().getName()) == 0)) {
							Player pl = e.getPlayer();
							plugin.db.removeGhost(pl.getName());
							if(plugin.getPlayer(pl.getName()) != null){
								plugin.getPlayer(pl.getName()).sendMessage("You have been reinstated as a person.");
								synchronized (this){
									plugin.getPlayer(pl.getName()).teleport(plugin.res(plugin.getPlayer(pl.getName())));
									plugin.ghosts.revealGhost(pl.getName());
								}
							}
						}
					} catch (SQLException e1) {

					}
				}

			}));
		}

		@EventHandler
		public void onCraftItem(CraftItemEvent e){
			if(plugin.db.isGhost(plugin.getServer().getPlayer(e.getWhoClicked().getName()))){
				e.setCancelled(true);
			}
		}

		@EventHandler
		public void onPlayerPickupItem(PlayerPickupItemEvent e){
			if(plugin.db.isGhost(e.getPlayer())){
				e.setCancelled(true);
			}
		}

		@EventHandler
		public void onEntityDamageByEntity(EntityDamageByEntityEvent e){
			if(e.getDamager() instanceof Player && plugin.db.isGhost((Player)e.getDamager())){
				e.setCancelled(true);
			}
		}

		@EventHandler
		public void onPlayerCommandPreprocess(PlayerCommandPreprocessEvent e){
			if(!e.getMessage().equals("/timeleft")){
				if(plugin.db.isGhost(e.getPlayer()))
					e.setCancelled(true);
			}
		}

		@EventHandler
		public void onEntityDamage(EntityDamageEvent e){
			if(e.getEntity() instanceof Player && plugin.db.isGhost((Player)e.getEntity())){
				e.setCancelled(true);
			}
		}

		@EventHandler
		public void onPlayerChat(AsyncPlayerChatEvent e){
			if(plugin.db.isGhost(e.getPlayer())){
				for(org.bukkit.World w : plugin.getServer().getWorlds())
					for(Player pl : w.getPlayers())
						if(plugin.db.isGhost(pl) || plugin.hasPermission(pl, "deathpenalty.hearghosts", true))
							pl.sendMessage("[Ghost of "+e.getPlayer().getDisplayName()+  "] " + ChatColor.GRAY + e.getMessage());
				e.setCancelled(true);
			}
		}

		@EventHandler
		public void onPlayerTeleport(PlayerTeleportEvent e){
			if(plugin.db.isGhost(e.getPlayer()) && plugin.getConfig().getBoolean("disablePortals") && e.getCause().equals(TeleportCause.END_PORTAL) || e.getCause().equals(TeleportCause.NETHER_PORTAL))
				e.setCancelled(true);
		}

	}