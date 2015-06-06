addSbtPlugin("com.typesafe.play" % "interplay" % sys.props.getOrElse("interplay.version", "1.0.1"))

libraryDependencies ++= Seq(
  "com.typesafe.play" %% "play-doc" % "1.3.0-SNAPSHOT",
  "com.typesafe" %% "rp-docs-models" % "0.1-SNAPSHOT"
)