// The Grinder
// Copyright (C) 2000, 2001  Paco Gomez
// Copyright (C) 2000, 2001  Philip Aston

// This program is free software; you can redistribute it and/or
// modify it under the terms of the GNU General Public License
// as published by the Free Software Foundation; either version 2
// of the License, or (at your option) any later version.

// This program is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
// GNU General Public License for more details.

// You should have received a copy of the GNU General Public License
// along with this program; if not, write to the Free Software
// Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.

package net.grinder.communication;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.util.HashMap;
import java.util.Map;


/**
 * Class that manages the receipt of multicast messages.
 *
 * @author Philip Aston
 * @version $Revision$
 */
public final class Receiver
{
    private final byte[] m_buffer = new byte[65536];
    private final String m_multicastAddressString;
    private final int m_multicastPort;
    private final MulticastSocket m_socket;
    private final DatagramPacket m_packet;
    private final Map m_sequenceValues = new HashMap();
    private boolean m_listening = false;

    private boolean m_shuttingDown = false;
    private boolean m_shutDown = false;
    private Message m_shutdownMessage;

    /**
     * Constructor.
     *
     * @param multicastAddressString The multicast address to bind on.
     * @param multicastPort The port to bind to.
     *
     * @ throws CommunicationException If socket could not be bound to.
     **/
    public Receiver(String multicastAddressString, int multicastPort)
	throws CommunicationException
    {
	m_multicastAddressString = multicastAddressString;
	m_multicastPort = multicastPort;

	try {
	    m_socket = new MulticastSocket(m_multicastPort);
	    m_socket.joinGroup(
		InetAddress.getByName(m_multicastAddressString));
	}
	catch (IOException e) {
	    throw new CommunicationException(
		"Could not bind to multicast address " +
		multicastAddressString + ":" + multicastPort, e);
	}

	m_packet = new DatagramPacket(m_buffer, m_buffer.length);
    }

    /**
     * Only one thread should call this method at any one time. This
     * method blocks until a message is available, or another thread
     * has called {@link #shutdown}. Typically called from a message
     * dispatch loop.
     *
     * @return The message or <code>null</code> if shutting down.
     *
     * @throws CommunicationException If more than one thread attempts
     * to call <code>waitForMessage</code> on a single {@link
     * Receiver}.
     * @throws CommunicationException If an IO exception occurs
     * reading the mesage.
     **/
    public final Message waitForMessage() throws CommunicationException
    {
	synchronized (this) {
	    if (m_listening) {
		throw new CommunicationException(
		    "More than one thread called waitForMessage()");
	    }

	    m_listening = true;
	}

	try {
	    final Message message;

	    if (m_shuttingDown) {
		shutdownComplete();
		return null;
	    }
	
	    try {
		m_packet.setData(m_buffer, 0, m_buffer.length);
		m_socket.receive(m_packet);

		final ByteArrayInputStream byteStream =
		    new ByteArrayInputStream(m_buffer, 0, m_buffer.length);

		final ObjectInputStream objectStream =
		    new ObjectInputStream(byteStream);

		message = (Message)objectStream.readObject();
	    }
	    catch (Exception e) {
		throw new CommunicationException(
		    "Error receving multicast packet", e);
	    }

	    if (message.equals(m_shutdownMessage)) {
		shutdownComplete();
		return null;
	    }

	    final String senderID = message.getSenderUniqueID();
	    final long sequenceNumber = message.getSequenceNumber();

	    final SequenceValue sequenceValue =
		(SequenceValue)m_sequenceValues.get(senderID);

	    if (sequenceValue != null) {
		sequenceValue.nextValue(sequenceNumber, senderID);
	    }
	    else {
		m_sequenceValues.put(senderID,
				     new SequenceValue(sequenceNumber));
	    }
	    
	    return message;
	}
	finally {
	    synchronized (this) {
		m_listening = false;
	    }

	    // Check this after reseting "listening" flag to avoid
	    // race condition where shutdown() decides to send a
	    // ShutdownMessage but our caller decides to not listen
	    // for any more messages.
	    if (m_shuttingDown) {
		shutdownComplete();
	    }
	}
    }

    /**
     * Shut down this reciever.
     **/
    public final void shutdown()
    {
	if (!m_shutDown) {
	    if (!m_shuttingDown) {
		m_shuttingDown = true;

		final boolean needSuicideMessage;

		synchronized(this) {
		    needSuicideMessage = m_listening;
		}

		if (needSuicideMessage) {
		    try {
			// Pretty hacky way of shutting down the receiver.
			// The packet goes out on the wire. Can't do much
			// else with the DatagramSocket API though.
			m_shutdownMessage = new ShutdownMessage();

			new SenderImplementation(
			    "suicide is painless", m_multicastAddressString,
			    m_multicastPort).send(m_shutdownMessage);
		    }
		    catch (CommunicationException e) {
			// We made our best effort.
			shutdownComplete();
		    }
		}
		else {
		    shutdownComplete();
		}
	    }

	    while (!m_shutDown) {
		try {
		    synchronized (this) {
			wait();
		    }
		}
		catch (InterruptedException e) {
		}
	    }
	}
    }

    private final synchronized void shutdownComplete()
    {
	m_shutDown = true;
	notifyAll();
    }

    /**
     * Numeric sequence checker. Relies on caller for synchronisation.
     **/
    private final class SequenceValue
    {
	private long m_value;

	/**
	 * Constructor.
	 * @param initialValue The initial sequence value.
	 **/
	public SequenceValue(long initialValue) 
	{
	    m_value = initialValue;
	}

	/**
	 * Check the next value in the sequence, and store it for next time.
	 *
	 * @param newValue The next value.
	 * @throws CommunicationException If the message is out of sequence.
	 **/
	public final void nextValue(long newValue, String senderID)
	    throws CommunicationException
	{
	    if (newValue != ++m_value) {
		System.err.println(
		    "Out of sequence message from Sender '" +
		    senderID + "' (received " + newValue + 
		    ", expected " + m_value + ")");

		m_value = newValue;
	    }
	}
    }

    /**
     * Message used to signal shutdown to thread blocked in {@link #waitForMessage}.
     **/
    private static final class ShutdownMessage extends Message
    {
    }
}
