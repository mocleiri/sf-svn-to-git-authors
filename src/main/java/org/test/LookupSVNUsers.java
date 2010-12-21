/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.test;

import java.io.File;
import java.io.IOException;
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
import org.slf4j.Logger;

/**
 * @author mocleiri
 *
 * This program reads in a list of svn usernames and then scrapes sourceforge.net for their full names.  The results are written out to the file.
 * 
 * We hardcode an email address of username@users.sourceforge.net since these are actual email addresses that work.
 * 
 */
public class LookupSVNUsers {
	
	private static Logger log = org.slf4j.LoggerFactory.getLogger(LookupSVNUsers.class);

	
	/**
	 * @param args
	 * @throws IOException 
	 */
	public static void main(String[] args) throws IOException {

		if (args.length != 2) {
			log.error("USAGE: <svn users file(input)> <git authors file (output)>");
			
			System.exit(-1);
		}
		
		Set<String>unmatchedNameSet = new LinkedHashSet<String>();
		
		Map<String, GitUser>gitUserMap = new LinkedHashMap<String, GitUser>();
		
		String svnAuthorsFile = args[0];
		
		List<String> lines = FileUtils.readLines(new File(svnAuthorsFile));
		
	
		
		for (String line : lines) {
			
			// intentionally handle both upper and lower case varients of the same name.
			String svnUserName = line.trim();
			
			if (svnUserName.contains("("))
				continue; // skip over this line as we can't use it on the url
			
						
			if (gitUserMap.keySet().contains(svnUserName))
				continue; // skip this duplicate.
			
			log.info("starting on user = " + svnUserName);
			
			String gitName = extractFullName(svnUserName);

			if (gitName == null) {
				
				gitName = extractFullName (svnUserName.toLowerCase());
			}
			
		
			
			if (gitName == null) {
				unmatchedNameSet.add(svnUserName);
			}
			else {

				gitUserMap.put(svnUserName, new GitUser(svnUserName, gitName));
				log.info("mapped user (" + svnUserName + ") to: " + gitName);


			}
			
			
			
			
		}
		
		List<String>mergedList = new ArrayList<String>();
		
		mergedList.add("# GENERATED ");
		
		List<String>userNameList = new ArrayList<String>();
		
		userNameList.addAll(gitUserMap.keySet());
		
		Collections.sort(userNameList);
		
		for (String userName : userNameList) {
			
			GitUser gUser = gitUserMap.get(userName);
		
			mergedList.add(gUser.getSvnAuthor() + " = " + gUser.getGitUser() + " <" + gUser.getSvnAuthor() + "@users.sourceforge.net>");
			
			
		}
	

		for (String username : unmatchedNameSet) {
			
			log.warn("failed to match SVN User = " + username);

			// add in the unmatched entries as is.
			mergedList.add (username + " = " + username + " <" + username + "@users.sourceforge.net>");
		}
	
		FileUtils.writeLines(new File (args[1]),"UTF-8", mergedList);
		
		
		
		
	}

	private static String extractFullName(String userName) throws IOException {

		HttpClient client = new DefaultHttpClient();

		HttpResponse response = client.execute(new HttpGet("https://sourceforge.net/users/" + userName));
		
		HttpEntity entity = response.getEntity();
		
		List<String> content = IOUtils.readLines(entity.getContent(), "UTF-8");
		
		int i = 0;
		
		for (String cLine : content) {
			
			String trimmed = cLine.trim();
			
			if (cLine.contains("/users/" + userName)) {
				
				String[] parts = trimmed.split ("</a> ");
				
				if (parts.length > 1) {
					String name = parts[1].replaceAll("</h1>", "");
			
					return name;		
				}
					
			}
			
			
		}

		return null;
	}

}
