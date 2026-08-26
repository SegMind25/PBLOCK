# PBLOCK ProGuard Rules

# Keep Device Admin receiver
-keep class com.pblock.app.PBlockDeviceAdminReceiver

# Keep helper class
-keep class com.pblock.app.PBlockHelper

# Keep BootHostsManager
-keep class com.pblock.app.BootHostsManager

# Keep all service classes
-keep class com.pblock.app.PBlockService
-keep class com.pblock.app.PBlockVpnService

# Keep BootReceiver
-keep class com.pblock.app.BootReceiver

# Keep puzzle dialogs
-keep class com.pblock.app.AlgoPuzzleDialog
-keep class com.pblock.app.UltimatePuzzleDialog

# Keep ConsoleActivity
-keep class com.pblock.app.ConsoleActivity
