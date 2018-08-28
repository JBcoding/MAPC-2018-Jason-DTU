package mapc2017.data;

import massim.scenario.city.data.Location;

public class Entity {
	
	private String 					name,
									team,
									role;
	private double 					lat, 
									lon;
	
	public Entity(String name, String team, double lat, double lon, String role) {
		this.name 	= name;
		this.team	= team;
		this.lat	= lat;
		this.lon	= lon;
		this.role	= role;
	}
	
	// SETTERS	
	
	public void setLat(double lat) {
		this.lat = lat;
	}
	
	public void setLon(double lon) {
		this.lon = lon;
	}
	
	// GETTERS
	
	public String getName() {
		return name;
	}
	
	public String getTeam() {
		return team;
	}
	
	public String getRole() {
		return role;
	}
	
	public Location getLocation() {
		return new Location(lat, lon);
	}
	
	@Override
	public String toString() {
		return name;
	}
}
