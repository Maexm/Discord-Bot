package security;
/**
 * Security levels, used for permission comparison.
 * The higher the value, the more permissions its security level has.
 * Different security values should not be adjacent to other, already used values,
 * this allows to quickly add new security levels, if needed.
 * @author MaximOleschenko
 *
 */
public class SecurityLevel {
	
	public final static int DEV = Integer.MAX_VALUE;
	
	public final static int ADM = 10000;
	
	public final static int KREIS = 1000;
	
	public final static int STANDARD = 0;
	
	public final static int USAGE_FORBIDDEN = Integer.MIN_VALUE;

}
