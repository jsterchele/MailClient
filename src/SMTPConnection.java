import java.net.*;
import java.io.*;
import java.util.*;

/* $Id: SMTPConnection.java,v 1.1.1.1 2003/09/30 14:36:01 kangasha Exp $ */

/**
 * Open an SMTP connection to a remote machine and send one mail.
 * Here is an image that demonstrates SMTP establishing a connection and sending mail
 * <br>
 * <img src="http://ntrg.cs.tcd.ie/undergrad/4ba2/x400/smtp_session.gif"/>
 * 
 * @author Joe Sterchele
 * @author Jussi Kangasharju
 */
public class SMTPConnection {
	/* The socket to the server */
	public Socket connection;

	/* Streams for reading and writing the socket */
	public BufferedReader fromServer;
	public DataOutputStream toServer;

	private static final int SMTP_PORT = 25;
	private static final String CRLF = "\r\n";

	/* Are we connected? Used in close() to determine what to do. */
	private boolean isConnected = false;

	/** Create an SMTPConnection object.
	 *  Create the socket and the associated streams.
	 *  Initialize SMTP connection. */
 public SMTPConnection(Envelope envelope) throws IOException {
	connection = new Socket(envelope.DestAddr, SMTP_PORT);
	fromServer = new BufferedReader(new InputStreamReader(connection.getInputStream()));
	toServer =   new DataOutputStream(connection.getOutputStream());
	
	/* Read a line from server and check that the reply code is 220.
	   checking that the mail server is connected and ready.
	  */

	String reply = fromServer.readLine();
	if (parseReply(reply) != 220) {
		System.out.println("Error in connect.");
		System.out.println(reply);
		return;
}

	/** SMTP handshake. **/
	/* Getting the hostname of the machine */	
	String localhost = (InetAddress.getLocalHost()).getHostName();
	/* Send Hello to Server, see if we can get a reply of 250 */
	try{
		sendCommand("HELO " + localhost, 250);
	}catch (IOException e) {
		System.out.println("Error:::");
		System.out.println("Aborting the connection");
		return;
	}
	
	isConnected = true;
 }
 
	/**
	 * Send the message. Simply writes the correct SMTP-commands in the correct
	 * order. No checking for errors, just throw them to the caller.
	 */
	public void send(Envelope envelope) throws IOException {
		//reply code for MAIL FROM is 250, getting information from the envelope
		sendCommand("MAIL FROM:<" + envelope.Sender + ">", 250);
		//reply code for RCPT TO is 250, getting information from the envelope
		sendCommand("RCPT TO:<" + envelope.Recipient + ">", 250);
		//reply code for DATA is 354
		sendCommand("DATA", 354);
		//the message is sent after the DATA command on it's own
		sendCommand(envelope.Message.toString() + CRLF + ".", 250);
		//If the message has been accepted for delivery, the STMP server
		//will respond with a reply code of 250.
	}

    /** Close the connection. First, terminate on SMTP level, then
    close the socket. */
 public void close() {
	isConnected = false;
	try {
		//221 is the reply code for the quit command
	    sendCommand("QUIT", 221);
	    // connection.close();
	} catch (IOException e) {
	    System.out.println("Unable to close connection: " + e);
	    isConnected = true;
	}
 }

	/**  Send an SMTP command to the server. Check for reply code.
	 *   Throws and IOException if the reply code from the server does not match
	 *   the proper reply code. 
	 *   
	 *   <a href="http://www.samlogic.net/articles/smtp-commands-reference.htm">Here is a web site that explains more about reply codes</a>
	 *   
	 *@param command a String command that will  be sent
	 *@param rc a Integer of the return code for the command
	 */
	private void sendCommand(String command, int rc) throws IOException {
		String reply = null;
		//writing bytes to the server  -> ending w/ CRFL for all commands
		toServer.writeBytes(command + CRLF);
		//reading in the the server
		reply = fromServer.readLine();
		//if the reply does not equal the reply code (rc) 
		if (parseReply(reply) != rc) {
			//print out error messages
			System.out.println("Error with command: " + command);
			System.out.println(reply);
			System.out.println("Looking for reply code: " + rc);
			//throws exception. closes the connection.
			throw new IOException();
		}
	}

	/** Parse the reply line from the server. Returns the reply code.
	*@param reply the reply line from the server
	*@return i an int of the reply code
	**/
	private int parseReply(String reply) {
		//splits the reply string
		StringTokenizer parser = new StringTokenizer(reply);
		String replycode = parser.nextToken();
		//must return a int
		int i = Integer.parseInt(replycode);
		return i;
	}

	/** Destructor. Closes the connection if something bad happens. 
	 * 
	 */
	protected void finalize() throws Throwable {
		if (isConnected) {
			close();
		}
		super.finalize();
	}
}

