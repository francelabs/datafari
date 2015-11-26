# -*- encoding: utf-8 -*-
Gem::Specification.new do |s|
  s.name = "filesize"
  s.version = "0.0.4"

  s.authors = ["Dominik Honnef"]
  s.date = %q{2009-07-26}
  s.description = "filesize is a small class for handling filesizes with both the SI and binary prefixes, allowing conversion from any size to any other size."
  s.summary = s.description

  s.email = "dominikh@fork-bomb.org"
  s.extra_rdoc_files = ["README.markdown", "CHANGELOG", "lib/filesize.rb"]
  s.files = ["README.markdown", "CHANGELOG", "lib/filesize.rb", "filesize.gemspec"]
  s.homepage = "http://filesize.rubyforge.org/"
  s.required_ruby_version = ">= 1.8.6"
  s.rubyforge_project = "filesize"

  s.add_development_dependency "rspec", "~> 3.0"
end
