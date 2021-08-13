/*
 * IJ-Plugins
 * Copyright (C) 2002-2021 Jarek Sacha
 * Author's email: jpsacha at gmail dot com
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 *
 * Latest release available at https://github.com/ij-plugins/ijp-toolkit/
 */
package net.sf.ij_plugins.dcraw;

/**
 * Provides version number of ij-dcraw plugin bundle. Example usage:
 * <pre>
 * String version = DCRawVersion.getInstance().toString();
 * </pre>
 *
 * @author Jarek Sacha
 */
public class DCRawVersion {

    private static final DCRawVersion OUR_INSTANCE = new DCRawVersion();
    private static final String VERSION = "1.5.0";


    private DCRawVersion() {
    }

    public static DCRawVersion getInstance() {
        return OUR_INSTANCE;
    }

    @Override
    public String toString() {
        return VERSION;
    }
}
