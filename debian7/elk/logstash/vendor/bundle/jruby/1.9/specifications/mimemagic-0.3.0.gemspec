# -*- encoding: utf-8 -*-
# stub: mimemagic 0.3.0 ruby lib

Gem::Specification.new do |s|
  s.name = "mimemagic"
  s.version = "0.3.0"

  s.required_rubygems_version = Gem::Requirement.new(">= 0") if s.respond_to? :required_rubygems_version=
  s.require_paths = ["lib"]
  s.authors = ["Daniel Mendler"]
  s.date = "2015-03-25"
  s.description = "Fast mime detection by extension or content in pure ruby (Uses freedesktop.org.xml shared-mime-info database)"
  s.email = ["mail@daniel-mendler.de"]
  s.homepage = "https://github.com/minad/mimemagic"
  s.licenses = ["MIT"]
  s.rubyforge_project = "mimemagic"
  s.rubygems_version = "2.4.8"
  s.summary = "Fast mime detection by extension or content"

  s.installed_by_version = "2.4.8" if s.respond_to? :installed_by_version

  if s.respond_to? :specification_version then
    s.specification_version = 4

    if Gem::Version.new(Gem::VERSION) >= Gem::Version.new('1.2.0') then
      s.add_development_dependency(%q<bacon>, [">= 0"])
      s.add_development_dependency(%q<rake>, [">= 0"])
    else
      s.add_dependency(%q<bacon>, [">= 0"])
      s.add_dependency(%q<rake>, [">= 0"])
    end
  else
    s.add_dependency(%q<bacon>, [">= 0"])
    s.add_dependency(%q<rake>, [">= 0"])
  end
end
