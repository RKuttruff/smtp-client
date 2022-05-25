
public interface Auth{
	static final int ERR_AUTH_INFO_NOT_FOUND = 0x9;
	static final int ERR_AUTH_INFO_INCOMPLETE = 0xa;
	
	public String buildAuthString(String user);
}