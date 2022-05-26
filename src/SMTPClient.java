/*Copyright (C) 2022  Riley Kuttruff
*
*	This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or
*	(at your option) any later version.
*
*	This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General
*	Public License for more details.
*
*	You should have received a copy of the GNU General Public License along with this program.  If not, see <https://www.gnu.org/licenses/>.
*
*/

import java.io.*;
import java.net.*;
import java.nio.*;
import java.nio.charset.*;
import java.util.*;
import java.util.regex.*;

import javax.net.SocketFactory;
import javax.net.ssl.*;

import javax.swing.JOptionPane;
import javax.swing.JLabel;
import javax.swing.JPasswordField;
import javax.swing.JPanel;

public class SMTPClient{
	//Basic constants
	private static final String CRLF = "\r\n";
	private static final String SMTP_SERVER_URL = "smtp.gmail.com";
	private static final int SMTP_SERVER_PORT = 465;
	
	//Return codes
	public static final int ERR_OK = 0x0;
	public static final int ERR_NO_HOST = 0x1;
	public static final int ERR_CONNECTION_FAILED = 0x2;
	public static final int ERR_IO_ERROR = 0x3;
	public static final int ERR_AUTH_FAILED = 0x4;
	public static final int ERR_INVALID_OPT = 0x5;
	public static final int ERR_BAD_COMMAND_LINE = 0x6;
	public static final int ERR_NO_RECIPIENTS = 0x7;
	public static final int ERR_NO_GUI = 0x8;
	
	//SMTP return code constants
	//RFC 5321 4.2.2-3
	private static final int SMTP_STATUS_A = 200;
	private static final int SMTP_STATUS_B = 211;
	private static final int SMTP_HELP = 214;
	private static final int SMTP_READY = 220;
	private static final int SMTP_CLOSING = 221;
	private static final int SMTP_OK = 250;
	private static final int SMTP_WILL_FORWARD = 251;
	private static final int SMTP_CANNOT_VERIFY_WILL_TRY = 252;
	private static final int SMTP_START_MAIL = 354;
	private static final int SMTP_UNAVAILABLE_CONNECTION_PROBLEM = 421;
	private static final int SMTP_MAILBOX_UNAVAILABLE = 450;
	private static final int SMTP_ABORTED_LOCAL_ERROR = 451;
	private static final int SMTP_TOO_MANY = 452;
	private static final int SMTP_UNABLE_TO_ACCOMMODATE_PARAMETERS = 455;
	private static final int SMTP_SYNTAX_ERROR = 500;
	private static final int SMTP_SYNTAX_ERROR_PARAMETERS_OR_ARGUMENTS = 501;
	private static final int SMTP_COMMAND_NOT_IMPLEMENTED = 502;
	private static final int SMTP_BAD_SEQUENCE = 503;
	private static final int SMTP_PARAMETER_NOT_IMPLEMENTED = 504;
	private static final int SMTP_INVALID_ADDRESS = 551;
	private static final int SMTP_EXCEEDED_STORAGE_ALLOCATION = 552;
	private static final int SMTP_MAILBOX_NAME_INVALID = 553;
	private static final int SMTP_TRANSACTION_FAILED = 554;
	private static final int SMTP_MAILFROM_RCPTTO_NOT_RECOGNIZED = 555;
	
	//Commands required for minimum implementation of RFC 5321 4.5.1 (Except VRFY
	//which is not supported by gmail anyway)
	private static final String EHLO = "EHLO ";
	private static final String HELO = "HELO ";
	private static final String MAIL = "MAIL FROM:<%s>";
	private static final String RCPT = "RCPT TO:<%s>";
	private static final String DATA = "DATA";
	private static final String RSET = "RSET";
	private static final String NOOP = "NOOP";
	private static final String QUIT = "QUIT";
	
	//I/O variables
	private static Socket socket;
	private static PrintWriter out;
	private static PrintStream stdOut, stdErr;
	private static BufferedReader in, stdIn;
	
	//Session data
	private static String uName;		//-from=addr
	private static String[] recipients;	//-to=addr(;addr)*
	private static byte[] authData;
	private static char[] pass;			//-pass=password
	private static List<String> files;
	
	//Client type data
	private static int type;		//-type=(cli)|(gui)|(raw)|(file) ; If -type=file, files are added in cmdline after --, requires from to and pass to be set
	private static boolean verbose;	//-v
	private static boolean pipe;
	
	//And more constants
	private static final int TYPE_CLI = 1;
	private static final int TYPE_FILE = 2;
	private static final int TYPE_GUI = 3;
	private static final int TYPE_RAW_SMTP = 4;
	private static final int TYPE_DEFAULT = TYPE_RAW_SMTP;
	//private static final int TYPE_DEFAULT = TYPE_GUI;
	
	private static final Pattern COLON = Pattern.compile(":");
	
	private static final String[] AUTH_METHODS = {
		"PLAIN",
		"XOAUTH2"
	};
	
	private static String authMethod;
	
	static{
		System.setProperty("line.separator", CRLF);		//Since SMTP uses <CRLF>, set this as the default.
		System.setProperty("java.net.preferIPv6Addresses", "true");
		
		stdOut = System.out;	//So I don't have to keep typing System.out
		stdErr = System.err;	
		
		verbose = false;
		pipe = false;
		type = TYPE_DEFAULT;
		
		socket = null;
		out = null;
		in = null;
		
		uName = null;
		recipients = null;
		authData = null;
		pass = null;
		files = null;
		
		authMethod = AUTH_METHODS[1];
		
		stdIn = new BufferedReader(new InputStreamReader(System.in));
		
		//A shutdown hook to make sure the socket is closed when the program exits
		Runtime.getRuntime().addShutdownHook(new Thread(() -> {
			try{
				if(in != null)
					in.close();
				
				if(out != null)
					out.close();
				
				if(socket != null && !socket.isClosed())
					socket.close();
			}
			catch(IOException e){}
		}, "Shutdown-Cleanup"));
	}
	
	public static void main(String[] args) throws Exception{
		splitCommandLine(args);
		
		switch(type){
			case TYPE_CLI:
				cliClient();
				break;
			case TYPE_GUI:
				guiClient();
				break;
			case TYPE_RAW_SMTP:
				rawClient();
				break;
			case TYPE_FILE:
				fileClient();
				break;
		}
	}
	
	//Tries to create a TCP socket to url:port with SSL
	private static Socket openConnection(String url, int port){
		InetAddress[] addresses = new InetAddress[0];
		Socket sock = null;
		
		if(verbose)
			stdOut.print("Resolving hostname " + url + "...");
		
		try{
			addresses = InetAddress.getAllByName(url);
		}
		catch(UnknownHostException e){
			stdErr.println("\nCannot resolve hostname: " + url);
			System.exit(ERR_NO_HOST);
		}
		
		if(verbose){
			stdOut.println("done\n");
			
			stdOut.println("Resolved addresses:");
			
			for(InetAddress addr : addresses)
				stdOut.println("  " + inetAddressToHostString(addr));
			
			stdOut.println();
		}
		
		SocketFactory factory = SSLSocketFactory.getDefault();
		
		for(InetAddress addr : addresses){
			String addrString = inetAddressToHostString(addr);
			
			if(verbose)
				stdOut.print("Connecting to " + addrString + ":" + port + "...");
			
			try{
				sock = factory.createSocket(addr, port);
				sock.setKeepAlive(true);
				
				logVerbose("done\n");
				
				in = new BufferedReader(new InputStreamReader(sock.getInputStream()));
				out = new PrintWriter(sock.getOutputStream(), true);
				
				return sock;
			}
			catch(IOException e){
				logVerbose("failed");
				
				sock = null;
			}
		}
		
		stdErr.println("Failed to connect!");
		System.exit(ERR_CONNECTION_FAILED);
		
		return null;
	}
	
	//Formats printing of IP addresses
	private static String inetAddressToHostString(InetAddress addr){
		return (addr instanceof Inet4Address) ? addr.getHostAddress() : "[" + shortenInet6Addr(addr.getHostAddress()) + "]";
	}
	
	private static String shortenInet6Addr(String ip){
		if(ip == null)
			return ip;
		
		String[] hextets = COLON.split(ip);
		
		if(hextets.length != 8)
			return ip;
		
		int curStart = -1, maxStart = -1, curLength = 0, maxLength = 0;
		
		for(int i = 0; i < hextets.length; i++){
			String h = hextets[i];
			
			if(h.equals("0")){
				if(curStart == -1)
					curStart = i;
				else
					curLength++;
			}
			else if(curStart != -1){
				if(curLength > maxLength){
					maxLength = curLength;
					maxStart = curStart;
				}
				
				curStart = -1;
				curLength = 0;
			}
		}
		
		if(curLength > maxLength){
			maxLength = curLength;
			maxStart = curStart;
		}
		
		if(maxLength == 0)
			maxStart = -1;
		
		StringBuilder sb = new StringBuilder(39);
		
		for(int i = 0; i < hextets.length; i++){
			if(i == maxStart){
				int l = sb.length();
				
				if(l > 0)
					if(sb.charAt(l - 1) == ':')
						sb.deleteCharAt(l - 1);
				
				sb.append("::");
				i += maxLength;
			}
			else{
				sb.append(hextets[i]);
				
				if(i < hextets.length - 1)
					sb.append(':');
			}
		}
		
		return sb.toString();
	}
	
	private static void getXOAuth2Data(){
		
	}
	
	//Gets username and password (if necessary) and encodes them in Base64 for PLAIN authentication
	private static void buildPlainAuthData(){
		if(uName == null)
			getUser();
		
		if(pass == null)
			getPass();
		
		int bufSize = 2;
		
		byte[] uBytes, pBytes;
		
		uBytes = uName.getBytes();
		
		//AVOID using String objects...
		CharBuffer cbuf = CharBuffer.wrap(pass);
		ByteBuffer  buf = Charset.defaultCharset().encode(cbuf);
		
		pBytes = Arrays.copyOfRange(buf.array(), buf.position(), buf.limit());
		
		Arrays.fill(cbuf.array(), '\0');
		Arrays.fill(buf.array(), (byte)0);
		
		cbuf = null;
		buf = null;
		
		bufSize += (uBytes.length + pBytes.length);
		
		byte[] auth = new byte[bufSize];
		
		int pos = 0;
		
		auth[pos++] = 0;
		
		for(byte b : uBytes)
			auth[pos++] = b;
		
		auth[pos++] = 0;
		
		for(byte b : pBytes)
			auth[pos++] = b;
		
		Arrays.fill(pBytes, (byte)0);
		
		pBytes = new byte[0];
		
		authData = Base64.getEncoder().encode(auth);
		
		Arrays.fill(auth, (byte)0);
	}
	
	private static void buildAuthData(){
		switch(authMethod){
			case "PLAIN":
				buildPlainAuthData();
				break;
			case "XOAUTH2":
				getXOAuth2Data();
				break;
		}
	}
	
	//Actually submit the authentication and return the response.
	private static Response submitAuthentication(){
		Response resp;
		
		try{
			out.printf("AUTH %s ", authMethod);
			out.flush();
			
			OutputStream os = socket.getOutputStream();
			os.write(authData);
			os.flush();
			
			out.println();
			
			resp = Response.getResponse(in);
			Arrays.fill(authData, (byte)0);
			authData = new byte[0];
		}
		catch(IOException e){
			resp = null;
		}
		
		if(resp == null){
			stdErr.println("An IO error occurred!");
			System.exit(ERR_IO_ERROR);
		}
		
		if(resp.getResponseCodeType() == 5){
			stdErr.println("Authentication failed!");
			System.exit(ERR_AUTH_FAILED);
		}
		
		return resp;
	}
	
	//Prompt for the username
	private static void getUser(){
		stdOut.print("Enter gmail address (Ex: username@gmail.com): ");
		
		try{
			uName = stdIn.readLine().trim();
		}
		catch(IOException e){
			stdErr.println("An IO error occurred...");
			System.exit(ERR_IO_ERROR);
		}
	}
	
	//Prompt for the password (through GUI, if necessary)
	private static void getPass(){
		if(pipe){
			if(headless()){
				stdErr.println("Since stdin is used as an input, an alternate means of obtaining authentication information is needed (JOptionPane); however, the current environment does not support GUI.");
				
				if(verbose)
					stdErr.println("(java.awt.GraphicsEnvironment.isHeadless() == true)");
				
				System.exit(ERR_NO_GUI);
			}
			
			char[] buf;
			int ret;
			
			JPanel panel = new JPanel();
			panel.add(new JLabel("Enter your password: "));
			JPasswordField passField = new JPasswordField(15);
			
			passField.addHierarchyListener(GUIClient.PASS_JOP_LISTENER);	//See GUIClient.java for what this is...
			
			panel.add(passField);
			String[] opts = {"OK", "Cancel"};
			
			do{
				ret = JOptionPane.showOptionDialog(null, panel, "Password",
												   JOptionPane.NO_OPTION,
												   JOptionPane.PLAIN_MESSAGE,
												   null, opts, opts[0]);
												   
				if(ret == 1){
					stdOut.println("No password provided, cannot authenticate.");
					System.exit(ERR_AUTH_FAILED);
					return;
				}
				else
					buf = passField.getPassword();
			}while(buf.length == 0);
			
			pass = buf;
		}
		else
			pass = System.console().readPassword("Enter gmail password: ");
	}
	
	//Parse the command line...
	private static void splitCommandLine(String[] args){
		boolean typeSet = false;		//-type option has been set
		boolean fileSet = false;		//-type=file option has been set
		boolean fileListStart = false;	//-- has occurred (no further options allowed)
		boolean userSet = false;		//-from option has been set
		boolean rcptSet = false;		//-to option has been set
		boolean passSet = false;		//-pass option has been set
		boolean authSet = false;		//-auth option has been set
		
		for(String arg : args){
			String originalArg = arg;	//Save the value of the argument (for error messages)
			
			if(!fileListStart && arg.startsWith("-")){	//If it is an option & we're still reading options
				arg = arg.substring(1);
				
				if(arg.equalsIgnoreCase("v")){
					verbose = true;
				}
				else if(arg.startsWith("from=")){
					if(userSet){
						stdErr.println("Repeated argument: " + originalArg);
						System.exit(ERR_BAD_COMMAND_LINE);
					}
					
					arg = splitKeyValue(arg);
					
					if(arg == null){
						stdErr.println("Invalid argument: " + originalArg);
						
						System.exit(ERR_BAD_COMMAND_LINE);
					}
					
					uName = arg;
					userSet = true;
				}
				else if(arg.startsWith("to=")){
					if(rcptSet){
						stdErr.println("Repeated argument: " + originalArg);
						System.exit(ERR_BAD_COMMAND_LINE);
					}
					
					arg = splitKeyValue(arg);
					
					if(arg == null){
						stdErr.println("Invalid argument: " + originalArg);
						
						System.exit(ERR_BAD_COMMAND_LINE);
					}
					
					recipients = arg.split(";");
					rcptSet = true;
				}
				else if(arg.startsWith("pass=")){
					if(passSet){
						stdErr.println("Repeated argument: " + originalArg);
						System.exit(ERR_BAD_COMMAND_LINE);
					}
					
					arg = splitKeyValue(arg);
					
					if(arg == null){
						stdErr.println("Invalid argument: " + originalArg);
						
						System.exit(ERR_BAD_COMMAND_LINE);
					}
					
					pass = arg.toCharArray();
					passSet = true;
				}
				else if(arg.equalsIgnoreCase("askpass")){
					if(passSet){
						stdErr.println("Repeated argument: " + originalArg);
						System.exit(ERR_BAD_COMMAND_LINE);
					}
					
					passSet = true;
				}
				else if(arg.equalsIgnoreCase("auth")){
					if(authSet){
						stdErr.println("Repeated argument: " + originalArg);
						System.exit(ERR_BAD_COMMAND_LINE);
					}
					
					arg = splitKeyValue(arg);
					
					if(arg == null){
						stdErr.println("Invalid argument: " + originalArg);
						
						System.exit(ERR_BAD_COMMAND_LINE);
					}
					
					String am = arg;
					
					boolean valid = false;
					
					for(String m : AUTH_METHODS)
						if(am.equals(m)){
							valid = true;
							break;
						}
						
					if(!valid){
						stdErr.println("Invalid auth method: " + am);
						
						System.exit(ERR_INVALID_OPT);
					}
					
					authMethod = am;
					authSet = true;
				}
				else if(arg.equalsIgnoreCase("help")){
					help();
				}
				else if(arg.equalsIgnoreCase("list-auth")){
					stdOut.println("Valid methods:\n");
					
					for(String m : AUTH_METHODS)
						stdOut.println("\t" + m);
					
					System.exit(ERR_OK);
				}
				else if(arg.startsWith("type=")){
					if(typeSet){
						stdErr.println("Repeated argument: " + originalArg);
						System.exit(ERR_BAD_COMMAND_LINE);
					}
					
					arg = splitKeyValue(arg);
					
					if(arg == null){
						stdErr.println("Invalid argument: " + originalArg);
						
						System.exit(ERR_BAD_COMMAND_LINE);
					}
					
					switch(arg){
						case "cli":
							type = TYPE_CLI;
							break;
						case "gui":
							type = TYPE_GUI;
							break;
						case "raw":
							type = TYPE_RAW_SMTP;
							break;
						case "file":
							type = TYPE_FILE;
							files = new ArrayList<String>();
							fileSet = true;
							break;
						default:
							stdErr.println("Invalid argument: " + originalArg);
							System.exit(ERR_BAD_COMMAND_LINE);
					}
					
					typeSet = true;
				}
				else if(arg.equals("-")){
					if(!(fileSet && userSet && rcptSet/* && passSet*/)){	//Switching to file list: check type=file and all needed data has been provided
						stdErr.println("Command line missing required arguments");
						System.exit(ERR_BAD_COMMAND_LINE);
					}
					
					fileListStart = true;
				}
				else{
					stdErr.println("Nonexistant option: " + originalArg);
					System.exit(ERR_INVALID_OPT);
				}
			}
			else if(fileListStart){
				files.add(arg);
			}
		}
		
		if(!typeSet)
			help();
		
		if(fileSet && files.size() == 0){	//If type=file, there MUST be files...
			stdErr.println("No files specified!");
			System.exit(ERR_BAD_COMMAND_LINE);
		}
		
		if(fileSet){
			for(String file : files){	//If type=file, check the files
				if(file.equals("-")){
					if(pipe){			//stdin cannot be used more than once
						stdErr.println("stdin specified twice!");
						System.exit(ERR_BAD_COMMAND_LINE);
					}
					else
						pipe = true;
				}
				else if(!new File(file).exists()){	//All files must exist
					stdErr.printf("File %s does not exist!\n", file);
					System.exit(404 /*ERR_FILE_NOT_FOUND*/);
				}
			}
		}
	}
	
	//Convenience method to split -key=value arguments
	private static String splitKeyValue(String kv){
		try{
			String v = kv.split("=", 2)[1];
			
			return v;
		}
		catch(Exception e){
			return null;
		}
	}
	
	//Print the help message and exit
	private static void help(){
		for(String helpLn : HELP_MSG)
			stdOut.println(helpLn);
		
		System.exit(ERR_OK);
	}
	
	private static String readLine(BufferedReader reader){
		try{
			return reader.readLine();
		}
		catch(IOException e){
			return null;
		}
	}
	
	//Verbose output only if -v set
	private static void logVerbose(String str){
		if(verbose)
			stdOut.println(str);
	}
	
	//Verbose output only if -v set
	private static void logVerbose(Object str){
		if(verbose)
			stdOut.println(str.toString());
	}
	
	//Prints a prompt, loops until user enters y or n
	private static boolean yesNo(String prompt){
		String resp;
		
		Boolean b = null;
		
		try{
			do{
				stdOut.print(prompt + "[y/n]:");
				
				resp = stdIn.readLine().trim();
				
				if(resp.equalsIgnoreCase("y"))
					b = true;
				else if(resp.equalsIgnoreCase("n"))
					b = false;
			}while(b == null);
		}
		catch(IOException e){
			return false;
		}
		
		return b;
	}
	
	//Raw client implementation
	private static void rawClient(){
		String inputString;
		
		stdOut.println("Raw SMTP client:");
		
		boolean autoEHLO, autoAUTH;
		
		autoEHLO = yesNo("Automatically send EHLO? ");
		autoAUTH = yesNo("Automatically generate & submit authentication? ");
		
		socket = openConnection(SMTP_SERVER_URL, SMTP_SERVER_PORT);
		
		Response resp = Response.getResponse(in);
		
		resp.print();
		
		if(resp.getResponseCode() != SMTP_READY){
			stdErr.println("SMTP server not ready - " + resp.getResponseCode());
			System.exit(resp.getResponseCode());
		}
		
		boolean msgBody = false;
		
		for(;;){
			if(autoEHLO){
				inputString = EHLO + "localhost";
				stdOut.println(inputString);
				autoEHLO = false;
			}
			else if(autoAUTH){
				buildAuthData();
				logVerbose(String.format("AUTH %s ****", authMethod));
				resp = submitAuthentication();
				resp.print();
				autoAUTH = false;
				if(resp.getResponseCodeType() == 5){
					stdErr.println("SMTP Error - " + resp.getResponseCode());
					System.exit(resp.getResponseCode());
				}

				continue;
			}
			else
				inputString = readLine(stdIn);
			
			
			if(inputString == null){
				stdErr.println("IO error!");
				System.exit(ERR_IO_ERROR);
			}
			
			if(msgBody && inputString.trim().equals("."))
				msgBody = false;
			
			out.println(inputString);
			
			if(msgBody)
				continue;	//Since server does not reply between DATA and ., skip getting the response
							//since it will block forever trying to read from the socket
			
			resp = Response.getResponse(in);
			resp.print();
			
			if(resp.getResponseCodeType() == 5){
				stdErr.println("SMTP Error - " + resp.getResponseCode());
				System.exit(resp.getResponseCode());
			}
			else if(resp.getResponseCode() == SMTP_CLOSING){
				if(verbose)
					stdOut.println("Transaction complete - closing...");
				
				try{
					in.close();
					out.close();
					socket.close();
				}
				catch(IOException e){
					if(verbose){
						stdErr.println("An exception occurred when closing streams/socket:");
						e.printStackTrace(stdErr);
					}
				}
				
				System.exit(ERR_OK);
			}
			else if(resp.getResponseCode() == SMTP_START_MAIL)
				msgBody = true;
		}
	}
	
	//CLI client NON-implementation
	private static void cliClient(){
		stdOut.println("CLI not implemented yet");
		System.exit(-1 /*ERR_NOT_IMPLEMENTED*/);
	}
	
	//GUI client implementation
	private static void guiClient(){
		if(headless()){		//MUST be able to use GUIs in the first place...
			stdErr.println("Current environment does not support GUI!");
			
			if(verbose)
				stdErr.println("(java.awt.GraphicsEnvironment.isHeadless() == true)");
			
			System.exit(ERR_NO_GUI);
		}
		
		//Container class for message data
		GUIClient.ClientData data = new GUIClient.ClientData();
		
		//Initialize it to what we have already...
		data.uName = uName;
		data.pass = pass;
		data.recipients = recipients;
		
		//...and use the GUI to get the rest.
		data = GUIClient.getMessage(data);
		
		if(!data.isDone()){
			stdOut.println("Form incomplete. Mail not sent.");
			System.exit(ERR_OK);
		}
		
		List<String> message = data.messageLines;
		
		//Retrieve client data
		uName = data.uName;
		pass = data.pass;
		recipients = data.recipients;
		
		String subject = data.subject;
		
		subject = (subject != null) ? subject : "";
		
		socket = openConnection(SMTP_SERVER_URL, SMTP_SERVER_PORT);
		Response resp = Response.getResponse(in);
		
		if(resp.getResponseCode() != SMTP_READY){
			stdErr.println("SMTP server not ready - " + resp.getResponseCode());
			System.exit(resp.getResponseCode());
		}
		
		logVerbose(EHLO + "localhost");
		out.println(EHLO + "localhost");
		
		resp = Response.getResponse(in);
		logVerbose(resp);
		
		if(resp.getResponseCode() != SMTP_OK){
			stdErr.println("SMTP Error - " + resp.getResponseCode());
			System.exit(resp.getResponseCode());
		}
		
		buildAuthData();
		logVerbose(String.format("AUTH %s ****", authMethod));
		resp = submitAuthentication();
		logVerbose(resp);
		
		logVerbose(String.format(MAIL, uName));
		out.println(String.format(MAIL, uName));
		resp = Response.getResponse(in);
		logVerbose(resp);
		
		if(resp.getResponseCode() != SMTP_OK){
			stdErr.println("SMTP Error - " + resp.getResponseCode());
			System.exit(resp.getResponseCode());
		}
		
		boolean atLeastOne = false;
		
		//Check the recipients
		for(String recipient : recipients){
			logVerbose(String.format(RCPT, recipient));
			out.println(String.format(RCPT, recipient));
			resp = Response.getResponse(in);
			logVerbose(resp);
			
			if(resp.getResponseCodeType() != 2){
				if(resp.getResponseCodeType() == 5){
					stdErr.println("SMTP Error - " + resp.getResponseCode());
					System.exit(resp.getResponseCode());
				}
				else
					stdErr.println("Cannot sent to " + recipient + " skipping...");
			}
			else
				atLeastOne = true;
		}
		
		if(!atLeastOne){
			stdOut.println("No valid recipient addresses given, quitting...");
			
			logVerbose(QUIT);
			out.println(QUIT);
			resp = Response.getResponse(in);
			logVerbose(resp);

			try{
				in.close();
				out.close();
				socket.close();
			}
			catch(IOException e){
				if(verbose){
					stdErr.println("An exception occurred when closing streams/socket:");
					e.printStackTrace(stdErr);
				}
			}
			
			System.exit(ERR_NO_RECIPIENTS);
		}
		
		logVerbose(DATA);
		out.println(DATA);
		resp = Response.getResponse(in);
		logVerbose(resp);
		
		if(resp.getResponseCode() != SMTP_START_MAIL){
			stdErr.println("SMTP Error - " + resp.getResponseCode());
			System.exit(resp.getResponseCode());
		}
		
		String subjectHeader = String.format("Subject:%s" + CRLF, subject);
		logVerbose(subjectHeader);
		out.println(subjectHeader);
		
		for(String line : message){
			if(line.startsWith("."))
				line = "." + line;	//Escape leading '.'
			
			logVerbose(line);
			out.println(line);
		}
		
		logVerbose(".");
		out.println(".");
		resp = Response.getResponse(in);
		logVerbose(resp);
		
		if(resp.getResponseCode() != SMTP_OK){
			stdErr.println("SMTP Error - " + resp.getResponseCode());
			System.exit(resp.getResponseCode());
		}
		
		logVerbose(QUIT);
		out.println(QUIT);
		resp = Response.getResponse(in);
		logVerbose(resp);
	}
	
	//File client
	private static void fileClient(){
		socket = openConnection(SMTP_SERVER_URL, SMTP_SERVER_PORT);
		
		BufferedReader reader;
		
		Response resp = Response.getResponse(in);
		
		if(resp.getResponseCode() != SMTP_READY){
			stdErr.println("SMTP server not ready - " + resp.getResponseCode());
			System.exit(resp.getResponseCode());
		}
		
		logVerbose(EHLO + "localhost");
		out.println(EHLO + "localhost");
		
		resp = Response.getResponse(in);
		logVerbose(resp);
		
		if(resp.getResponseCode() != SMTP_OK){
			stdErr.println("SMTP Error - " + resp.getResponseCode());
			System.exit(resp.getResponseCode());
		}
		
		buildAuthData();
		logVerbose(String.format("AUTH %s ****", authMethod));
		resp = submitAuthentication();
		logVerbose(resp);
		
		logVerbose(String.format(MAIL, uName));
		out.println(String.format(MAIL, uName));
		resp = Response.getResponse(in);
		logVerbose(resp);
		
		if(resp.getResponseCode() != SMTP_OK){
			stdErr.println("SMTP Error - " + resp.getResponseCode());
			System.exit(resp.getResponseCode());
		}
		
		boolean atLeastOne = false;
		
		for(String recipient : recipients){
			logVerbose(String.format(RCPT, recipient));
			out.println(String.format(RCPT, recipient));
			resp = Response.getResponse(in);
			logVerbose(resp);
			
			if(resp.getResponseCodeType() != 2){
				if(resp.getResponseCodeType() == 5){
					stdErr.println("SMTP Error - " + resp.getResponseCode());
					System.exit(resp.getResponseCode());
				}
				else
					stdErr.println("Cannot sent to " + recipient + " skipping...");
			}
			else
				atLeastOne = true;
		}
		
		if(!atLeastOne){
			stdOut.println("No valid recipient addresses given, quitting...");
			
			logVerbose(QUIT);
			out.println(QUIT);
			resp = Response.getResponse(in);
			logVerbose(resp);

			try{
				in.close();
				out.close();
				socket.close();
			}
			catch(IOException e){
				if(verbose){
					stdErr.println("An exception occurred when closing streams/socket:");
					e.printStackTrace(stdErr);
				}
			}
			
			System.exit(ERR_NO_RECIPIENTS);
		}
		
		logVerbose(DATA);
		out.println(DATA);
		resp = Response.getResponse(in);
		logVerbose(resp);
		
		if(resp.getResponseCode() != SMTP_START_MAIL){
			stdErr.println("SMTP Error - " + resp.getResponseCode());
			System.exit(resp.getResponseCode());
		}
		
		String line;
		
		try{
			for(String file : files){	//Iterate through the files provided
				reader = file.equals("-") ? stdIn : new BufferedReader(new FileReader(new File(file)));	//Open a Reader for the file or stdin
				
				for(;;){	//Read each line, escape it if necessary, and send it
					line = reader.readLine();
					
					if(line == null)
						break;
					
					if(line.startsWith("."))
						line = "." + line;
					
					logVerbose(line);
					out.println(line);
				}
				
				reader.close();	//Then close the reader
			}
		}
		catch(IOException e){}
		
		logVerbose(".");
		out.println(".");
		resp = Response.getResponse(in);
		logVerbose(resp);
		
		if(resp.getResponseCode() != SMTP_OK){
			stdErr.println("SMTP Error - " + resp.getResponseCode());
			System.exit(resp.getResponseCode());
		}
		
		logVerbose(QUIT);
		out.println(QUIT);
		resp = Response.getResponse(in);
		logVerbose(resp);
	}
	
	private static boolean headless(){
		return java.awt.GraphicsEnvironment.isHeadless();
	}
	
	//Print a list of options to select from and return the chosen option
	private static String selectOption(String... options){
		int n = options.length;
		
		for(;;){
			out.println("Select one of the following options: \n");
			
			for(int i = 1; i <= n; ++i)
				stdOut.printf("[%d]:\t%s\n", i, options[i - 1]);
			
			String in = readLine(stdIn);
			
			try{
				int i = Integer.valueOf(in);
				
				if(i >= 1 && i <= n){
					return options[i - 1];
				}
			}catch(Exception e){}
		}
		
	}
	
	//Container for server responses. Provides a static method to handle waiting for and reading responses.
	private static class Response implements Iterable<String>{
		private int respCode, respType;
		private List<String> respLines;
		
		private static final Pattern RESP_NONFINAL, RESP_FINAL, SP;
		
		static{
			RESP_NONFINAL = Pattern.compile("[2-5][0-5][0-9]-.*$");
			RESP_FINAL = Pattern.compile("[2-5][0-5][0-9]( .*)?$");
			SP = Pattern.compile(" ");
		}
		
		private Response(){
			respCode = respType = -1;
			respLines = new ArrayList<String>();
		}
		
		public int getResponseCode(){
			return respCode;
		}
		
		public int getResponseCodeType(){
			return respType;
		}
		
		public List<String> getResponseLines(){
			return Collections.unmodifiableList(respLines);
		}
		
		public void print(){
			stdOut.println(this);
		}
		
		@Override
		public String toString(){
			StringBuilder sb = new StringBuilder();
			
			for(String str : respLines)
				sb.append(str + "\n");
			
			return sb.toString().trim();
		}
		
		@Override
		public Iterator<String> iterator(){
			Iterator<String> itr = getResponseLines().iterator();
			
			return new Iterator<String>(){
				@Override
				public boolean hasNext(){
					return itr.hasNext();
				}
				
				@Override
				public String next(){
					return itr.next();
				}
				
				@Override
				public void remove(){}
			};
		}
		
		public static Response getResponse(BufferedReader reader){
			Response resp = new Response();
			List<String> lines = resp.respLines;
			String line;
			
			try{
				while(true){
					line = reader.readLine();
					
					if(matches(line, RESP_NONFINAL))
						lines.add(line);
					else if(matches(line, RESP_FINAL)){
						lines.add(line);
						resp.respCode = Integer.parseInt(split(SP, line, 2)[0].trim());
						resp.respType = resp.respCode / 100;
						break;
					}
					else
						throw new InvalidResponseException(line);
				}
			}
			catch(IOException e){
				stdErr.println("An IO error occurred...");
				System.exit(ERR_IO_ERROR);
			}
			
			return resp;
		}
		
		private static boolean matches(CharSequence seq, Pattern p){
			return p.matcher(seq).matches();
		}
		
		private static String[] split(Pattern p, CharSequence seq){
			return split(p, seq, 0);
		}
		
		private static String[] split(Pattern p, CharSequence seq, int limit){
			return p.split(seq, limit);
		}
	}

	//The help message...
	private static final String[] HELP_MSG = {
		"Usage:",
		"",
		"  java SMTPClient -type=raw|cli|gui [OPTIONS...]",
		"  java SMTPClient -type=file [-v] -from=<usr gmail addr> \\",
		"                  -to=<rcpt addr>[(;<rcpt addr>)*] [-pass=<usr passwd>] -- FILE...",
		"",
		"Options:",
		"",
		"  -v",
		"    Prints verbose output.",
		"",
		"  -type=raw|cli|gui|file",
		"    Sets the type of client.",
		"      -raw:   Manual SMTP interaction",
		"      -cli:   Command-Line interface (THIS HAS NOT BEEN IMPLEMENTED)",
		"      -gui:   Graphical user interface",
		"      -file:  Reads message data from files & standard input (in a similar fashion to cat).",
		"              The list of files MUST appear after --. Filename - indicates reading from",
		"              standard input. This type REQUIRES setting the user address and recipient",
		"              addresses as arguments. The user password MAY also be set. If not,",
		"              however, a GUI dialog will be used to prompt for the password.",
		"",
		"  -from=<address>",
		"    Sets the gmail address of the user. If unset, user will be prompted at runtime.",
		"",
		"  -to=<addr>[(;<addr>)*]",
		"    Sets the address(es) of the recipients. If unset, user will be prompted at runtime. For",
		"    multiple recipients, separate addresses by semicolons.",
		"",
		"  -pass=<password>",
		"    Sets user's gmail password. If unset, user will be prompted at runtime. It is",
		"    recommended that this not be used to avoid the password being stored in an",
		"    immutable String object, rather than using a clearable character array.",
		"",
		"  -auth=<AUTH method>",
		"    Sets the authentication method to use. Consult option -list-auth for valid options.",
		"",
		"  -list-auth",
		"    Prints all implemented AUTH methods, then exits.",
		"",
		"  -help",
		"    Prints this message, then exits.",
		"",
		"EXIT VALUES",
		"",
		"  0  OK",
		"  1  Could not resolve SMTP host",
		"  2  Could not connect to SMTP server",
		"  3  IO error",
		"  4  SMTP authentication failed",
		"  5  Invalid command line option",
		"  6  Bad command line",
		"  7  No valid recipient addresses",
		"  8  GUI requested but not supported by the runtime environment.",
		"  9  Authenication info file (.auth) not found.",
		"  10 Authenication info file (.auth) does not contain needed fields.",
		"",
		"  404  File not found. (for type=file)",
		"",
		"  -1  Feature not implemented",
		"",
		"  Other",
		"    SMTP error code (4xx, 5xx)",
		""
	};
}