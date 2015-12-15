Gem::Specification.new do |s|
  s.name = "spoon"
  s.version = "0.0.4"
  s.authors = ["Charles Oliver Nutter"]
  s.date = "2013-03-29"
  s.description = s.summary = "Spoon is an FFI binding of the posix_spawn function (and Windows equivalent), providing fork+exec functionality in a single shot."
  s.files = `git ls-files`.lines.map(&:chomp)
  s.require_paths = ["lib"]
  s.add_dependency('ffi')
end
