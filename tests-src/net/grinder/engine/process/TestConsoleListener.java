// Copyright (C) 2001, 2002, 2003 Philip Aston
// All rights reserved.
//
// This file is part of The Grinder software distribution. Refer to
// the file LICENSE which is part of The Grinder distribution for
// licensing details. The Grinder distribution is available on the
// Internet at http://grinder.sourceforge.net/
//
// THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
// "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
// LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS
// FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE
// REGENTS OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT,
// INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
// (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
// SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION)
// HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT,
// STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
// ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED
// OF THE POSSIBILITY OF SUCH DAMAGE.

package net.grinder.engine.process;

import java.io.InputStream;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;

import junit.framework.TestCase;

import net.grinder.common.LogCounter;
import net.grinder.communication.Receiver;
import net.grinder.communication.ResetGrinderMessage;
import net.grinder.communication.Sender;
import net.grinder.communication.StartGrinderMessage;
import net.grinder.communication.StopGrinderMessage;
import net.grinder.communication.StreamReceiver;
import net.grinder.communication.StreamSender;


/**
 * Unit test case for <code>ConsoleListener</code>.
 *
 * @author Philip Aston
 * @version $Revision$
 */
public class TestConsoleListener extends TestCase {

  public TestConsoleListener(String name) {
    super(name);
  }

  private final LogCounter m_logger = new LogCounter();
  private Receiver m_receiver;
  private Sender m_sender;

  protected void setUp() throws Exception {
    final PipedOutputStream outputStream = new PipedOutputStream();
    final InputStream inputStream = new PipedInputStream(outputStream);

    m_receiver = new StreamReceiver(inputStream);
    m_sender = new StreamSender(outputStream);
  }

  protected void tearDown() throws Exception {
    m_receiver.shutdown();
  }

  public void testConstruction() throws Exception {
    final MyMonitor myMonitor = new MyMonitor();

    final ConsoleListener listener0 =
      new ConsoleListener(m_receiver, myMonitor, m_logger);

    final ConsoleListener listener1 =
      new ConsoleListener(m_receiver, myMonitor, m_logger);

    listener0.shutdown();
    listener1.shutdown();

    assertEquals(0, m_logger.getNumberOfMessages());
    assertEquals(0, m_logger.getNumberOfErrors());
  }

  public void testListener() throws Exception {
    final MyMonitor myMonitor = new MyMonitor();

    final ConsoleListener listener =
      new ConsoleListener(m_receiver, myMonitor, m_logger);

    assertEquals(0, listener.received(ConsoleListener.ANY));

    final MyMonitor.WaitForMessages t1 =
      myMonitor.new WaitForMessages(1000, listener,
                                    ConsoleListener.RESET |
                                    ConsoleListener.START);
    t1.start();

    m_sender.send(new ResetGrinderMessage());
    m_sender.send(new StartGrinderMessage());
    t1.join();
    assertTrue(!t1.getTimerExpired());
    assertEquals(0, listener.received(ConsoleListener.ANY));

    final MyMonitor.WaitForMessages t2 =
      myMonitor.new WaitForMessages(1000, listener, ConsoleListener.STOP);
    t2.start();

    m_sender.send(new StartGrinderMessage());
    m_sender.send(new StopGrinderMessage());
    t2.join();
    assertTrue(!t2.getTimerExpired());

    assertEquals(0, listener.received(ConsoleListener.RESET));

    assertEquals(ConsoleListener.START,
                 listener.received(ConsoleListener.START |
                                   ConsoleListener.STOP));

    assertEquals(4, m_logger.getNumberOfMessages());
    assertEquals(0, m_logger.getNumberOfErrors());

    listener.shutdown();
  }

  public void testShutdown() throws Exception {

    final ConsoleListener listener =
      new ConsoleListener(m_receiver, new MyMonitor(), m_logger);

    assertTrue(listener.getReceiverThread().isAlive());
    listener.shutdown();

    while (listener.getReceiverThread().isAlive()) {
      Thread.sleep(10);
    }

    assertEquals(0, m_logger.getNumberOfMessages());
    assertEquals(0, m_logger.getNumberOfErrors());
  }

  private final static class MyMonitor implements Monitor {
    private final class WaitForMessages extends Thread {
      private final long m_time;
      private final ConsoleListener m_listener;
      private int m_expectedMessages;
      private boolean m_timerExpired = false;

      public WaitForMessages(long time, ConsoleListener listener,
                             int expectedMessages) {
        m_time = time;
        m_listener = listener;
        m_expectedMessages = expectedMessages;
      }

      public final boolean getTimerExpired() {
        return m_timerExpired;
      }

      public final void run() {
        synchronized(MyMonitor.this) {
          long currentTime = System.currentTimeMillis();
          final long wakeUpTime = currentTime + m_time;
		
          while (currentTime < wakeUpTime) {
            final int receivedMessages =
              m_listener.received(m_expectedMessages);

            m_expectedMessages ^= receivedMessages;

            if (m_expectedMessages == 0) {
              return;
            }

            try {
              MyMonitor.this.wait(wakeUpTime - currentTime);

              currentTime = System.currentTimeMillis();

              if (currentTime >= wakeUpTime) {
                m_timerExpired = true;
              }
            }
            catch (InterruptedException e) {
              currentTime = System.currentTimeMillis();
            }
          }
        }
      }
    }
  }
}
