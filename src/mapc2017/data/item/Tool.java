package mapc2017.data.item;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

public class Tool extends Item {
	
	private Set<String> roles = new HashSet<>();

	public Tool(String name, int volume) {
		super(name, volume, Collections.emptySet(), Collections.emptyMap());
	}

	public int getNumber() {
		return Integer.parseInt(getName().replace("tool", ""));
	}
	
	public void addRole(String role) {
		roles.add(role);
	}
	
	public Set<String> getRoles() {
		return new HashSet<>(roles);
	}

}
