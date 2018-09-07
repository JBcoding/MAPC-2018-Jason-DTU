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
+!getToFacility(F) : not enoughCharge & not isChargingStation(F) <- !charge; !getToFacility(F).
+!getToFacility(F) 													<- !doAction(goto(F)); 	!getToFacility(F).

// Meant for getting to resource nodes
+!getToLocation(F, _, _) : inFacility(F).
+!getToLocation(F, Lat, Lon) : not canMove <- !doAction(recharge); !getToLocation(F, Lat, Lon).
+!getToLocation(F, Lat, Lon) : not enoughCharge & not isChargingStation(F) <- !charge; !getToLocation(F, Lat, Lon).
+!getToLocation(F, Lat, Lon) <- !doAction(goto(Lat, Lon)); !getToLocation(F, Lat, Lon).

+!charge : charge(X) & currentBattery(X).
+!charge : not canMove <- !doAction(recharge); !charge.
+!charge : inChargingStation <-
    !doAction(charge);
    !charge.
+!charge <-
	getClosestFacility("chargingStation", F);
	!getToFacility(F); 
	!charge.
