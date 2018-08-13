# Multi-Agent System for the MAPC 2017

The source code contains Jason-DTU's multi-agent system for the Multi-Agent Programming Contest 2017. This has been developed using Jason, CArtAgO and Java. The contest can be found at https://multiagentcontest.org/2017/.

## Members 
* Jørgen Villadsen
* Oliver Fleckenstein
* Helge Hatteland
* John Bruntse Larsen

## Dependencies 
Our system uses Jason and CArtAgO, which are not included in the source files. These can be found at the links below.  
* [Download Jason](https://sourceforge.net/projects/jason/files/)
* [Download CArtAgO](https://sourceforge.net/projects/cartago/files/cartago/2.0/cartago-2.0.1.zip/download)

Get the libraries from [official releases](https://github.com/agentcontest/massim/releases). (Our system have been tested with version 1.7). These should be put in the libs/agentcontest/ directory, if you want to use our build files. 

If the osm files for the map are missing from our source code, they can be found at the MAPC 2017's official repository: [official osm files](https://github.com/agentcontest/massim/tree/master/server/osm). These should be put in the osm directory. 

## How to build and run
The project can be build using the provided **ant** build files 

```
ant -f ./BuildRunner.xml
```

We have provided three build files: 
* BuildMAS.xml, which will create *Multiagent.jar*, an executable to run the multi-agent system by itself.
* BuildServer.xml, which will create *Server.jar*, an executable to run the server hosting the scenario.
* BuildRunner.xml, which will create *Runner.jar*, an executable to run both the server and the multi-agent system. 

### Config files
We have included configuration files, *conf/SampleConfig.json*, which should get you started. For more information, please resort to [the official MAPC 2017 documention](https://github.com/agentcontest/massim/blob/master/docs/eismassim.md).
