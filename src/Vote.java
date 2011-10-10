/*Object which stores a player's vote for lynching in the Werewolf Game*/

package org.jibble.pircbot.llama.werewolf.objects;

public class Vote
{
	String name,		//name of the voter
		vote;			//name of the person they have voted for
	
	public void setName(String name)
	{
		this.name = name; 
	}

	public void setVote(String vote)
	{
		this.vote = vote; 
	}

	public String getName()
	{
		return (this.name); 
	}

	public String getVote()
	{
		return (this.vote); 
	}
		
	public Vote(String name, String vote)
	{
		setName(name);
		setVote(vote);
	}
}