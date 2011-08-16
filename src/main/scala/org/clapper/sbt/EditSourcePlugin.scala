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

package org.clapper.sbt

import sbt._
import Keys._
import Defaults._
import Project.Initialize

import java.io.File
import java.text.SimpleDateFormat
import java.util.Date

import scala.io.Source
import scala.util.matching.Regex

/**
 * Plugin for SBT (Simple Build Tool) that provides a task to edit a source
 * file. Similar, in concept, to a copy-filter in Ant.
 *
 * To use this plugin, mix it into your SBT project.
 */
object EditSourcePlugin extends Plugin
{
    // -----------------------------------------------------------------
    // Plugin Settings and Tasks
    // -----------------------------------------------------------------

    val EditSource = config("editsource")

    // Update with:
    //
    // variables in EditSource <+= organization {org => ("organization", org)}
    // variables in EditSource += ("foo", "bar")
    val variables = SettingKey[Seq[Tuple2[String, String]]](
        "variables", "variable -> value mappings")

    // e.g., replace all instances of "foo" (caseblind) with "bar", but
    // only if "foo" appears by itself.
    // substitutions in EditSource += ("""(?i)\bfoo\b""".r, "bar")
    val substitutions = SettingKey[Seq[Tuple2[Regex, String]]](
        "substitutions", "regex -> replacement strings")

    // sources is a list of source files to edit.
    val sources = TaskKey[Seq[File]]("sources", "List of sources to edit")

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

        edit <<= editTask
    ))

    // -----------------------------------------------------------------
    // Methods
    // -----------------------------------------------------------------

    private def editTask: Initialize[Task[Unit]] =
    {
        (sources, variables, substitutions, targetDirectory) map
        {
            (sources, variables, substitutions, targetDirectory) =>

            val varMap = variables.toMap
            for (source <- sources)
                editSource(source,
                           variables.toMap, 
                           substitutions.toMap,
                           targetDirectory)
        }
    }

    private def editSource(source: File,
                           variables: Map[String, String],
                           substitutions: Map[Regex, String],
                           targetDirectory: File): Unit =
    {
    }
}
