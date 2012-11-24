package com.verybigcorp.deathpenalty.timing;

import java.sql.SQLException;
import java.util.TimerTask;

import com.verybigcorp.deathpenalty.DeathPenalty;

public class DelayedHide extends TimerTask {
	
	DeathPenalty plugin;
	
	public DelayedHide(DeathPenalty plugin) {
		this.plugin = plugin;
	}
	
	@Override
	public void run(){
		try {
			plugin.ghosts.hideGhosts();
		} catch (SQLException e) {

		}
	}
}
