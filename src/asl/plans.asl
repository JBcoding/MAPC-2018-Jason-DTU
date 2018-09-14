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
        //.print("Done building well");
    }.
+!buildWell :
    atPeriphery
    <-
    getMoney(Money);
    bestWellType(Money, WellType);
    if (not (WellType == "none")) {
        !doAction(build(WellType));
        !buildWell;
    } else {
        .print("Not enough massium to build any well");
    }.
+!buildWell
    <-
    closestPeriphery(Lat, Lon);
    !getToPeripheryLocation(Lat, Lon);
    !buildWell.

+!dismantleOwnWell : inOwnWell <- !doAction(dismantle); !dismantleOwnWell.

+!dismantleEnemyWell : inEnemyWell <- !doAction(dismantle); !dismantleEnemyWell.
+!dismantleEnemyWell
    <-
    getEnemyWell(F, Lat, Lon);
    // TODO: What to do if there is no known enemy well?
    !getToLocation(F, Lat, Lon);
    !dismantleEnemyWell.

+!upgrade(Type) :
    inShop
    <-
    getMoney(Money);
    getUpgradePrice(Type, Price);
// TODO: Possibly decide whether upgrading is worth the cost
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

+!buyItems([]).
+!buyItems([map(Item, 	   0)|Items]) <- !buyItems(Items).
+!buyItems([map(Item, Amount)|Items]) : inShop(Shop) <-
	getAvailableAmount(Item, Amount, Shop, AmountAvailable);
	!doAction(buy(Item, AmountAvailable));
	!buyItems(Items);
	!buyItems([map(Item, Amount - AmountAvailable)]).

+!deliverItems(TaskId, Facility) <-
	!getToFacility(Facility);
 	!doAction(deliver_job(TaskId)).

+!assembleItems([]).
+!assembleItems([map(	_, 		0) | Items]) <- !assembleItems(Items).
+!assembleItems([map(Item, Amount) | Items]) <-
	getRequiredItems(Item, ReqItems);
	!assembleItem(Item, ReqItems);
	!assembleItems([map(Item, Amount - 1) | Items]).

// Recursively assemble required items
+!assembleItem(	  _, 	   []).
+!assembleItem(Item, ReqItems) <-
	!assembleItems(ReqItems);
	!doAction(assemble(Item)).

+!assistAssemble(Agent) : load(0) | assembleComplete.
+!assistAssemble(Agent) <- !doAction(assist_assemble(Agent)); !assistAssemble(Agent).

+!retrieveTools([]).
+!retrieveTools([Tool | Tools]) : have(Tool) 	<- !retrieveTools(Tools).
+!retrieveTools([Tool | Tools]) 				<- !retrieveTool(Tool);	!retrieveTools(Tools).
+!retrieveTool(Tool) : canUseTool(Tool) 		<- !retrieveItems([map(Tool, 1)]).
+!retrieveTool(Tool) 							<- .print("Can not use ", Tool). // Need help from someone that can use this tool

+!getToFacility(F) : inFacility(F).
+!getToFacility(F) : not canMove									<- !doAction(recharge); !getToFacility(F).
+!getToFacility(F) : not enoughCharge & not isChargingStation(F)    <- !charge; !getToFacility(F).
+!getToFacility(F) 													<- !doAction(goto(F)); 	!getToFacility(F).

// Meant for getting to resource nodes and wells
+!getToLocation(F, _, _) : inFacility(F).
+!getToLocation(F, Lat, Lon) : not canMove <- !doAction(recharge); !getToLocation(F, Lat, Lon).
+!getToLocation(F, Lat, Lon) : not enoughCharge & not isChargingStation(F) <- !charge; !getToLocation(F, Lat, Lon).
+!getToLocation(F, Lat, Lon) <- !doAction(goto(Lat, Lon)); !getToLocation(F, Lat, Lon).

// Gets close to this location
+!getToPeripheryLocation(Lat, Lon) : atPeriphery.
+!getToPeripheryLocation(Lat, Lon) : not canMove <- !doAction(recharge); !getToPeripheryLocation(Lat, Lon).
+!getToPeripheryLocation(Lat, Lon) : not enoughCharge <- !charge; !getToPeripheryLocation(Lat, Lon).
+!getToPeripheryLocation(Lat, Lon) <- !doAction(goto(Lat, Lon)); !getToPeripheryLocation(Lat, Lon).

+!charge : charge(X) & currentBattery(X).
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












+!gatherUntilFull(V) : remainingCapacity(C) & C >= V <- !doAction(gather); !gatherUntilFull(V).
+!gatherUntilFull(V).

+!gatherRole: gather(X) & X <-
    getResourceNode(F);
    getFacilityName(F, N);
    getCoords(F, Lat, Lon);
    !getToLocation(N, Lat, Lon);
    getItemVolume(F, V);
    !gatherUntilFull(V);
    getMainStorageFacility(S);
    !getToFacility(S);
    getItemNameAndQuantity(Item, Quantity);
    !doAction(store(Item, Quantity));
    !gatherRole.