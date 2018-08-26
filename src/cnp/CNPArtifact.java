package cnp;

import cartago.Artifact;
import cartago.GUARD;
import cartago.INTERNAL_OPERATION;
import cartago.LINK;
import cartago.OPERATION;
import cartago.OpFeedbackParam;

public class CNPArtifact extends Artifact {
	
	private Bid 		bestBid;
	private boolean 	isOpen;
	private String 		winner;
	
	/**
	 * 
	 * @param duration
	 */
	void init()
	{		
		execInternalOp("awaitBids", 500);
	}
	
	/**
	 * Awaits bids, closing the bidding after a given duration.
	 * @param duration - Duration to wait.
	 */
	@INTERNAL_OPERATION
	void awaitBids(long duration)
	{		
		this.isOpen = true;
		
		await_time(duration);
		
		this.isOpen = false;		
	}
	
	/**
	 * Accepts a bid if the bidding is open, setting the bid's ID
	 * as a feedback parameter. If the bid is better than previous
	 * bids, the bestBid is updated.
	 * @param bid - An agent's bid.
	 * @param id - ID of the bid.
	 */
	@OPERATION
	void bid(int bid)
	{		
		if (isOpen)
		{
			if (bestBid == null || bestBid.getBid() > bid)
			{
				bestBid = new Bid(getOpUserName(), bid);
			}
		}
	}
	
	/**
	 * Guard to signal when bidding is closed.
	 * @return True if bidding is closed, false otherwise.
	 */
	@GUARD
	boolean biddingClosed()
	{
		return !isOpen;
	}
	
	/**
	 * Sets the ID of the best bid as a feedback parameter when the
	 * bidding is closed.
	 * @param id - ID of the best bid.
	 */
	@OPERATION
	void winner(OpFeedbackParam<Boolean> won)
	{		
		await("biddingClosed");
		
		winner = getWinner();
		
		if (winner != null && winner.equals(getOpUserName()))
		{
			won.set(true);
		}
		else
		{
			won.set(false);
		}
	}
	
	/**
	 * Any agent is allowed to immediately take a task which has been 
	 * bid for, but not received any bids.
	 */
	@OPERATION
	void takeTask(OpFeedbackParam<Boolean> canTake)
	{
		await("biddingClosed");
		
		if (bestBid == null && winner == null)
		{
			winner = getOpUserName();
			
			canTake.set(true);
		}
		else
		{
			canTake.set(false);
		}
	}
	
	@LINK
	void disposeArtifact()
	{
		this.dispose();
	}
	
	private String getWinner()
	{
		if (bestBid != null)
		{
			return bestBid.agent;
		}
		return null;
		
	}
	
	static class Bid {
		
		private String 	agent;
		private int 	bid;
		
		public Bid(String agent, int bid) {
			this.agent 	= agent;
			this.bid	= bid;
		}
		
		public String 	getAgent() 	{ return this.agent; }
		public int 		getBid()	{ return this.bid; }
	}
}
