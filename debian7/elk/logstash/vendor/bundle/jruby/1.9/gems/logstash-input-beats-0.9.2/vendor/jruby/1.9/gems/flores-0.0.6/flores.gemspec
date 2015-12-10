Gem::Specification.new do |spec|
  files = %x(git ls-files).split("\n")

  spec.name = "flores"
  spec.version = "0.0.6"
  spec.summary = "Fuzz, randomize, and stress your tests"
  spec.description = <<-DESCRIPTION
    Add fuzzing, randomization, and stress to your tests.

    This library is an exploration to build the tools to let you write tests
    that find bugs.

    In memory of Carlo Flores.
  DESCRIPTION
  spec.license = "AGPL 3.0 - http://www.gnu.org/licenses/agpl-3.0.html"

  spec.files = files
  spec.require_paths << "lib"

  spec.authors = ["Jordan Sissel"]
  spec.email = ["jls@semicomplete.com"]
end
