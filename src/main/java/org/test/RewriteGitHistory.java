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
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.filefilter.IOFileFilter;
import org.apache.commons.io.filefilter.NotFileFilter;
import org.apache.commons.io.filefilter.PrefixFileFilter;
import org.eclipse.jgit.JGitText;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.RmCommand;
import org.eclipse.jgit.api.CreateBranchCommand.SetupUpstreamMode;
import org.eclipse.jgit.api.ListBranchCommand.ListMode;
import org.eclipse.jgit.api.errors.ConcurrentRefUpdateException;
import org.eclipse.jgit.api.errors.InvalidRefNameException;
import org.eclipse.jgit.api.errors.JGitInternalException;
import org.eclipse.jgit.api.errors.NoFilepatternException;
import org.eclipse.jgit.api.errors.NoHeadException;
import org.eclipse.jgit.api.errors.NoMessageException;
import org.eclipse.jgit.api.errors.RefAlreadyExistsException;
import org.eclipse.jgit.api.errors.RefNotFoundException;
import org.eclipse.jgit.api.errors.WrongRepositoryStateException;
import org.eclipse.jgit.errors.AmbiguousObjectException;
import org.eclipse.jgit.errors.IncorrectObjectTypeException;
import org.eclipse.jgit.errors.MissingObjectException;
import org.eclipse.jgit.lib.CommitBuilder;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.FileMode;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.ObjectInserter;
import org.eclipse.jgit.lib.PersonIdent;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.lib.RefUpdate;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.lib.RefUpdate.Result;
import org.eclipse.jgit.lib.TreeFormatter;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevWalk;
import org.eclipse.jgit.storage.file.FileRepositoryBuilder;
import org.eclipse.jgit.treewalk.TreeWalk;
import org.eclipse.jgit.treewalk.filter.PathFilter;
import org.slf4j.Logger;

/**
 * @author mocleiri
 * 
 * The base class for rewriting Git history.
 * 
 * Right now there are two approaches.
 * 
 * One: convertBranch.  This will rewrite the author, committer and comment for all commits in a given branch.
 * 
 * Two: extractBranch.  Assuming at the whole repo was checked out as trunk this will extract a new branch whose commits are made up of only the subtree we are extracting.  
 * Also we add in a new commit at the start of the branch that removes all of the files/paths that are not part of the extraction.
 * 
 */
public class RewriteGitHistory {

	private static final Logger log = org.slf4j.LoggerFactory
			.getLogger(RewriteGitHistory.class);

	protected Repository repository;
	protected Git git;

	protected final ICommitFilter commitFilter;
	
	/**
	 * If we need to create new commits then this is the person
	 * they are assigned to.
	 * 
	 */
	protected final PersonIdent selfCommitIdent;

	/**
	 * 
	 */
	public RewriteGitHistory(File gitRepositoryPath,
			ICommitFilter personIdentFilter, PersonIdent selfCommitIdent) throws IOException {
		super();
		this.commitFilter = personIdentFilter;
		this.selfCommitIdent = selfCommitIdent;

		FileRepositoryBuilder builder = new FileRepositoryBuilder();

		repository = builder.setGitDir(gitRepositoryPath).readEnvironment() // scan
																			// environment
																			// GIT_*
																			// variables
				
				.build();

		git = new Git(repository);

	}

	/**
	 * 
	 * @param regex
	 * @return the list of branches names from the repository that match the
	 *         regex given.
	 * 
	 */
	public List<String> extractBranchNames(ListMode mode, String regex) {

		List<Ref> branchList = git.branchList().setListMode(mode).call();

		return extractNameList(branchList);

	}

	protected List<String> extractNameList(List<Ref> refList) {
		List<String> nameList = new ArrayList<String>();

		for (Ref ref : refList) {

			nameList.add(ref.getName());
		}

		return nameList;
	}

	protected void printCommit(RevCommit commit) {

		PersonIdent ident = commit.getCommitterIdent();

		log.info("commit = " + commit.getId().getName());
		log.info("user = " + ident.getName());
		log.info("email = " + ident.getEmailAddress());
		log.info("date = " + ident.getWhen());
		log.info("comment = " + commit.getShortMessage());

	}

	/**
	 * Apply the commit on the current branch and update the head pointer.
	 * 
	 * @param commitBuilder
	 * @throws IOException
	 * @throws NoHeadException
	 */
	protected ObjectId executeCommit(CommitBuilder commitBuilder)
			throws NoHeadException, IOException {

		ObjectInserter inserter = repository.getObjectDatabase().newInserter();

		ObjectId newBaseId = null;
		
		try {
			newBaseId = inserter.insert(commitBuilder);

			inserter.flush();

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
				ru.setRefLogMessage("commit : " + newCommit.getShortMessage(),
						false);

				ru.setExpectedOldObjectId(headId);
				Result rc = ru.update();
				
				log.info("rc.type = " + rc.name());

			} finally {
				revWalk.release();
			}
			
		} finally {
			inserter.release();
		}
		
		return newBaseId;

	}
	
	/**
	 * Here we convert the branch to a new branch. We first extract the ordered
	 * list of commits.
	 * 
	 * Then we iterate over the commits and at each step we create a new commit.
	 * 
	 * The new commit references the same tree but has a different commit
	 * message (strips out the git-svn-id tags) and the authors are converted
	 * from subversion users to be the GitUsers.
	 * @param targetBranch 
	 * 
	 * @param branchName
	 * @param newBranchName
	 * @throws JGitInternalException
	 * @throws RefAlreadyExistsException
	 * @throws RefNotFoundException
	 * @throws InvalidRefNameException
	 * @throws NoHeadException
	 * @throws IOException
	 */
	public void convertBranch(String sourceBranchName, String targetBranch)
			throws JGitInternalException, RefAlreadyExistsException,
			RefNotFoundException, InvalidRefNameException, NoHeadException,
			IOException {

		// start on the source branch
		git.checkout().setName(sourceBranchName).call();
		
		// extract all history
		Iterable<RevCommit> revIter = git.log().call();

		List<RevCommit> oldestToNewestCommits = new ArrayList<RevCommit>();

		for (RevCommit commit : revIter) {

			// reverse so we can replay on the new branch
			oldestToNewestCommits.add(0, commit);

		}

		// first commit
		RevCommit firstCommit = oldestToNewestCommits.get(0);

		/*
		 * Create the new branch pointed at the first commit from the sourceBranch
		 * 
		 */
		Ref ref = git.checkout().setCreateBranch(true).setName(targetBranch).setStartPoint(firstCommit).call();

		ObjectId newBaseId = ref.getObjectId();

		for (RevCommit revCommit : oldestToNewestCommits) {

			printCommit(revCommit);

			PersonIdent author = commitFilter.filterPersonIdent(revCommit
					.getAuthorIdent());

			PersonIdent committer = commitFilter.filterPersonIdent(revCommit
					.getAuthorIdent());

			CommitBuilder commitBuilder = new CommitBuilder();

			commitBuilder.setParentId(newBaseId);
			commitBuilder.setTreeId(revCommit.getTree().getId());
			commitBuilder.setAuthor(author);
			commitBuilder.setCommitter(committer);
			commitBuilder.setEncoding(revCommit.getEncoding());
			commitBuilder.setMessage(commitFilter.filterCommitMessage(revCommit
					.getFullMessage()));

			newBaseId = executeCommit (commitBuilder);
			
			

		}

	}
	
	/**
	 * In this case the svn repository has been cloned but there are no seperate
	 * branches. Master points at the root with all the directories contained
	 * beneath it.
	 * 
	 * What we want to accomplish is to create new commits for each 'branch'
	 * that will contain only their contents.
	 * 
	 * We do not erase or modify the original commits.
	 * 
	 * We locate all of the commits that contain the includePath.
	 * 
	 * Then we locate the first commit that included it. We create new commits for each of the existing commits
	 * but we modify the tree to only contain the matched tree of each commit.
	 * @throws NoHeadException 
	 * @throws WrongRepositoryStateException 
	 * @throws ConcurrentRefUpdateException 
	 * @throws NoMessageException 
	 * @throws NoFilepatternException 
	 * 
	 * 
	 */
	public void extractBranch(String sourceBranchName, String newBranchName,
			String  includePath) throws JGitInternalException,
			RefAlreadyExistsException, RefNotFoundException,
			InvalidRefNameException, AmbiguousObjectException, IOException, NoHeadException, NoMessageException, ConcurrentRefUpdateException, WrongRepositoryStateException, NoFilepatternException {

		Ref branchId = git.checkout().setName(sourceBranchName).call();

		RevWalk rwalk = new RevWalk(repository);

		RevCommit tip = rwalk.lookupCommit(branchId.getObjectId());

		rwalk.markStart(tip);

		rwalk.setTreeFilter(PathFilter.create(includePath));

		Iterator<RevCommit> commitIter = rwalk.iterator();

		List<CommitBuilder>newBranchCommits = new ArrayList<CommitBuilder>();
		
		RevCommit branchPoint = null;
		
		while (commitIter.hasNext()) {

			RevCommit commit = (RevCommit) commitIter.next();

			printCommit(commit);

			final TreeWalk walk = new TreeWalk(repository);
			walk.setRecursive(false);  //only look at the top level entries in the commit tree
			walk.addTree(commit.getTree().getId());

			// match all of the paths on this commit
			List<ObjectId>matchedPaths = new ArrayList<ObjectId>();
			
			while (walk.next()) {
				final FileMode mode = walk.getFileMode(0);
//				if (mode == FileMode.TREE)
//					log.info("0");
//				log.info(mode.toString());
//				log.info(" ");
				
				if (mode == FileMode.TREE) {
					
					String path = walk.getPathString();
					
					if (path.startsWith(includePath)) {
						log.info("matched " +  Constants.typeString(mode.getObjectType()) + " sha1: " + walk.getObjectId(0).name() + " path: " + walk.getPathString()); 

						matchedPaths.add(walk.getObjectId(0));
						
						branchPoint = commit;
					}
					else {
						log.debug("excluded " + Constants.typeString(mode.getObjectType()) + " sha1: " + walk.getObjectId(0).name() + " path: " + walk.getPathString());
					}
				}
				else {
				log.debug("excluded " + Constants.typeString(mode.getObjectType()) +  " sha1: " + walk.getObjectId(0).name() + " path: " + walk.getPathString());
				}				
				

			}
			
			if (matchedPaths.size() > 1) {
				// create a tree to hold the tree
				log.warn("multiple matches found for path = " + includePath + " commit = " + commit.getId().name());
			}
			else if (matchedPaths.size() == 1) {
				
				PersonIdent author = commitFilter.filterPersonIdent(commit
						.getAuthorIdent());

				PersonIdent committer = commitFilter.filterPersonIdent(commit
						.getAuthorIdent());

				CommitBuilder commitBuilder = new CommitBuilder();

				// set parent id later once we have the full path
//				commitBuilder.setParentId(newBaseId);
				commitBuilder.setTreeId(matchedPaths.get(0));
				commitBuilder.setAuthor(author);
				commitBuilder.setCommitter(committer);
				commitBuilder.setEncoding(commit.getEncoding());
				commitBuilder.setMessage(commitFilter.filterCommitMessage(commit
						.getFullMessage()));
				
				newBranchCommits.add(0, commitBuilder);
			}
			else {
				// do nothing
			}

		}
		
		// done with the commits.
		ObjectId parent = branchPoint;
		
		if (branchPoint.getParentCount() > 0)
			parent = branchPoint.getParent(0);
		
		Ref branch = git.checkout().setName(newBranchName).setCreateBranch(true).setStartPoint(parent.name()).call();
		
		// first step is that we need to delete all of the files that do not match the pattern
		RmCommand rmCommand = new RmCommand(repository);
		
		File workTree = git.getRepository().getWorkTree();

		// only get the top level files and directories
		
		File[] topLevelFiles = workTree.listFiles();
		
		for (File file : topLevelFiles) {

			if (!file.getName().startsWith(includePath)) {
				log.info("removing file = " + file.getAbsolutePath());
				rmCommand.addFilepattern(file.getAbsolutePath());
			}
		}
		
		rmCommand.call();
		
		RevCommit ref = git.commit().setAuthor(selfCommitIdent).setCommitter(selfCommitIdent).setMessage("Delete uneeded content from newly extracted branch '" + newBranchName +"'").call();
		
		ObjectId parentId = ref.getId();
		
		for (CommitBuilder commitBuilder : newBranchCommits) {
		
			commitBuilder.setParentId(parentId);
			
			parentId = executeCommit(commitBuilder);
			
		}
		
	}

	/**
	 * We look backwards from the head commit on the branch named.  We are looking for the path given.
	 * 
	 * We will accumulate the list of commits where it exists.  We go all the way  back to the root of the branch to account for all branches.
	 *   
	 * 
	 * @param branchName
	 * @param filePrefix
	 * @throws MissingObjectException
	 * @throws IncorrectObjectTypeException
	 * @throws IOException
	 * @throws JGitInternalException
	 * @throws RefAlreadyExistsException
	 * @throws RefNotFoundException
	 * @throws InvalidRefNameException
	 */
	public void listMatchingCommits(String branchName, String filePrefix)
			throws MissingObjectException, IncorrectObjectTypeException,
			IOException, JGitInternalException, RefAlreadyExistsException,
			RefNotFoundException, InvalidRefNameException {

		Ref branchId = git.checkout().setName(branchName).call();

		RevWalk rwalk = new RevWalk(repository);

		RevCommit tip = rwalk.lookupCommit(branchId.getObjectId());

		rwalk.markStart(tip);

		rwalk.setTreeFilter(PathFilter.create(filePrefix));

		Iterator<RevCommit> commitIter = rwalk.iterator();

		while (commitIter.hasNext()) {

			RevCommit commit = (RevCommit) commitIter.next();

			printCommit(commit);

			final TreeWalk walk = new TreeWalk(repository);
			walk.setRecursive(false);
			walk.addTree(commit.getTree().getId());

			List<String>matchedPaths = new ArrayList<String>();
			
			while (walk.next()) {
				final FileMode mode = walk.getFileMode(0);
//				if (mode == FileMode.TREE)
//					log.info("0");
//				log.info(mode.toString());
//				log.info(" ");
				
				if (mode == FileMode.TREE) {
					
					String path = walk.getPathString();
					
					if (path.startsWith(filePrefix)) {
						log.info("matched " +  Constants.typeString(mode.getObjectType()) + " sha1: " + walk.getObjectId(0).name() + " path: " + walk.getPathString());
					}
					else {
						log.info("excluded " + Constants.typeString(mode.getObjectType()) + " sha1: " + walk.getObjectId(0).name() + " path: " + walk.getPathString());
					}
				}
				else {
				log.info("excluded " + Constants.typeString(mode.getObjectType()) +  " sha1: " + walk.getObjectId(0).name() + " path: " + walk.getPathString());
				}				
				

			}

			// while (objWalk.n)
			//
			// objWalk.

		}

	}
}
