/*
  ---------------------------------------------------------------------------
  This software is released under a BSD license, adapted from
  http://opensource.org/licenses/bsd-license.php

  Copyright (c) 2010, Brian M. Clapper
  All rights reserved.

  Redistribution and use in source and binary forms, with or without
  modification, are permitted provided that the following conditions are
  met:

  * Redistributions of source code must retain the above copyright notice,
    this list of conditions and the following disclaimer.

  * Redistributions in binary form must reproduce the above copyright
    notice, this list of conditions and the following disclaimer in the
    documentation and/or other materials provided with the distribution.

  * Neither the names "clapper.org", "Era", nor the names of its
    contributors may be used to endorse or promote products derived from
    this software without specific prior written permission.

  THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS
  IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO,
  THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR
  PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR
  CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
  EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
  PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR
  PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
  LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
  NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
  SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
  ---------------------------------------------------------------------------
*/

package org.clapper.sbtplugins.izpack
{
    import scala.xml._
    import scala.collection.mutable.{ListBuffer, 
                                     Map => MutableMap,
                                     HashSet => MutableSet}
    import sbt._
    import java.io.File

    trait OperatingSystemConstraints
    {
        var operatingSystems = new MutableSet[String]

        def onlyFor(osNames: String*) = 
            for (os <- osNames)
                operatingSystems += os

        def operatingSystemsToXML =
            for (os <- operatingSystems) yield <os family={os}/>
    }

    trait Section
    {
        val SectionName: String

        def sectionToXML: Node

        var customXML: NodeSeq = NodeSeq.Empty

        def toXML =
        {
            // Append any custom XML to the end, as a series of child
            // elements.
            val node = sectionToXML
            val custom = customXML
            val allChildren = node.child ++ custom
            Elem(node.prefix, node.label, node.attributes, node.scope,
                 allChildren: _*)
        }

        protected def writeString(path: Path, str: String): Unit =
            writeString(path.toString, str)

        protected def writeString(path: String, str: String): Unit =
        {
            import java.io.FileWriter
            val writer = new FileWriter(path)
            writer.write(str + "\n")
            writer.flush
            writer.close
        }

        protected def emptyString(s: String) = (s == null) || (s.trim == "")

        protected def stringToOption(s: String) =
            if (emptyString(s)) None else Some(s)

        protected def optionToString(o: Option[String]) =
            if (o == None) "" else o.get

        protected def stringOptionToTextNode(o: Option[String], name: String) =
            o match
            {
                case None =>
                    new Comment("No " + name + " element")
                case Some(text) =>
                    Elem(null, name, Node.NoAttributes, TopScope, Text(text))
            }

        protected def maybeXML(name: String, create: Boolean): Node =
            maybeXML(name, create, Map.empty[String,String])

        protected def maybeXML(name: String,
                               create: Boolean,
                               attrs: Map[String, String]): Node =
        {
            def makeAttrs(attrs: List[(String, String)]): MetaData =
            {
                attrs match
                {
                    case (n, v) :: Nil =>
                        new UnprefixedAttribute(n, v, Node.NoAttributes)
                    case (n, v) :: tail => 
                        new UnprefixedAttribute(n, v, makeAttrs(tail))
                    case Nil =>
                        Null
                }
            }

            if (! create)
                new Comment("No " + name + " element")

            else
            {
                Elem(null, name, makeAttrs(attrs.elements.toList), TopScope,
                     Text(""))
            }
        }

        protected def yesno(b: Boolean): String = if (b) "yes" else "no"
    }

    /*-------------------------------------------------------------------*\
                                 Main Config Class
    \*-------------------------------------------------------------------*/

    /**
     */
    abstract class IzPackConfig(val workingInstallDir: Path, 
                                val log: Logger) extends Section
    {
        final val SectionName = "IzPackConfig"

        final val Windows = "windows"
        final val Darwin  = "darwin"
        final val MacOS   = Darwin
        final val MacOSX  = Darwin
        final val Unix    = "unix"
        final val OS = "os"

        private var theInfo: Option[Info] = None
        var languages: List[String] = Nil
        private var theResources: Option[Resources] = None
        private var thePackaging: Option[Packaging] = None
        private var theGuiPrefs: Option[GuiPrefs] = None
        private var thePanels: Option[Panels] = None
        private var thePacks: Option[Packs] = None

        private lazy val dateFormatter =
            new java.text.SimpleDateFormat("yyyy/MM/dd HH:mm:ss")

        private def setOnlyOnce[T <: Section](target: Option[T], 
                                              newValue: Option[T]): Option[T] =
        {
            (target, newValue) match
            {
                case (Some(t), None) =>
                    None

                case (None, None) =>
                    None

                case (Some(t), Some(v)) =>
                    throw new RuntimeException("You may specify only one " +
                                               t.SectionName + " object.")

                case (None, Some(v)) =>
                    newValue
            }
        }

        def info_=(i: Option[Info]): Unit =
            theInfo = setOnlyOnce(theInfo, i)
        def info: Option[Info] = theInfo

        def panels_=(p: Option[Panels]): Unit =
            thePanels = setOnlyOnce(thePanels, p)
        def panels: Option[Panels] = thePanels

        def resources_=(r: Option[Resources]): Unit =
            theResources = setOnlyOnce(theResources, r)
        def resources: Option[Resources] = theResources

        def packaging_=(p: Option[Packaging]): Unit =
            thePackaging = setOnlyOnce(thePackaging, p)
        def packaging: Option[Packaging] = thePackaging

        def guiprefs_=(g: Option[GuiPrefs]): Unit =
            theGuiPrefs = setOnlyOnce(theGuiPrefs, g)
        def guiprefs: Option[GuiPrefs] = theGuiPrefs

        def packs_=(p: Option[Packs]): Unit =
            thePacks = setOnlyOnce(thePacks, p)
        def packs: Option[Packs] = thePacks

        private def languagesToXML =
        {
            val langs = if (languages == Nil) List("eng") else languages
            <locale>
            {for (name <- languages) yield <langpack iso3={name}/>}
            </locale>
        }

        def sectionToXML =
        {
            val now = dateFormatter.format(new java.util.Date)

            <installation version="1.0">
            {new Comment("IzPack installation file.")}
            {new Comment("Generated by org.clapper SBT IzPack plugin: " + now)}
            {optionalSectionToXML(info, "Info")}
            {languagesToXML}
            {optionalSectionToXML(resources, "Resources")}
            {optionalSectionToXML(packaging, "Packaging")}
            {optionalSectionToXML(guiprefs, "GuiPrefs")}
            {optionalSectionToXML(panels, "Panels")}
            {optionalSectionToXML(packs, "Packs")}
            </installation>
        }

        private def optionalSectionToXML(section: Option[Section], 
                                         name: String) =
            section match
            {
                case Some(sect) => sect.toXML
                case None       => new Comment("No " + name + " section")
            }

        def generate(): Unit =
        {
            import Path._

            log.info("Creating " + workingInstallDir)
            new File(workingInstallDir.absolutePath.toString).mkdirs()
            val installFile = workingInstallDir / "install.xml"
            log.info("Generating " + installFile)
            val xml = toXML
            val prettyPrinter = new PrettyPrinter(256, 2)
            writeString(installFile, prettyPrinter.format(xml))
            log.info("Done.")
        }

        private def relPath(s: String) = Path.fromString(workingInstallDir, s)

        /*------------------------------------------------------------------*\
                                      <info>
        \*------------------------------------------------------------------*/

        private case class Author(val name: String, val email: Option[String])
        {
            override def toString = 
                email match
                {
                    case None    => name
                    case Some(e) => name + " at " + e
                }
        }

        class Info extends Section
        {
            final val SectionName = "Info"

            info = Some(this)

            var useUninstaller = true
            var requiresJDK = false
            var runPrivileged = false
            var pack200 = false
            var writeInstallationInfo = true

            private val authors = new ListBuffer[Author]

            private val JavaVersion = "javaversion"
            private val URL = "url"
            private val AppSubpath = "appsubpath"
            private val WebDir = "webdir"
            private val AppName = "appname"
            private val AppVersion = "appversion"

            private val options = MutableMap.empty[String, Option[String]]

            private def setOption(name: String, value: String) =
                options += name -> stringToOption(value)

            private def getOption(name: String) =
                optionToString(options.getOrElse(name, None))

            setOption(JavaVersion, "1.6")
            setOption(AppName, "Hey I need a name!")

            def author(name: String): Unit =
                author(name, "")
            def author(name: String, email: String): Unit =
                authors += Author(name, stringToOption(email))

            def url_=(u: String): Unit = setOption(URL, u)
            def url: String = getOption(URL)

            def javaVersion_=(v: String): Unit = setOption(JavaVersion, v)
            def javaVersion: String = getOption(JavaVersion)

            def appName_=(v: String): Unit = setOption(AppName, v)
            def appName: String = getOption(AppName)

            def appVersion_=(v: String): Unit = setOption(AppVersion, v)
            def appVersion: String = getOption(AppVersion)

            def appSubPath_=(v: String): Unit = setOption(AppSubpath, v)
            def appSubPath: String = getOption(AppSubpath)

            def webdir_=(v: String): Unit = setOption(WebDir, v)
            def webdir: String = getOption(WebDir)

            def sectionToXML =
            {
                def opt(name: String): Node =
                    stringOptionToTextNode(options.getOrElse(name, None), name)

                <info>
                    <appname>{appName}</appname>
                    {opt(AppVersion)}
                    {opt(JavaVersion)}
                    {opt(AppSubpath)}
                    {opt(URL)}
                    {opt(WebDir)}
                    <writeinstallationinformation>
                        {yesno(writeInstallationInfo)}
                    </writeinstallationinformation>
                    <requiresjdk>{yesno(requiresJDK)}</requiresjdk>
                    {maybeXML("uninstaller", useUninstaller, 
                              Map("write" -> "yes"))}
                    {maybeXML("run-privileged", runPrivileged)}
                    {maybeXML("pack200", pack200)}
                    {authorsToXML}
                </info>
            }

            private def authorsToXML =
            {
                if (authors.size > 0)
                {
                    <authors>
                    {
                        for (author <- authors)
                        yield <author name={author.name}
                                      email={author.email.getOrElse("")}/>
                    }
                    </authors>
                }
                else
                {
                    new Comment("no authors")
                }
            }
        }

        /*------------------------------------------------------------------*\
                                     <locale>
        \*------------------------------------------------------------------*/

        /*------------------------------------------------------------------*\
                                    <resources>
        \*------------------------------------------------------------------*/

        class Resources extends Section
        {
            final val SectionName = "Resources"

            resources = Some(this)

            final val LicensePanel = "HTMLLicensePanel.license"
            final val InfoPanel = "HTMLInfoPanel.info"
            final val Logo = "Installer.image"

            private var licensePath: Option[Path] = None
            private var infoPath: Option[Path] = None
            private var imagePath: Option[Path] = None
            private var installDirectory = new InstallDirectory

            private val paths = MutableMap.empty[String, Option[Path]]

            private def setPath(name: String, value: Path) =
                paths += name -> Some(value)

            private def getPath(name: String): Path =
                paths.getOrElse(name, None) match
                {
                    case None => relPath("nothing")
                    case Some(p) => p
                }

            def licensePanel_=(p: Path): Unit = setPath(LicensePanel, p)
            def licensePanel: Path = getPath(LicensePanel)

            def infoPanel_=(p: Path): Unit = setPath(InfoPanel, p)
            def infoPanel: Path = getPath(InfoPanel)

            def logo_=(p: Path): Unit = setPath(Logo, p)
            def logo: Path = getPath(Logo)

            class InstallDirectory
            {
                installDirectory = this

                implicit def stringToInstallDirectory(s: String) =
                    new InstallDirectoryBuilder(s)

                val dirs = MutableMap.empty[String,String]
                var customXML: NodeSeq = NodeSeq.Empty

                class InstallDirectoryBuilder(val path: String)
                {
                    def on(operatingSystem: String): Unit =
                        dirs += operatingSystem -> path
                }

                def toXML =
                {
                    val nodes =
                    {
                        for ((os, path) <- dirs)
                            yield <res id={"TargetPanel.dir." + os}
                                       src={installDirString(os, path)}/>
                    }.toList
                    NodeSeq.fromSeq(nodes ++ customXML)
                }

                private def installDirString(os: String, path: String): String =
                {
                    val filename = "instdir_" + os + ".txt"
                    val fullPath = workingInstallDir / filename
                    // Put the string in the file. That's how IzPack wants it.
                    writeString(fullPath, path)
                    fullPath.toString
                }
            }

            def sectionToXML =
            {
                <resources>
                {pathToXML(LicensePanel)}
                {pathToXML(Logo)}
                {pathToXML(InfoPanel)}
                {installDirectory.toXML}
                </resources>
            }

            private def pathToXML(id: String) =
            {
                paths.getOrElse(id, None) match
                {
                    case None    =>  new Comment("no " + id + " resource")
                    case Some(p) => <res id={id} src={p.toString}/>
                }
            }
        }

        /*------------------------------------------------------------------*\
                                    <packaging>
        \*------------------------------------------------------------------*/

        class Packaging extends Section
        {
            final val SectionName = "Packaging"

            packaging = Some(this)

            object Packager extends Enumeration
            {
                private val packageName = "com.izforge.izpack.compiler"
                type Packager = Value

                val SingleVolume = Value(packageName + ".Packager")
                val MultiVolume = Value(packageName + ".MultiVolumePackager")
            }

            import Packager._

            var packager: Packager = SingleVolume
            var volumeSize: Int = 0
            var firstVolFreeSpace: Int = 0

            def sectionToXML =
            {
                if ((packager != MultiVolume) &&
                    ((volumeSize + firstVolFreeSpace) > 0))
                {
                    log.warn("volumeSize and firstVolFreeSpace are " +
                             "ignored unless packager is MultiVolume.")
                }

                var unpacker: String = ""
                <packaging>
                    <packager class={packager.toString}>
                    {
                        packager match
                        {
                            case MultiVolume =>
                                <options
                                   volumesize={volumeSize.toString}
                                   firstvolumefreespace={firstVolFreeSpace.toString}
                                />
                                unpacker = "com.izforge.izpack.installer." +
                                           "MultiVolumeUnpacker"
                            case SingleVolume =>
                                new Comment("no options")
                                unpacker = "com.izforge.izpack.installer." +
                                           "Unpacker"
                        }
                    }
                    </packager>
                    <unpacker class={unpacker}/>
                </packaging>
            }
        }

        /*------------------------------------------------------------------*\
                                    <guiprefs>
        \*------------------------------------------------------------------*/

        class GuiPrefs extends Section
        {
            final val SectionName = "GuiPrefs"

            guiprefs = Some(this)

            var height: Int = 600
            var width: Int = 800
            var resizable: Boolean = true

            private var lafs = new ListBuffer[LookAndFeel]

            class LookAndFeel(val name: String)
                extends Section with OperatingSystemConstraints
            {
                final val SectionName = "LookAndFeel"

                lafs += this

                var params = Map.empty[String, String]


                def sectionToXML =
                {
                    <laf name={name}>
                        {operatingSystemsToXML}
                    </laf>
                }
            }

            def sectionToXML =
            {
                val strResizable = if (resizable) "yes" else "no"

                <guiprefs height={height.toString} width={width.toString}
                          resizable={yesno(resizable)}>
                {for (laf <- lafs) yield laf.toXML}
                </guiprefs>
            }
        }

        /*------------------------------------------------------------------*\
                                     <panels>
        \*------------------------------------------------------------------*/

        class Panels extends Section
        {
            final val SectionName = "Panels"

            panels = Some(this)

            final val Panel = "panel"

            private val panelClasses = new ListBuffer[Panel]

            class Panel(val name: String) extends Section
            {
                final val SectionName = "Panel"

                final val Jar = "jar"
                var jarOption: Option[PathFinder] = None
                var help: Map[String, Path] = Map.empty[String, Path]

                panelClasses += this

                def jar_=(p: PathFinder): Unit =
                    p.get.size match
                    {
                        case 0 => 
                            throw new RuntimeException("No jar")
                        case 1 => 
                            jarOption = Some(p.get.toList.head)
                        case _ => 
                            throw new RuntimeException("Too many matching jars")
                    }

                def jar: PathFinder = 
                    jarOption.getOrElse(relPath("nothing.jar"))

                def sectionToXML =
                {
                    val jarAttr =
                    {
                        jarOption match
                        {
                            case None    => ""
                            case Some(p) => p.toString
                        }
                    }

                    <panel classname={name} jar={jarAttr}>
                    {
                        if (help.size > 0)
                        {
                            for ((lang, path) <- help)
                                yield <help iso3={lang} src={path.toString}/>
                        }
                        else
                            new Comment("no help")
                    }
                    </panel>
                }
            }

            def sectionToXML =
            {
                <panels>
                {for (panel <- panelClasses) yield panel.toXML}
                </panels>
            }
        }

        /*------------------------------------------------------------------*\
                                     <packs>
        \*------------------------------------------------------------------*/

        class Packs extends Section
        {
            final val SectionName = "Packs"

            packs = Some(this)

            private var individualPacks = new ListBuffer[Pack]

            class Pack(val name: String)
                extends Section with OperatingSystemConstraints
            {
                final val SectionName = "Pack"

                individualPacks += this

                var required = false
                var preselected = false
                var hidden = false
                var depends: List[Pack] = Nil

                private var files = new ListBuffer[OneFile]
                private var filesets = new ListBuffer[FileSet]
                private var executables = new ListBuffer[Executable]
                private var parsables = new ListBuffer[Parsable]

                trait OneFile extends Section with OperatingSystemConstraints
                {
                    files += this
                }

                object Overwrite extends Enumeration
                {
                    type Overwrite = Value

                    val Yes = Value("true")
                    val No = Value("false")
                    val AskYes = Value("asktrue")
                    val AskNo = Value("askfalse")
                    val Update = Value("update")
                }

                class SingleFile(val source: Path, val target: String)
                    extends OneFile
                {
                    final val SectionName = "SingleFile"

                    def sectionToXML =
                    {
                        <singlefile src={source.toString}
                                    target={target}>
                          {operatingSystemsToXML}
                        </singlefile>
                    }
                }

                class File(val source: Path, val targetdir: String)
                    extends OneFile
                {
                    final val SectionName = "File"

                    var unpack = false
                    var overwrite = Overwrite.Update

                    def sectionToXML =
                    {
                        <file src={source.toString}
                                 targetdir={targetdir}
                                 unpack={yesno(unpack)}
                                 override={overwrite.toString}>
                          {operatingSystemsToXML}
                        </file>
                    }
                }

                class FileSet(val files: PathFinder, val targetdir: String)
                    extends OperatingSystemConstraints
                {
                    final val SectionName = "FileSet"

                    var overwrite = Overwrite.Update

                    def toXML =
                    {
                        val nodes = 
                        {
                            for (path <- files.get)
                                yield fileToXML(path)
                        }.toList
                        NodeSeq.fromSeq(nodes)
                    }

                    private def fileToXML(path: Path) =
                    {
                        <file src={path.toString}
                              targetdir={targetdir}
                              override={overwrite.toString}>
                          {operatingSystemsToXML}
                        </file>
                    }
                }

                class Executable(val target: String, val stage: String)
                    extends Section with OperatingSystemConstraints
                {
                    final val SectionName = "Executable"

                    executables += this

                    def this(target: String) = this(target, "never")

                    def sectionToXML =
                    {
                        <executable targetfile={target} stage={stage}>
                            {operatingSystemsToXML}
                        </executable>
                    }
                }

                class Parsable(val target: String)
                    extends Section with OperatingSystemConstraints
                {
                    final val SectionName = "Parsable"

                    parsables += this

                    def sectionToXML =
                    {
                        <parsable targetfile={target}>
                            {operatingSystemsToXML}
                        </parsable>
                    }
                }

                def sectionToXML =
                {
                    <pack name={name} 
                          required={yesno(required)}
                          hidden={yesno(hidden)}
                          preselected={yesno(preselected)}>
                        {operatingSystemsToXML}
                        {
                            for (dep <- depends)
                                yield <depends packname={dep.name}/>
                        }
                        {for (f <- files) yield f.toXML}
                        {for (fs <- filesets) yield fs.toXML}
                    </pack>
                }
            }

            def sectionToXML =
            {
                <packs>
                {for (pack <- individualPacks) yield pack.toXML}
                </packs>
            }
        }
    }
}

package org.clapper.sbtplugins
{
    import sbt._
    import java.io.File
    import scala.io.Source
    import com.izforge.izpack.compiler.CompilerConfig
    import org.clapper.sbtplugins.izpack._

    /**
     * Plugin for SBT (Simple Build Tool) that provides a method that will
     * configure and run the IzPack installer.
     *
     * To use this plugin, mix it into your SBT project.
     */
    trait IzPackPlugin extends DefaultProject
    {
        /*------------------------------------------------------------------*\
                                    Dependencies
        \*------------------------------------------------------------------*/

        /*------------------------------------------------------------------*\
                                       Tasks
        \*------------------------------------------------------------------*/

        lazy val makeInstallConfig = task { generateInstallConfig }

        /*------------------------------------------------------------------*\
                                     Methods
        \*------------------------------------------------------------------*/

        def izPackConfig: Option[IzPackConfig] = None

        def generateInstallConfig: Option[String] =
        {
            izPackConfig match
            {
                case None =>
                    log.warn("Can't create IzPack installer XML file, since " +
                             "no in-memory configuration is defined.")

                case Some(config) =>
                    config.generate
            }

            None
        }

        /**
         * Build the actual installer jar.
         *
         * @param installerConfig  the IzPack installer configuration file
         * @param installerJar     where to store the installer jar file
         */
        def izpackMakeInstaller(installFile: Path, installerJar: Path) =
        {
            FileUtilities.withTemporaryDirectory(log)
            {
                baseDir =>

                    val compilerConfig = new CompilerConfig(
                        installFile.absolutePath,
                        baseDir.getPath, // basedir
                        CompilerConfig.STANDARD,
                        installerJar.absolutePath
                    )
                log.info("Creating installer in " + installerJar)
                compilerConfig.executeCompiler
                None
            }
        }
    }
}
