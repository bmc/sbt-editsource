# Change Log for sbt-editsource

**Version 1.0.0**

* Updated Grizzled Scala dependency to 4.4.1.
* Updated to cross-build against SBT 1.0.x and 0.13.x.

**Version 0.7.0**

Upgraded to SBT 0.13.x.

**Version 0.6.5:**

Fixed problem with dependency versioning.

**Version 0.6.3:**

* Updated to SBT 0.12.
* Now cross-builds against both Scala 2.9.1 and Scala 2.9.2.
* Doug Tangren's (excellent) [ls](http://ls.implicit.ly/) SBT plugin is now
  specified directly in the project, to make it easier for others to build
  this plugin.

**Version 0.6.2:**

* Rebuilt with newer version of Grizzled Scala, to fix a dependency problem.

**Version 0.6.1:**

Fixed Ivy-related publishing issue.

**Version 0.6:**

* Updated to SBT 0.11.2.
* Added support for *ls.implicit.ly* metadata.

**Version 0.5.2:**

`EditSource.editSourceSettings` is now deprecated, in favor of
just `EditSource.settings`.

**Version 0.5.1:**

[Issue #2][]: Corrected date/time formats.

[Issue #2]: https://github.com/bmc/sbt-editsource/issues/2

**Version 0.5:**

_Change in Setting and Task Key Namespace_

*sbt-editsource* setting and task keys are already inside in inner
`EditSource` object, for namespace scoping. This revision adds a trick by
[Josh Suereth][], to make usage easier. Basically, the keys are now defined
like this:

```scala
object EditSource extends Plugin {
  object EditSource {
    val Config = config("editsource") extend(Runtime)

    val sources = SettingKey[Seq[File]](
      "source-files", "List of sources to transform"
    ) in Config

    val targetDirectory = SettingKey[File](
      "target-directory", "Where to copy edited files"
    ) in Config

    ...
  }
}
```

Putting the `in Config` after *each* setting or task changes the `build.sbt`
usage pattern from the clunky

```scala
EditSource.sources in EditSource <<= ...
```
to the far more outrageously winning

```scala
EditSource.sources <<= ...
```

[Josh Suereth]: http://suereth.blogspot.com/

_Changes in Settings_

`EditSource.sourceFiles` is now `EditSource.sources`, for consistency with
other SBT plugins and settings.

Merged in a patch from [David M. Lee][], changing `editTask` to return
a `Seq[File]`, allowing one to add `editTask` as a `resourceGenerator`,
running it as part of the `package` command. For instance:

```scala
resourceGenerators in Compile <+=
  (EditSource.edit in EditSource.Config).identity
```

[David M. Lee]: https://github.com/leedm777

**Version 0.4.2:**

Put plugin settings into a sub-object, so they don't clash with
other settings on (auto) import into `build.sbt`. Accessing a setting
is now accomplished with:

```scala
EditSource.configFile in EditSource.Config <<= baseDirectory(...)
```

* Converted code to conform with standard Scala coding style.
* Now published for Scala 2.9.0 and 2.9.0-1, as well as 2.8.1.

**Version 0.4.1:**

Renamed various plugin settings and variables, so their names wouldn't
clash, on import, with other plugins.

**Version 0.4:**

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

**Version 0.3:**

Now published to the [Scala Tools Maven repository][], which [SBT][]
includes by default. Thus, if you're using SBT, it's longer necessary to
specify a custom repository to find this artifact.

[Scala Tools Maven repository]: http://www.scala-tools.org/repo-releases/
[SBT]: http://code.google.com/p/simple-build-tool/

**Version 0.3.1:**

The Markdown plugin still uses the Showdown Javascript Markdown parser,
but the host domain (`attacklab.net`) is offline. Switched to use a
copy cached in the `sbt-plugins` GitHub downloads area.

**Version 0.2:**

* The [IzPack plugin][] now provides support for configuring the installer
  directly within SBT, using Scala, rather than XML.
* Web pages now exist for each plugin, including a comprehensive one
  for the IzPack plugin.

[IzPack plugin]: http://software.clapper.org/sbt-plugins/izpack.html

**Version 0.2.2:**

Now uses [Posterous-SBT][] SBT plugin.

[Posterous-SBT]: http://github.com/n8han/posterous-sbt

**Version 0.2.1:**

* Changed [Markdown plugin][] to permit insertion of an arbitrary list of
  HTML nodes in the generated HTML `head` section.
* Internal changes to the [IzPack plugin][] to use `map()` calls instead of
  `for` and `yield`.

[IzPack plugin]: http://software.clapper.org/sbt-plugins/izpack.html
[Markdown plugin]: http://software.clapper.org/sbt-plugins/markdown.html

**Version 0.1:**

First release to the web.


