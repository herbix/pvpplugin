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
	
	private static final String[] color = {"", "��c", "��9", "��4", "��1"};
	
	public String getTeamColor() {
		return online ? color[team] : color[team+2];
	}

	public String getOldScoreString() {
		return oldScoreString;
	}
	
	public String newScoreString() {
		String part1 = getTeamColor();
		String part2 = playerName;
		String part3 = "(" + killCount + "/" + deathCount + ")";
		if(2 + part2.length() + part3.length() > 16) {
			part2 = part2.substring(0, 16 - 2 - part3.length() - 2) + "..";
		}
		oldScoreString = part1 + part2 + part3;
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

	public void reset() {
		killCount = 0;
		deathCount = 0;
		continuousKillCount = 0;
	}

	private static final String[] cks = {
		null,
		null,
		null,
		"��e��lkilling spree��r",
		"��e��ldominating��r",
		"��e��lmega kill��r",
		"��e��lunstoppable��r", 
		"��e��lwicked sick��r", 
		"��e��lmonster kill��r", 
		"��e��lgod like��r",
		"��e��lholy shit��r"
	};
	
	public String getContinuousKillString() {
		if(continuousKillCount > 10) {
			return cks[10];
		}
		return cks[continuousKillCount];
	}
}
