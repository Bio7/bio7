/*******************************************************************************
 * Copyright (c) 2007-2026 M. Austenfeld
 *
 * This program and the accompanying materials are made available under
 * the terms of the Eclipse Public License 2.0 which is available at
 * https://www.eclipse.org/legal/epl-2.0
 *
 * Contributors:
 * Marcel Austenfeld - initial API and implementation
 *******************************************************************************/

package com.eco.bio7.pybridge;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import org.eclipse.jface.fieldassist.ContentProposal;
import org.eclipse.jface.fieldassist.IContentProposal;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.Event;

import com.eco.bio7.Bio7Plugin;
import com.eco.bio7.pyffm.PySession;
import com.eco.bio7.pyffm.PyValue;
import com.eco.bio7.rbridge.completion.ContentProposalAdapter;
import com.eco.bio7.rbridge.completion.ShellCompletion;
import com.eco.bio7.rbridge.completion.StyledTextContentAdapter;
import com.eco.bio7.rbridge.views.RShellText;
import com.swtdesigner.ResourceManager;

/**
 * The code completion of the text field of the Python-Shell view (as in the
 * R-Shell): the names of the workspace, attributes of objects (obj.), keywords,
 * builtins and modules (import). With jedi installed in the Python environment
 * its completions are used (also of the results of calls). The keys and the
 * popup are those of the R-Shell (see the R-Shell preferences).
 */
final class PythonShellCompletion {

	/* The time the UI thread waits at most for Python */
	private static final long WAIT_MS = 2_000;
	private static final String SEP = "\u001f";

	private final RShellText control;
	private final ContentProposalAdapter adapter;

	/* The last result of Python: the text before the token and the proposals */
	private String cachedContext;
	private String cachedToken;
	private List<Item> cachedItems = List.of();
	/* The start of the token of the shown proposals */
	private int tokenStart;

	private record Item(String name, String kind, String doc) {
	}

	private static final class Proposal extends ContentProposal {
		final Image image;
		final String kind;

		Proposal(String content, String label, String description, Image image, String kind) {
			super(content, label, description);
			this.image = image;
			this.kind = kind;
		}
	}

	PythonShellCompletion(RShellText control) {
		this.control = control;
		IPreferenceStore store = Bio7Plugin.getDefault().getPreferenceStore();
		char[] auto = null;
		if (store.getBoolean("RSHELL_TYPED_CODE_COMPLETION")) {
			StringBuilder chars = new StringBuilder("._");
			for (char c : store.getString("RSHELL_ACTIVATION_CHARS").toCharArray()) {
				if (Character.isLetter(c) && chars.indexOf(String.valueOf(c)) < 0) {
					chars.append(c);
				}
			}
			auto = chars.toString().toCharArray();
		}
		adapter = new ContentProposalAdapter(control, new StyledTextContentAdapter(), this::proposals, ShellCompletion.getActivationKeystroke(), auto);
		adapter.setPopupSize(new Point(store.getInt("CODE_COMPLETION_POPUP_SIZE_X"), store.getInt("CODE_COMPLETION_POPUP_SIZE_Y")));
		adapter.setPropagateKeys(true);
		adapter.setLabelProvider(new LabelProvider() {
			@Override
			public String getText(Object element) {
				return ((IContentProposal) element).getLabel();
			}

			@Override
			public Image getImage(Object element) {
				return element instanceof Proposal p ? p.image : null;
			}
		});
		adapter.setProposalAcceptanceStyle(ContentProposalAdapter.PROPOSAL_IGNORE);
		adapter.addContentProposalListener((org.eclipse.jface.fieldassist.IContentProposalListener) this::accept);
	}

	/** True while the popup of the proposals is open (it takes Return and the arrow keys). */
	boolean isOpen() {
		return adapter.isProposalPopupOpen();
	}

	/* ---------------- The proposals ---------------- */

	private IContentProposal[] proposals(String contents, int position) {
		String before = contents.substring(0, Math.min(position, contents.length()));
		int start = tokenStartOf(before);
		String context = before.substring(0, start);
		String token = before.substring(start);
		List<Item> items;
		/* Typing more of the same name: filtered without Python */
		if (context.equals(cachedContext) && cachedToken != null && token.startsWith(cachedToken)) {
			items = cachedItems;
		} else {
			items = fromPython(contents, position);
			if (items == null) {
				return new IContentProposal[0];
			}
			cachedContext = context;
			cachedToken = token;
			cachedItems = items;
		}
		tokenStart = start;
		List<IContentProposal> result = new ArrayList<>();
		for (Item it : items) {
			if (it.name.startsWith(token)) {
				String label = it.name + ("keyword".equals(it.kind) ? "" : "  - " + ("variable".equals(it.kind) && !it.doc.isEmpty() ? it.doc : it.kind));
				result.add(new Proposal(it.name, label, it.doc.isEmpty() ? null : it.doc, image(it.kind), it.kind));
			}
		}
		return result.toArray(new IContentProposal[0]);
	}

	/* The start of the name at the end of the text (after the last '.') */
	private static int tokenStartOf(String before) {
		int i = before.length();
		while (i > 0 && (Character.isJavaIdentifierPart(before.charAt(i - 1)))) {
			i--;
		}
		return i;
	}

	/* The completions of Python (null if Python is not running, busy or too slow) */
	private List<Item> fromPython(String contents, int position) {
		PySession py = EmbeddedPython.session();
		if (py == null || EmbeddedPython.isBusy()) {
			return null;
		}
		CompletableFuture<PyValue> f = CompletableFuture.supplyAsync(() -> {
			try {
				PythonShellView.loadModule(py);
				py.assign("_bio7_cargs", new PyValue.PyList(List.of(PyValue.of(contents), PyValue.of(String.valueOf(position)))));
				return py.eval("__import__('_bio7shell').complete(*_bio7_cargs)");
			} catch (Exception e) {
				return null;
			}
		});
		PyValue v;
		try {
			v = f.get(WAIT_MS, TimeUnit.MILLISECONDS);
		} catch (Exception e) {
			return null;
		}
		String[] values = v instanceof PyValue.Str s ? s.values() : null;
		if (values == null || values.length == 0) {
			return values == null ? null : List.of();
		}
		List<Item> items = new ArrayList<>();
		for (int i = 1; i < values.length; i++) {
			String[] p = values[i].split(SEP, 3);
			items.add(new Item(p[0], p.length > 1 ? p[1] : "", p.length > 2 ? p[2] : ""));
		}
		return items;
	}

	/* ---------------- Inserting a proposal ---------------- */

	private void accept(IContentProposal proposal) {
		control.setFocus();
		String text = control.getText();
		int caret = control.getCaretPosition();
		int start = Math.min(tokenStart, caret);
		String after = text.substring(caret);
		String insert = proposal.getContent();
		int cursor = start + insert.length();
		/* A function or class: with parentheses, the cursor between them (as in the R-Shell) */
		if (proposal instanceof Proposal p && ("function".equals(p.kind) || "class".equals(p.kind)) && !after.startsWith("(")
				&& !IMPORT.matcher(text.substring(0, start)).find()) {
			insert += "()";
			cursor = start + insert.length() - 1;
		}
		control.setText(text.substring(0, start) + insert + after);
		control.setSelection(cursor);
		cachedContext = null;
		/* As in the R-Shell: a change for the listeners of the text field */
		control.notifyListeners(SWT.KeyUp, new Event());
	}

	/* In an import statement no parentheses are added */
	private static final java.util.regex.Pattern IMPORT = java.util.regex.Pattern.compile("^\\s*(?:import|from)\\b");

	private static Image image(String kind) {
		String path = switch (kind) {
		case "module" -> "/icons/package_obj.svg";
		case "class" -> "icons/s3.svg";
		case "function" -> "icons/brkp_obj.svg";
		case "keyword" -> "/icons/settings_obj.svg";
		case "variable" -> "/icons/goto_input.svg";
		default -> "icons/types.svg";
		};
		return ResourceManager.getPluginImage(Bio7Plugin.getDefault(), path);
	}
}
