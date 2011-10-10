/******************
 * Werewolf.java
 * Main code file for The Werewolf Game bot, based on pIRC Bot framework (www.jibble.org)
 * Coded by Mark Vine
 * All death/character/other description texts written by Darkshine
 * 31/5/2004 - 4/6/2004
 * v0.99b
 *****************/
package org.jibble.pircbot.llama.werewolf;

import java.io.*;
import java.util.*;

import org.jibble.pircbot.*;
import org.jibble.pircbot.llama.werewolf.objects.*;

public class Werewolf extends PircBot
{
	Vector players,		//Vector to store all the players in the game
		priority,		//Vector to store players for the next round, one the current game is full
		votes,			//A Vector to hold the votes of the villagers
		wolves, 		//A Vector to store the wolf/ves
		wolfVictim;		//A Vector to hold the wolves choices (in the case there are 2 wolves taht vote differently
	
	final int JOINTIME = 60,	//time (in seconds) for people to join the game (final because cannot be altered)
		MINPLAYERS = 4,			//Minimum number of players to start a game
		MAXPLAYERS = 12,		//Maximum number of players allowed in the game
		TWOWOLVES = 8,			//Minimum number of players needed for 2 wolves
		
		//Final ints to describe the types of message that can be sent (Narration, game, control, notice), so
		//they can be coloured accordingly after being read from the file
		NOTICE = 1, NARRATION = 2, GAME = 3, CONTROL = 4;
		
	int dayTime = 90,			//time (in seconds) for daytime duration (variable because it changes with players)
		nightTime = 60,			//time (in seconds) for night duration
		voteTime = 30,			//time (in seconds) for the lynch vote
		seer,					//index of the player that has been nominated seer
		toSee = -1,				//index of the player the seer has selected to see. If no player, this is -1
		roundNo;				//holds the number of the current round (for the star trek mode) 
	int[] notVoted,				//holds the count of how many times players have not voted successively
		wasVoted;				//holds the count of how many votes one person has got.
	
	boolean connected = false,	//boolean to show if the bot is connected
		playing = false,		//boolean to show if the game is running
		day = false,			//boolean to show whether it's day or night
		gameStart = false,		//boolean to show if it's the start of the game (for joining)
		firstDay,				//boolean to show if it's the first day (unique day message)
		firstNight,				//boolean to show if it's the first night (unique night message)
		tieGame = true,			//boolean to determine if there will be a random tie break with tied votes.
		timeToVote,				//boolean to show whether it's currently time to vote
		debug,					//boolean to show if debug mode is one or off (Print to log files)
		doBarks;				//boolean to show if the bot should make random comments after long periods of inactivity
	boolean[] wolf,				//array for checking if a player is a wolf
		dead,					//array for checking if a player is dead
		voted;					//array to check which players have already voted
	
	Timer gameTimer,			//The game Timer (duh)
		idleTimer;				//timer to count idle time for random comments
	
	String name,			//The bot's name
		network,			//The network to connect to
		gameChan,			//The channel the game is played in
		ns,					//The nickname service nick
		command,			//The command the bot sends to the nickservice to identify on the network
		gameFile,			//Specifies the file name to read the game texts from.
		role,
		beenwolf = "",
		beenseer = "",
		oneWolf, manyWolves;
	long delay;				//The delay for messages to be sent
	
	public Werewolf()
	{
		this.setLogin("Werewolf");
		this.setVersion("Werewolf Game Bot by LLamaBoy and Darkshine - using pIRC framework from http://www.jible.org");
		this.setMessageDelay(100);
		
		String filename = "werewolf.ini",
			lineRead = "";
			gameFile = "wolfgame.txt";
		FileReader reader;
		BufferedReader buff;
		
		try
		{
			reader = new FileReader(filename);
			buff = new BufferedReader(reader);
			
			while(!lineRead.startsWith("botname"))
				lineRead = buff.readLine();
			name = lineRead.substring(lineRead.lastIndexOf(" ") + 1, lineRead.length());
			
			while(!lineRead.startsWith("network"))
				lineRead = buff.readLine();
			network = lineRead.substring(lineRead.lastIndexOf(" ") + 1, lineRead.length());
			
			while(!lineRead.startsWith("channel"))
				lineRead = buff.readLine();
			gameChan = lineRead.substring(lineRead.lastIndexOf(" ") + 1, lineRead.length());
			
			while(!lineRead.startsWith("nickservice"))
				lineRead = buff.readLine();
			ns = lineRead.substring(lineRead.lastIndexOf(" ") + 1, lineRead.length());
			
			while(!lineRead.startsWith("nickcmd"))
				lineRead = buff.readLine();
			command = lineRead.substring(lineRead.indexOf("=") + 2, lineRead.length());
			
			while(!lineRead.startsWith("debug"))
				lineRead = buff.readLine();
				
			String onoff = lineRead.substring(lineRead.lastIndexOf(" ") + 1, lineRead.length());
			if(onoff.equalsIgnoreCase("on"))
				debug = true;
			else if (onoff.equalsIgnoreCase("off"))
				debug = false;
			else
			{
				System.out.println("Unknown debug value, defaulting to on.");
				debug = true;
			}
			
			while(!lineRead.startsWith("delay"))
				lineRead = buff.readLine();
				
			delay = Long.parseLong(lineRead.substring(lineRead.lastIndexOf(" ") + 1, lineRead.length()));
			this.setMessageDelay(delay);
			
			while(!lineRead.startsWith("daytime"))
				lineRead = buff.readLine();
			try
			{
				int tmp = Integer.parseInt(lineRead.substring(lineRead.indexOf("=") + 2, lineRead.length()));
				dayTime = tmp;
			}
			catch(NumberFormatException nfx)
			{
				System.out.println("Bad day time value; defaulting to 90 seconds");
				dayTime = 90;
			}
				
			while(!lineRead.startsWith("nighttime"))
				lineRead = buff.readLine();
			try
			{
				int tmp = Integer.parseInt(lineRead.substring(lineRead.indexOf("=") + 2, lineRead.length()));
				nightTime = tmp;
			}
			catch(NumberFormatException nfx)
			{
				System.out.println("Bad night time value; defaulting to 45 seconds");
				nightTime = 45;
			}
				
			while(!lineRead.startsWith("votetime"))
				lineRead = buff.readLine();
			try
			{
				int tmp = Integer.parseInt(lineRead.substring(lineRead.indexOf("=") + 2, lineRead.length()));
				voteTime = tmp;
			}
			catch(NumberFormatException nfx)
			{
				System.out.println("Bad vote time value; defaulting to 90 seconds");
				voteTime = 30;
			}
				
			while(!lineRead.startsWith("tie"))
				lineRead = buff.readLine();
				
			onoff = lineRead.substring(lineRead.lastIndexOf(" ") + 1, lineRead.length());
			if(onoff.equalsIgnoreCase("on"))
				tieGame = true;
			else if (onoff.equalsIgnoreCase("off"))
				tieGame = false;
			else
			{
				System.out.println("Unknown vote tie value, defaulting to on.");
				tieGame = true;
			}
			
			while(!lineRead.startsWith("idlebarks"))
				lineRead = buff.readLine();
			
			onoff = lineRead.substring(lineRead.lastIndexOf(" ") + 1, lineRead.length());
			if(onoff.equalsIgnoreCase("on"))
				doBarks = true;
			else if (onoff.equalsIgnoreCase("off"))
				doBarks = false;
			else
			{
				System.out.println("Unknown vote tie value, defaulting to off.");
				doBarks = true;
			}	
		}
		catch(FileNotFoundException fnfx)
		{
			System.err.println("Initialization  file " + filename + " not found.");
			fnfx.printStackTrace();
			System.exit(1);
		}
		catch(IOException iox)
		{
			System.err.println("File read Exception");
			iox.printStackTrace();
			System.exit(1);
		}
		catch(Exception x)
		{
			System.err.println("Other Exception caught");
			x.printStackTrace();
			System.exit(1);
		}
		
		if(debug)
		{
			this.setVerbose(true);
			try
			{
				File file = new File("wolf.log");
				if(!file.exists())
					file.createNewFile();
				PrintStream fileLog = new PrintStream(new FileOutputStream(file, true));
				System.setOut(fileLog);
				System.out.println((new Date()).toString());
				System.out.println("Starting log....");
				
				File error = new File("error.log");
				if(!file.exists())
					file.createNewFile();
				PrintStream errorLog = new PrintStream(new FileOutputStream(error, true));
				System.setErr(errorLog);
				System.err.println((new Date()).toString());
				System.err.println("Starting error log....");
				
			}
			catch(FileNotFoundException fnfx)
			{
				fnfx.printStackTrace();
			}
			catch(IOException iox)
			{
				iox.printStackTrace();
			}
		}
		
		connectAndJoin();
		startIdle();
	}
	
	//overloaded method that calls the method with 2 player names
	protected String getFromFile(String text, String player, int time, int type)
	{
		return getFromFile(text, player, null, time, type);
	}
	
	//a couple of game string need 2 player names, so this is the ACTUAL method
	protected String getFromFile(String text, String player, String player2, int time, int type)
	{
		FileReader reader;
		BufferedReader buff;
		
		try
		{
			reader = new FileReader(gameFile);
			buff = new BufferedReader(reader);
			String lineRead = "";

			while(!lineRead.equals(text))
			{
				lineRead = buff.readLine();
			}
			
			Vector texts = new Vector(1, 1);
			lineRead = buff.readLine();
				
			while(!lineRead.equals(""))
			{
				texts.add(lineRead);
				lineRead = buff.readLine();
			}
			
			buff.close();
			reader.close();
			
			int rand = (int) (Math.random() * texts.size());
			String toSend = (String)texts.get(rand);
			
			if(texts.size() > 0)
			{
				switch(type)
				{
					case NOTICE:	//no colour formatting
						toSend = toSend.replaceAll("PLAYER", player);
						toSend = toSend.replaceAll("PLAYR2", player2);
						toSend = toSend.replaceAll("TIME", "" + time);
						toSend = toSend.replaceAll("ISAWOLF?", role);
						if(wolves != null && !wolves.isEmpty())
							toSend = toSend.replaceAll("WOLF", (wolves.size() == 1 ? oneWolf : manyWolves));
						toSend = toSend.replaceAll("BOTNAME", this.getNick());
						toSend = toSend.replaceAll("ROLE", role);
						
						//for one of the Bored texts
						java.text.DecimalFormat two = new java.text.DecimalFormat("00");
						
						toSend = toSend.replaceAll("RANDDUR", ((int)(Math.random() * 2)) + " days, " +
							two.format((int)(Math.random() * 24)) + ":" +
							two.format((int)(Math.random() * 61)) + ":" +
							two.format((int)(Math.random() * 61)));
						toSend = toSend.replaceAll("RANDNUM", "" + ((int)(Math.random() * 50) + 1));
						
						return toSend;
						//break;
					
					case NARRATION:	//blue colour formatting
						toSend = toSend.replaceAll("PLAYER",
							Colors.DARK_BLUE + Colors.BOLD + player + Colors.NORMAL + Colors.DARK_BLUE);
						toSend = toSend.replaceAll("PLAYR2",
							Colors.DARK_BLUE + Colors.BOLD + player2 + Colors.NORMAL + Colors.DARK_BLUE);
						toSend = toSend.replaceAll("TIME",
							Colors.DARK_BLUE + Colors.BOLD + time +	Colors.NORMAL + Colors.DARK_BLUE);
						if(wolves != null && !wolves.isEmpty())
							toSend = toSend.replaceAll("WOLF", (wolves.size() == 1 ? oneWolf : manyWolves));
						toSend = toSend.replaceAll("BOTNAME", this.getNick());
						toSend = toSend.replaceAll("ROLE", role);
						return Colors.DARK_BLUE + toSend;
						//break;
						
					case GAME:		//red colour formatting
						toSend = toSend.replaceAll("PLAYER",
							Colors.BROWN + Colors.UNDERLINE + player + Colors.NORMAL + Colors.RED);
						toSend = toSend.replaceAll("PLAYR2",
							Colors.BROWN + Colors.UNDERLINE + player2 + Colors.NORMAL + Colors.RED);
						toSend = toSend.replaceAll("TIME",
							Colors.BROWN + Colors.UNDERLINE + time + Colors.NORMAL + Colors.RED);
						if(wolves != null && !wolves.isEmpty())
							toSend = toSend.replaceAll("WOLF", (wolves.size() == 1 ? oneWolf : manyWolves));
						toSend = toSend.replaceAll("BOTNAME", this.getNick());
						toSend = toSend.replaceAll("ROLE", role);
						return Colors.RED + toSend;
						//break;
						
					case CONTROL:	//Green colour formatting
						toSend = toSend.replaceAll("PLAYER",
							Colors.DARK_GREEN + Colors.UNDERLINE + player + Colors.NORMAL + Colors.DARK_GREEN);
						toSend = toSend.replaceAll("PLAYR2",
							Colors.DARK_GREEN + Colors.UNDERLINE + player2 + Colors.NORMAL + Colors.DARK_GREEN);
						toSend = toSend.replaceAll("TIME",
							Colors.DARK_GREEN + Colors.UNDERLINE + time + Colors.NORMAL + Colors.DARK_GREEN);
						if(wolves != null && !wolves.isEmpty())
							toSend = toSend.replaceAll("WOLF", (wolves.size() == 1 ? oneWolf : manyWolves));
						toSend = toSend.replaceAll("BOTNAME", this.getNick());
						toSend = toSend.replaceAll("ROLE", role);
						return Colors.DARK_GREEN + toSend;
						//break;
						
					default:
						return null;
				}
			}
		}
		catch(Exception x)
		{
			x.printStackTrace();
		}
		
		return null;
	}
	
	protected void onPrivateMessage(String sender, String login, String hostname, String message)
	{
		if(playing) //commands only work if the game is on
		{
			if(message.toLowerCase().equalsIgnoreCase("join")) //join the game
			{
				if(gameStart)
				{
					if(isInChannel(sender))
					{
						if(!isNameAdded(sender)) //has the player already joined?
						{
							if(players.size() < MAXPLAYERS) //if there are less than MAXPLAYERS player joined
							{
								if(players.add(sender)) //add another one
								{
									this.setMode(gameChan, "+v " + sender);
									this.sendMessage(gameChan, getFromFile("JOIN", sender, 0, NARRATION));
									this.sendNotice(sender,
										getFromFile("ADDED", null, 0, NOTICE));
								}
								else	//let the user know the adding failed, so he can try again
									this.sendNotice(sender,
										"Could not add you to player list. Please try again.");
							}
							else if(priority.size() < MAXPLAYERS) //if play list is full, add to priority list
							{
								if(priority.add(sender))
									this.sendNotice(sender,
										"Sorry, the maximum players has been reached. You have been placed in the " +
										"priority list for the next game.");
								else
									this.sendNotice(sender,
										"Could not add you to priority list. Please try again.");
							}
							else //if both lists are full, let the user know to wait for the current game to end
							{
								this.sendNotice(sender,
									"Sorry, both player and priority lists are full. Please wait for the " +
									"the current game to finish before trying again.");
							}
						}
						else //if they have already joined, let them know
						{
							this.sendNotice(sender,
								"You are already on the player list. Please wait for the next game to join again.");
						}
					}
				}
				else //if the game is already running, dont add anyone else to either list
				{
					this.sendNotice(sender,
						"The game is currently underway. Please wait for " +
						"the current game to finish before trying again.");
				}
			}
			else if(isNamePlaying(sender))
			{
				if(timeToVote) //commands for when it's vote time
				{
					if(message.toLowerCase().startsWith("vote")) //vote to lynch someone
					{
						if(!hasVoted(sender))
						{
							try
							{
								String choice = message.substring(message.indexOf(" ") + 1, message.length());
								choice = choice.trim();
								
								if(isNamePlaying(choice))
								{
									Vote vote = new Vote(sender, choice);
									
									for(int i = 0 ; i < players.size() ; i++)
									{
										if(players.get(i) != null)
										{
											if(((String)players.get(i)).equalsIgnoreCase(choice) && !dead[i])
												while(!votes.add(vote));
											else if(((String)players.get(i)).equalsIgnoreCase(choice) && dead[i])
											{
												this.sendNotice(sender, "Your choice is already dead.");
												return;
											}
										}
									}
									
									for(int i = 0 ; i < players.size() ; i++)
									{
										if(sender.equals((String)players.get(i)))
											if(!dead[i])
											{
												voted[i] = true;
												notVoted[i] = 0;
												this.sendMessage(gameChan,
													getFromFile("HAS-VOTED", sender, choice, 0, NARRATION));
											}
											else
												this.sendNotice(sender, "You are dead. You cannot vote.");
									}
								}
								else
									this.sendNotice(sender, "Your choice is not playing in the current game. Please select someone else.");
							}
							catch(Exception x)
							{
								this.sendNotice(sender, "Please vote in the correct format: /msg " + this.getName() +
									" vote <player>");
								x.printStackTrace();
							}
						}
						else
							this.sendNotice(sender, "You have already voted. You may not vote again until tomorrow.");
					}
				}
				else if(!day) //commands for the night
				{
					if(message.toLowerCase().startsWith("kill")) //only wolves can kill someone. They may change their minds if they wish to.
					{
						if(isNamePlaying(sender))
						{
							boolean isWolf = false;
							
							for(int i = 0 ; i < wolves.size() ; i++)
							{
								if(wolves.get(i).equals(sender))
									isWolf = true;
							}
							
							if(isWolf)
							{
								try
								{	
									String victim = message.substring(message.indexOf(" ") + 1, message.length());
									victim = victim.trim();
								
									if(isNamePlaying(victim))
									{
										int index = 0;
										boolean isDead = false;
										
										for(int i = 0 ; i < players.size() ; i++)
										{
											if(((String)players.get(i)).equalsIgnoreCase(victim))
												if(dead[i])
													isDead = true;
										}
										if(!isDead)
										{
											if(!victim.equalsIgnoreCase(sender))
											{
												while(!wolfVictim.add(victim));
												
												if(wolves.size() == 1)
												{
													this.sendNotice(sender,
														getFromFile("WOLF-CHOICE", victim, 0, NOTICE));
												}
												else
												{	
													this.sendNotice(sender,
														getFromFile("WOLVES-CHOICE", victim, 0, NOTICE));
														
													for(int i = 0 ; i < wolves.size() ; i++)
													{
														if(!((String)wolves.get(i)).equals(sender))
															this.sendNotice((String)wolves.get(i),
																getFromFile("WOLVES-CHOICE-OTHER", sender, victim, 0, NOTICE));
													}
												}
											}
											else
												this.sendNotice(sender, "You cannot eat yourself!");
										}
										else
											this.sendNotice(sender, "That person is already dead.");
									}
									else
										throw new Exception();
								}
								catch(Exception x)
								{
									x.printStackTrace();
									this.sendNotice(sender, "Please choose a valid player.");
								}
							}
							else
								this.sendNotice(sender, getFromFile("NOT-WOLF", null, 0, NOTICE));
						}
						else
						{
							this.sendNotice(sender, "You aren't playing!");
						}
					}
					if(message.toLowerCase().startsWith("see")) //only the seer may watch over someone
					{
						try
						{
							if(isNamePlaying(sender))	
							{
								if(players.get(seer).equals(sender))
								{
									if(!dead[seer])
									{
										String see = message.substring(message.indexOf(" ") + 1, message.length());
										see = see.trim();
										if(isNamePlaying(see))
										{
											if(!sender.equals(see))
											{
												for(int i = 0 ; i < players.size() ; i++)
												{
													if(players.get(i) != null && ((String)players.get(i)).equalsIgnoreCase(see))
														toSee = i;
												}
												
												this.sendNotice(sender, 
													getFromFile("WILL-SEE", (String) players.get(toSee), 0, NOTICE));
											}
											else
												this.sendNotice(sender, "You already know that you are human!");
										}
									}
									else
										this.sendNotice(sender, getFromFile("SEER-DEAD", null, 0, NOTICE));
								}
								else
									this.sendNotice(sender,
										getFromFile("NOT-SEER", null, 0, NOTICE));
							}
							else
								this.sendNotice(sender, "That person is not playing.");
						}
						catch(Exception x)
						{
							this.sendNotice(sender, "Please provide a valid player name.");
							x.printStackTrace();
						}
					}
					if(message.toLowerCase().startsWith("protect")) //Only the GA may protect someone
					{
						//to be implemented (maybe)
					}
				}
				if(message.toLowerCase().equalsIgnoreCase("alive"))
				{
					String names = "The players left alive are: ";
					
					for(int i = 0 ; i < players.size() ; i++)
					{
						if(!dead[i] && players.get(i) != null)
							names += (String)players.get(i) + " ";
					}
					
					this.sendNotice(sender, names);
				}
				if(message.toLowerCase().equalsIgnoreCase("role"))
				{
					if(!gameStart)
					{
						for(int i = 0 ; i < players.size() ; i++)
						{
							if(sender.equals((String)players.get(i)))
								if(wolf[i])
								{
									if(wolves.size() == 1)
										this.sendNotice(sender,	getFromFile("W-ROLE", null, 0, NOTICE));
									else
									{
										for(int j = 0 ; j < wolves.size() ; j++)
										{
											if(!sender.equals(wolves.get(j)))
												this.sendNotice(sender,
													getFromFile("WS-ROLE", (String) wolves.get(j), 0, NOTICE));
										}
									}
								}
								else if (i == seer)
									this.sendNotice(sender,	getFromFile("S-ROLE", null, 0, NOTICE));
								else
									this.sendNotice(sender,	getFromFile("V-ROLE", null, 0, NOTICE));
										
						}
					}
				}
			}
		}
	}
	
	protected void onMessage(String channel, String sender, String login, String hostname, String message) 
	{
		if(message.toLowerCase().startsWith("!quit"))
		{
			if(isSenderOp(sender))
			{
				if(playing)
					doVoice(false);
				this.setMode(gameChan, "-mN");
				this.partChannel(gameChan, "Werewolf Game Bot created by LLamaBoy." +
				" Texts by Darkshine. Based on the PircBot at http://www.jibble.org/");
				this.sendMessage(sender, "Now quitting...");
				while(this.getOutgoingQueueSize() != 0);
				this.quitServer();
				System.out.println("Normal shutdown complete");
				for(int i = 0 ; i < 4 ; i++)
				{
					System.out.println();
					System.err.println();
				}
				System.out.close();
				try
				{
					Thread.sleep(1000);
				}
				catch(InterruptedException ix)
				{
					ix.printStackTrace();
				}
				System.exit(0);
			}
		}
		else if(!playing)
		{
			if(message.startsWith("!startrek"))		//Easter egg suggested by Deepsmeg.
			{
				gameFile = "startrek.txt";
			}
			if(message.startsWith("!start")) //initiates a game.
			{
				startGame(sender);
				
				//Get the strings to distinguish between a single wolf and 2 wolves in the texts.
				oneWolf = getFromFile("1-WOLF", null, 0, NOTICE);
				manyWolves = getFromFile("MANY-WOLVES", null, 0, NOTICE);
				
				if(players.add(sender))
				{
					this.setMode(gameChan, "+v " + sender);
					this.sendNotice(sender,
						getFromFile("ADDED", null, 0, NOTICE));
				}
				else
					this.sendNotice(sender,
						"Could not add you to player list. Please try again." +
						" (/msg " + this.getName() + " join.");
			}
			else if(message.startsWith("!daytime ")) //alter the duration of the day
			{
				if(isSenderOp(sender))
				{
					try
					{
						int time = (Integer.parseInt(message.substring(message.indexOf(" ") + 1, message.length())));
						if(time > 0)
						{
							dayTime = time;
							this.sendMessage(gameChan, this.getFromFile("DAYCHANGE", null, time, CONTROL));
							 //"Duration of the day now set to " + Colors.DARK_GREEN + Colors.UNDERLINE + dayTime + Colors.NORMAL + " seconds");
						}
						else
							throw new Exception();
					}
					catch(Exception x)
					{
						this.sendMessage(gameChan, "Please provide a valid value for the daytime length (!daytime <TIME IN SECONDS>)");
					}
				}
			}
			else if(message.startsWith("!nighttime ")) //alter the duration of the night
			{
				if(isSenderOp(sender))
				{
					try
					{
						int time = (Integer.parseInt(message.substring(message.indexOf(" ") + 1, message.length())));
						if(time > 0)
						{
							nightTime = time;
							this.sendMessage(gameChan, this.getFromFile("NIGHTCHANGE", null, time, CONTROL));
						}
						else
							throw new Exception();
					}
					catch(Exception x)
					{
						this.sendMessage(gameChan, "Please provide a valid value for the night time length (!nighttime <TIME IN SECONDS>)");
					}
				}
			}
			else if(message.startsWith("!votetime ")) //alter the time for a vote
			{
				if(isSenderOp(sender))
				{
					try
					{
						int time = (Integer.parseInt(message.substring(message.indexOf(" ") + 1, message.length())));
						if(time > 0)
						{
							voteTime = time;
							this.sendMessage(gameChan, this.getFromFile("VOTECHANGE", null, time, CONTROL));
						}
						else
							throw new Exception();
					}
					catch(Exception x)
					{
						this.sendMessage(gameChan, "Please provide a valid value for the Lynch Vote time length (!votetime <TIME IN SECONDS>)");
					}
				}
			}
			else if(message.startsWith("!tie ")) //tie option
			{
				if(isSenderOp(sender))
				{
					try
					{
						String tie = message.substring(message.indexOf(" ") + 1, message.length());
						if(tie.equalsIgnoreCase("on"))
						{
							tieGame = true;
							this.sendMessage(gameChan, Colors.DARK_GREEN + "Vote tie activated");
						}
						else if(tie.equalsIgnoreCase("off"))
						{
							tieGame = false;
							this.sendMessage(gameChan, Colors.DARK_GREEN + "Vote tie deactivated");
						}
						else
							throw new Exception();
					}
					catch(Exception x)
					{
						this.sendMessage(gameChan, "Please provide a valid value for the vote tie condition (!tie ON/OFF)");
					}
				}
			}
			else if(message.startsWith("!shush")) //stop idle barks
			{
				if(isSenderOp(sender))
				{
					if(doBarks)
					{
						doBarks = false;
						idleTimer.cancel();
						idleTimer = null;
						this.sendMessage(gameChan, Colors.DARK_GREEN + "I won't speak any more.");
					}
					else
						this.sendMessage(gameChan, Colors.DARK_GREEN + "I'm already silent. Moron.");
				}
			}
			else if(message.startsWith("!speak")) //enable idle barks
			{
				if(isSenderOp(sender))
				{
					if(!doBarks)
					{
						doBarks = true;
						startIdle();
						this.sendMessage(gameChan, Colors.DARK_GREEN + "Speakage: on.");
					}
					else
						this.sendMessage(gameChan, Colors.DARK_GREEN + "I'm already speaking and stuff. kthx");
				}
			}
			else if(message.startsWith("!daytime"))	
				this.sendMessage(gameChan, Colors.DARK_GREEN + "Day length is " + dayTime + " seconds.");
			else if(message.startsWith("!nighttime"))	
				this.sendMessage(gameChan, Colors.DARK_GREEN + "Night length is " + nightTime + " seconds.");
			else if(message.startsWith("!votetime"))	
				this.sendMessage(gameChan, Colors.DARK_GREEN + "Lynch Vote length is " + voteTime + " seconds.");
			else if(message.startsWith("!tie"))	
				this.sendMessage(gameChan, Colors.DARK_GREEN + "Lynch vote tie is " + (tieGame ? "on." : "off."));
		}
		else if(message.indexOf("it") != -1 && message.indexOf(this.getNick()) != -1 && day &&
			message.indexOf("it") < message.indexOf(this.getNick())) //when it looks like the bot is accused, send a reply
		{
			this.sendMessage(gameChan, "Hey, screw you, " + sender + "! I didn't kill anyone!");
		}
		else
		{
			if(playing)
			{
				if(!gameStart)
				{
					for(int i = 0 ; i < players.size() ; i++)
					{
						if(dead[i] && sender.equals(players.get(i)))
							this.setMode(gameChan, "-v " + sender);
					}
					
				}
			}
		}
	}
	
	protected void onAction(String sender, String login, String hostname, String target, String action)
	{
		if(playing)
		{
			if(!gameStart)
			{
				for(int i = 0 ; i < players.size() ; i++)
				{
					if(dead[i] && sender.equals(players.get(i)))
						this.setMode(gameChan, "-v " + sender);
				}
			}
		}
	}
	
	protected boolean isSenderOp(String nick) //to check if the sender of a message is op, necessary for some options
	{
		User users[] = this.getUsers(gameChan);
			
		for(int i = 0 ; i < users.length ; i++)
		{
			if(users[i].getNick().equals(nick))
				if(users[i].isOp())
					return true;
				else
					return false;
		}
		return false;
	}
	
	//if the player changed their nick and they're in the game, changed the listed name
	protected void onNickChange(String oldNick, String login, String hostname, String newNick)
	{
		if(playing)
		{
			if(isNameAdded(oldNick))
			{
				for(int i = 0 ; i < players.size() ; i++)
				{	
					if(oldNick.equals((String) players.get(i)))
					{
						String old = (String)players.set(i, newNick);
						
						if(!dead[i])
							this.sendMessage(gameChan, Colors.DARK_GREEN +
								old + " has changed nick to " + players.get(i) + "; Player list updated.");
						break;
					}
				}
				for(int i = 0 ; i < priority.size() ; i++)
				{	
					if(oldNick.equals((String) priority.get(i)))
					{
						players.set(i, newNick);
						this.sendMessage(gameChan, Colors.DARK_GREEN +
							newNick + " has changed nick; Priority list updated.");
						break;
					}
				}
				
				//This doesn't seem to work at times...
				if(timeToVote) //if the player changes nick during the vote period, update any votes against him
				{
					for(int i = 0 ; i < votes.size() ; i++)
					{
						if(((String)((Vote)votes.get(i)).getVote()).equalsIgnoreCase(oldNick))
						{
							Vote newVote = new Vote((String)votes.get(i), newNick);
							votes.set(i, newVote);
						}
					}
				}
			}
		}
	}
	
	//if a player leaves while te game is one, remove him from the player list
	//and if there is a priority list, add the first person from that in his place.
	protected void onPart(String channel, String sender, String login, String hostname) 
	{
		if(playing)
		{
			if(!priority.isEmpty())
			{
				String newPlayer = (String) priority.get(0);
				String role = "";
				priority.remove(0);
				
				for(int i = 0 ; i < players.size() ; i++)
				{
					if(sender.equals((String)players.get(i)) && !dead[i])
					{
						if(i == seer)
							role = getFromFile("ROLE-SEER", null, 0, NOTICE);
						else if(wolf[i])
							role = getFromFile("ROLE-WOLF", null, 0, NOTICE);
						else
							role = getFromFile("ROLE-VILLAGER", null, 0, NOTICE);
						players.set(i, newPlayer);
						
						this.sendNotice(newPlayer,
							getFromFile("FLEE-PRIORITY-NOTICE", sender, 0, NOTICE));
						this.sendMessage(gameChan,
							getFromFile("FLEE-PRIORITY-NOTICE", sender, newPlayer, 0, CONTROL));
						if(day || timeToVote)
							this.setMode(gameChan, "+v " + newPlayer);
						
						if(i == seer)
							this.sendNotice(newPlayer,
								getFromFile("S-ROLE", null, 0, NOTICE));
						else if(wolf[i])
							if(wolves.size() == 1)
								this.sendNotice(newPlayer,
									getFromFile("W-ROLE", null, 0, NOTICE));
							else
								this.sendNotice(newPlayer,
									getFromFile("WS-ROLE", null, 0, NOTICE));
						else
							this.sendNotice(newPlayer,
								getFromFile("V-ROLE", null, 0, NOTICE));
						
						break;
					}
				}
			}
			else if(!gameStart)
			{
				int index = -1;
				for(int i = 0 ; i < players.size() ; i++)
				{
					if(sender.equals((String)players.get(i)))
					{
						index = i;
						players.set(i, null);
						if(!dead[i])
						{
							if(wolf[i])
							{
								for(int j = 0 ; j < wolves.size() ; j++)
								{
									if(((String)wolves.get(j)).equals(sender))
										wolves.remove(j);
								}
								
								this.sendMessage(gameChan,
									getFromFile("FLEE-WOLF", sender, 0, NARRATION));
							}
							else
								this.sendMessage(gameChan,
									getFromFile("FLEE-VILLAGER", sender, 0, NARRATION));
								
							dead[i] = true;
								
							if(wolfVictim != null)
							{
								for(int j = 0 ; j < wolfVictim.size() ; j++)
								{
									if(sender.equals(wolfVictim.get(j)))
										wolfVictim.set(j, null);
								}
							}
					
							checkWin();
						}
					}
				}
				if(timeToVote)
				{
					for(int i = 0 ; i < votes.size() ; i++)
					{
						if(((String)((Vote)votes.get(i)).getVote()).equalsIgnoreCase(sender))
							votes.remove(i); 		//if the player leaves, no votes for him
					}
				}
			}
			else if(gameStart)
			{
				for(int i = 0 ; i < players.size() ; i++)
				{
					if(sender.equals(players.get(i)))
					{
						players.remove(i);
						this.sendMessage(gameChan,
								getFromFile("FLEE", sender, 0, NARRATION));
					 	break;
					}
				}
			}
		}
		else
		{
			if(priority == null || priority.isEmpty());
			else
			{
				for(int i = 0 ; i < priority.size() ; i++)
				{
					if(sender.equals((String)priority.get(i)))
					{
						priority.remove(i);
						this.sendMessage(gameChan, Colors.DARK_GREEN + Colors.UNDERLINE +
							sender + Colors.NORMAL + Colors.DARK_GREEN +
							", a player on the priority list, has left. Removing from list...");
					}
				}
			}
		}
	}
	
	protected void onTopic(String channel, String topic, String setBy, long date, boolean changed)
	{
		if(playing)
		{
			this.sendMessage(gameChan, "Hey! Can't you see we're trying to play a game here? >:(");
		}
	}
	
	protected void onQuit(String sourceNick, String sourceLogin, String sourceHostname, String reason)
	{
		this.onPart(gameChan, sourceNick, sourceLogin, sourceHostname);
	}
	
	protected void onDisconnect()
	{
		connected = false;
		connectAndJoin();
	}
	
	protected void connectAndJoin()
	{
		while(!connected) //keep trying until successfully connected
		{
			try
			{
				this.setName(name);
				this.connect(network);
				Thread.sleep(1000);		//allow it some time before identifiying
				if(!ns.equalsIgnoreCase("none"))
				{
					this.sendMessage(ns, command);		//identify with nickname service
					Thread.sleep(2000);		//allow the ident to go through before joining
				}
				this.joinChannel(gameChan);
				this.setMode(gameChan, "-mN");
				connected = true;
			}
			catch(IOException iox)
			{
				System.err.println("Could not connect/IO");
			}
			catch(NickAlreadyInUseException niux)
			{
				System.err.println("Nickname is already in use. Choose another nick");
				System.exit(1);
			}
			catch(IrcException ircx)
			{
				System.err.println("Could not connect/IRC");
			}
			catch(InterruptedException iex)
			{
				System.err.println("Could not connect/Thread");
			}
		}
	}
	
	protected void startIdle()
	{
		if(doBarks)
		{
			idleTimer = new Timer();
			//trigger a chat every 60-240 mins
			idleTimer.schedule(new WereTask(), ((int)(Math.random() * 7200) + 3600) * 1000);
		}
	}
	
	protected void onKick(String channel, String kickerNick, String kickerLogin,
		String kickerHostname, String recipientNick, String reason)
	{
		if(recipientNick.equals(this.getName()))
			this.joinChannel(channel);
		else
			this.onPart(gameChan, recipientNick, null, null);
	}
	
	protected void onJoin(String channel, String sender, String login, String hostname)
	{
		if(!sender.equals(this.getNick()))
		{
			if(gameStart)
				this.sendNotice(sender,
					getFromFile("GAME-STARTED", null, 0, NOTICE));
			else if(playing)
				this.sendNotice(sender,
					getFromFile("GAME-PLAYING", null, 0, NOTICE));
		}
	}
	
	protected void startGame(String sender)
	{	
		if(doBarks)
			idleTimer.cancel();	//don't make comments while the game's on.
		
		this.setMode(gameChan, "+mN");
		
		roundNo = 0;
		
		if(priority == null || priority.isEmpty())
			players = new Vector(MINPLAYERS, 1);
		else
		{
			players = new Vector(MINPLAYERS, 1);
			
			this.sendMessage(gameChan, Colors.DARK_GREEN +
				"");
			for(int i = 0 ; i < priority.size() ; i++)
			{
				if(sender.equals((String)priority.get(i)))
					priority.remove(i);
				else
				{
					if(players.add((String)priority.get(i)))
					{
						this.setMode(gameChan, "+v " + (String)priority.get(i));
						this.sendMessage(gameChan,
							getFromFile("JOIN", sender, 0, NARRATION));
					}
					else
						this.sendNotice(gameChan, "Sorry, you could not be added. Please try again.");
				}
			}
		}
			
		priority = new Vector(1, 1);
		wolves = new Vector(1, 1);
		
		playing = true;
		day = false;
		timeToVote = false;
		gameStart = true;
		firstDay = true;
		firstNight = true;
		toSee = -1;
		
		this.sendMessage(gameChan, 
			getFromFile("STARTGAME", sender, JOINTIME, NARRATION));
			
		this.sendNotice(gameChan,
			getFromFile("STARTGAME-NOTICE", sender, 0, NOTICE));
		
		while(this.getOutgoingQueueSize() != 0);
		
		gameTimer = new Timer();
		gameTimer.schedule(new WereTask(), JOINTIME * 1000);
	}
	
	protected void playGame()
	{
		String nicks = "",
			modes = "";
		int count = 0;
		
		if(playing)
		{
			if(timeToVote)
			{
				for(int i = 0 ; i < players.size() ; i++)
				{
					if(!dead[i])
					{
						if(notVoted[i] == 2)
						{
							dead[i] = true;
							
							this.sendMessage(gameChan,
								getFromFile("NOT-VOTED", (String)players.get(i), 0, NARRATION));
							this.sendNotice((String) players.get(i),
								getFromFile("NOT-VOTED-NOTICE", null, 0, NOTICE));
								
							this.setMode(gameChan, "-v " + players.get(i));
						}
					}
				}
				
				if(checkWin())
					return;
				
				this.sendMessage(gameChan,
					getFromFile("VOTETIME", null, voteTime, GAME));
						
				while(this.getOutgoingQueueSize() != 0);
				
				gameTimer.schedule(new WereTask(), voteTime * 1000);
			}
			else if(day)
			{
				if(toSee != -1)
					if(!dead[seer] && !dead[toSee])
					{
						if(wolf[toSee])
							role = getFromFile("ROLE-WOLF", null, 0, NOTICE);
						else
							role = getFromFile("ROLE-VILLAGER", null, 0, NOTICE);
						
						this.sendNotice((String)players.get(seer),
							getFromFile("SEER-SEE", (String) players.get(toSee),toSee, NOTICE));
					}
					else if(dead[seer])
						this.sendNotice((String)players.get(seer),
							getFromFile("SEER-SEE-KILLED", (String) players.get(toSee), 0, NOTICE));
					else
						this.sendNotice((String)players.get(seer),
							getFromFile("SEER-SEE-TARGET-KILLED", (String) players.get(toSee), 0, NOTICE));
				
				doVoice(true);
						
				this.sendMessage(gameChan,
					getFromFile("DAYTIME", null, dayTime, GAME));
				
				while(this.getOutgoingQueueSize() != 0);
				
				gameTimer.schedule(new WereTask(), dayTime * 1000);
			}
			else if(!day)
			{
				doVoice(false);
				
				if(firstNight)
				{
					firstNight = false;
					this.sendMessage(gameChan,
						getFromFile("FIRSTNIGHT", null, 0, NARRATION));
				}
				else
					this.sendMessage(gameChan, Colors.DARK_BLUE +
						getFromFile("NIGHTTIME", null, 0, NARRATION));
						
				if(wolves.size() == 1)
					this.sendMessage(gameChan,
						getFromFile("WOLF-INSTRUCTIONS", null, nightTime, GAME));
				else
					this.sendMessage(gameChan,
						getFromFile("WOLVES-INSTRUCTIONS", null, nightTime, GAME));
						
				if(!dead[seer])
					this.sendMessage(gameChan,
						getFromFile("SEER-INSTRUCTIONS", null, nightTime, GAME));
				
				while(this.getOutgoingQueueSize() != 0);
				
				gameTimer.schedule(new WereTask(), nightTime * 1000);
			}
		}
	}
	
	//method to batch voice/devoice all the users on the playerlist.
	protected void doVoice(boolean on)
	{
		String nicks = "",
			modes = "";
		int count = 0;
		
		if(on)
			modes = "+";
		else
			modes = "-";
		
		for(int i = 0 ; i < players.size() ; i++)
		{
			try
			{
				if(!dead[i] && players.get(i) != null)
				{
					nicks += players.get(i) + " ";
					modes += "v";
					count++;
					if(count % 4 == 0)
					{
						this.setMode(gameChan, modes + " " + nicks);
						nicks = "";
						
						if(on)
							modes = "+";
						else
							modes = "-";
						
						count = 0;
					}
				}
			}
			catch(NullPointerException npx)
			{
				System.out.println("Could not devoice, no dead array");
			}
		}
		
		this.setMode(gameChan, modes + " " + nicks); //mode the stragglers that dont make a full 4
	}
	
	//check if the name who tries to join is IN THE DAMN CHANNEL AT THE TIME (fuck you false)
	protected boolean isInChannel(String name)
	{
		User[] users = this.getUsers(gameChan);
		
		for(int i = 0 ; i < users.length ; i++)
		{
			if(users[i].getNick().equals(name))
				return true;
		}
		
		return false;
	}
	
	//method to go through the player and priority lists, to check if the player has already joined the game
	protected boolean isNameAdded(String name)
	{
		for(int i = 0 ; i < players.size() ; i++)
		{	
			if(name.equals((String) players.get(i)))
				return true;
		}
		for(int i = 0 ; i < priority.size() ; i++)
		{	
			if(name.equals((String) players.get(i)))
				return true;
		}
		
		return false;
	}
	
	//go through the player list, check the player is in the current game
	protected boolean isNamePlaying(String name)
	{
		for(int i = 0 ; i < players.size() ; i++)
		{
			if(players.get(i) != null && name.equalsIgnoreCase((String) players.get(i)))
				return true;
		}
		
		return false;
	}
	
	protected boolean hasVoted(String name)
	{
		for(int i = 0 ; i < players.size() ; i++)
		{
			if(name.equals((String) players.get(i)))
				if(voted[i])
					return true;
				else
					return false;
		}
		
		return false;
	}
	
	protected void tallyVotes()
	{	
		this.sendMessage(gameChan,
			getFromFile("TALLY", null, 0, CONTROL));
		
		for(int i = 0 ; i < players.size() ; i++)
		{
			if(players.get(i) != null)
			{
				if(voted[i])
				{
					Vote thisVote = null;
					
					for(int k = 0 ; k < votes.size() ; k++)
					{
						if(((String)players.get(i)).equals(((Vote)votes.get(k)).getName()))
						{
							thisVote = (Vote)votes.get(k);
							break;
						}
					}
					
					for(int j = 0 ; j < players.size() ; j++)
					{
						if(players.get(j) != null && thisVote.getVote() != null)
							if(thisVote.getVote().equalsIgnoreCase((String)players.get(j)))
								wasVoted[j]++;
					}
				}
				else if(!dead[i])
				{
					notVoted[i]++;
				}
			}
		}
		
		int majority = 0,	//holds the majority vote
			guilty = -1;	//holds the index of the person with the majority
		Vector majIndexes = new Vector(1, 1);	//Vector which holds the indexes of all those with a majority
		
		for(int i = 0 ; i < wasVoted.length ; i++) //loop once to find the majority
		{
			if(wasVoted[i] > majority)
				majority = wasVoted[i];
		}
		
		for(int i = 0 ; i < wasVoted.length ; i++) //loop again to count how many have a majority (tie case)
		{
			if(wasVoted[i] == majority)
				majIndexes.add(new Integer(i));
		}
		
		if(majIndexes.size() == 1) //only one with majority
			guilty = Integer.parseInt(((Integer)majIndexes.get(0)).toString());
		else if(tieGame && (majIndexes != null && majIndexes.size() != 0))
		{
			int rand = (int) (Math.random() * majIndexes.size());
			if (wasVoted[((Integer)majIndexes.get(rand)).intValue()] == 0) //if the majority was 0, no-one voted
				guilty = -1;
			else
			{
				guilty = ((Integer)majIndexes.get(rand)).intValue();
				this.sendMessage(gameChan,
					getFromFile("TIE", null, 0, CONTROL));
			}
		}
		else
			guilty = -10;
		
		if(guilty == -10)
		{
			this.sendMessage(gameChan, Colors.DARK_BLUE +
				getFromFile("NO-LYNCH", null, 0, NARRATION));
		}
		else if(guilty != -1)
		{
			String guiltyStr = (String) players.get(guilty);
			dead[guilty] = true;
			
			if(guiltyStr == null) //if the guilty person is null, he left during the lynch vote.
			{
				this.sendMessage(gameChan,
					getFromFile("LYNCH-LEFT", null, 0, CONTROL));
				return;
			}
			
			if(guilty == seer)
			{
				this.sendMessage(gameChan,
					getFromFile("SEER-LYNCH", guiltyStr, 0, NARRATION));
				role = getFromFile("ROLE-SEER", null, 0, NARRATION);
			}
			else if(wolf[guilty])
			{
				if(wolves.size() != 1)
				{
					for(int i = 0 ; i < wolves.size() ; i++)
					{
						if(guiltyStr.equals((String)wolves.get(i)))
							wolves.remove(i);
					}
				}
				
				this.sendMessage(gameChan,
					getFromFile("WOLF-LYNCH", guiltyStr, 0, NARRATION));
				role = getFromFile("ROLE-WOLF", null, 0, NARRATION);
			}
			else
			{
				this.sendMessage(gameChan,
					getFromFile("VILLAGER-LYNCH", guiltyStr, 0, NARRATION));
				role = getFromFile("ROLE-VILLAGER", null, 0, NARRATION);
			}
			
			this.sendMessage(gameChan,
				getFromFile("IS-LYNCHED", guiltyStr, 0, NARRATION));
			
			if(guilty != seer && guilty > -1 && !wolf[guilty])
				this.sendNotice(guiltyStr,
					getFromFile("DYING-BREATH", null, 0, NOTICE));
			else
				this.setMode(gameChan, "-v " + guiltyStr);
		}
		else //if guilty == -1
		{
			this.sendMessage(gameChan,
				getFromFile("NO-VOTES", null, 0, NARRATION));
		}
	}
	
	protected void wolfKill()
	{
		String victim = "";
			
		if(wolfVictim.isEmpty())
		{
			this.sendMessage(gameChan,
				getFromFile("NO-KILL", null, 0, NARRATION));
			return;
		}
		else if(wolfVictim.size() == 1)
			victim = (String) wolfVictim.get(0);
		else
		{
			if(wolfVictim.get(0).equals(wolfVictim.get(1)))
				victim = (String) wolfVictim.get(0);
			else
			{
				int randChoice = (int) (Math.random() * wolfVictim.size());
				victim = (String) wolfVictim.get(randChoice);
			}
				
		}
		
		for(int i = 0 ; i < players.size() ; i++)
		{
			if(players.get(i) != null && ((String)players.get(i)).equalsIgnoreCase(victim))
			{
				if(players.get(i) != null)
				{
					String deadName = (String) players.get(i);	//use the name from the game list for case accuracy
					
					dead[i] = true;		//make the player dead
					
					if(i == seer)
					{
						this.sendMessage(gameChan,
							getFromFile("SEER-KILL", deadName, 0, NARRATION));
						role = getFromFile("ROLE-SEER", null, 0, NOTICE);
					}
					else
					{
						this.sendMessage(gameChan, 
							getFromFile("VILLAGER-KILL", deadName, 0, NARRATION));
						role = getFromFile("ROLE-VILLAGER", null, 0, NOTICE);
					}
					
					this.sendMessage(gameChan, Colors.DARK_BLUE +
						getFromFile("IS-KILLED", deadName, 0, NARRATION));
					
					this.setMode(gameChan, "-v " + victim);
				}
				else
				{
					for(int j = 0 ; j < wolves.size() ; j++)
					{
						this.sendNotice((String)wolves.get(j), "The person you selected has left the game");
					}
				}
			}
		}
	}
	
	protected void setRoles()
	{
		if(players.size() < MINPLAYERS)		
		{
			//Not enough players
			this.setMode(gameChan, "-mN");
			doVoice(false);
			
			this.sendMessage(gameChan,
				getFromFile("NOT-ENOUGH", null, 0, CONTROL));
			playing = false;
			gameFile = "wolfgame.txt"; //reset the game file
			startIdle();
			
			return;
		}
		else
		{
			int randWolf = (int) (Math.random() * players.size());
			wolves.add(players.get(randWolf));
			wolf[randWolf] = true;
			
			if(players.size() < TWOWOLVES) //if there are lese than TWOWOLVES players, only one wolf
				this.sendNotice((String)players.get(randWolf),
					getFromFile("WOLF-ROLE", (String)players.get(randWolf), 0, NOTICE));
			else //otherwise, 2 wolves, and they know each other
			{
				boolean isWolf = true;
				
				while(isWolf) //to make sure the random number isnt the same again.
				{
					randWolf = (int) (Math.random() * players.size());
					
					if(!wolf[randWolf])
						isWolf = false;
						
				}
				wolves.add(players.get(randWolf));
				wolf[randWolf] = true;
				
				//pm both wolves and let them know who the other is
				//a bit ugly, but it does the job
				for(int i = 0 ; i < wolves.size() ; i++)
					this.sendNotice((String)wolves.get(i),
						getFromFile("WOLVES-ROLE", (String)(i == 0 ? wolves.get(1) : wolves.get(0)),
							0, NOTICE));
						
				this.sendMessage(gameChan,
					getFromFile("TWOWOLVES", null, 0, CONTROL));
			}
		}
		
		//Find a seer. He cannot be a wolf, obviously.
		boolean isWolf = true;
				
		while(isWolf)
		{
			seer = (int) (Math.random() * players.size());
					
			if(!wolf[seer])
				isWolf = false;
		}
		
		this.sendNotice((String)players.get(seer),
			getFromFile("SEER-ROLE", (String)players.get(seer), 0, NOTICE));
			
		for(int i = 0 ; i < players.size() ; i++) //tell anyone that isnt a wolf that they are human
		{
			try { if(i%2 == 0) Thread.sleep(300); }
			catch(Exception x) { x.printStackTrace(); }
			if(!wolf[i] && i != seer)
				this.sendNotice((String)players.get(i),
					getFromFile("VILLAGER-ROLE", (String)players.get(i), 0, NOTICE));
		}
	}
	
	protected boolean checkWin()
	{
		int humanCount = 0,	//count how many humans are left
			wolfCount = 0;	//count how many wolves are left
			
		for(int i = 0 ; i < players.size() ; i++)
		{
			if(!wolf[i] && !dead[i] && players.get(i) != null)
				humanCount++;
			else if(wolf[i] && !dead[i])
				wolfCount++;
		}
		
		if(wolfCount == 0)	//humans win
		{
			playing = false;
			this.sendMessage(gameChan,
				getFromFile("VILLAGERS-WIN", null, 0, NARRATION));
			this.sendMessage(gameChan,
				getFromFile("CONGR-VILL", null, 0 , NARRATION));
			
			doVoice(false);
			this.setMode(gameChan, "-mN");
			
			day = false;
			
			for(int i = 0 ; i < players.size() ; i++) 
				dead[i] = false;
				
			gameTimer.cancel();		//stop the game timer, since someone won.
			gameTimer = null;
			//reset the game file to default
			gameFile = "wolfgame.txt";
			
			//start the idling again
			startIdle();
			
			return true;
		}
		else if(wolfCount == humanCount)	//wolves win
		{
			playing = false;
			if(players.size() < TWOWOLVES)
			{
				String wolfPlayer = "";
				for(int i = 0 ; i < players.size() ; i++)
				{
					if(wolf[i])
						wolfPlayer = (String) players.get(i);
				}
				
				this.sendMessage(gameChan, 
					getFromFile("WOLF-WIN", wolfPlayer, 0, NARRATION));
				this.sendMessage(gameChan,
					getFromFile("CONGR-WOLF", wolfPlayer, 0 , NARRATION));
			
			}
			else
			{
				String theWolves = getFromFile("WOLVES-WERE", null, 0, CONTROL);
				
				for(int i = 0 ; i < wolves.size() ; i++)
				{
					if(wolves.get(i) != null)
						theWolves += (String) wolves.get(i) + " ";
				}
				
				this.sendMessage(gameChan, Colors.DARK_BLUE +
					getFromFile("WOLVES-WIN", null, 0, NARRATION));
				this.sendMessage(gameChan,
					getFromFile("CONGR-WOLVES", null, 0 , NARRATION));
				this.sendMessage(gameChan, theWolves);
			}
				
			doVoice(false);
			this.setMode(gameChan, "-mN");
			
			for(int i = 0 ; i < players.size() ; i++) 
				dead[i] = false;
			
			day = false;
			
			gameTimer.cancel();		//stop the game timer, since someone won.
			gameTimer = null;
			//reset the game file to default
			gameFile = "wolfgame.txt";
			
			//start the idling again
			startIdle();
			
			return true;
		}
		else	//No-one wins
			return false;
	}
	
	public static void main(String args[])
	{
		new Werewolf();
	}
	
 	private class WereTask extends TimerTask
 	{
 		public void run()
 		{
 			if(playing)
 			{
	 			if(gameStart) //the start of the game
	 			{
		 			gameStart = false; //stop the joining
		 			
		 			wolfVictim = new Vector(1, 1);
		 			votes = new Vector(1, 1);
		 			
		 			
		 			voted = new boolean[players.size()];
		 			wolf = new boolean[players.size()];
		 			dead = new boolean[players.size()];
		 			notVoted = new int[players.size()];
		 			wasVoted = new int[players.size()];
		 			
		 			for(int i = 0 ; i < players.size() ; i++)
		 			{
		 				voted[i] = false;	//initiate if people have voted to false
		 				wolf[i] = false;	//set up the wolf array. Noone is a wolf at first.
		 				dead[i] = false;	//set up the dead array. Noone is dead at first.
		 				notVoted[i] = 0;	//set up the not voted count. There are no non voters at first.
		 				wasVoted[i] = 0;	//set up the vote count. Nobody has any votes at first.
		 			}
		 			
		 			Werewolf.this.sendMessage(gameChan, Colors.DARK_GREEN + "Joining ends.");
		 			Werewolf.this.setRoles();
					
		 			//once everything is set up, start the game proper
					playGame();
		 		}
		 		else if(day) //the day ends
		 		{
		 			day = !day;
		 			
		 			timeToVote = true;
		 			
		 			playGame();
		 		
		 		}
		 		else if(timeToVote) //voting time begins
		 		{
		 			timeToVote = false;
		 			
		 			tallyVotes();
		 			
		 			votes = new Vector(1, 1);
		 			
		 			for(int i = 0 ; i < voted.length ; i++)
		 			{
		 				voted[i] = false;
		 			}
		 			
		 			for(int i = 0 ; i < wasVoted.length ; i++)
		 			{
		 				wasVoted[i] = 0;
		 			}
		 			
		 			toSee = -1;
		 			
		 			checkWin();
		 			playGame();
		 		}
		 		else if(!day) //the night ends
		 		{
		 			wolfKill();
		 			
		 			wolfVictim = new Vector(1, 1);
		 			
		 			day = !day;
		 			checkWin();
		 			playGame();	
		 		}
		 	}
		 	else
		 	{
		 		User[] users = Werewolf.this.getUsers(gameChan);
		 		String rand;
		 		do
		 		{	rand = users[((int)(Math.random() * users.length))].getNick();	}
		 		while(rand.equals(Werewolf.this.getNick()));
		 		
		 		String msg = Werewolf.this.getFromFile("BORED", rand, 0, Werewolf.this.NOTICE);
		 		if(msg != null)
		 			Werewolf.this.sendMessage(gameChan, msg);
		 		Werewolf.this.startIdle();
		 	}
 		}
 	}
}