package security;

import discord4j.common.util.Snowflake;
import discord4j.core.object.entity.Member;
import discord4j.core.object.entity.User;
import exceptions.NoPermissionException;
import snowflakes.GuildID;
import snowflakes.RoleID;

public class SecurityProvider {
	/**
	 * 
	 * @param user The user, whose permissions will be checked
	 * @param required The required SecurityLevel
	 * @return True if the users SecurityLevel is equal or higher compared to the required SecurityLevel.
	 */
	public static boolean hasPermission(User user, int required, Snowflake ownerId) {
		return SecurityProvider.getPermissionLevel(user, ownerId) >= required;
	}
	/**
	 * Works just like hasPermission does, but instead of returning a boolean,
	 * a checked exception is thrown, if user has not enough permissions.
	 * @param user
	 * @param required
	 * @throws NoPermissionException
	 */
	public static void checkPermission(User user, int required, Snowflake ownerId) throws NoPermissionException {
		int lvl = SecurityProvider.getPermissionLevel(user, ownerId);
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
	public static int getPermissionLevel(User user, Snowflake ownerId) {
		//	Me. The developer me
		if(user.getId().equals(ownerId)) {
			return SecurityLevel.DEV;
		}
		
		// Else check their roles (users from outside the main guild are blocked)
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
