package mapc2017.data.facility;

import massim.scenario.city.data.Location;

public class Facility {
	
	private String 	name;
	private double 	lat, 
					lon;
	
	public Facility(String name, double lat, double lon) {
		this.name 	= name;
		this.lat	= lat;
		this.lon	= lon;
	}
	
	public Facility(Facility facility) {
		this(facility.name, facility.lat, facility.lon);
	}
	
	public String getName() {
		return this.name;
	}
	
	public int getNumber() {
		return Integer.parseInt(getName().replaceFirst("[a-zA-Z]+", ""));
	}
	
	public Location getLocation() {
		return new Location(lon, lat);
	}
	
	@Override
	public String toString() {
		return name;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((name == null) ? 0 : name.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		Facility other = (Facility) obj;
		if (name == null) {
			if (other.name != null)
				return false;
		} else if (!name.equals(other.name))
			return false;
		return true;
	}
}
