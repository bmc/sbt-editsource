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

import scala.util.matching.Regex
import scala.Enumeration
import scala.annotation.tailrec

import java.text.SimpleDateFormat
import java.util.Date

import grizzled.string.template.UnixShellStringTemplate

private[editsource] class EditSourceStringTemplate(vars: Map[String, String]) {
  private val delegate = new UnixShellStringTemplate(dereference _,
                                                     """[a-zA-Z0-0_.]+""",
                                                     true)
  private val TimeFormat = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss")
  private val DateFormat = new SimpleDateFormat("yyyy/MM/dd")

  @inline final def substitute(line: String) = delegate.sub(line)

  def dereference(varName: String): Option[String] = {
    if (varName.trim == "")
      None
    else if (varName.startsWith("env."))
      env(varName.drop(4))
    else if (varName.startsWith("sys."))
      sys(varName.drop(4))
    else
      vars.get(varName)
  }

  private def env(s: String): Option[String] = {
    val result = System.getenv(s)
    if (result == null) None else Some(result)
  }

  private def sys(s: String): Option[String] = {
    val result = s match {
      case "now"   => TimeFormat.format(new Date)
      case "today" => DateFormat.format(new Date)
      case _       => System.getProperty(s)
    }

    if (result == null) None else Some(result)
  }
}
