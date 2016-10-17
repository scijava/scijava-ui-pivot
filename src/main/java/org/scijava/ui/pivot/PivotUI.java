/*
 * #%L
 * SciJava UI components for Apache Pivot.
 * %%
 * Copyright (C) 2011 - 2015 Board of Regents of the University of
 * Wisconsin-Madison.
 * %%
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 * 
 * 1. Redistributions of source code must retain the above copyright notice,
 *    this list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDERS OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 * #L%
 */

package org.scijava.ui.pivot;

import java.io.File;
import java.lang.reflect.Field;
import java.util.concurrent.ExecutionException;

import org.apache.pivot.collections.Sequence;
import org.apache.pivot.wtk.Alert;
import org.apache.pivot.wtk.DesktopApplicationContext;
import org.apache.pivot.wtk.FileBrowserSheet;
import org.apache.pivot.wtk.Sheet;
import org.apache.pivot.wtk.SheetCloseListener;
import org.scijava.display.Display;
import org.scijava.log.LogService;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;
import org.scijava.thread.ThreadService;
import org.scijava.ui.AbstractUserInterface;
import org.scijava.ui.DialogPrompt;
import org.scijava.ui.DialogPrompt.MessageType;
import org.scijava.ui.DialogPrompt.OptionType;
import org.scijava.ui.SystemClipboard;
import org.scijava.ui.UserInterface;
import org.scijava.ui.viewer.DisplayWindow;
import org.scijava.widget.FileWidget;

/**
 * Apache Pivot-based user interface for ImageJ.
 * 
 * @author Curtis Rueden
 */
@Plugin(type = UserInterface.class, name = PivotUI.NAME)
public class PivotUI extends AbstractUserInterface implements Runnable {

	public static final String NAME = "pivot";

	@Parameter
	private ThreadService threadService;

	@Parameter(required = false)
	private LogService log;

	/** The Pivot application context. */
	private PivotApplication app;

	// -- UserInterface methods --

	@Override
	public PivotApplicationFrame getApplicationFrame() {
		return app.getApplicationFrame();
	}

	@Override
	public PivotToolBar getToolBar() {
		return app.getToolBar();
	}

	@Override
	public PivotStatusBar getStatusBar() {
		return app.getStatusBar();
	}

	@Override
	public SystemClipboard getSystemClipboard() {
		throw new UnsupportedOperationException("Not supported yet.");
	}

	@Override
	public DisplayWindow createDisplayWindow(final Display<?> display) {
		throw new UnsupportedOperationException("Not supported yet.");
	}

	@Override
	public DialogPrompt dialogPrompt(final String message, final String title,
		final MessageType msg, final OptionType option)
	{
		final org.apache.pivot.wtk.MessageType messageType;
		switch (msg) {
			case ERROR_MESSAGE:
				messageType = org.apache.pivot.wtk.MessageType.ERROR;
				break;
			case QUESTION_MESSAGE:
				messageType = org.apache.pivot.wtk.MessageType.QUESTION;
				break;
			case WARNING_MESSAGE:
				messageType = org.apache.pivot.wtk.MessageType.WARNING;
				break;
			default:
				messageType = org.apache.pivot.wtk.MessageType.INFO;
				break;
		}

		Alert.alert(messageType, message, title, null, getApplicationFrame(), null);
		return null;//FIXME
	}

	@Override
	public File chooseFile(String title, File file, String style) {
		final FileBrowserSheet.Mode mode = style.equals(FileWidget.SAVE_STYLE)
			? FileBrowserSheet.Mode.OPEN : style.equals(FileWidget.DIRECTORY_STYLE)
				? FileBrowserSheet.Mode.SAVE_TO : FileBrowserSheet.Mode.OPEN;

		final FileBrowserSheet fileBrowserSheet = new FileBrowserSheet();
		fileBrowserSheet.setTitle(title);

		if (mode == FileBrowserSheet.Mode.SAVE_AS) {
			fileBrowserSheet.setSelectedFile(new File(fileBrowserSheet
				.getRootDirectory(), "New File"));
		}

		fileBrowserSheet.setMode(mode);
		class FileSheetCloseListener implements SheetCloseListener {

			private Sequence<File> files;

			@Override
			public void sheetClosed(final Sheet sheet) {
				files = sheet.getResult() ? fileBrowserSheet.getSelectedFiles() : null;
				synchronized (this) {
					notify();
				}
			}
		}
		final FileSheetCloseListener l = new FileSheetCloseListener();
		synchronized (l) {
			fileBrowserSheet.open(getApplicationFrame(), l);
			try { l.wait(); } catch (InterruptedException e) {}
		}
		return l.files == null ? null : l.files.get(0);
	}

	@Override
	public void showContextMenu(final String menuRoot, final Display<?> display,
		final int x, final int y)
	{
		throw new UnsupportedOperationException("Not supported yet.");
	}

	@Override()
	public boolean requiresEDT() {
		return true;
	}

	// -- Disposable methods --

	@Override
	public void dispose() {
		// TODO: Dispose of the Pivot application context and UI elements.
	}

	// -- Runnable methods --

	@Override
	public void run() {
		DesktopApplicationContext.main(PivotApplication.class, new String[0]);
	}

	// -- Internal methods --

	@Override
	protected void createUI() {
		try {
			// call run() method in a separate thread, blocking until finished
			threadService.run(this).get();
		}
		catch (final ExecutionException exc) {
			if (log != null) log.error(exc);
		}
		catch (final InterruptedException exc) {
			if (log != null) log.error(exc);
		}
		app = getApplicationContext();
		app.setContext(getContext());
		app.initialize();
	}

	// -- Helper methods --

	/**
	 * HACK: There does not seem to be a way (at least, not obvious to me) to
	 * obtain the Application instance from the DesktopApplicationContext. The
	 * static application field stores it, but it is private. Hence, we force our
	 * way past accessibility restrictions to grab it anyway...
	 */
	private PivotApplication getApplicationContext() {
		try {
			final Field field =
				DesktopApplicationContext.class.getDeclaredField("application");
			field.setAccessible(true);
			return (PivotApplication) field.get(null);
		}
		catch (final NoSuchFieldException exc) {
			if (log != null) log.error(exc);
		}
		catch (final IllegalArgumentException exc) {
			if (log != null) log.error(exc);
		}
		catch (final IllegalAccessException exc) {
			if (log != null) log.error(exc);
		}
		return null;
	}

}
