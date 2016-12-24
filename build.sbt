version := "2.0"

scalaVersion := "2.11.8"

scalacOptions in Compile ++= Seq("-target:jvm-1.8", "-encoding", "UTF-8", "-language:postfixOps", "-deprecation")

libraryDependencies ++= {
  val akkaVersion = "2.4.16"
  val sprayVersion = "1.3.4"
  val slickVersion = "3.1.1"
  val slf4jVersion: String = "1.7.22"
  Seq(
    "org.postgresql"      %   "postgresql"         % "9.4.1212",
    "com.zaxxer"          %   "HikariCP"           % "2.5.1",
    "com.typesafe.slick"  %%  "slick"              % slickVersion,
    "com.typesafe.slick"  %%  "slick-hikaricp"     % slickVersion,
    "org.json4s"          %%  "json4s-native"      % "3.5.0",
    "com.pauldijou"       %%  "jwt-json4s-native"  % "0.9.2",
    "io.spray"            %%  "spray-can"          % sprayVersion,
    "io.spray"            %%  "spray-routing"      % sprayVersion,
    "com.typesafe.akka"   %%  "akka-actor"         % akkaVersion,
    "com.typesafe.akka"   %%  "akka-cluster"       % akkaVersion,
    "com.typesafe.akka"   %%  "akka-contrib"       % akkaVersion,
    "org.slf4j"           %   "slf4j-api"          % slf4jVersion,
    "org.slf4j"           %   "log4j-over-slf4j"   % slf4jVersion,
    "org.slf4j"           %   "slf4j-simple"       % slf4jVersion,
    "com.typesafe.akka"   %%  "akka-testkit"       % akkaVersion    % Test,
    "io.spray"            %%  "spray-testkit"      % sprayVersion   % Test,
    "org.specs2"          %%  "specs2"             % "2.3.13"       % Test,
    "com.h2database"      %   "h2"                 % "1.4.193"      % Test
  )
}
    