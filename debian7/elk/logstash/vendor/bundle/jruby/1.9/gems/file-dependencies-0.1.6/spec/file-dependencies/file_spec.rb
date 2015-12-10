require 'spec_helper'
require 'file-dependencies/file'
include WebMock::API
describe FileDependencies::File do

  describe ".calculate_sha1" do

    after do 
      ::File.unlink(file)
    end
   
    let(:file) { Assist.generate_file('some_content') }
    it 'gives back correct sha1 value' do
      expect(FileDependencies::File.calculate_sha1(file)).to(eq('778164c23fae5935176254d2550619cba8abc262'))
    end

    it 'raises an error when the file doesnt exist' do
      expect { FileDependencies::File.calculate_sha1('dont_exist') }.to(raise_error(Errno::ENOENT))
    end
  end

  describe ".validate_sha1" do

    after do 
      ::File.unlink(file)
    end

    describe "with a sha1 string" do

      let(:file) { Assist.generate_file('some_content') }
      it 'returns true when sha1 comparing is valid' do
        remote_sha1 = '778164c23fae5935176254d2550619cba8abc262'
        expect(FileDependencies::File.validate_sha1(file, remote_sha1)).to(be_truthy)
      end

      it 'raises error when invalid' do
        remote_sha1 = '778164c23fae5935176254d2550619cba8abc263'
        expect { FileDependencies::File.validate_sha1(file, remote_sha1) }.to(raise_error(RuntimeError))
      end

    end

    describe "with no validation" do

      let(:file) { Assist.generate_file('some_content') }
      it 'always returns true' do
        remote_sha1 = 'none'
        expect(FileDependencies::File.validate_sha1(file, remote_sha1)).to(be_truthy)
      end

    end

    describe "with a remote file" do

      after do 
        ::File.unlink(sha1file)
        ::File.unlink(sha1file2)
      end

      let(:file) { Assist.generate_file('some_content') }
      let(:sha1file) { Assist.generate_file('778164c23fae5935176254d2550619cba8abc262') }
      let(:sha1file2) { Assist.generate_file('778164c23fae5935176254d2550619cba8abc263') }
      let(:remote_sha1) { 'http://example.com/sha1file' }

      it 'returns true when sha1 comparing is valid' do
        expect(FileDependencies::File).to receive(:download).with(remote_sha1, Dir.tmpdir).and_return(sha1file)
        expect(FileDependencies::File.validate_sha1(file, remote_sha1)).to(be_truthy)
      end

      it 'raises error when invalid' do
        expect(FileDependencies::File).to receive(:download).with(remote_sha1, Dir.tmpdir).and_return(sha1file2)
        expect { FileDependencies::File.validate_sha1(file, remote_sha1) }.to(raise_error)
      end

    end
  end

  describe ".fetch_sha1" do
 
    describe "With a sha1 string" do
      let(:remote_sha1) { '778164c23fae5935176254d2550619cba8abc262' }
      it 'returns sha1 string when valid' do
        expect(FileDependencies::File.fetch_sha1(remote_sha1)).to(eq(remote_sha1))
      end

      let(:faulty_remote_sha1) { '778164c23fae5935176254d2550619cba8abc2' }
      it 'raises error when sha1 string is invalid' do
        expect { FileDependencies::File.fetch_sha1(faulty_remote_sha1) }.to(raise_error)
      end
    end

    describe "with a remote sha1" do

      after do 
        ::File.unlink(sha1file)
        ::File.unlink(sha1file1)
      end

      let(:sha1file) { Assist.generate_file('778164c23fae5935176254d2550619cba8abc262') }
      let(:sha1file1) { Assist.generate_file('778164c23fae5935176254d2550619cba8abc26') }
      let(:remote_sha1) { 'http://example.com/sha1file' }

      it 'returns sha1 string when valid' do
        expect(FileDependencies::File).to receive(:download).with(remote_sha1, Dir.tmpdir).and_return(sha1file)
        expect(FileDependencies::File.fetch_sha1(remote_sha1)).to(eq('778164c23fae5935176254d2550619cba8abc262'))
      end

      it 'raises error when sha1 string is invalid' do
        expect(FileDependencies::File).to receive(:download).with(remote_sha1, Dir.tmpdir).and_return(sha1file1)
        expect { FileDependencies::File.fetch_sha1(remote_sha1) }.to(raise_error(RuntimeError))
      end

    end
  end

  describe ".download" do

    after do
      FileUtils.remove_entry_secure(tmpdir)
      FileUtils.remove_entry_secure(file)
    end

    let(:tmpdir) { Stud::Temporary.directory }
    url = 'http://www.example.com/somefile'
    let(:file) { Assist.generate_file('778164c23fae5935176254d2550619cba8abc262') }

    it 'returns the path to the file downloaded' do
      stub_request(:get, url).to_return(:body => File.new(file), :status => 200)
      expect(FileDependencies::File.download(url, tmpdir)).to(eq(File.join(tmpdir, 'somefile')))
    end

    it 'raises an error if the file does not exist' do
      stub_request(:get, url).to_return(:status => 404)
      expect { FileDependencies::File.download(url, tmpdir) }.to(raise_error(RuntimeError))
    end

    it 'raises an error on timeout' do
      stub_request(:get, url).to_timeout
      expect { FileDependencies::File.download(url, tmpdir) }.to(raise_error(Timeout::Error))
    end

  end

end
