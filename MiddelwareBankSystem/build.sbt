name := "ExchangeRateServer"

version := "0.1"

scalaVersion := "2.12.8"
scalacOptions += "-Ypartial-unification"

PB.targets in Compile := Seq(
  scalapb.gen() -> (sourceManaged in Compile).value
)

val finagleVersion = "19.4.0"

libraryDependencies ++= Seq(
  "org.typelevel" %% "cats-core" % "1.6.0",
  "org.typelevel" %% "cats-effect" % "1.2.0",
  
  "io.grpc" % "grpc-netty" % scalapb.compiler.Version.grpcJavaVersion,
  "com.thesamet.scalapb" %% "scalapb-runtime-grpc" % scalapb.compiler.Version.scalapbVersion,
  "com.thesamet.scalapb" %% "scalapb-runtime" % scalapb.compiler.Version.scalapbVersion % "protobuf",
  
  "com.twitter" %% "finagle-thriftmux" % finagleVersion,
  "com.twitter" %% "scrooge-core" % finagleVersion,
  
  "org.slf4j" % "slf4j-log4j12" % "1.7.5",
  
  "com.typesafe.akka" %% "akka-actor" % "2.5.22"
)
