/**
 * 
 */
package org.test;

/**
 * @author mocleiri
 * 
 */
public class GitUser {

	public GitUser(String svnAuthor, String gitUser, String gitEmail) {
		super();
		this.svnAuthor = svnAuthor;
		this.gitUser = gitUser;
		this.gitEmail = gitEmail;
	}

	public GitUser(String svnUserName, String gitUser) {
		svnAuthor = svnUserName;
		this.gitUser = gitUser;
		this.gitEmail = null;
	}

	private final String svnAuthor;
	private final String gitUser;
	private final String gitEmail;

	/**
	 * @return the svnAuthor
	 */
	public String getSvnAuthor() {
		return svnAuthor;
	}

	/**
	 * @return the gitUser
	 */
	public String getGitUser() {
		return gitUser;
	}

	/**
	 * @return the gitEmail
	 */
	public String getGitEmail() {
		return gitEmail;
	}
	/**
	 * @param gitEmail
	 *            the gitEmail to set
	 */

}
