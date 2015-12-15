v0.2.8
  fixes for issues-28,29,31
    correction for Time#to_s
    new options to control date serialization
    optimizations suggested by @headius
  jar compiled for jruby 1.7.17
  jruby 1.7.17
  jackson 2.4.4

v0.2.7
  fixes for issues-23,24
    add to_time as option for serializing Time like objects
  jar compiled for jruby 1.7.11

v0.2.6
  fix issue-20
    allow jruby to convert Ruby StringIO into Java
    by not type checking passed arg
    this is because jruby 1.7.9 has changed the type of java object backing Ruby StringIO
  jar compiled for jruby 1.7.8 (jruby 1.7.9 in the maven repo has an error in the pom.xml)
  jruby 1.7.8, jruby 1.7.9 (tested)
  jackson 2.3.0

v0.2.5
  fix issue-16
    reduce the gem size by:
      change pom.xml to only include relevant java jars
      exclude benchmaking from the gemspec files
  jruby 1.7.5
  jackson 2.2.3

v0.2.4
  fix issue-15
    return Ruby nil instead of Java null
  fix issue-14
    remove all usage of Ruby.getGlobalRuntime
    pass the runtime from the calling Ruby ThreadContext into the deserializers and converters
  jruby 1.7.5
  jackson 2.2.3

v0.2.3
  fix issue-12
    improve the serialization support for non Json Datatype Ruby objects
    now has support for serializing via toJava, to_h, to_hash, to_a, to_json
  fix for failing MultiJson unicode test
  jruby 1.7.4
  jackson 2.2.3

v0.2.2
  fix issue-13
    compile Java for 1.6 compatibility
  documentation tweaks
  jruby 1.7.4
  jackson 2.2.3

v0.2.1
  documentation tweaks
  fix issue-7
    add pluggable String and Symbol Converters for JSON values
  jruby 1.7.4
  jackson 2.2.3

v0.2.0
  extract all Java -> Ruby generation to reusable RubyUtils static class
  support BigDecimal
  remove JSON Api
  fixes issues 5, 6, 8, 

  jruby 1.7.3
  jackson 2.2.2

v0.1.1
  fix Time regex
v0.1.0
  MutiJson compatibility
  switch to using almost all Java, i.e. define most of the ruby modules in Java
  jruby 1.7.3
  jackson 2.1.4
v0.0.7
  first release - minimal jruby wrapper around jackson 1.9.5 jars
