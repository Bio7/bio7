package com.eco.bio7.reditor.database.view;

import java.util.ArrayList;
import java.util.List;
/*A class to store R database profiles!*/
public class ConnectionProfiles {
	
	List<ConnectionProfile> profiles = new ArrayList<ConnectionProfile>();

    public List<ConnectionProfile> getProfile() {
        return profiles;
    }

    public void addProfile(ConnectionProfile profile) {
        this.profiles.add(profile);
    }

}
