package com.verybigcorp.deathpenalty;

import java.io.File;
import java.io.IOException;
import java.sql.SQLException;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

import org.bukkit.ChatColor;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.YamlConfiguration;
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
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.util.Vector;

import ru.tehkode.permissions.PermissionManager;
import ru.tehkode.permissions.bukkit.PermissionsEx;

public class DeathPenalty extends JavaPlugin {
	Logger log;
	EventListener l;
	GhostTimer g;
	Timer gtimer;
	boolean debugMode = false;
	boolean isNull = false; // If the ghost spawn location is not set in the configuration
	boolean isNull2 = false;
	YamlConfiguration spawn;
	DBHandler db;
	@Override
	public void onDisable(){
		if(gtimer != null)
			gtimer.cancel();
		db.finalize();
		log("v"+ this.getDescription().getVersion() + "is now disabled.");
	}
	
	@Override
	public void onEnable(){
		getConfig().set("dbVersion", getConfig().getInt("dbVersion", -1) == -1 ? DBHandler.VERSION : getConfig().getInt("dbVersion", -1));
		getConfig().set("ghostTime", getConfig().getInt("ghostTime") == 0 ? 300 : getConfig().getInt("ghostTime"));
		getConfig().set("usePermissions", getConfig().getBoolean("usePermissions"));
		getConfig().set("cakeAmount", getConfig().getInt("cakeAmount"));
		getConfig().set("disablePortals", getConfig().getBoolean("disablePortals"));
		getConfig().set("ghostsFly",getConfig().getBoolean("ghostsFly"));
		getConfig().set("lives", getConfig().getInt("lives"));
		getConfig().set("maxGhostTimes",getConfig().getInt("maxGhostTimes", 0));
		getConfig().set("banOnGhostLifeDepletion", getConfig().getBoolean("banOnGhostLifeDepletion", false));
		getConfig().set("banTime", getConfig().getInt("banTime"));
		getConfig().set("permaGhost", getConfig().getBoolean("permaGhost"));
		//getConfig().set("banTime", );
		saveConfig();
		try {
			relSpawn();
			spawn.set("world", spawn.getString("world"));
			spawn.set("ghostSpawn", spawn.getVector("ghostSpawn"));
			saveSpawn();
			relSpawn();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			log("error in the creation of the database! " + e.getMessage());
		}
		l = new EventListener(this);
		db = new DBHandler(this);
		gtimer = new Timer();
		g = new GhostTimer();
		if(!getConfig().getBoolean("permaGhost"))
			gtimer.schedule(g, 1000, 1000);
		isNull = spawn.getVector("ghostSpawn") == null;
		isNull2 = spawn.getVector("pSpawn") == null;
		log("v"+ this.getDescription().getVersion() + "is now enabled.");
	}
	
	public void log(String s){
		if(log == null)
			log = getLogger();
		log.info(s);
	}
	
	public void relSpawn() throws IOException {
		File f = new File(getDataFolder(), "ghostSpawn.yml");
		f.createNewFile();
		spawn = YamlConfiguration.loadConfiguration(f);
	}
	
	public void saveSpawn() throws IOException {
		if(spawn != null)
			spawn.save(new File(getDataFolder(), "ghostSpawn.yml"));
	}

	public boolean onCommand(CommandSender sender, Command cmd, String commandLabel, String[] args){
		if(cmd.getName().equalsIgnoreCase("dp")){
			if(args.length == 1){
				if(args[0].equalsIgnoreCase("setghostspawn")){
					if(!(sender instanceof Player)){
						sender.sendMessage("Only players may use this command.");
						return false;
					}
					if(!(hasPermission((Player)sender, "deathpenalty.changespawn", true)))
						return false;
					spawn.set("ghostSpawn", ((Player)sender).getLocation().toVector());
					spawn.set("world", ((Player)sender).getLocation().getWorld().getName());
					try {
						saveSpawn();
						relSpawn();
					} catch (IOException e) {
						
					}
					isNull = false;
					sender.sendMessage(ChatColor.GREEN + "Ghost spawn location set!");
					return true;
				} else if(debugMode && args[0].equalsIgnoreCase("dump")){
					try {
						for(String s : db.getGhosts())
							sender.sendMessage("Player "+ s + " is a ghost.");
					} catch (SQLException e) {
						
					}
					return true;
				} else if(args[0].equalsIgnoreCase("setafterghostspawn")){
					if(!(sender instanceof Player)){
						sender.sendMessage("Only players may use this command.");
						return false;
					}
					if(!(hasPermission((Player)sender, "deathpenalty.changespawn", true)))
						return false;
					spawn.set("pSpawn", ((Player)sender).getLocation().toVector());
					spawn.set("pWorld", ((Player)sender).getLocation().getWorld().getName());
					try {
						saveSpawn();
						relSpawn();
					} catch (IOException e) {
						
					}
					isNull2 = false;
					sender.sendMessage(ChatColor.GREEN + "Player respawn location set!");
					return true;
				} else if(args[0].equalsIgnoreCase("dump")){
					if(!(sender instanceof Player)){
						try {
							if(db.getCachedGhosts().size() == 0)
								sender.sendMessage("There are no ghosts.");
							for(String s : db.getCachedGhosts()){
								sender.sendMessage("The Ghost of "+s + " has "+ formatSeconds(db.getTimeLeft(s)) + " left and is at " + db.getGhostTimesLeft(s) + " ghost times.");
							}
						} catch (SQLException e) {
							
						}
						return true;
					}
				} else if(args[0].equalsIgnoreCase("fixghosts")){
					if(!(sender instanceof Player)){
						try {
							hideGhosts();
							log("Ghosts are now hidden.");
						} catch (SQLException e) {
							log("Unable to fix ghosts. Please try again or report this error.");
						}
					}
				} else if(args[0].equalsIgnoreCase("reload")){
					if(sender instanceof Player)
						return false;
					try {
						relSpawn();
						reloadConfig();
						sender.sendMessage("Configuration reloaded.");
						
					} catch (IOException e) {
						
					}
				}
			} else if(args.length == 2){
				if(args[0].equalsIgnoreCase("resurrect") && ((sender instanceof Player && hasPermission((Player)sender, "deathpenalty.resurrect", false)) || !(sender instanceof Player))){
					try {
						if(getPlayer(args[1]) != null && db.isGhost(getPlayer(args[1]))){
							db.decrementTime(getPlayer(args[1]).getName(), db.getTimeLeft(getPlayer(args[1]).getName()));
							sender.sendMessage("Resurrecting "+getPlayer(args[1]).getDisplayName());
							getPlayer(args[1]).sendMessage(ChatColor.GRAY + "Your time is up. Move to be resurrected. " + resAppend(getPlayer(args[1]).getName()));
						} else {
							sender.sendMessage("That player is not a ghost!");
						}
					} catch (SQLException e) {
						
					}
					return true;
				} if(args[0].equalsIgnoreCase("unban") && !(sender instanceof Player)){
					try {
						db.removeBan(args[1]);
						sender.sendMessage("Ban lifted.");
					} catch (SQLException e) {
						
					}
				}
			}
		} else if(cmd.getName().equalsIgnoreCase("timeleft")){
			if(!(sender instanceof Player)){
				sender.sendMessage("Only players may use this command!");
				return false;
			}
			try {
				if(db.isGhost((Player)sender)){
					if(!getConfig().getBoolean("permaGhost"))
						sender.sendMessage(ChatColor.GRAY + (db.getTimeLeft(sender.getName()) > 0 ? "You have "+ formatSeconds(db.getTimeLeft(sender.getName())) + " left." : "Your time is up. Move to be resurrected."));
					else
						sender.sendMessage(ChatColor.GRAY + "You are permanently a ghost.");
				}
			} catch (SQLException e) {
				
			}
			return true;
		}
		return false;
	}
	
	public String formatSeconds(int seconds){
		String ret = "";
		ret = String.format("%d day(s), %d hour(s), %d minute(s), %d second(s)", TimeUnit.SECONDS.toDays(seconds), TimeUnit.SECONDS.toHours(seconds) % 24, TimeUnit.SECONDS.toMinutes(seconds) - TimeUnit.HOURS.toMinutes(TimeUnit.SECONDS.toHours(seconds)), TimeUnit.SECONDS.toSeconds(seconds) - TimeUnit.MINUTES.toSeconds(TimeUnit.SECONDS.toMinutes(seconds)));
		return ret;
	}
	
	public class EventListener implements Listener {
		DeathPenalty p;
		public EventListener(DeathPenalty plugin){
			p = plugin;
			p.getServer().getPluginManager().registerEvents(this, plugin);
		}
		
		@EventHandler
		public void onPlayerJoin(final PlayerJoinEvent e){
				try {
					db.addPlayer(e.getPlayer().getName());
					if(db.isGhost(e.getPlayer())){
						e.getPlayer().sendMessage("You are a ghost.");
						if(db.getTimeLeft(e.getPlayer().getName()) <= 0)
							e.getPlayer().sendMessage(ChatColor.GRAY + "Your time is up. Move to be resurrected. " + resAppend(e.getPlayer().getName()));
						if(getConfig().getBoolean("ghostsFly"))
							e.getPlayer().setGameMode(GameMode.CREATIVE);
					}
					new Timer().schedule(new DelayedHide(), 2000);
				} catch (SQLException e1) {
					
				}
		}
		
		@EventHandler
		public void onPlayerLogin(PlayerLoginEvent e){
			try {
				if(db.isBanned(e.getPlayer().getName())){
					e.disallow(Result.KICK_OTHER, "You are still banned for " + formatSeconds(db.banTimeLeft(e.getPlayer().getName())));
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
					if(db.isGhost(e1.getEntity()))
						return;
					if(hasPermission(e1.getEntity(), "deathpenalty.ignore", false))
						return;
		            try {
		            	
						if(db.decrementLives(e1.getEntity().getName()) <= 0){
							db.addGhost(e1.getEntity().getName());
							hideGhost(e1.getEntity().getName());
				            e1.getEntity().sendMessage(ChatColor.GRAY + "You have become a ghost of your former self. To see how much time you have left, type in /timeleft");
				            if(getConfig().getBoolean("banOnGhostLifeDepletion"))
				            	db.increaseGhostTimes(e1.getEntity().getName());
						} else {
							int lives = db.nLives(e1.getEntity().getName());
							String life = " lives";
							if(lives == 1)
								life = " life";
							e1.getEntity().sendMessage(ChatColor.GRAY + "You have "+ lives + life + " left.");
						}

						
						if(db.getGhostTimesLeft(e1.getEntity().getName()) > getConfig().getInt("maxGhostTimes") && getConfig().getBoolean("banOnGhostLifeDepletion")){
							revealGhost(e1.getEntity().getName());
							db.ban(e1.getEntity().getName());
							e1.getEntity().kickPlayer("You have died. You may rejoin in " + formatSeconds(db.banTimeLeft(e1.getEntity().getName())));
							db.resetPlayer(e1.getEntity().getName());
						}
					} catch (SQLException e1) {
						log("sql error!" + e1.getMessage());
					}
		            
				}
				
			}).start();
			
		}
		
		@EventHandler
		public void onEntityTarget(EntityTargetEvent e){
			if(e.getTarget() instanceof Player){
				if(db.isGhost((Player)e.getTarget()))
					e.setTarget(null);
			}
		}
		
		@EventHandler(priority = EventPriority.HIGHEST)
		public void onPlayerRespawn(final PlayerRespawnEvent e){
				try {
					if(db.isGhost(e.getPlayer()) && !isNull){
						e.setRespawnLocation(((Vector) spawn.get("ghostSpawn")).toLocation(getServer().getWorld(spawn.getString("world"))));
						if(getConfig().getBoolean("ghostsFly"))
							e.getPlayer().setGameMode(GameMode.CREATIVE);
					}
					if(db.getGhostTimesLeft(e.getPlayer().getName()) > getConfig().getInt("maxGhostTimes") || !getConfig().getBoolean("banOnGhostLifeDepletion"))
						db.resetLives(e.getPlayer().getName());
				} catch (SQLException e1) {
					
				}
		}
		
	
		@EventHandler
		public void onPlayerExpChange(PlayerExpChangeEvent e){
			if(db.isGhost(e.getPlayer())){
				e.setAmount(0);
			}
		}
		
		@EventHandler
		public void onPlayerDropItem(PlayerDropItemEvent e) {
			if(db.isGhost(e.getPlayer())){
				e.setCancelled(true);
			}
		}
		
		@EventHandler
		public void onBlockPlace(BlockPlaceEvent e){
			if(db.isGhost(e.getPlayer())){
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
			if(db.isGhost(e.getPlayer())){
				e.setCancelled(true);
			}
		}
		
		@EventHandler
		public void onPlayerInteract(final PlayerInteractEvent e){
				try {
					if(db.isGhost(e.getPlayer())){
						if(e.getAction() == Action.RIGHT_CLICK_BLOCK){
							if(e.getClickedBlock().getType().equals(Material.CAKE_BLOCK) && !db.hasEaten(e.getPlayer().getName()) && getConfig().getInt("cakeAmount") > 0 && !getConfig().getBoolean("permaGhost")){
								db.setHasEaten(e.getPlayer().getName());
								e.getPlayer().sendMessage(ChatColor.GRAY + "Your ghost time has decreased by " + formatSeconds(getConfig().getInt("cakeAmount")) + " seconds!");
								if(db.decrementTime(e.getPlayer().getName(), getConfig().getInt("cakeAmount")) <= 0){
									if(getPlayer(e.getPlayer().getName()) != null){
										getPlayer(e.getPlayer().getName()).sendMessage(ChatColor.GRAY + "Your time is up. Move to be resurrected. " + resAppend(e.getPlayer().getName()));
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
			if(db.isGhost(e.getPlayer()))
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
						if(db.isGhost(e.getPlayer()) && db.getTimeLeft(e.getPlayer().getName()) == 0){
							Player pl = e.getPlayer();
							db.removeGhost(pl.getName());
							if(getPlayer(pl.getName()) != null){
								getPlayer(pl.getName()).sendMessage("You have been reinstated as a person.");
								synchronized (this){
									getPlayer(pl.getName()).teleport(res(getPlayer(pl.getName())));
									revealGhost(pl.getName());
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
			if(db.isGhost(getServer().getPlayer(e.getWhoClicked().getName()))){
				e.setCancelled(true);
			}
		}
		
		@EventHandler
		public void onPlayerPickupItem(PlayerPickupItemEvent e){
			if(db.isGhost(e.getPlayer())){
				e.setCancelled(true);
			}
		}
		
		@EventHandler
		public void onEntityDamageByEntity(EntityDamageByEntityEvent e){
			if(e.getDamager() instanceof Player && db.isGhost((Player)e.getDamager())){
				e.setCancelled(true);
			}
		}
		
		@EventHandler
		public void onPlayerCommandPreprocess(PlayerCommandPreprocessEvent e){
			if(!e.getMessage().equals("/timeleft")){
				if(db.isGhost(e.getPlayer()))
					e.setCancelled(true);
			}
		}
		
		@EventHandler
		public void onEntityDamage(EntityDamageEvent e){
			if(e.getEntity() instanceof Player && db.isGhost((Player)e.getEntity())){
				e.setCancelled(true);
			}
		}
		
		@EventHandler
		public void onPlayerChat(PlayerChatEvent e){
			if(db.isGhost(e.getPlayer())){
				for(World w : getServer().getWorlds())
					for(Player pl : w.getPlayers())
						if(db.isGhost(pl) || hasPermission(pl, "deathpenalty.hearghosts", true))
							pl.sendMessage("[Ghost of "+e.getPlayer().getDisplayName()+  "] " + ChatColor.GRAY + e.getMessage());
				e.setCancelled(true);
			}
		}
		
		@EventHandler
		public void onPlayerTeleport(PlayerTeleportEvent e){
			if(db.isGhost(e.getPlayer()) && getConfig().getBoolean("disablePortals") && e.getCause().equals(TeleportCause.END_PORTAL) || e.getCause().equals(TeleportCause.NETHER_PORTAL))
				e.setCancelled(true);
		}
		
	}
	
	public String resAppend(String p) throws SQLException{
		String s = "";
		int tleft = getConfig().getInt("maxGhostTimes") - db.getGhostTimesLeft(p);
		if(getConfig().getBoolean("banOnGhostLifeDepletion")){
			String time = "times";
			if(tleft == 1)
				time = "time";
			s = "You may be a ghost " + (tleft < 0 ? 0 : tleft) + " more " + time + ".";
		}
		return s != "" ? tleft == 0 ? "Once you're out of lives, you will be banned." + "" : s : "";
	}
	
	public Location res(Player p){
		Location loc = p.getLocation();
		if(!isNull2){
			loc = spawn.getVector("pSpawn").toLocation(getServer().getWorld(spawn.getString("pWorld")));
			while(!loc.getChunk().isLoaded()) loc.getChunk().load();
		}
		return loc;
	}
	
	public class GhostTimer extends TimerTask {

		@Override
		public void run() {
			// TODO Auto-generated method stub
			try {
				String[] arr = db.getCachedGhosts().toArray(new String[db.getCachedGhosts().size()]);
				for(int x = 0; x < arr.length; x++){
					if(db.decrementTime(arr[x], 1) > 0){
						if(db.getTimeLeft(arr[x]) == 1 && getServer().getPlayer(arr[x])!=null)
							getServer().getPlayer(arr[x]).sendMessage(ChatColor.GRAY + "Your time is up. Move to be resurrected. " + resAppend(getPlayer(arr[x]).getName()));
					}
				}
				db.reduceBans();
			} catch (SQLException e) {
				
			} catch (Exception e){
				
			}
		}
		
	}
	
	public class DelayedHide extends TimerTask {
		@Override
		public void run(){
			try {
				hideGhosts();
			} catch (SQLException e) {
				
			}
		}
	}
	
	public boolean hasPermission(Player p, String permission, boolean ovrOP){
		if(getConfig().getBoolean("usePermissions")){
			return hPerm(p, permission);
		}
		return ovrOP && p.isOp();
	}
	
	public boolean hPerm(Player p, String perm){
		if(getServer().getPluginManager().isPluginEnabled("PermissionsEx")){
		    PermissionManager permissions = PermissionsEx.getPermissionManager();
		    return permissions.has(p, perm);
		}
		return p.hasPermission(perm);
	}
	
	
	public Player getPlayer(String name){
		/*Player pl = null;
		for(int x = 0; x < getServer().getWorlds().size(); x++){
			for(int y = 0; y < getServer().getWorlds().get(x).getPlayers().size(); y++)
				if(getServer().getWorlds().get(x).getPlayers().get(y).getName().equalsIgnoreCase(name))
					pl = getServer().getWorlds().get(x).getPlayers().get(y);
		}*/
		return getServer().getPlayer(name);
	}
	
	public void hideGhost(String s) throws SQLException{
		Player[] arr = getServer().getOnlinePlayers();
		for(int x = 0; x < arr.length; x++)
			if(getPlayer(arr[x].getName()) != null && !db.isGhost(arr[x]) && !arr[x].hasPermission("deathpenalty.see"))
				arr[x].hidePlayer(getPlayer(s));
	}
	
	public void revealGhost(String s) throws SQLException {
		Player[] arr = getServer().getOnlinePlayers();
		for(int x = 0; x < arr.length; x++)
			if(getPlayer(arr[x].getName()) != null && !arr[x].canSee(getPlayer(s)))
				arr[x].showPlayer(getPlayer(s));
		if(getConfig().getBoolean("ghostsFly")){
			getPlayer(s).setGameMode(GameMode.SURVIVAL);
			getPlayer(s).getInventory().clear();
		}
		db.resetLives(s);
	}
	
	public void hideGhosts() throws SQLException {
		for(String s : db.getGhosts()){
			if(getPlayer(s) != null)
				hideGhost(s);
		}
	}
}
