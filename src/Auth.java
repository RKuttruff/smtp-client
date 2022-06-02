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
 *  Basic interface for SMTP authentication method implementations. PLAIN method is excluded for now as it is already implemented in {@Link SMTPClient}
 *  <p>
 *  Also contains AUTH-specific error codes.
 *
 *  @author     Riley Kuttruff
 *  @version    1.0
 */
public interface Auth{
    /**Error code: Needed authentication information file has not been found*/
    static final int ERR_AUTH_INFO_NOT_FOUND = 0x9;
    /**Error code: Authentication information file is missing data needed for specified method.*/
    static final int ERR_AUTH_INFO_INCOMPLETE = 0xa;
    /**Error code: Authentication child process failed.*/
    static final int ERR_AUTH_SUBPROC_FAILED = 0xb;
    
    /**
     * Produce authentication data.
     * <p>
     * Produces the data used as an argument to the AUTH SMTP command. In particular, it produces the string that follows {@code AUTH <METHOD> }.
     * 
     * @param user Username for authentication.
     * @return The bytes of the argument to the AUTH command, just the authentication data, not the method name itself.
     */
    public byte[] buildAuthString(String user);
}