{ include("connections.asl") }
{ include("stdlib.asl") }
{ include("rules.asl") }

!focusArtifacts.

//+step(X) <- // .print(""); .print("STEP ", X); .print("").
//    .print("---------------------------------------------------------",
//           "---------------------------------------------------------",
//           "------------------------------------------------------> ", X).


+task(TaskId, Type) : Type \== "auction" <-
	getJob(TaskId, Storage, Items);
	.print("New task: ", TaskId, " - ", Items, " - Type: ", Type);
	getClosestWorkshopToStorage(Storage, Workshop); // TODO: Do we need this?
    // TODO: Consider waiting with announcing until we are done scouting
	if (Type = "mission") {
	    !announceAssemble(Items, Workshop, TaskId, Storage, "mission");
    } else {
        !announceAssemble(Items, Workshop, TaskId, Storage, "new");
    }
	clearTask(TaskId); -task(TaskId, _).
	
+task(TaskId, "auction") <- announceAuction(TaskId); clearTask(TaskId); -task(TaskId, _).
	
+!announceAssemble([], _, _, _).
+!announceAssemble(Items, Workshop, TaskId, Storage, Type) <-
    announceAssemble(Items, Workshop, TaskId, Storage, Type).

+!announceRetrieve(Agent, [map(_, 0)|Rest], Roles, Workshop) <- !announceRetrieve(Agent, Rest, Roles, Workshop).
+!announceRetrieve(Agent, ResourceList, Roles, Workshop) <- announceRetrieve(Agent, ResourceList, Roles, Workshop).

+!announceAssist(Agent, Roles, Workshop) <- announceAssist(Agent, Roles, Workshop).

+assembleRequest(_, _, TaskId, _, "new", CNPId) <-
	takeTask(CanTake)[artifact_id(CNPId)];
	if (CanTake)
	{
		completeJob(TaskId);
		clearAssemble(CNPId);
	}.
