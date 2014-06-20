package com.github.herbix.pvpplugin;

import java.awt.image.BufferedImage;
import java.util.Map;
import java.util.Map.Entry;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.map.MapCanvas;
import org.bukkit.map.MapCursor.Type;
import org.bukkit.map.MapCursorCollection;
import org.bukkit.map.MapRenderer;
import org.bukkit.map.MapView;

import static org.bukkit.map.MapCursor.Type.RED_POINTER;
import static org.bukkit.map.MapCursor.Type.BLUE_POINTER;
import static org.bukkit.map.MapCursor.Type.WHITE_POINTER;

public class PvpMapRenderer extends MapRenderer {

	private Map<UUID, State> playerStates;
	private BufferedImage image = new BufferedImage(128, 128, BufferedImage.TYPE_INT_ARGB);

	public PvpMapRenderer(PvpPlugin plugin) {
		playerStates = plugin.getPlayerStates();
	}

	@SuppressWarnings("deprecation")
	@Override
	public void render(MapView map, MapCanvas canvas, Player player) {
		State s = playerStates.get(player.getUniqueId());
		if(s == null) {
			return;
		}

		World world = player.getWorld();
		Location playerLoc = player.getLocation();
		playerLoc.setY(0);
		
		if(world.getTime() % 30 == player.getEntityId() % 30) {
			for(int i=0; i<127; i++) {
				for(int j=0; j<127; j++) {
					int x = (i-64)*8;
					int z = (j-64)*8;
					
					if(new Location(world, x, 0, z).distance(playerLoc) > 64) {
						continue;
					}
					
					if(!world.isChunkLoaded((i-64)/2, (j-64)/2)) {
						continue;
					}
					
					Material m = getHighestBlockTypeAt(world, x, z);
					if(m == Material.STATIONARY_WATER || m == Material.WATER) {
						image.setRGB(i, j, 0xFF8080FF);
					} else {
						image.setRGB(i, j, 0xFF008000);
					}
				}
			}
		}

		canvas.drawImage(0, 0, image);
		
		MapCursorCollection cursors = new MapCursorCollection();
		
		for(Entry<UUID, State> entry : playerStates.entrySet()) {
			Player target = Bukkit.getPlayer(entry.getKey());
			State targetstate = entry.getValue();
			if(target == null || (targetstate.team == 0 && target != player)) {
				continue;
			}
			Location l = target.getLocation();
			double yaw = l.getYaw();
			if(yaw < 0) {
				yaw += 360 * (-(int)(yaw / 360) + 1);
			}
			byte angle = (byte)((yaw / 180 * 8 + 0.5) % 16);
			Type type = target != player ? targetstate.team == 1 ? RED_POINTER : BLUE_POINTER : WHITE_POINTER;
			cursors.addCursor(l.getBlockX()/4, l.getBlockZ()/4, angle, type.getValue());
		}
		
		canvas.setCursors(cursors);
		
	}

	private Material getHighestBlockTypeAt(World w, int x, int z) {
		return w.getBlockAt(x, 62, z).getType();
	}

}
