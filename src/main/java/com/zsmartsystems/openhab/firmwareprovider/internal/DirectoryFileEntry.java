/**
 * Copyright (c) 2018-2021 by Z-Smart Systems Ltd.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package com.zsmartsystems.openhab.firmwareprovider.internal;

/**
 *
 * @author Chris Jackson
 *
 */
public class DirectoryFileEntry {
    private String providerFilename;

    private String filename;
    private Integer filesize;
    private String thingTypeUid;
    private String version;
    private String prerequisiteVersion;
    private String model;
    private String vendor;
    private String description;
    private String hash;

    /**
     * @return the providerFilename
     */
    public String getProviderFilename() {
        return providerFilename;
    }

    /**
     * @param providerFilename the providerFilename to set
     */
    public void setProviderFilename(String providerFilename) {
        this.providerFilename = providerFilename;
    }

    /**
     * @return the filename
     */
    public String getFilename() {
        return filename == null ? "" : filename;
    }

    /**
     * @return the filesize
     */
    public Integer getFilesize() {
        return filesize;
    }

    /**
     * @param filesize the filesize to set
     */
    public void setFilesize(Integer filesize) {
        this.filesize = filesize;
    }

    /**
     * @return the thingTypeUid
     */
    public String getThingTypeUid() {
        return thingTypeUid == null ? "" : thingTypeUid;
    }

    /**
     * @return the version
     */
    public String getVersion() {
        return version == null ? "" : version;
    }

    /**
     * @return the model
     */
    public String getModel() {
        return model == null ? "" : model;
    }

    /**
     * @return the vendor
     */
    public String getVendor() {
        return vendor == null ? "" : vendor;
    }

    /**
     * @return the description
     */
    public String getDescription() {
        return description == null ? "" : description;
    }

    /**
     * @return the hash
     */
    public String getHash() {
        return hash == null ? "" : hash;
    }

    /**
     * @return the prerequisiteVersion
     */
    public String getPrerequisiteVersion() {
        return prerequisiteVersion == null ? "" : prerequisiteVersion;
    }
}
