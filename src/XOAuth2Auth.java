import java.util.*;
import java.io.*;

public class XOAuth2Auth implements Auth{
	
	
	@Override
	public String buildAuthString(String user){
		final String FMT = "user=%s\001auth=Bearer %s\001\001";
		
		String authToken = null;
		
		return new String(Base64.getEncoder().encode(String.format(FMT, user, authToken).getBytes()));
	}
}