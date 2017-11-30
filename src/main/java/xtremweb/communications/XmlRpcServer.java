/*
 * Copyrights     : CNRS
 * Author         : Oleg Lodygensky
 * Acknowledgment : XtremWeb-HEP is based on XtremWeb 1.8.0 by inria : http://www.xtremweb.net/
 * Web            : http://www.xtremweb-hep.org
 *
 *      This file is part of XtremWeb-HEP.
 *
 *    XtremWeb-HEP is free software: you can redistribute it and/or modify
 *    it under the terms of the GNU General Public License as published by
 *    the Free Software Foundation, either version 3 of the License, or
 *    (at your option) any later version.
 *
 *    XtremWeb-HEP is distributed in the hope that it will be useful,
 *    but WITHOUT ANY WARRANTY; without even the implied warranty of
 *    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *    GNU General Public License for more details.
 *
 *    You should have received a copy of the GNU General Public License
 *    along with XtremWeb-HEP.  If not, see <http://www.gnu.org/licenses/>.
 *
 */

package xtremweb.communications;

/**
 * Copyright 1999 Hannes Wallnoefer
 * Implements an XML-RPC server. See http://www.xmlrpc.com/
 */

import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.EmptyStackException;
import java.util.Hashtable;
import java.util.Stack;
import java.util.Vector;

import org.apache.log4j.Logger;


/**
 * A multithreaded, reusable XML-RPC server object. The name may be misleading
 * because this does not open any server sockets. Instead it is fed by passing
 * an XML-RPC input stream to the execute method. If you want to open a HTTP
 * listener, use the WebServer class instead.
 */
public class XmlRpcServer {

	private final Hashtable handlers;
	private static final Logger logger = Logger.getLogger(XmlRpcServer.class);

	/**
	 * Construct a new XML-RPC server. You have to register handlers to make it
	 * do something useful.
	 */
	public XmlRpcServer() {
		handlers = new Hashtable();
	}

	/**
	 * Register a handler object with this name. Methods of this objects will be
	 * callable over XML-RPC as "handlername.methodname". For more information
	 * about XML-RPC handlers see the <a href="../index.html#1a">main
	 * documentation page</a>.
	 */
	public void addHandler(final String handlername, final Object handler) {
		if ((handler instanceof XmlRpcHandler) || (handler instanceof AuthenticatedXmlRpcHandler)) {
			handlers.put(handlername, handler);
		} else if (handler != null) {
			handlers.put(handlername, new Invoker(handler));
		}
	}

	/**
	 * Remove a handler object that was previously registered with this server.
	 */
	public void removeHandler(final String handlername) {
		handlers.remove(handlername);
	}

	/**
	 * Parse the request and execute the handler method, if one is found.
	 * Returns the result as XML. The calling Java code doesn't need to know
	 * whether the call was successful or not since this is all packed into the
	 * response.
	 */
	public byte[] execute(final InputStream is) {
		return execute(is, null, null);
	}

	/**
	 * Parse the request and execute the handler method, if one is found. If the
	 * invoked handler is AuthenticatedXmlRpcHandler, use the credentials to
	 * authenticate the user.
	 */
	public byte[] execute(final InputStream is, final String user, final String password) {
		final Worker worker = getWorker();
		final byte[] retval = worker.execute(is, user, password);
		pool.push(worker);
		return retval;
	}

	private final Stack pool = new Stack();
	private int workers = 0;

	private final Worker getWorker() {
		try {
			return (Worker) pool.pop();
		} catch (final EmptyStackException x) {
			if (workers < 100) {
				workers += 1;
				return new Worker();
			}
			throw new RuntimeException("System overload");
		}
	}

	class Worker extends XmlRpc {

		private Vector inParams;
		private Object outParam;
		private byte[] result;
		private StringBuffer strbuf;

		public byte[] execute(final InputStream is, final String user, final String password) {
			inParams = new Vector();
			if (strbuf == null) {
				strbuf = new StringBuffer();
			} else {
				strbuf.setLength(0);
			}
			final long now = System.currentTimeMillis();

			try {
				parse(is);
				logger.debug("method name: " + getMethodName());
				logger.debug("inparams: " + inParams);

				if (getErrorLevel() > NONE) {
					throw new Exception(getErrorMsg());
				}
				Object handler = null;

				String handlerName = null;
				final int dot = getMethodName().indexOf(".");
				if (dot > -1) {
					handlerName = getMethodName().substring(0, dot);
					handler = handlers.get(handlerName);
					if (handler != null) {
						setMethodName(getMethodName().substring(dot + 1));
					}
				}

				if (handler == null) {
					handler = handlers.get("$default");
				}

				if (handler == null) {
					if (dot > -1) {
						throw new Exception("RPC handler object \"" + handlerName
								+ "\" not found and no default handler registered.");
					} else {
						throw new Exception("RPC handler object not found for \"" + getMethodName()
								+ "\": no default handler registered.");
					}
				}

				if (handler instanceof AuthenticatedXmlRpcHandler) {
					outParam = ((AuthenticatedXmlRpcHandler) handler).execute(getMethodName(), inParams, user,
							password);
				} else {
					outParam = ((XmlRpcHandler) handler).execute(getMethodName(), inParams);
				}
				logger.debug("outparam = " + outParam);

				final XmlWriter writer = new XmlWriter(strbuf);
				writeResponse(outParam, writer);
				result = writer.getBytes();

			} catch (final Exception x) {
				logger.error("Caught exception: ", x);
				final XmlWriter writer = new XmlWriter(strbuf);
				final String message = x.toString();
				final int code = x instanceof XmlRpcException ? ((XmlRpcException) x).getCode() : 0;
				writeError(code, message, writer);
				try {
					result = writer.getBytes();
				} catch (final UnsupportedEncodingException encx) {
					System.err.println("XmlRpcServer.execute: " + encx);
					result = writer.toString().getBytes();
				}
			}
			logger.debug("Spent " + (System.currentTimeMillis() - now) + " millis in request");
			return result;
		}

		/**
		 * Called when an object to be added to the argument list has been
		 * parsed.
		 */
		@Override
		void objectParsed(final Object what) {
			inParams.addElement(what);
		}

		/**
		 * Writes an XML-RPC response to the XML writer.
		 */
		void writeResponse(final Object param, final XmlWriter writer) {
			writer.startElement("methodResponse");
			writer.startElement("params");
			writer.startElement("param");
			writeObject(param, writer);
			writer.endElement("param");
			writer.endElement("params");
			writer.endElement("methodResponse");
		}

		/**
		 * Writes an XML-RPC error response to the XML writer.
		 */
		void writeError(final int code, final String message, final XmlWriter writer) {
			final Hashtable h = new Hashtable();
			h.put("faultCode", new Integer(code));
			h.put("faultString", message);
			writer.startElement("methodResponse");
			writer.startElement("fault");
			writeObject(h, writer);
			writer.endElement("fault");
			writer.endElement("methodResponse");
		}

	}
}

/** This class uses Java Reflection to call methods matching an XML-RPC call */
class Invoker implements XmlRpcHandler {

	private final Object invokeTarget;
	private final Class targetClass;
	private static final Logger logger = Logger.getLogger(Invoker.class);

	public Invoker(final Object target) {
		invokeTarget = target;
		targetClass = invokeTarget instanceof Class ? (Class) invokeTarget : invokeTarget.getClass();
		logger.debug("Target object is " + targetClass);
	}

	@Override
	public Object execute(final String methodName, final Vector params) throws Exception {

		Class[] argClasses = null;
		Object[] argValues = null;
		if (params != null) {
			argClasses = new Class[params.size()];
			argValues = new Object[params.size()];
			for (int i = 0; i < params.size(); i++) {
				argValues[i] = params.elementAt(i);
				if (argValues[i] instanceof Integer) {
					argClasses[i] = Integer.TYPE;
				} else if (argValues[i] instanceof Double) {
					argClasses[i] = Double.TYPE;
				} else if (argValues[i] instanceof Boolean) {
					argClasses[i] = Boolean.TYPE;
				} else {
					argClasses[i] = argValues[i].getClass();
				}
			}
		}

		Method method = null;

		logger.debug("Searching for method: " + methodName);
		for (int i = 0; i < argClasses.length; i++) {
			logger.debug("Parameter " + i + ": " + argClasses[i] + " = " + argValues[i]);
		}

		try {
			method = targetClass.getMethod(methodName, argClasses);
		} catch (final NoSuchMethodException nsm_e) {
			throw nsm_e;
		} catch (final SecurityException s_e) {
			throw s_e;
		}

		Object returnValue = null;
		try {
			returnValue = method.invoke(invokeTarget, argValues);
		} catch (final IllegalAccessException iacc_e) {
			throw iacc_e;
		} catch (final IllegalArgumentException iarg_e) {
			throw iarg_e;
		} catch (final InvocationTargetException it_e) {
			logger.error("Caught exception: ", it_e);
			throw new Exception(it_e.getTargetException().toString());
		}

		return returnValue;
	}

}
