# -*- encoding: utf-8 -*-
# stub: manticore 0.4.4 java lib

Gem::Specification.new do |s|
  s.name = "manticore"
  s.version = "0.4.4"
  s.platform = "java"

  s.required_rubygems_version = Gem::Requirement.new(">= 0") if s.respond_to? :required_rubygems_version=
  s.require_paths = ["lib"]
  s.authors = ["Chris Heald"]
  s.cert_chain = ["-----BEGIN CERTIFICATE-----\nMIIDaDCCAlCgAwIBAgIBATANBgkqhkiG9w0BAQUFADA9MQ8wDQYDVQQDDAZjaGVh\nbGQxFTATBgoJkiaJk/IsZAEZFgVnbWFpbDETMBEGCgmSJomT8ixkARkWA2NvbTAe\nFw0xNTAzMDQwNTA5MjhaFw0xNjAzMDMwNTA5MjhaMD0xDzANBgNVBAMMBmNoZWFs\nZDEVMBMGCgmSJomT8ixkARkWBWdtYWlsMRMwEQYKCZImiZPyLGQBGRYDY29tMIIB\nIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAwcnX2yl+rbjUztC4V+iUJgWv\nNPxqU4bQBaL+w00wVABWr04Hjg+cEkqiJ6A0kXxZIz5uXKhhvsaO50NvHfplVcUf\nBxQabIfCS79xdvexXN0or3F5saatGaGa4cj/0uUHjX7w+K5MpEVfbjJp0uAKp62B\nwUU2ilmn7EvZhEUPOMQi01t8z8OsOGc8kF2UtU1kGCxLV7eThWqu8CdXrux5E140\n7SnFnPlnXNeHqwZdOMZzQ9PiTQAPCKO3AY0aBFQeG3wlFPqkcEjOrtV1h7werUwz\naNb4t5sAuu0f/9B646BOjiMgj1WeUlhgiAsaF5kVvLFNCxwS/xpcN3X01M2KdQID\nAQABo3MwcTAJBgNVHRMEAjAAMAsGA1UdDwQEAwIEsDAdBgNVHQ4EFgQUtFVpwfEG\n78mBd2ulzsS+SlffdOcwGwYDVR0RBBQwEoEQY2hlYWxkQGdtYWlsLmNvbTAbBgNV\nHRIEFDASgRBjaGVhbGRAZ21haWwuY29tMA0GCSqGSIb3DQEBBQUAA4IBAQApPK7H\n4nM6IAzpeA1fYuBsH+Z7ENr2p02ElinUVZMiUmWsFjoZqDLWDWIHJnlJCwUZjnBb\ngXpEFiKiS9XgkRmIOIb3fpD9M574vo/AyvAzvQHLCN7p86IFWQSTsAQptcPfQBL7\n8yWPlXfbX6qxnLHrkjcSnRo61v4NqU+y6x4WEX6Dp90DK3mNLgk7kQk4GhBXevnV\n4Mmxp+JAtISoamMfjldbJhCMkWaZUwDsdFzSfTXl7yg772IYZfICzD7eENPiIcmu\ncnyabLOcGIKZNxvnSfwOuCBnjgoSOyJi/n48n1s+OPB/OmPJoWmhKu2DO4sUb4+K\n/3Mhp5UWSl9SmDR1\n-----END CERTIFICATE-----\n"]
  s.date = "2015-08-20"
  s.description = "Manticore is an HTTP client built on the Apache HttpCore components"
  s.email = ["cheald@mashable.com"]
  s.homepage = "https://github.com/cheald/manticore"
  s.licenses = ["MIT"]
  s.rubygems_version = "2.4.8"
  s.summary = "Manticore is an HTTP client built on the Apache HttpCore components"

  s.installed_by_version = "2.4.8" if s.respond_to? :installed_by_version

  if s.respond_to? :specification_version then
    s.specification_version = 4

    if Gem::Version.new(Gem::VERSION) >= Gem::Version.new('1.2.0') then
      s.add_development_dependency(%q<bundler>, ["~> 1.3"])
      s.add_development_dependency(%q<rake>, [">= 0"])
    else
      s.add_dependency(%q<bundler>, ["~> 1.3"])
      s.add_dependency(%q<rake>, [">= 0"])
    end
  else
    s.add_dependency(%q<bundler>, ["~> 1.3"])
    s.add_dependency(%q<rake>, [">= 0"])
  end
end
