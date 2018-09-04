!observe.

+!observe <-
	?findTaskArtifact(Id);
	focus(Id).
	
+?findTaskArtifact(A) <-
	lookupArtifact("TaskArtifact", A).
	
-?findTaskArtifact(A) <-
	.wait(10);
	?findTaskArtifact(A).	

+task(Task, CNPName) <- 
	lookupArtifact(CNPName, CNPId);
	bid(20, BidId)[artifact_id(CNPId)];
	.print("Id of bid: ", BidId);
	winner(WinnerId)[artifact_id(CNPId)];
	.print("Id of winning bid: ", WinnerId).
	