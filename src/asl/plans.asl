+!giveItems(_, []).
+!giveItems(Agent, [map(Item, Amount)|Items]) : connection(Agent, Entity, _) <-
	!doAction(give(Entity, Item, Amount));
	!giveItems(Agent, Items).

+!receiveItems([]).
+!receiveItems([_|Items]) <-
	!doAction(receive);
	!receiveItems(Items).

+!retrieveItems(map(Node, Amount)) <-
    getLocation(Node, Lat, Lon);
    getResource(Node, Item);
	!getToLocation(Node, Lat, Lon);
	!gatherItem(Node, map(Item, Amount)).

+!gatherItem(Node, Map) : getInventory(Inv) & contains(Map, Inv).
+!gatherItem(Node, map(Item, Amount)) <-
    !doAction(gather);
    !gatherItem(Node, map(Item, Amount)).

// Assumes we are at the storage facility.
+!getItems([]).
+!getItems([Item|Items]) : getInventory(Inv) & contains(Item, Inv) <-
    map(I, Amount) = Item;
    unreserve(I, Amount);
    !getItems(Items).
+!getItems([map(Item, Amount) | Items]) : getInventory(Inv) & append(Items, [map(Item, Amount)], NewItems) <-
    isBase(Item, BaseItem);
    itemInStorageIncludingReserved(Item, Amount, Ready);
    if (BaseItem & not Ready) {
        !doAction(recharge);
    } else {
        !doAction(retrieve(Item, Amount));
    }
    // Sometimes it seems to take a bit to observe the retrieve,
    // so try to retrieve the rest of the items first.
    !getItems(NewItems).


+!buildWell :
    inOwnWell &
    inFacility(F)
    <-
    wellHasFullIntegrity(F, X);
    if (not X) {
        !doAction(build);
        !buildWell;
    } else {
        stopBuilding;
        .print("Done building well");
    }.
+!buildWell : atPeriphery & inFacility & not inOwnWell <-
    .print("Hoping for different well builder. In: ", F);
    stopBuilding.
+!buildWell :
    atPeriphery
    <-
    getMoney(Money);
    bestWellType(Money, WellType);
    setToBuild(WellType, CanBuild);
    if (not (WellType == "none") & CanBuild) {
        !doAction(build(WellType));
        !buildWell;
    } else {
        stopBuilding;
        .print("Not enough massium to build any well (1)");
    }.
+!buildWell
    <-
    getMoney(Money);
    bestWellType(Money, WellType);
    setToBuild(WellType, CanBuild);
    if (not (WellType == "none") & CanBuild) {
        closestPeriphery(Lat, Lon);
        !getToPeripheryLocationStart(Lat, Lon);
        !buildWell;
    } else {
        stopBuilding;
        .print("Not enough massium to build any well (2)");
    }.

//+!dismantleOwnWell : inOwnWell <- !doAction(dismantle); !dismantleOwnWell.

+!dismantleEnemyWell : inEnemyWell <- !doAction(dismantle); !dismantleEnemyWell.
+!dismantleEnemyWell
    <-
    getEnemyWell(F, Lat, Lon);
    if (not (F == "none")) {
        !getToLocationWell(F, Lat, Lon);
    } else {
        getRandomPeripheralLocation(PerLat, PerLon);
        !getToPeripheryLocationStart(PerLat, PerLon);
    }
    !dismantleEnemyWell.

+!upgrade(Type) :
    inShop
    <-
    getMoney(Money);
    getUpgradePrice(Type, Price);
// TODO: Possibly decide whether upgrading is worth the cost (It rarely is. Maybe we never want to upgrade.)
    if (Price <= Money) {
        !doAction(upgrade(Type));
    } else {
        .print("Not enough money to upgrade");
    }.
+!upgrade(Type) <-
    getClosestFacility("shop", F);
    !getToFacility(F);
    !upgrade(Type).

+!upgradeSpeed <- !upgrade("speed").
+!upgradeVision <- !upgrade("vision").
+!upgradeSkill <- !upgrade("skill").
+!upgradeCapacity <- !upgrade("load").
+!upgradeBattery <- !upgrade("battery").

+!gather : inResourceNode	<- !doAction(gather); !gather.
+!gather 					<-
	getClosestFacility("resourceNode", F);
	if (not (F == "none"))
	{
		!getToFacility(F);
		!gather;
	}
	else { .print("Can not find any resource nodes"); }.

+!deliverItems(TaskId) : lastAction("deliver_job") & lastActionResult("failed_job_status").
+!deliverItems(TaskId) : lastAction("deliver_job") & lastActionResult("successful").
+!deliverItems(TaskId) : lastAction("deliver_job") & lastActionResult("successful_partial").
+!deliverItems(TaskId) : lastAction("deliver_job") & lastActionResult("useless").
+!deliverItems(TaskId) <-
 	!doAction(deliver_job(TaskId));
 	!deliverItems(TaskId).

+!assembleItems([]).
+!assembleItems([map(Item, Amount) | Items])
    : lastAction("assemble") & not lastActionResult("successful") <-
    // The last assemble failed, so don't decrement again
    .print("Retrying assemble(", Item, ")");
    !doAction(assemble(Item));
    !assembleItems([map(Item, Amount) | Items]).
+!assembleItems([map(Item, 0) | Items]) <-
    !assembleItems(Items).
+!assembleItems([map(Item, Amount) | Items]) <-
	getRequiredItems(Item, ReqItems);
    !assembleItem(Item, ReqItems);
    !assembleItems([map(Item, Amount - 1) | Items]).

// Recursively assemble required items
+!assembleItem(_, []). // Item is a base item.
+!assembleItem(Item, ReqItems) : myRole(Role) <-
	!assembleItems(ReqItems);
	.print("assemble(", Item, ") <-- ", Role);
	!doAction(assemble(Item)).

+!assistAssemble(Agent) : assembleComplete.
+!assistAssemble(Agent) : myRole(Role) <-
    !doAction(assist_assemble(Agent));
    !assistAssemble(Agent).

+!getToFacility(F) : inFacility(F).
+!getToFacility(F) : not canMove									<- !doAction(recharge); !getToFacility(F).
+!getToFacility(F) : not enoughCharge & not isChargingStation(F)    <- !charge; !getToFacility(F).
+!getToFacility(F) 													<- !doAction(goto(F)); 	!getToFacility(F).

// Meant for getting to resource nodes
+!getToLocation(F, Lat, Lon) : build <- !buildWell; !getToLocation(F, Lat, Lon).
+!getToLocation(F, _, _) : inFacility(F).
+!getToLocation(F, Lat, Lon) : not canMove <- !doAction(recharge); !getToLocation(F, Lat, Lon).
+!getToLocation(F, Lat, Lon) : not enoughCharge & not isChargingStation(F) <- !charge; !getToLocation(F, Lat, Lon).
+!getToLocation(F, Lat, Lon) <- !doAction(goto(Lat, Lon)); !getToLocation(F, Lat, Lon).

// Meant for getting to wells
+!getToLocationWell(F, Lat, Lon) : build <- !buildWell; !getToLocationWell(F, Lat, Lon).
+!getToLocationWell(F, _, _) : inFacility(F).
+!getToLocationWell(F, Lat, Lon) <- doesWellExist(F, X); if (X) {!getToLocationWellP2(F, Lat, Lon);}.
+!getToLocationWellP2(F, Lat, Lon) : not canMove <- !doAction(recharge); !getToLocationWell(F, Lat, Lon).
+!getToLocationWellP2(F, Lat, Lon) : not enoughCharge & not isChargingStation(F) <- !charge; !getToLocationWell(F, Lat, Lon).
+!getToLocationWellP2(F, Lat, Lon) <- !doAction(goto(Lat, Lon)); !getToLocationWell(F, Lat, Lon).

// Gets close to this location
+!getToPeripheryLocationStart(Lat, Lon) :
    destroy &
    lastAction("goto") &
    lastActionResult("failed_no_route")
    <-
    // Go somewhere else if we can't get to the assigned well
    getRandomPeripheralLocation(PerLat, PerLon);
    !getToPeripheryLocation(PerLat, PerLon, true).
+!getToPeripheryLocationStart(Lat, Lon) :
    destroy
    <-
    getEnemyWell(F, _, _);
    if (F == "none") {
        canSee(Lat, Lon, CanSee);
        !getToPeripheryLocation(Lat, Lon, CanSee);
    }.
+!getToPeripheryLocationStart(Lat, Lon) <- !getToPeripheryLocation(Lat, Lon, true).
+!getToPeripheryLocation(Lat, Lon, CanSee) : atPeriphery & CanSee.
+!getToPeripheryLocation(Lat, Lon, CanSee) : not canMove <- !doAction(recharge); !getToPeripheryLocationStart(Lat, Lon).
+!getToPeripheryLocation(Lat, Lon, CanSee) : not enoughCharge <- !charge; !getToPeripheryLocationStart(Lat, Lon).
+!getToPeripheryLocation(Lat, Lon, CanSee) <- !doAction(goto(Lat, Lon)); !getToPeripheryLocationStart(Lat, Lon).

+!charge : fullCharge.
+!charge : inChargingStation <-
    !doAction(charge);
    !charge.
+!charge : not canMove <- !doAction(recharge); !charge.
+!charge <-
	getClosestFacility("chargingStation", F);
	!getToFacility(F);
	!charge.

+!scoutt : scout(X) & X <- getClosestUnexploredPosition(Lat, Lon); !scout(Lat, Lon).
+!scoutt.

+!scout(Lat, Lon) : scout(X) & X & not canMove <- !doAction(recharge); !scout(Lat, Lon).
+!scout(Lat, Lon) : scout(X) & X & not enoughCharge <- !charge; !scout(Lat, Lon).
+!scout(Lat, Lon) : scout(X) & X <-
    !doAction(goto(Lat, Lon));
    canSee(Lat, Lon, Yes);
    if (Yes) {
        !scoutt;
    } else {
        !scout(Lat, Lon);
    }.
+!scout(_, _) : scout(X) & not X.

+!gatherUntilFull(_) : build <-
    !buildWell;
    // Perform the previous step for !gatherRole
    getResourceNode(F);
    getFacilityName(F, N);
    getCoords(F, Lat, Lon);
    !getToLocation(N, Lat, Lon);
    getItemVolume(F, V);
    !gatherUntilFull(V).
+!gatherUntilFull(V) : remainingCapacity(C) & C >= V <- !doAction(gather); !gatherUntilFull(V).
+!gatherUntilFull(V).

+!emptyInventory : build <- !buildWell; getMainStorageFacility(S); !getToFacility(S); !emptyInventory.
+!emptyInventory : getInventory(Inv) <- !emptyInventory(Inv).

// This way we can change the order and mitigate slow inventory updates
+!emptyInventory([]).
+!emptyInventory([Item|Items]) : getInventory(Inv) & not contains(Item, Inv) <- !emptyInventory(Items).
+!emptyInventory([map(Item, Amount) | Items]) : append(Items, [map(Item, Amount)], NewItems) <-
    !doAction(store(Item, Amount));
    !emptyInventory(NewItems).

+!gatherRole: gather(X) & X <-
    getResourceNode(F);
    getFacilityName(F, N);
    getCoords(F, Lat, Lon);
    !getToLocation(N, Lat, Lon);
    getItemVolume(F, V);
    !gatherUntilFull(V);
    getMainStorageFacility(S);
    !getToFacility(S);
    !emptyInventory;
    !gatherRole.

+!assembleItemM(Item, Quantity) <-
    haveItem(Item, Quantity, Yes);
    if (not Yes) {
        requestHelp;
        !doAction(assemble(Item));
        !assembleItemM(Item, Quantity);
    }.

+!getItemsToBuildItem(Item, Q) <-
    getRequiredItems(Item, Q, Items);
    !getItems(Items).

+!builderRole: assister <-
    getWorkShop(W);
    !getToFacility(W);
    .wait(100); // dirty fix (wait for requestHelp calls to complete)
    getMainTruckName(T);
    !doAction(assist_assemble(T));
    !builderRole.

+!builderRole: builder(X) & X & myRole("truck") <-
    getWorkShop(W);
    getMainStorageFacility(S);
    !getToFacility(S);
    somethingToBuild(Y);
    if (Y) {
        getItemToBuild(Item, Quantity);

        // Below is a 5 step plan to build anything !!!
        // 1. Take needed items out
        !getItemsToBuildItem(Item, Quantity);
        // 2. go to workshop
        !getToFacility(W);
        // 3. assemble
        !assembleItemM(Item, Quantity);
        // 4. go to storage
        !getToFacility(S);
        // 5. empty inventory
        !emptyInventory;

    } else {
        .print("Currently nothing to build");
        .wait({+step(_)});
    }
    !builderRole.