/**
 * Copyright (c) 2018-2020 by Z-Smart Systems Ltd.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package com.zsmartsystems.openhab.firmwareprovider.internal;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardWatchEventKinds;
import java.nio.file.WatchEvent;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import org.eclipse.smarthome.core.thing.ThingTypeUID;
import org.eclipse.smarthome.core.thing.binding.firmware.Firmware;
import org.eclipse.smarthome.core.thing.binding.firmware.FirmwareBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.io.xml.StaxDriver;

/**
 * Manages the directory - reading the zipped files, creating a consolidated file directory and providing firmware
 * information for use in the framework.
 *
 * @author Chris Jackson
 *
 */
public class DirectoryReader {
    private final Logger logger = LoggerFactory.getLogger(DirectoryReader.class);

    private final static String DIRECTORY_NAME = "directory.xml";
    private final String folder;

    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
    private WatchService watchService;

    private final Map<Firmware, DirectoryFileEntry> directory = new ConcurrentHashMap<>();

    public DirectoryReader(String folder) {
        this.folder = folder;

        File directoryFile = new File(folder);
        if (!directoryFile.exists()) {
            logger.debug("Creating ZigBee persistence folder '{}'", directoryFile);
            if (!directoryFile.mkdirs()) {
                logger.error("Error while creating ZigBee persistence folder {}", directoryFile);
                return;
            }
        }

        for (final File file : directoryFile.listFiles()) {
            if (file.isFile()) {
                if (file.getName().matches(".*\\.fwp")) {
                    loadDirectory(file.getAbsolutePath());
                }
            }
        }

        startWatching();
    }

    private void loadDirectory(String filename) {
        logger.debug("Firmware reader loading '" + filename + "'");

        File directoryFile = new File(filename);
        if (!directoryFile.exists()) {
            logger.debug("Firmware file does not exist '" + filename + "'");
            return;
        }

        String directoryXml = "";
        Map<String, Integer> filenames = new ConcurrentHashMap<>();

        try {
            FileInputStream fis = new FileInputStream(filename);
            ZipInputStream zis = new ZipInputStream(new BufferedInputStream(fis));
            ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                logger.debug("Extracting: {}", entry.getName());
                if (!DIRECTORY_NAME.equals(entry.getName())) {
                    int bytesRead;
                    int totalSize = 0;
                    final byte[] buffer = new byte[1024];
                    while ((bytesRead = zis.read(buffer, 0, buffer.length)) != -1) {
                        totalSize += bytesRead;
                    }

                    filenames.put(entry.getName(), totalSize);
                    continue;
                }

                final char[] buffer = new char[1024];

                StringBuilder builder = new StringBuilder(buffer.length);
                Reader in = new InputStreamReader(zis, StandardCharsets.UTF_8);
                int charsRead;
                while ((charsRead = in.read(buffer, 0, buffer.length)) != -1) {
                    builder.append(Arrays.copyOf(buffer, charsRead));
                }
                directoryXml = builder.toString();
            }
            zis.close();
        } catch (Exception e) {
            e.printStackTrace();
        }

        if (directoryXml.isEmpty()) {
            logger.error("Firmware file {} myst contain the directory file'" + DIRECTORY_NAME + "'", filename);
            return;
        }

        XStream stream = new XStream(new StaxDriver());
        stream.setClassLoader(this.getClass().getClassLoader());
        stream.alias("Directory", HashSet.class);
        stream.alias("DirectoryEntry", DirectoryFileEntry.class);

        Collection<DirectoryFileEntry> directoryEntries;
        directoryEntries = (Collection<DirectoryFileEntry>) stream.fromXML(directoryXml);

        Map<Firmware, DirectoryFileEntry> directoryUpdates = new ConcurrentHashMap<>();
        for (DirectoryFileEntry directoryEntry : directoryEntries) {
            if (directoryEntry.getThingTypeUid().isEmpty()) {
                logger.error("Firmware directory entries must specify the 'thingTypeUid'");
                continue;
            }
            if (directoryEntry.getVersion().isEmpty()) {
                logger.error("Firmware directory entries must specify the 'version'");
                continue;
            }
            if (!filenames.containsKey(directoryEntry.getFilename())) {
                logger.error("File in firmware directory for {} does not exist: '{}'", filename,
                        directoryEntry.getFilename());
                continue;
            }

            FirmwareBuilder builder = getFirmwareBuilder(directoryEntry);

            directoryEntry.setProviderFilename(directoryFile.getName());
            directoryEntry.setFilesize(filenames.get(directoryEntry.getFilename()));
            directoryUpdates.put(builder.build(), directoryEntry);
        }

        purgeDirectory(filename);
        directory.putAll(directoryUpdates);
    }

    private void purgeDirectory(String filename) {
        logger.debug("Firmware reader purging '" + filename + "'");

        Iterator<Entry<Firmware, DirectoryFileEntry>> iterator = directory.entrySet().iterator();
        while (iterator.hasNext()) {
            Entry<Firmware, DirectoryFileEntry> dirEntry = iterator.next();
            if (dirEntry.getValue().getProviderFilename().equals(filename)) {
                logger.debug("Firmware reader purging '" + filename + "'");
                iterator.remove();
            }
        }
    }

    private FirmwareBuilder getFirmwareBuilder(DirectoryFileEntry directoryEntry) {
        FirmwareBuilder builder = FirmwareBuilder.create(new ThingTypeUID(directoryEntry.getThingTypeUid()),
                directoryEntry.getVersion());

        if (!directoryEntry.getModel().isEmpty()) {
            builder.withModel(directoryEntry.getModel());
        }
        if (!directoryEntry.getVendor().isEmpty()) {
            builder.withVendor(directoryEntry.getVendor());
        }
        if (!directoryEntry.getDescription().isEmpty()) {
            builder.withDescription(directoryEntry.getDescription());
        }
        if (!directoryEntry.getHash().isEmpty()) {
            builder.withMd5Hash(directoryEntry.getHash());
        }
        if (!directoryEntry.getPrerequisiteVersion().isEmpty()) {
            builder.withPrerequisiteVersion(directoryEntry.getHash());
        }

        return builder;
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
                        for (WatchEvent<?> event : key.pollEvents()) {
                            logger.debug("Event kind: {}. File affected: {}.", event.kind(), event.context());

                            // Update the directory if any file is deleted, or the directory is modified
                            if (StandardWatchEventKinds.ENTRY_DELETE.equals(event.kind())) {
                                purgeDirectory(event.context().toString());
                            } else {
                                loadDirectory(folder + event.context().toString());
                            }
                            logger.debug("Event kind: {}. File completed", event.kind());
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

    public Map<Firmware, DirectoryFileEntry> getDirectory() {
        return directory;
    }

    public Firmware getFirmware(DirectoryFileEntry firmwareEntry) {
        logger.debug("Getting firmware version {} for {} from {}", firmwareEntry.getVersion(),
                firmwareEntry.getThingTypeUid(), firmwareEntry.getFilename());
        Firmware firmware = null;
        try {
            FileInputStream fis = new FileInputStream(folder + firmwareEntry.getProviderFilename());
            ZipInputStream zis = new ZipInputStream(new BufferedInputStream(fis));
            ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                if (!firmwareEntry.getFilename().equals(entry.getName())) {
                    continue;
                }

                final byte[] fileBuffer = new byte[firmwareEntry.getFilesize()];

                int bytesRead;
                int totalSize = 0;
                final byte[] tempBuffer = new byte[1024];
                while ((bytesRead = zis.read(tempBuffer, 0, tempBuffer.length)) != -1) {
                    System.arraycopy(tempBuffer, 0, fileBuffer, totalSize, bytesRead);
                    totalSize += bytesRead;
                }

                if (totalSize != firmwareEntry.getFilesize()) {
                    logger.debug("Firmware did not read correct length of data from {}. {}/{}",
                            firmwareEntry.getFilename(), bytesRead, firmwareEntry.getFilesize());
                    break;
                }
                InputStream inputStream = new ByteArrayInputStream(fileBuffer);

                FirmwareBuilder builder = getFirmwareBuilder(firmwareEntry);
                firmware = builder.withInputStream(inputStream).build();
                break;
            }
            zis.close();
        } catch (Exception e) {
            logger.debug("Firmware creation exception: ", e);
        }

        return firmware;
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();

        boolean first = true;
        for (DirectoryFileEntry entry : directory.values()) {
            if (!first) {
                builder.append("\n");
            }
            builder.append(entry.getProviderFilename());
            builder.append(' ');
            builder.append(entry.getThingTypeUid());
            builder.append(' ');
            builder.append(entry.getVersion());
            builder.append(' ');
            builder.append(entry.getFilename());
        }

        return builder.toString();
    }
}
