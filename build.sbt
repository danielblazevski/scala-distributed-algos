
// setup for multiproject build
val commonSettings = Seq(
  name := "distributed algorithms fun",
  version := "1.0",
  scalaVersion := "2.10.5",
  resolvers += "Typesafe Repository" at "http://repo.typesafe.com/typesafe/releases/",
  libraryDependencies += "com.typesafe.akka" %% "akka-actor" % "2.1.1"
)

lazy val electionLeader = (project in file("election-leader-ring"))
  .settings(commonSettings)
  .settings(
    name := "election"
  )