/*
 * Copyright (C) 2019 denkbares GmbH, Germany
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

package com.denkbares.plugin;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class JPFResource implements Resource {
	private static final Logger LOGGER = LoggerFactory.getLogger(JPFResource.class);

	private final URL url;
	private final String relativePath;

	public JPFResource(URL url, String relativePath) {
		this.url = url;
		this.relativePath = relativePath;
	}

	@Override
	public InputStream getInputStream() throws IOException {
		return url.openStream();
	}

	@Override
	public String getPathName() {
		return relativePath;
	}

	@Override
	public long getSize() {
		int size = -1;
		try {
			URLConnection connection = url.openConnection();
			size = connection.getContentLength();
		}
		catch (IOException e) {
			LOGGER.warn("cannot open resource to determine content size", e);
		}
		return size;
	}

}
