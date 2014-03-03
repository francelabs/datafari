package com.francelabs.db.model;

public class ADInfo {
	
	private String domaine;
	private String login;
	private String pass;
	private String actif;
	private String ip;
	private String admin;
	private String groupadmin;
	
	public ADInfo(String domaine, String login, String pass, String string, String ip, String admin, String groupadmin){
		this.setDomaine(domaine);
		this.setLogin(login);
		this.setPass(pass);
		this.setActif(string);
		this.setIp(ip);
		this.setAdmin(admin);
		this.setGroupadmin(groupadmin);
		
	}

	public String getDomaine() {
		return domaine;
	}

	public void setDomaine(String domaine) {
		this.domaine = domaine;
	}

	public String getLogin() {
		return login;
	}

	public void setLogin(String login) {
		this.login = login;
	}

	public String getPass() {
		return pass;
	}

	public void setPass(String pass) {
		this.pass = pass;
	}

	public String getActif() {
		return actif;
	}

	public void setActif(String actif) {
		this.actif = actif;
	}

	public String getIp() {
		return ip;
	}

	public void setIp(String ip) {
		this.ip = ip;
	}

	public String getAdmin() {
		return admin;
	}

	public void setAdmin(String admin) {
		this.admin = admin;
	}

	public String getGroupadmin() {
		return groupadmin;
	}

	public void setGroupadmin(String groupadmin) {
		this.groupadmin = groupadmin;
	}

}
