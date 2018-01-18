# SLRP – Extending the Experiment

[![Build Status](https://travis-ci.org/hsuhw/slrp-ext.svg?branch=develop)](https://travis-ci.org/hsuhw/slrp-ext)
[![Coverage](https://codecov.io/gh/hsuhw/slrp-ext/graph/badge.svg)](https://codecov.io/gh/hsuhw/slrp-ext)

This project is a reimplementation of the CAV'16 paper by
Anthony W. Lin and Philipp Rümmer.  Made with a reference to the
[original](https://github.com/uuverifiers/autosat/tree/master/LivenessProver)
implementation, it is planned for exploring some further experiments.

## Usage

The project is compiled using Gradle (tested with Gradle 4.2).  After
a proper Gradle installation, simply run `build` command under the project
directory:

```
$ gradle build
```

Verify that all the tests have passed.  And `./slrp-ext` should be
ready for use:

```
$ ./slrp-ext -h
usage: slrp-ext [OPTIONS] FILE

options:
  -h,--help                print this message and exit
  -l,--log-level=<LEVEL>   set the logging level ("debug"|"info"|"warn"|"error"|"fatal")
                           (default "warn")
  -m,--mode=<MODE>         set the mode ("exp"|"cav16mono") (default "exp")
  -v,--version             print the version information and exit
```
