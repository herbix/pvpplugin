package com.github.herbix.pvpplugin;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.UUID;

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerLoginEvent;
import org.bukkit.event.player.PlayerLoginEvent.Result;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scoreboard.DisplaySlot;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.ScoreboardManager;
import org.bukkit.scoreboard.Team;

public class PvpPlugin extends JavaPlugin implements Listener, Runnable {
	
	private Map<UUID, State> playerStates = new HashMap<UUID, State>();
	private Scoreboard sb;
	private boolean gameStart = false;

	@Override
	public void onEnable() {
		getServer().getPluginManager().registerEvents(this, this);

		ScoreboardManager sm = getServer().getScoreboardManager();
		Scoreboard mb = sm.getMainScoreboard();
		sb = sm.getNewScoreboard();

		Team red = mb.getTeam("pvp-red");
		Team blue = mb.getTeam("pvp-blue");
		if(red == null) {
			red = mb.registerNewTeam("pvp-red");
		}
		if(blue == null) {
			blue = mb.registerNewTeam("pvp-blue");
		}

		red.setAllowFriendlyFire(false);
		red.setCanSeeFriendlyInvisibles(true);
		red.setPrefix("¡ìc");
		red.setDisplayName("Red Team");

		blue.setAllowFriendlyFire(false);
		blue.setCanSeeFriendlyInvisibles(true);
		blue.setPrefix("¡ì9");
		blue.setDisplayName("Blue Team");

		Player[] players = getServer().getOnlinePlayers();
		for(Player player : players) {
			player.setScoreboard(sb);
		}
		
		Objective states = sb.getObjective("States");
		if(states == null) {
			states = sb.registerNewObjective("States", "dummy");
		}

		states.setDisplayName("¡ìc¡ìlHealth");
		states.setDisplaySlot(DisplaySlot.SIDEBAR);

		getServer().getScheduler().scheduleSyncRepeatingTask(this, this, 0, 20);
	}

	@Override
	public void onDisable() {
		HandlerList.unregisterAll((JavaPlugin) this);

		ScoreboardManager sm = getServer().getScoreboardManager();
		Scoreboard mb = sm.getMainScoreboard();

		mb.getTeam("pvp-red").unregister();
		mb.getTeam("pvp-blue").unregister();
		sb.getObjective("States").unregister();

		Player[] players = getServer().getOnlinePlayers();
		for(Player player : players) {
			player.setScoreboard(mb);
		}
		
		getServer().getScheduler().cancelTasks(this);
	}
	
	@Override
	public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
		if(sender instanceof Player) {
			
		}
		return false;
	}

	@EventHandler
	public void onLogin(PlayerLoginEvent event) {
		if(!gameStart) {
			return;
		}
		UUID playerId = event.getPlayer().getUniqueId();
		if(!playerStates.containsKey(playerId)) {
			event.disallow(Result.KICK_OTHER, "You didn't join the game.");
		}
	}
	
	@EventHandler
	public void onJoin(PlayerJoinEvent event) {
		if(!gameStart) {
			UUID playerId = event.getPlayer().getUniqueId();
			if(!playerStates.containsKey(playerId)) {
				playerStates.put(playerId, new State(event.getPlayer().getName()));
			}
		} else {
			State state = playerStates.get(event.getPlayer().getUniqueId());
			state.oldOnline = state.online;
			state.online = true;
		}
		event.getPlayer().setScoreboard(sb);
	}

	@EventHandler
	public void onDeath(PlayerDeathEvent event) {
		Player dead = event.getEntity();
		State s = playerStates.get(dead.getUniqueId());
		s.oldDeathCount = s.deathCount;
		s.deathCount++;

		Player killer = dead.getKiller();
		if(killer != null) {
			s = playerStates.get(killer.getUniqueId());
			s.oldKillCount = s.killCount;
			s.killCount++;
		}
	}
	
	@EventHandler
	public void onQuit(PlayerQuitEvent event) {
		if(!gameStart) {
			State state = playerStates.remove(event.getPlayer().getUniqueId());
			sb.resetScores(state.getTeamColor() + state.playerName + "(" + state.killCount + "/" + state.deathCount + ")");
		} else {
			State state = playerStates.get(event.getPlayer().getUniqueId());
			state.oldOnline = state.online;
			state.online = false;
		}
	}

	@Override
	public void run() {
		Objective states = sb.getObjective("States");

		sb.clearSlot(DisplaySlot.SIDEBAR);

		for(Entry<UUID, State> entry : playerStates.entrySet()) {
			UUID playerId = entry.getKey();
			State state = entry.getValue();
			Player player = getServer().getPlayer(playerId);
			int health = 0;
			
			if(player != null) {
				if(!gameStart && !player.isDead()) {
					player.setHealth(20);
				}
				health = (int)player.getHealth();
			}

			sb.resetScores(state.getOldTeamColor() + state.playerName + "(" + state.oldKillCount + "/" + state.oldDeathCount + ")");
			states.getScore(state.getTeamColor() + state.playerName + "(" + state.killCount + "/" + state.deathCount + ")").setScore(health);
		}

		states.setDisplaySlot(DisplaySlot.SIDEBAR);
	}

	public void i(String msg) {
		getLogger().info(msg);
	}

}
