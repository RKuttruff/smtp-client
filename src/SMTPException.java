public class SMTPException extends RuntimeException{
	public SMTPException(){
		this(null);
	}
	
	public SMTPException(String msg){
		super(msg);
	}
	
	public SMTPException(String msg, Throwable cause){
		super(msg, cause);
	}
}