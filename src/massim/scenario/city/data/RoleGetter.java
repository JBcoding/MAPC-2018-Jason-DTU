package massim.scenario.city.data;

import massim.protocol.scenario.city.data.RoleData;

import java.util.Set;

public class RoleGetter {
    public static Role getRole(RoleData roleData, Set<String> permissions) {
        return new Role(roleData, permissions);
    }
}
