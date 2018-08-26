!announce.

+!announce <- 
	!setupTaskArtifact(Id);
	announce("description", 5000)[artifact_id(Id)];
	.print("Announced task").
	
+!setupTaskArtifact(A) <-
	makeArtifact("TaskArtifact", "cnp.TaskArtifact", [], A).