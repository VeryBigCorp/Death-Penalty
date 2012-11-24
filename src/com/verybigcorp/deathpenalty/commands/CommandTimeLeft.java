package com.verybigcorp.deathpenalty.commands;

import java.sql.SQLException;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import com.verybigcorp.deathpenalty.DeathPenalty;

public class CommandTimeLeft implements CommandExecutor {

	DeathPenalty plugin;
	
	public CommandTimeLeft(DeathPenalty plugin) {
		this.plugin = plugin;
	}
	
	@Override
	public boolean onCommand(CommandSender sender, Command cmd, String commandLabel, String[] args) {
		if(cmd.getName().equalsIgnoreCase("timeleft")){
			if(!(sender instanceof Player)){
				sender.sendMessage("Only players may use this command!");
				return false;
			}
			try {
				if(plugin.db.isGhost((Player)sender)){
					if(!plugin.getConfig().getBoolean("permaGhost"))
						sender.sendMessage(ChatColor.GRAY + (plugin.db.getTimeLeft(sender.getName()) > 0 ? "You have " + plugin.formatSeconds(plugin.db.getTimeLeft(sender.getName())) + " left." : "Your time is up. Move to be resurrected."));
					else
						sender.sendMessage(ChatColor.GRAY + "You are permanently a ghost.");
				}
			} catch (SQLException e) {

			}
			return true;
		}
		
		return false;
	}
}
