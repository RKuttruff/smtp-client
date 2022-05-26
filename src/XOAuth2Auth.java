import java.util.*;
import java.io.*;

public class XOAuth2Auth implements Auth{
	
	private static boolean verifyEnvFile(){
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
	
	@Override
	public String buildAuthString(String user){
		final String FMT = "user=%s\001auth=Bearer %s\001\001";
		
		String authToken = null;
		
		if(!verifyEnvFile()){
			System.err.println("Auth info file is missing needed fields. See README for info");
			System.exit(ERR_AUTH_INFO_INCOMPLETE);
		}
		
		
		
		return new String(Base64.getEncoder().encode(String.format(FMT, user, authToken).getBytes()));
	}
}