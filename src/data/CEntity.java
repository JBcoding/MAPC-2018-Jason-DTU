package data;

import java.util.HashSet;
import java.util.Set;

import info.AgentArtifact;
import info.FacilityArtifact;
import massim.protocol.messagecontent.Action;
import massim.scenario.city.ActionExecutor;
import massim.scenario.city.data.Entity;
import massim.scenario.city.data.Item;
import massim.scenario.city.data.Location;
import massim.scenario.city.data.Role;
import massim.scenario.city.data.Route;
import massim.scenario.city.data.facilities.Facility;
import massim.scenario.city.util.GraphHopperManager;

/**
 * The body of an agent in the City scenario.
 */
public class CEntity {

    private Role role;
    private Set<String> permissions;
    private double lat; 
    private double lon;
    private Route route;
    private int routeLength;
    private Facility facility;
    private AgentArtifact agentArtifact;

    private int currentSpeed;
    private int currentVision;
    private int currentSkill;

    private int currentCharge;
    private int currentBattery;
    private int currentCapacity;;
    private CBoundedItemBox items;

    private Action lastAction = Action.STD_NO_ACTION;
    private String lastActionResult = ActionExecutor.SUCCESSFUL;
    private Object[] lastActionParam;

    public CEntity(Role role, Location location){
        this.role = role;

        permissions = new HashSet<>();

        if (role.getName().equals("drone")) 	permissions.add(GraphHopperManager.PERMISSION_AIR);
        else 				        	permissions.add(GraphHopperManager.PERMISSION_ROAD);
        
        items = new CBoundedItemBox(role.getMaxLoad());
        this.lat = location.getLat();
        this.lon = location.getLon();
    }

    public Set<String> getPermissions()
    {
    	return this.permissions;
    }

    public int getCurrentSpeed() {
        return currentSpeed;
    }

    public void setCurrentSpeed(int currentSpeed) {
        this.currentSpeed = currentSpeed;
    }

    public int getCurrentVision() {
        return currentVision;
    }

    public void setCurrentVision(int currentVision) {
        this.currentVision = currentVision;
    }

    public int getCurrentSkill() {
        return currentSkill;
    }

    public void setCurrentSkill(int currentSkill) {
        this.currentSkill = currentSkill;
    }

    public int getCurrentCharge(){
        return currentCharge;
    }

    public void setCurrentCharge(int charge)
    {
        this.currentCharge = charge;
    }

    public int getCurrentBattery(){
        return currentBattery;
    }

    public void setCurrentBattery(int battery)
    {
        this.currentBattery = battery;
    }

    public int getCurrentCapacity(){
        return currentCapacity;
    }

    public void setCurrentCapacity(int capacity) {
        this.currentCapacity = capacity;
    }

    public int getCurrentLoad(){
        return items.getCurrentVolume();
    }

    public Role getRole(){
        return role;
    }

    public Location getLocation(){
        return new Location(this.lon, this.lat);
    }
    
    public void setLat(double lat) {
    	this.lat = lat;
    }
    
    public void setLon(double lon)
    {
    	this.lon = lon;
    }

    public void setLastActionResult(String lastActionResult){
        this.lastActionResult = lastActionResult;
    }

    /**
     * @return the current route or null if no route is followed
     */
    public Route getRoute(){
        return route;
    }

    /**
     * @param route the route this entity should follow now
     */
    public void setRoute(Route route){
        this.route = route;
    }
    
    /**
     * @param length of the current route
     */
    public void setRouteLength(int length)
    {
    	this.routeLength = length;
    }
    
    /**
     * @return The length of the current route
     */
    public int getRouteLength()
    {
    	return this.routeLength;
    }

    /**
     * Moves this entity along the route.
     * @param cost the energy cost of the goto action
     * @return true if successful
     */
    public boolean advanceRoute(int cost) {
        if (route == null) {
            return false;
        }
        if (currentCharge < cost){
            route = null;
            currentCharge = 0;
            return false;
        }
        currentCharge -= cost;
        Location newLoc = this.route.advance(getCurrentSpeed());
        if (newLoc != null) 
    	{
        	this.lat = newLoc.getLat();
        	this.lon = newLoc.getLon();
    	}
        if (route.isCompleted()) route = null;
        return true;
    }

    public void setLastAction(Action action) {
        lastAction = action;
    }

    /**
     * @param item the item type to check
     * @return the number of items of that type the entity has stashed away
     */
    public int getItemCount(Item item){
        return items.getItemCount(item);
    }

    /**
     * @return the free volume of this entity
     */
    public int getFreeSpace() {
        return items.getFreeSpace();
    }

    /**
     * Transfers items from this entity to another. Without regard for capacity problems.
     * Only transfers items if available.
     * @param receiverEntity the entity to receive the item
     * @param item item type to transfer
     * @param amount how many items to transfer
     * @return the number of items that were actually transferred
     */
    public int transferItems(Entity receiverEntity, Item item, int amount) {
        int removed = removeItem(item, amount);
        receiverEntity.addItem(item, removed);
        return removed;
    }


    /**
     * Removes a number of items from this entity, but not more than available.
     * @param item type to remove
     * @param remove number of items to remove
     * @return the number of items that were actually removed
     */
    public int removeItem(Item item, int remove){
        return items.remove(item, remove);
    }

    /**
     * Adds a number of items to the entity if capacity allows.
     * @param item the item's type
     * @param amount number of items to add
     * @return whether the items have been added
     */
    public boolean addItem(Item item, int amount){
        return items.store(item, amount);
    }

    /**
     * @return the result of the last action
     */
    public String getLastActionResult() {
        return lastActionResult;
    }

    /**
     * @return the last action executed for this agent
     */
    public Action getLastAction() {
        return lastAction;
    }

    /**
     * Charges the battery of this entity
     * @param rate the amount to charge
     */
    public void charge(int rate) {
        currentCharge = Math.min(currentCharge + rate, getCurrentBattery());
    }

    /**
     * Just removes the current route of the entity
     */
    public void clearRoute() {
        role = null;
    }

    /**
     * @return the mutable inventory of this agent. Proceed with caution.
     */
    public CItemBox getInventory(){
        return items;
    }

    /**
     * "Moves" this entity to a new location.
     * @param location the location to move the entity to
     */
    public void setLocation(Location location){
        this.lat = location.getLat();
        this.lon = location.getLon();
    }

    /**
     * Removes all items from this entity.
     */
    public void clearInventory() {
        items = new CBoundedItemBox(items.getCurrentVolume() + items.getFreeSpace());
    }

    /**
     * Completely drains the entity's battery.
     */
    public void discharge(){
        this.currentCharge = 0;
    }

	public void setFacility(Facility facility) {
		this.facility = facility;		
	}
	
	public boolean inFacility(String facilityName) {
		if (this.facility == null) return false;
		return this.facility.getName().equals(facilityName);
	}
	
	public String getFacilityName()
	{
		return facility == null ? "none" : facility.getName();
	}

	public void setLastActionParam(Object[] lastActionParam) 
	{
		this.lastActionParam = lastActionParam;
	}
	
	public Object[] getLastActionParam()
	{
		return this.lastActionParam;
	}


	public static int scoutCount;
    public void addAgentArtifact(AgentArtifact agentArtifact) {
        this.agentArtifact = agentArtifact;
        if (this.role.getName().equals("drone") && scoutCount == 0) {
            this.agentArtifact.setToScout();
            scoutCount ++;
            FacilityArtifact.calculateMissingResourceNodes();
        }
    }

}
