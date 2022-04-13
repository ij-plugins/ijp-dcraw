/*
 * IJ-Plugins DCRaw
 * Copyright (C) 2008-2022 Jarek Sacha
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
 * Latest release available at https://github.com/ij-plugins/ijp-dcraw
 */

package ij_plugins.dcraw;

import org.junit.Test;

import javax.swing.*;

import static org.junit.Assert.assertNotNull;

public class IJPUtilsTest {

    @Test
    public void loadIcon() {
        final ImageIcon icon = IJPUtils.loadIcon();
        assertNotNull(icon);
    }
}