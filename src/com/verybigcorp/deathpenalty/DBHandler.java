package com.verybigcorp.deathpenalty;

import java.io.File;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

import org.bukkit.entity.Player;

// The class to handle all of the database calls
public class DBHandler {
	Connection conn;
	Statement stat;
	DeathPenalty plugin;
	List<String> cachedGhosts;
	public static int VERSION = 2;

	public DBHandler(DeathPenalty p){
		plugin = p;
		try {
			Class.forName("org.sqlite.JDBC");
			File d = new File(p.getDataFolder().getCanonicalPath()+"/ghosts.db");
			d.createNewFile();
			conn = DriverManager.getConnection("jdbc:sqlite:"+ p.getDataFolder().getCanonicalPath()+"/ghosts.db");
			conn.setAutoCommit(true);
			stat = conn.createStatement();
			create_tablesConnect();
			updateDB();
			cachedGhosts = getGhosts();
		} catch (SQLException e) {
			p.log("error in the sql: " + e.getMessage());
		} catch (ClassNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			p.log("error in creation of the database: "+e.getLocalizedMessage());
		}
	}

	public void removeCached(String s){
		cachedGhosts.remove(s);
	}

	public void addCached(String s){
		cachedGhosts.add(s);
	}

	public void finish(){
		try {
			conn.close();
		} catch (SQLException e) {

		}
	}

	// Ghost methods

	public void addGhost(String s) throws SQLException {
		addCached(s);
		PreparedStatement sql = conn.prepareStatement("UPDATE players SET isGhost=?, timeleft=?, hasEaten=? WHERE username=?;");
		sql.setBoolean(1, true);
		sql.setInt(2, plugin.getConfig().getInt("ghostTime"));
		sql.setBoolean(3, false);
		sql.setString(4, s);
		sql.execute();
		sql.close();
	}

	public void addPlayer(String p) throws SQLException{
		if(getPlayers().contains(p))
			return;
		PreparedStatement sql = conn.prepareStatement("INSERT INTO players (username, isGhost, timeleft, hasEaten, lives, ghostLives) VALUES (?,?,?,?,?,?);");
		sql.setString(1, p);
		sql.setBoolean(2, false);
		sql.setInt(3, 0);
		sql.setBoolean(4, false);
		sql.setInt(5, plugin.getConfig().getInt("lives"));
		sql.setInt(6, 0);
		sql.execute();
		sql.close();
	}

	public List<String> getPlayers() throws SQLException {
		List<String> l = new ArrayList<String>();
		String select = "SELECT username FROM players;";
		ResultSet res = stat.executeQuery(select);
		while(res.next()){
			l.add(res.getString("username"));
		}
		res.close();
		return l;
	}

	public List<String> getGhosts() throws SQLException {
		List<String> l = new ArrayList<String>();
		PreparedStatement sql = conn.prepareStatement("SELECT username FROM players WHERE isGhost=?;");
		sql.setBoolean(1, true);
		ResultSet res = sql.executeQuery();
		while(res.next()){
			l.add(res.getString("username"));
		}
		res.close();
		return l;
	}

	public List<String> getCachedGhosts() {
		return cachedGhosts;
	}

	public List<String> getGhosts_v1() throws SQLException {
		List<String> l = new ArrayList<String>();
		PreparedStatement sql = conn.prepareStatement("SELECT username FROM ghosts;");
		ResultSet res = sql.executeQuery();
		while(res.next()){
			l.add(res.getString("username"));
		}
		res.close();
		return l;
	}

	public int decrementTime(String p, int amt) throws SQLException {
		try {
			if(getTimeLeft(p) > 0){
				PreparedStatement sql = conn.prepareStatement("UPDATE players SET timeleft=? WHERE username=?;");
				sql.setInt(1, getTimeLeft(p)-amt > 0 ? getTimeLeft(p)-amt : 0);
				sql.setString(2, p);
				sql.executeUpdate();
				sql.close();
			} else {
				return 0;
			}
		} catch (SQLException e) {

		}
		return getTimeLeft(p);
	}


	public int getTimeLeft(String p) throws SQLException {
		PreparedStatement sql = conn.prepareStatement("SELECT * FROM players WHERE username=?;");
		sql.setString(1, p);
		ResultSet r = sql.executeQuery();
		while(r.next()){
			return r.getInt("timeleft");
		}
		sql.close();
		return -1;
	}

	public boolean hasEaten(String s) throws SQLException {
		PreparedStatement sql = conn.prepareStatement("SELECT * FROM players WHERE username=?;");
		sql.setString(1, s);
		ResultSet r = sql.executeQuery();
		while(r.next()){
			return r.getBoolean("hasEaten");
		}
		return false;
	}

	public void setHasEaten(String s) throws SQLException {
		PreparedStatement sql = conn.prepareStatement("UPDATE players SET hasEaten=? WHERE username=?;");
		sql.setBoolean(1, true);
		sql.setString(2, s);
		sql.executeUpdate();
		sql.close();
	}

	public boolean isGhost(Player p) {
		return cachedGhosts.contains(p.getName());
	}


	public void removeGhost(String s) throws SQLException {
		removeCached(s);
		String delete = "UPDATE players SET hasEaten=?, isGhost=? WHERE username=?;";
		PreparedStatement sql = conn.prepareStatement(delete);
		sql.setBoolean(1, false);
		sql.setBoolean(2, false);
		sql.setString(3, s);
		sql.executeUpdate();
		sql.close();
	}

	public int decrementLives(String s) throws SQLException{
		if(nLives(s) > 0){
			PreparedStatement sql = conn.prepareStatement("UPDATE players SET lives=? WHERE username=?;");
			sql.setInt(1, nLives(s)-1);
			sql.setString(2, s);
			sql.executeUpdate();
			sql.close();
		}
		return nLives(s);
	}
	
	public void increaseLives(String s) throws SQLException{
		if(nLives(s) < plugin.getConfig().getInt("lives")){
			PreparedStatement sql = conn.prepareStatement("UPDATE players SET lives=? WHERE username=?;");
			sql.setInt(1, nLives(s)+1);
			sql.setString(2, s);
			sql.executeUpdate();
			sql.close();
		}
	}

	public int nLives(String s) throws SQLException {
		PreparedStatement sql = conn.prepareStatement("SELECT * FROM players WHERE username=?;");
		sql.setString(1, s);
		ResultSet r = sql.executeQuery();
		while(r.next()){
			return r.getInt("lives");
		}
		sql.close();
		return -1;
	}

	public void resetLives(String s) throws SQLException{
		PreparedStatement sql = conn.prepareStatement("UPDATE players SET lives=? WHERE username=?;");
		sql.setInt(1, plugin.getConfig().getInt("lives"));
		sql.setString(2, s);
		sql.executeUpdate();
		sql.close();
	}

	public int getGhostTimesLeft(String s) throws SQLException {
		PreparedStatement sql = conn.prepareStatement("SELECT * FROM players WHERE username=?;");
		sql.setString(1, s);
		ResultSet r = sql.executeQuery();
		while(r.next()){
			return r.getInt("ghostLives");
		}
		sql.close();
		return 0;
	}

	public void resetGhostTimes(String s) throws SQLException {
		PreparedStatement sql = conn.prepareStatement("UPDATE players SET ghostLives=? WHERE username=?;");
		sql.setInt(1, 0);
		sql.setString(2, s);
		sql.executeUpdate();
		sql.close();
	}

	public int increaseGhostTimes(String s) throws SQLException {
		if(getGhostTimesLeft(s) <= plugin.getConfig().getInt("maxGhostTimes")){
			PreparedStatement sql = conn.prepareStatement("UPDATE players SET ghostLives=? WHERE username=?;");
			sql.setInt(1, getGhostTimesLeft(s)+1);
			sql.setString(2, s);
			sql.executeUpdate();
			sql.close();
		}
		return getGhostTimesLeft(s);
	}

	public void ban(String s) throws SQLException {
		String delete = "INSERT INTO banned (username, timeleft) VALUES (?,?);";
		PreparedStatement sql = conn.prepareStatement(delete);
		sql.setString(1, s.toLowerCase());
		sql.setInt(2, plugin.getConfig().getInt("banTime"));
		sql.executeUpdate();
		sql.close();
		if(isGhost(plugin.getPlayer(s)))
			removeGhost(s);
	}

	public boolean isBanned(String s) throws SQLException{
		return getBanned().contains(s.toLowerCase());
	}

	public int banTimeLeft(String s) throws SQLException {
		PreparedStatement sql = conn.prepareStatement("SELECT * FROM banned WHERE username=?;");
		sql.setString(1, s.toLowerCase());
		ResultSet r = sql.executeQuery();
		while(r.next()){
			return r.getInt("timeleft");
		}
		sql.close();
		return -1;
	}

	public void reduceBanTime(String s) throws SQLException {
		if(banTimeLeft(s) > 0){
			PreparedStatement sql = conn.prepareStatement("UPDATE banned SET timeleft=? WHERE username=?;");
			sql.setInt(1, banTimeLeft(s)-1);
			sql.setString(2, s.toLowerCase());
			sql.executeUpdate();
			sql.close();
		} else {
			removeBan(s);
		}
	}

	public void reduceBans() throws SQLException {
		for(final String s : getBanned())
			reduceBanTime(s);
	}

	public List<String> getBanned() throws SQLException{
		List<String> l = new ArrayList<String>();
		String select = "SELECT username FROM banned;";
		ResultSet res = stat.executeQuery(select);
		while(res.next()){
			l.add(res.getString("username"));
		}
		res.close();
		return l;
	}

	public void removeBan(String s) throws SQLException {
		String delete = "DELETE FROM banned WHERE username=?;";
		PreparedStatement sql = conn.prepareStatement(delete);
		sql.setString(1, s.toLowerCase());
		sql.executeUpdate();
		sql.close();
		resetPlayer(s);
	}

	public void resetPlayer(String s) throws SQLException{
		PreparedStatement sql = conn.prepareStatement("UPDATE players SET isGhost=?, timeleft=?, hasEaten=?, lives=?, ghostLives=? WHERE username=?;");
		sql.setBoolean(1, false);
		sql.setInt(2, 0);
		sql.setBoolean(3, false);
		sql.setInt(4, plugin.getConfig().getInt("lives"));
		sql.setInt(5, 0);
		sql.setString(6, s);
		sql.execute();
		sql.close();
		cachedGhosts.remove(s);
	}

	public void create_tablesConnect() throws SQLException {
        stat.executeUpdate("CREATE TABLE IF NOT EXISTS players (id integer PRIMARY KEY AUTOINCREMENT NOT NULL UNIQUE, username TEXT, isGhost boolean, timeleft integer, hasEaten boolean, lives integer, ghostLives integer);");
        stat.executeUpdate("CREATE TABLE IF NOT EXISTS banned (username TEXT, timeleft integer);");
    }

	public void updateDB() throws SQLException {
		int dbVer = plugin.getConfig().getInt("dbVersion");
		if(dbVer < 1){
			stat.executeUpdate("ALTER TABLE ghosts ADD COLUMN hasEaten boolean;");
			plugin.getConfig().set("dbVersion", 1);
			plugin.saveConfig();
			plugin.reloadConfig();
		}
		if(dbVer == 1 && VERSION == 2){
			plugin.log("updating database from version 1 to 2...");
			String[] ghosts = getGhosts_v1().toArray(new String[getGhosts_v1().size()]);
			for(int x = 0; x<ghosts.length; x++){
				addPlayer(ghosts[x]);
				addGhost(ghosts[x]);
			}
			plugin.getConfig().set("dbVersion", VERSION);
			plugin.saveConfig();
			plugin.reloadConfig();
			stat.executeUpdate("DROP TABLE ghosts;");
		}
		if(dbVer == 3 && VERSION == 2){
			plugin.log("downgrading database from version 3 to 2...");
			stat.executeUpdate("DROP TABLE IF EXISTS players;");
			create_tablesConnect();
			plugin.getConfig().set("dbVersion", VERSION);
			plugin.saveConfig();
			plugin.reloadConfig();
		}
	}
}
