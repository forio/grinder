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

import java.io.IOException;
import java.io.File;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;


/**
 * Utility methods for working with directories.
 *
 * @author Philip Aston
 * @version $Revision$
 */
public final class Directory  {

  private final File m_directory;

  /**
   * Constructor.
   *
   * @param directory The directory which this <code>Directory</code>
   * operates upon.
   * @exception DirectoryException If <code>directory</code> is not a directory.
   */
  public Directory(File directory) throws DirectoryException {
    if (!directory.isDirectory()) {
      throw new DirectoryException(directory.getPath() +
                                   " is not a directory");
    }

    m_directory = directory;
  }


  /**
   * List the files in the hierarchy below the directory.
   *
   * @return The list of files.
   */
  public File[] listContents() {
    return listContents(false);
  }

  private File[] listContents(boolean includeDirectories) {

    final List resultList = new ArrayList();
    final Set visited = new HashSet();
    final Set directoriesToVisit = new HashSet();
    directoriesToVisit.add(m_directory);

    while (directoriesToVisit.size() > 0) {
      final File[] directories =
        (File[]) directoriesToVisit.toArray(
          new File[directoriesToVisit.size()]);

      for (int i = 0; i < directories.length; ++i) {
        final File directory = directories[i];

        directoriesToVisit.remove(directory);
        visited.add(directory);

        final File[] children = directory.listFiles();

        for (int j = 0; j < children.length; ++j) {
          final File child = children[j];

          if (includeDirectories || !child.isDirectory()) {
            resultList.add(child);
          }

          if (child.isDirectory() && !visited.contains(child)) {
            directoriesToVisit.add(child);
          }
        }
      }
    }

    return (File[])resultList.toArray(new File[resultList.size()]);
  }

  /**
   * Delete the contents of the directory. The directory itself is not
   * removed.
   */
  public void deleteContents() {

    // We rely on the order of the listContents result: more deeply
    // nested files are later in the list.
    final File[] deleteList = listContents(true);

    for (int i = deleteList.length - 1; i >= 0; --i) {
      deleteList[i].delete();
    }
  }

  /**
   * Return the files as an array of {@link FileContents}.

   * @return The array.
   * @exception FileContents.FileContentsException If an error occurs.
   */
  public FileContents[] toFileContentsArray()
    throws FileContents.FileContentsException {

    final File[] files = listContents();
    final FileContents[] result = new FileContents[files.length];

    for (int i = 0; i < files.length; ++i) {
      result[i] = new FileContents(m_directory, files[i]);
    }

    return result;
  }

  /**
   * An exception type used to report Directory related problems.
   */
  public static final class DirectoryException extends IOException {
    DirectoryException(String message) {
      super(message);
    }
  }
}
