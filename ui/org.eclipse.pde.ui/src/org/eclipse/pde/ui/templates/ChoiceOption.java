package org.eclipse.pde.ui.templates;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.*;
import org.eclipse.swt.layout.*;
import org.eclipse.swt.widgets.*;

/**
 * Implementation of the TemplateOption that allows users to
 * choose a value from the fixed set of options.
 * <p>
 * <b>Note:</b> This class is part of an interim API that is still under development and expected to
 * change significantly before reaching stability. It is being made available at this early stage to solicit feedback
 * from pioneering adopters on the understanding that any code that uses this API will almost certainly be broken
 * (repeatedly) as the API evolves.
 * </p>
 */
public class ChoiceOption extends TemplateOption {
	private String[][] choices;
	private Control labelControl;
	private Button[] buttons;
	private boolean blockListener;

	/**
	 * Constructor for ChoiceOption.
	 * @param section the parent section.
	 * @param name the unique name
	 * @param label the presentable label 
	 * @param choices the list of choices from which the value
	 * can be chosen. Each array entry should be an array of size 2,
	 * where position 0 will be interpeted as the choice unique
	 * name, and position 1 as the choice presentable label.
	 * <p>
	 * <b>Note:</b> This method is part of an interim API that is still under development and expected to
	 * change significantly before reaching stability. It is being made available at this early stage to solicit feedback
	 * from pioneering adopters on the understanding that any code that uses this API will almost certainly be broken
	 * (repeatedly) as the API evolves.
	 * </p>
	 */
	public ChoiceOption(
		BaseOptionTemplateSection section,
		String name,
		String label,
		String[][] choices) {
		super(section, name, label);
		this.choices = choices;
	}

	/**
	 * @see TemplateField#createControl(Composite, int, FormWidgetFactory)
	 * <p>
	 * <b>Note:</b> This method is part of an interim API that is still under development and expected to
	 * change significantly before reaching stability. It is being made available at this early stage to solicit feedback
	 * from pioneering adopters on the understanding that any code that uses this API will almost certainly be broken
	 * (repeatedly) as the API evolves.
	 * </p>
	 */
	public void createControl(Composite parent, int span) {
		Composite container = createComposite(parent, span);
		fill(container, span);
		GridLayout layout = new GridLayout();
		layout.marginWidth = layout.marginHeight = 0;
		container.setLayout(layout);
		labelControl = createLabel(container, span);
		labelControl.setEnabled(isEnabled());
		fill(labelControl, span);

		buttons = new Button[choices.length];

		SelectionListener listener = new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				Button b = (Button) e.widget;
				if (blockListener)
					return;
				if (b.getSelection()) {
					ChoiceOption.super.setValue(b.getData().toString());
					getSection().validateOptions(ChoiceOption.this);
				}
			}
		};

		for (int i = 0; i < choices.length; i++) {
			String[] choice = choices[i];
			Button button = createRadioButton(parent, span, choice);
			buttons[i] = button;
			button.addSelectionListener(listener);
			button.setEnabled(isEnabled());
		}
		if (getChoice() != null)
			selectChoice(getChoice());
	}
	/**
	 * Returns the string value of the current choice.
	 * @return the current choice or <samp>null</samp> if not initialized.
	 * <p> 
	 * <b>Note:</b> This method is part of an interim API that is still under development and expected to
	 * change significantly before reaching stability. It is being made available at this early stage to solicit feedback
	 * from pioneering adopters on the understanding that any code that uses this API will almost certainly be broken
	 * (repeatedly) as the API evolves.
	 * </p>
	 */
	public String getChoice() {
		return getValue() != null ? getValue().toString() : null;
	}

	/**
	 * Implements the superclass method by passing the new value
	 * to the option's widget.
	 * @param value the new value.
	 * <p>
	 * <b>Note:</b> This method is part of an interim API that is still under development and expected to
	 * change significantly before reaching stability. It is being made available at this early stage to solicit feedback
	 * from pioneering adopters on the understanding that any code that uses this API will almost certainly be broken
	 * (repeatedly) as the API evolves.
	 * </p>
	 */
	public void setValue(Object value) {
		super.setValue(value);
		if (buttons != null && value != null) {
			selectChoice(value.toString());
		}
	}
	/**
	 * Implements the superclass method by updating the
	 * enable state of the option's widget.
	 * <p>
	 * <b>Note:</b> This method is part of an interim API that is still under development and expected to
	 * change significantly before reaching stability. It is being made available at this early stage to solicit feedback
	 * from pioneering adopters on the understanding that any code that uses this API will almost certainly be broken
	 * (repeatedly) as the API evolves.
	 * </p>
	 */
	public void setEnabled(boolean enabled) {
		super.setEnabled(enabled);
		if (labelControl != null) {
			labelControl.setEnabled(enabled);
			for (int i = 0; i < buttons.length; i++) {
				buttons[i].setEnabled(isEnabled());
			}
		}
	}
	
	private GridData fill(Control control, int span) {
		GridData gd = new GridData(GridData.HORIZONTAL_ALIGN_FILL);
		gd.horizontalSpan = span;
		control.setLayoutData(gd);
		return gd;
	}

	private Composite createComposite(Composite parent, int span) {
		Composite composite = new Composite(parent, SWT.NULL);
		fill(composite, span);
		return composite;
	}

	private Button createRadioButton(Composite parent, int span, String[] choice) {
		Button button = new Button(parent, SWT.RADIO);
		button.setData(choice[0]);
		button.setText(choice[1]);
		GridData gd = fill(button, span);
		gd.horizontalIndent = 10;
		return button;
	}

	private void selectChoice(String choice) {
		blockListener = true;
		for (int i = 0; i < buttons.length; i++) {
			Button button = buttons[i];
			String bname = button.getData().toString();
			if (bname.equals(choice)) {
				button.setSelection(true);
			} else {
				button.setSelection(false);
			}
		}
		blockListener = false;
	}
}