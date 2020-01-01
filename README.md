This bundle implements a FirmwareProvider to support bindings that provide firmware updates.

To use this, create a folder ```firmware``` in the ```userdata``` folder. Inside this folder, place the firmware files that are to be provided to the binding, along with a directory file as described below.

The directory file is an XML file called ```directory.xml```. This has the following format -:

``` xml
<Directory>
    <DirectoryEntry>
        <filename>ncp-uart-rts-cts-use-with-serial-uart-btl-5.8.0.ebl</filename>
        <thingTypeUid>zigbee:coordinator_ember</thingTypeUid>
        <version>6.5.4.0</version>
        <vendor>Silabs</vendor>
        <model>EM358x</model>
    </DirectoryEntry>
</Directory>
```

The directory must contain at least the ```filename```, ```thingTypeUid``` and ```version``` tags. ```vendor``` and ```model``` may be used by the binding to further distinguish different firmware versions when different types of the same thing type are supported.

The directory file can be updated while the system is operating, however firmware will only be made available to bindings if the above requirements are met.
 