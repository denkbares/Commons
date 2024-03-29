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
package com.denkbares.progress.test;

import java.awt.*;
import java.awt.event.WindowEvent;
import java.lang.reflect.InvocationTargetException;

import javax.swing.*;

import org.junit.Assume;
import org.junit.Test;

import com.denkbares.progress.ProgressDialog;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * 
 * @author Albrecht Striffler (denkbares GmbH)
 * @created 16.04.2013
 */
public class ProgressDialogTest {

	private static class TestCancelAction extends Thread {

		boolean canceled = false;

		@Override
		public void run() {
			canceled = true;
		}

		public boolean isCanceled() {
			return canceled;
		}
	}

	@Test
	public void progressDialog() throws InterruptedException, InvocationTargetException {
		if (GraphicsEnvironment.isHeadless()) {
			Assume.assumeTrue("JVM is headless, skipping test", false);
			return;
		}

		TestCancelAction cancelAction = new TestCancelAction();
		final ProgressDialog progressDialog = new ProgressDialog("Test");
		progressDialog.setCancelAction(cancelAction);
		progressDialog.setVisible(true);
		progressDialog.updateProgress(0.1f, "Start");
		SwingUtilities.invokeAndWait(() -> assertEquals(0.1f, progressDialog.getProgress(), 0.00001));
		progressDialog.updateProgress(1f, "Done");
		progressDialog.dispatchEvent(new WindowEvent(progressDialog,
				WindowEvent.WINDOW_CLOSING));
		assertTrue(cancelAction.isCanceled());
	}
}
