/*
 * crforge - https://github.com/voonhous/crforge
 * SPDX-License-Identifier: Apache-2.0
 * Porting this code? Please cite this file and the commit you read: see the README.
 */

package org.crforge.desktop;

import com.badlogic.gdx.Application;
import com.badlogic.gdx.ApplicationLogger;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Graphics;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.InputProcessor;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Files;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.glutils.GLVersion;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.utils.Clipboard;
import com.badlogic.gdx.utils.GdxNativesLoader;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.nio.ShortBuffer;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;

/**
 * The libGDX application the visualizer runs on without a display: no window and no GPU, a fixed
 * delta time in place of the wall clock, and an OpenGL that draws nothing but digests every call it
 * is given, with the contents of every buffer and array the call passes (vertices, indices,
 * uniforms, texture pixels). The deletions of a frame are digested as a set.
 *
 * <p>The wall clock reaches a window's drawing in two places, both removed here: the renderers'
 * delta time, and how long scene2d keeps a clicked button drawn pressed.
 *
 * <p>Everything the visualizer does reaches the screen through OpenGL, so two runs that gave the
 * same calls draw the same pixels on a given driver. The digest of one frame's calls is what {@link
 * VisualizerTrace} records. The natives of the fonts run as they do in a window, so the glyph
 * pixels are the real ones.
 */
final class HeadlessGdx {

  private static final int MAX_TEXTURE_SIZE = 4096;

  private static final int FRAMEBUFFER_COMPLETE = 0x8CD5;

  private final Gl gl = new Gl();

  private float deltaSeconds;

  private final int width;

  private final int height;

  private int mouseX;

  private int mouseY;

  private InputProcessor processor;

  /** Whether the application asked to exit, which the visualizer does only on a crash. */
  private boolean exitAsked;

  private final Clipboard clipboard =
      new Clipboard() {
        private String contents = "";

        @Override
        public boolean hasContents() {
          return !contents.isEmpty();
        }

        @Override
        public String getContents() {
          return contents;
        }

        @Override
        public void setContents(String content) {
          contents = content;
        }
      };

  /**
   * Installs a headless application in libGDX's statics, with the window size the launcher opens.
   *
   * @param width the window's width in pixels
   * @param height the window's height in pixels
   * @param deltaSeconds the delta time every frame reports
   */
  HeadlessGdx(int width, int height, float deltaSeconds) {
    this.width = width;
    this.height = height;
    this.deltaSeconds = deltaSeconds;
    GdxNativesLoader.load();
    // A clicked button stays drawn pressed for this long by the wall clock; here not at all, since
    // no other input of a frame depends on how fast the run is.
    ClickListener.visualPressedDuration = 0f;
    Graphics graphics = proxy(Graphics.class, this::graphics);
    Input input = proxy(Input.class, this::input);
    Gdx.files = new Lwjgl3Files();
    Gdx.graphics = graphics;
    Gdx.input = input;
    Gdx.gl = gl.proxy;
    Gdx.gl20 = gl.proxy;
    Gdx.gl30 = null;
    Gdx.gl31 = null;
    Gdx.gl32 = null;
    Gdx.app = proxy(Application.class, this::application);
  }

  /** The delta time the coming frames report, the fixed step of a scenario's stretch. */
  void delta(float seconds) {
    deltaSeconds = seconds;
  }

  /**
   * The digest of the calls since the last frame ended, and starts the next frame's.
   *
   * @return the frame's calls, draw calls and digest
   */
  Frame endFrame() {
    if (exitAsked) {
      throw new AssertionError("the visualizer crashed and asked to exit; see the log");
    }
    return gl.endFrame();
  }

  /** What one frame sent to OpenGL. */
  record Frame(long calls, long draws, String digest) {}

  private Object application(Object proxy, Method method, Object[] args) {
    return switch (method.getName()) {
      case "getType" -> Application.ApplicationType.Desktop;
      case "getClipboard" -> clipboard;
      case "getGraphics" -> Gdx.graphics;
      case "getInput" -> Gdx.input;
      case "getFiles" -> Gdx.files;
      case "getApplicationLogger" -> null;
      case "exit" -> {
        exitAsked = true;
        yield null;
      }
      case "postRunnable" -> {
        ((Runnable) args[0]).run();
        yield null;
      }
      case "hashCode" -> System.identityHashCode(proxy);
      case "equals" -> proxy == args[0];
      case "toString" -> "HeadlessGdx.Application";
      default -> defaultValue(method);
    };
  }

  private Object graphics(Object proxy, Method method, Object[] args) {
    return switch (method.getName()) {
      case "getWidth", "getBackBufferWidth" -> width;
      case "getHeight", "getBackBufferHeight" -> height;
      case "getDeltaTime", "getRawDeltaTime" -> deltaSeconds;
      case "getFramesPerSecond" -> Math.round(1 / deltaSeconds);
      case "getDensity", "getBackBufferScale" -> 1f;
      case "getPpiX", "getPpiY", "getPpcX", "getPpcY" -> 96f;
      case "getGL20" -> gl.proxy;
      case "getGLVersion" ->
          new GLVersion(Application.ApplicationType.Desktop, "2.1", "headless", "headless");
      case "getBufferFormat" -> new Graphics.BufferFormat(8, 8, 8, 8, 16, 0, 0, false);
      case "hashCode" -> System.identityHashCode(proxy);
      case "equals" -> proxy == args[0];
      case "toString" -> "HeadlessGdx.Graphics";
      default -> defaultValue(method);
    };
  }

  private Object input(Object proxy, Method method, Object[] args) {
    return switch (method.getName()) {
      case "getX" -> mouseX;
      case "getY" -> mouseY;
      case "setCursorPosition" -> {
        mouseX = (Integer) args[0];
        mouseY = (Integer) args[1];
        yield null;
      }
      case "setInputProcessor" -> {
        processor = (InputProcessor) args[0];
        yield null;
      }
      case "getInputProcessor" -> processor;
      case "hashCode" -> System.identityHashCode(proxy);
      case "equals" -> proxy == args[0];
      case "toString" -> "HeadlessGdx.Input";
      default -> defaultValue(method);
    };
  }

  @SuppressWarnings("unchecked")
  private static <T> T proxy(Class<T> type, InvocationHandler handler) {
    return (T) Proxy.newProxyInstance(type.getClassLoader(), new Class<?>[] {type}, handler);
  }

  /** What a call nobody here answers returns: nothing, zero or false. */
  private static Object defaultValue(Method method) {
    Class<?> type = method.getReturnType();
    if (type == boolean.class) return false;
    if (type == int.class) return 0;
    if (type == long.class) return 0L;
    if (type == float.class) return 0f;
    if (type == double.class) return 0d;
    if (type == short.class) return (short) 0;
    if (type == byte.class) return (byte) 0;
    if (type == char.class) return (char) 0;
    if (type == String.class) return "";
    if (type == ApplicationLogger.class) return null;
    return null;
  }

  /** An OpenGL that hands out names, reports success and digests every call in order. */
  private static final class Gl {

    final GL20 proxy = HeadlessGdx.proxy(GL20.class, this::call);

    private final MessageDigest digest = sha256();

    private final Map<String, Integer> locations = new HashMap<>();

    private final List<String> deleted = new ArrayList<>();

    private int nextName = 1;

    private long calls;

    private long draws;

    Frame endFrame() {
      Collections.sort(deleted);
      deleted.forEach(this::text);
      deleted.clear();
      Frame frame =
          new Frame(calls, draws, HexFormat.of().formatHex(digest.digest()).substring(0, 16));
      calls = 0;
      draws = 0;
      return frame;
    }

    private Object call(Object self, Method method, Object[] args) {
      String name = method.getName();
      if (method.getDeclaringClass() == Object.class) {
        return switch (name) {
          case "hashCode" -> System.identityHashCode(self);
          case "equals" -> self == args[0];
          default -> "HeadlessGdx.Gl";
        };
      }
      record(name, args);
      calls++;
      if (name.equals("glDrawArrays") || name.equals("glDrawElements")) {
        draws++;
      }
      return switch (name) {
        case "glCreateProgram", "glCreateShader", "glGenBuffer", "glGenTexture" -> nextName++;
        case "glGenFramebuffer", "glGenRenderbuffer" -> nextName++;
        case "glGetAttribLocation", "glGetUniformLocation" ->
            locations.computeIfAbsent(args[0] + "/" + args[1], key -> locations.size());
        case "glCheckFramebufferStatus" -> FRAMEBUFFER_COMPLETE;
        case "glGetIntegerv" -> {
          ((IntBuffer) args[1]).put(((IntBuffer) args[1]).position(), MAX_TEXTURE_SIZE);
          yield null;
        }
        case "glGetShaderiv", "glGetProgramiv" -> {
          // Compiled and linked, with nothing active: the locations are asked for by name.
          int pname = (Integer) args[1];
          boolean count = pname == GL20.GL_ACTIVE_UNIFORMS || pname == GL20.GL_ACTIVE_ATTRIBUTES;
          ((IntBuffer) args[2]).put(((IntBuffer) args[2]).position(), count ? 0 : 1);
          yield null;
        }
        case "glGenBuffers", "glGenTextures", "glGenFramebuffers", "glGenRenderbuffers" -> {
          IntBuffer names = (IntBuffer) args[1];
          for (int i = 0; i < (Integer) args[0]; i++) {
            names.put(names.position() + i, nextName++);
          }
          yield null;
        }
        default -> defaultValue(method);
      };
    }

    private void record(String name, Object[] args) {
      if (name.startsWith("glDelete")) {
        // Resources are let go in the order of hash maps keyed by identity, which differs from one
        // process to another and draws nothing: what is deleted counts, not the order.
        deleted.add(name + Arrays.deepToString(new Object[][] {args}));
        return;
      }
      text(name);
      if (args == null) {
        return;
      }
      for (Object arg : args) {
        switch (arg) {
          case null -> text("null");
          case ByteBuffer bytes -> digest.update(bytes.duplicate());
          case FloatBuffer floats -> {
            FloatBuffer copy = floats.duplicate();
            while (copy.hasRemaining()) {
              text(Integer.toHexString(Float.floatToRawIntBits(copy.get())));
            }
          }
          case IntBuffer ints -> {
            IntBuffer copy = ints.duplicate();
            while (copy.hasRemaining()) {
              text(Integer.toHexString(copy.get()));
            }
          }
          case ShortBuffer shorts -> {
            ShortBuffer copy = shorts.duplicate();
            while (copy.hasRemaining()) {
              text(Integer.toHexString(copy.get()));
            }
          }
          case Buffer other ->
              throw new AssertionError("a buffer not digested: " + other.getClass());
          case float[] floats -> {
            for (float value : floats) {
              text(Integer.toHexString(Float.floatToRawIntBits(value)));
            }
          }
          case int[] ints -> {
            for (int value : ints) {
              text(Integer.toHexString(value));
            }
          }
          case byte[] bytes -> digest.update(bytes);
          default -> text(arg.toString());
        }
      }
    }

    private void text(String value) {
      digest.update(value.getBytes(StandardCharsets.UTF_8));
      digest.update((byte) 0);
    }

    private static MessageDigest sha256() {
      try {
        return MessageDigest.getInstance("SHA-256");
      } catch (NoSuchAlgorithmException e) {
        throw new IllegalStateException(e);
      }
    }
  }
}
