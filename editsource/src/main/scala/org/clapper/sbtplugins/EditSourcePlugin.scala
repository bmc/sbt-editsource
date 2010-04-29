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
 * edit a source.
 *
 * To use this plugin, mix it into your SBT project.
 */
trait EditSourcePlugin extends Project
{
    /* ---------------------------------------------------------------------- *\
                                  Methods
    \* ---------------------------------------------------------------------- */

    /**
     * Edits a source, substituting variable references. Variable references
     * look like @var@. The supplied map is used to find variable values; the
     * keys are the variable names, without the @ characters. Any variable that
     * isn't found in the map is silently ignored. This capability is similar
     * to the substitution capabilities provided by Ant's filter sets.
     *
     * @param in    the source to read
     * @param vars  the variables
     *
     * @return the lines from the source, with appropriate substitutions.
     */
    def editSourceToList(in: Source, vars: Map[String, String]): List[String] =
    {
        def doEdits(line: String, keys: List[String]): String =
        {
            keys match
            {
                case Nil => 
                    line

                case key :: tail =>
                    val value = vars(key)
                    doEdits(line.replaceAll("@" + key + "@", value), tail)
            }
        }

        in.getLines.map(doEdits(_, vars.keys.toList)).toList
    }

    /**
     * Edits a source, substituting variable references and writing the
     * result to a file. Variable references look like @var@. The supplied
     * map is used to find variable values; the keys are the variable
     * names, without the @ characters. Any variable that isn't found in
     * the map is silently ignored. This capability is similar to the
     * substitution capabilities provided by Ant's filter sets.
     *
     * @param in    the source to read
     * @param vars  the variables
     * @param out   where to write the result. The file is overwritten.
     */
    def editSourceToFile(in: Source, vars: Map[String, String], out: File) =
    {
        import java.io.{FileWriter, PrintWriter}

        val fOut = new PrintWriter(new FileWriter(out))
        try
        {
            editSourceToList(in, vars).foreach(fOut.println(_))
        }

        finally
        {
            fOut.close
        }
    }
}
