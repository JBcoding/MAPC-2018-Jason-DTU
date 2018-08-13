package mapc2017.env.info;

public class DynamicInfo {
	
	private static DynamicInfo instance;	
	public  static DynamicInfo get() { return instance; }
	
	private long	money,
					timestamp,
					deadline;
	private int		step;
	
	public DynamicInfo() {
		instance = this;
	}
	
	/////////////
	// GETTERS //
	/////////////

	public long getTimestamp() {
		return timestamp;
	}

	public long getDeadline() {
		return deadline;
	}

	public int getStep() {
		return step;
	}

	public long getMoney() {
		return money;
	}
	
	/////////////
	// SETTERS //
	/////////////

	public void setTimestamp(long timestamp) {
		this.timestamp = timestamp;
	}

	public void setDeadline(long deadline) {
		this.deadline = deadline;
	}

	public void setStep(int step) {
		this.step = step;
	}

	public void setMoney(long money) {
		this.money = money;
	}
	
	/////////////
	// METHODS //
	/////////////
	
	public static boolean isDeadlinePassed() {
		return System.currentTimeMillis() > instance.getDeadline();
	}
	
	public static boolean isLastStep() {
		return instance.getStep() == StaticInfo.get().getSteps() - 1;
	}

}
