package com.github.herbix.pvpplugin;

public class State {

	public String playerName;

	public int team;
	public int killCount;
	public int deathCount;
	public boolean online = true;
	
	public int oldTeam;
	public int oldKillCount;
	public int oldDeathCount;
	public boolean oldOnline = true;
	
	public State(String player) {
		this.playerName = player;
	}
	
	private static final String[] color = {"", "¡ìc", "¡ì9", "¡ì8"};
	
	public String getOldTeamColor() {
		return oldOnline ? color[oldTeam] : color[3];
	}
	
	public String getTeamColor() {
		return online ? color[team] : color[3];
	}
}
