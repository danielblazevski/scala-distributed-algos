## Leader election using Finagle

This is an implementation of the hirschberg and Sinclair algorithm for
leader election in a ring using [Finagle](https://twitter.github.io/finagle/) for RPCs.  

This was done as a hack project and not meant to be used in production anywhere.  
It currently has all servers and clients setup on localhost on different ports on `localhost`

To run, define a ring of servers via

`$ sbt "electionLeader/runMain election.electionLeaderFinagle PP ID"`
where `PP` is the ending of the port of the server that will run on port `80PP` and `ID` is an integer for
the Id of the server. 

For example, to setup a ring of three serves on ports 8000, 8001, 8002 with ids 10, 11, 12 run

```
$ sbt "electionLeader/runMain election.electionLeaderFinagle 00 10"
$ sbt "electionLeader/runMain election.electionLeaderFinagle 01 11"
$ sbt "electionLeader/runMain election.electionLeaderFinagle 02 12"
```
in three different terminal sessions.  
