+retrieveRequest(AgentStr, [map(Node,Amount)|Nodes], Roles, Workshop, CNPId) : free <-
	!!retrieveRequest(AgentStr, [map(Node,Amount)|Nodes], Roles, Workshop, CNPId).

+!retrieveRequest(AgentStr, [map(Node,Amount)|Nodes], Roles, Workshop, CNPId)
	: free & remainingCapacity(Capacity) & speed(Speed) <-
	-free;

	getResource(Node, Item);
	getAmountToCarry(Item, Amount, Capacity, AmountToRetrieve, Rest);
	getVolume([map(Item, AmountToRetrieve)], Volume);
	distanceToFacility(Node, Distance);

	// Negative volume since lower is better
	Bid = math.ceil(Distance/Speed) * 10 - Volume; // TODO: Should this be 10?

	if (not AmountToRetrieve = 0) {
		bid(Bid)[artifact_id(CNPId)];
		winner(Won)[artifact_id(CNPId)];

		if (Won) {
			clearRetrieve(CNPId);
			!retrieve(AgentStr, Node, map(Item, AmountToRetrieve), Roles, Workshop, [map(Node, Rest)|Nodes]);
		}
	}

	+free.


+assistRequest(AgentStr, Roles, Workshop, CNPId) : free <-
	!!assistRequest(AgentStr, Roles, Workshop, CNPId).

+!assistRequest(AgentStr, Roles, Workshop, CNPId)
	: free & speed(Speed) & myRole(Role) <-
	-free;

	distanceToFacility(Workshop, Distance);

	// Negative volume since lower is better
	Bid = math.ceil(Distance/Speed) * 10; // TODO: Should this be 10?

	if (.member(Role, Roles)) {
        bid(Bid)[artifact_id(CNPId)];
        winner(Won)[artifact_id(CNPId)];

        if (Won) {
            clearAssist(CNPId);
            !assist(AgentStr, Roles, Workshop);
        }
	}

	+free.


+assembleRequest(Items, Workshop, TaskId, DeliveryLocation, _, CNPId) : free <-
	!!assembleRequest(Items, Workshop, TaskId, DeliveryLocation, _, CNPId).

+!assembleRequest(Items, Workshop, TaskId, DeliveryLocation, _, CNPId)
	: free & remainingCapacity(Capacity) & speed(Speed) <-
	-free;

	getItemsToCarry(Items, Capacity, ItemsToAssemble, AssembleRest);
	getBaseItems(ItemsToAssemble, ItemsToRetrieve);
	getVolume(ItemsToRetrieve, Volume);
	distanceToFacility(Workshop, Distance);

	// Negative volume since lower is better
	Bid = math.ceil(Distance/Speed) * 10 - Volume;

	if (not ItemsToRetrieve = []) {
		bid(Bid)[artifact_id(CNPId)];
		winner(Won)[artifact_id(CNPId)];

		if (Won) {
			.print("To assemble: ", ItemsToAssemble, " - ", AssembleRest);
			clearAssemble(CNPId);
			getRequiredRoles(ItemsToAssemble, Roles);
			!assemble(ItemsToRetrieve, ItemsToAssemble, Roles, AssembleRest, Workshop, TaskId, DeliveryLocation);
		}
	}
	+free.

+auction(TaskId, CNPId) : free <- 
	-free;
	takeTask(Can)[artifact_id(CNPId)];
	if (Can)
	{
		getBid(TaskId, Bid);
		if (Bid \== 0) { !doAction(bid_for_job(TaskId, Bid)); }
	}
	+free.
	
+!retrieve(_, _, map(_, 0), [], _, _) <- .print("+!retrieve empty amount").
+!retrieve(AgentStr, Node, map(Item, AmountToRetrieve), Roles, Workshop, ToAnnounce)
	: .my_name(Me) & .term2string(Agent, AgentStr)
	& myRole(Role) & deleteAny(Role, Roles, RolesRest) <-

	if (ToAnnounce = [map(Node,Rest)|Nodes] & not (Rest = 0 & Nodes = [])) {
		.send(Agent,   tell, assistNeeded);
		.send(Agent, untell, assistNeeded); // Simulates a signal
		.send(announcer, achieve, announceRetrieve(Agent, [map(Node,Rest)|Nodes], RolesRest, Workshop));
	} else {
        if (not RolesRest = []) {
            .print("Calling on extra roles: ", RolesRest);
            .send(Agent,   tell, assistNeeded);
            .send(Agent, untell, assistNeeded); // Simulates a signal
            .send(announcer, achieve, announceAssist(Agent, RolesRest, Workshop));
	    }
	}

	!retrieveItems(map(Node, AmountToRetrieve));

	!getToFacility(Workshop);

	.send(Agent, tell, assistReady(Me));

	.print("Waiting to assist ", AgentStr);

	.wait(assembleReady(ReadyStep));
	.wait(step(ReadyStep));

	!assistAssemble(Agent);

	.send(Agent, untell, assistReady(Me)).

+!assist(_, [], _).
+!assist(AgentStr, Roles, Workshop)
	: .my_name(Me) & .term2string(Agent, AgentStr)
	& myRole(Role) & deleteAny(Role, Roles, RolesRest)
	<-

	if (not RolesRest = []) {
		.send(Agent,   tell, assistNeeded);
		.send(Agent, untell, assistNeeded); // Simulates a signal
		.send(announcer, achieve, announceAssist(AgentStr, RolesRest, Workshop));
	}

	!getToFacility(Workshop);

	.send(Agent, tell, assistReady(Me));

	.print("Waiting to role assist: ", AgentStr);

	.wait(assembleReady(ReadyStep));
	.wait(step(ReadyStep));

	!assistAssemble(Agent);

	.send(Agent, untell, assistReady(Me)).

+!assemble([], [], _, _, _, _, _).
+!assemble(ItemsToRetrieve, ItemsToAssemble, Roles, AssembleRest, Workshop, TaskId, DeliveryLocation)
	: .my_name(Me) & myRole(Role) & deleteAny(Role, Roles, RolesRest) <-

	getResourceList(ItemsToRetrieve, ResourceList);
	ResourceList = [Node|RetrieveRest];

	+assistants([]);

    // The retrievers are responsible for assisting with the assembly.
    // They will call in more if not all roles are filled.
	if (not RetrieveRest = []) {
		+assistCount(1);
		.send(announcer, achieve, announceRetrieve(Me, RetrieveRest, RolesRest, Workshop));
	}

    // Assemblers are responsible for assembling separate items to be delivered.
	if (not AssembleRest = []) {
		.send(announcer, achieve, announceAssemble(AssembleRest, Workshop, TaskId, DeliveryLocation, "old"));
	}

	!retrieveItems(Node);

	!getToFacility(Workshop);

	.print("Waiting for others at ", Workshop);

	.wait(assistCount(N) & assistants(L) & .length(L, N));
	
	?step(X);
	ReadyStep = X + 1;
	
	for (.member(A, L)) {
		.send(A, tell, assembleReady(ReadyStep));
	}

	.print("Waiting for step ", ReadyStep);
	
	.wait(step(ReadyStep));

	.print("Commencing assemble: ", ItemsToAssemble);

	!assembleItems(ItemsToAssemble);	
	!!assembleComplete;

	.print("Assemble complete");

	!deliverItems(TaskId, DeliveryLocation).

//+free : scout(X) & X <- -free; !scoutt; +free.

//+free : build <- .print("Building"); -free; !buildWell; +free.

//+free : destroy <- .print("Destroying"); -free; !dismantleEnemyWell; +free.

+free : retrieveRequest(AgentStr, [map(Node,Amount)|Nodes], Workshop, CNPId)
		& remainingCapacity(Capacity) <-
	getResource(Node, Item);
	getAmountToCarry(Item, Amount, Capacity, AmountToRetrieve, Rest);

	if (not AmountToRetrieve = 0) {
		takeTask(CanTake)[artifact_id(CNPId)];
		
		if (CanTake) {
	        -free;
			clearRetrieve(CNPId);
			!retrieve(AgentStr, Node, map(Item, AmountToRetrieve), Roles, Workshop, [map(Node,Rest)|Nodes]);
        	+free;
		}
	}.


+free : assistRequest(AgentStr, Roles, Workshop, CNPId) & myRole(Role) <-
    if (.member(Role, Roles)) {
        takeTask(CanTake)[artifact_id(CNPId)];

        if (CanTake) {
            -free;
            clearAssist(CNPId);
            !assist(AgentStr, Roles, Workshop);
            +free;
        }
    }.

+free : assembleRequest(Items, Workshop, TaskId, DeliveryLocation, _, CNPId)
      & remainingCapacity(Capacity) <-

	getItemsToCarry(Items, Capacity, ItemsToAssemble, AssembleRest);
	getBaseItems(ItemsToAssemble, ItemsToRetrieve);
	
	if (not ItemsToRetrieve = []) {
		takeTask(CanTake)[artifact_id(CNPId)];
		
		if (CanTake) {
            -free;
			clearAssemble(CNPId);
			getRequiredRoles(ItemsToAssemble, Roles);
			!assemble(ItemsToRetrieve, ItemsToAssemble, Roles, AssembleRest, Workshop, TaskId, DeliveryLocation);
			+free;
		}
	}.

@assistCount[atomic]
+assistNeeded : assistCount(N) <- -+assistCount(N + 1).

@assistants[atomic]
+assistReady(A) : assistants(L) & assistCount(M) <-
    .length([A|L], N);
    .print("Assistants ready: ", N, " / ", M);
    -+assistants([A|L]).

+!assembleComplete : assistants(L) & step(X) <-

	for (.member(A, L)) 
	{
		.send(A, tell, assembleComplete);
	}
	
	.wait(step(Y) & Y > X);

	for (.member(A, L)) 
	{
		.send(A, untell, assembleComplete);
		.send(A, untell, assembleReady(_));
	}
	
	-assistCount(_);
	-assistants(_).
