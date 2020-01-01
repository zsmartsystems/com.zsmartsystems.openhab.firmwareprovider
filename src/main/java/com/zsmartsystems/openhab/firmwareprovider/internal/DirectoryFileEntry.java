/**
 * Copyright (c) 2018-2020 by Z-Smart Systems Ltd.
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
    private String filename;
    private String thingTypeUid;
    private String version;
    private String model;
    private String vendor;

    /**
     * @return the filename
     */
    public String getFilename() {
        return filename;
    }

    /**
     * @return the thingTypeUid
     */
    public String getThingTypeUid() {
        return thingTypeUid;
    }

    /**
     * @return the version
     */
    public String getVersion() {
        return version;
    }

    /**
     * @return the model
     */
    public String getModel() {
        return model;
    }

    /**
     * @return the vendor
     */
    public String getVendor() {
        return vendor;
    }

}
