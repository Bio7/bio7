/*******************************************************************************
 * Copyright (c) 2007-2026 M. Austenfeld
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     M. Austenfeld
 *******************************************************************************/

package com.eco.bio7.rbridge.backend;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.rosuda.REngine.REXP;
import org.rosuda.REngine.REXPDouble;
import org.rosuda.REngine.REXPFactor;
import org.rosuda.REngine.REXPGenericVector;
import org.rosuda.REngine.REXPInteger;
import org.rosuda.REngine.REXPList;
import org.rosuda.REngine.REXPLogical;
import org.rosuda.REngine.REXPMismatchException;
import org.rosuda.REngine.REXPNull;
import org.rosuda.REngine.REXPRaw;
import org.rosuda.REngine.REXPString;
import org.rosuda.REngine.REXPUnknown;
import org.rosuda.REngine.REngineException;
import org.rosuda.REngine.RList;
import org.rosuda.REngine.Rserve.RConnection;
import org.rosuda.REngine.Rserve.RserveException;

import com.eco.bio7.rffm.RException;
import com.eco.bio7.rffm.RSession;
import com.eco.bio7.rffm.RValue;

/**
 * The embedded backend (experimental): R in the process of Bio7 (plugin
 * com.eco.bio7.rffm). The values are converted between the data model of the
 * plugin (RValue) and the REXP classes which Bio7 uses.
 */
public final class EmbeddedRBackend implements RBackend {

	private static final String[] ATTRIBUTES = { "names", "dim", "dimnames", "class", "levels", "row.names" };

	private final RSession session;

	public EmbeddedRBackend(RSession session) {
		this.session = session;
	}

	public RSession session() {
		return session;
	}

	private static RserveException error(RException e) {
		return new RserveException(null, e.getMessage());
	}

	@Override
	public REXP eval(String code) throws RserveException {
		try {
			return toREXP(session.eval(code));
		} catch (RException e) {
			throw error(e);
		}
	}

	@Override
	public void voidEval(String code) throws RserveException {
		try {
			session.voidEval(code);
		} catch (RException e) {
			throw error(e);
		}
	}

	@Override
	public void assign(String name, String value) throws RserveException {
		try {
			session.assign(name, value);
		} catch (RException e) {
			throw error(e);
		}
	}

	@Override
	public void assign(String name, REXP value) throws RserveException {
		try {
			session.assign(name, toRValue(value));
		} catch (RException e) {
			throw error(e);
		} catch (REXPMismatchException e) {
			throw new RserveException(null, e.getMessage());
		}
	}

	@Override
	public void assign(String name, double[] values) throws REngineException {
		try {
			session.assign(name, values);
		} catch (RException e) {
			throw error(e);
		}
	}

	@Override
	public void assign(String name, int[] values) throws REngineException {
		try {
			session.assign(name, values);
		} catch (RException e) {
			throw error(e);
		}
	}

	@Override
	public void assign(String name, String[] values) throws REngineException {
		try {
			session.assign(name, values);
		} catch (RException e) {
			throw error(e);
		}
	}

	@Override
	public void assign(String name, byte[] values) throws REngineException {
		try {
			session.assign(name, values);
		} catch (RException e) {
			throw error(e);
		}
	}

	@Override
	public void assign(String name, REXP value, REXP env) throws REngineException {
		if (env != null) {
			throw new REngineException(null, "Environment references are not supported by embedded R");
		}
		assign(name, value);
	}

	@Override
	public boolean close() {
		/* Embedded R runs until Bio7 ends */
		return true;
	}

	@Override
	public void shutdown() {
	}

	@Override
	public boolean isConnected() {
		return session.isAlive();
	}

	@Override
	public boolean isEmbedded() {
		return true;
	}

	@Override
	public RConnection rserveConnection() {
		return null;
	}

	/* ---------------- RValue <-> REXP ---------------- */

	/** An RValue as REXP (the classes of REngine which Bio7 uses). */
	public static REXP toREXP(RValue v) {
		REXPList attr = attributes(v);
		return switch (v) {
		case RValue.Null n -> new REXPNull();
		case RValue.Logical l -> {
			byte[] b = new byte[l.values().length];
			for (int i = 0; i < b.length; i++) {
				int x = l.values()[i];
				b[i] = x == RValue.NA_INTEGER ? REXPLogical.NA : x != 0 ? REXPLogical.TRUE : REXPLogical.FALSE;
			}
			yield new REXPLogical(b, attr);
		}
		case RValue.Int i -> i.isFactor() && i.levels() != null ? new REXPFactor(i.values(), i.levels(), attr) : new REXPInteger(i.values(), attr);
		case RValue.Num d -> new REXPDouble(d.values(), attr);
		case RValue.Str s -> new REXPString(s.values(), attr);
		case RValue.Raw r -> new REXPRaw(r.values(), attr);
		case RValue.List l -> {
			List<REXP> values = new ArrayList<>(l.length());
			for (RValue e : l.values()) {
				values.add(toREXP(e));
			}
			String[] names = l.names();
			yield new REXPGenericVector(names == null ? new RList(values) : new RList(values, names), attr);
		}
		case RValue.Other o -> new REXPUnknown(-1, attr);
		};
	}

	private static REXPList attributes(RValue v) {
		if (v.attributes().isEmpty()) {
			return null;
		}
		List<REXP> values = new ArrayList<>();
		List<String> names = new ArrayList<>();
		for (var e : v.attributes().entrySet()) {
			names.add(e.getKey());
			values.add(toREXP(e.getValue()));
		}
		return new REXPList(new RList(values, names.toArray(String[]::new)));
	}

	/** A REXP as RValue (for assign). */
	public static RValue toRValue(REXP x) throws REXPMismatchException {
		if (x == null || x.isNull()) {
			return RValue.Null.INSTANCE;
		}
		Map<String, RValue> attr = new LinkedHashMap<>();
		for (String name : ATTRIBUTES) {
			REXP a = x.getAttribute(name);
			if (a != null && !a.isNull()) {
				attr.put(name, toRValue(a));
			}
		}
		if (x instanceof REXPFactor f) {
			attr.put("levels", new RValue.Str(f.asFactor().levels()));
			attr.put("class", new RValue.Str("factor"));
			return new RValue.Int(f.asIntegers(), attr);
		}
		if (x instanceof REXPLogical l) {
			boolean[] t = l.isTRUE();
			boolean[] na = l.isNA();
			int[] v = new int[t.length];
			for (int i = 0; i < v.length; i++) {
				v[i] = na[i] ? RValue.NA_INTEGER : t[i] ? 1 : 0;
			}
			return new RValue.Logical(v, attr);
		}
		if (x instanceof REXPInteger) {
			return new RValue.Int(x.asIntegers(), attr);
		}
		if (x instanceof REXPDouble) {
			return new RValue.Num(x.asDoubles(), attr);
		}
		if (x instanceof REXPString) {
			return new RValue.Str(x.asStrings(), attr);
		}
		if (x instanceof REXPRaw) {
			return new RValue.Raw(x.asBytes(), attr);
		}
		if (x instanceof REXPGenericVector g) {
			RList list = g.asList();
			List<RValue> values = new ArrayList<>(list.size());
			for (int i = 0; i < list.size(); i++) {
				values.add(toRValue(list.at(i)));
			}
			if (list.isNamed() && !attr.containsKey("names")) {
				String[] names = new String[list.size()];
				for (int i = 0; i < names.length; i++) {
					names[i] = list.keyAt(i);
				}
				attr.put("names", new RValue.Str(names));
			}
			return new RValue.List(values, attr);
		}
		throw new REXPMismatchException(x, "transfer to embedded R");
	}
}
