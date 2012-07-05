package com.verybigcorp.deathpenalty.timing;

import java.sql.SQLException;
import java.util.TimerTask;

import org.bukkit.ChatColor;

import com.verybigcorp.deathpenalty.DeathPenalty;

public class GhostTimer extends TimerTask {

	DeathPenalty plugin;
	
	public GhostTimer(DeathPenalty plugin) {
		this.plugin = plugin;
	}
	
	@Override
	public void run() {
		try {
			String[] arr = plugin.db.getCachedGhosts().toArray(new String[plugin.db.getCachedGhosts().size()]);
			for(int x = 0; x < arr.length; x++){
				if(plugin.db.decrementTime(arr[x], 1) > 0){
					if(plugin.db.getTimeLeft(arr[x]) == 1 && plugin.getServer().getPlayer(arr[x])!=null)
						plugin.getServer().getPlayer(arr[x]).sendMessage(ChatColor.GRAY + "Your time is up. Move to be resurrected. " + plugin.resAppend(plugin.getPlayer(arr[x]).getName()));
				}
			}
			plugin.db.reduceBans();
		} catch (SQLException e) {

		} catch (Exception e){

		}
	}

}
