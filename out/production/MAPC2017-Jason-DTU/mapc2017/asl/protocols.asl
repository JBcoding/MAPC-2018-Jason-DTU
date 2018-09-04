	
+!initiateAssembleProtocol(Items) <-
	!skip(.count(assistant	   [source(_)], N) 
		& .count(assistantReady[source(_)], N));
	for (assistant[source(A)]) { .send(A,   tell, assemble); }
	!assembleItems(Items);	
	for (assistant[source(A)]) { .send(A, untell, assemble); }.
	
+!acceptAssembleProtocol(Agent) <-
	.send(Agent, tell, assistantReady);	
	!skip(assemble[source(Agent)], 200);
	!assistAssemble(Agent);	
	.send(Agent, untell, assistantReady).
