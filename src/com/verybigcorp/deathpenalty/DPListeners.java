package com.verybigcorp.deathpenalty;

import java.sql.SQLException;
import java.util.Timer;

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
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.inventory.CraftItemEvent;
import org.bukkit.event.player.PlayerChatEvent;
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
		DeathPenalty p;
		public DPListeners(DeathPenalty plugin){
			p = plugin;
		}

		@EventHandler
		public void onPlayerJoin(final PlayerJoinEvent e){
				try {
					p.db.addPlayer(e.getPlayer().getName());
					if(p.db.isGhost(e.getPlayer())){
						e.getPlayer().sendMessage("You are a ghost.");
						if(p.db.getTimeLeft(e.getPlayer().getName()) <= 0)
							e.getPlayer().sendMessage(ChatColor.GRAY + "Your time is up. Move to be resurrected. " + p.resAppend(e.getPlayer().getName()));
						if(p.getConfig().getBoolean("ghostsFly"))
							e.getPlayer().setGameMode(GameMode.CREATIVE);
					}
					new Timer().schedule(new DelayedHide(p), 2000);
				} catch (SQLException e1) {

				}
		}

		@EventHandler
		public void onPlayerLogin(PlayerLoginEvent e){
			try {
				if(p.db.isBanned(e.getPlayer().getName())){
					e.disallow(Result.KICK_OTHER, "You are still banned for " + p.formatSeconds(p.db.banTimeLeft(e.getPlayer().getName())));
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
					if(p.db.isGhost(e1.getEntity()))
						return;
					if(p.hasPermission(e1.getEntity(), "deathpenalty.ignore", false))
						return;
		            try {

						if(p.db.decrementLives(e1.getEntity().getName()) <= 0){
							p.db.addGhost(e1.getEntity().getName());
							p.hideGhost(e1.getEntity().getName());
				            e1.getEntity().sendMessage(ChatColor.GRAY + "You have become a ghost of your former self. To see how much time you have left, type in /timeleft");
				            if(p.getConfig().getBoolean("banOnGhostLifeDepletion"))
				            	p.db.increaseGhostTimes(e1.getEntity().getName());
						} else {
							int lives = p.db.nLives(e1.getEntity().getName());
							String life = " lives";
							if(lives == 1)
								life = " life";
							e1.getEntity().sendMessage(ChatColor.GRAY + "You have "+ lives + life + " left.");
						}


						if(p.db.getGhostTimesLeft(e1.getEntity().getName()) > p.getConfig().getInt("maxGhostTimes") && p.getConfig().getBoolean("banOnGhostLifeDepletion")){
							p.revealGhost(e1.getEntity().getName());
							p.db.ban(e1.getEntity().getName());
							e1.getEntity().kickPlayer("You have died. You may rejoin in " + p.formatSeconds(p.db.banTimeLeft(e1.getEntity().getName())));
							p.db.resetPlayer(e1.getEntity().getName());
						}
					} catch (SQLException e1) {
						p.log("sql error!" + e1.getMessage());
					}

				}

			}).start();

		}

		@EventHandler
		public void onEntityTarget(EntityTargetEvent e){
			if(e.getTarget() instanceof Player){
				if(p.db.isGhost((Player)e.getTarget()))
					e.setTarget(null);
			}
		}

		@EventHandler(priority = EventPriority.HIGHEST)
		public void onPlayerRespawn(final PlayerRespawnEvent e){
				try {
					if(p.db.isGhost(e.getPlayer()) && !p.isNull){
						e.setRespawnLocation(((Vector) p.spawn.get("ghostSpawn")).toLocation(p.getServer().getWorld(p.spawn.getString("world"))));
						if(p.getConfig().getBoolean("ghostsFly"))
							e.getPlayer().setGameMode(GameMode.CREATIVE);
					}
					if(p.db.getGhostTimesLeft(e.getPlayer().getName()) > p.getConfig().getInt("maxGhostTimes") || !p.getConfig().getBoolean("banOnGhostLifeDepletion"))
						p.db.resetLives(e.getPlayer().getName());
				} catch (SQLException e1) {

				}
		}


		@EventHandler
		public void onPlayerExpChange(PlayerExpChangeEvent e){
			if(p.db.isGhost(e.getPlayer())){
				e.setAmount(0);
			}
		}

		@EventHandler
		public void onPlayerDropItem(PlayerDropItemEvent e) {
			if(p.db.isGhost(e.getPlayer())){
				e.setCancelled(true);
			}
		}

		@EventHandler
		public void onBlockPlace(BlockPlaceEvent e){
			if(p.db.isGhost(e.getPlayer())){
				e.setCancelled(true);
			}
		}

		/*@EventHandler
		public void onFoodLevelChange(FoodLevelChangeEvent e){
			try {
				if(db.isGhost((Player)e.getEntity())){
					//e.setCancelled(true);
				}
			} catch (SQLException e1) {
				
			}
		}*/

		@EventHandler
		public void onBlockBreak(BlockBreakEvent e){
			if(p.db.isGhost(e.getPlayer())){
				e.setCancelled(true);
			}
		}

		@EventHandler
		public void onPlayerInteract(final PlayerInteractEvent e){
				try {
					if(p.db.isGhost(e.getPlayer())){
						if(e.getAction() == Action.RIGHT_CLICK_BLOCK){
							if(e.getClickedBlock().getType().equals(Material.CAKE_BLOCK) && !p.db.hasEaten(e.getPlayer().getName()) && p.getConfig().getInt("cakeAmount") > 0 && !p.getConfig().getBoolean("permaGhost")){
								p.db.setHasEaten(e.getPlayer().getName());
								e.getPlayer().sendMessage(ChatColor.GRAY + "Your ghost time has decreased by " + p.formatSeconds(p.getConfig().getInt("cakeAmount")) + " seconds!");
								if(p.db.decrementTime(e.getPlayer().getName(), p.getConfig().getInt("cakeAmount")) <= 0){
									if(p.getPlayer(e.getPlayer().getName()) != null){
										p.getPlayer(e.getPlayer().getName()).sendMessage(ChatColor.GRAY + "Your time is up. Move to be resurrected. " + p.resAppend(e.getPlayer().getName()));
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
			if(p.db.isGhost(e.getPlayer()))
				e.setCancelled(true);
		}

		/*@EventHandler
		public void onPlayerBucketEvent(PlayerBucketEvent e){
			try {
				if(db.isGhost(e.getPlayer())){
					e.setCancelled(true);
				}
			} catch (SQLException e1) {
				
			}
		}*/

		@EventHandler
		public void onPlayerMove(final PlayerMoveEvent e){
			new Thread(new Runnable(){

				@Override
				public void run() {
					// TODO Auto-generated method stub
					try {
						if(p.db.isGhost(e.getPlayer()) && p.db.getTimeLeft(e.getPlayer().getName()) == 0){
							Player pl = e.getPlayer();
							p.db.removeGhost(pl.getName());
							if(p.getPlayer(pl.getName()) != null){
								p.getPlayer(pl.getName()).sendMessage("You have been reinstated as a person.");
								synchronized (this){
									p.getPlayer(pl.getName()).teleport(p.res(p.getPlayer(pl.getName())));
									p.revealGhost(pl.getName());
								}
							}
						}
					} catch (SQLException e1) {

					}
				}

			}).start();
		}

		@EventHandler
		public void onCraftItem(CraftItemEvent e){
			if(p.db.isGhost(p.getServer().getPlayer(e.getWhoClicked().getName()))){
				e.setCancelled(true);
			}
		}

		@EventHandler
		public void onPlayerPickupItem(PlayerPickupItemEvent e){
			if(p.db.isGhost(e.getPlayer())){
				e.setCancelled(true);
			}
		}

		@EventHandler
		public void onEntityDamageByEntity(EntityDamageByEntityEvent e){
			if(e.getDamager() instanceof Player && p.db.isGhost((Player)e.getDamager())){
				e.setCancelled(true);
			}
		}

		@EventHandler
		public void onPlayerCommandPreprocess(PlayerCommandPreprocessEvent e){
			if(!e.getMessage().equals("/timeleft")){
				if(p.db.isGhost(e.getPlayer()))
					e.setCancelled(true);
			}
		}

		@EventHandler
		public void onEntityDamage(EntityDamageEvent e){
			if(e.getEntity() instanceof Player && p.db.isGhost((Player)e.getEntity())){
				e.setCancelled(true);
			}
		}

		@EventHandler
		public void onPlayerChat(PlayerChatEvent e){
			if(p.db.isGhost(e.getPlayer())){
				for(org.bukkit.World w : p.getServer().getWorlds())
					for(Player pl : w.getPlayers())
						if(p.db.isGhost(pl) || p.hasPermission(pl, "deathpenalty.hearghosts", true))
							pl.sendMessage("[Ghost of "+e.getPlayer().getDisplayName()+  "] " + ChatColor.GRAY + e.getMessage());
				e.setCancelled(true);
			}
		}

		@EventHandler
		public void onPlayerTeleport(PlayerTeleportEvent e){
			if(p.db.isGhost(e.getPlayer()) && p.getConfig().getBoolean("disablePortals") && e.getCause().equals(TeleportCause.END_PORTAL) || e.getCause().equals(TeleportCause.NETHER_PORTAL))
				e.setCancelled(true);
		}

	}