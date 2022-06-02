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

public interface SMTPConstants{
    /*      SMTP return code constants      */
    /*      Defined in RFC 5321 4.2.2-3     */
    /**RFC 5321 Section 4.2.2: System status, or system help reply*/
    static final int SMTP_STATUS = 211;
    /**RFC 5321 Section 4.2.2: Help message (Information on how to use the receiver or the meaning of a particular non-standard command; this reply is useful only to the human user)*/
    static final int SMTP_HELP = 214;
    /**RFC 5321 Section 4.2.2: &lt;domain&gt; Service ready*/
    static final int SMTP_READY = 220;
    /**RFC 5321 Section 4.2.2: &lt;domain&gt; Service closing transmission channel*/
    static final int SMTP_CLOSING = 221;
    /**RFC 5321 Section 4.2.2: Requested mail action okay, completed*/
    static final int SMTP_OK = 250;
    /**RFC 5321 Section 4.2.2: User not local; will forward to &lt;forward-path&gt; (See Section 3.4)*/
    static final int SMTP_WILL_FORWARD = 251;
    /**RFC 5321 Section 4.2.2: Cannot VRFY user, but will accept message and attempt delivery (See Section 3.5.3)*/
    static final int SMTP_CANNOT_VERIFY_WILL_TRY = 252;
    /**RFC 5321 Section 4.2.2: Start mail input; end with &lt;CRLF&gt;.&lt;CRLF&gt;*/
    static final int SMTP_START_MAIL = 354;
    /**RFC 5321 Section 4.2.2: &lt;domain&gt; Service not available, closing transmission channel (This may be a reply to any command if the service knows it must shut down)*/
    static final int SMTP_UNAVAILABLE_CONNECTION_PROBLEM = 421;
    /**RFC 5321 Section 4.2.2: Requested mail action not taken: mailbox unavailable (e.g.,mailbox busy or temporarily blocked for policy reasons)*/
    static final int SMTP_MAILBOX_UNAVAILABLE = 450;
    /**RFC 5321 Section 4.2.2: Requested action aborted: local error in processing*/
    static final int SMTP_ABORTED_LOCAL_ERROR = 451;
    /**RFC 5321 Section 4.2.2: Requested action not taken: insufficient system storage*/
    static final int SMTP_TOO_MANY = 452;
    /**RFC 5321 Section 4.2.2: Server unable to accommodate parameters*/
    static final int SMTP_UNABLE_TO_ACCOMMODATE_PARAMETERS = 455;
    /**RFC 5321 Section 4.2.2: Syntax error, command unrecognized (This may include errors such as command line too long)*/
    static final int SMTP_SYNTAX_ERROR = 500;
    /**RFC 5321 Section 4.2.2: Syntax error in parameters or arguments*/
    static final int SMTP_SYNTAX_ERROR_PARAMETERS_OR_ARGUMENTS = 501;
    /**RFC 5321 Section 4.2.2: Command not implemented (see Section 4.2.4)*/
    static final int SMTP_COMMAND_NOT_IMPLEMENTED = 502;
    /**RFC 5321 Section 4.2.2: Bad sequence of commands*/
    static final int SMTP_BAD_SEQUENCE = 503;
    /**RFC 5321 Section 4.2.2: Command parameter not implemented*/
    static final int SMTP_PARAMETER_NOT_IMPLEMENTED = 504;
    /**RFC 5321 Section 4.2.2: User not local; please try &lt;forward-path&gt; (See Section 3.4)*/
    static final int SMTP_INVALID_ADDRESS = 551;
    /**RFC 5321 Section 4.2.2: Requested mail action aborted: exceeded storage allocation*/
    static final int SMTP_EXCEEDED_STORAGE_ALLOCATION = 552;
    /**RFC 5321 Section 4.2.2: Requested action not taken: mailbox name not allowed (e.g.,mailbox syntax incorrect)*/
    static final int SMTP_MAILBOX_NAME_INVALID = 553;
    /**RFC 5321 Section 4.2.2: Transaction failed (Or, in the case of a connection-opening response, "No SMTP service here")*/
    static final int SMTP_TRANSACTION_FAILED = 554;
    /**RFC 5321 Section 4.2.2: MAIL FROM/RCPT TO parameters not recognized or not implemented*/
    static final int SMTP_MAILFROM_RCPTTO_NOT_RECOGNIZED = 555;
    
    /*      Commands required for minimum implementation of RFC 5321 4.5.1 (Except VRFY which is not supported by gmail anyway)     */
    /**Extended session initiation command*/
    static final String EHLO = "EHLO ";
    /**Basic session initiation command*/
    static final String HELO = "HELO ";
    /**Sender address command*/
    static final String MAIL = "MAIL FROM:<%s>";
    /**Recipient(s) command*/
    static final String RCPT = "RCPT TO:<%s>";
    /**Command to begin senting mail data*/
    static final String DATA = "DATA";
    /**Abort current transaction and discard all stored data*/
    static final String RSET = "RSET";
    /**No-op. Does nothing*/
    static final String NOOP = "NOOP";
    /**Closes the communication channel*/
    static final String QUIT = "QUIT";
}