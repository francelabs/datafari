require 'stringio'
if RUBY_VERSION < '1.9'
  require 'enumerator'
  require 'generator' # Must require first, because of warning in Ruby 1.8.7 with `ruby -w -r generator -e ""`
end
require './test/test_helper'
$bogus = []

module Kernel
  def require_with_bogus_extension(lib)
    $bogus << lib
    require_without_bogus_extension(lib)
  end
  alias_method :require_without_bogus_extension, :require
  alias_method :require, :require_with_bogus_extension

  if defined? BasicObject and BasicObject.superclass
    BasicObject.send :undef_method, :require
    BasicObject.send :undef_method, :require_with_bogus_extension
  end
end

class AAA_TestBackportGuards < Test::Unit::TestCase
  def setup
    $VERBOSE = true
    @prev, $stderr = $stderr, StringIO.new
  end

  def teardown
    assert_equal '', $stderr.string
    $stderr = @prev
  end

  EXCLUDE = %w[require require_with_backports require_without_backports] # Overriden in all circumstances to load the std-lib
  EXCLUDE.map!(&:to_sym) if instance_methods.first.is_a?(Symbol)

  # For some very strange reason, Hash[kvp.flatten] doesn't always work in 1.8.6??
  def hash(key_value_pairs)
    h = {}
    key_value_pairs.each{|k,v| h[k] = v}
    h
  end

  def class_signature(klass)
    hash(
      (klass.instance_methods - EXCLUDE).map{|m| [m, klass.instance_method(m)] } +
      (klass.methods - EXCLUDE).map{|m| [".#{m}", klass.method(m) ]}
    )
  end

  CLASSES = [Array, Binding, Dir, Enumerable, Fixnum, Float, GC,
      Hash, Integer, IO, Kernel, Math, MatchData, Method, Module, Numeric,
      ObjectSpace, Proc, Process, Range, Regexp, String, Struct, Symbol] +
    [ENV, ARGF].map{|obj| class << obj; self; end }

  case RUBY_VERSION
    when '1.8.6'
    when '1.8.7'
      CLASSES << Enumerable::Enumerator
    else
      CLASSES << Enumerator
  end

  def digest
    hash(
      CLASSES.map { |klass| [klass, class_signature(klass)] }
    )
  end

  def digest_delta(before, after)
    delta = {}
    before.each do |klass, methods|
      compare = after[klass]
      d = methods.map do |name, unbound|
        name unless unbound == compare[name]
      end + (compare.map(&:first) - methods.map(&:first))
      d.compact!
      delta[klass] = d unless d.empty?
    end
    delta unless delta.empty?
  end

  # Order super important!
  def test__1_abbrev_can_be_required_before_backports
    assert require('abbrev')
    assert !$LOADED_FEATURES.include?('backports')
  end

  # Order super important!
  def test__2_backports_wont_override_unnecessarily
    before = digest
    latest = "2.0.0"
    unless RUBY_VERSION <= '1.8.6'
      require "backports/#{[RUBY_VERSION, latest].min}"
      after = digest
      assert_nil digest_delta(before, after)
    end
    unless RUBY_VERSION >= latest
      require "backports"
      after = digest
      assert !digest_delta(before, after).nil?
    end
  end

  def test_setlib_load_correctly_after_requiring_backports
    path = File.expand_path("../../lib/backports/1.9.2/stdlib/matrix.rb", __FILE__)
    assert_equal false,  $LOADED_FEATURES.include?(path)
    assert_equal true,  require('matrix')
    assert_equal true,  $bogus.include?("matrix")
    assert_equal true,  $LOADED_FEATURES.include?(path)
    assert_equal false, require('matrix')
  end

  def test_setlib_load_correctly_before_requiring_backports_test
    assert_equal true,  $bogus.include?("abbrev")
    path = File.expand_path("../../lib/backports/2.0.0/stdlib/abbrev.rb", __FILE__)
    assert_equal true,  $LOADED_FEATURES.include?(path)
    assert_equal false, require('abbrev')
  end

  def test_backports_does_not_interfere_for_libraries_without_backports_test
    assert_equal true,  require('scanf')
    assert_equal false, require('scanf')
  end

  def test_load_correctly_new_libraries_test
    path = File.expand_path("../../lib/backports/2.0.0/stdlib/fake_stdlib_lib.rb", __FILE__)
    assert_equal false, $LOADED_FEATURES.include?(path)
    assert_equal true,  require('fake_stdlib_lib')
    assert_equal true,  $LOADED_FEATURES.include?(path)
    assert_equal false, require('fake_stdlib_lib')
  end

  def test_no_warnings
    require 'ostruct'
    require 'set'
    require 'backports/1.8.7/array/each'
    require 'backports/1.8.7/enumerator/next'
    assert_equal 1, [1,2,3].each.next # [Bug #70]
  end

  def test_rails
    require 'active_support/all'
    $stderr.string = '' # disregard warnings by Rails
    before = digest
    require "backports/rails"
    after = digest
    assert_nil digest_delta(before, after)
  end
end
