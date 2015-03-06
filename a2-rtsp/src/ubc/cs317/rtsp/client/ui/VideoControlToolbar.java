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

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JButton;
import javax.swing.JOptionPane;
import javax.swing.JToolBar;

import ubc.cs317.rtsp.client.exception.RTSPException;

public class VideoControlToolbar extends JToolBar {

	private MainWindow main;
	private JButton openButton, playButton, pauseButton;
	private JButton closeButton;
	private JButton disconnectButton;

	public VideoControlToolbar(MainWindow mainWindow) {

		this.main = mainWindow;

		setFloatable(false);

		openButton = new JButton("Open");
		openButton.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				try {
					String videoName = JOptionPane
							.showInputDialog("Video file:");
					if (videoName != null)
						main.getSession().open(videoName);
				} catch (RTSPException ex) {
					JOptionPane.showMessageDialog(main, ex.getMessage());
				}
			}
		});
		this.add(openButton);

		this.addSeparator();

		playButton = new JButton("Play");
		playButton.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				try {
					main.getSession().play();
				} catch (RTSPException ex) {
					JOptionPane.showMessageDialog(main, ex.getMessage());
				}
			}
		});
		this.add(playButton);

		pauseButton = new JButton("Pause");
		pauseButton.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				try {
					main.getSession().pause();
				} catch (RTSPException ex) {
					JOptionPane.showMessageDialog(main, ex.getMessage());
				}
			}
		});
		this.add(pauseButton);

		this.addSeparator();

		closeButton = new JButton("Close");
		closeButton.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				try {
					main.getSession().close();
				} catch (RTSPException ex) {
					JOptionPane.showMessageDialog(main, ex.getMessage());
				}
			}
		});
		this.add(closeButton);

		this.addSeparator();

		disconnectButton = new JButton("Disconnect");
		disconnectButton.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				main.disconnect(true);
			}
		});
		this.add(disconnectButton);
	}
}
