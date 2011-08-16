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
import Defaults._
import Project.Initialize

import java.io.{File, FileWriter, PrintWriter}
import java.text.SimpleDateFormat
import java.util.Date

import scala.io.Source
import scala.util.matching.Regex
import scala.Enumeration
import scala.annotation.tailrec

import grizzled.file.{util => FileUtil}
import grizzled.string.template.UnixShellStringTemplate

/**
 * Plugin for SBT (Simple Build Tool) that provides a task to edit a source
 * file. Similar, in concept, to a copy-filter in Ant.
 *
 * To use this plugin, mix it into your SBT project.
 */
object EditSource extends Plugin
{
    // -----------------------------------------------------------------
    // Plugin Settings and Tasks
    // -----------------------------------------------------------------

    sealed trait SubOpt {val code: Int}
    case object SubOnce extends SubOpt { val code = 1 }
    case object SubAll extends SubOpt { val code = 2 }

    private case class SubMapValue(replacement: String, code: SubOpt)

    type SubstitutionValue = Tuple3[Regex, String, SubOpt]

    val EditSource = config("editsource")

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
    val substitutions = SettingKey[Seq[SubstitutionValue]](
        "substitutions", "regex -> replacement strings")

    // sources is a list of source files to edit.
    val sourceFiles = SettingKey[Seq[File]]("source-files",
                                            "List of sources to edit")

    // targetDirectory is the directory where edited files are to be
    // written. Directory structure is NOT preserved.
    val targetDirectory = SettingKey[File]("target-directory",
                                           "Where to copy edited files")

    val edit = TaskKey[Unit]("edit", "Fire it up.")

    private val DateFormat = new SimpleDateFormat("yyyyy/mm/dd")

    def editSourceSettings: Seq[sbt.Project.Setting[_]] =
    inConfig(EditSource)(Seq(

        variables := Seq(("today", DateFormat.format(new Date))),
        variables <+= scalaVersion (sv => ("scalaVersion", sv)),
        variables <+= baseDirectory (bd => ("baseDirectory", bd.absolutePath)),

        substitutions := Seq.empty[Tuple3[Regex, String, SubOpt]],

        sourceFiles := Seq.empty[File],

        targetDirectory <<= baseDirectory(_ / "target"),

        edit <<= editTask
    ))

    // -----------------------------------------------------------------
    // Methods
    // -----------------------------------------------------------------

    private def editTask: Initialize[Task[Unit]] =
    {
        (sourceFiles, variables, substitutions, targetDirectory, streams) map
        {
            (sources, variables, substitutions, targetDirectory, streams) =>

            val varMap = variables.toMap
            for (source <- sources)
                editSource(source,
                           variables.toMap, 
                           substitutions,
                           targetDirectory,
                           streams.log)
        }
    }

    private def editSource(sourceFile: File,
                           variables: Map[String, String],
                           substitutions: Seq[SubstitutionValue],
                           targetDirectory: File,
                           log: Logger): Unit =
    {
        val subMap = Map(
            substitutions map {t => t._1 -> SubMapValue(t._2, t._3)}: _*
        )

        val targetFile = Path(targetDirectory) / Path(sourceFile).name

        if (targetFile.exists &&
            (sourceFile.lastModified <= targetFile.lastModified))
        {
            log.debug("\"%s\" is up-to-date." format targetFile.toString)
        }

        else
        {
            log.info("Editing \"%s\" to \"%s\"" format (sourceFile.toString,
                                                        targetFile.toString))
            val out = new PrintWriter(new FileWriter(targetFile))
            val in = Source.fromFile(sourceFile)
            val varMap = variables.toMap
            val varSub = new UnixShellStringTemplate(v => varMap.get(v), false)

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
                            substitutions: List[SubstitutionValue]): String =
    {
        def doSub(s: String, sub: SubstitutionValue): String =
        {
            val (re, replacement, opt) = sub

            val s2 = opt match
            {
                case SubOnce => re.replaceFirstIn(s, replacement)
                case SubAll  => re.replaceAllIn(s, replacement)
            }
            println(s + " -> " + s2)
            s2
        }

        @tailrec 
        def doSubs(s: String, subsLeft: List[SubstitutionValue]): String =
        {
            subsLeft match
            {
                case Nil =>
                    s

                case sub :: rest =>
                    doSubs(doSub(line, sub), rest)
            }
        }

        doSubs(line, substitutions)
    }
}
