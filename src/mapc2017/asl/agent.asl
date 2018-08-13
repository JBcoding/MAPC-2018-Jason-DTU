{ include("rules.asl") }
{ include("plans.asl") }
{ include("protocols.asl") }

!init.

+!init : .my_name(Me) & .term2string(Me, AgentPerceiver) <-
	!focusArtifact("SimStartPerceiver");
	!focusArtifact("ReqActionPerceiver");
	!focusArtifact(AgentPerceiver);
	!focusArtifact("JobDelegator").
-!init <- .wait(500); !init.

+!focusArtifact(Name) <- lookupArtifact(Name, Id); focus(Id).

+!doAction(Action) <- performAction(Action); .wait({+step(_)}).

//+start <- !reset; !free.
+start <- !start.

+task(Id, Items, Storage, ShoppingList, Workshop) <- 
	!task(doTask(Id, Items, Storage, ShoppingList, Workshop)).
+task(AgentStr, ShoppingList, Workshop) : .term2string(Agent, AgentStr) <-
	!task(doTask(Agent, ShoppingList, Workshop)).
+task(Id, Bid) <-
	!task(doTask(Id, Bid)).
+task("release") <-
	!reset; !!free.
+task(Storage) <- 
	!task(doTask(Storage)).
	
+!task(Task) : .print(Task) & false.
+!task(Task) <-	!stop; !Task; !!free.
-!task(Task) <- .print("Failed"); .wait(1000); !task(Task).

+!start <- !reset; free; !buyAvailable; !charge; !gather; !goToCenter; !skip.
+!free  <- !stop;  free; !charge; !gather; !goToCenter; !skip.
+!stop  <- .drop_all_desires; .drop_all_intentions.
+!reset <- !stop; .drop_all_events;
	for (assemble	   [source(A)]) { -assemble      [source(A)] };
	for (assistant     [source(A)]) { -assistant     [source(A)] };
	for (assistantReady[source(A)]) { -assistantReady[source(A)] };.

+step(X) : .my_name(agent1) & .print("Step: ", X) & false.
+step(X) : lastAction("gather").// & lastActionResult("successful_partial").
+step(X) : lastActionResult(R) & lastAction(A) & lastActionParams(P)
		 & not A = "goto" & not A = "noAction" & not A = "charge" & not A = "skip"
		 & not A = "assist_assemble" & not A = "recharge" <- .print(R, " ", A, " ", P).
+step(X) : lastActionResult(R) &   not lastActionResult("successful") 
		 & lastAction(A) & lastActionParams(P) <- .print(R, " ", A, " ", P).
		 
