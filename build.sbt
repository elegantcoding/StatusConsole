name := "status-console"

version := "0.1.0"

organization := "com.elegantcoding"

scalaVersion := "2.11.1"

scalacOptions ++= Seq("-encoding", "UTF-8", "-deprecation", "-unchecked", "-feature")

publishTo := Some(Resolver.file("file",  new File( "c:/usr/elegantcoding.github.io/repo/releases" )) )

libraryDependencies ++= Seq(
  "org.scalatest" %% "scalatest" % "2.2.0" % "test",
  "com.googlecode.lanterna" % "lanterna" % "2.1.8",
  "commons-io" % "commons-io" % "2.4"
)

//seq(lsSettings :_*)

//(LsKeys.tags in LsKeys.lsync) := Seq("rdf", "ntriples", "nquads")

//(description in LsKeys.lsync) :=
//  "RDF processor console support."
//
//(externalResolvers in LsKeys.lsync) := Seq(
//  "elegantcoding releases" at "http://elegantcoding.github.io/repo/releases")
//
//instrumentSettings
