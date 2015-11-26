# encoding: utf-8
# This file is part of ruby-flores.
# Copyright (C) 2015 Jordan Sissel
# 
# This program is free software: you can redistribute it and/or modify
# it under the terms of the GNU Affero General Public License as
# published by the Free Software Foundation, either version 3 of the
# License, or (at your option) any later version.
# 
# This program is distributed in the hope that it will be useful,
# but WITHOUT ANY WARRANTY; without even the implied warranty of
# MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
# GNU Affero General Public License for more details.
# 
# You should have received a copy of the GNU Affero General Public License
# along with this program.  If not, see <http://www.gnu.org/licenses/>.
require "flores/namespace"
require "flores/rspec"

# RSpec helpers for stress testing examples
#
# Setting it up in rspec:
#
#     RSpec.configure do |c|
#       c.extend RSpec::StressIt
#     end
#
# TODO(sissel): Show an example of stress_it and analyze_it
module Flores::RSpec::Analyze
  # Save state after each example so it can be used in analysis after specs are completed.
  #
  # If you use this, you'll want to set your RSpec formatter to
  # Flores::RSpec::Formatter::Analyze
  #
  # Let's show an example that fails sometimes.
  #
  #     describe "Addition of two numbers" do
  #       context "positive numbers" do
  #         analyze_results
  #         let(:a) { Flores::Random.number(1..1000) }
  #     
  #         # Here we make negative numbers possible to cause failure in our test.
  #         let(:b) { Flores::Random.number(-200..1000) }
  #         subject { a + b }
  #     
  #         stress_it "should be positive" do
  #           expect(subject).to(be > 0)
  #         end
  #       end
  #     end
  #
  # And running it:
  #
  #     % rspec -f Flores::RSpec::Formatter::Analyze
  #     Addition of two numbers positive numbers should be positive
  #       98.20% tests successful of 3675 tests
  #       Failure analysis:
  #         1.80% -> [66] RSpec::Expectations::ExpectationNotMetError
  #           Sample exception for {:a=>126.21705882478048, :b=>-139.54814492675024, :subject=>-13.33108610196976}
  #             expected: > 0
  #                  got:   -13.33108610196976
  #           Samples causing RSpec::Expectations::ExpectationNotMetError:
  #             {:a=>90.67298249206425, :b=>-136.6237821353908, :subject=>-45.95079964332655}
  #             {:a=>20.35865155878871, :b=>-39.592417377658876, :subject=>-19.233765818870165}
  #             {:a=>158.07905166101787, :b=>-177.5864470909581, :subject=>-19.50739542994023}
  #             {:a=>31.80445518715138, :b=>-188.51942190504894, :subject=>-156.71496671789757}
  #             {:a=>116.1479954937354, :b=>-146.18477887927958, :subject=>-30.036783385544183}
  def analyze_results
    # TODO(sissel): Would be lovely to figure out how to inject an 'after' for
    # all examples if we are using the Analyze formatter.
    # Then this method could be implied by using the right formatter, or something.
    after do |example|
      example.metadata[:values] = __memoized.clone
    end
  end

  # A formatter to show analysis of an `analyze_it` example. 
  class Analysis < StandardError
    def initialize(results)
      @results = results
    end # def initialize

    def total
      @results.reduce(0) { |m, (_, v)| m + v.length }
    end # def total

    def success_count
      if @results.include?(:passed)
        @results[:passed].length
      else
        0
      end
    end # def success_count

    def success_and_pending_count
      count = 0
      [:passed, :pending].each do |group|
        count += @results[group].length
      end
      count
    end # def success_count

    def percent(count)
      return (count + 0.0) / total
    end # def percent

    def percent_s(count)
      return format("%.2f%%", percent(count) * 100)
    end # def percent_s

    def to_s # rubocop:disable Metrics/AbcSize
      # This method is crazy complex for a formatter. Should refactor this significantly.
      report = []
      if @results[:pending].any?
        # We have pending examples, put a clear message.
        report << "#{percent_s(success_and_pending_count)} (of #{total} total) tests are successful or pending"
      else
        report << "#{percent_s(success_count)} (of #{total} total) tests are successful"
      end
      report += failure_summary if success_and_pending_count < total
      report.join("\n")
    end # def to_s

    # TODO(sissel): All these report/summary/to_s things are an indication that the
    # report formatting belongs in a separate class.
    def failure_summary
      report = ["Failure analysis:"]
      report += @results.sort_by { |_, v| -v.length }.collect do |group, instances|
        next if group == :passed
        next if group == :pending
        error_report(group, instances)
      end.reject(&:nil?).flatten
      report
    end # def failure_summary

    def error_report(error, instances)
      report = error_summary(error, instances)
      report += error_sample_states(error, instances) if instances.size > 1
      report
    end # def error_report

    def error_summary(error, instances)
      sample = instances.sample(1)
      [ 
        "  #{percent_s(instances.length)} -> [#{instances.length}] #{error}",
        "    Sample exception for #{sample.first[0]}",
        sample.first[1].to_s.gsub(/^/, "      ")
      ]
    end # def error_summary

    def error_sample_states(error, instances)
      [ 
        "    Samples causing #{error}:",
        *instances.sample(5).collect { |state, _exception| "      #{state}" }
      ]
    end # def error_sample_states
  end # class Analysis
end # Flores::RSpec::Analyze
