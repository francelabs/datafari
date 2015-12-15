# this file is maven DSL and used by maven via jars/executor.rb

bdir = ENV_JAVA[ "jars.basedir" ]

basedir( bdir )
if basedir != bdir
  # older maven-tools needs this
  self.instance_variable_set( :@basedir, bdir )
end

( 0..10000 ).each do |i|
  coord = ENV_JAVA[ "jars.#{i}" ]
  break unless coord
  artifact = Maven::Tools::Artifact.from_coordinate( coord )
  exclusions = []
  ( 0..10000 ).each do |j|
    exclusion = ENV_JAVA[ "jars.#{i}.exclusions.#{j}" ]
    break unless exclusion
    exclusions << exclusion
  end
  scope = ENV_JAVA[ "jars.#{i}.scope" ]
  artifact.scope = scope if scope
  dependency_artifact( artifact ) do
    exclusions.each do |ex|
      exclusion ex
    end
  end
end

jruby_plugin :gem, ENV_JAVA[ "jruby.plugins.version" ]

jfile = ENV_JAVA[ "jars.jarfile" ]
jarfile( jfile ) if jfile

gemspec rescue nil

properties( 'project.build.sourceEncoding' => 'utf-8' )

plugin :dependency, ENV_JAVA[ "dependency.plugin.version" ]

# some output
model.dependencies.each do |d|
  puts "      " + d.group_id + ':' + d.artifact_id + (d.classifier ? ":" + d.classifier : "" ) + ":" + d.version + ':' + (d.scope || 'compile')
  puts "          exclusions: " + d.exclusions.collect{ |e| e.group_id + ':' + e.artifact_id }.join unless d.exclusions.empty?
end
