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

import org.eclipse.jgit.lib.PersonIdent;
import org.slf4j.Logger;

/**
 * @author mocleiri 
 * 
 * This assumes everything in the targeted branch flat (i.e. created using 'git svn clone svn/repo/root' without specifying the standard layout).
 * 
 * It will call the extract branch functionality that will slice off a specific subdirectory from the main history into its own branch.
 *  
 *
 */
public class ExtractBranchMain {

	private static final Logger log = org.slf4j.LoggerFactory.getLogger(ExtractBranchMain.class);
	
	/**
	 * 
	 * Starts the migration process.
	 * 
	 * We need to create a new branch sequence for each existing branch.  Each commit is modified with a new username and to remove any svn-id tag
	 * 
	 */
	public static void main(String[] args) {
	
		if (args.length != 7) {
			
			log.error("USAGE: <path to .git directory> <git authors file> <initial branch> <new branch name> <path prefix> <git commit username> <git commit email>");
			System.exit(-1);
		}
		
		String gitRepository = args[0];
		
		String gitAuthorsFile = args[1];
		
		String initialBranch = args[2];
		
		String newBranchName = args[3];
		
		String pathPrefix = args[4];
		
		String gitCommitUser = args[5];
		
		String gitCommitEmail = args[6];
		
		try {
			
			
			
			
			RewriteGitHistory filter = new RewriteGitHistory(new File (gitRepository), new DefaultCommitFilter(gitAuthorsFile), new PersonIdent(gitCommitUser, gitCommitEmail));
			
//			filter.listMatchingCommits("master", pathPrefix);
			
			
			filter.extractBranch(initialBranch, newBranchName, pathPrefix);
			
		} catch (Exception e) {
			log.error("failed to extract branch " + newBranchName, e);
			
		}
		
			
	}

}
