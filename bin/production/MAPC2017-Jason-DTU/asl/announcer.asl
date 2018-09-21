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
    haveItemsReady(Items, X);
	if (Type = "mission") {
        .print("~~~ TAKING MISSION: ", TaskId, "(", X, ") ~~~");
	    !announceDeliver(Items, TaskId, Storage, "mission");
    } else {
        if (X) {
            .print("~~~ TAKING JOB: ", TaskId, "~~~");
            !announceDeliver(Items, TaskId, Storage, "new");
        } else {
            .print("Forgoing ", TaskId);
        }
    }
	clearTask(TaskId); -task(TaskId, _).

+task(TaskId, "auction") <- .print("Ignoring auction"). // announceAuction(TaskId); clearTask(TaskId); -task(TaskId, _).
	
+!announceDeliver([], _, _, _).
+!announceDeliver(Items, TaskId, Storage, Type) <-
    announceDeliver(Items, TaskId, Storage, Type).

+deliverRequest(_, TaskId, _, "new", CNPId) <-
	takeTask(CanTake)[artifact_id(CNPId)];
	if (CanTake) {
		completeJob(TaskId);
		clearDeliver(CNPId);
	}.
