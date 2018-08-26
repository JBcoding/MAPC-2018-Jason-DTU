!init.

+!init : .my_name(Me) <-
	makeArtifact("EIArtifact", "env.EIArtifact", [], Id);
	makeArtifact("ItemArtifact", "info.ItemArtifact", [], _);
	makeArtifact("FacilityArtifact", "info.FacilityArtifact", [], _);
	makeArtifact("StaticInfoArtifact", "info.StaticInfoArtifact", [], _);
	makeArtifact("DynamicInfoArtifact", "info.DynamicInfoArtifact", [], _);
	makeArtifact("JobArtifact", "info.JobArtifact", [], _);
	makeArtifact("TaskArtifact", "cnp.TaskArtifact", [], TaskId);
	focus(Id);
	focus(TaskId).
	
+reset <- 
	for (assembleRequest(_, _, _, _, _, CnpId)) { clearAssemble(CnpId); };
	for (retrieveRequest(_, _, _, CnpId)) { clearRetrieve(CnpId); };
	-reset.
	