/**
 * The JPerseusSDR library is free software; you can redistribute 
 * it and/or modify it under the terms of the GNU Lesser General Public 
 * License as published by the Free Software Foundation; either version 
 * 3.0 of the License, or (at your option) any later version.
 * 
 * The JPerseusSDR library is distributed in the hope that it will
 * be useful, but WITHOUT ANY WARRANTY; without even the implied warranty 
 * of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  
 * See the GNU Lesser General Public License for more details.
 * 
 * You should have received a copy of the GNU Lesser General Public
 * License along with the Perseus SDR Library; 
 * if not, see 'http://www.gnu.org/licenses/'.
 * 
 * This library is assumed to work under J2SE &gt;= 7.
 * NOTE: It is assumed that the Perseus USB drivers are already installed on your system. The Perseus USB drivers 
 *       are available in the distribution CD-ROM which comes with the Perseus receiver.
 * 
 * This library is inspired from the Microtelecom Software Defined Radio Developer Kit (SDRDK) and is subject to licensing
 *  conditions contained in the document "SDRDK-Licensing-Agreement-v20.docx" (available under the folder "/resources")
 *  Microtelecom SDRDK is (c) 2007-2013, Microtelecom s.r.l. - Pavia di Udine, Italy
 *  Author : Nicolangelo PALERMO - IV3NWV
 * 
 * This library is also inspired from the Perseus Software Defined Radio Control Library for Linux (libperseus-sdr),
 *  originally written by Nicolangelo PALERMO and maintained by Andrea MONTEFUSCO. This library can be found at :
 *  - https://github.com/Microtelecom/libperseus-sdr
 * 
 * The JPerseus library, source code and documentation is published under the GNU Lesser General Public Licence v3
 *  (available under the folder "/resources").
 * 
 * Copyright 2017, Mehdi DHAKOUANI
 */
package perseus.utils;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.Map;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;

/**
 * Class used to transfer embedded resources on the computer.
 *
 * @author Mehdi DHAKOUANI
 * @version 1.0.0
 * 
 */
public class XResource {
    
    private final Map<String, File> resources = new HashMap<>();
    
    private static XResource INSTANCE = null;
    private ClassLoader classLoader = null;
    
    private XResource() {
        this.classLoader = this.getClass().getClassLoader();
        this.resources.put("perseus48k24v31.sbs", null);
        this.resources.put("perseus95k24v31.sbs", null);
        this.resources.put("perseus96k24v31.sbs", null);
        this.resources.put("perseus125k24v21.sbs", null);
        this.resources.put("perseus192k24v31.sbs", null);
        this.resources.put("perseus250k24v21.sbs", null);
        this.resources.put("perseus500k24v21.sbs", null);
        this.resources.put("perseus1m24v21.sbs", null);
        this.resources.put("perseus2m24v21.sbs", null);
        this.resources.put("perseusds16v10.sbs", null);
        this.resources.put("perseus_default_ds16v21.hex", null);
        this.resources.put("perseus_wb_ds16v21.hex", null);
    }
    
    /**
     * Singleton class allowing for access to all resources
     * @return the unique instance of XResource
     * @throws java.io.FileNotFoundException when one of the resources called by this singleton could not be found.
     * @throws java.io.IOException when one of the resources called by this singleton could not be accessed.
     */
    public static XResource GetInstance() throws FileNotFoundException, IOException {
        if (INSTANCE == null) {
            INSTANCE = new XResource();
            INSTANCE.userSetup();
        }
        return INSTANCE;
    }
    
    /**
     * Copy all resources in a folder defined by @Definitions.RESOURCES_PATH.
     * If a resource already exist, compare its size and copy it only if the size if different.
     */
    private void userSetup() throws FileNotFoundException, IOException {
        File resourcesPath = new File(Definitions.RESOURCES_PATH);
        for (String resource : this.resources.keySet()) {
            URI uri_source = this.find(resource);
            File f_dest = new File(resourcesPath + File.separator + resource);
            byte[] buffer = IOUtils.toByteArray(uri_source);
            if (f_dest.exists() && f_dest.length() != buffer.length) {
                f_dest.delete();
            }
            FileUtils.writeByteArrayToFile(f_dest, buffer);
            this.resources.put(resource, f_dest);
        }
    }
    
    /**
     * Find a resource stored in the JAR of this library
     * @param resourceFilename the filename of the resource we look for.
     * @return the URI of the resource
     */
    private URI find(String resourceFilename) throws FileNotFoundException, IOException {
        URI ret = null;
        if (resourceFilename != null && !resourceFilename.trim().isEmpty()) {
            try {
                ret = this.classLoader.getResource(resourceFilename).toURI();
            } catch (URISyntaxException ex) {
                throw new FileNotFoundException("JPerseus resource '"+resourceFilename+"' cannot be found in the JAR file.");
            }
        } else {
            throw new IOException("JPerseus requested resource is invalid.");
        }
        return ret;
    }
    
    /**
     * Gets the file associated with a resource name.
     *
     * @param resourceName the name of the resource to retrieve.
     * @return the file associated with that resource's name.
     */
    public File getResourceFile(String resourceName) {
        return this.resources.get(resourceName);
    }
    
}
