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

package org.clapper.sbtplugins

import sbt._

import java.io.File

import scala.io.Source

/**
 * Plugin for SBT (Simple Build Tool) that provides a method that will
 * run Markdown on a file.
 *
 * To use this plugin, mix it into your SBT project. Then, ensure that
 * your update action depends on `markdownUpdate`, and your cleanLib action
 * depends on `markdownCleanLib`. To run Markdown on a file, call the
 * `markdown()` method. For example:
 *
 * {{{
 * import sbt._
 * import org.clapper.sbtplugins.MarkdownPlugin
 * class MyProject(info: ProjectInfo)
 *     extends DefaultProject(info)
 *     with MarkdownPlugin
 * {
 *     override def updateAction = markdownUpdate dependsOn(super.updateAction)
 *     override def cleanLibAction super.cleanLibAction dependsOn(markdownCleanLibAction)
 *     ...
 * }
 * }}}
 *
 * You can invoke the `markdown()` method within any task.
 */
trait MarkdownPlugin extends Project
{
    val MarkdownLibDir: String = "markdown_lib"

    /* ---------------------------------------------------------------------- *\
                       Managed External Dependencies
    \* ---------------------------------------------------------------------- */

    val ShowdownURL = "http://attacklab.net/showdown/showdown-v0.9.zip"
    val ShowdownLocal = MarkdownLibDir / "showdown.js"

    /* ---------------------------------------------------------------------- *\
                                   Tasks
    \* ---------------------------------------------------------------------- */

    lazy val markdownUpdateAction = task { doMarkdownUpdate }
    lazy val markdownCleanLibAction = task { doMarkdownCleanLib}

    /* ---------------------------------------------------------------------- *\
                                  Methods
    \* ---------------------------------------------------------------------- */

    /**
     * Run Markdown to convert a source (Markdown) file to HTML. The title
     * of the result HTML document is taken from the first line of the
     * Markdown document.
     *
     * @param markdownSource  the path to the source file
     * @param targetHTML      the path to the output file
     * @param log             logger to use
     */
    def markdown(markdownSource: Path, targetHTML: Path, log: Logger): Unit =
        markdown(markdownSource, targetHTML, log, None, None)

    /**
     * Run Markdown to convert a source (Markdown) file to HTML. The title
     * of the result HTML document is taken from the first line of the
     * Markdown document.
     *
     * @param markdownSource  the path to the source file
     * @param targetHTML      the path to the output file
     * @param log             logger to use
     * @param css             CSS file to haul inline, or None
     * @param externalJS      external Javascript script to reference, or None
     */
    def markdown(markdownSource: Path, 
                 targetHTML: Path,
                 log: Logger,
                 css: Option[Path],
                 externalJS: Option[String]): Unit =
    {
        // Use Rhino to run the Showdown (Javascript) Markdown converter.
        // MarkdownJ has issues and appears to be unmaintained.
        //
        // Showdown is here: http://attacklab.net/showdown/
        //
        // This code was adapted from various examples, including the one
        // at http://blog.notdot.net/2009/10/Server-side-JavaScript-with-Rhino

        import org.mozilla.javascript.{Context, Function}
        import java.io.{FileOutputStream,
                        FileReader,
                        OutputStreamWriter,
                        PrintWriter}
        import java.text.SimpleDateFormat
        import java.util.Date
        import scala.xml.parsing.XhtmlParser

        val Encoding = "ISO-8859-1"

        log.info("Generating \"" + targetHTML + "\" from \"" +
                 markdownSource + "\"")

        // Initialize the Javascript environment
        val ctx = Context.enter
        try
        {
            val scope = ctx.initStandardObjects

            // Open the Showdown script and evaluate it in the Javascript
            // context.

            val scriptPath = ShowdownLocal
            val showdownScript = fileLines(scriptPath) mkString "\n"

            ctx.evaluateString(scope, showdownScript, "showdown", 1, null)

            // Instantiate a new Showdown converter.

            val converterCtor = ctx.evaluateString(scope, "Showdown.converter",
                                                   "converter", 1, null)
                                .asInstanceOf[Function]
            val converter = converterCtor.construct(ctx, scope, null)

            // Get the function to call.

            val makeHTML = converter.get("makeHtml", 
                                         converter).asInstanceOf[Function]

            // Load the markdown source into a string, and convert it to HTML.

            val markdownSourceLines = fileLines(markdownSource).toList
            val markdownSourceString = markdownSourceLines mkString ""
            val htmlBody = makeHTML.call(ctx, scope, converter,
                                         Array[Object](markdownSourceString))

            // Prepare the final HTML.

            val cssLines = css match
            {
                case Some(path) => fileLines(path) mkString ""
                case None       => ""
            }

            val js = externalJS match
            {
                case Some(str) => <script type="text/javascript" src={str}/>
                case None      => <!-- no external JS -->
            }

            // Title is first line.
            val title = markdownSourceLines.head

            // Can't parse the body into something that can be interpolated
            // unless it's inside a single element. So, stuff it inside a
            // <div>. Use the id "body", which is necessary for the table
            // of contents stuff to work.
            val sHTML = "<div id=\"body\">" + htmlBody.toString + "</div>"
            val body = XhtmlParser(Source.fromString(sHTML))
            val out = new PrintWriter(
                          new OutputStreamWriter(
                              new FileOutputStream(targetHTML.absolutePath), 
                              Encoding))
            val contentType = "text/html; charset=" + Encoding
            val html = 
<html>
<head>
<title>{title}</title>
<style type="text/css">
{css}
</style>
{js}
<meta http-equiv="content-type" content={contentType}/>
</head>
<body onLoad="createTOC()">
{body}
<hr/>
<i>Generated {new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date)}</i>
</body>
</html>
            out.println(html.toString)
            out.close
        }

        finally
        {
            Context.exit
        }
    }

    /* ---------------------------------------------------------------------- *\
                              Private Methods
    \* ---------------------------------------------------------------------- */

    private def fileLines(path: Path): Iterator[String] =
        Source.fromFile(new File(path.absolutePath)).getLines

    private def doMarkdownUpdate: Option[String] =
    {
        // Download, unpack, and save the Showdown package.

        import java.net.URL

        FileUtilities.createDirectory(MarkdownLibDir, log)
        if (! ShowdownLocal.exists)
        {
            val destPath = Path.fromFile(new File(ShowdownLocal.absolutePath))
            def doInDirectory(dir: String)
                             (action: File => Either[String,String]):
                Either[String,String] =
            {
                val fDir = new File(dir)
                assert (fDir.exists && fDir.isDirectory)
                action(fDir)
            }

            FileUtilities.doInTemporaryDirectory[String](log)
            {
                tempDir: File =>

                log.info("Downloading and unpacking: " + ShowdownURL)
                FileUtilities.unzip(new URL(ShowdownURL),
                                    Path.fromFile(tempDir),
                                    log)

                val js = Path.fromFile(tempDir) / "src" / "showdown.js"
                assert (js.exists)
                log.info("Copying " + js + " to " + destPath)
                FileUtilities.copyFile(js, destPath, log)
                assert(destPath.asFile.exists)

                Right("")
            }
        }

        None
    }

    private def doMarkdownCleanLib: Option[String] =
    {
        if (ShowdownLocal.exists)
        {
            log.info("Deleting " + MarkdownLibDir);
            FileUtilities.clean(MarkdownLibDir, log);
        }

        None
    }
}
