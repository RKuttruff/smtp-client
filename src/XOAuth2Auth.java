import java.util.*;
import java.io.*;

public class XOAuth2Auth implements Auth{
	
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
	
	@Override
	public byte[] buildAuthString(String user){
		final String FMT = "user=%s\001auth=Bearer %s\001\001";
		
		String authToken = null;
		
		ProcessBuilder pb = new ProcessBuilder("python3", "auth.py").redirectError(ProcessBuilder.Redirect.INHERIT);
		
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
	
	public XOAuth2Auth(){
		if(!verifyEnvFile()){
			System.err.println("Auth info file is missing needed fields. See README for info");
			System.exit(ERR_AUTH_INFO_INCOMPLETE);
		}
	}
}