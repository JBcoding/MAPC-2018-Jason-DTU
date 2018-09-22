{ include("connections.asl") }
{ include("stdlib.asl") }
{ include("rules.asl") }
{ include("plans.asl") }
{ include("protocols.asl") }
{ include("requests.asl") }

// Initial goals
!register.
!focusArtifacts.

!startLoop.

!continueLoopSleep.

+!startLoop <- .wait({+step(_)}); .wait(500); !loop.

+!loop : scout(X) & X <- .print("Scouting"); !scoutt; !loop.
+!loop : deliver(X) & X <-
    .print("Delivering items");
    .send(announcer, tell, available);
    getMainStorageFacility(S);
    !getToFacility(S);
    +free.
+!loop : build <- .print("Building well"); !buildWell; !loop.
+!loop : destroy <- .print("Dismantling wells"); !dismantleEnemyWell; !loop.
+!loop : gather(X) & X <- .print("Gathering items"); !gatherRole; !loop.
+!loop : builder(X) & X <- .print("Creating items"); !builderRole; !loop.
+!loop : not fullCharge <- .print("ERROR: Nothing to do. Should have a role"); !charge; !loop.

// Percepts
+!doAction(Action) : .my_name(Me) <-
    doActionStart; jia.action(Me, Action); .wait({+step(_)}); doActionEnd.

+step(X) : lastAction("goto") & lastActionResult("failed_no_route") <- !charge.
+step(X) : lastAction("build") & lastActionResult("failed_location") <- stopBuilding.
+step(X) : lastAction("assist_assemble") & lastActionResult("failed_counterpart").
+step(X) : lastAction("give") 		 & lastActionResult("successful") <- .print("Give successful!").
+step(X) : lastAction("receive") 	 & lastActionResult("successful") <- .print("Receive successful!").
+step(X) : lastAction("recharge").
+step(X) : lastAction("deliver_job") & lastActionResult("successful") & lastActionParam([Id])
	<- .print("   ~*~ JOB SUCCESSFUL! ID: ", Id, " ~*~   "); incJobCompletedCount; completeJob(Id).
+step(X) : lastAction("deliver_job") & lastActionResult(R) & lastActionParam(P)
    <- .print("   ~~~ DELIVER JOB: ", R, " ", P, " ~~~   ").
//+step(X) : lastAction("bid_for_job") & lastActionResult("successful") & .print("Bid on job successful ") & false.
+step(X) : lastAction("gather") & lastActionResult("successful_partial").
+step(X) : lastAction("retrieve") & lastActionResult("failed_item_amount").
+step(X) : lastActionResult(R) &   not lastActionResult("successful")
		 & lastAction(A) & lastActionParam(P) <- .print(R, " ", A, " ", P).
		 
+reset <- .print("resetting"); .drop_all_desires; .drop_all_events; .drop_all_intentions; -reset.

+!continueLoopSleep <- .wait({+step(_)}); !continueLoop.
+!continueLoop <- .wait(500); doingAction(X); if (not X) {!doAction(recharge); !continueLoop;} else {!continueLoopSleep;}.