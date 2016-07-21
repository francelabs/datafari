package com.francelabs.datafari.exception;

public enum CodesReturned {
	TRUE(3),
	FALSE(2),
	ALREADYPERFORMED(1),
	ALLOK(0),
	GENERALERROR(-1),
	NOTCONNECTED(-2),
	PROBLEMCONNECTIONDATABASE(-3),
	PROBLEMQUERY(-4),
	PARAMETERNOTWELLSET(-5),
	PROBLEMCONNECTIONAD(-6),
	ADUSERNOTEXISTS(-900),
	FAILTOSIGNIN(-101),
	USERALREADYINBASE(-800);

    private final int value;

    CodesReturned(final int newValue) {
        value = newValue;
    }

    public int getValue() { return value; }
    
    @Override
    public String toString() { return Integer.toString(getValue()); }
}
