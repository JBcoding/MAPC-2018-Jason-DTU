{ include("connections.asl") }
{ include("stdlib.asl") }
{ include("rules.asl") }

!focusArtifacts.

+task(TaskId, Type) : Type \== "auction" <-
	getJob(TaskId, Storage, Items);
	.print("New task: ", TaskId, " - ", Items, " - Type: ", Type);
	getClosestWorkshopToStorage(Storage, Workshop);
	if (Type = "mission") { !announceAssemble(Items, Workshop, TaskId, Storage, "mission"); }
	else { !announceAssemble(Items, Workshop, TaskId, Storage, "new"); }
	clearTask(TaskId); -task(TaskId, _).
	
+task(TaskId, "auction") <- announceAuction(TaskId); clearTask(TaskId); -task(TaskId, _).
	
+!announceAssemble([], _, _, _, _).
+!announceAssemble(Items, Workshop, TaskId, Storage, Type) <-
    announceAssemble(Items, Workshop, TaskId, Storage, Type).

+!announceRetrieve(Agent, [map(_,[])|Rest], Workshop) <- !announceRetrieve(Agent, Rest, Workshop).
+!announceRetrieve(Agent, ResourceList, Workshop) <- announceRetrieve(Agent, ResourceList, Workshop).
	
+assembleRequest(_, _, TaskId, _, "new", CNPId) <-
	takeTask(CanTake)[artifact_id(CNPId)];
	if (CanTake)
	{
		completeJob(TaskId);
		clearAssemble(CNPId);
	}.
