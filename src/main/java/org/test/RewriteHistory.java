/**
 * 
 */
package org.test;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.eclipse.jgit.api.CreateBranchCommand;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.GitCommand;
import org.eclipse.jgit.lib.CommitBuilder;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.PersonIdent;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.storage.file.FileRepositoryBuilder;
import org.slf4j.Logger;

/**
 * @author mocleiri
 * 
 * Uses jgit to rewrite the commit history using out svn-to-git authors file.
 *
 */
public class RewriteHistory {

	private static final Logger log = org.slf4j.LoggerFactory.getLogger(RewriteHistory.class);
	
	/**
	 * @param args
	 */
	public static void main(String[] args) {

		String gitRepository = args[0];
		
		FileRepositoryBuilder builder = new FileRepositoryBuilder();
		
		try {
			Repository repository = builder.setGitDir(new File(gitRepository))
			.readEnvironment() // scan environment GIT_* variables
			.findGitDir() // scan up the file system tree
			.build();
			
			Git git = new Git(repository);
			
			
			
			Iterable<RevCommit> revIter = git.log().call();
			
			List<RevCommit>oldestToNewestCommits = new ArrayList<RevCommit>();
			
			for (RevCommit commit : revIter) {

				oldestToNewestCommits.add(0, commit);
				
			}
			
			// first commit
			RevCommit firstCommit = oldestToNewestCommits.get(0);
			
			// create new branch
			
			git.branchDelete().setBranchNames("rewrite-master").setForce(true).call();
			
			git.checkout().setCreateBranch(true).setName("rewrite-master");
			
			for (RevCommit revCommit : oldestToNewestCommits) {
				
				printCommit(revCommit);
				
				
				
				
				
			}
			
//			for (String ref : refMap.keySet()) {
//
//				Ref r = refMap.get(ref);
//
//				ObjectId id = r.getObjectId();
//
//				RevWalk walk = new RevWalk(repository);
//
//				RevCommit commit = walk.parseCommit(id);
//
//				printCommit(commit);
//
//				RevTree commitTree = commit.getTree();
//
//				TreeWalk walker = new TreeWalk(repository);
//
//				while (commit.getParentCount() > 0) {
//
//					commit = commit.getParent(0);
//
//					printCommit(commit);
//
//				}
//
//			}
			
			
		} catch (Exception e) {
			log.error ("exception occured.", e);
		}
	}

	private static void printCommit(RevCommit commit) {
		
		PersonIdent ident = commit.getCommitterIdent();
		
		log.info("commit = " + commit.getId().getName());
		log.info("user = " + ident.getName());
		log.info("email = " + ident.getEmailAddress());
		log.info("date = " + ident.getWhen());
		
	}

}
