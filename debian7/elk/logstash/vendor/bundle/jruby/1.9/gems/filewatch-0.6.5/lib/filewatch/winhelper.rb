require "ffi"
	
module Winhelper
	extend FFI::Library
	
	ffi_lib 'kernel32'
	ffi_convention :stdcall
	class FileTime < FFI::Struct
		layout :lowDateTime, :uint,
		:highDateTime, :uint
	end
	
	#http://msdn.microsoft.com/en-us/library/windows/desktop/aa363788(v=vs.85).aspx
	class FileInformation < FFI::Struct
		def initialize()
			createTime = FileTime.new
			lastAccessTime = FileTime.new
			lastWriteTime = FileTime.new
		end
		
		layout :fileAttributes, :uint, #DWORD    dwFileAttributes;
		:createTime, FileTime,  #FILETIME ftCreationTime;
		:lastAccessTime, FileTime, #FILETIME ftLastAccessTime;
		:lastWriteTime, FileTime, #FILETIME ftLastWriteTime;
		:volumeSerialNumber, :uint, #DWORD    dwVolumeSerialNumber;
		:fileSizeHigh, :uint, #DWORD    nFileSizeHigh;
		:fileSizeLow, :uint, #DWORD    nFileSizeLow;
		:numberOfLinks, :uint, #DWORD    nNumberOfLinks;
		:fileIndexHigh, :uint, #DWORD    nFileIndexHigh;
		:fileIndexLow, :uint #DWORD    nFileIndexLow;
	end
		
	
	#http://msdn.microsoft.com/en-us/library/windows/desktop/aa363858(v=vs.85).aspx
	#HANDLE WINAPI CreateFile(_In_ LPCTSTR lpFileName,_In_  DWORD dwDesiredAccess,_In_ DWORD dwShareMode,
	#						_In_opt_  LPSECURITY_ATTRIBUTES lpSecurityAttributes,_In_  DWORD dwCreationDisposition,
	#						_In_      DWORD dwFlagsAndAttributes,_In_opt_  HANDLE hTemplateFile);
	attach_function :GetOpenFileHandle, :CreateFileA, [:pointer, :uint, :uint, :pointer, :uint, :uint, :pointer], :pointer	
	
	#http://msdn.microsoft.com/en-us/library/windows/desktop/aa364952(v=vs.85).aspx
	#BOOL WINAPI GetFileInformationByHandle(_In_   HANDLE hFile,_Out_  LPBY_HANDLE_FILE_INFORMATION lpFileInformation);
	attach_function :GetFileInformationByHandle, [:pointer, :pointer], :int
	
	attach_function :CloseHandle, [:pointer], :int
	
	
	def self.GetWindowsUniqueFileIdentifier(path)
		handle = GetOpenFileHandle(path, 0, 7, nil, 3, 128, nil)
		fileInfo = Winhelper::FileInformation.new
		success = GetFileInformationByHandle(handle, fileInfo)
		CloseHandle(handle)
		if  success == 1
			#args = [
			#		fileInfo[:fileAttributes], fileInfo[:volumeSerialNumber], fileInfo[:fileSizeHigh], fileInfo[:fileSizeLow], 
			#		fileInfo[:numberOfLinks], fileInfo[:fileIndexHigh], fileInfo[:fileIndexLow]
			#	]
			#p "Information: %u %u %u %u %u %u %u " % args
			#this is only guaranteed on NTFS, for ReFS on windows 2012, GetFileInformationByHandleEx should be used with FILE_ID_INFO, which returns a 128 bit identifier
			return "#{fileInfo[:volumeSerialNumber]}-#{fileInfo[:fileIndexLow]}-#{fileInfo[:fileIndexHigh]}"
		else
			#p "cannot retrieve file information, returning path"
			return path;
		end
	end
end

#fileId = Winhelper.GetWindowsUniqueFileIdentifier('C:\inetpub\logs\LogFiles\W3SVC1\u_ex1fdsadfsadfasdf30612.log')
#p "FileId: " + fileId
#p "outside function, sleeping"
#sleep(10)