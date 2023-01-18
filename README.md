# Proxima

[![standard-readme compliant](https://img.shields.io/badge/readme%20style-standard-brightgreen.svg?style=flat-square)](https://github.com/RichardLitt/standard-readme)

A general-purpose asynchronous pathfinding library for Minecraft, with an emphasis on performance and scalability. 

## Table of Contents

- [Background](#background)
- [Install](#install)
- [Usage](#usage)
- [Maintainers](#maintainers)
- [Contributing](#contributing)
- [License](#license)

## Background

This project began because it was necessary to implement an efficient, concurrent pathfinding system for my Minecraft server [Phantazm](https://github.com/PhantazmNetwork/PhantazmServer).

## Install

<a href="https://cloudsmith.io/~steanky/repos/element/packages/detail/maven/proxima-core/latest/a=noarch;xg=com.github.steanky/"><img src="https://api-prd.cloudsmith.io/v1/badges/version/steanky/proxima/maven/proxima-core/latest/a=noarch;xg=com.github.steanky/?render=true&show_latest=true" alt="Latest version of 'proxima-core' @ Cloudsmith" /></a>

Proxima binaries are hosted over on [Cloudsmith](https://cloudsmith.io/~steanky/repos/proxima). You can use Proxima by adding it as a dependency to your build management system of choice.

For Gradle, add the repository URL like this:

```groovy

repositories {
    maven {
        url 'https://dl.cloudsmith.io/public/steanky/proxima/maven/'
    }
}

```


And in your dependencies section:

```groovy

dependencies {

    implementation 'com.github.steanky:proxima-core:1.0.0'

}

```

(this assumes version 1.0.0, you'll probably want to grab the latest version above)


You can also build binaries directly from source:

```shell
git clone https://github.com/Steanky/Proxima.git
cd ./Proxima
./gradlew build
```

## Usage

Proxima uses Java 17 and Gradle 7.4 by default.

## Maintainers

[Steanky](https://github.com/Steanky)

## Contributing

PRs accepted.

## License

[GNU General Public License v3.0](LICENSE)

## Hosting

[![Hosted By: Cloudsmith](https://img.shields.io/badge/OSS%20hosting%20by-cloudsmith-blue?logo=cloudsmith&style=for-the-badge)](https://cloudsmith.com)

Package repository hosting is graciously provided by  [Cloudsmith](https://cloudsmith.com). Cloudsmith is the only fully hosted, cloud-native, universal package management solution, that enables your organization to create, store and share packages in any format, to any place, with total confidence.
