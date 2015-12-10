#-*- mode: ruby -*-

task :default => [ :specs ]

desc 'run specs'
task :specs do
  $LOAD_PATH << "specs"

  Dir['specs/*_spec.rb'].each do |f| 
    require File.basename( f.sub(/.rb$/, '' ) )
  end
end

task :headers do
  require 'copyright_header'

  s = Gem::Specification.load( Dir["*gemspec"].first )

  args = {
    :license => s.license, 
    :copyright_software => s.name,
    :copyright_software_description => s.description,
    :copyright_holders => s.authors,
    :copyright_years => [Time.now.year],
    :add_path => "lib",
    :output_dir => './'
  }

  command_line = CopyrightHeader::CommandLine.new( args )
  command_line.execute
end

# vim: syntax=Ruby
