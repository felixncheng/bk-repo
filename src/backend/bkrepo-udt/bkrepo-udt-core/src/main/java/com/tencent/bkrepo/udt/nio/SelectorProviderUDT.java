/**
 * Copyright (C) 2009-2013 Barchart, Inc. <http://www.barchart.com/>
 *
 * All rights reserved. Licensed under the OSI BSD License.
 *
 * http://www.opensource.org/licenses/bsd-license.php
 */
package com.tencent.bkrepo.udt.nio;

import java.io.IOException;
import java.net.ProtocolFamily;
import java.nio.channels.DatagramChannel;
import java.nio.channels.Pipe;
import java.nio.channels.spi.SelectorProvider;

import com.tencent.bkrepo.udt.SocketUDT;
import com.tencent.bkrepo.udt.TypeUDT;

/**
 * selection provider for UDT
 * <p>
 * note: you must use the same system-wide provider instance for the same
 * {@link TypeUDT} of UDT channels and UDT selectors;
 */
public class SelectorProviderUDT extends SelectorProvider {

	/**
	 * system-wide provider instance, for {@link TypeUDT#DATAGRAM} UDT sockets
	 */
	public static final SelectorProviderUDT DATAGRAM = //
	new SelectorProviderUDT(TypeUDT.DATAGRAM);

	/**
	 * system-wide provider instance, for {@link TypeUDT#STREAM} UDT sockets
	 */
	public static final SelectorProviderUDT STREAM = //
	new SelectorProviderUDT(TypeUDT.STREAM);

	public static SelectorProviderUDT from(final TypeUDT type) {
		switch (type) {
		case DATAGRAM:
			return DATAGRAM;
		case STREAM:
			return STREAM;
		default:
			throw new IllegalStateException("wrong type=" + type);
		}
	}

	private volatile int acceptQueueSize = SocketUDT.DEFAULT_ACCEPT_QUEUE_SIZE;

	private volatile int maxSelectorSize = SocketUDT.DEFAULT_MAX_SELECTOR_SIZE;

	private final TypeUDT type;

	/**
	 * {@link TypeUDT} of UDT sockets generated by this provider
	 */
	public final TypeUDT type() {
		return type;
	}

	protected SelectorProviderUDT(final TypeUDT type) {
		this.type = type;
	}

	public int getAcceptQueueSize() {
		return acceptQueueSize;
	}

	public int getMaxSelectorSize() {
		return maxSelectorSize;
	}

	/**
	 * Not supported.
	 */
	@Override
	public DatagramChannel openDatagramChannel() throws IOException {
		throw new UnsupportedOperationException("feature not available");
	}

	@Override
	public DatagramChannel openDatagramChannel(ProtocolFamily family) throws IOException {
		return null;
	}

	/**
	 * Not supported.
	 */
	@Override
	public Pipe openPipe() throws IOException {
		throw new UnsupportedOperationException("feature not available");
	}

	/**
	 * Open UDT {@link KindUDT#RENDEZVOUS} socket channel.
	 * 
	 * @see RendezvousChannelUDT
	 */
	public RendezvousChannelUDT openRendezvousChannel() throws IOException {
		final SocketUDT socketUDT = new SocketUDT(type);
		return new RendezvousChannelUDT(this, socketUDT);
	}

	/**
	 * Open UDT specific selector.
	 * 
	 * @see SelectorUDT
	 */
	@Override
	public SelectorUDT openSelector() throws IOException {
		return new SelectorUDT(this, maxSelectorSize);
	}

	/**
	 * Open UDT {@link KindUDT#ACCEPTOR} socket channel.
	 * 
	 * @see ServerSocketChannelUDT
	 */
	@Override
	public ServerSocketChannelUDT openServerSocketChannel() throws IOException {
		final SocketUDT serverSocketUDT = new SocketUDT(type);
		return new ServerSocketChannelUDT(this, serverSocketUDT);
	}

	/**
	 * Open UDT {@link KindUDT#CONNECTOR} socket channel.
	 * 
	 * @see SocketChannelUDT
	 */
	@Override
	public SocketChannelUDT openSocketChannel() throws IOException {
		final SocketUDT socketUDT = new SocketUDT(type);
		return new SocketChannelUDT(this, socketUDT);
	}

	public void setAcceptQueueSize(final int queueSize) {
		acceptQueueSize = queueSize;
	}

	public void setMaxSelectorSize(final int selectorSize) {
		maxSelectorSize = selectorSize;
	}

}
