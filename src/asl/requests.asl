+deliverRequest(Items, TaskId, DeliveryLocation, _, CNPId) : free <-
	!!deliverRequest(Items, TaskId, DeliveryLocation, _, CNPId).

+!deliverRequest(Items, TaskId, DeliveryLocation, _, CNPId)
	: free & remainingCapacity(Capacity) & speed(Speed) <-
	-free;

	getItemsToCarry(Items, Capacity, ItemsForMe, ItemsRest);

	getVolume(ItemsForMe, Volume);

	getMainStorageFacility(S);
	distanceToFacility(S, Distance);

	// Negative volume since lower is better
	Bid = math.ceil(Distance/Speed) * 10 - Volume;

	if (not ItemsForMe = []) {
		bid(Bid)[artifact_id(CNPId)];
		winner(Won)[artifact_id(CNPId)];

		if (Won) {
			.print("To deliver: ", ItemsForMe, " - ", ItemsRest);
			clearDeliver(CNPId);
			!deliver(ItemsForMe, ItemsRest, TaskId, DeliveryLocation);
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

+!deliver([], [], _, _).
+!deliver(ItemsForMe, ItemsRest, TaskId, DeliveryLocation) <-
    .send(announcer, tell, busy);

    // We cannot carry it all, so we need some more deliverers.
	if (not ItemsRest = []) {
		.send(announcer, achieve, announceDeliver(ItemsRest, TaskId, DeliveryLocation, "old"));
	}

    getMainStorageFacility(S);
    !getToFacility(S);
	!getItems(ItemsForMe);
	!getToFacility(DeliveryLocation);
	!deliverItems(TaskId);
    .send(announcer, tell, available);
	!getToFacility(S);
	!emptyInventory.

+free : deliverRequest(Items, TaskId, DeliveryLocation, _, CNPId) & remainingCapacity(Capacity) <-
	getItemsToCarry(Items, Capacity, ItemsForMe, ItemsRest);

	if (not ItemsForMe = []) {
		takeTask(CanTake)[artifact_id(CNPId)];
		
		if (CanTake) {
            -free;
			clearDeliver(CNPId);
			!deliver(ItemsForMe, ItemsRest, TaskId, DeliveryLocation);
			+free;
		}
	}.

