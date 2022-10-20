# Template

[![standard-readme compliant](https://img.shields.io/badge/readme%20style-standard-brightgreen.svg?style=flat-square)](https://github.com/RichardLitt/standard-readme)

A standard template repository for my Java projects.

This repository contains:

1. Initial build files to ease multi-module development
2. A default license
3. A default .editorconfig

## Table of Contents

- [Background](#background)
- [Install](#install)
- [Usage](#usage)
- [Maintainers](#maintainers)
- [Contributing](#contributing)
- [License](#license)

## Background

This repository began when I got tired of duplicating all my initial code for new products.

## Install

As a template repository, users are expected to generate their own repositories from this one using GitHub's template repository feature.

## Usage

Template uses Java 17 and Gradle 7.4 by default.

### Gradle Plugins

Template specifies a few plugins in `buildSrc`: `java-conventions` and `library-conventions`. Modules should generally rely on one or the other as follows (in their respective `build.gradle.kts`): 

```kotlin
plugins {
    id("template.java-conventions")
}
```

One can edit the plugin scripts in `buildSrc` to apply build configuration changes globally across all modules that use them.

`java-conventions` specifies the Maven public repository, as well as some dependencies: junit-jupiter, mockito, and jetbrains-annotations. It also configures the `Jar` task to copy the module or project license into any built jars.

`java-library` supplies everything in `java-conventions`, with the addition of also producing javadoc and source jars.

### Gradle Modules

By default, there is a single module, `template-java`, that is included by default. Module names should be prefixed by the root project name (`template` being the default). Module *directory* names should not have this prefix.

Assuming `git` is available from the command line, the buildscript will automatically generate (and add) a new `.gitignore` file which excludes the `./build` directory (relative to each module). 

## Maintainers

[Steanky](https://github.com/Steanky)

## Contributing

PRs accepted.

## License

[GNU General Public License v3.0](LICENSE)
