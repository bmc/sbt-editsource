---
title: "sbt-editsource: An SBT plugin for editing files"
layout: withTOC
---

# Introduction

[sbt-editsource][] is a text substitution plugin for [SBT][] 0.10.x. In a
way, it's a poor man's [*sed*][](1), for SBT. It provides the ability to
apply line-by-line substitutions to a source text file, producing an edited
output file. It supports two kinds of edits:

* Variable substitution, where `${var}` is replaced by a value.
* *sed*-like [regular expression][] substitution.

Each of these capabilities is explained in more detail, below.

**This plugin only works with SBT 0.10.x.** If you're using SBT 0.7, there's
an older version (with fewer features and a different variable syntax)
[here](http://software.clapper.org/sbt-plugins/editsource.html).

# Getting the Plugin

First, within your SBT project, create `project/plugins/build.sbt` (if it
doesn't already exist) and add the following:

    // The plugin is only published for 2.8.1
    libraryDependencies <<= (scalaVersion, libraryDependencies) { (scv, deps) =>
        if (scv == "2.8.1")
            deps :+ "org.clapper" %% "sbt-editsource" % "0.4"
        else
            deps
    }

Next, in your main project `build.sbt` file, add:

    seq(org.clapper.sbt.editsource.EditSource.editSourceSettings: _*)

Now the plugin is available to your SBT builds.

# Settings

The plugin provides the following new settings.


---

**`sourceFiles`**

---

The source files to be edited. For instance, suppose you want to edit all
files under "src" ending in ".txt". To do so, use:

    sourceFiles in EditSource <++= baseDirectory(d => (d / "src" * "*.txt").get)

If you also want to apply the edits to all files ending in ".md", use either:

    sourceFiles in EditSource <++= baseDirectory(d => (d / "src" * "*.txt").get)

    sourceFiles in EditSource <++= baseDirectory(d => (d / "src" * "*.md).get)
    
or, more succinctly:

    sourceFiles in EditSource <++= baseDirectory { dir =>
        (dir / "src" * "*.txt").get ++
        (dir / "src" * "*.md").get
    }

---

**`targetDirectory`**

---

The directory to which to write the edited versions of the source files.
For example:

    targetDirectory in EditSource <<= baseDirectory(_ / "target")

See also `flatten`, below.


---

**`flatten`**

---

If `flatten` is `true`, then the edited files will all be placed directly
in `targetDirectory`; if there are name clashes, then some files will be
overwritten. If `flatten` is `false`, then the partial path to each source
file is preserved in the target directory.

An example will help clarify. Consider the following file tree:
 
![Directory tree](tree.png)

Let's assume you're editing all the files ending in ".md", into the *target*
directory.

    sourceFiles in EditSource <++= baseDirectory(d => (d / "src" * "*.md).get)

    targetDirectory in EditSource <<= baseDirectory(_ / "target")
    
If you also set:

    flatten in EditSource := true

the edit operation will put all the edited versions of all three files
directly in the *target* directory.

If, instead, you set:

    flatten in EditSource := false

you'll end up with the following edited versions:

* _target/src/main/code/overview.md_
* _target/src/main/code/design.md_
* _target/src/doc/user-guide.md_

---

**`variables`**

---

`variables` is a sequence of `(variableName, value)` pairs. For instance,
the following two lines define (a) a `${projectName}` variable that
substitutes the name of the project, and (b) `${author}` variable:

    name := "my-project"

    variables in EditSource <+= name {name => ("projectName", name)}

    variables in EditSource += ("author", "Brian Clapper")

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

---

**`substitutions`**

---

`substitutions` is a sequence of [regular expression][] substitutions, of
the form:

    sub(regex, replacement)
    sub(regex, replacement, flags)

There's only one (optional) flag right now:

* `SubAll`: replace all occurrences of the regular expression in each line,
  not just the first occurrence.
  
Additional flags can be specified inline, as indicated in the documentation
for the [java.util.regex.Pattern][] class.

For example, to replace the first occurrence of the word "test" in each
line with "TEST", without regard to case, you might use:

    substitutions in EditSource += sub("""(?i)\btest\b""".r, "TEST")

The "`(?i)`" is the embedded option sequence that tells the regular expression
parser to use case-blind comparison.

Similarly, to replace all occurrences of the word "test", *with* regard to
case, you might use:

    substitutions in EditSource += sub("""\btest\b""".r, "TEST", SubAll)

You can specify multiple substitutions, of course:

    substitutions in EditSource ++= Seq(
        sub("""^#""".r, "//"),
        sub("""\b(?i)simple build tool\b""".r, "Scalable Build Tool", SubAll)
    )

Caveats:

* Regular expression substitutions are run *after* variable substitutions.
* Regular expression groups are not yet supported. 

# Tasks

The plugin provides two new SBT tasks.

* `editsource:edit` performs the edits on each source file that is out
  of date with respect to its corresponding target file. If no variable
  substitutions or regular expression substitutions are specified,
  `editsource:edit` does nothing.

* `editsource:clean` deletes all target edited files. `editsource:clean`
  is also automatically linked into the main SBT `clean` task.

# Restrictions

* Regular expression groups are not yet supported. (They will be, though.)
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

This software is copyright &copy; 2010-2011 Brian M. Clapper and is
released under a [BSD License][].

# Patches

I gladly accept patches from their original authors. Feel free to email
patches to me or to fork the [GitHub repository][] and send me a pull
request. Along with any patch you send:

* Please state that the patch is your original work.
* Please indicate that you license the work to the Grizzled-Scala project
  under a [BSD License][].

[BSD License]: license.html
[sbt-edit-source]: http://software.clapper.org/sbt-editsource
[Scala]: http://www.scala-lang.org/
[GitHub repository]: http://github.com/bmc/sbt-editsource
[GitHub]: https://github.com/bmc/
[Scala Tools Maven repository]: http://www.scala-tools.org/repo-releases/
[SBT]: https://github.com/harrah/xsbt/
[bmc@clapper.org]: mailto:bmc@clapper.org
[changelog]: CHANGELOG.html
[sed]: http://en.wikipedia.org/wiki/Sed
[regular expression]: http://download.oracle.com/javase/6/docs/api/java/util/regex/Pattern.html
[java.util.regex.Pattern]: http://download.oracle.com/javase/6/docs/api/java/util/regex/Pattern.html
