name := "versioned"

organization := "net.noerd"

version := "0.1-SNAPSHOT"

scalaVersion := "2.9.1"

scalacOptions := Seq( "-unchecked", "-deprecation" )

mainClass in (Compile, run) := Some( "net.noerd.versioned.Main" )

seq(webSettings :_*)

libraryDependencies ++= Seq(
    "io.netty" % "netty" % "3.3.1.Final",
    "org.scalatra" %% "scalatra" % "2.0.3",
    "javax.servlet" % "servlet-api" % "3.0-alpha-1" % "provided",
    "org.eclipse.jetty" % "jetty-webapp" % "8.0.4.v20111024" % "test;container",
    "net.liftweb" %% "lift-json" % "2.4",    
    "org.scalatest" %% "scalatest" % "1.6.1" % "test"
)

resolvers += "Sonatype OSS Snapshots" at "http://oss.sonatype.org/content/repositories/snapshots/"