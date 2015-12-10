# -*- mode:ruby -*-

gemspec

distribution_management do
  repository :id => :ossrh, :url => 'https://oss.sonatype.org/service/local/staging/deploy/maven2/'
end

plugin :deploy, '2.8.2' do
  execute_goal :deploy, :phase => :deploy, :id => 'deploy gem to maven central', :skip => '${deploy.skip}'
end

properties 'push.skip' => true, 'deploy.skip' => true

profile :id => :release do
  properties 'maven.test.skip' => true, 'invoker.skip' => true
  properties 'push.skip' => false, 'deploy.skip' => false
  plugin :gpg, '1.5' do
    execute_goal :sign, :id => 'sign artifacts', :phase => :verify
  end
  build do
    default_goal :deploy
  end
end

# vim: syntax=Ruby
