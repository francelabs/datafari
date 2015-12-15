# -*- encoding: utf-8 -*-
# stub: jruby-openssl 0.9.12 java lib

Gem::Specification.new do |s|
  s.name = "jruby-openssl"
  s.version = "0.9.12"
  s.platform = "java"

  s.required_rubygems_version = Gem::Requirement.new(">= 0") if s.respond_to? :required_rubygems_version=
  s.require_paths = ["lib"]
  s.authors = ["Ola Bini", "JRuby contributors"]
  s.date = "2015-10-08"
  s.description = "JRuby-OpenSSL is an add-on gem for JRuby that emulates the Ruby OpenSSL native library."
  s.email = "ola.bini@gmail.com"
  s.homepage = "https://github.com/jruby/jruby-openssl"
  s.licenses = ["EPL-1.0", "GPL-2.0", "LGPL-2.1"]
  s.requirements = ["jar org.bouncycastle:bcpkix-jdk15on, 1.50", "jar org.bouncycastle:bcprov-jdk15on, 1.50"]
  s.rubygems_version = "2.4.8"
  s.summary = "JRuby OpenSSL"

  s.installed_by_version = "2.4.8" if s.respond_to? :installed_by_version

  if s.respond_to? :specification_version then
    s.specification_version = 4

    if Gem::Version.new(Gem::VERSION) >= Gem::Version.new('1.2.0') then
      s.add_development_dependency(%q<jar-dependencies>, ["~> 0.1.0"])
      s.add_development_dependency(%q<mocha>, ["~> 1.1.0"])
      s.add_development_dependency(%q<ruby-maven>, [">= 0"])
    else
      s.add_dependency(%q<jar-dependencies>, ["~> 0.1.0"])
      s.add_dependency(%q<mocha>, ["~> 1.1.0"])
      s.add_dependency(%q<ruby-maven>, [">= 0"])
    end
  else
    s.add_dependency(%q<jar-dependencies>, ["~> 0.1.0"])
    s.add_dependency(%q<mocha>, ["~> 1.1.0"])
    s.add_dependency(%q<ruby-maven>, [">= 0"])
  end
end
