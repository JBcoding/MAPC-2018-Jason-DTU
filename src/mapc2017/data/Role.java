package mapc2017.data;

import java.util.HashSet;
import java.util.Set;

public class Role {
	
	private String 		name;
	private int 		speed, 
						load, 
						battery;
	private Set<String> tools;
	
	public Role(String name, int speed, int load, int battery, Set<String> tools) {
		this.name		= name;
		this.speed		= speed;
		this.load		= load;
		this.battery	= battery;
		this.tools		= tools;
	}

	public String getName() {
		return name;
	}

	public int getSpeed() {
		return speed;
	}

	public int getLoad() {
		return load;
	}

	public int getBattery() {
		return battery;
	}

	public Set<String> getTools() {
		return new HashSet<>(tools);
	}
	
	public boolean canUseTool(String tool) {
		return tools.contains(tool);
	}
	
	public Object[] getData()
	{
		return new Object[] {
				name,
				speed,
				load,
				battery,
				tools.toArray()
		};
	}
	
	@Override
	public String toString() {
		return name;
	}

}
