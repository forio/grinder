// Copyright (C) 2004 Philip Aston
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

package net.grinder.util;

import java.io.File;
import java.io.FileOutputStream;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import net.grinder.testutility.AbstractFileTestCase;
import net.grinder.testutility.FileUtilities;


/**
 * Unit test case for {@link Directory}.
 *
 * @author Philip Aston
 * @version $Revision$
 */
public class TestDirectory extends AbstractFileTestCase {

  public void testConstruction() throws Exception {

    try {
      new Directory(new File(getDirectory(), "x"));
      fail("Expected DirectoryException");
    }
    catch (Directory.DirectoryException e) {
    }

    final Directory directory = new Directory(getDirectory());
    assertEquals(0, directory.getWarnings().length);

    assertEquals(getDirectory(), directory.getAsFile());
  }

  public void testListContents() throws Exception {

    final Directory directory = new Directory(getDirectory());

    final String[] files = {
      "directory/foo/bah/blah",
      "directory/blah",
      "a/b/c/d/e",
      "a/b/f/g/h",
      "a/b/f/g/i",
      "x",
      "y/z",
      "another",
    };

    final File[] badDirectories = {
      new File(getDirectory(), "directory/foo/bah/blah.cantread"),
      new File(getDirectory(), "readonly"),
    };

    for (int i = 0; i < badDirectories.length; ++i) {
      badDirectories[i].getParentFile().mkdirs();
      badDirectories[i].mkdir();
      FileUtilities.setCanRead(badDirectories[i], false);
    }

    final Set expected = new HashSet();

    for (int i=0; i<files.length; ++i) {
      final File file = new File(getDirectory(), files[i]);
      file.getParentFile().mkdirs();
      file.createNewFile();

      // Result uses relative paths.
      expected.add(new File(files[i]));
    }

    final File[] allFiles = directory.listContents();

    for (int i=0; i<allFiles.length; ++i) {
      assertTrue("Contains " + allFiles[i], expected.contains(allFiles[i]));
    }

    final String[] warnings = directory.getWarnings();
    assertEquals(badDirectories.length, warnings.length);

    final StringBuffer warningsBuffer = new StringBuffer();

    for (int i = 0; i < warnings.length; ++i) {
      warningsBuffer.append(warnings[i]);
      warningsBuffer.append("\n");
    }

    final String warningsString = warningsBuffer.toString();

    for (int i = 0; i < badDirectories.length; ++i) {
      assertTrue(warningsBuffer + " contains " + badDirectories[i].getPath(),
                 warningsString.indexOf(badDirectories[i].getPath()) > -1);

      FileUtilities.setCanRead(badDirectories[i], true);
    }
  }

  public void testDeleteContents() throws Exception {

    final Directory directory = new Directory(getDirectory());

    final String[] files = {
      "directory/foo/bah/blah",
      "directory/blah",
      "a/b/c/d/e",
      "a/b/f/g/h",
      "a/b/f/g/i",
      "x",
      "y/z",
      "another",
    };

    for (int i=0; i<files.length; ++i) {
      final File file = new File(getDirectory(), files[i]);
      file.getParentFile().mkdirs();
      file.createNewFile();
    }
    
    assertTrue(getDirectory().list().length > 0);

    directory.deleteContents();

    assertEquals(0, getDirectory().list().length);
  }
}
