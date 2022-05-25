import java.util.*;
import java.io.*;

public class XOAuth2Auth implements Auth{
	
	private static final String CID = "CLIENT_ID",
								CSEC = "CLIENT_SECRET";
	
	private static Map<String, String> state;
	
	private static final File STATE_FILE;
	
	static{
		state = new HashMap<>();
		STATE_FILE = new File(".auth");
		
		readState();
	}
	
	private static void readState(){
		try{
			Scanner s = new Scanner(STATE_FILE, "UTF-8");
			
			while(s.hasNextLine()){
				String line = s.nextLine();
				
				if(!(line.length() == 0 || line.charAt(0) == '#')){
					String[] kv = line.split("=" , 2);
					
					if(kv.length > 1)
						state.put(kv[0], kv[1]);
					else
						state.put(kv[0], "");
				}
				
			}
			
			s.close();
			
			if(!(state.containsKey(CID) && state.containsKey(CSEC))){
				System.err.println("Auth info file is missing needed fields. See README for info");
				System.exit(ERR_AUTH_INFO_INCOMPLETE);
			}
		}
		catch(FileNotFoundException e){
			System.err.println("Auth info file not found. Attempting to create a blank one. See README for info");
			
			try{
				PrintStream ps = new PrintStream(STATE_FILE, "UTF-8");
				
				ps.println("### TOKENS ###");
				ps.println();
				ps.println("#OAUTH");
				ps.println();
				ps.println("CLIENT_ID=");
				ps.println("CLIENT_SECRET=");
				ps.println();
				
				ps.close();
			}
			catch(Exception e){}
			
			System.exit(ERR_AUTH_INFO_NOT_FOUND);
		}
		catch(IOException e){
			System.err.println("Failed to read auth info file");
			System.exit(SMTPClient.ERR_IO_ERROR);
		}
		
	}
	
	private static void writeState(){
		try{
			PrintStream ps = new PrintStream(STATE_FILE, "UTF-8");
				
			ps.println("### TOKENS ###");
			ps.println();
			ps.println("#OAUTH");
			ps.println();
			
			for(Map.Entry<String, String> e : state.entrySet())
				ps.printf("%s=%s\n", e.getKey(), e.getValue());
			
			ps.close();
		}
		catch(Exception e){
			System.err.println("Failed to write auth info file");
			System.exit(SMTPClient.ERR_IO_ERROR);
		}
	}
	
	@Override
	public String authenticate(){
		return null;
	}
}