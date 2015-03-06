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

package ubc.cs317.rtsp.client.exception;

public class RTSPException extends Exception {

	public RTSPException(String message) {
		super(message);
	}

	public RTSPException(Throwable cause) {
		super(cause);
	}

	public RTSPException(String message, Throwable cause) {
		super(message, cause);
	}
}
