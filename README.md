# CRCE - Component Repository supporting Compatibility Evaluation

CRCE is an experimental repository, designed to support research into component-based and modular systems undertaken by ReliSA research group at the Faculty of Applied Sciences, University of West Bohemia (http://relisa.kiv.zcu.cz).  The project website is over at Assembla: https://www.assembla.com/spaces/crce/wiki .

## Prerequisities

- **JDK 7** set in `JAVA_HOME` environment variable before starting CRCE 
  - there is a problem with running on JRE 8 and building on JDK 8
    - Web UI dependencies
	- pax module strongly depends on JRE7
- **MongoDB**, tested on v2.6.10, v3.4.10
- **Maven 3**, tested on 3.5.2

On linux, switching to JDK 7 for development/build can be done via `sudo update-alternatives --config java`. For basic switch between work environment could be used tool SDKMan (https://sdkman.io/).

## Build

1. `crce-parent` in `/pom` directory
2. `shared-build-settings` in `/build`
3. everything in `/third-party` (bash: `.../third-party$ for d in * ; do cd $d; mvn clean install; cd .. ; done`)
4. `crce-core-reactor` in `/core`
5. `crce-modules-reactor` in `/modules`

With bash interpereter can be used shortcut by running script build.bash, which run each step once every time. If there was build error during use of general build script the best practice to complete process is manualy pass all other steps.

On linux, step 3. can be perfomed via `.../third-party$ for d in * ; do cd $d ; mvn clean install ; cd .. ; done`.  In case of maven error "Received fatal alert: protocol_version", use `mvn -Dhttps.protocols=TLSv1.2 ...` after https://stackoverflow.com/a/50924208/261891.

## Start up

Build `crce-modules-reactor` in `/deploy`.

For run in docker run command in `/deploy`:

```docker build . -t crce-dock```

For run on local machine run command in `/deploy`:

Run CRCE using Maven plugin for pax in `crce-modules-reactor` module (i.e. `/deploy` directory):

```mvn pax:provision```


In both cases the output log should write up some info about dependencies terminated by lines similar to the following:

```
Listening for transport dt_socket at address: 65505
____________________________
Welcome to Apache Felix Gogo

g! X 10, 2017 10:38:47 DOP. org.glassfish.jersey.server.ApplicationHandler initialize
INFO: Initiating Jersey application, version Jersey: 2.9.1 2014-06-01 23:30:50...
```

At the moment, a bunch of errors will probably come up:

```
[Fatal Error] :1:1: Content is not allowed in prolog.
```

The cause of the latter is a badly loaded binary of mathematical solver which does not affect common application run. Any other error/exception (typically OSGi complaining about a thing) is a problem that needs to be examined as such. However, it should not happen with this version.

Started up, the application is accessible at:

- web UI: http://localhost:8080/crce
- REST web services: http://localhost:8080/rest/v2/

Updated (more or less) REST WS documentation is available at [Apiary](https://crceapi.docs.apiary.io/).

### lpsolve installation

To solve the issue with mathematical solver, you need to install [lpsolve library](https://sourceforge.net/projects/lpsolve/) to your computer. To do that, follow [their guide](http://lpsolve.sourceforge.net/5.5/Java/README.html#install) step by step.

> Note that on Windows, you do not have to place the libs to `\WINDOWS` or `\WINDOWS\SYSTEM32` as the guide states. Put it wherever you wish and add the directory to your `Path`.

## Code updates

After modifying a part of code, only the parental module needs to be rebuilt (no need to rebuild all). After that, the pax process must be restarted.
