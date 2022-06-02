/*Copyright (C) 2022  Riley Kuttruff
*
*   This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or
*   (at your option) any later version.
*
*   This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General
*   Public License for more details.
*
*   You should have received a copy of the GNU General Public License along with this program.  If not, see <https://www.gnu.org/licenses/>.
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

/**
 *  Main class for the simple SMTP client.
 *
 *  @author     Riley Kuttruff
 *  @version    1.0
 */
public class SMTPClient{
    /*      Basic constants     */
    /**SMTP uses Carriage return-line feed.*/
    private static final String CRLF = "\r\n";
    /**URL for GMail SMTP server*/
    private static final String SMTP_SERVER_URL = "smtp.gmail.com";
    /**GMail SMTP port number*/
    private static final int SMTP_SERVER_PORT = 465;
    
    /*      SMTP return code constants      */
    /*      Defined in RFC 5321 4.2.2-3     */
    /**RFC 5321 Section 4.2.2: System status, or system help reply*/
    private static final int SMTP_STATUS = 211;
    /**RFC 5321 Section 4.2.2: Help message (Information on how to use the receiver or the meaning of a particular non-standard command; this reply is useful only to the human user)*/
    private static final int SMTP_HELP = 214;
    /**RFC 5321 Section 4.2.2: &lt;domain&gt; Service ready*/
    private static final int SMTP_READY = 220;
    /**RFC 5321 Section 4.2.2: &lt;domain&gt; Service closing transmission channel*/
    private static final int SMTP_CLOSING = 221;
    /**RFC 5321 Section 4.2.2: Requested mail action okay, completed*/
    private static final int SMTP_OK = 250;
    /**RFC 5321 Section 4.2.2: User not local; will forward to &lt;forward-path&gt; (See Section 3.4)*/
    private static final int SMTP_WILL_FORWARD = 251;
    /**RFC 5321 Section 4.2.2: Cannot VRFY user, but will accept message and attempt delivery (See Section 3.5.3)*/
    private static final int SMTP_CANNOT_VERIFY_WILL_TRY = 252;
    /**RFC 5321 Section 4.2.2: Start mail input; end with &lt;CRLF&gt;.&lt;CRLF&gt;*/
    private static final int SMTP_START_MAIL = 354;
    /**RFC 5321 Section 4.2.2: &lt;domain&gt; Service not available, closing transmission channel (This may be a reply to any command if the service knows it must shut down)*/
    private static final int SMTP_UNAVAILABLE_CONNECTION_PROBLEM = 421;
    /**RFC 5321 Section 4.2.2: Requested mail action not taken: mailbox unavailable (e.g.,mailbox busy or temporarily blocked for policy reasons)*/
    private static final int SMTP_MAILBOX_UNAVAILABLE = 450;
    /**RFC 5321 Section 4.2.2: Requested action aborted: local error in processing*/
    private static final int SMTP_ABORTED_LOCAL_ERROR = 451;
    /**RFC 5321 Section 4.2.2: Requested action not taken: insufficient system storage*/
    private static final int SMTP_TOO_MANY = 452;
    /**RFC 5321 Section 4.2.2: Server unable to accommodate parameters*/
    private static final int SMTP_UNABLE_TO_ACCOMMODATE_PARAMETERS = 455;
    /**RFC 5321 Section 4.2.2: Syntax error, command unrecognized (This may include errors such as command line too long)*/
    private static final int SMTP_SYNTAX_ERROR = 500;
    /**RFC 5321 Section 4.2.2: Syntax error in parameters or arguments*/
    private static final int SMTP_SYNTAX_ERROR_PARAMETERS_OR_ARGUMENTS = 501;
    /**RFC 5321 Section 4.2.2: Command not implemented (see Section 4.2.4)*/
    private static final int SMTP_COMMAND_NOT_IMPLEMENTED = 502;
    /**RFC 5321 Section 4.2.2: Bad sequence of commands*/
    private static final int SMTP_BAD_SEQUENCE = 503;
    /**RFC 5321 Section 4.2.2: Command parameter not implemented*/
    private static final int SMTP_PARAMETER_NOT_IMPLEMENTED = 504;
    /**RFC 5321 Section 4.2.2: User not local; please try &lt;forward-path&gt; (See Section 3.4)*/
    private static final int SMTP_INVALID_ADDRESS = 551;
    /**RFC 5321 Section 4.2.2: Requested mail action aborted: exceeded storage allocation*/
    private static final int SMTP_EXCEEDED_STORAGE_ALLOCATION = 552;
    /**RFC 5321 Section 4.2.2: Requested action not taken: mailbox name not allowed (e.g.,mailbox syntax incorrect)*/
    private static final int SMTP_MAILBOX_NAME_INVALID = 553;
    /**RFC 5321 Section 4.2.2: Transaction failed (Or, in the case of a connection-opening response, "No SMTP service here")*/
    private static final int SMTP_TRANSACTION_FAILED = 554;
    /**RFC 5321 Section 4.2.2: MAIL FROM/RCPT TO parameters not recognized or not implemented*/
    private static final int SMTP_MAILFROM_RCPTTO_NOT_RECOGNIZED = 555;
    
    /*      Commands required for minimum implementation of RFC 5321 4.5.1 (Except VRFY which is not supported by gmail anyway)     */
    /**Extended session initiation command*/
    private static final String EHLO = "EHLO ";
    /**Basic session initiation command*/
    private static final String HELO = "HELO ";
    /**Sender address command*/
    private static final String MAIL = "MAIL FROM:<%s>";
    /**Recipient(s) command*/
    private static final String RCPT = "RCPT TO:<%s>";
    /**Command to begin senting mail data*/
    private static final String DATA = "DATA";
    /**Abort current transaction and discard all stored data*/
    private static final String RSET = "RSET";
    /**No-op. Does nothing*/
    private static final String NOOP = "NOOP";
    /**Closes the communication channel*/
    private static final String QUIT = "QUIT";
    
    /*      I/O variables       */
    /**Connection to SMTP server*/
    private static Socket socket;
    /**Output stream to the server*/
    private static PrintWriter out;
    /**Standard output*/
    private static PrintStream stdOut;
    /**Standard error*/
    private static PrintStream stdErr;
    /**Input stream from the server*/
    private static BufferedReader in;
    /**Standard input*/
    private static BufferedReader stdIn;
    
    /*      Session data        */
    /**Sender's username*/
    private static String uName;        //-from=addr
    /**Recipient's username(s)*/
    private static String[] recipients; //-to=addr(;addr)*
    /**Argument to the AUTH command*/
    private static byte[] authData;
    /**User password*/
    private static char[] pass;         //-pass=password
    /**Input file paths to file client*/
    private static List<String> files;
    /**Authenication method to be used*/
    private static String authMethod;
    /**Valid authenication methods (both implemented and accepted)*/
    private static String[] validAuthMethods;
    
    /*      Client type data        */
    /**Type of client in use*/
    private static int type;        //-type=(cli)|(gui)|(raw)|(file) ; If -type=file, files are added in cmdline after --, requires from to and pass to be set
    /**Provide verbose output*/
    private static boolean verbose; //-v
    /**Is standard input is being used for mail data input*/
    private static boolean pipe;
    
    /*      And more constants      */
    /**Command-Line interface client <u><b>(NOT IMPLEMENTED)</u></b>*/
    private static final int TYPE_CLI = 1;
    /**Client to read from file(s) and/or standard input*/
    private static final int TYPE_FILE = 2;
    /**GUI-based client*/
    private static final int TYPE_GUI = 3;
    /**Raw interaction with SMTP server*/
    private static final int TYPE_RAW_SMTP = 4;
    /**Default client: RAW*/
    private static final int TYPE_DEFAULT = TYPE_RAW_SMTP;
    
    /**@hidden*/
    //Used for pretty-printing IPv6 addresses
    private static final Pattern COLON = Pattern.compile(":");
    
    /**Currently implemented authenication methods*/
    private static final String[] AUTH_METHODS = {
        "PLAIN",
        "XOAUTH2"
    };
    
    static{
        System.setProperty("line.separator", CRLF);     //Since SMTP uses <CRLF>, set this as the default.
        System.setProperty("java.net.preferIPv6Addresses", "true");
        
        stdOut = System.out;    //So I don't have to keep typing System.out
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
        
        authMethod = null;
        validAuthMethods = null;
        
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
    
    /**@hidden*/
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
    /**
     * Opens TCP socket to a given server.
     * <p>
     * Opens a TCP socket with SSL to {@code url:port}. First attempts to resolve the server hostname into 
     * IP addresses, then tries to open the connection with each. If no IP addresses can be resolved or all 
     * resolved IP addresses fail to connect, the program will exit.
     * 
     * @param url Server domain name
     * @param port Port number to connect on
     * @return {@link Socket} object to the remote server
     */
    private static Socket openConnection(String url, int port){
        InetAddress[] addresses = new InetAddress[0];
        Socket sock = null;
        
        if(verbose)
            stdOut.print("Resolving hostname " + url + "...");
        
		//Try to resolve hostname
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
        
		//Loop through the resolved addresses and try to connect on the given port
		//Return on first success
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
    /**@hidden*/
    private static String inetAddressToHostString(InetAddress addr){
        return (addr instanceof Inet4Address) ? addr.getHostAddress() : "[" + shortenInet6Addr(addr.getHostAddress()) + "]";
    }
    
    //Shortens in IPv6 address into a more readable format
    /**@hidden*/
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
    
    /**
     * Builds XOAUTH2 argument string.
     * <p>
     * Stores in {@link #authData}.
     */
    private static void getXOAuth2Data(){
        Auth auth = new XOAuth2Auth();
        
        if(uName == null)
            getUser();
        
        authData = auth.buildAuthString(uName);
    }
    
    //Gets username and password (if necessary) and encodes them in Base64 for PLAIN authentication
    /**
     * Builds PLAIN argument string.
     * <p>
     * Stores in {@link #authData}.
     */
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
    
    /**
     * Builds the authenication method argument for the selected method.
     */
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
    /**
     * Submits the authenication command and handles the results
     * <p>
     * Program will exit upon I/O error or response code of type {@code 5xx} (Permanent Negative Completion)
     * 
     * @return A {@link Response} object containing the server's reply to the command.
     */
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
            stdErr.println(resp);
            System.exit(ERR_AUTH_FAILED);
        }
        
        return resp;
    }
    
    //Prompt for the username
    /**
     * Gets the sender username from the user.
     * <p>
     * Stores in {@link #uName}.
     */
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
    /**
     * Gets the sender username from the user.
     * <p>
     * If standard input is used as an input to the file client, uses GUI if possible.
     * Stores in {@link #uName}.
     */
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
            
            passField.addHierarchyListener(GUIClient.PASS_JOP_LISTENER);    //See GUIClient.java for what this is...
            
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
    
    /**
     * Determine valid authenication methods.
     * <p>
     * Parses server response to {@code EHLO} command to determine the server's accepted authenication 
     * methods. These are intersected with the methods implemented by the program. If there are no valid 
     * methods, the program exits. If there is only one valid method, it is selected for use automatically. 
     * 
     * @param r {@link Response} object read from server after {@code EHLO} command is sent.
     */
    private static void getValidAuths(Response r){
        String methods = null;
        
        for(String l : r)
            if(l.contains("AUTH"))
                methods = l.split("AUTH", 2)[1].trim();
            
        Set<String> implemented = new HashSet<>(Arrays.asList(AUTH_METHODS));
        Set<String> accepted = new HashSet<>(Arrays.asList(methods.split(" ")));
        
        implemented.retainAll(accepted);
        
        validAuthMethods = implemented.toArray(new String[implemented.size()]);
        
        if(validAuthMethods.length == 0){
            System.err.println("No accepted AUTH methods by this server have been implemented");
            System.exit(ERR_NO_VALID_AUTHS);
        }
        else if(validAuthMethods.length == 1)
            authMethod = validAuthMethods[0];
    }
    
    /**
     * Promts the user for authenication method to use.
     * <p>
     * Tries to use GUI if standard input is used as an input to the file client.
     */
    private static void getAuthMethod(){
        if(pipe && headless()){
            stdErr.println("Since stdin is used as an input, an alternate means of obtaining authentication information is needed (JOptionPane); however, the current environment does not support GUI.");
            
            if(verbose)
                stdErr.println("(java.awt.GraphicsEnvironment.isHeadless() == true)");
            
            System.exit(ERR_NO_GUI);
        }
        else if(!pipe)
            authMethod = selectOption("AUTH Method", validAuthMethods);
        else{
            int choice = JOptionPane.showOptionDialog(
                null,
                "Select a valid AUTH method",
                "Select AUTH",
                JOptionPane.DEFAULT_OPTION,
                JOptionPane.QUESTION_MESSAGE,
                null,
                validAuthMethods,
                validAuthMethods[0]
            );
            
            if(choice == JOptionPane.CLOSED_OPTION){
                System.err.println("No auth method chosen, closing");
                System.exit(ERR_OK);
            }
            
            authMethod = validAuthMethods[choice];
        }
    }
    
    /**
     * Parses and validates the command line.
     * 
     * @param args Command line argument array provided to the {@code main} method
     */
    private static void splitCommandLine(String[] args){
        boolean typeSet = false;        //-type option has been set
        boolean fileSet = false;        //-type=file option has been set
        boolean fileListStart = false;  //-- has occurred (no further options allowed)
        boolean userSet = false;        //-from option has been set
        boolean rcptSet = false;        //-to option has been set
        boolean passSet = false;        //-pass option has been set
        boolean authSet = false;        //-auth option has been set
        
        for(String arg : args){
            String originalArg = arg;   //Save the value of the argument (for error messages)
            
            if(!fileListStart && arg.startsWith("-")){  //If it is an option & we're still reading options
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
                else if(arg.startsWith("auth=")){
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
                        
                        System.exit(ERR_BAD_COMMAND_LINE);
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
                    if(!(fileSet && userSet && rcptSet/* && passSet*/)){    //Switching to file list: check type=file and all needed data has been provided
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
        
        if(fileSet && files.size() == 0){   //If type=file, there MUST be files...
            stdErr.println("No files specified!");
            System.exit(ERR_BAD_COMMAND_LINE);
        }
        
        if(fileSet){
            for(String file : files){   //If type=file, check the files
                if(file.equals("-")){
                    if(pipe){           //stdin cannot be used more than once
                        stdErr.println("stdin specified twice!");
                        System.exit(ERR_BAD_COMMAND_LINE);
                    }
                    else
                        pipe = true;
                }
                else if(!new File(file).exists()){  //All files must exist
                    stdErr.printf("File %s does not exist!\n", file);
                    System.exit(404 /*ERR_FILE_NOT_FOUND*/);
                }
            }
        }
    }
    
    //Convenience method to split -key=value arguments
    /**@hidden*/
    private static String splitKeyValue(String kv){
        try{
            String v = kv.split("=", 2)[1];
            
            return v;
        }
        catch(Exception e){
            return null;
        }
    }
    
    /**
     * Print the help message and exit
     */
    private static void help(){
        for(String helpLn : HELP_MSG)
            stdOut.println(helpLn);
        
        System.exit(ERR_OK);
    }
    
	//Convenience method
    /**@hidden*/
    private static String readLine(BufferedReader reader){
        try{
            return reader.readLine();
        }
        catch(IOException e){
            return null;
        }
    }
    
    //Verbose output only if -v set
    /**@hidden*/
    private static void logVerbose(String str){
        if(verbose)
            stdOut.println(str);
    }
    
    //Verbose output only if -v set
    /**@hidden*/
    private static void logVerbose(Object str){
        if(verbose)
            stdOut.println(str.toString());
    }
    
    //Prints a prompt, loops until user enters y or n
    /**
     * Promts the user with a yes/no choice.
     * 
     * Returns choice as {@code boolean}.
     * 
     * @param prompt Custom prompt text
     * @return The user's choice as a {@code boolean} value.
     * @see #selectOption
     */
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
            }while(b == null);	//Loop until user enters a valid input
        }
        catch(IOException e){
            return false;
        }
        
        return b;
    }
    
    //Raw client implementation
    /**
     * Runs the raw SMTP interaction client
     */
    private static void rawClient(){
        String inputString;
        
        stdOut.println("Raw SMTP client:");
        
        boolean autoEHLO, autoAUTH;
        
		//Prompt user whether they want EHLO/AUTH commands to be sent
        autoEHLO = yesNo("Automatically send EHLO? ");
        autoAUTH = autoEHLO ? yesNo("Automatically generate & submit authentication? ") : false;	//Don't auto sent AUTH if EHLO hasn't been sent & processed
        
		//Open the connection...
        socket = openConnection(SMTP_SERVER_URL, SMTP_SERVER_PORT);
        
        Response resp = Response.getResponse(in);
        
        resp.print();
        
        if(resp.getResponseCode() != SMTP_READY){
            stdErr.println("SMTP server not ready - " + resp.getResponseCode());
            System.exit(resp.getResponseCode());
        }
        
        boolean msgBody = false;
        
        for(;;){	//Main loop
            if(autoEHLO){		//Send EHLO if user wants
                inputString = EHLO + "localhost";
                stdOut.println(inputString);
                autoEHLO = false;
            }
            else if(autoAUTH){
                getValidAuths(resp);
        
                if(authMethod == null)
                    getAuthMethod();
                
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
            
			//In the message body, terminate message body (resume waiting for responses) on single '.'
            if(msgBody && inputString.trim().equals("."))
                msgBody = false;
            
            out.println(inputString);	//Send what the user types
            
			//If we're in the message body, loop instead of waiting for the server to respond
            if(msgBody)
                continue;   //Since server does not reply between DATA and ., skip getting the response
                            //since it will block forever trying to read from the socket
            
            resp = Response.getResponse(in);
            resp.print();
            
            if(resp.getResponseCodeType() == 5){	//Exit if an error occurs
                stdErr.println("SMTP Error - " + resp.getResponseCode());
                System.exit(resp.getResponseCode());
            }
            else if(resp.getResponseCode() == SMTP_CLOSING){	//When we're done
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
            else if(resp.getResponseCode() == SMTP_START_MAIL)	//Start message body when the server is ready
                msgBody = true;
        }
    }
    
    //CLI client NON-implementation
    /**@hidden*/
    private static void cliClient(){
        stdOut.println("CLI not implemented yet");
        System.exit(-1 /*ERR_NOT_IMPLEMENTED*/);
    }
    
    //GUI client implementation
    /**
     * Runs the GUI SMTP client
     */
    private static void guiClient(){
        if(headless()){     //MUST be able to use GUIs in the first place...
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
        
		//Open the connection...
        socket = openConnection(SMTP_SERVER_URL, SMTP_SERVER_PORT);
        Response resp = Response.getResponse(in);
        
        if(resp.getResponseCode() != SMTP_READY){	//...if we're ready...
            stdErr.println("SMTP server not ready - " + resp.getResponseCode());
            System.exit(resp.getResponseCode());
        }
        
		//Send & process EHLO
        logVerbose(EHLO + "localhost");
        out.println(EHLO + "localhost");
        
        resp = Response.getResponse(in);
        logVerbose(resp);
        
        if(resp.getResponseCode() != SMTP_OK){
            stdErr.println("SMTP Error - " + resp.getResponseCode());
            System.exit(resp.getResponseCode());
        }
        
        getValidAuths(resp);
        
        if(authMethod == null)
            getAuthMethod();
        
		//Authenication
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
        
        //Check the recipients
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
        
		//All good! Time to enter the message
        logVerbose(DATA);
        out.println(DATA);
        resp = Response.getResponse(in);
        logVerbose(resp);
        
        if(resp.getResponseCode() != SMTP_START_MAIL){
            stdErr.println("SMTP Error - " + resp.getResponseCode());
            System.exit(resp.getResponseCode());
        }
        
		//Server's ready for the message. Write the header (subject line)
        String subjectHeader = String.format("Subject:%s" + CRLF, subject);
        logVerbose(subjectHeader);
        out.println(subjectHeader);
        
		//Message body
        for(String line : message){
            if(line.startsWith("."))
                line = "." + line;  //Escape leading '.'
            
            logVerbose(line);
            out.println(line);
        }
        
		//Terminate the message...
        logVerbose(".");
        out.println(".");
        resp = Response.getResponse(in);
        logVerbose(resp);
        
        if(resp.getResponseCode() != SMTP_OK){
            stdErr.println("SMTP Error - " + resp.getResponseCode());
            System.exit(resp.getResponseCode());
        }
        
		//...and close the connection
        logVerbose(QUIT);
        out.println(QUIT);
        resp = Response.getResponse(in);
        logVerbose(resp);
    }
    
    //File client
    /**
     * Runs the SMTP file based input client
     */
    private static void fileClient(){
		//Open the connection...
        socket = openConnection(SMTP_SERVER_URL, SMTP_SERVER_PORT);
        
        BufferedReader reader;
        
        Response resp = Response.getResponse(in);
        
        if(resp.getResponseCode() != SMTP_READY){
            stdErr.println("SMTP server not ready - " + resp.getResponseCode());
            System.exit(resp.getResponseCode());
        }
        
		//Send & process EHLO
        logVerbose(EHLO + "localhost");
        out.println(EHLO + "localhost");
        
        resp = Response.getResponse(in);
        logVerbose(resp);
        
        if(resp.getResponseCode() != SMTP_OK){
            stdErr.println("SMTP Error - " + resp.getResponseCode());
            System.exit(resp.getResponseCode());
        }
        
        getValidAuths(resp);
        
        if(authMethod == null)
            getAuthMethod();
        
		//Authenication
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
        
		//Check the recipients
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
        
		//All good! Time to enter the message
        logVerbose(DATA);
        out.println(DATA);
        resp = Response.getResponse(in);
        logVerbose(resp);
        
        if(resp.getResponseCode() != SMTP_START_MAIL){
            stdErr.println("SMTP Error - " + resp.getResponseCode());
            System.exit(resp.getResponseCode());
        }
        
		//Server's ready for the message. Write the header (subject line)
        String line;
        
        try{
            for(String file : files){   //Iterate through the files provided
                reader = file.equals("-") ? stdIn : new BufferedReader(new FileReader(new File(file))); //Open a Reader for the file or stdin
                
                for(;;){    //Read each line, escape it if necessary, and send it
                    line = reader.readLine();
                    
                    if(line == null)
                        break;
                    
                    if(line.startsWith("."))
                        line = "." + line;
                    
                    logVerbose(line);
                    out.println(line);
                }
                
                reader.close(); //Then close the reader
            }
        }
        catch(IOException e){}
        
		//Terminate the message...
        logVerbose(".");
        out.println(".");
        resp = Response.getResponse(in);
        logVerbose(resp);
        
        if(resp.getResponseCode() != SMTP_OK){
            stdErr.println("SMTP Error - " + resp.getResponseCode());
            System.exit(resp.getResponseCode());
        }
        
		//...and close the connection
        logVerbose(QUIT);
        out.println(QUIT);
        resp = Response.getResponse(in);
        logVerbose(resp);
    }
    
    /**@hidden*/
    private static boolean headless(){
        return java.awt.GraphicsEnvironment.isHeadless();
    }
    
    //Print a list of options to select from and return the chosen option
    /**
     * Promts the user with a multiple choices.
     * 
     * @param what What the user is chposing
     * @param options The user's options
     * @return The user's choice
     * @see #yesNo
     */
    private static String selectOption(String what, String... options){
        int n = options.length;
        
        for(;;){
            stdOut.println("Select one of the following options for: " + what + "\n");
            
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
    
    /**
     * Container for server responses. 
     * <p>
     * Provides a static method to handle waiting for and reading responses.
     * 
     *  @author     Riley Kuttruff
     *  @version    1.0
     */
    private static class Response implements Iterable<String>{
        /**@hidden*/
        private int respCode, respType;
        /**@hidden*/
        private List<String> respLines;
        
        /**@hidden*/
        private static final Pattern RESP_NONFINAL, RESP_FINAL, SP;
        
        static{
            RESP_NONFINAL = Pattern.compile("[2-5][0-5][0-9]-.*$");
            RESP_FINAL = Pattern.compile("[2-5][0-5][0-9]( .*)?$");
            SP = Pattern.compile(" ");
        }
        
        /**
         * Default constructor.
         */
        private Response(){
            respCode = respType = -1;
            respLines = new ArrayList<String>();
        }
        
        /**
         * Returns the response code from the server.
         * 
         * @return Server's response code
         */
        public int getResponseCode(){
            return respCode;
        }
        
        /**
         * Returns the type of this response's response code.
         * 
         * @return Server's response code type (First digit)
         */
        public int getResponseCodeType(){
            return respType;
        }
        
        /**
         * Lines of the response text.
         * <p>
         * Returned as an unmodifiable list.
         * 
         * @return {@link java.util.List} object containing the response text line-by-line.
         * @see Collections##unmodifiableList(java.util.List)
         */
        public List<String> getResponseLines(){
            return Collections.unmodifiableList(respLines);
        }
        
        /**
         * Print this response to standard output.
         */
        public void print(){
            stdOut.println(this);
        }
        
        /**
         * Reconstructs the text of the server's response.
         * 
         * @return Response text
         */
        @Override
        public String toString(){
            StringBuilder sb = new StringBuilder();
            
            for(String str : respLines)
                sb.append(str + "\n");
            
            return sb.toString().trim();
        }
        
        /**
         * Provides an iterator over the lines of the response text.
         * <p>
         * Iterator cannot be used to modify the response, {@code remove()} has no effect.
         * 
         * @return Iterator over the lines of the response text.
         */
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
                public void remove(){}	//Ignore this. Response should be immutable
            };
        }
        
        /**
         * Parses server response.
         * 
         * @param reader {@link BufferedReader} object around the server's output
         * @return Parsed Response object
         */
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
        
		//Regex convenience methods
		
        /**@hidden*/
        private static boolean matches(CharSequence seq, Pattern p){
            return p.matcher(seq).matches();
        }
        
        /**@hidden*/
        private static String[] split(Pattern p, CharSequence seq){
            return split(p, seq, 0);
        }
        
        /**@hidden*/
        private static String[] split(Pattern p, CharSequence seq, int limit){
            return p.split(seq, limit);
        }
    }

    /**@hidden*/
    //The help message...
    private static final String[] HELP_MSG = {
        "Usage:",
        "",
        "  java SMTPClient -type=raw|cli|gui [OPTIONS...]",
        "  java SMTPClient -type=file [-v] [-from=<usr gmail addr>] \\",
        "                  [-to=<rcpt addr>[(;<rcpt addr>)*]] [-pass=<usr passwd>] -- FILE...",
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
        "  9  Authentication info file (.env) not found.",
        "  10 Authentication info file (.env) does not contain needed fields.",
        "  11 Authentication subprocess failure.",
        "  12 No valid authentication methods.",
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