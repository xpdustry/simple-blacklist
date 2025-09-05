### 1.7:
  - Updated plugin description
  - Updated README
  - Updated Config
  - Moved BlacklistCommand to Main
  - Removed PlayerColorCodes and PlayerLogger
  - Updated Logger
  - Optimized Strings formatting 

#### 1.6:
  - **CRITICAL**: Removed IP and subnet support, because it's not the goal of this plugin
  - Changed logger system
  - Removed VersionChecker because it's useless for this type of plugin
  - Changed command usage
  - Updated README and added a warning to use the [Anti-VPN-Service](github.com/xpdustry/Anti-VPN-Service) plugin for IP and subnet filtering
  - Added colors when displaying plugin config
  - Added command to client-side, for admin players

#### 1.5:
  - Fixed compilation issue with Mindustry sources and changed to Toxopid
  - Updated the README
  - Added handling of IP addresses and subnets 
    - **Note: for advanced filtering and features, use the [Anti-VPN-Service](github.com/xpdustry/Anti-VPN-Service) plugin instead.**
  - Added events to keep tracking of verification steps *(useful for doing statistics with another plugin)*
  - Modified command usage
  - Updated plugin.json
  - **CRITICAL**: Changed settings to an independent config file.
    - **Note: old config will be automatically migrated to the new system.**
  - Added a version checker
  - Added a case sensitive feature for nickname blacklist.

#### 1.4:
  - Added option to ban IP
  - Fixed display bug when more regex than nickname list

#### 1.3:
  - Fixed bug when kick player, is not realy kicked (verifications continue after kick)
  - Added a prevention of multiple connection of a client

#### 1.2:
  - Fixed bug with count of regex not saving
  - Fixed bug with regex don't removing to list (without restart)
  - Fixed bug when kicking the player, an empty account is created
  - Added more client verifications (before nickname check) to avoid empty accounts
  - Added setting to ignore admins
  - Changed help argument
  - **CRITICAL**: Changed settings fields 
    - **Please check your actual configuration before update, because this will be reset some blacklist settings**
  - Command with no argument now print all informations of plugin
  - Changed usage of command
  - Fixed bug with compilation

#### 1.1:
  - Added regex blacklist
  - Improved help argument
  - Added a priority setting between nicknames and regex blacklist
  - Added possibility to disable a list
  - Improved blacklist display


#### 1.0:
 - Added nicknames blacklist
 - Added count of kicking
 - Added listener priority setting
 - Added working mode setting between ban or kick player
 - Added command to control the blacklist
 - Added a custom message when kick player
