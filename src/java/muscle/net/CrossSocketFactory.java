package muscle.net;

import java.io.*;
import java.net.*;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

/** 
 * 
 * @author Mariusz Mamonski
 */

public class CrossSocketFactory implements jade.imtp.leap.JICP.SocketFactory, SocketFactory {

	public static final String PROP_PORT_RANGE_MIN = "pl.psnc.mapper.muscle.portrange.min";
	public static final String PROP_PORT_RANGE_MAX = "pl.psnc.mapper.muscle.portrange.max";
	public static final String PROP_MAIN_PORT = "pl.psnc.mapper.muscle.magicPort";
	public static final String PROP_DEBUG = "pl.psnc.mapper.muscle.debug";
	public static final String PROP_TRACE = "pl.psnc.mapper.muscle.trace";
	public static final String PROP_MTO_ADDRESS = "pl.psnc.mapper.muscle.mto.address";
	public static final String PROP_MTO_PORT = "pl.psnc.mapper.muscle.mto.port";

	public static final String ENV_COORDINATOR_URL = "QCG_COORDINATOR_URL";
	public static final String ENV_SESSION_ID = "SESSION_ID";

	public static final String PUT_MSG_TEMPLATE_1 = "<SOAP-ENV:Envelope xmlns:SOAP-ENV=\"http://schemas.xmlsoap.org/soap/envelope/\"> "
			+ "<SOAP-ENV:Body><smoacoordinator:PutProcessEntry xmlns:smoacoordinator=\"http://schemas.qoscosgrid.org/coordinator/2009/04/service\"><smoacoordinator:ProcessEntry>"
			+ "<smoacoordinator:ProcessEntryHeader><smoacoordinator:Key>";
	public static final String PUT_MSG_TEMPLATE_2 = /* @SESSION_KEY@ */"</smoacoordinator:Key></smoacoordinator:ProcessEntryHeader>"
			+ "<smoacoordinator:ProcessData><items><items>";
	public static final String PUT_MSG_TEMPLATE_3 = /* @MASTER_HOST@ */"</items><items>";
	public static final String PUT_MSG_TEMPLATE_4 = /* @MASTER_PORT@ */"</items></items></smoacoordinator:ProcessData>"
			+ "</smoacoordinator:ProcessEntry></smoacoordinator:PutProcessEntry></SOAP-ENV:Body></SOAP-ENV:Envelope>";

	public static final String GET_MSG_TEMPLATE_1 = "<SOAP-ENV:Envelope xmlns:SOAP-ENV=\"http://schemas.xmlsoap.org/soap/envelope/\">"
			+ "<SOAP-ENV:Body><smoacoordinator:GetProcessEntry xmlns:smoacoordinator=\"http://schemas.qoscosgrid.org/coordinator/2009/04/service\">"
			+ "<smoacoordinator:ProcessEntryHeader><smoacoordinator:Key>";
	public static final String GET_MSG_TEMPLATE_2 = /* @SESSION_KEY@ */"</smoacoordinator:Key></smoacoordinator:ProcessEntryHeader></smoacoordinator:GetProcessEntry></SOAP-ENV:Body></SOAP-ENV:Envelope>";

	protected int portMin = 9000;
	protected int portMax = 9500;
	protected int magicPort = 22;
	protected boolean debug = true;
	protected boolean trace = false;
	protected String socketFile = "socket.file";
	protected InetAddress mtoAddr = null;
	protected int mtoPort = -1;

	public CrossSocketFactory() {
		if (System.getProperty(PROP_PORT_RANGE_MIN) != null) {
			portMin = Integer.valueOf(System.getProperty(PROP_PORT_RANGE_MIN));
		}

		if (System.getProperty(PROP_PORT_RANGE_MAX) != null) {
			portMax = Integer.valueOf(System.getProperty(PROP_PORT_RANGE_MAX));
		}

		if (System.getProperty(PROP_MAIN_PORT) != null) {
			magicPort = Integer.valueOf(System.getProperty(PROP_MAIN_PORT));
		}

		if (System.getProperty(PROP_DEBUG) != null) {
			debug = Boolean.valueOf(System.getProperty(PROP_DEBUG));
		}

		if (System.getProperty(PROP_TRACE) != null) {
			trace = Boolean.valueOf(System.getProperty(PROP_TRACE));
		}

		if (System.getProperty(PROP_MTO_ADDRESS) != null) {
			try {
				mtoAddr = InetAddress.getByName(System
						.getProperty(PROP_MTO_ADDRESS));
			} catch (UnknownHostException e) {
				logDebug("Provided MTO address unresolvable: "
						+ e.getLocalizedMessage());
			}
		}

		if (System.getProperty(PROP_MTO_PORT) != null) {
			mtoPort = Integer.valueOf(System.getProperty(PROP_MTO_PORT));
		}
	}

	public ServerSocket createServerSocket(int port, int backlog,
			InetAddress addr) throws IOException {
		
		if (port == magicPort) {
			logDebug("binding socket on MAIN port and addr " + addr);
		} else if (port == 0) {
			logDebug("binding socket on ANY port and addr " + addr);
		} else {
			logDebug("binding socket on port " + port + " and addr " + addr);
			trace();
			throw new java.net.BindException();
		}

		trace();

		ServerSocket ss = null;
		Exception lastEx = null;

		for (int i = portMin; i <= portMax; i++) {
			try {
				logDebug("Trying to bind on port: " + i);
				ss = new ServerSocket(i, backlog, addr);
				break;
			} catch (BindException ex) {
				logDebug("BindFailed: " + ex.getMessage());
				lastEx = ex;
			}
		}

		if (ss != null) {
			System.err.println("Bound to port: " + ss.getLocalPort());

			if (mtoAddr != null && mtoPort != -1) {
				try {
					mtoRegisterListening((InetSocketAddress) ss.getLocalSocketAddress());
				} catch (IOException ex) {
					throw new IOException("Could not register a server socket at the MTO: " + ex.getMessage());
				}

				logDebug("Registered to MTO");
			} else
				logDebug("Missing MTO address / port. MTO will not be used.");

			if (port == magicPort) {
				putConnectionData(InetAddress.getLocalHost()
						.getHostAddress(), ss.getLocalPort());

			}
			return ss;
		} else {
			throw new BindException(lastEx.getMessage());
		}
	}

	private void putConnectionData(String hostAddress, int localPort)
			throws IOException {
		String coordinatorURL = System.getenv(ENV_COORDINATOR_URL);
		String sessionID = System.getenv(ENV_SESSION_ID);
		StringBuilder message = new StringBuilder(4096);

		if (coordinatorURL == null)
			throw new IOException(ENV_COORDINATOR_URL + " env variable not set");

		if (sessionID == null)
			throw new IOException(ENV_SESSION_ID + " env variable not set");

		message.append(PUT_MSG_TEMPLATE_1);
		message.append(sessionID);
		message.append(PUT_MSG_TEMPLATE_2);
		message.append(hostAddress);
		message.append(PUT_MSG_TEMPLATE_3);
		message.append(localPort);
		message.append(PUT_MSG_TEMPLATE_4);

		URL url = new URL(coordinatorURL);

		sendHTTPRequest(url, message.toString());

	}

	private String sendHTTPRequest(URL url, String request) throws IOException {
		HttpURLConnection conn = (HttpURLConnection) url.openConnection();
		StringBuffer response = new StringBuffer(4096);
		OutputStreamWriter osw = null;
		BufferedReader brd = null;

		try {

			if (trace)
				logDebug("Request Message: " + request);

			conn.setDoOutput(true);
			conn.setRequestMethod("POST");

			osw = new OutputStreamWriter(conn.getOutputStream());
			osw.write(request);
			osw.flush();

			if (conn.getResponseCode() != HttpURLConnection.HTTP_OK)
				throw new IOException("Call to " + url + " failed with code "
						+ conn.getResponseCode());

			brd = new BufferedReader(new InputStreamReader(
					conn.getInputStream()));
			String line;
			while ((line = brd.readLine()) != null) {
				response.append(line);
			}

			if (trace)
				logDebug("Response Message: " + response);

		} catch (RuntimeException e) {
			throw e;
		} finally {

			if (osw != null)
				osw.close();

			if (brd != null)
				brd.close();
		}

		return response.toString();
	}

	private InetSocketAddress getConnectionData() throws IOException {
		String coordinatorURL = System.getenv(ENV_COORDINATOR_URL);
		String sessionID = System.getenv(ENV_SESSION_ID);
		StringBuilder message = new StringBuilder(4096);

		logDebug("Acquiring connection data (" + coordinatorURL + ")");

		if (coordinatorURL == null)
			throw new IOException(ENV_COORDINATOR_URL + " env variable not set");

		if (sessionID == null)
			throw new IOException(ENV_SESSION_ID + " env variable not set");

		URL url = new URL(coordinatorURL);

		message.append(GET_MSG_TEMPLATE_1);
		message.append(sessionID);
		message.append(GET_MSG_TEMPLATE_2);

		String response = sendHTTPRequest(url, message.toString());
		String host = null;
		int port = -1;

		try {
			/*
			 * This little code save couple of MB needed for WebService
			 * framework
			 */
			SAXParserFactory factory = SAXParserFactory.newInstance();
			SAXParser saxParser = factory.newSAXParser();
			XMLToListHandler saxHandler = new XMLToListHandler("items");
			saxParser.parse(new InputSource(new StringReader(response)),
					saxHandler);

			if (saxHandler.getParsedValues().size() != 2) {
				for (String value : saxHandler.getParsedValues()) {
					logDebug(" parsed value: " + value);
				}
				throw new IOException(
						"Unexpected coordinator response (items count = "
								+ saxHandler.getParsedValues().size() + " )");
			}

			host = saxHandler.getParsedValues().get(0);
			port = Integer.parseInt(saxHandler.getParsedValues().get(1));
		} catch (SAXException ex) {
			throw new IOException("Failed to parse coordinator response: " + ex);
		} catch (ParserConfigurationException ex) {
			throw new AssertionError(ex);
		}

		logDebug("Master host: " + host);
		logDebug("Master port: " + port);

		try {
			/**
			 * very ugly but what else we can do if bounded socket is not enough
			 * for JADE...
			 */
			Thread.sleep(3000);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}

		return new InetSocketAddress(host, port);
	}

	protected void trace() {
		if (trace) {
			System.err.println("---TRACE---");
			try {
				throw new Exception();
			} catch (Exception ex) {
				ex.printStackTrace();
			}
		}
	}

	protected void logDebug(String msg) {
		if (debug) {
			System.err.println("-- XSS -- : " + msg);
		}
	}

	public Socket createSocket() {
		logDebug("creating new client socket");
		trace();
		return new CrossSocket();
	}

	private class CrossSocket extends Socket {
		public void connect(SocketAddress endpoint, int timeout)
				throws IOException {
			logDebug("connecting to:" + endpoint);
			InetSocketAddress processedEndpoint = (InetSocketAddress) processEndpoint(endpoint);

			int port = processedEndpoint.getPort();
			if (port >= portMin && port <= portMax) {
				// direct connection
				super.connect(processedEndpoint, timeout);
			} else {
				if (mtoPort == -1 || mtoAddr == null) {
					logDebug("Connection out of port range, but MTO is not configured. Trying local");
					super.connect(processedEndpoint, timeout);
					return;
				}

				mtoConnect(timeout, processedEndpoint);
			}
		}

		public void connect(SocketAddress endpoint) throws IOException {
			connect(endpoint, 0);
		}

		private SocketAddress processEndpoint(SocketAddress endpoint)
				throws IOException {
			InetSocketAddress iaddr = (InetSocketAddress) endpoint;

			if (iaddr.getPort() == magicPort)
				return getConnectionData();
			else
				return endpoint;
		}

		private void mtoConnect(int timeout, InetSocketAddress processedEndpoint)
				throws IOException {
			super.connect(new InetSocketAddress(mtoAddr, mtoPort), timeout);

			// prepare & send request
			MtoRequest req = new MtoRequest();
			req.setSource((InetSocketAddress) getLocalSocketAddress());
			req.setDestination(processedEndpoint);
			req.type = MtoRequest.TYPE_CONNECT;

			getOutputStream().write(req.write().array());

			// Read answer
			ByteBuffer buffer = ByteBuffer.allocate(MtoRequest.byteSize());
			getInputStream().read(buffer.array());
			MtoRequest res = MtoRequest.read(buffer);

//			System.out.println("Req: " + req.toString());
//			System.out.println("Res: " + res.toString());

			assert res.dstA.equals(req.dstA) && res.dstP == req.dstP
					&& res.srcA.equals(req.srcA) && res.srcP == req.srcP;
			assert res.type == MtoRequest.TYPE_CONNECT_RESPONSE;

			// React
			int response = new DataInputStream(getInputStream()).readInt();
			if (response != 0) {
				close();
				throw new IOException("MTO denied connection");
			}
		}

	}

	private class XMLToListHandler extends DefaultHandler {
		protected List<String> parsedValues = new ArrayList<String>();
		protected String selectorName;
		protected boolean activated;

		public XMLToListHandler(String selectorName) {
			this.selectorName = selectorName;
		}

		public void characters(char[] ch, int start, int length)
				throws SAXException {

			if (activated) {
				parsedValues.add(new String(ch, start, length));
				activated = false;
			}
		}

		public void startElement(String uri, String localName, String qName,
				Attributes attributes) throws SAXException {
			if (qName.endsWith(selectorName))
				activated = true;
		}

		public List<String> getParsedValues() {
			return parsedValues;
		}

	}

	private void mtoRegisterListening(InetSocketAddress isa) throws IOException {
		// If one tries to register a wildcard, register all known addresses
		if(isa.getAddress().isAnyLocalAddress())
		{
			for( NetworkInterface interf : Collections.list(NetworkInterface.getNetworkInterfaces())) {
				for(InetAddress addr : Collections.list(interf.getInetAddresses())) {
					mtoRegisterListening(new InetSocketAddress(addr, isa.getPort()));
				}
			}
			return;
		}
		
		// If one registers loopback, do it only if the MTO is on loopback as well
		if(isa.getAddress().isLoopbackAddress() && ! mtoAddr.isLoopbackAddress())
			return;
		
		if( ! (isa.getAddress() instanceof Inet4Address ) )
			return;
		
		
		MtoRequest r = new MtoRequest();
		r.type = MtoRequest.TYPE_REGISTER;
		r.setSource(isa);
		Socket s = new Socket();
		s.connect(new InetSocketAddress(mtoAddr, mtoPort));
		s.getOutputStream().write(r.write().array());
		s.close();
	}
}
