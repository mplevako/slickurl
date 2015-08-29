version := "2.0"

scalaVersion := "2.11.7"

scalacOptions in GlobalScope ++= Seq("encoding", "UTF-8", "-language:postfixOps", "-deprecation")

libraryDependencies ++= {
  val akkaVersion = "2.3.12"
  val sprayVersion = "1.3.3"
  val slf4jVersion: String = "1.7.12"
  Seq(
    "postgresql"          %   "postgresql"        % "9.1-901-1.jdbc4",
    "com.zaxxer"          %   "HikariCP-java6"    % "2.3.7",
    "com.typesafe.slick"  %%  "slick"             % "3.0.2",
    "org.json4s"          %%  "json4s-native"     % "3.2.11",
    "io.spray"            %%  "spray-can"         % sprayVersion,
    "io.spray"            %%  "spray-routing"     % sprayVersion,
    "com.typesafe.akka"   %%  "akka-actor"        % akkaVersion,
    "com.typesafe.akka"   %%  "akka-cluster"      % akkaVersion,
    "com.typesafe.akka"   %%  "akka-contrib"      % akkaVersion,
    "com.typesafe.akka"   %%  "akka-testkit"      % akkaVersion         % Test,
    "io.spray"            %%  "spray-testkit"     % sprayVersion        % Test,
    "org.specs2"          %%  "specs2"            % "2.4.2"             % Test,
    "com.h2database"      %   "h2"                % "1.4.188"           % Test,
    "org.slf4j"           %   "slf4j-api"         % slf4jVersion,
    "org.slf4j"           %   "log4j-over-slf4j"  % slf4jVersion,
    "org.slf4j"           %   "slf4j-simple"      % slf4jVersion
  )
}
    