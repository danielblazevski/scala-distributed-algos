
// setup for multiproject build
val commonSettings = Seq(
  name := "distributed algorithms fun",
  version := "1.0",
  scalaVersion := "2.12.1",
  libraryDependencies ++= Seq("com.twitter" %% "finagle-http" % "17.10.0",
    "org.scalatest" %% "scalatest" % "3.0.0" % Test,
    "org.mockito" % "mockito-all" % "2.0.2-beta" % Test
  )
)

lazy val electionLeader : Project = (project in file("election-leader-ring"))
  .settings(commonSettings)
