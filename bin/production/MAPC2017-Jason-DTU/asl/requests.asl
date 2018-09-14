+retrieveRequest(AgentStr, [map(Node,Amount)|Shops], Workshop, CNPId) : free <-
	!!retrieveRequest(AgentStr, [map(Node,Amount)|Shops], Workshop, CNPId).

+!retrieveRequest(AgentStr, [map(Node,Amount)|Nodes], Workshop, CNPId)
	: free & remainingCapacity(Capacity) & speed(Speed) <-
	
	-free;
	getResource(Node, Item);
	getAmountToCarry(Item, Amount, Capacity, AmountToRetrieve, Rest);
	getVolume([map(Item, AmountToRetrieve)], Volume);
	distanceToFacility(Node, Distance);
	
	// Negative volume since lower is better
	Bid = math.ceil(Distance/Speed) * 10 - Volume; // TODO: Should this be 10?

	if (not AmountToRetrieve = 0)
	{ 
		bid(Bid)[artifact_id(CNPId)];
		winner(Won)[artifact_id(CNPId)];

		if (Won)
		{
			clearRetrieve(CNPId);
			!retrieve(AgentStr, Node, map(Item, AmountToRetrieve), Workshop, [map(Node, Rest)|Nodes]);
		}
	}

	+free.

+assembleRequest(Items, Workshop, TaskId, DeliveryLocation, _, CNPId) : free <-
	!!assembleRequest(Items, Workshop, TaskId, DeliveryLocation, _, CNPId).

+!assembleRequest(Items, Workshop, TaskId, DeliveryLocation, _, CNPId) 
	: free & remainingCapacity(Capacity) & speed(Speed) <-
	-free;

    getRequiredRoles(Items, Roles);

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
			!assemble(ItemsToRetrieve, ItemsToAssemble, AssembleRest, Workshop, TaskId, DeliveryLocation);
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
	
+!retrieve(_, _, map(_, 0), _, _).
+!retrieve(AgentStr, Node, map(Item, AmountToRetrieve), Workshop, ToAnnounce)
	: .my_name(Me) & .term2string(Agent, AgentStr) <-

	if (ToAnnounce = [map(Node,Rest)|Nodes] & not (Rest = 0 & Shops = []))
	{
		.send(Agent,   tell, assistNeeded);
		.send(Agent, untell, assistNeeded); // Simulates a signal
		.send(announcer, achieve, announceRetrieve(Agent, [map(Node,Rest)|Nodes], Workshop));
	}
	
	!retrieveItems(map(Node, AmountToRetrieve));

	!getToFacility(Workshop);
	
	.send(Agent, tell, assistReady(Me));
	
	.wait(assembleReady(ReadyStep));
	.wait(step(ReadyStep));
	
	!assistAssemble(Agent);
	
	.send(Agent, untell, assistReady(Me)).
	
+!assemble([], _, _).
+!assemble(ItemsToRetrieve, ItemsToAssemble, AssembleRest, Workshop, TaskId, DeliveryLocation) 
	: .my_name(Me) <-

	getResourceList(ItemsToRetrieve, ResourceList);
	.print(ResourceList);
	ResourceList = [Node|RetrieveRest];

	+assistants([]);

	if (not RetrieveRest = [])
	{
		+assistCount(1);
		.send(announcer, achieve, announceRetrieve(Me, RetrieveRest, Workshop));
	}

	if (not AssembleRest = [])
	{
		.send(announcer, achieve, announceAssemble(AssembleRest, Workshop, TaskId, DeliveryLocation, "old"));			
	}

	!retrieveItems(Node);

	!getToFacility(Workshop);
	
	.wait(assistCount(N) & assistants(L) & .length(L, N));
	
	?step(X);
	ReadyStep = X + 1;
	
	for (.member(A, L))
	{
		.send(A,   tell, assembleReady(ReadyStep));
	}

	.print("Waiting for step ", ReadyStep);
	
	.wait(step(ReadyStep));
	
	!assembleItems(ItemsToAssemble);	
	!!assembleComplete;
	
	!getToFacility(DeliveryLocation);
	!deliverItems(TaskId, DeliveryLocation).

+free : retrieveRequest(AgentStr, [map(Node,Amount)|Nodes], Workshop, CNPId)
		& remainingCapacity(Capacity) <-
		
	getResource(Node, Item);
	getAmountToCarry(Item, Amount, Capacity, AmountToRetrieve, Rest);

	if (not AmountToRetrieve = 0)
	{
		takeTask(CanTake)[artifact_id(CNPId)];
		
		if (CanTake)
		{
			-free;
			clearRetrieve(CNPId);			
			!retrieve(AgentStr, Node, map(Item, AmountToRetrieve), Workshop, [map(Shop,Rest)|Shops]);
			+free;
		}
	}.
	
+free : assembleRequest(Items, Workshop, TaskId, DeliveryLocation, _, CNPId)
		& remainingCapacity(Capacity) <-

	getItemsToCarry(Items, Capacity, ItemsToAssemble, AssembleRest);
	getBaseItems(ItemsToAssemble, ItemsToRetrieve);
	
	if (not ItemsToRetrieve = [])
	{
		takeTask(CanTake)[artifact_id(CNPId)];
		
		if (CanTake)
		{		
			-free;
			clearAssemble(CNPId);			
			!assemble(ItemsToRetrieve, ItemsToAssemble, AssembleRest, Workshop, TaskId, DeliveryLocation);			
			+free;
		}
	}.
	
@assistCount[atomic]
+assistNeeded : assistCount(N) <- -+assistCount(N + 1).

@assistants[atomic]
+assistReady(A) : assistants(L) <- -+assistants([A|L]).

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
