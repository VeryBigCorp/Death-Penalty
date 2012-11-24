package com.verybigcorp.deathpenalty;

import java.sql.SQLException;

import org.bukkit.GameMode;
import org.bukkit.entity.Player;

public class Ghost {
	
	DeathPenalty plugin;
	
	public Ghost(DeathPenalty plugin) {
		this.plugin = plugin;
	}

	public void hideGhost(String s) {
		Player[] onlinePlayers = plugin.getServer().getOnlinePlayers();
		for(Player p : onlinePlayers)
			if(!plugin.db.isGhost(p) && !p.hasPermission("deathpenalty.see"))
				p.hidePlayer(plugin.getPlayer(s));
	}

	public void revealGhost(String s) {
		Player[] onlinePlayers = plugin.getServer().getOnlinePlayers();
		for(Player p : onlinePlayers)
			if(!p.canSee(plugin.getPlayer(s)))
				p.showPlayer(plugin.getPlayer(s));
		if(!plugin.getConfig().getBoolean("ghostsFly")){
			plugin.getPlayer(s).setGameMode(GameMode.SURVIVAL);
			plugin.getPlayer(s).getInventory().clear();
		}
		plugin.db.resetLives(s);
	}
	
	public void hideGhosts() throws SQLException {
		for(String s : plugin.db.getCachedGhosts()){
			if(plugin.getPlayer(s) != null)
				this.hideGhost(s);
		}
	}
	
	public boolean isGhost(String name) {
		return plugin.db.isGhost(name);
	}
}
