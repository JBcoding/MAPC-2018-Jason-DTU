// Plans
+!register <- register.
-!register <- .wait(100); !register.

+!focusArtifact(Name) <- lookupArtifact(Name, Id); focus(Id).
+!focusArtifacts : .my_name(Me) & .term2string(Me, Name) <-
	!focusArtifact("TaskArtifact");
	!focusArtifact("EIArtifact");
	makeArtifact(Name, "info.AgentArtifact", [], _);
	!focusArtifact(Name);
	.print("Successfully focused artifacts").
-!focusArtifacts <- .wait(500); !focusArtifacts.
