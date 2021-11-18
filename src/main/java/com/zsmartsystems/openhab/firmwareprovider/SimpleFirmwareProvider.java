/**
 * Copyright (c) 2018-2021 by Z-Smart Systems Ltd.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package com.zsmartsystems.openhab.firmwareprovider;

import java.io.File;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.core.OpenHAB;
import org.openhab.core.thing.Thing;
import org.openhab.core.thing.binding.firmware.Firmware;
import org.openhab.core.thing.firmware.FirmwareProvider;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.zsmartsystems.openhab.firmwareprovider.internal.DirectoryFileEntry;
import com.zsmartsystems.openhab.firmwareprovider.internal.DirectoryReader;

/**
 * A simple firmware provider.
 *
 * This allows users to create a zipped file containing an XML directory and the respective binary files into a folder
 * to be provided to the openHAB framework.
 *
 * @author Chris Jackson
 */
@Component(immediate = true, service = { FirmwareProvider.class })
public class SimpleFirmwareProvider implements FirmwareProvider {
    private Logger logger = LoggerFactory.getLogger(SimpleFirmwareProvider.class);

    private DirectoryReader directoryReader;

    @Activate
    protected void activate() {
        String folder = OpenHAB.getUserDataFolder() + File.separator + "firmware" + File.separator;
        directoryReader = new DirectoryReader(folder);
    }

    @Deactivate
    protected void deactivate() {
        directoryReader.shutdown();
        directoryReader = null;
    }

    @Override
    public @Nullable Firmware getFirmware(@NonNull Thing thing, @NonNull String version) {
        return getFirmware(thing, version, null);
    }

    @Override
    public @Nullable Firmware getFirmware(@NonNull Thing thing, @NonNull String version, @Nullable Locale locale) {
        Map<Firmware, DirectoryFileEntry> directory = directoryReader.getDirectory();
        DirectoryFileEntry foundFirmware = null;
        for (Entry<Firmware, DirectoryFileEntry> firmwareSet : directory.entrySet()) {
            Firmware firmware = firmwareSet.getKey();
            if (firmware.getThingTypeUID().equals(thing.getThingTypeUID()) && firmware.getVersion().equals(version)) {
                foundFirmware = firmwareSet.getValue();
                break;
            }
        }

        if (foundFirmware == null) {
            logger.debug("Unable to find firmware version {}", version);
            return null;
        }

        return directoryReader.getFirmware(foundFirmware);
    }

    @Override
    public @Nullable Set<@NonNull Firmware> getFirmwares(@NonNull Thing thing) {
        return getFirmwares(thing, null);
    }

    @Override
    public @Nullable Set<@NonNull Firmware> getFirmwares(@NonNull Thing thing, @Nullable Locale locale) {
        final Set<Firmware> firmwareSet = new HashSet<>();

        for (Firmware firmware : directoryReader.getDirectory().keySet()) {
            if (firmware.getThingTypeUID().equals(thing.getThingTypeUID())) {
                firmwareSet.add(firmware);
            }
        }
        return firmwareSet;
    }
}
