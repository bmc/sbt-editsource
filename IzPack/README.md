IzPack SBT Plugin
=================

## Introduction

This project contains an [IzPack][izpack] plugin for the [SBT][sbt]
build tool.

[sbt]: http://code.google.com/p/simple-build-tool/
[izpack]: http://izpack.org/

## Getting this Plugin

### The Released Version

In your own project, create a `project/plugins/Plugins.scala` file (if you
haven't already), and add the following lines, to make the project available
to your SBT project:

    val orgClapperMavenRepo = "clapper.org Maven Repo" at "http://maven.clapper.org/"

    val izpackPlugin = "org.clapper" % "sbt-izpack-plugin" % "0.1"

### The Development Version

You can also use the development version of this plugin (that is, the
version checked into the [GitHub repository][github-repo]), by building it
locally.

First, download the plugin's source code by cloning this repository.

    git clone http://github.com/bmc/sbt-plugins.git

Then, within the `izpack` project directory, publish it locally:

    sbt update publish-local

[github-repo]: http://github.com/bmc/sbt-plugins

## Using the Plugin

Please see [the IzPack Plugin web site][izpack-plugin-page] for detailed usage instructions.

[wiki-izpack-plugin]: http://wiki.github.com/bmc/sbt-plugins/izpackplugin

Regardless of how you get the plugin, here's how to use it in your SBT
project.

First, in your `project/plugins/Plugins.scala` file, add the following
line, which will ensure that the IzPack compiler is available to your
build script:

    val izPack = "org.codehaus.izpack" % "izpack-standalone-compiler" % "4.3.1"

Next, create a project build file in `project/build/`, if you haven't done
that already. Then, ensure that the project mixes in `IzPackPlugin`. Once
you've done that, you can use the plugin's `izpackMakeInstaller()` method.

The `izpackMakeInstaller()` method takes, as input:

* The [IzPack installation configuration][izpack-install] file, an XML file
  that tells IzPack how to build the installer. You can either use a
  canned file, or you can create one from a template. (I typically create one
  from a template, using the [EditSource][editsource] SBT plugin to fill in
  various things, such as jar files, directory names, etc.)
* The path (as an SBT `Path` object) to the target installer jar file to be
  created.

[izpack-install]: http://izpack.org/documentation/installation-files.html
[editsource]: http://github.com/bmc/sbt-plugins/tree/master/editsource/

Here's an example:

    import sbt_
    import org.clapper.sbtplugins.IzPackPlugin

    class MyProject(info: ProjectInfo) 
        extends DefaultProject with IzPackPlugin with EditSourcePlugin
    {
        val installTemplate = "src" / "main" / "izpack" / "install.xml"
        FileUtilities.withTemporaryDirectory(log)
        {
            jarDir =>

            val jars = (("lib" +++ "lib_managed") ** 
                        ("*.jar") - "scalatest*.jar"
                                  - "scala-library*.jar"
                                  - "scala-compiler.jar"
                                  - "izpack*.jar")
            FileUtilities.copyFlat(jars.get, Path.fromFile(jarDir), log)

            val installFile = File.createTempFile("inst", ".xml")
            installFile.deleteOnExit
            editSourceToFile(Source.fromFile(installTemplate.absolutePath),
                             Map("EXTRA_JARS_DIR" -> jarDir.getPath,
                                 "DOCS_DIR" -> ("src" / "docs").absolutePath),
                             installFile)
            izpackMakeINstaller(Path.fromFile(installFile),
                                "target" / "installer.jar")
        }

        None
    }

## License

This plugin is released under a BSD license, adapted from
<http://opensource.org/licenses/bsd-license.php>

Copyright &copy; 2010, Brian M. Clapper
All rights reserved.

Redistribution and use in source and binary forms, with or without
modification, are permitted provided that the following conditions are
met:

* Redistributions of source code must retain the above copyright notice,
  this list of conditions and the following disclaimer.

* Redistributions in binary form must reproduce the above copyright
  notice, this list of conditions and the following disclaimer in the
  documentation and/or other materials provided with the distribution.

* Neither the names "clapper.org" nor the names of its contributors may be
  used to endorse or promote products derived from this software without
  specific prior written permission.

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

## Copyrights

These plugins are copyright &copy; 2010 Brian M. Clapper.

[SBT][sbt] is copyright &copy; 2008, 2009 Mark Harrah, Nathan Hamblen.
