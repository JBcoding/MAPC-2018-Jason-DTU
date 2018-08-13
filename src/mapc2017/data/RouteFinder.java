package mapc2017.data;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;

import massim.scenario.city.CityMap;
import massim.scenario.city.data.Location;
import massim.scenario.city.data.Route;

public class RouteFinder extends CityMap {
	
	private static final long serialVersionUID = 1L;
	
	private final double minCenterLat, maxCenterLat,
						 minCenterLon, maxCenterLon;

	public RouteFinder(String mapName, double cellSize, 
			double minLat, double maxLat, double minLon, double maxLon,
			Location center, int proximity) 
	{
		super(mapName, (int) cellSize, minLat, maxLat, minLon, maxLon, center);
		
		Location.setProximity(proximity);
		
		minCenterLat = (minLat + center.getLat()) / 2.0;
		maxCenterLat = (maxLat + center.getLat()) / 2.0;
		minCenterLon = (minLon + center.getLon()) / 2.0;
		maxCenterLon = (maxLon + center.getLon()) / 2.0;
	}
	
	public Route findRoute(Location from, Location to, String permission) {
		return super.findRoute(from, to, new HashSet<>(Arrays.asList(permission)));
	}
	
	public Location getRandomCenterLocation() {
		return super.getRandomLocationInBounds(
				Collections.emptySet(), 10, 
				minCenterLat, maxCenterLat, 
				minCenterLon, maxCenterLon);
	}
	
	public Location getRandomLocation() {
		return super.getRandomLocation(Collections.emptySet(), 10);
	}
}
