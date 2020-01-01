/**
 * Copyright (c) 2018-2020 by Z-Smart Systems Ltd.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package com.zsmartsystems.openhab.firmwareprovider.internal;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardWatchEventKinds;
import java.nio.file.WatchEvent;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.eclipse.smarthome.core.thing.ThingTypeUID;
import org.eclipse.smarthome.core.thing.binding.firmware.Firmware;
import org.eclipse.smarthome.core.thing.binding.firmware.FirmwareBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.io.xml.StaxDriver;

/**
 *
 * @author Chris Jackson
 *
 */
public class DirectoryReader {
    private final Logger logger = LoggerFactory.getLogger(DirectoryReader.class);

    private final static String DIRECTORY_NAME = "directory.xml";
    private final String folder;
    private final String directoryFilename;

    ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
    private WatchService watchService;

    private Map<Firmware, String> directory = Collections.emptyMap();

    public DirectoryReader(String folder) {
        this.folder = folder;
        this.directoryFilename = folder + DIRECTORY_NAME;

        File directoryFile = new File(folder);
        if (!directoryFile.exists()) {
            logger.debug("Creating ZigBee persistence folder '{}'", directoryFile);
            if (!directoryFile.mkdirs()) {
                logger.error("Error while creating ZigBee persistence folder {}", directoryFile);
                return;
            }
        }

        loadDirectory();
        startWatching();
    }

    private void loadDirectory() {
        logger.debug("Firmware directory reader loading '" + directoryFilename + "'");

        File directoryFile = new File(directoryFilename);
        if (!directoryFile.exists()) {
            logger.debug("Firmware directory file does not exist '" + directoryFilename + "'");
            synchronized (this) {
                directory = Collections.emptyMap();
            }
            return;
        }

        XStream stream = new XStream(new StaxDriver());
        stream.setClassLoader(this.getClass().getClassLoader());
        stream.alias("Directory", HashSet.class);
        stream.alias("DirectoryEntry", DirectoryFileEntry.class);

        Collection<DirectoryFileEntry> directoryEntries;
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(new FileInputStream(directoryFile), "UTF-8"))) {
            directoryEntries = (Collection<DirectoryFileEntry>) stream.fromXML(reader);
        } catch (Exception e) {
            logger.error("Error reading firmware directory: ", e);
            directoryEntries = Collections.emptySet();
        }

        Map<Firmware, String> directory = new ConcurrentHashMap<>();
        for (DirectoryFileEntry directoryEntry : directoryEntries) {
            if (directoryEntry.getThingTypeUid().isEmpty()) {
                logger.error("Firmware directory entries must specify the 'thingTypeUid'");
            }
            if (directoryEntry.getVersion().isEmpty()) {
                logger.error("Firmware directory entries must specify the 'version'");
            }
            final File file = new File(folder + directoryEntry.getFilename());
            if (!file.exists()) {
                logger.error("File in firmware directory does not exist: '{}'", file);
                continue;
            }

            FirmwareBuilder builder = FirmwareBuilder.create(new ThingTypeUID(directoryEntry.getThingTypeUid()),
                    directoryEntry.getVersion());

            if (!directoryEntry.getModel().isEmpty()) {
                builder.withModel("EM358x");
            }
            if (!directoryEntry.getVendor().isEmpty()) {
                builder.withVendor("Silabs").build();
            }

            directory.put(builder.build(), directoryEntry.getFilename());
        }

        synchronized (this) {
            this.directory = Collections.unmodifiableMap(directory);
        }
    }

    private void startWatching() {
        scheduler.schedule(new Runnable() {
            @Override
            public void run() {
                logger.debug("Firmware watcher starting watching '{}'", folder);
                try {
                    watchService = FileSystems.getDefault().newWatchService();
                    Path path = Paths.get(folder);
                    path.register(watchService, StandardWatchEventKinds.ENTRY_CREATE,
                            StandardWatchEventKinds.ENTRY_DELETE, StandardWatchEventKinds.ENTRY_MODIFY);

                    WatchKey key;
                    while ((key = watchService.take()) != null) {
                        boolean update = false;
                        for (WatchEvent<?> event : key.pollEvents()) {
                            logger.debug("Event kind: {}. File affected: {}.", event.kind(), event.context());

                            // Update the directory if any file is deleted, or the directory is modified
                            if (DIRECTORY_NAME.equals(event.context())
                                    || StandardWatchEventKinds.ENTRY_DELETE.equals(event.kind())) {
                                update = true;
                            }
                        }
                        if (update) {
                            loadDirectory();
                        }
                        key.reset();
                    }
                } catch (IOException | InterruptedException e) {
                    logger.debug("Firmware watcher exception: ", e);
                }

                logger.debug("Firmware watcher stopped watching '{}'", folder);
            }
        }, 5, TimeUnit.SECONDS);

    }

    public void shutdown() {
        try {
            watchService.close();
            scheduler.shutdown();
        } catch (IOException e) {
            logger.debug("Firmware watcher shutdown exception: ", e);
        }
    }

    public Map<Firmware, String> getDirectory() {
        return directory;
    }
}
