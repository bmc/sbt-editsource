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

import com.izforge.izpack.compiler.CompilerConfig

/**
 * Plugin for SBT (Simple Build Tool) that provides a method that will
 * configure and run the IzPack installer.
 *
 * To use this plugin, mix it into your SBT project.
 */
trait IzPackPlugin extends Project
{
    /* ---------------------------------------------------------------------- *\
                               Dependencies
    \* ---------------------------------------------------------------------- */

    /* ---------------------------------------------------------------------- *\
                                  Methods
    \* ---------------------------------------------------------------------- */

    /**
     * Build the actual installer jar.
     *
     * @param installerConfig  the IzPack installer configuration file (which
     *                         is an XML file that tells IzPack what to put
     *                         in the generated installer jar file).
     * @param installerJar     where to store the installer jar file
     */
    def izpackMakeInstaller(installFile: Path, installerJar: Path)
    {
        FileUtilities.withTemporaryDirectory(log)
        {
            baseDir =>

            val compilerConfig = new CompilerConfig(installFile.absolutePath,
                                                    baseDir.getPath, // basedir
                                                    CompilerConfig.STANDARD,
                                                    installerJar.absolutePath)
            log.info("Creating installer jar.")
            compilerConfig.executeCompiler
            None
        }
    }
}
