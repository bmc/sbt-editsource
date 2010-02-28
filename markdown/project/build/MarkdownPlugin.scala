import sbt._

class MarkdownPluginProject(info: ProjectInfo) extends PluginProject(info)
{
    val rhino = "rhino" % "js" % "1.7R2"
}
