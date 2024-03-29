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
 * SMTP exception for a malformed response from the server.
 *
 *  @author     Riley Kuttruff
 *  @version    1.0
 */
public class InvalidResponseException extends SMTPException{
    public InvalidResponseException(){
        this(null);
    }
    
    public InvalidResponseException(String msg){
        super(msg);
    }
}