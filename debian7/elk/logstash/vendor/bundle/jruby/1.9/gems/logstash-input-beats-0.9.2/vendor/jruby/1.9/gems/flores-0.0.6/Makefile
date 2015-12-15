GEMSPEC=$(shell ls *.gemspec)
VERSION=$(shell awk -F\" '/spec.version/ { print $$2 }' $(GEMSPEC))
NAME=$(shell awk -F\" '/spec.name/ { print $$2 }' $(GEMSPEC))
GEM=$(NAME)-$(VERSION).gem

.PHONY: package
package: | $(GEM)

.PHONY: gem
gem: $(GEM)

$(GEM):
	gem build $(GEMSPEC)

.PHONY: test-package
test-package: $(GEM)
	# Sometimes 'gem build' makes a faulty gem.
	gem unpack $(GEM)
	rm -rf $(NAME)-$(VERSION)/

.PHONY: publish
publish: test-package
	gem push $(GEM)

.PHONY: install
install: $(GEM)
	gem install $(GEM)

.PHONY: clean
clean:
	-rm -rf .yardoc $(GEM) $(NAME)-$(VERSION)/

.PHONY: rubocop
rubocop:
	rubocop -D

.PHONY: test
test: rubocop
	rspec -f Flores::RSpec::Formatters::Analyze

.PHONY: test
test-fast: rubocop
	ITERATIONS=10 rspec -f Flores::RSpec::Formatters::Analyze
