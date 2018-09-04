// Rules
speed(S)		:- myRole(Role) & role(Role, S, _, _, _).
maxLoad(L)		:- myRole(Role) & role(Role, _, L, _, _).
maxCharge(C)	:- myRole(Role) & role(Role, _, _, C, _).
canUseTool(T)	:- myRole(Role) & role(Role, _, _, _, Tools) & .member(T, Tools).
routeDuration(D)	:- routeLength(L) & speed(S) & D = math.ceil(L / S).

chargeThreshold(X) :- maxCharge(C) & X = 0.35 * C.
capacity(C) :- maxLoad(M) & load(L) & C = M - L.
canMove :- charge(X) & X >= 10.

// Check facility type
isChargingStation(F)	:- .substring("chargingStation", F).
isWorkshop(F)			:- .substring("workshop", F).
isStorage(F)			:- .substring("storage",  F).
isShop(F)				:- .substring("shop",     F).

// Check if agent is in this type of facility
inChargingStation 	:- inFacility(F) & isChargingStation(F).
inWorkshop 			:- inFacility(F) & isWorkshop(F).
inStorage 			:- inFacility(F) & isStorage(F).
inShop	    		:- inFacility(F) & isShop(F).
inShop(F)			:- inFacility(F) & inShop.

contains(map(Item, X), [map(Item, Y) | _]) 	:- X <= Y. 		// There is a .member function, but we need to unwrap the objects
contains(Item, [_ | Inventory]) 			:- contains(Item, Inventory). 

enoughCharge :- routeLength(L) & enoughCharge(L).
enoughCharge(L) :- speed(S) & charge(C) & chargeThreshold(Threshold) & 
				Steps = math.ceil(L / S) & Steps <= (C - Threshold) / 10.
				
getInventory(Inventory)			:- .my_name(Me) & getInventory(Me, Inventory).
getInventory(Agent, Inventory) 	:- jia.getInventory(Agent, Inventory).

hasItems(Items) 		:- .my_name(Me) & hasItems(Me, Items).
hasItems(Agent, Items) 	:- jia.hasItems(Agent, Items).

hasBaseItems(Items) 		:- .my_name(Me) & hasBaseItems(Me, Items).
hasBaseItems(Agent, Items) 	:- jia.hasBaseItems(Agent, Items).
