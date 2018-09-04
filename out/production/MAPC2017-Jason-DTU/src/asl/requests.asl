+retrieveRequest(AgentStr, [map(Shop,Items)|Shops], Workshop, CNPId) : free <-
	!!retrieveRequest(AgentStr, [map(Shop,Items)|Shops], Workshop, CNPId). 

+!retrieveRequest(AgentStr, [map(Shop,Items)|Shops], Workshop, CNPId) 
	: free & capacity(Capacity) & speed(Speed) <-
	
	-free;
	getItemsToCarry(Items, Capacity, ItemsToRetrieve, Rest);	
	getVolume(ItemsToRetrieve, Volume);	
	distanceToFacility(Shop, Distance);	
	
	// Negative volume since lower is better
	Bid = math.ceil(Distance/Speed) * 10 - Volume; 
	
	if (not ItemsToRetrieve = []) 
	{ 
		bid(Bid)[artifact_id(CNPId)]; 
		winner(Won)[artifact_id(CNPId)];
		
		if (Won)
		{
			clearRetrieve(CNPId);
			!retrieve(AgentStr, ItemsToRetrieve, Workshop, [map(Shop,Rest)|Shops]);		
		}
	}
	+free.

+assembleRequest(Items, Workshop, TaskId, DeliveryLocation, _, CNPId) : free <-
	!!assembleRequest(Items, Workshop, TaskId, DeliveryLocation, _, CNPId).

+!assembleRequest(Items, Workshop, TaskId, DeliveryLocation, _, CNPId) 
	: free & capacity(Capacity) & speed(Speed) <-
	
	-free;
	getItemsToCarry(Items, Capacity, ItemsToAssemble, AssembleRest);
	getBaseItems(ItemsToAssemble, ItemsToRetrieve);
	getVolume(ItemsToRetrieve, Volume);
	distanceToFacility(Workshop, Distance);	
	
	// Negative volume since lower is better
	Bid = math.ceil(Distance/Speed) * 10 - Volume; 
	
	if (not ItemsToRetrieve = []) 
	{ 
		bid(Bid)[artifact_id(CNPId)];
		winner(Won)[artifact_id(CNPId)];
		
		if (Won)
		{		
			.print(ItemsToAssemble, " - ", AssembleRest);
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
	
+!retrieve(_, [], _, _).
+!retrieve(AgentStr, ItemsToRetrieve, Workshop, ToAnnounce) 
	: .my_name(Me) & .term2string(Agent, AgentStr) <-
	
	if (ToAnnounce = [map(Shop,Rest)|Shops] & not (Rest = [] & Shops = []))
	{
		.send(Agent,   tell, assistNeeded);
		.send(Agent, untell, assistNeeded); // Simulates a signal
		.send(announcer, achieve, announceRetrieve(Agent, [map(Shop,Rest)|Shops], Workshop));
	}
	
	!retrieveItems(map(Shop, ItemsToRetrieve));

	!getToFacility(Workshop);
	
	.send(Agent,   tell, assistReady(Me));
	
	.wait(assembleReady(ReadyStep));
	.wait(step(ReadyStep));
	
	!assistAssemble(Agent);
	
	.send(Agent, untell, assistReady(Me)).
	
+!assemble([], _, _).
+!assemble(ItemsToRetrieve, ItemsToAssemble, AssembleRest, Workshop, TaskId, DeliveryLocation) 
	: .my_name(Me) <-

	getShoppingList(ItemsToRetrieve, ShoppingList);
	ShoppingList = [Shop|RetrieveRest];	
	
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
	
	!retrieveItems(Shop);
	!getToFacility(Workshop);
	
	.wait(assistCount(N) & assistants(L) & .length(L, N));
	
	?step(X);
	ReadyStep = X + 1;
	
	for (.member(A, L))
	{
		.send(A,   tell, assembleReady(ReadyStep));
	}
	
	.wait(step(ReadyStep));
	
	!assembleItems(ItemsToAssemble);	
	!!assembleComplete;
	
	!getToFacility(DeliveryLocation);
	!deliverItems(TaskId, DeliveryLocation).

	
+free : retrieveRequest(AgentStr, [map(Shop,Items)|Shops], Workshop, CNPId) 
		& capacity(Capacity) <-
		
	getItemsToCarry(Items, Capacity, ItemsToRetrieve, Rest);
	
	if (not ItemsToRetrieve = [])
	{
		takeTask(CanTake)[artifact_id(CNPId)];
		
		if (CanTake)
		{
			-free;
			clearRetrieve(CNPId);			
			!retrieve(AgentStr, ItemsToRetrieve, Workshop, [map(Shop,Rest)|Shops]);			
			+free;
		}
	}.
	
+free : assembleRequest(Items, Workshop, TaskId, DeliveryLocation, _, CNPId)
		& capacity(Capacity) <-
		
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
