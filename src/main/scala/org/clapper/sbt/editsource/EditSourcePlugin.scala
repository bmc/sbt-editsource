/*
  ---------------------------------------------------------------------------
  This software is released under a BSD license, adapted from
  http://opensource.org/licenses/bsd-license.php

  Copyright (c) 2010-2015, Brian M. Clapper
  All rights reserved.

  Redistribution and use in source and binary forms, with or without
  modification, are permitted provided that the following conditions are
  met:

  * Redistributions of source code must retain the above copyright notice,
    this list of conditions and the following disclaimer.

  * Redistributions in binary form must reproduce the above copyright
    notice, this list of conditions and the following disclaimer in the
    documentation and/or other materials provided with the distribution.

  * Neither the names "clapper.org", "sbt-editsource", nor the names of any
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

package org.clapper.sbt.editsource

import sbt._
import sbt.Keys._
import java.io.{File, FileWriter, PrintWriter}
import java.text.SimpleDateFormat
import java.util.Date

import scala.io.Source
import scala.util.matching.Regex
import scala.annotation.tailrec
import scala.language.implicitConversions
import scala.util.{Failure, Success}

import grizzled.file.{util => FileUtil}

/**
 * Plugin for SBT (Simple Build Tool) that provides a task to edit a source
 * file. Similar, in concept, to a copy-filter in Ant.
 *
 * To use this plugin, mix it into your SBT project.
 */
object EditSourcePlugin extends AutoPlugin {

  override def requires = plugins.JvmPlugin

  // -----------------------------------------------------------------
  // Classes, traits, implicits
  // -----------------------------------------------------------------

  /**
   * A substitution option (e.g., "substitute all")
   */
  object SubOpt extends Enumeration {
    type SubOpt = Value

    val NoSubstitutionOpts = Value(0x01)
    val SubstituteAll      = Value(0x02)
  }

  import SubOpt._

  /**
   * Container for substitution options.
   */
  sealed class SubOpts(options: List[SubOpt]) {
    // Create a bit mask of the code values.

    private[editsource] val codes =
      options.map{_.id}.foldLeft(0) {(a, b) => a | b}

    def this(opt: SubOpt) = this(List(opt))

    /**
     * Determine whether a set of substitution options contains a
     * particular option.
     */
    @inline final def contains(opt: SubOpt) = (codes & opt.id) != 0
  }

  /**
   * Implicitly convert a single SubOpt to a SubOpts container. Useful
   * for outside usability.
   */
  implicit def subOptToSubOpts(opt: SubOpt): SubOpts = new SubOpts(opt)

  /**
   * Contains a substitution. Instead of instantiating this directly,
   * though, use the `sub` convenience function.
   */
  case class Substitution(re: Regex, replacement: String, options: SubOpts)

  // -----------------------------------------------------------------
  // Plugin Settings and Task Declarations
  // -----------------------------------------------------------------

  override def trigger = allRequirements

  object autoImport { // Stuff that's autoimported into the user's build.sbt

    val SubAll = SubOpt.SubstituteAll
    val NoSubOpts = SubOpt.NoSubstitutionOpts

    val EditSource = config("editsource")

    val targetDirectory = settingKey[File]("Where to copy the edited files")

    // Update with:
    //
    // variables in EditSource <+= organization {org => ("organization", org)}
    // variables in EditSource += ("foo", "bar")
    val variables = settingKey[Seq[(String, String)]](
      "variable -> value mappings"
    )


    // e.g., replace all instances of "foo" (caseblind) with "bar", but
    // only if "foo" appears by itself.
    // substitutions in EditSource += ("""(?i)\bfoo\b""".r, "bar")
    val substitutions = settingKey[Seq[Substitution]](
      "regex -> replacement strings"
    )

    // Whether or not to flatten the directory structure.
    val flatten = settingKey[Boolean](
      "Don't preserve source directory structure."
    )

    val edit = taskKey[Seq[File]]("Fire up the editin' engine.")

    val clean = TaskKey[Unit]("clean", "Remove target files") in EditSource

    private val DateFormat = new SimpleDateFormat("yyyy/MM/dd")

    lazy val baseSettings: Seq[Def.Setting[_]] = Seq(
      variables := Seq(("today", DateFormat.format(new Date))),
      variables += "baseDirectory" -> baseDirectory.value.absolutePath,
      variables += "scalaVersion" -> scalaVersion.value,

      flatten         := true,
      substitutions   := Seq.empty[Substitution],
      sources         := Seq.empty[File],
      targetDirectory := baseDirectory(_ / "target").value,

      edit in EditSource  := EditSourceRunner.edit(EditSource).value,
      clean in EditSource := EditSourceRunner.clean(EditSource).value
    )

    /**
     * Convenience method to make it easier to define substitutions
     * in a build file. Example:
     *
     * {{{
     * substitutions in EditSource := Seq(
     *     sub("""\b(?i)test\b""".r, "TEST", SubAll),
     *     sub("""\b(?i)simple build tool\b""".r, "Scalable Build Tool")
     * )
     * }}}
     *
     * @param regex        The regular expression to find
     * @param substitution The string to substitute
     * @param opts         Any additional options
     */
    def sub(regex:        Regex,
            substitution: String,
            opts:         SubOpts = NoSubOpts) = {
      Substitution(regex, substitution, opts)
    }
  }

  import autoImport._

  override lazy val projectSettings = inConfig(EditSource)(baseSettings)

  // -----------------------------------------------------------------
  // Implementation Stuff
  // -----------------------------------------------------------------

  object EditSourceRunner {
    def edit(config: Configuration): Def.Initialize[Task[Seq[File]]] = Def.task {
      val sourceFiles = (sources in EditSource).value
      val vars        = (variables in EditSource).value
      val subs        = (substitutions in EditSource).value
      val targetDir   = (targetDirectory in EditSource).value
      val baseDir     = baseDirectory.value
      val flattenTree = (flatten in EditSource).value
      val log         = streams.value.log

      sourceFiles map editSource(vars.toMap, subs, targetDir, baseDir,
                                 flattenTree, log)
    }

    def clean(config: Configuration): Def.Initialize[Task[Unit]] = Def.task {
      val sourceFiles = (sources in EditSource).value
      val targetDir   = (targetDirectory in EditSource).value
      val baseDir     = baseDirectory.value
      val flattenTree = (flatten in EditSource).value
      val log         = streams.value.log

      for (src <- sourceFiles) {
        val target = targetFor(src, targetDir, baseDir, flattenTree)
        if (target.exists) {
          log.debug(s"Deleting $target ...")
          target.delete
        }
      }
    }
  }

  // -----------------------------------------------------------------
  // Private Utility Methods
  // -----------------------------------------------------------------

  private def editSource(variables: Map[String, String],
                         substitutions: Seq[Substitution],
                         targetDirectory: File,
                         baseDirectory: File,
                         flatten: Boolean,
                         log: Logger)
                        (sourceFile: File): File = {
    val targetFile = targetFor(sourceFile,
                               targetDirectory,
                               baseDirectory,
                               flatten)

    if (targetFile.exists && (! (sourceFile newerThan targetFile)))
      log.debug("\"%s\" is up-to-date." format targetFile.toString)

    else {
      val targetPath = targetFile.toString
      log.info("Editing \"%s\" to \"%s\"" format (sourceFile.toString,
                                                  targetPath))

      val parentDir = new File(FileUtil.dirname(targetPath))
      log.debug("Ensuring that \"%s\" exists." format parentDir)

      if (parentDir.exists)
        log.debug("\"%s\" already exists." format parentDir)
      else {
        log.debug("Creating \"%s\"." format parentDir)
        if (! parentDir.mkdirs())
          throw new Exception("Can't create \"%s\"" format parentDir)
      }

      val out = new PrintWriter(new FileWriter(targetFile))
      val in = Source.fromFile(sourceFile)
      val varSub = new EditSourceStringTemplate(variables)

      for (line <- in.getLines()) {
        // Apply all variables, then the regexs.

        val subbedValue = varSub.substitute(line) match {
          case Failure(e)  => throw new Exception(s"Substitution error", e)
          case Success(s)  => s
        }

        out.println(applyRegexs(subbedValue, substitutions.toList))
      }

      out.close()
    }

    targetFile
  }

  private def applyRegexs(line: String,
                          substitutions: List[Substitution]): String = {
    def doSub(s: String, sub: Substitution): String = {
      if (sub.options contains SubstituteAll)
        sub.re.replaceAllIn(s, sub.replacement)
      else
        sub.re.replaceFirstIn(s, sub.replacement)
    }

    @tailrec def doSubs(s: String, subsLeft: List[Substitution]): String = {
      subsLeft match {
        case Nil         => s
        case sub :: rest => doSubs(doSub(s, sub), rest)
      }
    }

    doSubs(line, substitutions)
  }

  private def targetFor(sourceFile: File,
                        targetDirectory: File,
                        baseDirectory: File,
                        flatten: Boolean = true): File = {
    if (flatten)
      Path(targetDirectory) / Path(sourceFile).name

    else {
      val sourcePath = sourceFile.absolutePath
      val targetDirPath = targetDirectory.absolutePath
      val basePath = baseDirectory.absolutePath
      if (! sourcePath.startsWith(basePath)) {
        throw new Exception("Can't preserve directory structure for " +
                            "\"%s\", since it isn't under the base " +
                            "directory \"%s\""
                            format (sourcePath, basePath))
      }

      Path(targetDirectory) / sourcePath.drop(basePath.length)
    }
  }
}
