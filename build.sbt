
version := "1.0"

scalaVersion := "2.11.2"

libraryDependencies ++= {
  val akkaVersion = "2.3.6"
  val sprayVersion = "1.3.1"
  val slf4jVersion: String = "1.7.7"
  Seq(
    "postgresql"          %   "postgresql"        % "9.1-901-1.jdbc4",
    "com.typesafe.slick"  %%  "slick"             % "2.1.0",
    "org.json4s"          %%  "json4s-native"     % "3.2.10",
    "io.spray"            %%  "spray-can"         % sprayVersion,
    "io.spray"            %%  "spray-routing"     % sprayVersion,
    "com.typesafe.akka"   %%  "akka-actor"        % akkaVersion,
    "com.typesafe.akka"   %%  "akka-cluster"      % akkaVersion,
    "com.typesafe.akka"   %%  "akka-contrib"      % akkaVersion,
    "com.typesafe.akka"   %%  "akka-testkit"      % akkaVersion         % "test",
    "io.spray"            %%  "spray-testkit"     % sprayVersion        % "test",
    "org.specs2"          %%  "specs2"            % "2.4.2"             % "test",
    "com.h2database"      %   "h2"                % "1.4.181"           % "test",
    "org.slf4j"           %   "slf4j-api"         % slf4jVersion,
    "org.slf4j"           %   "log4j-over-slf4j"  % slf4jVersion,
    "org.slf4j"           %   "slf4j-simple"      % slf4jVersion
  )
}
    