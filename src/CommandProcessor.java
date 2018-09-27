import java.util.Collections;
import java.util.Date;
import java.util.Iterator;

/**
 * The most important class. This processes all the commands issued by the users
 *
 * @author jmishra
 */
public class CommandProcessor
{

	// session added for saving some typing overhead and slight performance benefit
	private static final Config CONFIG = Config.getInstance();

	/**
	 * A method to do login. Should show LOGIN_PROMPT for the nickname,
	 * PASSWORD_PROMPT for the password. Says SUCCESSFULLY_LOGGED_IN is
	 * successfully logs in someone. Must set the logged in user in the Config
	 * instance here
	 *
	 * @throws WhatsAppException if the credentials supplied by the user are
	 * invalid, throw this exception with INVALID_CREDENTIALS as the message
	 */
	public static void doLogin() throws WhatsAppException
	{
		CONFIG.getConsoleOutput().printf(Config.LOGIN_PROMPT);
		String nickname = CONFIG.getConsoleInput().nextLine();
		CONFIG.getConsoleOutput().printf(Config.PASSWORD_PROMPT);
		String password = CONFIG.getConsoleInput().nextLine();

		Iterator<User> userIterator = CONFIG.getAllUsers().iterator();
		while (userIterator.hasNext())
		{
			User user = userIterator.next();
			if (user.getNickname().equals(nickname) && user.getPassword()
					.equals(password))
			{
				CONFIG.setCurrentUser(user);
				CONFIG.getConsoleOutput().
				printf(Config.SUCCESSFULLY_LOGGED_IN);
				return;
			}

		}
		throw new WhatsAppException(String.
				format(Config.INVALID_CREDENTIALS));
	}

	/**
	 * A method to logout the user. Should print SUCCESSFULLY_LOGGED_OUT when
	 * done.
	 */
	public static void doLogout()
	{
		//TODO
		Config.getInstance().setCurrentUser(null);
		Config.getInstance().getConsoleOutput().printf(Config.SUCCESSFULLY_LOGGED_OUT);
	}

	/**
	 * A method to send a message. Handles both one to one and broadcasts
	 * MESSAGE_SENT_SUCCESSFULLY if sent successfully.
	 *
	 * @param nickname - can be a friend or broadcast list nickname
	 * @param message - message to send
	 * @throws WhatsAppRuntimeException simply pass this untouched from the
	 * constructor of the Message class
	 * @throws WhatsAppException throw this with one of CANT_SEND_YOURSELF,
	 * NICKNAME_DOES_NOT_EXIST messages
	 */
	public static void sendMessage(String nickname, String message) throws WhatsAppRuntimeException, WhatsAppException
	{
		//TODO

		
		User currUser= Config.getInstance().getCurrentUser(); //currUser = fromNickname in message constructor
		Date date = new Date(); // variable to hold sent time

		if(currUser.getNickname().equals(nickname))
			throw new WhatsAppException(Config.CANT_SEND_YOURSELF);

		try {
			if(!currUser.isExistingNickname(nickname))
				throw new WhatsAppException(Config.NICKNAME_DOES_NOT_EXIST);
		}catch(WhatsAppException e) {
			Config.getInstance().getConsoleOutput().printf(Config.NICKNAME_DOES_NOT_EXIST, nickname);
		}

		
		// message is a friend
		if(currUser.isFriend(nickname)) {
			//create the message to be sent, add it to the friends message list
			date = java.util.Calendar.getInstance().getTime();
			User toNickname = Helper.getUserFromNickname(currUser.getFriends(), nickname);
			Message m1 = new Message(currUser.getNickname(), toNickname.getNickname(), 
					null, date, message, false);

			toNickname.getMessages().add(m1);
			Message m2 = new Message(currUser.getNickname(), toNickname.getNickname(), 
					null, date, message, true);
			currUser.getMessages().add(m2);
			
			Config.getInstance().getConsoleOutput().printf(Config.MESSAGE_SENT_SUCCESSFULLY);
		}
		else if(currUser.isBroadcastList(nickname)) {
			//message to create is for a bcast list
			date = java.util.Calendar.getInstance().getTime();
			BroadcastList toBcastList = Helper.getBroadcastListFromNickname(currUser.getBroadcastLists(), nickname);

			//iterate through bcast list, add message to each members messages
			Iterator<String> itr = toBcastList.getMembers().iterator();
			String friend = null;
			User usrToAddMessage= null;
			while(itr.hasNext()) {
				
				friend = itr.next();
				usrToAddMessage = Helper.getUserFromNickname(currUser.getFriends(), friend);
				Message m1 = new Message(currUser.getNickname(), usrToAddMessage.getNickname(), null,
						date, message, false);
				usrToAddMessage.getMessages().add(m1);
			}
			
			//add it to the current users message list
			Message m2 = new Message(currUser.getNickname(), null, toBcastList.getNickname(),
					date, message, true);
			currUser.getMessages().add(m2);
			Config.getInstance().getConsoleOutput().printf(Config.MESSAGE_SENT_SUCCESSFULLY);
		}
	}

	/**
	 * Displays messages from the message list of the user logged in. Prints the
	 * messages in the format specified by MESSAGE_FORMAT. Says NO_MESSAGES if
	 * no messages can be displayed at the present time
	 *
	 * @param nickname - send a null in this if you want to display messages
	 * related to everyone. This can be a broadcast nickname also.
	 * @param enforceUnread - send true if you want to display only unread
	 * messages.
	 */
	public static void readMessage(String nickname, boolean enforceUnread)
	{
		User currUser= Config.getInstance().getCurrentUser();
		boolean found= false;

		//case 1: read all unread messages from nickname
		if(nickname != null && enforceUnread) {

		//read through each message in the current users list
			//if messages fromNickname = passed in nickname
				//set the message as read, update boolean
				//print the message

			for(Message m: currUser.getMessages()) {
				if(m.getFromNickname().equals(nickname)) {
					m.setRead(true);
					found = true;

					Config.getInstance().getConsoleOutput().printf(Config.MESSAGE_FORMAT, 
							m.getFromNickname(), m.getToNickname(), m.getMessage(),
							m.getSentTime());
				}
			}
		}
		//case 2: read ALL messages from a nickname
		//must include messages SENT from THIS USER to the given nickname
		else if(nickname != null && !enforceUnread) {

			//for each message in current users message list
				//check if given nickname matches fromNickname or toNickname of any message
				//also check if the given nickname is part of a message broadcast list
					//if so, mark message as read
					//update found boolean
					//print the message
			for(Message m: currUser.getMessages()) {
				if(m.getFromNickname().equals(nickname) || m.getToNickname().equals(nickname)
						|| currUser.isMemberOfBroadcastList(nickname, m.getBroadcastNickname()))
					m.setRead(true);
				found = true;

				Config.getInstance().getConsoleOutput().printf(Config.MESSAGE_FORMAT, 
						m.getFromNickname(), m.getToNickname(), m.getMessage(), 
						m.getSentTime());
			}
		}
		//case 3: read all unread message from ALL users known to logged in user
		//sort by message date
		else if(nickname == null && enforceUnread) {
			//for each message in the current users message list
			//check if the message is unread
			//if so, mark the message as read
			//update found boolean
			//print the messages in order by date
			for(Message m: currUser.getMessages()) {
				if(!m.isRead()) {
					m.setRead(true);
					found = true;
					
					Config.getInstance().getConsoleOutput().printf(Config.MESSAGE_FORMAT,
							m.getFromNickname(), m.getToNickname(), m.getMessage(),
							m.getSentTime());
				
				}
			}
		}

		//case 4: read ALL message (read or unread) from All users
		else if(nickname == null && !enforceUnread) {
			//for each message in the users list
			//print every message
				for(Message m: currUser.getMessages()) {
						m.setRead(true);
						found = true;
					
					Config.getInstance().getConsoleOutput().printf(Config.MESSAGE_FORMAT,
							m.getFromNickname(), m.getToNickname(), m.getMessage(),
							m.getSentTime());					
				}
		}
		
		//if no messages found, say so
		if(!found) {
			Config.getInstance().getConsoleOutput().printf(Config.NO_MESSAGES);
		}
	}

	/**
	 * Method to do a user search. Does a case insensitive "contains" search on
	 * either first name or last name. Displays user information as specified by
	 * the USER_DISPLAY_FOR_SEARCH format. Says NO_RESULTS_FOUND is nothing
	 * found.
	 *
	 * @param word - word to search for
	 * @param searchByFirstName - true if searching for first name. false for
	 * last name
	 */
	public static void search(String word, boolean searchByFirstName)
	{
		//TODO
		User currUser= Config.getInstance().getCurrentUser();
		User userToFind = null;
		boolean found= false; //boolean to keep track if a user is found that matches argument

		if(searchByFirstName) {
			Iterator<User> itr= Config.getInstance().getAllUsers().iterator();

			//search global user list for names that match the first name
			while(itr.hasNext()) {
				userToFind = itr.next();

				if(userToFind.getFirstName().contains(word)) {
					//user was found, update found variable
					found = true;

					//check if the found user is a friend of the user logged in
					//if so, final argument = "yes"
					if(currUser.getFriends().contains(userToFind)) {
						Config.getInstance().getConsoleOutput().printf(
								Config.USER_DISPLAY_FOR_SEARCH, userToFind.getLastName(), userToFind.getFirstName(),
								userToFind.getNickname(), "yes");
					}

					//if not, final argument = "no"
					else {
						Config.getInstance().getConsoleOutput().printf(
								Config.USER_DISPLAY_FOR_SEARCH, userToFind.getLastName(), userToFind.getFirstName(),
								userToFind.getNickname(), "no");
					}
				}
			}
		}
		else if(!searchByFirstName) {
			//search by last name
			Iterator<User> itr= Config.getInstance().getAllUsers().iterator();

			//iterate through global list to find a match for last name
			while(itr.hasNext()) {
				userToFind = itr.next();
				if(userToFind.getLastName().contains(word)) {
					//user was found, update found variable
					found = true;

					//check if it is a friend of current user
					if(currUser.getFriends().contains(userToFind)) {
						Config.getInstance().getConsoleOutput().printf(
								Config.USER_DISPLAY_FOR_SEARCH, userToFind.getFirstName(), userToFind.getLastName(),
								userToFind.getNickname(), "yes");
					}
					else if(!userToFind.getFriends().contains(userToFind)) {
						Config.getInstance().getConsoleOutput().printf(
								Config.USER_DISPLAY_FOR_SEARCH, userToFind.getFirstName(), userToFind.getLastName(),
								userToFind.getNickname(), "no");
					}
				}
			}
		}

		if(!found) {
			Config.getInstance().getConsoleOutput().printf(Config.CANT_LOCATE, word);
		}
	}

	/**
	 * Adds a new friend. Says SUCCESSFULLY_ADDED if added. Hint: use the
	 * addFriend method of the User class.
	 *
	 * @param nickname - nickname of the user to add as a friend
	 * @throws WhatsAppException simply pass the exception thrown from the
	 * addFriend method of the User class
	 */
	public static void addFriend(String nickname) throws WhatsAppException
	{
		//TODO
		User currUser = Config.getInstance().getCurrentUser();
		currUser.addFriend(nickname);

		if(currUser.isFriend(nickname))
			Config.getInstance().getConsoleOutput().printf(Config.SUCCESSFULLY_ADDED);

	}

	/**
	 * removes an existing friend. Says NOT_A_FRIEND if not a friend to start
	 * with, SUCCESSFULLY_REMOVED if removed. Additionally removes the friend
	 * from any broadcast list she is a part of
	 *
	 * @param nickname nickname of the user to remove from the friend list
	 * @throws WhatsAppException simply pass the exception from the removeFriend
	 * method of the User class
	 */
	public static void removeFriend(String nickname) throws WhatsAppException
	{
		CONFIG.getCurrentUser().removeFriend(nickname);
		CONFIG.getConsoleOutput().printf(Config.SUCCESSFULLY_REMOVED);
	}

	/**
	 * adds a friend to a broadcast list. Says SUCCESSFULLY_ADDED if added
	 *
	 * @param friendNickname the nickname of the friend to add to the list
	 * @param bcastNickname the nickname of the list to add the friend to
	 * @throws WhatsAppException throws a new instance of this exception with
	 * one of NOT_A_FRIEND (if friendNickname is not a friend),
	 * BCAST_LIST_DOES_NOT_EXIST (if the broadcast list does not exist),
	 * ALREADY_PRESENT (if the friend is already a member of the list),
	 * CANT_ADD_YOURSELF_TO_BCAST (if attempting to add the user to one of his
	 * own lists
	 */
	public static void addFriendToBcast(String friendNickname,
			String bcastNickname) throws WhatsAppException
	{
		if (friendNickname.equals(CONFIG.getCurrentUser().getNickname()))
		{
			throw new WhatsAppException(Config.CANT_ADD_YOURSELF_TO_BCAST);
		}
		if (!CONFIG.getCurrentUser().isFriend(friendNickname))
		{
			throw new WhatsAppException(Config.NOT_A_FRIEND);
		}
		if (!CONFIG.getCurrentUser().isBroadcastList(bcastNickname))
		{
			throw new WhatsAppException(String.
					format(Config.BCAST_LIST_DOES_NOT_EXIST, bcastNickname));
		}
		if (CONFIG.getCurrentUser().
				isMemberOfBroadcastList(friendNickname, bcastNickname))
		{
			throw new WhatsAppException(Config.ALREADY_PRESENT);
		}
		Helper.
		getBroadcastListFromNickname(CONFIG.getCurrentUser().
				getBroadcastLists(), bcastNickname).getMembers().
		add(friendNickname);
		CONFIG.getConsoleOutput().printf(Config.SUCCESSFULLY_ADDED);
	}

	/**
	 * removes a friend from a broadcast list. Says SUCCESSFULLY_REMOVED if
	 * removed
	 *
	 * @param friendNickname the friend nickname to remove from the list
	 * @param bcastNickname the nickname of the list from which to remove the
	 * friend
	 * @throws WhatsAppException throw a new instance of this with one of these
	 * messages: NOT_A_FRIEND (if friendNickname is not a friend),
	 * BCAST_LIST_DOES_NOT_EXIST (if the broadcast list does not exist),
	 * NOT_PART_OF_BCAST_LIST (if the friend is not a part of the list)
	 */
	public static void removeFriendFromBcast(String friendNickname,
			String bcastNickname) throws WhatsAppException
	{
		//TODO
		User currUser= Config.getInstance().getCurrentUser();
		if(!currUser.isFriend(friendNickname))
			throw new WhatsAppException(Config.NOT_A_FRIEND);

		if(!currUser.isBroadcastList(bcastNickname))
			throw new WhatsAppException(Config.BCAST_LIST_DOES_NOT_EXIST);

		if(!currUser.isMemberOfBroadcastList(friendNickname, bcastNickname))
			throw new WhatsAppException(Config.NOT_PART_OF_BCAST_LIST);

		BroadcastList b = Helper.getBroadcastListFromNickname(currUser.getBroadcastLists(), bcastNickname);
		Iterator<String> itr = b.getMembers().iterator();
		String friendToRemove= null;
		while(itr.hasNext()) {
			friendToRemove = itr.next();
			if(friendToRemove.equals(friendNickname)) {
				itr.remove();
				Config.getInstance().getConsoleOutput().printf(Config.SUCCESSFULLY_REMOVED);
			}
		}
	}

	/**
	 * A method to remove a broadcast list. Says BCAST_LIST_DOES_NOT_EXIST if
	 * there is no such list to begin with and SUCCESSFULLY_REMOVED if removed.
	 * Hint: use the removeBroadcastList method of the User class
	 *
	 * @param nickname the nickname of the broadcast list which is to be removed
	 * from the currently logged in user
	 * @throws WhatsAppException Simply pass the exception returned from the
	 * removeBroadcastList method of the User class
	 */
	public static void removeBroadcastcast(String nickname) throws WhatsAppException
	{
		//TODO
		User currUser = Config.getInstance().getCurrentUser();

		if(!currUser.isBroadcastList(nickname))
			throw new WhatsAppException(Config.BCAST_LIST_DOES_NOT_EXIST);

		currUser.removeBroadcastList(nickname);

		if(!currUser.isBroadcastList(nickname))
			Config.getInstance().getConsoleOutput().printf(Config.SUCCESSFULLY_REMOVED);
	}

	/**
	 * Processes commands issued by the logged in user. Says INVALID_COMMAND for
	 * anything not conforming to the syntax. This basically uses the rest of
	 * the methods in this class. These methods throw either or both an instance
	 * of WhatsAppException/WhatsAppRuntimeException. You ought to catch such
	 * exceptions here and print the messages in them. Note that this method
	 * does not throw any exceptions. Handle all exceptions by catch them here!
	 *
	 * @param command the command string issued by the user
	 */
	public static void processCommand(String command)
	{
		try
		{
			switch (command.split(":")[0])
			{
			case "logout":
				doLogout();
				break;
			case "send message":
				String nickname = command.
				substring(command.indexOf(":") + 1, command.
						indexOf(",")).trim();
				String message = command.
						substring(command.indexOf("\"") + 1, command.trim().
								length() - 1); // CORRECTED: Added - 1
				sendMessage(nickname, message);
				break;
			case "read messages unread from":
				nickname = command.
				substring(command.indexOf(":") + 1, command.trim().
						length()).trim();
				readMessage(nickname, true);
				break;
			case "read messages all from":
				nickname = command.
				substring(command.indexOf(":") + 1, command.trim().
						length()).trim();
				readMessage(nickname, false);
				break;
			case "read messages all":
				readMessage(null, false);
				break;
			case "read messages unread":
				readMessage(null, true);
				break;
			case "search fn":
				String word = command.
				substring(command.indexOf(":") + 1, command.trim().
						length()).trim();
				search(word, true);
				break;
			case "search ln":
				word = command.
				substring(command.indexOf(":") + 1, command.trim().
						length()).trim();
				search(word, false);
				break;
			case "add friend":
				nickname = command.
				substring(command.indexOf(":") + 1, command.trim().
						length()).trim();
				addFriend(nickname);
				break;
			case "remove friend":
				nickname = command.
				substring(command.indexOf(":") + 1, command.trim().
						length()).trim();
				removeFriend(nickname);
				break;
			case "add to bcast":
				String nickname0 = command.
				substring(command.indexOf(":") + 1, command.
						indexOf(",")).
				trim();
				String nickname1 = command.
						substring(command.indexOf(",") + 1, command.trim().
								length()).
						trim();
				addFriendToBcast(nickname0, nickname1);
				break;
			case "remove from bcast":
				nickname0 = command.
				substring(command.indexOf(":") + 1, command.
						indexOf(",")).
				trim();
				nickname1 = command.
						substring(command.indexOf(",") + 1, command.trim().
								length()).
						trim();
				removeFriendFromBcast(nickname0, nickname1);
				break;
			case "remove bcast":
				nickname = command.
				substring(command.indexOf(":") + 1, command.trim().
						length()).trim(); // CORRECTED: Added trim()
				removeBroadcastcast(nickname);
				break;
			default:
				CONFIG.getConsoleOutput().
				printf(Config.INVALID_COMMAND);
			}
		} catch (StringIndexOutOfBoundsException ex)
		{
			CONFIG.getConsoleOutput().
			printf(Config.INVALID_COMMAND);
		} catch (WhatsAppException | WhatsAppRuntimeException ex)
		{
			CONFIG.getConsoleOutput().printf(ex.getMessage());
		}
	}

}