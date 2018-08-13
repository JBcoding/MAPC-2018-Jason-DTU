package mapc2017.data.job;

import mapc2017.env.info.DynamicInfo;

public class AuctionJob extends Job {
	
	private int fine, 
				bid, 
				steps;
	private boolean isHighestBidder;

	public AuctionJob(Job job, int fine, int bid, int steps) {
		super(job);
		this.fine	= fine;
		this.bid	= bid;
		this.steps	= steps;
	}
	
	public AuctionJob(AuctionJob job) {
		this(job, job.fine, job.bid, job.steps);
	}

	public int getFine() {
		return fine;
	}

	public int getBid() {
		return bid;
	}
	
	public void setBid(int bid) {
		this.bid = bid;
	}

	public int getSteps() {
		return steps;
	}
	
	public boolean hasWon() {
		return this.getStart() + this.getSteps() < DynamicInfo.get().getStep();
	}
	
	public boolean isLastStep() {
		return this.getStart() + this.getSteps() == DynamicInfo.get().getStep();
	}

	public void setIsHighestBidder(boolean state) 
	{
		isHighestBidder = state;
	}
	
	public boolean isHighestBidder()
	{
		return this.isHighestBidder;
	}
	
	public void update(AuctionJob other)
	{
		if (other.getBid() < this.getBid())
		{
			this.bid = other.getBid();
			this.isHighestBidder = false;
		}
		else if (this.getBid() != 0)
		{
			this.isHighestBidder = true;
		}
	}
}
