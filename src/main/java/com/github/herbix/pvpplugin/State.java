package com.github.herbix.pvpplugin;

public class State {

	public String playerName;

	public int team;
	public boolean online = true;

	private int killCount;
	private int deathCount;
	private int continuousKillCount = 0;

	private String oldScoreString = "";
	
	public State(String player) {
		this.playerName = player;
	}
	
	private static final String[] color = {"", "¡ìc", "¡ì9", "¡ì4", "¡ì1"};
	
	public String getTeamColor() {
		return online ? color[team] : color[team+2];
	}

	public String getOldScoreString() {
		return oldScoreString;
	}
	
	public String newScoreString() {
		oldScoreString = getTeamColor() + playerName + "¡ìr(" + killCount + "/" + deathCount + ")";
		return oldScoreString;
	}
	
	public void kill() {
		killCount++;
		continuousKillCount++;
	}
	
	public void death(boolean byPlayer) {
		deathCount++;
		if(byPlayer) {
			continuousKillCount = 0;
		}
	}
}
