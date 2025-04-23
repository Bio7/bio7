package com.eco.bio7.reditor.database.view;
/*A class to store a profile for a R database!*/

public class ConnectionProfile {

	public String profileName;
	public String odbcDriver;
	public String server;
	public String database;
	public int port;
	public String user;
	public String password;
	public boolean createEncrypted;
	public String selDirectory;
	public String customDisconnect;
	public boolean sshEnabled;
	public String sshSshHost;
	public String sshCustomArgs;
	public int sshPort;
	public String sshUsername;
	public int sshTimeout;
	public String sshPrivateKey;
	public int localSshPort;

	public boolean isSshEnabled() {
		return sshEnabled;
	}

	public void setSshEnabled(boolean sshEnabled) {
		this.sshEnabled = sshEnabled;
	}

	public String getSshSshHost() {
		return sshSshHost;
	}

	public void setSshSshHost(String sshSshHost) {
		this.sshSshHost = sshSshHost;
	}

	public String getSshCustomArgs() {
		return sshCustomArgs;
	}

	public void setSshCustomArgs(String sshCustomArgs) {
		this.sshCustomArgs = sshCustomArgs;
	}

	public int getSshPort() {
		return sshPort;
	}

	public void setSshPort(int sshPort) {
		this.sshPort = sshPort;
	}

	public String getSshUsername() {
		return sshUsername;
	}

	public void setSshUsername(String sshUsername) {
		this.sshUsername = sshUsername;
	}

	public int getSshTimeout() {
		return sshTimeout;
	}

	public void setSshTimeout(int sshTimeout) {
		this.sshTimeout = sshTimeout;
	}

	public String getSshPrivateKey() {
		return sshPrivateKey;
	}

	public void setSshPrivateKey(String sshPrivateKey) {
		this.sshPrivateKey = sshPrivateKey;
	}

	public int getLocalSshPort() {
		return localSshPort;
	}

	public void setLocalSshPort(int localSshPort) {
		this.localSshPort = localSshPort;
	}

	public String getCustomDisconnect() {
		return customDisconnect;
	}

	public void setCustomDisconnect(String customDisconnect) {
		this.customDisconnect = customDisconnect;
	}

	public String getProfileName() {
		return profileName;
	}

	public void setProfileName(String profileName) {
		this.profileName = profileName;
	}

	public String getOdbcDriver() {
		return odbcDriver;
	}

	public void setOdbcDriver(String odbcDriver) {
		this.odbcDriver = odbcDriver;
	}

	public String getServer() {
		return server;
	}

	public void setServer(String server) {
		this.server = server;
	}

	public String getDatabase() {
		return database;
	}

	public void setDatabase(String database) {
		this.database = database;
	}

	public int getPort() {
		return port;
	}

	public void setPort(int port) {
		this.port = port;
	}

	public String getUser() {
		return user;
	}

	public void setUser(String user) {
		this.user = user;
	}

	public String getPassword() {
		return password;
	}

	public void setPassword(String password) {
		this.password = password;
	}

	public boolean isCreateEncrypted() {
		return createEncrypted;
	}

	public void setCreateEncrypted(boolean createEncrypted) {
		this.createEncrypted = createEncrypted;
	}

	public String getSelDirectory() {
		return selDirectory;
	}

	public void setSelDirectory(String selDirectory) {
		this.selDirectory = selDirectory;
	}

}
