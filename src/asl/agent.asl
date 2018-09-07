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


// !startLoop.

<<<<<<< HEAD
+!startLoop <- .wait({+step(_)}); !loop.
+!loop <- !getToFacility("shop1"); !getToFacility("shop2"); !loop.
=======
+!startLoop <- .wait({+step(_)}); .wait(500); !loop.
// +!loop <- !doAction(recharge); !loop.
+!loop : scout <- !getToFacility("shop2"); !loop.
+!loop <- !getToFacility("shop1"); !loop.
>>>>>>> 262754b0db3e2cff2a8502101573afee8500bfbe
	
// Percepts	
+!doAction(Action) : .my_name(Me) <- jia.action(Me, Action); .wait({+step(_)}).

+step(X) : lastAction("assist_assemble") & lastActionResult("failed_counterpart").
+step(X) : lastAction("give") 		 & lastActionResult("successful") <- .print("Give successful!").
+step(X) : lastAction("receive") 	 & lastActionResult("successful") <- .print("Receive successful!").
+step(X) : lastAction("deliver_job") & lastActionResult("successful") & lastActionParam([Id])
	<- .print("Job successful! ID: ", Id); incJobCompletedCount; completeJob(Id).
//+step(X) : lastAction("bid_for_job") & lastActionResult("successful") & .print("Bid on job successful ") & false.
+step(X) : lastActionResult(R) &   not lastActionResult("successful") 
		 & lastAction(A) & lastActionParam(P) <- .print(R, " ", A, " ", P).
		 
+reset <- .print("resetting"); .drop_all_desires; .drop_all_events; .drop_all_intentions; -reset.
