package info;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.logging.Logger;

import cartago.Artifact;
import cartago.OPERATION;
import cartago.OpFeedbackParam;
import eis.iilang.Percept;
import env.EIArtifact;
import env.Translator;

public class DynamicInfoArtifact extends Artifact {
	
	private static final Logger logger = Logger.getLogger(DynamicInfoArtifact.class.getName());

	private static final String DEADLINE			= "deadline";
	private static final String MONEY 				= "money";
	private static final String STEP 				= "step";
	private static final String TIMESTAMP 			= "timestamp";
	
	public static final Set<String>	PERCEPTS = Collections.unmodifiableSet(
		new HashSet<String>(Arrays.asList(DEADLINE, MONEY, STEP, TIMESTAMP)));

	private static long					deadline;
	private static int					money;
	private static int					step;
	private static long					timestamp;
	private static int					jobsCompleted;

	void init()
	{
		defineObsProperty("step", 0);
	}	
	
	@OPERATION
	void getDeadline(OpFeedbackParam<Long> ret)
	{
		ret.set(deadline);
	}
	
	@OPERATION
	void getMoney(OpFeedbackParam<Integer> ret)
	{
		ret.set(money);
	}
	
	@OPERATION
	void getStep(OpFeedbackParam<Integer> ret)
	{
		ret.set(step);
	}
	
	@OPERATION
	void getTimestamp(OpFeedbackParam<Long> ret)
	{
		ret.set(timestamp);
	}
	
	@OPERATION
	void incJobCompletedCount()
	{
		jobsCompleted++;
	}
	
	@OPERATION
	void getJobCompletedCount(OpFeedbackParam<Integer> ret)
	{
		ret.set(jobsCompleted);
	}
	
	public static int getJobsCompleted()
	{
		return jobsCompleted;
	}
	
	public static int getStep() 
	{
		return step;
	}
	
	public static int getMoney()
	{
		return money;
	}
	
	public static void perceiveUpdate(Collection<Percept> percepts)
	{		
		for (Percept percept : percepts)
		{
			switch (percept.getName())
			{
			case DEADLINE:   perceiveDeadline	(percept);	break;
			case MONEY:      perceiveMoney		(percept);  break;
			case STEP:       perceiveStep		(percept);  break;
			case TIMESTAMP:  perceiveTimestamp	(percept);  break;
			}
		}

		if (EIArtifact.LOGGING_ENABLED)
		{
			logger.info("Perceived dynamic info");
			logger.info("Perceived deadline:\t" + deadline);
			logger.info("Perceived money:\t" + money);
			logger.info("Perceived step:\t" + step);
			logger.info("Perceived timestamp:\t" + timestamp);
		}
	}
	
	// Literal(long)
	private static void perceiveDeadline(Percept percept)
	{
		Object[] args = Translator.perceptToObject(percept);
		
		deadline = (long) args[0];
	}

	// Literal(int)
	private static void perceiveMoney(Percept percept)
	{
		Object[] args = Translator.perceptToObject(percept);
		
		money = (int) args[0];
	}

	// Literal(int)
	public static void perceiveStep(Percept percept)
	{
		Object[] args = Translator.perceptToObject(percept);
		
		step = (int) args[0];
	}

	// Literal(long)
	private static void perceiveTimestamp(Percept percept)
	{
		Object[] args = Translator.perceptToObject(percept);
		
		timestamp = (long) args[0];
	}

	/**
	 * Resets the dynamic info artifact
	 */
	public static void reset() {
		deadline = 0;
		money = 0;
		timestamp = 0;
//		step = 0;
		jobsCompleted = 0;
	}
}
