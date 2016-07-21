package com.francelabs.datafari.exception;


public class DatafariServerException extends Exception {
	private CodesReturned codesReturned;

	public DatafariServerException(CodesReturned codesReturned, String message){
		super(message);
		this.codesReturned = codesReturned;
	}

	public CodesReturned getErrorCode() {
		// TODO Auto-generated method stub
		return codesReturned;
	}
	
}
