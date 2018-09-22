{ include("connections.asl") }
{ include("stdlib.asl") }
{ include("rules.asl") }

freeCount(0).

!focusArtifacts.

+step(X) <- // .print(""); .print("STEP ", X); .print("").
    .print("---------------------------------------------------------",
           "---------------------------------------------------------",
           "------------------------------------------------------> ", X).


+task(TaskId, Type) : Type \== "auction" & freeCount(N) <-
	getJob(TaskId, Storage, Items, EndStep);
	.print("New task: ", TaskId, " - ", Items, " - Type: ", Type);
    haveItemsReady(Items, X);
    if (X) {
	    if (Type = "mission") {
            .print("~~~ TAKING MISSION: ", TaskId, " (", EndStep, ") ", " ~~~");
	        !announceDeliver(Items, TaskId, Storage, "mission");
        } else {
            if (true) { // TODO: +available does not work right now (consider keeping one free for missions)
                .print("~~~ TAKING JOB: ", TaskId, " (", EndStep, ") ", "~~~");
                !announceDeliver(Items, TaskId, Storage, "new");
            } else {
                .print("No agents available for: ", TaskId);
            }
        }
    } else {
        .print("Forgoing ", Type, " ", TaskId);
    }
	clearTask(TaskId); -task(TaskId, _).

+task(TaskId, "auction") <- .print("Ignoring auction ", TaskId).
    // announceAuction(TaskId); clearTask(TaskId); -task(TaskId, _).
	
+!announceDeliver([], _, _, _).
+!announceDeliver(Items, TaskId, Storage, Type) <-
    announceDeliver(Items, TaskId, Storage, Type).

+deliverRequest(_, TaskId, _, "new", CNPId) <-
	takeTask(CanTake)[artifact_id(CNPId)];
	if (CanTake) {
		completeJob(TaskId);
		clearDeliver(CNPId);
	}.

@freeCount1[atomic]
+busy : freeCount(N) <- .print("Available: ", N - 1); -+freeCount(N - 1).

@freeCount2[atomic]
+available : freeCount(N) <- .print("Available: ", N + 1); -+freeCount(N + 1).
