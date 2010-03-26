import sbt._

class Plugins(info: ProjectInfo) extends PluginDefinition(info)
{
    val izpackPlugin = "org.clapper" % "sbt-izpack-plugin" % "0.2.1"
}
