/*
 * Copyright (C) 2024 denkbares GmbH, Germany
 *
 * This is free software; you can redistribute it and/or modify it under the
 * terms of the GNU Lesser General Public License as published by the Free
 * Software Foundation; either version 3 of the License, or (at your option) any
 * later version.
 *
 * This software is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public License for more
 * details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this software; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA, or see the FSF
 * site: http://www.fsf.org.
 */

package com.denkbares.utils.test;

import java.io.IOException;
import java.net.URL;

import com.denkbares.utils.ConsoleImage;

/**
 * @author Volker Belli (denkbares GmbH)
 * @created 26.08.2024
 */
public class ConsoleImageTester {
	public static void main(String[] args) throws IOException {
		var source = new URL("https://www.denkbares.com/leistungen/img/strategy.png");
//    	var source = new URL("https://www.denkbares.com/referenzen/img/krone.jpg");
		new ConsoleImage().maxWidth(80).println(source);
		new ConsoleImage().maxWidth(160).useColors(false).println(source);
	}
}
