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
import java.util.List;

import org.eclipse.jgit.api.ListBranchCommand.ListMode;
import org.eclipse.jgit.lib.PersonIdent;
import org.slf4j.Logger;

/**
 * @author mocleiri 
 * 
 * Launches the branch rewrite process.  This will rewrite all the commits from one branch and create a new branch representing those changes.
 *
 */
public class MigrateMain {

	private static final Logger log = org.slf4j.LoggerFactory.getLogger(MigrateMain.class);
	
	/**
	 * 
	 * Starts the migration process.
	 * 
	 * This is the rewrite commit process
	 * 
	 */
	public static void main(String[] args) {
	
		if (args.length != 6) {
			
			log.error("USAGE: <path to .git directory> <svn to git username file> <sourceBranch> <targetBranch> <git commit username> <git commit email>");
			System.exit(-1);
		}
		
		try {
			
			String gitRepository = args[0];
			
			String svnToGitUsernameFile = args[1];
			
			String sourceBranch = args[2];
			String targetBranch = args[3];
			
			String gitUser = args[4];
			
			String gitEmail = args[5];
			
			RewriteGitHistory rewriter = new RewriteGitHistory(new File (gitRepository), new DefaultCommitFilter(svnToGitUsernameFile), new PersonIdent(gitUser, gitEmail));
			
			rewriter.convertBranch(sourceBranch, targetBranch);
						
			
			
		} catch (Exception e) {
			log.error("failed to rewrite history", e);
			
		}
		
			
	}

}
