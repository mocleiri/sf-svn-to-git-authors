/**
 * 
 */
package org.test;

import java.io.File;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.eclipse.jgit.JGitText;
import org.eclipse.jgit.api.CreateBranchCommand;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.GitCommand;
import org.eclipse.jgit.api.errors.CannotDeleteCurrentBranchException;
import org.eclipse.jgit.api.errors.ConcurrentRefUpdateException;
import org.eclipse.jgit.api.errors.JGitInternalException;
import org.eclipse.jgit.api.errors.NoHeadException;
import org.eclipse.jgit.lib.CommitBuilder;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.ObjectDatabase;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.ObjectInserter;
import org.eclipse.jgit.lib.PersonIdent;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.lib.RefUpdate;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.lib.RepositoryState;
import org.eclipse.jgit.lib.RefUpdate.Result;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevWalk;
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
			
//			try {
//				git.branchDelete().setBranchNames("rewrite-master").setForce(true).call();
//			} catch (CannotDeleteCurrentBranchException e) {
//				git.checkout().setName("master").call();
//				git.branchDelete().setBranchNames("rewrite-master").setForce(true).call();
//				
//			}
			
			Ref ref = git.checkout().setCreateBranch(true).setName("rewrite-master").setStartPoint(firstCommit).call();
			
			ObjectId newBaseId = ref.getObjectId();
			
			for (RevCommit revCommit : oldestToNewestCommits) {
				
				printCommit(revCommit);
				
				PersonIdent author = filterIdent(revCommit.getAuthorIdent());
				
				PersonIdent committer = filterIdent(revCommit.getAuthorIdent());
				
				revCommit.getTree();
				
				CommitBuilder commitBuilder = new CommitBuilder();
				
				commitBuilder.setParentId(newBaseId);
				commitBuilder.setTreeId(revCommit.getTree().getId());
				commitBuilder.setAuthor(author);
				commitBuilder.setCommitter(committer);
				commitBuilder.setEncoding(revCommit.getEncoding());
				commitBuilder.setMessage(revCommit.getFullMessage());
				
				ObjectInserter inserter = repository.getObjectDatabase().newInserter();
				newBaseId = inserter.insert(commitBuilder);
				
				inserter.flush();
				
				try {
					Ref head = repository.getRef(Constants.HEAD);
					
					if (head == null)
						throw new NoHeadException(
								JGitText.get().commitOnRepoWithoutHEADCurrentlyNotSupported);

					// determine the current HEAD and the commit it is referring to
					ObjectId headId = repository.resolve(Constants.HEAD + "^{commit}");
					RevWalk revWalk = new RevWalk(repository);
					try {
						RevCommit newCommit = revWalk.parseCommit(newBaseId);
						RefUpdate ru = repository.updateRef(Constants.HEAD);
						ru.setNewObjectId(newBaseId);
						ru.setRefLogMessage("commit : "
								+ newCommit.getShortMessage(), false);

						ru.setExpectedOldObjectId(headId);
						Result rc = ru.update();
						
					} finally {
						revWalk.release();
					}
				} finally {
					inserter.release();
				}
				
				
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

	private static PersonIdent filterIdent(PersonIdent authorIdent) {
		// TODO: add in the rewrite logic
		return new PersonIdent("test", "test@test.com", authorIdent.getWhen(), authorIdent.getTimeZone());
	}

	private static void printCommit(RevCommit commit) {
		
		PersonIdent ident = commit.getCommitterIdent();
		
		log.info("commit = " + commit.getId().getName());
		log.info("user = " + ident.getName());
		log.info("email = " + ident.getEmailAddress());
		log.info("date = " + ident.getWhen());
		log.info("comment = " + commit.getShortMessage());
		
	}

}
