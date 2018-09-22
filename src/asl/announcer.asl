{ include("connections.asl") }
{ include("stdlib.asl") }
{ include("rules.asl") }

freeAgents([]).

!focusArtifacts.

+step(X) <- // .print(""); .print("STEP ", X); .print("").
    .print("---------------------------------------------------------",
           "---------------------------------------------------------",
           "------------------------------------------------------> ", X).


+task(TaskId, Type) : Type \== "auction" & freeAgents(Agents) & .length(Agents, N) & step(StartStep) <-
	getJob(TaskId, Storage, Items, EndStep);
	.print("New task: ", TaskId, " - ", Items, " - Type: ", Type);
    haveItemsReady(Items, X);
    estimateSteps(TaskId, Agents, Steps);
    if (X & N > 0 & StartStep + Steps < EndStep) {
	    if (Type = "mission") {
            .print("~~~ TAKING MISSION: ", TaskId, " (", EndStep, ") ", " ~~~");
	        !announceDeliver(Items, TaskId, Storage, "mission");
        } else {
            .print("~~~ TAKING JOB: ", TaskId, " (", EndStep, ") ", "~~~");
            !announceDeliver(Items, TaskId, Storage, "new");
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
+available(Agent) : freeAgents(L) <- .print("Available: ", L); -+freeAgents([Agent|L]).

@freeCount2[atomic]
-available(Agent) : freeAgents(L) & delete(Agent, L, L2) <- .print("Available: ", L2); -+freeAgents(L2).
