This bundle implements a FirmwareProvider to support openHAB or Eclipse SmartHome bindings that provide firmware updates.

To use this, create a folder ```firmware``` in the ```userdata``` folder. Inside this folder, place the firmware files. Each firmware is a ZIP format file, renamed with the extension ```FWP```. Each ```FWP``` file contains a directory XML file, and the binary files to be loaded to the device. No subdirectories can be contained in the ```FWP``` file.

The directory file is an XML file called ```directory.xml```. This has the following format -:

``` xml
<Directory>
    <DirectoryEntry>
        <filename>ncp-uart-rts-cts-use-with-serial-uart-btl-5.8.0.ebl</filename>
        <thingTypeUid>zigbee:coordinator_ember</thingTypeUid>
        <version>6.5.4.0</version>
        <vendor>Silabs</vendor>
        <model>EM358x</model>
        <description></description>
        <hash></hash>
        <prerequisiteVersion></prerequisiteVersion>
    </DirectoryEntry>
</Directory>
```

The directory must contain at least the ```filename```, ```thingTypeUid``` and ```version``` tags. ```vendor``` and ```model``` may be used by the binding to further distinguish different firmware versions when different types of the same thing type are supported.

The ```prerequisiteVersion``` indicates that the firmware can only be installed on a device on which a firmware with version greater or equal to the prerequisite version is already installed.  The ```hash``` is an MD5 hash of the binary file.

The directory file supports multiple ```<DirectoryEntry>``` tags, so a file can contain multiple sets of firmware. This allows firmware to be distributed as a set (for example providing all devices from a manufacturer in a single file) or individually. 

Firmware files can be updated while the system is operating, however firmware will only be made available to bindings if the above requirements are met.
