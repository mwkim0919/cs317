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

package ubc.cs317.rtsp.client.ui;

import java.awt.BorderLayout;
import java.awt.Image;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

import javax.swing.ImageIcon;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.SwingConstants;

import ubc.cs317.rtsp.client.exception.RTSPException;
import ubc.cs317.rtsp.client.model.Frame;
import ubc.cs317.rtsp.client.model.Session;
import ubc.cs317.rtsp.client.model.listener.SessionListener;

public class MainWindow extends JFrame implements SessionListener {

	private Session session;

	private VideoControlToolbar videoControlToolbar;
	private JLabel imagePanel;
	private JLabel videoNamePanel;

	public MainWindow() {

		super("Video Client");

		videoControlToolbar = new VideoControlToolbar(this);
		imagePanel = new JLabel();
		imagePanel.setHorizontalAlignment(SwingConstants.CENTER);
		imagePanel.setVerticalAlignment(SwingConstants.CENTER);
		videoNamePanel = new JLabel();
		videoNamePanel.setHorizontalAlignment(SwingConstants.CENTER);

		this.setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
		this.addWindowListener(new WindowAdapter() {
			@Override
			public void windowClosing(WindowEvent e) {
				disconnect(false);
			}
		});

		this.setLayout(new BorderLayout(1, 1));
		this.add(imagePanel, BorderLayout.CENTER);
		this.add(videoControlToolbar, BorderLayout.PAGE_START);
		this.add(videoNamePanel, BorderLayout.PAGE_END);

		this.setSize(1024, 600);

		showOpenDialog();
	}

	public Session getSession() {
		return session;
	}

	public void setSession(Session session) {
		if (this.session == session)
			return;

		if (this.session != null) {
			this.session.removeSessionListener(this);
			this.session.closeConnection();
		}

		this.session = session;

		if (session != null) {
			session.addSessionListener(this);
		}
	}

	public synchronized void disconnect(boolean showOpenDialog) {
		setSession(null);
		if (showOpenDialog)
			showOpenDialog();
		else
			System.exit(0);
	}

	/**
	 * 
	 */
	private void showOpenDialog() {
		new SelectServerDialog(this);
	}

	public static void main(String[] args) {

		new MainWindow();
	}

	@Override
	public void exceptionThrown(RTSPException exception) {
		JOptionPane.showMessageDialog(this, exception.getMessage());
	}

	@Override
	public void frameReceived(Frame frame) {
		if (frame == null)
			imagePanel.setIcon(null);
		else {

			Image scaledImage = frame.getImage().getScaledInstance(
					-imagePanel.getWidth(), imagePanel.getHeight(),
					Image.SCALE_FAST);
			imagePanel.setIcon(new ImageIcon(scaledImage));
		}
	}

	@Override
	public void videoNameChanged(String videoName) {
		if (videoName==null)
			videoNamePanel.setText("(click open to select a video)");
		else
			videoNamePanel.setText("Video: " + videoName);
	}
}
