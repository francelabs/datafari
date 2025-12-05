package com.francelabs.datafari.exception;


public class AwaitUserInputException extends Exception {
	private CodesReturned codesReturned;

	public AwaitUserInputException(CodesReturned codesReturned, String message){
		super(message);
		this.codesReturned = codesReturned;
	}

	public CodesReturned getErrorCode() {
		// TODO Auto-generated method stub
		return codesReturned;
	}
	
}
