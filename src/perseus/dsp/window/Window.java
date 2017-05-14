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
package perseus.dsp.window;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import perseus.dsp.maths.Complex;

/**
 * Abstract class used for Filter Windowing operations
 * 
 * @author Mehdi DHAKOUANI
 * @version 1.0.1
 */
public abstract class Window {
    
    public static final Map<String, Class> WINDOWTYPES = new HashMap<>();
    public static final Map<Class, Set<String>> WINDOWPARAMS = new HashMap<>();
    
    public static void Register(Class windowClass, String[] aliases, String[] parameters) {
        for (String alias : aliases) {
            WINDOWTYPES.put(alias.trim().toLowerCase(), windowClass);
        }
        Set<String> params = new HashSet<>();
        params.addAll(Arrays.asList(parameters));
        WINDOWPARAMS.put(windowClass, params);
    }
    
    public static Class GetWindow(String alias) {
        return WINDOWTYPES.get(alias.trim().toLowerCase());
    }
    
    public abstract double[] getResult();
    public abstract double[] windowInputToDouble(double[] buffer);
    public abstract Complex[] windowInputToComplex(double[] buffer);

}
