# jar-dependencies #

* [![Build Status](https://secure.travis-ci.org/mkristian/jar-dependencies.png)](http://travis-ci.org/mkristian/jar-dependencies)
* [![Code Climate](https://codeclimate.com/github/mkristian/jar-dependencies.png)](https://codeclimate.com/github/mkristian/jar-dependencies)

add gem dependencies for jar files to ruby gems.

## getting control back over your jar ##

jar dependencies are declared in the gemspec of the gem using the same notation as <https://github.com/mkristian/jbundler>.

when using ```require_jar``` to load the jar into JRuby's classloader then an version conflict will be detected and only **ONE** jar gets loaded. jbundler allows to select the version suitable for you application.

most maven-artifact do **NOT** use versions ranges but depends pick a version. then jbundler can always **overwrite** any such version.

## vendoring your jars before packing the jar ##

add to your Rakefile following:

    require 'jar_installer'
    task :install_jars do
      Jars::JarInstaller.vendor_jars
    end

which will install download the dependent jars into **JARS_HOME** and creats a file **lib/my_gem_jars.rb** which is just an enumeration of ```require_jars``` statements to load all the jars. the **vendor_jars** will copy them into the **lib** directory of the gem.

the location where jars are cached is per default **$HOME/.m2/repository** the same default as maven has to cache the jar-artifacts. it respects **$HOME/.m2/settings.xml** from maven with all its mirror and other settings or the environment variable **JARS_HOME**.

IMPORTANT: make sure that jar-dependencies is only a **development dependency** of your gem. if it is a runtime dependencies the require_jars file will be overwritten during installation.

## reduce the download and reuse the jars from maven local repository ##

if you do not vendor the jars into gem then the **jar-dependency** gem can vendor them when you install the gem. just skip use **Jars::JarInstaller.install_jars** from the above rake tasks.

until JRuby itself comes with this jar-dependencies as default gem, for this feature to work you need to install the **jar-dependencies** gem first and for bundler you need to use the **bundle-with-jars** command :(

IMPORTANT: make sure that jar-dependencies is a **runtime dependency** of your gem so the require_jars file will be overwritten during installation with the "correct" versions of the jars.

## for development you do not need to vendor the jars at all ##

just set an environment variable

    export JARS_VENDOR=false

this tells the jar_installer not vendor any jars but only create the file with the ```require_jar``` statements. this ```require_jars``` method will find the jar inside the maven local repository and loads it from there.

## some drawbacks ##

    * first you need to install the jar-dependency gem with its development dependencies installed (then ruby-maven gets installed as well)
	* bundler does not install the jar-dependencies (unless JRuby adds the gem as default gem)
	* you need ruby-maven doing the job of dependency resolution and downloading them. gems not part of <http://rubygems.org> will not work currently

## jar others then from maven-central ##

per default all jars need to come from maven-central (<search.maven.org>), in order to use jars from any other repo you need to add it into your settings.xml and configure it in a way that you use it without interactive prompt (username + passwords needs to be part of settings.xml).

**NOTE:** gems depending on jars other then maven-central will **NOT** work when they get published on rubygems.org since the user of those gems will not have the right settings.xml to allow them to access the jar dependencies.

## just look at the example ##

the [readme.md](example/Readme.md) walks you through an example and shows how development works and shows what happens during installation.

# configuration #

<table border='1'>
<tr>
<td>ENV</td><td>java system property</td><td>default</td><td>description</td>
</tr>
<tr>
<td>`JARS_DEBUG`</td><td>jars.debug</td><td>false</td><td>if set to true it will produce lots of debug out (maven -X switch)</td>
</tr>
<tr>
<td>`JARS_VERBOSE`</td><td>jars.verbose</td><td>false</td><td>if set to true it will produce some extra output</td>
</tr>
<tr>
<td>`JARS_HOME`</td><td>jars.home</td><td>$HOME/.m2/repository</td><td>filesystem location where to store the jar files and some metadata</td>
</tr>
<tr>
<td>`JARS_MAVEN_SETTINGS`</td><td>jars.maven.settings</td><td>$HOME/.m2/settings.xml</td><td>setting.xml for maven to use</td>
</tr>
<tr>
<td>`JARS_VENDOR`</td><td>jars.vendor</td><td>true</td><td>set to true means that the jars will be stored in JARS_HOME only</td>
</tr>
<tr>
<td>`JARS_SKIP`</td><td>jars.skip</td><td>true</td><td>do **NOT** install jar dependencies at all</td>
</tr>
</table>

# motivation #

just today I tumbled across [https://github.com/arrigonialberto86/ruby-band](https://github.com/arrigonialberto86/ruby-band) which usees jbundler to manage their jar dependencies which happens on the first 'require "ruby-band"'. their is no easy or formal way to find out which jars are added to jruby-classloader.

another issue was brought to my notice yesterday [https://github.com/hqmq/derelicte/issues/1](https://github.com/hqmq/derelicte/issues/1)

or the question of how to manage jruby projects with maven [http://ruby.11.x6.nabble.com/Maven-dependency-management-td4996934.html](http://ruby.11.x6.nabble.com/Maven-dependency-management-td4996934.html)

or a few days ago an issue for the rake-compile [https://github.com/luislavena/rake-compiler/issues/87](https://github.com/luislavena/rake-compiler/issues/87)

with jruby-9000 coming it is the right time to get the jar dependencies right - the current situation is like the time before bundler for gems.

