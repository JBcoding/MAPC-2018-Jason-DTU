package mapc2017.data.facility;

public class ResourceNode extends Facility {

	private String resource;
	
	public ResourceNode(Facility facility, String resource) {
		super(facility);
		this.resource = resource;
	}
	
	public String getResource() {
		return resource;
	}

}
