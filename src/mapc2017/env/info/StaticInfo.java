package mapc2017.env.info;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import mapc2017.data.Entity;
import mapc2017.data.Role;
import mapc2017.data.RouteFinder;
import massim.scenario.city.data.Location;
import massim.scenario.city.data.Route;
import massim.scenario.city.util.GraphHopperManager;

public class StaticInfo {
	
	private static StaticInfo instance;
	public  static StaticInfo get() { return instance; }
	
	private String 				id, 
								map, 
								team;
	private int					steps, proximity;
	private long				seedCapital;
	private double				minLat, maxLat, minLon, maxLon,
								centerLat, centerLon, cellSize;
	private Map<String, Entity>	entities 	= new HashMap<>();
	private Map<String, Role>	roles 		= new HashMap<>();
	private RouteFinder			routeFinder;
	
	public StaticInfo() {
		instance = this;
	}
	
	/////////////
	// GETTERS //
	/////////////
	
	public String getId() {
		return id;
	}
	
	public String getMap() {
		return map;
	}
	
	public String getTeam() {
		return team;
	}
	
	public long getSeedCapital() {
		return seedCapital;
	}
	
	public int getSteps() {
		return steps;
	}
	
	public Location getCenter() {
		return new Location(centerLon, centerLat);
	}
	
	public Set<Entity> getTeamEntities() {
		return entities.values().stream()
				.filter(e -> e.getTeam().equals(team))
				.collect(Collectors.toSet());
	}
	
	public Role getRole(String role) {
		return roles.get(role);
	}
	
	public Collection<Role> getRoles() {
		return roles.values();
	}
	
	public int getRouteLength(Location from, Location to) {
		Route r = routeFinder.findRoute(from, to, GraphHopperManager.PERMISSION_ROAD);
		return r != null ? r.getRouteLength() : 10000;
	}
	
	public int getRouteDuration(AgentInfo agent, Location to) {
		Route r = routeFinder.findRoute(agent.getLocation(), to, agent.getPermission());
		return r != null ? r.getRouteDuration(agent.getRole().getSpeed()) : 10000;
	}
	
	public Location getRandomCenterLocation() {
		return routeFinder.getRandomCenterLocation();
	}
	
	public Location getRandomLocation() {
		return routeFinder.getRandomLocation();
	}
	
	/////////////
	// SETTERS //
	/////////////
	
	public void setId(String id) {
		this.id = id;
	}
	
	public void setMap(String map) {
		this.map = map;
	}
	
	public void setMinLat(double minLat) {
		this.minLat = minLat;
	}

	public void setMaxLat(double maxLat) {
		this.maxLat = maxLat;
	}

	public void setMinLon(double minLon) {
		this.minLon = minLon;
	}

	public void setMaxLon(double maxLon) {
		this.maxLon = maxLon;
	}

	public void setCenterLat(double centerLat) {
		this.centerLat = centerLat;
	}

	public void setCenterLon(double centerLon) {
		this.centerLon = centerLon;
	}

	public void setTeam(String team) {
		this.team = team;
	}
	
	public void setSeedCapital(long seedCapital) {
		this.seedCapital = seedCapital;
	}

	public void setCellSize(double cellSize) {
		this.cellSize = cellSize;
	}
	
	public void setSteps(int steps) {
		this.steps = steps;
	}

	public void setProximity(int proximity) {
		this.proximity = proximity;
	}
	
	public void addEntity(Entity entity) {
		this.entities.put(entity.getName(), entity);
	}
	
	public void addRole(Role role) {
		this.roles.put(role.getName(), role);
	}
	
	public void initCityMap() {
		this.routeFinder = new RouteFinder(map, cellSize, 
				minLat, maxLat, minLon, maxLon, 
				this.getCenter(), proximity);
	}
}
