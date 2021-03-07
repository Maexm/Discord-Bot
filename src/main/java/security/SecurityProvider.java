package security;

import java.util.List;

import discord4j.common.util.Snowflake;
import discord4j.core.GatewayDiscordClient;
import discord4j.core.object.entity.Member;
import discord4j.core.object.entity.Role;
import discord4j.core.object.entity.User;
import discord4j.rest.util.Permission;
import exceptions.NoPermissionException;
import system.MiddlewareConfig;

public class SecurityProvider {

	public Snowflake specialRoleId;
	private final Snowflake guildId;
	private final Snowflake appOwnerId;
	private final GatewayDiscordClient client;

	public SecurityProvider(Snowflake specialRoleId, MiddlewareConfig guildConfig){
		this.specialRoleId = specialRoleId;
		this.client = guildConfig.getGlobalDiscordProxy().getClient();
		this.guildId = guildConfig.guildId;
		this.appOwnerId = this.client.getApplicationInfo().map(info -> info.getOwnerId()).block();
	}
	/**
	 * 
	 * @param user The user, whose permissions will be checked
	 * @param required The required SecurityLevel
	 * @return True if the users SecurityLevel is equal or higher compared to the required SecurityLevel.
	 */
	public boolean hasPermission(User user, SecurityLevel required) {
		return this.getPermissionLevel(user).compareTo(required) >= 0;
	}
	/**
	 * Works just like hasPermission does, but instead of returning a boolean,
	 * a checked exception is thrown, if user has not enough permissions.
	 * @param user
	 * @param required
	 * @throws NoPermissionException
	 */
	public void checkPermission(User user, SecurityLevel required) throws NoPermissionException {
		if(this.getPermissionLevel(user).compareTo(required) < 0) {
			throw new NoPermissionException("'"+user.getUsername()+"' has no permission to do a certain action. "+required+" was required");
		}
	}
	/**
	 * Determines and returns the SecurityLevel for a user.
	 * The security level can depend on the user's role, guild, or name. 
	 * @param user
	 * @return The user's SecurityLevel, based on values from SecurityLevel.
	 */
	public SecurityLevel getPermissionLevel(User user) {
		SecurityLevel ret = SecurityLevel.FORBIDDEN;
		//	Me. The developer me
		if(user.getId().equals(this.appOwnerId)){
			return SecurityLevel.DEV;
		}

		if(this.client.getGuildById(this.guildId).map(guild -> guild.getOwnerId()).block().equals(user.getId())){
			return SecurityLevel.GUILD_ADM;
		}
		
		// Return immediately, if guildId is null
		if(this.guildId == null){
			return SecurityProvider.getHighest(ret, SecurityLevel.DEFAULT);
		}
		
		// Else check their roles
		try {
			Member userAsMember = user.asMember(this.guildId).block();
			List<Role> roles = userAsMember.getRoles().collectList().block();
			ret = SecurityProvider.getHighest(ret, SecurityLevel.DEFAULT);

			if(roles != null){
				for(Role role : roles){
					if(role.getPermissions().contains(Permission.ADMINISTRATOR)){
						ret = SecurityProvider.getHighest(ret, SecurityLevel.GUILD_ADM);
					}
					else if(this.specialRoleId != null && role.getId().equals(this.specialRoleId)){
						ret = SecurityProvider.getHighest(ret, SecurityLevel.GUILD_SPECIAL);
					}
				}
			}
		}
		catch(Exception e) {
			ret = SecurityLevel.FORBIDDEN;
		}

		return ret;
	}

	public static SecurityLevel getHighest(SecurityLevel a, SecurityLevel b){
		return a.compareTo(b) >= 0 ? a : b;
	}
}
