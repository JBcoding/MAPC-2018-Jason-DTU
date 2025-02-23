// Rules

maxSpeed(S)		:- myRole(Role) & role(Role, S, _, _, _).
maxLoad(L)		:- myRole(Role) & role(Role, _, L, _, _).
maxBattery(C)	:- myRole(Role) & role(Role, _, _, C, _).
canUseTool(T)	:- .print("can use Tool").
routeDuration(D)	:- routeLength(L) & speed(S) & D = math.ceil(L / S).

chargeThreshold(X) :- currentBattery(C) & X = 0.35 * C.
remainingCapacity(C) :- currentCapacity(M) & load(L) & C = M - L.
canMove :- charge(X) & X >= 1.

// Check facility type
isChargingStation(F)	:- .substring("chargingStation", F).
isWorkshop(F)			:- .substring("workshop", F).
isStorage(F)			:- .substring("storage",  F).
isShop(F)				:- .substring("shop",     F).
isResourceNode(F)	    :- .substring("node", F).
isWell(F)	            :- .substring("well", F).

// Check if agent is in this type of facility
inChargingStation 	:- inFacility(F) & isChargingStation(F).
inWorkshop 			:- inFacility(F) & isWorkshop(F).
inStorage 			:- inFacility(F) & isStorage(F).
inShop	    		:- inFacility(F) & isShop(F).
inShop(F)			:- inFacility(F) & inShop.
inResourceNode      :- inFacility(F) & isResourceNode(F).
inWell              :- inFacility(F) & isWell(F).
inFacility          :- inFacility(F) & F \== "none".
inOwnWell           :- inWell & inOwnWell(X) & X.
inEnemyWell         :- inWell & not inOwnWell.

atPeriphery         :- atPeriphery(X) & X.

enoughMoneyForWell  :- enoughMoneyForWell(X) & X.
build               :- build(X) & X.
destroy             :- destroy(X) & X.
assister            :- builder(X) & X & not myRole("truck").

contains(map(Item, X), [map(Item, Y) | _]) 	:- X <= Y. 		// There is a .member function, but we need to unwrap the objects
contains(Item, [_ | Inventory]) 		    :- contains(Item, Inventory).

fullCharge :- charge(X) & currentBattery(X).

enoughCharge :- routeLength(L) & enoughCharge(L).
enoughCharge(L) :- speed(S) & charge(C) & chargeThreshold(Threshold) & 
				Steps = math.ceil(L / S) & Steps <= (C - Threshold).

enoughBattery :- routeLength(L) & enoughBattery(L).
enoughBattery(L) :- speed(S) & currentBattery(C) & chargeThreshold(Threshold) &
				Steps = math.ceil(L / S) & Steps <= (C - Threshold).
				
getInventory(Inventory)			:- .my_name(Me) & getInventory(Me, Inventory).
getInventory(Agent, Inventory) 	:- jia.getInventory(Agent, Inventory).

hasItems(Items) 		:- .my_name(Me) & hasItems(Me, Items).
hasItems(Agent, Items) 	:- jia.hasItems(Agent, Items).

hasBaseItems(Items) 		:- .my_name(Me) & hasBaseItems(Me, Items).
hasBaseItems(Agent, Items) 	:- jia.hasBaseItems(Agent, Items).
