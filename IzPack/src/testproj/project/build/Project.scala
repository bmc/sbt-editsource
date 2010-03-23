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

import sbt._
import org.clapper.sbtplugins.IzPackPlugin
import org.clapper.sbtplugins.izpack._

class TestProject(info: ProjectInfo) 
    extends DefaultProject(info) with IzPackPlugin
{
    lazy val makeInstallConfig = task { generateInstallConfig(config) }

    lazy val config = new IzPackConfig("target" / "install", log)
    {
        val InstallSrc = "src" / "main" / "install"

        customXML =
        (
            <conditions>
                <condition type="java" id="installonunix">
                    <java>
                        <class>com.izforge.izpack.util.OsVersion</class>
                        <field>IS_UNIX</field>
                    </java>
                </condition>
            </conditions>
            <installerrequirements>
                <installerrequirement condition="installonunix"
                                      message="Only installable on Unix"/>
            </installerrequirements>
        )

        new Info
        {
            appName = "foobar"
            appVersion = "1.0"
            author("Joe Schmoe", "schmoe@example.org")
            author("Brian Clapper", "bmc@clapper.org")
            url = "http://www.example.org/software/foobar/"
            javaVersion = "1.6"
            summaryLogFilePath = "$INSTALL_PATH/log.html"
        }

        // Use ISO3 language codes; that's what IzPack wants.

        languages = List("eng", "chn", "deu", "fra", "jpn", "spa", "rus")

        new Resources
        {
            new Resource
            {
                id = "HTMLLicensePanel.license"
                source = InstallSrc / "license.html"
            }

            new Resource
            {
                id = "HTMLInfoPanel.info"
                source = InstallSrc / "info.html"
            }

            new Resource
            {
                id = "Installer.image"
                source = InstallSrc / "logo.png"
            }

            new InstallDirectory
            {
                """C:\Program Files\clapper.org\test""" on Windows
                "/Applications/test" on MacOSX
                "/usr/local" on Unix
            }
        }

        new Variables
        {
            variable("foo", "bar")
        }

        new Packaging
        {
            packager  = Packager.MultiVolume
            volumeSize = (1024 * 1024 * 1024)
        }

        new GuiPrefs
        {
            height = 768
            width  = 1024

            new LookAndFeel("metouia")
            {
                onlyFor(Unix)
            }

            new LookAndFeel("liquid")
            {
                onlyFor(Windows, MacOS)

                params = Map("decorate.frames" -> "yes",
                             "decorate.dialogs" -> "yes")
            }
        }

        new Panels
        {
            new Panel("HelloPanel")
            new Panel("HTMLInfoPanel")
            new Panel("HTMLLicencePanel")
            new Panel("TargetPanel")
            {
                id = "target"
                help = Map("eng" -> InstallSrc / "TargetPanelHelp_en.html",
                           "deu" -> InstallSrc / "TargetPanelHelp_de.html")
            }

            new Panel("PacksPanel")
            new Panel("InstallPanel")
            new Panel("ProcessPanel")
            new Panel("XInfoPanel")
            new Panel("FinishPanel")
            new Panel("DummyPanel")
            {
                val scalaVersionDir = "scala-" + buildScalaVersion

                jar = "project" / "boot" / scalaVersionDir / "lib" / 
                      "scala-library.jar"

                new Action("postvalidate", "PostValidationAction")
                new Validator("org.clapper.izpack.TestValidator")
            }
        }

        new Packs
        {
            new Pack("Core")
            {
                required = true
                preselected = true

                new SingleFile("target" / "doc" / "LICENSE.html",
                               "LICENSE.html")

                new SingleFile("target" / "foo.jar",
                               "$INSTALL_PATH/lib/foo.jar")

                new SingleFile("src" / "main" / "install" / "foo.sh",
                               "$INSTALL_PATH/bin/foo.sh")
                {
                    onlyFor(Unix, MacOS)
                }

                new SingleFile("src" / "main" / "install" / "foo.bat",
                               "$INSTALL_PATH/bin/foo.bat")
                {
                    onlyFor(Windows)
                }

                new FileSet("project" ** "*.jar", "$INSTALL_PATH")

                new Executable("$INSTALL_PATH/bin/foo.bat")
                {
                    onlyFor(Windows)
                }

                new Executable("$INSTALL_PATH/bin/foo.sh")
                {
                    onlyFor(Unix, MacOS)
                }

                new Parsable("$INSTALL_PATH/bin/foo.bat")
                {
                    onlyFor(Windows)
                }

                new Parsable("$INSTALL_PATH/bin/foo.sh")
                {
                    onlyFor(Unix, MacOS)
                }
            }

            new Pack("Source")
            {
                required = false
                preselected = true

                new File("src" / "main" / "scala", "$INSTALL_DIR/src")
            }
        }
    }
}
