package mapc2017.data.facility;

public class ChargingStation extends Facility {
	
	int rate, blackout;
	
	public ChargingStation(Facility facility, int rate) {
		super(facility);
		this.rate = rate;
	}
	
	public boolean isActive() {
		return blackout == 0;
	}
	
	public void step() {
		if (blackout > 0) blackout--;
	}
	
	public void blackout() {
		blackout = 6;
	}

}
