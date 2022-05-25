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
		catch(IOException e){
			System.err.println("Auth info file not found. See README for info");
			System.exit(ERR_AUTH_INFO_NOT_FOUND);
		}
	}
	
	private static void writeState(){}
	
	@Override
	public String authenticate(){
		return null;
	}
}