import java.util.*;
import java.io.*;

public class XOAuth2Auth implements Auth{
	
	private static final String[] INT_CMD = {"python3", "auth.py"};
	private static final String[] EXE_CMD = {"auth.exe"};
	
	private static String[] getCmd(){
		String pathString = System.getenv("PATH");
		
		String[] dirs = pathString.split(System.getProperty("path.separator"));
		
		for(String dir : dirs){
			File d = new File(dir);
			
			if(d.listFiles((a) -> {return a.getName().contains("python3");}).length > 0)
				return INT_CMD;
		}
		
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
		
		ProcessBuilder pb = new ProcessBuilder(getCmd()).redirectError(ProcessBuilder.Redirect.INHERIT);
		
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