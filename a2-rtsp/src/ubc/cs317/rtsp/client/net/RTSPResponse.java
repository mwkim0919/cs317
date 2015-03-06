/*
 * University of British Columbia
 * Department of Computer Science
 * CPSC317 - Internet Programming
 * Assignment 2
 * 
 * Author: Jonatan Schroeder
 * January 2013
 * 
 * This code may not be used without written consent of the authors, except for 
 * current and future projects and assignments of the CPSC317 course at UBC.
 */

package ubc.cs317.rtsp.client.net;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import ubc.cs317.rtsp.client.exception.RTSPException;

/**
 * This class represents an RTSP response. The method
 * <code>readRTSPResponse</code> is used to read a response from a
 * BufferedReader (usually associated to a socket).
 */
public class RTSPResponse {

	private String rtspVersion;
	private int responseCode;
	private String responseMessage;
	private Map<String, String> headers;

	/**
	 * Creates an RTSP response.
	 * 
	 * @param rtspVersion
	 *            The String representation of the RTSP version (e.g.,
	 *            "RTSP/1.0").
	 * @param responseCode
	 *            The response code corresponding the result of the requested
	 *            operation.
	 * @param responseMessage
	 *            The response message associated to the response code.
	 */
	public RTSPResponse(String rtspVersion, int responseCode,
			String responseMessage) {
		this.rtspVersion = rtspVersion;
		this.responseCode = responseCode;
		this.responseMessage = responseMessage;
		this.headers = new HashMap<String, String>();
	}

	/**
	 * Returns the RTSP version included in the response. It is expected to be
	 * "RTSP/1.0".
	 * 
	 * @return A String representing the RTSP version read from the response.
	 */
	public String getRtspVersion() {
		return rtspVersion;
	}

	/**
	 * Returns the numeric response code included in the response. The code 200
	 * represent a successful response, while a code between 400 and 599
	 * represents an error.
	 * 
	 * @return The response code of the RTSP response.
	 */
	public int getResponseCode() {
		return responseCode;
	}

	/**
	 * Returns the response message associated to the response code. It should
	 * not be used for any automated verification, and is usually only intended
	 * for human users.
	 * 
	 * @return A String representing the message associated to the response
	 *         code.
	 */
	public String getResponseMessage() {
		return responseMessage;
	}

	/**
	 * Returns the value of the named header field.
	 * 
	 * @return The value of the header field named by headerName, or null if that
	 *         header wasn't included in the response.
	 */
	public String getHeaderValue(String headerName) {
		return headers.get(headerName.toUpperCase());
	}

	/**
	 * Reads and parses an RTSP response from the input. This input is usually
	 * expected to be associated to a socket (although there is no requirement
	 * that it would actually be so).
	 * 
	 * @param reader
	 *            A BufferedReader where the response is expected to be read
	 *            from.
	 * @return An RTSPResponse object if the response was read completely, or
	 *         null if the end of the stream was reached.
	 * @throws IOException
	 *             In case of an I/O error, such as loss of connectivity.
	 * @throws RTSPException
	 *             If the response doesn't match the expected format.
	 */
	public static RTSPResponse readRTSPResponse(BufferedReader reader)
			throws IOException, RTSPException {

		String firstLine = reader.readLine();
		if (firstLine == null)
			return null;
		String[] firstLineSplit = firstLine.split(" ", 3);

		if (firstLineSplit.length != 3
				|| !"RTSP/1.0".equalsIgnoreCase(firstLineSplit[0]))
			throw new RTSPException("Invalid response from RTSP server.");

		RTSPResponse response = new RTSPResponse(firstLineSplit[0],
				Integer.parseInt(firstLineSplit[1]), firstLineSplit[2]);

		String headerLine;
		while ((headerLine = reader.readLine()) != null
				&& !headerLine.equals("")) {

			String[] headerLineSplit = headerLine.split(":", 2);
			if (headerLineSplit.length != 2)
				continue;

			response.headers.put(headerLineSplit[0].toUpperCase(), headerLineSplit[1].trim());
		}

		return response;
	}

}