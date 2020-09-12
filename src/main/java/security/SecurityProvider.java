package security;

import discord4j.core.object.entity.Member;
import discord4j.core.object.entity.User;
import discord4j.core.object.util.Snowflake;
import exceptions.NoPermissionException;
import snowflakes.GuildID;
import snowflakes.RoleID;
import snowflakes.UserID;

public class SecurityProvider {
	/**
	 * 
	 * @param user The user, whose permissions will be checked
	 * @param required The required SecurityLevel
	 * @return True if the users SecurityLevel is equal or higher compared to the required SecurityLevel.
	 */
	public static boolean hasPermission(User user, int required) {
		return SecurityProvider.getPermissionLevel(user) >= required;
	}
	/**
	 * Works just like hasPermission does, but instead of returning a boolean,
	 * a checked exception is thrown, if user has not enough permissions.
	 * @param user
	 * @param required
	 * @throws NoPermissionException
	 */
	public static void checkPermission(User user, int required) throws NoPermissionException {
		int lvl = SecurityProvider.getPermissionLevel(user);
		if(lvl < required) {
			throw new NoPermissionException("'"+user.getUsername()+"' has no permission to do a certain action. "+required+" was required, but user had "+lvl);
		}
	}
	/**
	 * Determines and returns the SecurityLevel for a user.
	 * The security level can depend on the user's role, guild, or name. 
	 * @param user
	 * @return The user's SecurityLevel, based on values from SecurityLevel.
	 */
	public static int getPermissionLevel(User user) {
		//	Me. The developer me
		if(user.getId().equals(UserID.MAXIM)) {
			return SecurityLevel.DEV;
		}
		
		// Else check their roles (users from outside the guild are blocked)
		try {
			Member userAsMember = user.asMember(GuildID.UNSER_SERVER).block();
			
			if(SecurityProvider.memberHasRole(userAsMember, RoleID.ADMIN)) {
				return SecurityLevel.ADM;
			}
			else if(SecurityProvider.memberHasRole(userAsMember, RoleID.KREIS)) {
				return SecurityLevel.KREIS;
			}
			else {
				return SecurityLevel.STANDARD;
			}
		}
		catch(Exception e) {
			System.out.println("Someone outside of this guild has attempted to use me!");
			return SecurityLevel.USAGE_FORBIDDEN;
		}		
	}
	/**
	 * Checks whether or not a user is part of a certain discord guild role.
	 * Only "Unser Server" is currently supported.
	 * @param member
	 * @param roleID
	 * @return true, if member occupies the given role, false otherwise
	 */
	public static boolean memberHasRole(Member member, Snowflake roleID) {
		return member.getRoleIds().contains(roleID);
	}

}
