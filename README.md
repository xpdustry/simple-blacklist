# Simple Blacklist
Prohibits player nicknames containing a specific text or matching a regex.

To control the blacklist, run the ``blacklist`` command. *(Use ``blacklist help`` to see command usage)* <br>
The command is also available for admin players by using ``/blacklist``.

> [!IMPORTANT]
>
> This plugin only works around player nicknames. <br>
> For IP/Subnet filtering, please use the [Anti-VPN-Service](https://github.com/xpdustry/Anti-VPN-Service) plugin instead.


### Features
* **Nickname normalizer**: Removes color and glyphs from player name during checks.
* **Nickname blacklist**: Verify if the player name contains any element of the list.
* **Regex blacklist**: Verify if the player name matches with any pattern of the list.
* **Working mode**: Can be kick the player, ban the uuid or ban the IP.
* **Ignore admins**: Whether to ignore admin players.
* **Kick Message**: Custom kick message when rejecting the player.


### Building
Pre-build releases can be found [here](https://github.com/Xpdustry/simple-blacklist/releases). <br>
But if you want to compile the plugin yourself, just run ``./gradlew build``. 
The jar file will be located in the project directory and named ``simple-blacklist.jar``.


### Installing
Simply place the output jar from the step above in your server's `config/mods` directory and restart the server. <br>
List your currently installed plugins by running the `mods` command.
