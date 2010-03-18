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

    trait Section
    {
        val SectionName: String

        def set(key: String, value: Any): Unit = badField(key, value)

        def toXML: Elem

        implicit def stringToAssigner[T](key: String) = new Assigner(key, this)

        protected def badField(name: String, value: Any): Unit =
            badField(name, value, null)

        protected def badField(name: String, value: Any, msg: String): Unit =
        {
            val prefix = SectionName + " section: Unknown configuration item " +
                         "or bad value for configuration item: "  +
                         name + " := " + value.toString
            val exceptionMsg = if (msg == null) prefix else prefix + ". " + msg
            throw new RuntimeException(exceptionMsg)
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

        protected def yesno(b: Boolean): String = if (b) "yes" else "no"
    }

    class Assigner(val key: String, section: Section)
    {
        def := [T](value: T): Unit = section.set(key, value)
    }

    /*-------------------------------------------------------------------*\
                                 Main Config Class
    \*-------------------------------------------------------------------*/

    /**
     * val cfg = IzpackConfig
     * {
     *     cfg: IzPackConfig =>
     *
     *     cfg.info
     *     (
     *         "appname"    -> name,
     *         "appversion" -> thing
     *     )
     * }
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
        private var languages: List[String] = Nil
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

        def Languages(langs: String*) = languages = langs.toList

        private def languagesToXML =
        {
            val langs = if (languages == Nil) List("eng") else languages
            <locale>
            {for (name <- languages) yield <langpack iso3={name}/>}
            </locale>
        }

        private def sectionToXML(section: Option[Section], name: String) =
            section match
            {
                case Some(sect) => sect.toXML
                case None       => new Comment("No " + name + " section")
            }

        def toXML =
        {
            val now = dateFormatter.format(new java.util.Date)

            <installation version="1.0">
            {new Comment("IzPack installation file.")}
            {new Comment("Generated by org.clapper SBT IzPack plugin: " + now)}
            {sectionToXML(info, "Info")}
            {languagesToXML}
            {sectionToXML(resources, "Resources")}
            {sectionToXML(packaging, "Packaging")}
            {sectionToXML(guiprefs, "GuiPrefs")}
            {sectionToXML(panels, "Panels")}
            {sectionToXML(packs, "Packs")}
            </installation>
        }

        def generate(): Unit =
        {
            import Path._

            log.info("Creating " + workingInstallDir)
            new File(workingInstallDir.absolutePath.toString).mkdirs()
            val installFile = workingInstallDir / "install.xml"
            log.info("Generating " + installFile)
            val xml = toXML
            val prettyPrinter = new PrettyPrinter(80, 2)
            writeString(installFile, prettyPrinter.format(xml))
            log.info("Done.")
        }

        /*------------------------------------------------------------------*\
                                      <info>
        \*------------------------------------------------------------------*/

        class Author(val name: String, val email: String)
        {
            override def toString = name + " at " + email
        }

        class AuthorBuilder(val name: String)
        {
            def email(email: String) = new Author(name, email)
        }

        class Info extends Section
        {
            final val SectionName = "Info"

            info = Some(this)

            final val AppName = "appname"
            final val AppVersion = "appversion"
            final val AppSubpath = "appsubpath"
            final val Author = "author"
            final val URL = "url"
            final val JavaVersion = "javaversion"
            final val WebDir = "webdir"
            final val RequiresJDK = "requiresjdk"
            final val RunPrivileged = "runprivileged"
            final val MakeUninstaller = "uninstaller"

            private var appName: String = ""
            private var appVersion: String = ""
            private val authors = new ListBuffer[Author]
            private var appSubpath: Option[String] = None
            private var url: Option[String] = None
            private var javaVersion = "1.6"
            private var requiresJDK = false
            private var runPrivileged = false
            private var webdir: Option[String] = None
            private var useUninstaller = true

            implicit def stringToAuthorBuilder(name: String) =
                new AuthorBuilder(name)

            override def set(key: String, value: Any): Unit =
            {
                (key, value) match
                {
                    case (AppName, s: String)          => appName = s
                    case (AppVersion, s: String)       => appVersion = s
                    case (AppSubpath, s: String)       => appSubpath = Some(s)
                    case (Author, a: Author)           => authors += a
                    case (URL, s: String)              => url = Some(s)
                    case (JavaVersion, s: String)      => javaVersion = s
                    case (WebDir, s: String)           => webdir = Some(s)
                    case (RequiresJDK, b: Boolean)     => requiresJDK = b
                    case (RunPrivileged, b: Boolean)   => runPrivileged = b
                    case (MakeUninstaller, b: Boolean) => useUninstaller = b
                    case _                             => badField(key, value)
                }
            }

            def toXML =
            {
                <info>
                <appname>{appName}</appname>
                <appversion>{appVersion}</appversion>
                <javaversion>{javaVersion}</javaversion>
                <requiresjdk>{requiresJDK}</requiresjdk>

                {
                    appSubpath match
                    {
                        case None    => new Comment("no application subpath")
                        case Some(p) => <appsubpath>{p}</appsubpath>
                    }
                }

                {
                    if (authors.size > 0)
                    {
                        <authors>
                        {
                            for (author <- authors)
                                yield <author name={author.name}
                                              email={author.email}/>
                        }
                        </authors>
                    }
                    else
                    {
                        new Comment("no authors")
                    }
                }

                {
                    url match
                    {
                        case None    => new Comment("no URL")
                        case Some(u) => <url>{u}</url>
                    }
                }

                {
                    if (useUninstaller)
                        <uninstaller write="yes"/>
                    else
                        new Comment("no uninstaller")
                }

                {
                    if (runPrivileged)
                        <run-privileged/>
                    else
                        new Comment("no run-privileged")
                }

                {
                    webdir match
                    {
                        case None    => new Comment("no webdir")
                        case Some(d) => <webdir>{d}</webdir>
                    }
                }
                </info>
            }
        }

        /*------------------------------------------------------------------*\
                                     <locale>
        \*------------------------------------------------------------------*/

        /*------------------------------------------------------------------*\
                                    <resources>
        \*------------------------------------------------------------------*/

        class InstallDirectory(val operatingSystem: String, val path: String)
        {
            override def hashCode = operatingSystem.hashCode

            override def toString = path + " on " + operatingSystem
        }

        class InstallDirectoryBuilder(val path: String)
        {
            def on(operatingSystem: String): InstallDirectory =
                new InstallDirectory(operatingSystem, path)
        }

        class Resources extends Section
        {
            final val SectionName = "Resources"

            resources = Some(this)

            final val LicensePanel = "licensepanel"
            final val InfoPanel = "infopanel"
            final val Logo = "logo"
            final val InstallDir = "installdir"

            implicit def stringToInstallDirectory(s: String) =
                new InstallDirectoryBuilder(s)

            private var licensePath: Option[Path] = None
            private var infoPath: Option[Path] = None
            private var imagePath: Option[Path] = None
            private val installDirectories = new ListBuffer[InstallDirectory]

            override def set(key: String, value: Any): Unit =
            {
                (key, value) match
                {
                    case (LicensePanel, v: Any) => licensePath = toPath(key, v)
                    case (InfoPanel, v: Any)    => infoPath = toPath(key, v)
                    case (Logo, v: Any)         => imagePath = toPath(key, v)
                    case (InstallDir, v: Any)   => handleInstallDir(key, v)
                    case _                      => badField(key, value)
                }
            }

            def toXML =
            {
                <resources>
                {pathToXML("HTMLLicensePanel.licence", licensePath)}
                {pathToXML("Installer.image", imagePath)}
                {pathToXML("HTMLInfoPanel.info", infoPath)}
                {installDirectoriesToXML(workingInstallDir)}
                </resources>
            }

            private def toPath(key: String, value: Any): Option[Path] =
            {
                value match
                {
                    case s: String       => Some(Path.fromFile(new File(s)))
                    case p: Path         => Some(p)
                    case _               => badField(key, value); None
                }
            }

            private def handleInstallDir(key: String, value: Any) =
            {
                value match
                {
                    case dir: InstallDirectory => installDirectories += dir
                    case _ => badField(key, value)
                }
            }

            private def installDirToPath(workingDir: Path,
                                         installDir: InstallDirectory): String =
            {
                val filename = "instdir_" + installDir.operatingSystem + ".txt"
                val fullPath = workingDir / filename
                writeString(fullPath, installDir.path)
                fullPath.toString
            }

            private def pathToXML(id: String, p: Option[Path]) =
            {
                p match
                {
                    case None    =>  new Comment("no " + id + " resource")
                    case Some(p) => <res id={id} src={p.toString}/>
                }
            }

            private def installDirectoriesToXML(workingInstallDir: Path) =
            {
                if (installDirectories.size == 0)
                    new Comment("no overriding installation directories")
                else
                {
                    val nodes =
                    {
                        for (dir <- installDirectories)
                            yield
                            {
                                val path = installDirToPath(workingInstallDir, 
                                                            dir)
                                val id = "TargetPanel.dir." +
                                         dir.operatingSystem
                                <res id={id} src={path}/>
                            }
                    }

                    NodeSeq.fromSeq(nodes)
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

            final val Packager = "packager"
            final val VolumeSize = "volme-size"
            final val FirstVolumeFreeSpace = "firstvolumefreespace"
            final val MultiVolume =
                "com.izforge.izpack.compiler.MultiVolumePackager"
            final val SingleVolume =
                "com.izforge.izpack.compiler.Packager"

            private var packager: String = SingleVolume
            private var volumeSize: Int = 0
            private var firstVolFreeSpace: Int = 0

            private def setPackager(k: String, v: String) =
            {
                v match
                {
                    case SingleVolume => packager = SingleVolume
                    case MultiVolume  => packager = MultiVolume
                    case _            => badField(k, v)
                }
            }

            override def set(key: String, value: Any): Unit =
            {
                (key, value) match
                {
                    case (Packager, s: String)          => setPackager(key, s)
                    case (VolumeSize, i: Int)           => volumeSize = i
                    case (FirstVolumeFreeSpace, i: Int) => firstVolFreeSpace = i
                    case _                              => badField(key, value)
                }
            }

            def toXML =
            {
                if ((packager != MultiVolume) &&
                    ((volumeSize + firstVolFreeSpace) > 0))
                {
                    log.warn("volumesize and firstvolumefreespace are " +
                             "ignored unless packager is multi-volume.")
                }

                <packaging>
                <packager class={packager}>
                {
                    packager match
                    {
                        case MultiVolume =>
                            <options
                               volumesize={volumeSize.toString}
                               firstvolumefreespace={firstVolFreeSpace.toString}
                            />
                          case SingleVolume =>
                              new Comment("no options")
                      }
                  }
                  </packager>
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

            final val Height = "height"
            final val Width  = "width"
            final val Resizable = "resizable"

            private var height: Int = 600
            private var width: Int = 800
            private var resizable: Boolean = true
            private var lafs = new ListBuffer[LookAndFeel]

            class LookAndFeel extends Section
            {
                final val SectionName = "LookAndFeel"

                lafs += this

                final val Name = "name"

                var params = MutableMap.empty[String, String]
                var name: String = ""
                var operatingSystems = new ListBuffer[String]

                override def set(key: String, value: Any): Unit =
                {
                    (key, value) match
                    {
                        case (Name, s: String) =>
                            name = s
                        case (OS, s: String) =>
                            operatingSystems += s
                        case (n: String, v: String) =>
                            params += (n -> v)
                        case _ =>
                            badField(key, value)
                    }
                }

                def toXML =
                {
                    <laf name={name}>
                    {for (os <-operatingSystems) yield <os family={os}/>}
                    {
                        for ((name, value) <- params)
                            yield <param name={name} value={value}/>
                    }
                    </laf>
                }
            }

            override def set(key: String, value: Any): Unit =
            {
                (key, value) match
                {
                    case (Height, i: Int)        => height = i
                    case (Width, i: Int)         => width = i
                    case (Resizable, b: Boolean) => resizable = b
                    case _                       => badField(key, value)
                }
            }

            def toXML =
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
                var jar: Option[Path] = None

                panelClasses += this

                override def set(key: String, value: Any): Unit =
                {
                    (key, value) match
                    {
                        case (Jar, p: Path) =>
                            jar = Some(p)
                        case (Jar, p: PathFinder) =>
                            if (p.get.size == 0)
                                badField(key, value, "No jar file found")
                            if (p.get.size > 1)
                                badField(key, value, "Too many jar files found")
                            jar = Some(p.get.toList.head)
                        case (Jar, s: String) =>
                            jar = Some(Path.fromFile(new File(s)))
                        case _ =>
                            badField(key, value)
                    }
                }

                def toXML =
                {
                    if (jar != None)
                        <panel classname={name} jar={jar.get.toString}/>
                    else
                        <panel classname={name}/>
                }
            }

            def toXML =
            {
                <panels>
                {
                    for (panel <- panelClasses)
                        yield panel.toXML
                }
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
                extends Section
            {
                final val SectionName = "Pack"

                individualPacks += this

                final val FileSet = "fileset"
                final val SingleFile = "singlefile"

                private var isRequired = false

                private var packInfos = new ListBuffer[PackInfo]

                def required = isRequired = true

                abstract class PackInfo
                {
                    packInfos += this

                    var operatingSystems = new MutableSet[String]
                    var isParsable = false
                    var isExecutable = false
                    var executableStage = "never"

                    def onlyFor(os: String) = operatingSystems += os
                }

                class SingleFile(val source: Path, val target: String)
                    extends PackInfo
                {
                    def parsable = isParsable = true
                    def executable(): Unit = executable("never")
                    def executable(stage: String): Unit =
                    {
                        isExecutable = true
                        executableStage = stage
                    }
                }

                class FileSet(val dir: Path,
                              val includes: String,
                              val targetdir: String)
                    extends PackInfo

                def toXML =
                {
                    def listOperatingSystems(packInfo: PackInfo) =
                        for (os <- packInfo.operatingSystems)
                            yield <os family={os}/>

                    def packNode(packInfo: PackInfo) =
                    {
                        packInfo match
                        {
                            case s: SingleFile =>
                                <singlefile src={s.source.absolutePath}
                                            target={s.target}>
                                  {listOperatingSystems(packInfo)}
                                </singlefile>
                                if (s.isParsable) parsableToXML(s)
                                if (s.isExecutable) executableToXML(s)

                            case f: FileSet =>
                                <fileset dir={f.dir.absolutePath}
                                         includes={f.includes}
                                         targetdir={f.targetdir}>
                                    {listOperatingSystems(packInfo)}
                                </fileset>
                            case _ =>
                                assert(false)
                                new Comment("huh? " + packInfo.getClass)
                        }
                    }

                    def parsableToXML(p: SingleFile) =
                    {
                        assert(p.isParsable)
                        <parsable targetfile={p.target}>
                            {listOperatingSystems(p)}
                        </parsable>
                    }

                    def executableToXML(e: SingleFile) =
                    {
                        assert(e.isExecutable)
                        <executable targetfile={e.target}
                                    stage={e.executableStage}>
                            {listOperatingSystems(e)}
                        </executable>
                    }

                    <pack name={name} 
                          required={if (isRequired) "yes" else "no"}>
                    {
                        for (packInfo <- packInfos)
                            yield packNode(packInfo)
                    }
                    </pack>
                }
            }

            def toXML =
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
