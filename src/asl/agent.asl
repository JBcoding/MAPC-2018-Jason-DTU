{ include("connections.asl") }
{ include("stdlib.asl") }
{ include("rules.asl") }
{ include("plans.asl") }
{ include("protocols.asl") }
{ include("requests.asl") }

// Initial beliefs
free.

// Initial goals
!register.
!focusArtifacts.

!startLoop.

+!startLoop <- .wait({+step(_)}); .wait(500); !loop.
+!loop : free & scout(X) & X <- .print("Scouting"); -free; !scoutt; +free; !loop.
+!loop : build <- .print("Building well"); -free; !buildWell; +free; !loop.
+!loop : destroy <- .print("Dismantling wells"); -free; !dismantleEnemyWell; +free; !loop.
+!loop : free & gather(X) & X <- .print("Gathering items"); -free; !gatherRole; +free; !loop.
+!loop : free & builder(X) & X <- .print("Creating items"); -free; !builderRole; +free; !loop.
+!loop : not fullCharge <- .print("ERROR: Nothing to do. Should have a role"); !charge; !loop.

// Percepts
+!doAction(Action) : .my_name(Me) <- jia.action(Me, Action); .wait({+step(_)}).

+step(X) : lastAction("build") & lastActionResult("failed_location") <- stopBuilding.
+step(X) : lastAction("assist_assemble") & lastActionResult("failed_counterpart").
+step(X) : lastAction("give") 		 & lastActionResult("successful") <- .print("Give successful!").
+step(X) : lastAction("receive") 	 & lastActionResult("successful") <- .print("Receive successful!").
+step(X) : lastAction("deliver_job") & lastActionResult("successful") & lastActionParam([Id])
	<- .print("Job successful! ID: ", Id); incJobCompletedCount; completeJob(Id).
+step(X) : lastAction("deliver_job") & lastActionResult(R) & lastActionParam(P)
    <- .print("   ~~~ DELIVER JOB: ", R, " ", P, " ~~~   ").
//+step(X) : lastAction("bid_for_job") & lastActionResult("successful") & .print("Bid on job successful ") & false.
+step(X) : lastAction("gather") & lastActionResult("successful_partial").
+step(X) : lastActionResult(R) &   not lastActionResult("successful")
		 & lastAction(A) & lastActionParam(P) <- .print(R, " ", A, " ", P).
		 
+reset <- .print("resetting"); .drop_all_desires; .drop_all_events; .drop_all_intentions; -reset.
