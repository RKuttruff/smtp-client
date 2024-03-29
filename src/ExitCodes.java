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

/**
 *  Interface for exit codes for use program wide
 *  <p>
 *  <table class="striped">
 * <thead>
 *   <tr>
 *     <th>Value</th>
 *     <th>Description</th>
 *   </tr>
 * </thead>
 * <tbody>
 *   <tr>
 *     <td style="text-align:right">0</td>
 *     <td>OK</td>
 *   </tr>
 *   <tr>
 *     <td style="text-align:right">1</td>
 *     <td>Could not resolve SMTP host</td>
 *   </tr>
 *   <tr>
 *     <td style="text-align:right">2</td>
 *     <td>Could not connect to SMTP server</td>
 *   </tr>
 *   <tr>
 *     <td style="text-align:right">3</td>
 *     <td>IO error</td>
 *   </tr>
 *   <tr>
 *     <td style="text-align:right">4</td>
 *     <td>SMTP authentication failed</td>
 *   </tr>
 *   <tr>
 *     <td style="text-align:right">5</td>
 *     <td>Invalid command line option</td>
 *   </tr>
 *   <tr>
 *     <td style="text-align:right">6</td>
 *     <td>Bad command line</td>
 *   </tr>
 *   <tr>
 *     <td style="text-align:right">7</td>
 *     <td>No valid recipient addresses</td>
 *   </tr>
 *   <tr>
 *     <td style="text-align:right">8</td>
 *     <td>GUI requested but not supported by the runtime environment.</td>
 *   </tr>
 *   <tr>
 *     <td style="text-align:right">9</td>
 *     <td>Authentication info file (.env) not found.</td>
 *   </tr>
 *   <tr>
 *     <td style="text-align:right">10</td>
 *     <td>Authentication info file (.env) does not contain needed fields.</td>
 *   </tr>
 *   <tr>
 *     <td style="text-align:right">11</td>
 *     <td>Authentication subprocess failure.</td>
 *   </tr>
 *   <tr>
 *     <td style="text-align:right">12</td>
 *     <td>No valid authentication methods.</td>
 *   </tr>
 *   <tr>
 *     <td style="text-align:right">404</td>
 *     <td>File not found. (for type=file)</td>
 *   </tr>
 *   <tr>
 *     <td style="text-align:right">-1</td>
 *     <td>Feature not implemented</td>
 *   </tr>
 *   <tr>
 *     <td style="text-align:right">Other</td>
 *     <td>SMTP error code (4xx, 5xx)</td>
 *   </tr>
 * </tbody>
 * </table>
 *
 *  @author     Riley Kuttruff
 *  @version    1.0
 */
public interface ExitCodes{
    /*      Return codes        */
    /**Program exited normally, either mail was successfully sent or no mail sent but not due to errors.*/
    static final int ERR_OK = 0x0;
    /**No IP address(es) for SMTP server URL could be resolved*/
    static final int ERR_NO_HOST = 0x1;
    /**Failed to connect to SMTP server*/
    static final int ERR_CONNECTION_FAILED = 0x2;
    /**General I/O Errors*/
    static final int ERR_IO_ERROR = 0x3;
    /**Authentication failed*/
    static final int ERR_AUTH_FAILED = 0x4;
    /**Nonexistant command line option*/
    static final int ERR_INVALID_OPT = 0x5;
    /**Command line improperly configured*/
    static final int ERR_BAD_COMMAND_LINE = 0x6;
    /**No valid recipients given*/
    static final int ERR_NO_RECIPIENTS = 0x7;
    /**GUI requested/required but not available*/
    static final int ERR_NO_GUI = 0x8;
    /**Error code: Needed authentication information file has not been found*/
    static final int ERR_AUTH_INFO_NOT_FOUND = 0x9;
    /**Error code: Authentication information file is missing data needed for specified method.*/
    static final int ERR_AUTH_INFO_INCOMPLETE = 0xa;
    /**Error code: Authentication child process failed.*/
    static final int ERR_AUTH_SUBPROC_FAILED = 0xb;
    /**None of the implemented authenication methods are accepted by the server*/
    static final int ERR_NO_VALID_AUTHS = 0xc;
}