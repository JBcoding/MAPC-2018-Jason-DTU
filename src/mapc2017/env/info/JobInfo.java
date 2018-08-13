package mapc2017.env.info;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import mapc2017.data.job.AuctionJob;
import mapc2017.data.job.Job;
import mapc2017.data.job.MissionJob;
import mapc2017.data.job.PostedJob;
import mapc2017.data.job.SimpleJob;

public class JobInfo {
	
	private static JobInfo instance;
	public  static JobInfo get() { return instance; }

	private Map<String, AuctionJob> auctionJobs 	= new HashMap<>();
	private Map<String, SimpleJob> 	simpleJobs 		= new HashMap<>();
	private Map<String, MissionJob> missionJobs 	= new HashMap<>();
	private Map<String, PostedJob> 	postedJobs 		= new HashMap<>();	
	private Set<Job> 				removedJobs 	= new HashSet<>(),
									newJobs			= new HashSet<>();
	
	public JobInfo() {
		instance = this;
	}
	
	/////////////
	// GETTERS //
	/////////////
	
	public Job getJob(String jobId) {
			 if (simpleJobs .containsKey(jobId))	return simpleJobs .get(jobId);
		else if (missionJobs.containsKey(jobId))	return missionJobs.get(jobId);
		else if (auctionJobs.containsKey(jobId))	return auctionJobs.get(jobId);
		else										return postedJobs .get(jobId);	
	}
	
	public Set<Job> getNewJobs() {
		Set<Job> jobs = new HashSet<>(newJobs);		
		newJobs.clear();		
		return jobs;
	}
	
	/////////////
	// SETTERS //
	/////////////

	public void addJob(Job job) {
		if (job instanceof PostedJob) return;
		
		Job existing = getJob(job.getId());		
		
		if (existing == null) 
		{
			newJobs.add(job);
			this.putJob(job);
			return;
		}
		else if (job instanceof AuctionJob) 
		{
			((AuctionJob) existing).update((AuctionJob) job);
		}
		removedJobs.remove(existing);
	}
	
	public void putJob(Job job) {
			 if (job instanceof SimpleJob ) simpleJobs .put(job.getId(), (SimpleJob ) job);
		else if (job instanceof MissionJob)	missionJobs.put(job.getId(), (MissionJob) job);
		else if (job instanceof AuctionJob)	auctionJobs.put(job.getId(), (AuctionJob) job);
		else if (job instanceof PostedJob ) postedJobs .put(job.getId(), (PostedJob ) job);
		else throw new UnsupportedOperationException("Unsupported job: " + job.getId());		
	}

	public void removeJob(Job job) {
			 if (job instanceof SimpleJob ) simpleJobs .remove(job.getId());
		else if (job instanceof MissionJob)	missionJobs.remove(job.getId());
		else if (job instanceof AuctionJob)	auctionJobs.remove(job.getId());
		else if (job instanceof PostedJob ) postedJobs .remove(job.getId());
		else throw new UnsupportedOperationException("Unsupported job: " + job.getId());		
	}
	
	public void setRemovedJobs() {
		removedJobs.clear();
		removedJobs.addAll(simpleJobs .values());
		removedJobs.addAll(missionJobs.values());
		removedJobs.addAll(auctionJobs.values());
		removedJobs.addAll(postedJobs .values());
	}
	
	public Set<Job> getRemovedJobs() {
		return removedJobs;
	}
	
	public void clearJobs() {
		auctionJobs	.clear();
		simpleJobs	.clear();
		missionJobs	.clear();
		postedJobs	.clear();
	}
}
