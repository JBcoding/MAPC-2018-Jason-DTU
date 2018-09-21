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

//+!goToResourceNode(Lat, Lon)
//+!goToWell(Lat, Lon)

+!buildWell :
    inOwnWell &
    inFacility(F)
    <-
    wellHasFullIntegrity(F, X);
    if (not X) {
        !doAction(build);
        !buildWell;
    }
    else {
        stopBuilding;
        .print("Done building well");
    }.
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

+!dismantleEnemyWell : inEnemyWell <- !doAction(dismantle); !dismantleEnemyWell; markWellDestroyed.
+!dismantleEnemyWell
    <-
    getEnemyWell(F, Lat, Lon);
    if (not (F == "none")) {
        !getToLocation(F, Lat, Lon);
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

+!deliverItems(TaskId, Facility) <-
	!getToFacility(Facility);
	.print("At facility. Delivering.");
 	!doAction(deliver_job(TaskId)).

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

+!retrieveTools([]).
+!retrieveTools([Tool | Tools]) : have(Tool) 	<- !retrieveTools(Tools).
+!retrieveTools([Tool | Tools]) 				<- !retrieveTool(Tool);	!retrieveTools(Tools).
+!retrieveTool(Tool) : canUseTool(Tool) 		<- !retrieveItems([map(Tool, 1)]).
+!retrieveTool(Tool) 							<- .print("Can not use ", Tool). // Need help from someone that can use this tool

+!getToFacility(F) : build <- !buildWell; !getToFacility(F).
+!getToFacility(F) : inFacility(F).
+!getToFacility(F) : not canMove									<- !doAction(recharge); !getToFacility(F).
+!getToFacility(F) : not enoughCharge & not isChargingStation(F)    <- !charge; !getToFacility(F).
+!getToFacility(F) 													<- !doAction(goto(F)); 	!getToFacility(F).

// Meant for getting to resource nodes and wells
+!getToLocation(F, Lat, Lon) : build <- !buildWell; !getToLocation(F, Lat, Lon).
+!getToLocation(F, _, _) : inFacility(F).
+!getToLocation(F, Lat, Lon) : not canMove <- !doAction(recharge); !getToLocation(F, Lat, Lon).
+!getToLocation(F, Lat, Lon) : not enoughCharge & not isChargingStation(F) <- !charge; !getToLocation(F, Lat, Lon).
+!getToLocation(F, Lat, Lon) <- !doAction(goto(Lat, Lon)); !getToLocation(F, Lat, Lon).

// Gets close to this location
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

+!scoutt : scout(X) & X <- getClosestUnexploredPosition(Lat, Lon); /* .print("Scouting"); */ !scout(Lat, Lon).
+!scoutt.

+!scout(Lat, Lon) : scout(X) & X & not canMove <- !doAction(recharge); !scout(Lat, Lon).
+!scout(Lat, Lon) : scout(X) & X & not enoughCharge <- !charge; !scout(Lat, Lon).
+!scout(Lat, Lon) : scout(X) & X <- !doAction(goto(Lat, Lon)); 	!scoutt.
+!scout(_, _) : scout(X) & not X.

+!gatherUntilFull(V) : build <- !buildWell; !gatherUntilFull(V).
+!gatherUntilFull(V) : remainingCapacity(C) & C >= V <- !doAction(gather); !gatherUntilFull(V).
+!gatherUntilFull(V).

+!emptyInventory : build <- !buildWell; !emptyInventory.
+!emptyInventory : inStorage & load(L) & L >= 1 <-
    getItemNameAndQuantity(Item, Quantity);
    if (not Quantity == -1) {
        !doAction(store(Item, Quantity));
        !emptyInventory;
    }.
+!emptyInventory.

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
    .print("HELLLLLLLLLLLLLLLLLLL____OLLLLLLO");
    haveItem(Item, X, Quantity);
    .print("HELLLLLLLLLLLLLLLLLLL____OLLLLLLO");
    if (not X) {
        .print("HELLLLLLLLLLLLLLLLLLL____OLLLLLLO");
        requestHelp;
        !doAction(assemble(Item));
        !assembleItemM(Item, Quantity);
    }.

+!getItemsToBuildItem(Item, Q) <-
    getMissingItemToBuildItem(Item, ItemToRetrieve, Quantity);
    if (not Quantity == -1) {
        !doAction(retrieve(ItemToRetrieve, Q));
        !getItemsToBuildItem(Item, Q);
    }.

+!builderRole: builder(X) & X <-
    getMainTruckName(T);
    getWorkShop(W);
    isTruck(N);
    if (not N) {
        !getToFacility(W);
        .wait(100); // dirty fix
        !doAction(assist_assemble(T));
    } else {
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
    }
    !builderRole.