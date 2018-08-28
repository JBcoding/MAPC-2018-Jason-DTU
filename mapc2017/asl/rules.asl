// Role properties
speed(S)		:- role(_, S, _, _, _).
maxLoad(L)		:- role(_, _, L, _, _).
maxCharge(C)	:- role(_, _, _, C, _).
tools(T)		:- role(_, _, _, _, T).
capacity(C) 	:- maxLoad(M) & load(L) & C = M - L.
// Is facility type
isChargingStation(F)	:- .substring("chargingStation", 	F).
isWorkshop(F)			:- .substring("workshop", 			F).
isResourceNode(F)		:- .substring("resourceNode", 		F).
isStorage(F)			:- .substring("storage",  			F).
isShop(F)				:- .substring("shop",     			F).
// In facility
inChargingStation 	:- facility(F) & isChargingStation(F).
inWorkshop 			:- facility(F) & isWorkshop(F).
inResourceNode 		:- facility(F) & isResourceNode(F).
inStorage 			:- facility(F) & isStorage(F).
inShop	    		:- facility(F) & isShop(F).
inShop(F)			:- facility(F) & isShop(F).
// Charge utility
canMove 			:- charge(X) & X >= 10.
chargeThreshold(X) 	:- maxCharge(C) & X = 0.35 * C.
enoughCharge(F) 	:- charge(C) & chargeThreshold(T)
					 & getDurationToFacility(F, D) & D <= (C - T) / 10.					 
// Agent
atLocation	(Lat, Lon)			:- .my_name(Me) & mapc2017.jia.agent.atLocation(Me, Lat, Lon).
getInventory(Inventory)			:- .my_name(Me) & mapc2017.jia.agent.getInventory(Me, Inventory).
hasBaseItems(Items) 			:- .my_name(Me) & mapc2017.jia.agent.hasBaseItems(Me, Items).
hasItems	(Items) 			:- .my_name(Me) & mapc2017.jia.agent.hasItems(Me, Items).
hasTools	(Tools)				:- .my_name(Me) & mapc2017.jia.agent.hasTools(Me, Tools).
hasAmount	(Item, Amount)		:- .my_name(Me) & mapc2017.jia.agent.hasAmount(Me, Item, Amount).
buyAmount	(Item, Need, Buy)	:- inShop(F)    & getAvailableAmount(F, Item, Available) 
								 & hasAmount(Item, Has) & .min([Need - Has, Available], Buy).
// Facility
getAlternativeShop(I, A, S)		:- inShop(F)    & mapc2017.jia.facility.getAlternativeShop		(F, I, A, S).
getAvailableAmount(S, I, A)		:- 				  mapc2017.jia.facility.getAvailableAmount		(S, I, A).
getClosestFacility(F, T, C)		:- .term2string(Term, F) & mapc2017.jia.facility.getClosestFacility	(Term, T, C).
getClosestFacility(T, C)		:- .my_name(Me) & mapc2017.jia.facility.getClosestFacility		(Me, T, C).
getClosestShopSelling(I, S)		:- .my_name(Me) & mapc2017.jia.facility.getClosestShopSelling	(Me, I, S).
getDurationToFacility(F, D)		:- .my_name(Me) & mapc2017.jia.facility.getDurationToFacility	(Me, F, D).
getFacilityLocation(F, Lat, Lon):- 				  mapc2017.jia.facility.getFacilityLocation		(F, Lat, Lon).
getResourceNode(F)				:- .my_name(Me) & mapc2017.jia.facility.getResourceNode			(Me, F).
getDeliveredItems(F, I)			:-				  mapc2017.jia.facility.getDeliveredItems		(F, I).
// Items
getBaseItems	(L, B)			:- .list(L)   	& mapc2017.jia.items.getBaseItems	(L, B).
getLoadReq		(L, R)			:- .list(L)   	& mapc2017.jia.items.getLoadReq	(L, R).
getBaseVolume	(L, V)			:- .list(L)   	& mapc2017.jia.items.getBaseVolume(L, V).
getReqItems		(I, R)			:- .string(I) 	& mapc2017.jia.items.getReqItems	(I, R).
getVolume		(I, V)			:- .string(I) 	& mapc2017.jia.items.getVolume	(I, V).
getLeastAvailableItems(S, I)	:- .my_name(Me) & mapc2017.jia.items.getLeastAvailableItems(Me, S, I).
// Util
getRandomLocation		(Lat, Lon)	:- 			mapc2017.jia.util.getRandomLocation(Lat, Lon).
getRandomCenterLocation	(Lat, Lon)	:- 			mapc2017.jia.util.getRandomCenterLocation(Lat, Lon).