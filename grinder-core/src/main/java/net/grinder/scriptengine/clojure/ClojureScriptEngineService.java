// Copyright (C) 2011 Philip Aston
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
// COPYRIGHT HOLDERS OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT,
// INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
// (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
// SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION)
// HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT,
// STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
// ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED
// OF THE POSSIBILITY OF SUCH DAMAGE.

package net.grinder.scriptengine.clojure;

import static java.util.Collections.emptyList;

import java.util.List;

import net.grinder.engine.common.EngineException;
import net.grinder.engine.common.ScriptLocation;
import net.grinder.scriptengine.Instrumenter;
import net.grinder.scriptengine.ScriptEngineService;
import net.grinder.util.FileExtensionMatcher;


/**
 * Clojure script engine.
 *
 * @author Philip Aston
 */
public class ClojureScriptEngineService implements ScriptEngineService {

  private final FileExtensionMatcher m_cljFileMatcher =
    new FileExtensionMatcher(".clj");

  /**
   * {@inheritDoc}
   */
  @Override public ScriptEngine createScriptEngine(ScriptLocation script)
    throws EngineException {

    if (m_cljFileMatcher.accept(script.getFile())) {
      try {
        return new ClojureScriptEngine(script);
      }
      catch (LinkageError e) {
        throw new EngineException("Clojure is not on the classpath", e);
      }
    }

    return null;
  }

  /**
   * {@inheritDoc}
   */
  @Override public List<? extends Instrumenter> createInstrumenters()
    throws EngineException {
    return emptyList();
  }
}
