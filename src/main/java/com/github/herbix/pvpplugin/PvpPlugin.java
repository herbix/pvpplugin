package com.github.herbix.pvpplugin;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Random;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Biome;
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
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.Recipe;
import org.bukkit.inventory.ShapedRecipe;
import org.bukkit.inventory.ShapelessRecipe;
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
	private Location redStart;
	private Location blueStart;
	private List<Recipe> pvpAddRecipes = new ArrayList<Recipe>();

	@Override
	public void onEnable() {
		getServer().getPluginManager().registerEvents(this, this);
		initScoreboard();
		initRecipes();
		getServer().getScheduler().scheduleSyncRepeatingTask(this, this, 0, 20);
		getServer().getWorld("world").setGameRuleValue("keepInventory", "true");
	}

	private void initRecipes() {
		Recipe recipe = new ShapedRecipe(new ItemStack(Material.GHAST_TEAR))
				.shape(" # ", "$$$")
				.setIngredient('#', Material.GOLD_INGOT)
				.setIngredient('$', Material.WATER_BUCKET);
		getServer().addRecipe(recipe);
		pvpAddRecipes.add(recipe);
		
		recipe = new ShapedRecipe(new ItemStack(Material.BLAZE_ROD))
				.shape("%%%", "%#%", "$$$")
				.setIngredient('#', Material.TORCH)
				.setIngredient('$', Material.GOLD_NUGGET)
				.setIngredient('%', Material.ROTTEN_FLESH);
		getServer().addRecipe(recipe);
		pvpAddRecipes.add(recipe);
		
		recipe = new ShapelessRecipe(new ItemStack(Material.GLOWSTONE_DUST))
				.addIngredient(1, Material.SUGAR)
				.addIngredient(1, Material.BLAZE_POWDER);
		getServer().addRecipe(recipe);
		pvpAddRecipes.add(recipe);
		
		recipe = new ShapelessRecipe(new ItemStack(Material.NETHER_WARTS))
				.addIngredient(1, Material.SEEDS)
				.addIngredient(1, Material.REDSTONE);
		getServer().addRecipe(recipe);
		pvpAddRecipes.add(recipe);
	}

	private void initScoreboard() {
		ScoreboardManager sm = getServer().getScoreboardManager();
		sb = sm.getNewScoreboard();

		Team red = sb.getTeam("pvp-red");
		Team blue = sb.getTeam("pvp-blue");
		if(red == null) {
			red = sb.registerNewTeam("pvp-red");
		}
		if(blue == null) {
			blue = sb.registerNewTeam("pvp-blue");
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

		states.setDisplayName("¡ìa¡ìlHealth");
		states.setDisplaySlot(DisplaySlot.SIDEBAR);
	}

	@Override
	public void onDisable() {
		HandlerList.unregisterAll((JavaPlugin) this);

		sb.getTeam("pvp-red").unregister();
		sb.getTeam("pvp-blue").unregister();
		sb.getObjective("States").unregister();

		Player[] players = getServer().getOnlinePlayers();
		Scoreboard mb = getServer().getScoreboardManager().getMainScoreboard();
		for(Player player : players) {
			player.setScoreboard(mb);
			player.setCompassTarget(player.getWorld().getSpawnLocation());
		}

		Iterator<Recipe> it = getServer().recipeIterator();
		while(it.hasNext()) {
			if(pvpAddRecipes.contains(it.next())) {
				it.remove();
			}
		}
		
		getServer().getScheduler().cancelTasks(this);
	}
	
	@Override
	public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
		if(cmd.getName().equalsIgnoreCase("red") || cmd.getName().equalsIgnoreCase("blue")) {
			if(sender instanceof Player) {
				if(gameStart) {
					sender.sendMessage("Cannot change team during the game.");
					return true;
				}
				
				boolean isRed = cmd.getName().equalsIgnoreCase("red");
				Player player = (Player) sender;
				
				sb.getTeam("pvp-" + cmd.getName().toLowerCase()).addPlayer(player);
				
				playerStates.get(player.getUniqueId()).team = isRed ? 1 : 2;

				sendToAll("¡ìe" + player.getName() + "¡ìr is changed to team " + (isRed ? "¡ìcred" : "¡ì9blue") + ".");
				
				return true;
			} else {
				sender.sendMessage("This command can only be run by a player.");
				return true;
			}
		} else if(cmd.getName().equalsIgnoreCase("pvpstart")) {
			if(gameStart) {
				sender.sendMessage("Game has started.");
				return true;
			}
			
			for(Entry<UUID, State> entry : playerStates.entrySet()) {
				State state = entry.getValue();
				if(state.team == 0) {
					sender.sendMessage("There's one who didn't choose team.");
					return true;
				}
			}
			
			World world = Bukkit.getWorld("world");
			
			Random rand = new Random();
			int randx, randz, i;
			randx = rand.nextInt(1009) - 512;
			randz = rand.nextInt(1009) - 512;
			i = 0;
			while(i < 10 && (world.getBiome(randx, randz) == Biome.OCEAN ||
					world.getBiome(randx, randz) == Biome.DEEP_OCEAN)) {
				randx = rand.nextInt(1009) - 512;
				randz = rand.nextInt(1009) - 512;
				i++;
			}
			redStart = findHighestLocation(world, randx, randz, 16, 16);
			world.loadChunk(redStart.getBlockX()/16, redStart.getBlockZ()/16);
			
			randx = rand.nextInt(1009) - 512;
			randz = rand.nextInt(1009) - 512;
			i = 0;
			while(i < 10 && (world.getBiome(randx, randz) == Biome.OCEAN ||
					world.getBiome(randx, randz) == Biome.DEEP_OCEAN)) {
				randx = rand.nextInt(1009) - 512;
				randz = rand.nextInt(1009) - 512;
				i++;
			}
			blueStart = findHighestLocation(world, randx, randz, 16, 16);
			world.loadChunk(blueStart.getBlockX()/16, blueStart.getBlockZ()/16);

			for(Entry<UUID, State> entry : playerStates.entrySet()) {
				Player player = getServer().getPlayer(entry.getKey());
				State state = entry.getValue();
				if(state.team == 1) {
					player.teleport(redStart);
					player.setBedSpawnLocation(redStart, true);
				}
				if(state.team == 2) {
					player.teleport(blueStart);
					player.setBedSpawnLocation(blueStart, true);
				}
				player.setExp(0);
				player.getInventory().clear();
				state.reset();
			}
			
			world.setTime(0);
			gameStart = true;
			sendToAll("Game start. Have fun!");

			return true;
			
		} else if(cmd.getName().equalsIgnoreCase("pvpstop")) {
			if(!gameStart) {
				sendToAll("Game has not started.");
				return true;
			}

			gameStart = false;
			sender.sendMessage("Game stop.");
			return true;
		}
		return false;
	}

	private Location findHighestLocation(World world, int sx, int sz, int w, int h) {
		int hx = sx, hy = -1, hz = sz;
		for(int i=sx; i<sx+w; i++) {
			for(int j=sz; j<sz+h; j++) {
				int hm = world.getHighestBlockYAt(i, j);
				if(hm > hy) {
					hy = hm;
					hx = i;
					hz = j;
				}
			}
		}
		return new Location(world, hx, hy, hz);
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
			state.online = true;
		}
		event.getPlayer().setScoreboard(sb);
	}

	@EventHandler
	public void onDeath(PlayerDeathEvent event) {
		if(!gameStart) {
			return;
		}
			
		Player dead = event.getEntity();
		State deadstate = playerStates.get(dead.getUniqueId());

		Player killer = dead.getKiller();
		if(killer != null) {
			State killerstate = playerStates.get(killer.getUniqueId());
			killerstate.kill();
			if(deadstate.getContinuousKillString() != null) {
				sendToAll(killer.getPlayerListName() + " finished " + dead.getPlayerListName() + "'s " + deadstate.getContinuousKillString() + "!");
			}
			if(killerstate.getContinuousKillString() != null) {
				sendToAll(killer.getPlayerListName() + " is " + killerstate.getContinuousKillString() + "!");
			}
			deadstate.death(true);
		} else {
			deadstate.death(false);
		}

		State state = playerStates.get(dead.getUniqueId());
		if(state.team == 1) {
			dead.setBedSpawnLocation(redStart, true);
		}
		if(state.team == 2) {
			dead.setBedSpawnLocation(blueStart, true);
		}
	}

	@EventHandler
	public void onQuit(PlayerQuitEvent event) {
		if(!gameStart) {
			State state = playerStates.remove(event.getPlayer().getUniqueId());
			sb.resetScores(state.getOldScoreString());
		} else {
			State state = playerStates.get(event.getPlayer().getUniqueId());
			state.online = false;
		}
	}

	private void sendToAll(String string) {
		for(Entry<UUID, State> entry : playerStates.entrySet()) {
			UUID playerId = entry.getKey();
			Player player = getServer().getPlayer(playerId);
			
			if(player != null) {
				player.sendMessage(string);
			}
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

				checkPlayerLocation(player);
				setPlayerCompass(player, state);
				
				health = (int)player.getHealth();
			}

			sb.resetScores(state.getOldScoreString());
			states.getScore(state.newScoreString()).setScore(health);
		}

		states.setDisplaySlot(DisplaySlot.SIDEBAR);
	}

	private void setPlayerCompass(Player player, State state) {
		Player[] players = getServer().getOnlinePlayers();
		Location locPlayer = player.getLocation();
		Location locTarget = null;
		double distance = Double.MAX_VALUE;
		
		for(Player target : players) {
			State s = playerStates.get(target.getUniqueId());
			if(s != null) {
				if(s.team != state.team) {
					Location l = target.getLocation();
					double d = l.distance(locPlayer);
					if(d < distance) {
						distance = d;
						locTarget = l;
					}
				}
			}
		}
		
		if(locTarget != null) {
			player.setCompassTarget(locTarget);
		} else {
			player.setCompassTarget(player.getWorld().getSpawnLocation());
		}
	}

	private void checkPlayerLocation(Player player) {
		Location loc = player.getLocation();
		if(loc.getX() < -512) loc.setX(-512);
		if(loc.getZ() < -512) loc.setZ(-512);
		if(loc.getX() > 512) loc.setX(512);
		if(loc.getZ() > 512) loc.setZ(512);
		if(!loc.equals(player.getLocation())) {
			player.teleport(loc);
		}
	}

	public void i(String msg) {
		getLogger().info(msg);
	}

}
