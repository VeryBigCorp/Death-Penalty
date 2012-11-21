package com.verybigcorp.deathpenalty;

import java.sql.SQLException;

import org.bukkit.GameMode;
import org.bukkit.entity.Player;

public class Ghost {
	
	DeathPenalty plugin;
	
	public Ghost(DeathPenalty plugin) {
		this.plugin = plugin;
	}

	public void hideGhost(String s) throws SQLException{
		Player[] arr = plugin.getServer().getOnlinePlayers();
		for(int x = 0; x < arr.length; x++)
			if(plugin.getPlayer(arr[x].getName()) != null && !plugin.db.isGhost(arr[x]) && !arr[x].hasPermission("deathpenalty.see"))
				arr[x].hidePlayer(plugin.getPlayer(s));
	}

	public void revealGhost(String s) throws SQLException {
		Player[] arr = plugin.getServer().getOnlinePlayers();
		for(int x = 0; x < arr.length; x++)
			if(plugin.getPlayer(arr[x].getName()) != null && !arr[x].canSee(plugin.getPlayer(s)))
				arr[x].showPlayer(plugin.getPlayer(s));
		if(plugin.getConfig().getBoolean("ghostsFly")){
			plugin.getPlayer(s).setGameMode(GameMode.SURVIVAL);
			plugin.getPlayer(s).getInventory().clear();
		}
		plugin.db.resetLives(s);
	}
}
