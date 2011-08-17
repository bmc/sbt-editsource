---
title: "Change Log: sbt-editsource"
layout: default
---

Version 0.4:

* Pulled the EditSource plugin into its own project and GitHub repo.
* Refactored and reimplemented as an [SBT][] 0.10.x plugin.
* Changed syntax of variable references from `@varname@` to `${varname}`.
* Variable substitution now supports special `env` and `sys` prefixes.
  `${env.VAR}` substitutes `VAR` from the environment. (e.g., `${env.HOME}`)
  `${sys.now}` substitutes the current date and time in "yyyy/mm/dd HH:MM:SS"
  format. `${sys.today}` substitutes the current date in "yyyy/mm/dd" format.
  `${sys.anything_else}` attempts to resolve the variable from the JVM's
  `System.properties` list (e.g., `${sys.user.name}`, `${sys.java.io.tmpdir}`)
* Plugin now supports regular expression substitution.

[SBT]: http://code.google.com/p/simple-build-tool/

Version 0.3:

* Now published to the [Scala Tools Maven repository][], which [SBT][]
  includes by default. Thus, if you're using SBT, it's longer necessary to
  specify a custom repository to find this artifact.

[Scala Tools Maven repository]: http://www.scala-tools.org/repo-releases/
[SBT]: http://code.google.com/p/simple-build-tool/

Version 0.3.1:

* The Markdown plugin still uses the Showdown Javascript Markdown parser,
  but the host domain (`attacklab.net`) is offline. Switched to use a
  copy cached in the `sbt-plugins` GitHub downloads area.

Version 0.2:

* The [IzPack plugin][] now provides support for configuring the installer
  directly within SBT, using Scala, rather than XML.
* Web pages now exist for each plugin, including a comprehensive one
  for the IzPack plugin.

[IzPack plugin]: http://software.clapper.org/sbt-plugins/izpack.html

Version 0.2.2:

* Now uses [Posterous-SBT][] SBT plugin.

[Posterous-SBT]: http://github.com/n8han/posterous-sbt

Version 0.2.1:

* Changed [Markdown plugin][] to permit insertion of an arbitrary list of
  HTML nodes in the generated HTML `head` section.
* Internal changes to the [IzPack plugin][] to use `map()` calls instead of
  `for` and `yield`.

[IzPack plugin]: http://software.clapper.org/sbt-plugins/izpack.html
[Markdown plugin]: http://software.clapper.org/sbt-plugins/markdown.html

Version 0.1:

First release to the web.


