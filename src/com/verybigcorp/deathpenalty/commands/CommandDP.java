package com.verybigcorp.deathpenalty.commands;

import java.io.IOException;
import java.sql.SQLException;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import com.verybigcorp.deathpenalty.DeathPenalty;

public class CommandDP implements CommandExecutor {

	DeathPenalty plugin;
	
	public CommandDP(DeathPenalty plugin) {
		this.plugin = plugin;
	}
	
	public boolean onCommand(CommandSender sender, Command cmd, String commandLabel, String[] args){
		if(cmd.getName().equalsIgnoreCase("dp")){
			if(args.length == 1){
				if(args[0].equalsIgnoreCase("setghostspawn")){
					if(!(sender instanceof Player)){
						sender.sendMessage("Only players may use this command.");
						return false;
					}
					if(!(plugin.hasPermission((Player)sender, "deathpenalty.changespawn", true)))
						return false;
					plugin.spawn.set("ghostSpawn", ((Player)sender).getLocation().toVector());
					plugin.spawn.set("world", ((Player)sender).getLocation().getWorld().getName());
					try {
						plugin.saveSpawn();
						plugin.relSpawn();
					} catch (IOException e) {

					}
					plugin.isNull = false;
					sender.sendMessage(ChatColor.GREEN + "Ghost spawn location set!");
					return true;
				} else if(plugin.debugMode && args[0].equalsIgnoreCase("dump")){
					try {
						for(String s : plugin.db.getGhosts())
							sender.sendMessage("Player "+ s + " is a ghost.");
					} catch (SQLException e) {

					}
					return true;
				} else if(args[0].equalsIgnoreCase("setafterghostspawn")){
					if(!(sender instanceof Player)){
						sender.sendMessage("Only players may use this command.");
						return false;
					}
					if(!(plugin.hasPermission((Player)sender, "deathpenalty.changespawn", true)))
						return false;
					plugin.spawn.set("pSpawn", ((Player)sender).getLocation().toVector());
					plugin.spawn.set("pWorld", ((Player)sender).getLocation().getWorld().getName());
					try {
						plugin.saveSpawn();
						plugin.relSpawn();
					} catch (IOException e) {

					}
					plugin.isNull2 = false;
					sender.sendMessage(ChatColor.GREEN + "Player respawn location set!");
					return true;
				} else if(args[0].equalsIgnoreCase("dump")){
					if(!(sender instanceof Player)){
						try {
							if(plugin.db.getCachedGhosts().size() == 0)
								sender.sendMessage("There are no ghosts.");
							for(String s : plugin.db.getCachedGhosts()){
								sender.sendMessage("The Ghost of "+s + " has "+ plugin.formatSeconds(plugin.db.getTimeLeft(s)) + " left and is at " + plugin.db.getGhostTimesLeft(s) + " ghost times.");
							}
						} catch (SQLException e) {

						}
						return true;
					}
				} else if(args[0].equalsIgnoreCase("fixghosts")){
					if(!(sender instanceof Player)){
						try {
							plugin.hideGhosts();
							plugin.log("Ghosts are now hidden.");
						} catch (SQLException e) {
							plugin.log("Unable to fix ghosts. Please try again or report this error.");
						}
					}
				} else if(args[0].equalsIgnoreCase("reload")){
					if(sender instanceof Player)
						return false;
					try {
						plugin.relSpawn();
						plugin.reloadConfig();
						sender.sendMessage("Configuration reloaded.");

					} catch (IOException e) {

					}
				}
			} else if(args.length == 2){
				if(args[0].equalsIgnoreCase("resurrect") && ((sender instanceof Player && plugin.hasPermission((Player)sender, "deathpenalty.resurrect", false)) || !(sender instanceof Player))){
					try {
						if(plugin.getPlayer(args[1]) != null && plugin.db.isGhost(plugin.getPlayer(args[1]))){
							plugin.db.decrementTime(plugin.getPlayer(args[1]).getName(), plugin.db.getTimeLeft(plugin.getPlayer(args[1]).getName()));
							sender.sendMessage("Resurrecting " + plugin.getPlayer(args[1]).getDisplayName());
							plugin.getPlayer(args[1]).sendMessage(ChatColor.GRAY + "Your time is up. Move to be resurrected. " + plugin.resAppend(plugin.getPlayer(args[1]).getName()));
						} else {
							sender.sendMessage("That player is not a ghost!");
						}
					} catch (SQLException e) {

					}
					return true;
				} if(args[0].equalsIgnoreCase("unban") && !(sender instanceof Player)){
					try {
						plugin.db.removeBan(args[1]);
						sender.sendMessage("Ban lifted.");
					} catch (SQLException e) {

					}
				}  else if(args[0].equalsIgnoreCase("addlife") && ((sender instanceof Player && plugin.hasPermission((Player)sender, "deathpenalty.add", true)) || !(sender instanceof Player))){
					try {
						plugin.db.increaseLives(plugin.getPlayer(args[1]).getName());
						sender.sendMessage("Life addition successful! " + plugin.getPlayer(args[1]).getDisplayName() + " now has " + plugin.db.nLives(plugin.getPlayer(args[1]).getName()) + " lives left.");
					} catch(Exception e){
						sender.sendMessage("Unable to add life. Is the name incorrect?");
					}
				}
			}
		}
		return false;
	}
}
