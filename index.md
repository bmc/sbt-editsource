---
title: "sbt-editsource: An SBT plugin for editing files"
layout: withTOC
---

# Introduction

[sbt-editsource][] is a text substitution plugin for [SBT][] 0.13.x and
greater. In a way, it's a poor man's [*sed*][](1), for SBT. It provides the
ability to apply line-by-line substitutions to a source text file, producing an
edited output file. It supports two kinds of edits:

* Variable substitution, where `${var}` is replaced by a value.
* *sed*-like [regular expression][] substitution.

Each of these capabilities is explained in more detail, below.

# Getting the Plugin

Within your SBT project, create `project/plugins/build.sbt` (if it
doesn't already exist) and add the following:

```scala
addSbtPlugin("org.clapper" % "sbt-editsource" % "1.0.0")
```

The plugin is cross-built for both SBT 0.13.x and 1.0.x.


# Settings and Tasks

The plugin provides the following new settings and tasks.

**Note**: *sbt-editsource* uses predefined SBT settings, where possible (e.g.,
`sources`). Of course, that's not always possible. To be sure you're updating
the correct setting, *always* use the form:

```scala
settingName in EditSource
```

For instance:

```scala
flatten in EditSource := false
```

## Settings

The plugin provides the following new settings.

### `sources`

The source files to be edited. For instance, suppose you want to edit all
files under "src" ending in ".txt". To do so, use:

```scala
sources in EditSource ++= (baseDirectory.value / "src" * "*.txt").get
``` 

If you also want to apply the edits to all files ending in ".md", use either:

```scala
sources in EditSource ++= (baseDirectory.value / "src" * "*.txt").get
sources in EditSource ++= (baseDirectory.value / "src" * "*.md").get
```
    
or, more succinctly:

```scala
sources in EditSource ++= (baseDirectory.value / "src" * "*.txt").get ++
                          (baseDirectory.value / "src" * "*.md").get
```

### `targetDirectory`

The directory to which to write the edited versions of the source files.
For example:

```scala
targetDirectory in EditSource := baseDirectory.value / "target"
```


See also `flatten`, below.


### `flatten`

If `flatten` is `true`, then the edited files will all be placed
directly in `targetDirectory`; if there are name clashes, then
some files will be overwritten. If `flatten` is `false`, then the
partial path to each source file is preserved in the target directory.

An example will help clarify. Consider the following file tree:
 
![Directory tree](tree.png)

Let's assume you're editing all the files ending in ".md", into the *target*
directory.

```scala
sources in EditSource ++= (baseDirectory.value / "src" * "*.md").get
targetDirectory in EditSource := baseDirectory.value / "target"
```

With

```scala
flatten in EditSource := false
```

(which is the default), you'll end up with the following edited versions:

* `target/src/main/code/overview.md`
* `target/src/main/code/design.md`
* `target/src/doc/user-guide.md`

However, set:

```scala
flatten in EditSource := true
```

the edit operation will put all the edited versions of all three files
directly in the *target* directory, yielding:

* `target/overview.md`
* `target/design.md`
* `target/user-guide.md`

### `variables`

`variables` is a sequence of `(variableName, value)` pairs. Let's
take a look at some examples. We're going to define substitutions for three
keys:

* "name": The project's name, from the SBT configuration
* "version": The project version, also from the SBT configuration
* "author": The project's author, from a hard-coded string

```scala
variables in EditSource += "projectName" -> name.value
variables in EditSource += "version" -> version.value
variables in EditSource += "author" -> "Brian Clapper"
```

Once you've defined those variables, you're free to substitute them in
your files.

#### Predefined variables

Editsource provides some predefined variables for you:

* `baseDirectory`: The absolute path of the base (i.e., top-level) 
   project directory.
* `scalaVersion`: The current Scala version
* `today`: The current date, in `yyyy/mm/dd` form (e.g., "2018/01/01"). 
   (You can also use `sys.today`; see the next section.)
   
#### Using Variables in your Source Files

Inside a source file to be edited, variable references are of the form
`${varname}`, as in the Unix shell. A shortened `$varname` is also support.
The `${}` long form also supports a default syntax: `${varname?default}`.
If the reference variable has no value, then the default value is supplied,
instead. (The `?default` syntax is not supported for the short form
reference.)

With the above definitions in place, when the source files are edited, any
reference to `${projectName}` is replaced with "my-project", and any
reference to `${author}` is replaced with "Brian Clapper".

You can define any number of variables. If the edit logic encounters a
variable that isn't defined, it simply replaces the variable reference with
an empty string (like *bash* does).

In addition to the variables you define in your build file, the
*sbt-editsource* also honors the following special variable prefixes:

* `env.`: Any variable starting with `env.` is assumed to be an environment
  variable reference. For instance, `${env.HOME}` will substitute the value
  of the "HOME" environment variable.
* `sys.now`: The current date and time, in "yyyy/mm/dd HH:MM:ss" form. For
  example: `${sys.now}` might yield "2011/08/17 13:01:56"
* `sys.today`: The current date, in "yyyy/mm/dd" form.
* `sys.*something*`: Any other variable name starting with `sys.` is
  assumed to refer to a JVM system property and is resolved via a call to
  `java.lang.System.getProperty()`. Thus, `${sys.user.name}` substitutes
  the `user.name` property, and `${sys.java.io.tmpdir}` substitutes the
  `java.io.tmpdir` property.

### `substitutions`

`substitutions` is a sequence of [regular expression][] edits, of the form:

```scala
sub(regex, replacement)
sub(regex, replacement, flags)
```

There's only one (optional) flag right now:

* `SubAll`: replace all occurrences of the regular expression in each line,
  not just the first occurrence.
  
Additional flags can be specified inline, as indicated in the documentation
for the [java.util.regex.Pattern][] class.

For example, to replace the first occurrence of the word "test" in each
line with "TEST", without regard to case, you might use:

```scala
substitutions in EditSource += sub("""(?i)\btest\b""".r, "TEST")
```

The "`(?i)`" is the embedded option sequence that tells the regular expression
parser to use case-blind comparison.

Similarly, to replace all occurrences of the word "test", *with* regard to
case, you might use:

```scala
substitutions in EditSource += sub("""\btest\b""".r, "TEST", SubAll)
```

You can specify multiple substitutions, of course:

```scala
substitutions in EditSource ++= Seq(
    sub("""^#""".r, "//"),
    sub("""\b(?i)simple build tool\b""".r, "Scalable Build Tool", SubAll)
)
```

Also, regular expression [capturing groups][] are supported, so you can use
more complex regular expression substitutions like this:

```scala
// Remove everything up to, but not including the word "foo", but save
// the "foo" and everything after.

sub("""^.*(foo.*)$""".r, "$1")
```

*NOTE*: Regular expression substitutions are run *after* variable substitutions.

# Tasks

The plugin provides two new SBT tasks.

* `editsource:edit` performs the edits on each source file that is out
  of date with respect to its corresponding target file. If no variable
  substitutions or regular expression substitutions are specified,
  `editsource:edit` does nothing.

* `editsource:clean` deletes all target edited files. `editsource:clean`
  is also automatically linked into the main SBT `clean` task.

## Hooking the edit task into the compile phase

If you want the run `editsource:edit` every time you run `compile`, just
add this line to your `build.sbt`:

```
compile in Compile := ((compile in Compile) dependsOn (edit in EditSource)).value
```

# Restrictions

* Currently, *sbt-editsource* only supports one set of edits, applied to
  *all* specified files. That is, you cannot specify one set of edits for
  one group of files and a second set of edits for a different group of
  files. In the future, the plugin may be enhanced to support this
  capability.

# Change log

The change log for all releases is [here][changelog].

# Author

Brian M. Clapper, [bmc@clapper.org][]

# Copyright and License

This software is copyright &copy; 2010-2018 Brian M. Clapper and is
released under a [BSD License][].

# Patches

I gladly accept patches from their original authors. Feel free to email
patches to me or to fork the [GitHub repository][] and send me a pull
request. Along with any patch you send:

* Please state that the patch is your original work.
* Please indicate that you license the work to the Grizzled-Scala project
  under a [BSD License][].

[BSD License]: https://github.com/bmc/sbt-editsource/blob/master/LICENSE.md
[sbt-editsource]: http://software.clapper.org/sbt-editsource
[Scala]: http://www.scala-lang.org/
[GitHub repository]: http://github.com/bmc/sbt-editsource
[GitHub]: https://github.com/bmc/
[Scala Tools Maven repository]: http://www.scala-tools.org/repo-releases/
[SBT]: https://github.com/harrah/xsbt/
[bmc@clapper.org]: mailto:bmc@clapper.org
[changelog]: https://github.com/bmc/sbt-editsource/blob/master/CHANGELOG.md
[*sed*]: http://en.wikipedia.org/wiki/Sed
[regular expression]: http://download.oracle.com/javase/6/docs/api/java/util/regex/Pattern.html
[java.util.regex.Pattern]: http://download.oracle.com/javase/6/docs/api/java/util/regex/Pattern.html
[capturing groups]: http://download.oracle.com/javase/6/docs/api/java/util/regex/Pattern.html#cg
