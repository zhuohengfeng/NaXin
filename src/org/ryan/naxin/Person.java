package org.ryan.naxin;

import java.io.Serializable;


public class Person implements Serializable{
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private int UID = 0;
	private String Name = "";
	private int ImgID = 0;
	private int GroupID = 0;
	private long timeStamp = 0;
	private String personHost = "";
	
	public Person(int uID, String name, int ImgID, String host) {
		// TODO Auto-generated constructor stub
		this.UID = uID;
		this.Name = name;
		this.ImgID = ImgID;
		this.personHost = host;
		this.GroupID = 0;
		this.setTimeStamp(System.currentTimeMillis());
	}

	public int getGroupID() {
		return GroupID;
	}

	public void setGroupID(int groupID) {
		GroupID = groupID;
	}

	public int getUID() {
		return UID;
	}

	public void setUID(int uID) {
		UID = uID;
	}

	public String getName() {
		return Name;
	}

	public void setName(String name) {
		Name = name;
	}

	public int getImgID() {
		return ImgID;
	}

	public void setImgID(int imgID) {
		ImgID = imgID;
	}

	public long getTimeStamp() {
		return timeStamp;
	}

	public void setTimeStamp(long timeStamp) {
		this.timeStamp = timeStamp;
	}

	public String getPersonHost() {
		return personHost;
	}

	public void setPersonHost(String personHost) {
		this.personHost = personHost;
	}

	
}
