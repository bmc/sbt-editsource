/*
  ---------------------------------------------------------------------------
  This software is released under a BSD license, adapted from
  http://opensource.org/licenses/bsd-license.php

  Copyright (c) 2010-2011, Brian M. Clapper
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
import Keys._
import Project.Initialize

import java.io.{File, FileWriter, PrintWriter}
import java.text.SimpleDateFormat
import java.util.Date

import scala.io.Source
import scala.util.matching.Regex
import scala.Enumeration
import scala.annotation.tailrec

import grizzled.file.{util => FileUtil}

/**
 * Plugin for SBT (Simple Build Tool) that provides a task to edit a source
 * file. Similar, in concept, to a copy-filter in Ant.
 *
 * To use this plugin, mix it into your SBT project.
 */
object EditSource extends Plugin
{
    // -----------------------------------------------------------------
    // Classes, traits, implicits
    // -----------------------------------------------------------------

    /**
     * A substitution option (e.g., "substitute all")
     */
    sealed trait SubOpt {val code: Int}

    /**
     * Default: No substitutions.
     */
    case object NoSubOpts extends SubOpt { val code = 0 }

    /**
     * Substitute all occurrences of the matched expression in each
     * line, instead of just the first occurrence.
     */
    case object SubAll extends SubOpt { val code = 1 }

    /**
     * Container for substitution options.
     */
    sealed class SubOpts(options: List[SubOpt])
    {
        // Create a bit mask of the code values.

        private[editsource] val codes =
            options.map{_.code}.foldLeft(0) {(a, b) => a | b}

        
        def this(opt: SubOpt) = this(List(opt))

        /**
         * Determine whether a set of substitution options contains a
         * particular option.
         */
        @inline final def contains(opt: SubOpt) = (codes & opt.code) != 0
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
    case class Substitution(val re:           Regex,
                            val replacement:  String,
                            val options:      SubOpts)

    // -----------------------------------------------------------------
    // Plugin Settings and Task Declarations
    // -----------------------------------------------------------------

    val EditSource = config("editsource") extend(Runtime)

    // FIXME: Need to hook into clean.

    // Update with:
    //
    // variables in EditSource <+= organization {org => ("organization", org)}
    // variables in EditSource += ("foo", "bar")
    val variables = SettingKey[Seq[Tuple2[String, String]]](
        "variables", "variable -> value mappings")

    // e.g., replace all instances of "foo" (caseblind) with "bar", but
    // only if "foo" appears by itself.
    // substitutions in EditSource += ("""(?i)\bfoo\b""".r, "bar")
    val substitutions = SettingKey[Seq[Substitution]](
        "substitutions", "regex -> replacement strings")

    // sources is a list of source files to edit.
    val sourceFiles = SettingKey[Seq[File]]("source-files",
                                            "List of sources to edit")

    // targetDirectory is the directory where edited files are to be
    // written. Directory structure is NOT preserved.
    val targetDirectory = SettingKey[File]("target-directory",
                                           "Where to copy edited files")

    // Whether or not to flatten the directory structure.
    val flatten = SettingKey[Boolean]("flatten",
                                      "Don't preserve source directory " +
                                      "structure.")

    val edit = TaskKey[Unit]("edit", "Fire up the editin' engine.")
    val clean = TaskKey[Unit]("clean", "Remove target files.")

    private val DateFormat = new SimpleDateFormat("yyyyy/mm/dd")

    val editSourceSettings: Seq[sbt.Project.Setting[_]] =
    inConfig(EditSource)(Seq(

        variables := Seq(("today", DateFormat.format(new Date))),
        variables <+= scalaVersion (sv => ("scalaVersion", sv)),
        variables <+= baseDirectory (bd => ("baseDirectory", bd.absolutePath)),

        flatten := true,

        substitutions := Seq.empty[Substitution],

        sourceFiles := Seq.empty[File],

        targetDirectory <<= baseDirectory(_ / "target"),

        edit <<= editTask,
        clean <<= cleanTask
    )) ++
    inConfig(Compile)(Seq(
        // Hook our clean into the global one.
        clean in Global <<= (clean in EditSource).identity
    ))

    // -----------------------------------------------------------------
    // Public Methods
    // -----------------------------------------------------------------

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
    def sub(regex: Regex, substitution: String, opts: SubOpts = NoSubOpts) =
        Substitution(regex, substitution, opts)

    // -----------------------------------------------------------------
    // Task Implementations
    // -----------------------------------------------------------------

    private def cleanTask: Initialize[Task[Unit]] =
    {
        (sourceFiles, targetDirectory, baseDirectory, flatten, streams) map 
        {
            (sourceFiles, targetDirectory, baseDirectory, flatten, streams) =>

            for (sourceFile <- sourceFiles)
            {
                val targetFile = targetFor(sourceFile,
                                           targetDirectory,
                                           baseDirectory,
                                           flatten)
                if (targetFile.exists)
                {
                    streams.log.debug("Deleting \"%s\"" format targetFile)
                    targetFile.delete
                }
            }
        }
    }

    private def editTask: Initialize[Task[Unit]] =
    {
        (sourceFiles, variables, substitutions, targetDirectory,
         baseDirectory, flatten, streams) map
        {
            (sources, variables, subs, target, base, flatten, streams) =>

            val varMap = variables.toMap
            val log = streams.log
            sources map editSource(varMap, subs, target, base, flatten, log)
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
                          (sourceFile: File): Unit =
    {
        val targetFile = targetFor(sourceFile,
                                   targetDirectory,
                                   baseDirectory,
                                   flatten)

        if (targetFile.exists && (! (sourceFile newerThan targetFile)))
        {
            log.debug("\"%s\" is up-to-date." format targetFile.toString)
        }

        else
        {
            val targetPath = targetFile.toString
            log.info("Editing \"%s\" to \"%s\"" format (sourceFile.toString,
                                                        targetPath))

            val parentDir = new File(FileUtil.dirname(targetPath))
            log.debug("Ensuring that \"%s\" exists." format parentDir)

            if (parentDir.exists)
                log.debug("\"%s\" already exists." format parentDir)
            else
            {
                log.debug("Creating \"%s\"." format parentDir)
                if (! parentDir.mkdirs())
                    throw new Exception("Can't create \"%s\"" format parentDir)
            }

            val out = new PrintWriter(new FileWriter(targetFile))
            val in = Source.fromFile(sourceFile)
            val varSub = new EditSourceStringTemplate(variables)

            for (line <- in.getLines())
            {
                // Apply all variables, then the regexs.

                out.println(applyRegexs(varSub.substitute(line),
                                        substitutions.toList))
            }

            out.close()
        }
    }

    private def applyRegexs(line: String,
                            substitutions: List[Substitution]): String =
    {
        def doSub(s: String, sub: Substitution): String =
        {
            if (sub.options contains SubAll)
                sub.re.replaceAllIn(s, sub.replacement)
            else
                sub.re.replaceFirstIn(s, sub.replacement)
        }

        @tailrec 
        def doSubs(s: String, subsLeft: List[Substitution]): String =
        {
            subsLeft match
            {
                case Nil =>
                    s

                case sub :: rest =>
                    doSubs(doSub(s, sub), rest)
            }
        }

        doSubs(line, substitutions)
    }

    private def targetFor(sourceFile: File,
                          targetDirectory: File,
                          baseDirectory: File,
                          flatten: Boolean = true): File =
    {
        if (flatten)
        {
            Path(targetDirectory) / Path(sourceFile).name
        }

        else
        {
            val sourcePath = sourceFile.absolutePath
            val targetDirPath = targetDirectory.absolutePath
            val basePath = baseDirectory.absolutePath
            if (! sourcePath.startsWith(basePath))
            {
                throw new Exception("Can't preserve directory structure for " +
                                    "\"%s\", since it isn't under the base " +
                                    "directory \"%s\""
                                    format (sourcePath, basePath))
            }

            Path(targetDirectory) / sourcePath.drop(basePath.length)
        }
    }
}
