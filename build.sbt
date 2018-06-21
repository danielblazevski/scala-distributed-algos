
// setup for multiproject build
val commonSettings = Seq(
  name := "distributed algorithms fun",
  version := "1.0",
  scalaVersion := "2.12.3",
  libraryDependencies ++= Seq("com.twitter" %% "finagle-http" % "17.10.0")
)

lazy val electionLeader : Project = (project in file("election-leader-ring"))
  .settings(commonSettings)