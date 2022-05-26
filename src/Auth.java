
public interface Auth{
	static final int ERR_AUTH_INFO_NOT_FOUND = 0x9;
	static final int ERR_AUTH_INFO_INCOMPLETE = 0xa;
	static final int ERR_AUTH_SUBPROC_FAILED = 0xb;
	
	public byte[] buildAuthString(String user);
}