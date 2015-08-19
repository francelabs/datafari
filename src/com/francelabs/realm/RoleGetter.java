package com.francelabs.realm;

import java.util.ArrayList;

import com.mongodb.client.MongoDatabase;

public class RoleGetter {
	
	public final static String DefautRole = "normalUser";
	private MongoDatabase db;
	private User user;
	private ArrayList<String> roles; 
	
	public RoleGetter(String username,MongoDatabase db){
		this.db = db;
		user = new User(username,"",db);
		if (user.isInBase()){
			// if the user exists already in MongoDb
			roles = user.getRoles();
		}else{
			// if not, we enroll him in the MongoDB with a password==digest("") and with the defaultRole
			user.signup(DefautRole);
			roles = new ArrayList<String>();
			roles.add(DefautRole);
		}
	}
	public ArrayList<String> getRoles(){
		return roles;
	}
}
