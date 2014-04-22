/*
 * #%L
 * SciJava UI components for Apache Pivot.
 * %%
 * Copyright (C) 2011 - 2014 Board of Regents of the University of
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

package org.scijava.plugins.uis.pivot.widget;

import org.apache.pivot.wtk.BoxPane;
import org.apache.pivot.wtk.Dialog;
import org.apache.pivot.wtk.TablePane;
import org.scijava.module.Module;
import org.scijava.module.process.PreprocessorPlugin;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;
import org.scijava.plugins.uis.pivot.PivotUI;
import org.scijava.ui.AbstractInputHarvesterPlugin;
import org.scijava.ui.UIService;
import org.scijava.ui.UserInterface;
import org.scijava.widget.InputHarvester;
import org.scijava.widget.InputPanel;

/**
 * PivotInputHarvester is an {@link InputHarvester} that collects input
 * parameter values from the user using a {@link PivotInputPanel} dialog box.
 * 
 * @author Curtis Rueden
 * @author Barry DeZonia
 */
@Plugin(type = PreprocessorPlugin.class, priority = InputHarvester.PRIORITY)
public class PivotInputHarvester extends
	AbstractInputHarvesterPlugin<TablePane, BoxPane>
{

	@Parameter
	private UIService uiService;

	// -- InputHarvester methods --

	@Override
	public PivotInputPanel createInputPanel() {
		return new PivotInputPanel();
	}

	@Override
	public boolean harvestInputs(final InputPanel<TablePane, BoxPane> inputPanel,
		final Module module)
	{
		final PivotUI ui = getPivotUI();
		final String title = module.getInfo().getTitle();
		final Dialog dialog = new Dialog(title, inputPanel.getComponent());
		dialog.open(ui.getApplicationFrame());
		final boolean success = dialog.getResult();
		return success;
	}

	// -- Internal methods --

	@Override
	protected String getUI() {
		return PivotUI.NAME;
	}

	// -- Helper methods --

	private PivotUI getPivotUI() {
		final UserInterface ui = uiService.getUI(getUI());
		return (PivotUI) ui;
	}

}
