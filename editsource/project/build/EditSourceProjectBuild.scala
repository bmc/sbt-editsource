import sbt._

class EditSourcePluginProject(info: ProjectInfo) extends PluginProject(info)
{
    // Enable publishing in this subproject, with no dependencies on other
    // subprojects. For more details, see the relevent SBT wiki section:
    // http://code.google.com/p/simple-build-tool/wiki/SubProjects

    override def deliverProjectDependencies = Nil
}
