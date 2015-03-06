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

package ubc.cs317.rtsp.client.model;

import java.awt.Image;
import java.awt.Toolkit;

/**
 * This class represents an individual frame in a video stream.
 */
public class Frame {

	private byte payloadType;
	private boolean marker;
	private short sequenceNumber;
	private int timestamp;
	private byte[] payload;

	/**
	 * Creates a new frame.
	 * 
	 * @param payloadType
	 *            The numeric type of payload found in the frame. The most
	 *            common type is 26 (JPEG).
	 * @param marker
	 *            An indication if the frame is an important frame when compared
	 *            to other frames in the stream.
	 * @param sequenceNumber
	 *            A sequential number corresponding to the ordering of the
	 *            frame. This number is expected to start at 0 (zero) and
	 *            increase by one for each frame following that.
	 * @param timestamp
	 *            The number of milliseconds after the logical start of the
	 *            stream when this frame is expected to be played.
	 * @param payload
	 *            A byte array containing the payload (contents) of the frame.
	 * @param offset
	 *            The position in <tt>payload</tt> where the contents start.
	 * @param length
	 *            The number of bytes to be considered as contents in
	 *            <tt>payload</tt>.
	 */
	public Frame(byte payloadType, boolean marker, short sequenceNumber,
			int timestamp, byte[] payload, int offset, int length) {

		this.payloadType = payloadType;
		this.marker = marker;
		this.sequenceNumber = sequenceNumber;
		this.timestamp = timestamp;

		this.payload = new byte[length];
		System.arraycopy(payload, offset, this.payload, 0, length);
	}

	/**
	 * Creates a new frame.
	 * 
	 * @param payloadType
	 *            The numeric type of payload found in the frame. The most
	 *            common type is 26 (JPEG).
	 * @param marker
	 *            An indication if the frame is an important frame when compared
	 *            to other frames in the stream.
	 * @param sequenceNumber
	 *            A sequential number corresponding to the ordering of the
	 *            frame. This number is expected to start at 0 (zero) and
	 *            increase by one for each frame following that.
	 * @param timestamp
	 *            The number of milliseconds after the logical start of the
	 *            stream when this frame is expected to be played.
	 * @param payload
	 *            A byte array containing the payload (contents) of the frame.
	 */
	public Frame(byte payloadType, boolean marker, short sequenceNumber,
			int timestamp, byte[] payload) {
		this(payloadType, marker, sequenceNumber, timestamp, payload, 0,
				payload.length);
	}

	/**
	 * Returns the type of payload found in this frame.
	 * 
	 * @return A numeric type of payload.
	 */
	public byte getPayloadType() {
		return payloadType;
	}

	/**
	 * Returns an indication if the frame is to be considered an
	 * important/essential frame when compared to other frames in the stream.
	 * 
	 * @return <tt>true</tt> if the frame is important/essential, <tt>false</tt>
	 *         otherwise.
	 */
	public boolean isMarkerOn() {
		return marker;
	}

	/**
	 * Returns the sequence number of the frame. This number corresponds to 0
	 * for the first frame of the stream and increments by one for each frame
	 * after that.
	 * 
	 * @return The sequence number of the frame.
	 */
	public short getSequenceNumber() {
		return sequenceNumber;
	}

	/**
	 * Returns the timestamp of the frame, in milliseconds from the beginning of
	 * the stream.
	 * 
	 * @return Timestamp of the frame, in milliseconds.
	 */
	public int getTimestamp() {
		return timestamp;
	}

	/**
	 * Returns the raw data included in the frame.
	 * 
	 * @return A byte array corresponding to the raw data of the frame.
	 */
	public byte[] getPayload() {
		return payload.clone();
	}

	/**
	 * Returns the number of bytes in the payload (contents) of the frame. This
	 * is equivalent to <code>getPayload().length</code>.
	 * 
	 * @return The length of the payload.
	 */
	public int getPayloadLength() {
		return payload.length;
	}

	/**
	 * Creates an Image based on the payload of the frame.
	 * 
	 * @return An <code>Image</code> object corresponding to the frame contents.
	 */
	public Image getImage() {
		return Toolkit.getDefaultToolkit().createImage(getPayload());
	}
}
