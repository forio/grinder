// Copyright (C) 2000, 2001, 2002 Philip Aston
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

package net.grinder.tools.tcpproxy;

import java.util.Random;

import junit.framework.TestCase;


/**
 * Unit test case for <code>ConnectionDetails</code>.
 *
 * @author Philip Aston
 * @version $Revision$
 */
public class TestConnectionDetails extends TestCase {

  public void testConstructor() throws Exception {
    try {
      new ConnectionDetails(new EndPoint("one", 4789),
                            new EndPoint("one", 4789), true);
      fail("Expected IllegalArgumentException");
    }
    catch (IllegalArgumentException e) {
    }
  }

  public void testAccessors() throws Exception {

    final Random random = new Random();

    for (int i=0; i<10; ++i) {
      final byte[] localBytes = new byte[random.nextInt(30)];
      random.nextBytes(localBytes);
      final EndPoint localEndPoint =
        new EndPoint(new String(localBytes), random.nextInt(65536));

      final byte[] remoteBytes = new byte[random.nextInt(30)];
      random.nextBytes(remoteBytes);
      final EndPoint remoteEndPoint =
        new EndPoint(new String(remoteBytes), random.nextInt(65536));

      final boolean isSecure = random.nextBoolean();

      final ConnectionDetails connectionDetails =
        new ConnectionDetails(localEndPoint, remoteEndPoint, isSecure);

      assertEquals(localEndPoint, connectionDetails.getLocalEndPoint());
      assertEquals(remoteEndPoint, connectionDetails.getRemoteEndPoint());
      assertTrue(!(isSecure ^ connectionDetails.isSecure()));
    }
  }

  public void testToString() throws Exception {

    final ConnectionDetails connectionDetails =
      new ConnectionDetails(new EndPoint("one", 55),
                            new EndPoint("two", 121), true);

    assertEquals("one:55->two:121", connectionDetails.toString());
  }

  public void testGetURLBase() throws Exception {

    final ConnectionDetails connectionDetails =
      new ConnectionDetails(new EndPoint("one", 55),
                            new EndPoint("two", 121), true);

    assertEquals("https://two:121", connectionDetails.getURLBase("http"));

    final ConnectionDetails cd2 =
      new ConnectionDetails(new EndPoint("here", 65535),
                            new EndPoint("there", 32233),
                            false);

    assertEquals("xx://there:32233", cd2.getURLBase("xx"));
  }

  public void testEquality() throws Exception {
    final ConnectionDetails[] connectionDetails = {
      new ConnectionDetails(new EndPoint("A", 55),
                            new EndPoint("B", 80), false),
      new ConnectionDetails(new EndPoint("a", 55),
                            new EndPoint("B", 80), false),
      new ConnectionDetails(new EndPoint("c", 55),
                            new EndPoint("B", 80), false),
      new ConnectionDetails(new EndPoint("a", 55),
                            new EndPoint("B", 80), true),
      new ConnectionDetails(new EndPoint("a", 56),
                            new EndPoint("B", 80), false),
    };

    assertEquals(connectionDetails[0], connectionDetails[0]);
    assertEquals(connectionDetails[0], connectionDetails[1]);
    assertEquals(connectionDetails[1], connectionDetails[0]);
    assertFalse(connectionDetails[0].equals(connectionDetails[2]));
    assertFalse(connectionDetails[1].equals(connectionDetails[3]));
    assertFalse(connectionDetails[1].equals(connectionDetails[4]));

    assertFalse(connectionDetails[0].equals(this));
  }

  public void testGetOtherEnd() throws Exception {
    final ConnectionDetails connectionDetails =
      new ConnectionDetails(new EndPoint("blah", 123),
                            new EndPoint("blurgh", 9999), true);

    final ConnectionDetails otherEnd = connectionDetails.getOtherEnd();

    assertEquals(connectionDetails.getLocalEndPoint(),
                 otherEnd.getRemoteEndPoint());

    assertEquals(connectionDetails.getRemoteEndPoint(),
                 otherEnd.getLocalEndPoint());

    assertEquals(connectionDetails.isSecure(), otherEnd.isSecure());

    assertEquals(connectionDetails, otherEnd.getOtherEnd());
  }

  public void testGetConnectionIdentity() throws Exception {
    final ConnectionDetails connectionDetails1 =
      new ConnectionDetails(new EndPoint("foo", 123),
                            new EndPoint("bar", 9999), true);

    final ConnectionDetails connectionDetails2 =
      new ConnectionDetails(new EndPoint("foo", 123),
                            new EndPoint("beer", 9999), true);

    assertTrue(!(connectionDetails1.getConnectionIdentity().equals(
                   connectionDetails2.getConnectionIdentity())));

    assertEquals(connectionDetails1.getConnectionIdentity(),
                 connectionDetails1.getConnectionIdentity());

    assertEquals(connectionDetails1.getConnectionIdentity(),
                 connectionDetails1.getOtherEnd().getConnectionIdentity());

    assertEquals(connectionDetails2.getConnectionIdentity(),
                 connectionDetails2.getOtherEnd().getConnectionIdentity());
  }
}
