# Introduction


# Installation

The code in this repository has been tested on:

+ macOS 10.14.6
+ Ubuntu 18.04 Linux

It probably also works well on other versions of macOS and Ubuntu
Linux that are not too far away from those.

Software that is expected to be installed for at least some parts of
the code in this repository to work:

+ Java JDK 8 or later
+ Clojure CLI tools: https://clojure.org/guides/getting_started
+ `ts` command in the `moreutils` package:
  + macOS with Homebrew: `brew install moreutils`
  + macOS with MacPorts: `sudo port install moreutils`
  + Ubuntu Linux: `sudo apt-get install moreutils`
+ For macOS, the `greadlink` command in `coreutils` package:
  + macOS with Homebrew: `brew install coreutils`

# Commands

```bash
$ ./scripts/run-with-top.sh macos-10.4.6-adoptopenjdk-11.0.9.1 ./scripts/run-bench1.sh
```

# License

Copyright © 2020 Andy Fingerhut

This program and the accompanying materials are made available under the
terms of the Eclipse Public License 1.0 which is available at
http://www.eclipse.org/org/documents/epl-v10.html
