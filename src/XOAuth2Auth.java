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

import java.util.*;
import java.io.*;

/**
 * Class to handle XOAUTH2 SMTP authentication method. Sets up and executes a Python script ({@code auth.py}) as a subprocess, which returns an OAuth access token to the parent.
 *
 *  @author     Riley Kuttruff
 *  @version    1.0
 */
public class XOAuth2Auth implements Auth, ExitCodes{
    
    /**Subprocess command for when a Python interpreter is available in the PATH.*/
    private static final String[] INT_CMD = {"python3", "auth.py"};
    /**Subprocess command for when a Python interpreter is not available in the PATH.*/
    private static final String[] EXE_CMD = {"auth.exe"};
	
	/**Use auth state file in local directory*/
    private boolean localState;
    
    /**
     * Determines the command to use for XOAUTH2 subprocess.
     * <p>
     * Scans through the PATH environment variable to see if a Python interpreter is present in the path. If one is found, it is used with {@code auth.py}. Otherwise, 
     * a precompiled binary (<b>WINDOWS ONLY!!!</b>) is used. If it does not exist in the current directory, it will be extracted from the jar archive, executed, and 
     * deleted on program exit.
     * 
     * @return And array to use with {@link ProcessBuilder} for the XOAUTH2 subprocess.
     */
    private static String[] getCmd(){
        try{
            String pathString = System.getenv("PATH");
        
            String[] dirs = pathString.split(System.getProperty("path.separator"));
            
            for(String dir : dirs){
                File d = new File(dir);
                
                if(d.listFiles((a) -> {return a.getName().contains("python3");}).length > 0)
                    return INT_CMD;
            }
            
        }
        catch(Exception e){}
        
        if(!(new File("auth.exe").exists() || new File("auth").exists())){
            try{
                File exe = new File("auth.exe");
                
                exe.deleteOnExit();
                
                InputStream is = XOAuth2Auth.class.getResource("auth.exe").openStream();
                OutputStream os = new FileOutputStream(new File("auth.exe"));
                
                byte[] buf = new byte[4096];
                int len;
                
                while((len = is.read(buf)) != -1)
                    os.write(buf, 0, len);
                
                is.close();
                os.close();
            }
            catch(IOException e){
                System.err.println("An IO error occurred trying to extract the auth module");
                System.exit(SMTPClient.ERR_IO_ERROR);
            }
        }
            
        return EXE_CMD;
        
    }
    
    /**
     * Validates the OAuth environment file.
     * <p>
     * Checks the {@code .env} file containing needed fields for OAuth requests (Client ID, Client Secret) is present and contains the needed data.
     * 
     * @return {@code true} if the file exists and contains the needed data, {@code false} if the file exists and does not contain the needed data
     */
    private boolean verifyEnvFile(){
        File envfile = new File(".env");
        
        if(!envfile.exists()){
            System.err.println(".env file not found. See README for info");
            System.exit(ERR_AUTH_INFO_NOT_FOUND);
        }
        
        boolean id = false, sec = false;
        
        try{
            Scanner s = new Scanner(envfile);
            
            while(s.hasNextLine()){
                String line = s.nextLine();
                
                id = id || line.startsWith("CLIENT_ID");
                sec = sec || line.startsWith("CLIENT_SECRET");
            }
            
            s.close();
        }
        catch(Exception e) {}
        
        return id && sec;
    }
    
    /**
     * Creates the argument to the AUTH XOAUTH2 command.
     * <p>
     * Executes the Python subprocess and processes the returned access token to return the base64 encoded argument to the AUTH command.
     * 
     * @return Argument to AUTH command
     */
    @Override
    public byte[] buildAuthString(String user){
        final String FMT = "user=%s\001auth=Bearer %s\001\001";
        
        String authToken = null;
        
        ProcessBuilder pb = new ProcessBuilder(getCmd()).redirectError(ProcessBuilder.Redirect.INHERIT);
		pb.environment().put("username", user);
		
		if(localState)
			pb.environment().put("uselocalstate", "");
        
        try{
            Process p = pb.start();
            
            InputStream os = p.getInputStream();
            
            do{
                try{
                    p.waitFor();
                }
                catch(InterruptedException e){}
            }while(p.isAlive());
            
            int ret = p.exitValue();
            
            if(ret != 0){
                System.err.printf("An error occurred with the subprocess (0x%x)\n", ret);
                System.exit(ERR_AUTH_SUBPROC_FAILED);
            }
            
            authToken = new Scanner(os).nextLine();
            
            return Base64.getEncoder().encode(String.format(FMT, user, authToken).getBytes());
        }
        catch(Exception e){
            System.err.println("An error occurred with the subprocess.");
            System.exit(ERR_AUTH_SUBPROC_FAILED);
            
            return null;
        }
    }
    
    /** Default constructor, just validates the {@code .env} file */
    public XOAuth2Auth(boolean local){
        if(!verifyEnvFile()){
            System.err.println("Auth info file is missing needed fields. See README for info");
            System.exit(ERR_AUTH_INFO_INCOMPLETE);
        }
		
		localState = local;
    }
}