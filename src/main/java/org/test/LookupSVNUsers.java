/**
 * 
 */
package org.test;

import java.io.File;
import java.io.IOException;
import java.io.UTFDataFormatException;
import java.lang.Character.UnicodeBlock;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.eclipse.jetty.util.Utf8StringBuilder;
import org.slf4j.Logger;

/**
 * @author Mike
 *
 */
public class LookupSVNUsers {
	
	private static Logger log = org.slf4j.LoggerFactory.getLogger(LookupSVNUsers.class);

	static class GitUser {
		
		public GitUser(String svnUserName, String gitUser) {
			svnAuthor = svnUserName;
			this.gitUser = gitUser;
		}
		private String svnAuthor;
		private String gitUser;
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
	
		
		
		
		
	}
	/**
	 * @param args
	 * @throws IOException 
	 */
	public static void main(String[] args) throws IOException {

		if (args.length != 2) {
			log.error("USAGE: <svn users file(input)> <git authors file (output)> ");
			System.exit(-1);
		}
		
		Set<String>unmatchedNameSet = new LinkedHashSet<String>();
		
		Map<String, GitUser>gitUserMap = new LinkedHashMap<String, LookupSVNUsers.GitUser>();
		
		String svnAuthorsFile = args[0];
		
		List<String> lines = FileUtils.readLines(new File(svnAuthorsFile));
		
	
		
		for (String line : lines) {
			
			String svnUserName = line.trim().toLowerCase();
			
			if (svnUserName.contains("("))
				continue; // skip over this line
			
			HttpClient client = new DefaultHttpClient();
			
			if (gitUserMap.keySet().contains(svnUserName))
				continue; // skip this duplicate.
			
			log.info("starting on user = " + svnUserName);
			
			boolean matched = false;
			
			
			HttpResponse response = client.execute(new HttpGet("https://sourceforge.net/users/" + svnUserName));
			
			HttpEntity entity = response.getEntity();
			
			List<String> content = IOUtils.readLines(entity.getContent(), "UTF-8");
			
			int i = 0;
			
			for (String cLine : content) {
				
				String trimmed = cLine.trim();
				
				if (cLine.contains("/users/" + svnUserName)) {
					
					String[] parts = trimmed.split ("</a> ");
					
					if (parts.length > 1) {
						String name = parts[1].replaceAll("</h1>", "");
						
						gitUserMap.put(svnUserName, new GitUser(svnUserName, name));
						log.info("mapped user (" + svnUserName + ") to: " + name);
						matched = true;
						break;
					}
						
				}
				
				
			}
			
			if (!matched) {
				unmatchedNameSet.add(svnUserName);
			}
			
			
			
			
		}
		
		List<String>mergedList = new ArrayList<String>();
		
		mergedList.add("# GENERATED ");
		
		List<String>userNameList = new ArrayList<String>();
		
		userNameList.addAll(gitUserMap.keySet());
		
		Collections.sort(userNameList);
		
		for (String userName : userNameList) {
			
			GitUser gUser = gitUserMap.get(userName);
		
			mergedList.add(gUser.getSvnAuthor() + " = " + gUser.getGitUser() + " <" + gUser.getSvnAuthor() + "@wicketstuff.org>");
			
			
		}
		
		FileUtils.writeLines(new File (args[1]),"UTF-8", mergedList);
		
		for (String username : unmatchedNameSet) {
			
			log.warn("failed to match SVN User = " + username);
		}
		
		
		
	}

}
