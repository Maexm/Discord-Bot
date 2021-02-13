package security;
/**
 * Security levels, used for permission comparison.
 * The higher the value, the more permissions its security level has.
 * @author MaximOleschenko
 *
 */
public enum SecurityLevel {
	FORBIDDEN,
	DEFAULT,
	GUILD_SPECIAL,
	GUILD_ADM,
	ADM,
	DEV
}
