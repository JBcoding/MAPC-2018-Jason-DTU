# Multi-Agent System for the MAPC 2018

The source code contains Jason-DTU's multi-agent system for the Multi-Agent Programming Contest 2018.
This has been developed using Jason, CArtAgO and Java. The contest can be found at https://multiagentcontest.org/2018/.

## Members 
* Jørgen Villadsen
* Mads Okholm Bjørn
* Andreas Halkjær From
* Thomas Søren Henney
* John Bruntse Larsen

## Dependencies 
Our system uses Jason and CArtAgO, which are not included in the source files. These can be found at the links below.  

* [Download Jason](https://sourceforge.net/projects/jason/files/)
* [Download CArtAgO](https://sourceforge.net/projects/cartago/files/cartago/2.0/cartago-2.0.1.zip/download)

Get the libraries from [official releases](https://github.com/agentcontest/massim/releases). (Our system has been tested with version 1.7).
These should be put in the libs/agentcontest/ directory, if you want to use our build files. 

If the osm files for the map are missing from our source code, they can be found at the MAPC 2018's official repository: [official osm files](https://github.com/agentcontest/massim/tree/master/server/osm). These should be put in the osm directory. 

## How to build and run
The project can be build using the provided **ant** build file:

```
ant -f ./BuildRunner.xml
```

Or to only compile the source code:

```
ant -f ./BuildRunner.xml compile
```

The resulting `Runner.jar` runs both the server and multi-agent system.

### Config files

We have included configuration files, *conf/SampleConfig.json*, which should get you started.
For more information, please resort to [the official MAPC 2017 documention](https://github.com/agentcontest/massim/blob/master/docs/eismassim.md).

