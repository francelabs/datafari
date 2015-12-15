#-*- mode: ruby -*-

gemfile

# TODO should be setup whenever a plugin uses gems
plugin_repository :id => 'rubygems-releases', :url => 'http://rubygems-proxy.torquebox.org/releases'

jruby_plugin( :minitest, :minispecDirectory => "specs/*_spec.rb" ) do
  execute_goals(:spec)
  gem 'ruby-maven', '${ruby-maven.version}'
end

# retrieve the ruby-maven version
pro = @model.profiles.detect { |p| p.id.to_sym == :gemfile } || @model
ruby_maven = pro.dependencies.detect { |d| d.artifact_id == 'ruby-maven' }

properties( 'jruby.versions' => ['1.7.12', '1.7.22', '${jruby.version}'
                                ].join(','),
            'jruby.modes' => ['1.9', '2.0', '2.2'].join(','),
            # just lock the version
            'bundler.version' => '1.9.2',
            'ruby-maven.version' => ruby_maven.version,
            'jruby.version' => '9.0.3.0',
            'jruby.plugins.version' => '1.1.2',
            'push.skip' => true  )

plugin :invoker, '1.8' do
  execute_goals( :install, :run,
                 :id => 'integration-tests',
                 :projectsDirectory => 'integration',
                 :streamLogs => true,
                 :goals => ['install'],
                 :preBuildHookScript => 'setup.bsh',
                 :postBuildHookScript => 'verify.bsh',
                 :cloneProjectsTo => '${project.build.directory}',
                 :properties => { 'jar-dependencies.version' => '${project.version}',
                   # use an old jruby with old ruby-maven here
                   'jruby.version' => '1.7.20',
                   'jruby.plugins.version' => '${jruby.plugins.version}',
                   'bundler.version' => '${bundler.version}',
                   'ruby-maven.version' => '${ruby-maven.version}',
                   # dump pom for the time being - for travis
                   'polyglot.dump.pom' => 'pom.xml'})
end

distribution_management do
  repository :id => :ossrh, :url => 'https://oss.sonatype.org/service/local/staging/deploy/maven2/'
end

profile :id => :release do
  properties 'maven.test.skip' => true, 'invoker.skip' => true, 'push.skip' => false

  distribution_management do
    repository :id => :ossrh, :url => 'https://oss.sonatype.org/service/local/staging/deploy/maven2/'
  end

  build do
    default_goal :deploy
  end

  plugin :gpg, '1.5' do
    execute_goal :sign, :id => 'sign artifacts', :phase => :verify
  end
  plugin :deploy, '2.8.2' do
    execute_goal :deploy, :phase => :deploy, :id => 'deploy gem to maven central'
  end
end

# vim: syntax=Ruby
