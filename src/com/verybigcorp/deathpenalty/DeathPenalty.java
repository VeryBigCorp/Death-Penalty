package com.verybigcorp.deathpenalty;

import java.io.File;
import java.io.IOException;
import java.sql.SQLException;
import java.util.Timer;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

import org.bukkit.Location;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;

import ru.tehkode.permissions.PermissionManager;
import ru.tehkode.permissions.bukkit.PermissionsEx;

import com.verybigcorp.deathpenalty.commands.CommandDP;
import com.verybigcorp.deathpenalty.commands.CommandTimeLeft;
import com.verybigcorp.deathpenalty.timing.GhostTimer;

public class DeathPenalty extends JavaPlugin {
	Logger log;
	
	DPListeners l = new DPListeners(this);
	public GhostTimer g = new GhostTimer(this);
	Timer gtimer = new Timer();
	public boolean debugMode = false;
	public boolean isGhostSpawnLocationNotSet = false;
	public boolean isPlayerSpawnLocationNotSet = false;
	public YamlConfiguration spawn = new YamlConfiguration();
	public DBHandler db;
	public CommandDP dpCommand = new CommandDP(this);
	public CommandTimeLeft timeleftCommand = new CommandTimeLeft(this);
	public Ghost ghosts = new Ghost(this);
	
	@Override
	public void onDisable(){
		if(gtimer != null)
			gtimer.cancel();
		db.finish();
		log("v"+ this.getDescription().getVersion() + "is now disabled.");
	}

	@Override
	public void onEnable(){
		setupConfig();
		db = new DBHandler(this);
		
		try {
			reloadSpawnConfig();
			spawn.set("world", spawn.getString("world"));
			spawn.set("ghostSpawn", spawn.getVector("ghostSpawn"));
			saveSpawnConfig();
			reloadSpawnConfig();
		} catch (IOException e) {
			log("error in the creation of the database! " + e.getMessage());
		}
		
		// register the events
		PluginManager pm = this.getServer().getPluginManager();
		pm.registerEvents(l, this);
		
		// register the command
		getCommand("dp").setExecutor(dpCommand);
		getCommand("timeleft").setExecutor(timeleftCommand);
		
		if(!getConfig().getBoolean("permaGhost"))
			gtimer.schedule(g, 1000, 1000);
		isGhostSpawnLocationNotSet = spawn.getVector("ghostSpawn") == null;
		isPlayerSpawnLocationNotSet = spawn.getVector("pSpawn") == null;
		log("v"+ this.getDescription().getVersion() + "is now enabled.");
	}
	
	public void setupConfig() {
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
	}

	public void log(String s){
		if(log == null)
			log = getLogger();
		log.info(s);
	}

	public void reloadSpawnConfig() throws IOException {
		File f = new File(getDataFolder(), "ghostSpawn.yml");
		f.createNewFile();
		spawn = YamlConfiguration.loadConfiguration(f);
	}

	public void saveSpawnConfig() throws IOException {
		if(spawn != null)
			spawn.save(new File(getDataFolder(), "ghostSpawn.yml"));
	}

	public String formatSeconds(int seconds){
		String ret = "";
		ret = String.format("%d day(s), %d hour(s), %d minute(s), %d second(s)", TimeUnit.SECONDS.toDays(seconds), TimeUnit.SECONDS.toHours(seconds) % 24, TimeUnit.SECONDS.toMinutes(seconds) - TimeUnit.HOURS.toMinutes(TimeUnit.SECONDS.toHours(seconds)), TimeUnit.SECONDS.toSeconds(seconds) - TimeUnit.MINUTES.toSeconds(TimeUnit.SECONDS.toMinutes(seconds)));
		return ret;
	}

	public Location res(Player p){
		Location loc = p.getLocation();
		if(!isPlayerSpawnLocationNotSet){
			loc = spawn.getVector("pSpawn").toLocation(getServer().getWorld(spawn.getString("pWorld")));
			while(!loc.getChunk().isLoaded()) loc.getChunk().load();
		}
		return loc;
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
		return getServer().getPlayer(name);
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
}