Markdown SBT Plugin
==================

## Introduction

This project contains a [Markdown][markdown] plugin for the [SBT][sbt]
build tool. This Markdown plugin uses the [Showdown][showdown] Javascript 
Markdown parser and the Mozilla [Rhino][rhino] Javascript engine to convert
Markdown into HTML. For details on the approach, see my
[Parsing Markdown in Scala][markdown-blog] blog entry.

[sbt]: http://code.google.com/p/simple-build-tool/
[markdown]: http://daringfireball.net/projects/markdown/
[showdown]: http://attacklab.net/showdown/
[rhino]: http://www.mozilla.org/rhino/
[markdown-blog]: http://brizzled.clapper.org/id/98

## Getting this Plugin

### The Released Version

In your own project, create a `project/plugins/Plugins.scala` file (if you
haven't already), and add the following lines, to make the project available
to your SBT project:

    val orgClapperMavenRepo = "clapper.org Maven Repo" at "http://maven.clapper.org/"

    val markdown = "org.clapper" % "sbt-markdown-plugin" % "0.1.1"

### The Development Version

You can also use the development version of this plugin (that is, the
version checked into the [GitHub repository][github-repo]), by building it
locally.

First, download the plugin's source code by cloning this repository.

    git clone http://github.com/bmc/sbt-plugins.git

Then, within the `markdown` project directory, publish it locally:

    sbt update publish-local

[github-repo]: http://github.com/bmc/sbt-plugins

## Using the Plugin

Regardless of how you get the plugin, here's how to use it in your SBT
project.

Create a project build file in `project/build/`, if you haven't already.
Then, ensure that the project mixes in `MarkdownPlugin`. You have to ensure
that you hook in the Markdown plugin's `update` and `clean-lib` logic, as
shown below. Once you've done that, you can use the plugin's `markdown()`
method. Here's an example:

    import sbt_
    import org.clapper.sbtplugins.MarkdownPlugin

    class MyProject(info: ProjectInfo) extends DefaultProject with MarkdownPlugin
    {
        override def cleanLibAction = super.cleanAction dependsOn(markdownCleanLibAction)
        override def updateAction = super.updateAction dependsOn(markdownUpdateAction)

        // An "htmlDocs" action that creates an HTML file from a Markdown source.
        val usersGuideMD = "src" / "docs" / "guide.md"
        val usersGuideHTML = "target" / "doc" / "guide.html"
        lazy val htmlDocs = fileTask(usersGuideMD from usersGuideHTML)
        {
            markdown(usersGuideMD, usersGuideHTML, log)
        }
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
