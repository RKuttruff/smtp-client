public class InvalidResponseException extends SMTPException{
	public InvalidResponseException(){
		this(null);
	}
	
	public InvalidResponseException(String msg){
		super(msg);
	}
}