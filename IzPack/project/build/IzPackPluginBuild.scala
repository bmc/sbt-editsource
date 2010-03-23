import sbt._

class IzPackPluginProject(info: ProjectInfo) extends PluginProject(info)
{
    /* ---------------------------------------------------------------------- *\
                               Dependencies
    \* ---------------------------------------------------------------------- */

    val izPack = "org.codehaus.izpack" % "izpack-standalone-compiler" % "4.3.2"

    /* ---------------------------------------------------------------------- *\
                                Publishing
    \* ---------------------------------------------------------------------- */

    // Enable publishing in this subproject, with no dependencies on other
    // subprojects. For more details, see the relevent SBT wiki section:
    // http://code.google.com/p/simple-build-tool/wiki/SubProjects

    override def deliverProjectDependencies = Nil
}
