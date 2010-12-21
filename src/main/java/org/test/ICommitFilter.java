/**
 * 
 */
package org.test;

import org.eclipse.jgit.lib.PersonIdent;

/**
 * @author mocleiri
 *
 */
public interface ICommitFilter {
	
	public PersonIdent filterPersonIdent(PersonIdent personIdent);
	
	public String filterCommitMessage (String commitMessage);
}
