package rsb;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;

import rsb.methods.MethodContext;

import org.python.core.Py;
import org.python.core.PyObject;
import org.python.core.PyException;
import org.python.util.InteractiveConsole;
import org.python.util.InteractiveInterpreter;

public class RemotePy {

    private MethodContext ctx;

    public final int DEFAULT_PORT = 5518;

    private final String STD_IN = "<stdin>";
    private final String NEXT_PROMPT = ">>> ";
    private final String CONT_PROMPT = "... ";
    private final String WELCOME = InteractiveConsole.getDefaultBanner()
        + "\n" + NEXT_PROMPT;

    private InteractiveInterpreter jython;
    private volatile Thread serverThread;
    private final Object runLock;

    public RemotePy(MethodContext ctx) {
        this.ctx = ctx;

        this.serverThread = null;
        this.runLock = new Object();

        this.jython = new InteractiveInterpreter();
        this.jython.exec("0"); // Dummy exec in order to speed up response on first command
    }

    public synchronized void startServer(final int port) {
        final boolean started = isStarted();
        if (started) {
            return;
        }

        jython.set("ctx", this.ctx);

        serverThread = new Thread(new Runnable() {
            public void run() {
                startServerImpl(port);
            }
        }, "JythonServer");

        serverThread.setDaemon(true);
        serverThread.start();
    }

    public boolean isStarted() {
        return serverThread != null;
    }

    public synchronized void stopServer() {
        if (serverThread != null) {
            jython.cleanup();
            serverThread.interrupt();
        }
    }

    private void startServerImpl(final int port) {
        try {
            final ServerSocket server = new ServerSocket(port);
            try {
                while (true) {
                    final Socket connection = server.accept();
                    handleConnection(connection);
                }
            } catch (final IOException e) {
                server.close();
                e.printStackTrace();
            }

        } catch (final IOException e) {
            e.printStackTrace();
        }
    }

    public void handleConnection(final Socket connection) {
        new Thread(new Runnable() {

            public void run() {
                try {
                    final OutputStream outputStream = connection.getOutputStream();
                    final InputStream inputStream = connection.getInputStream();
                    outputStream.write(WELCOME.getBytes());

                    final StringBuilder codeBuilder = new StringBuilder();
                    while (true) {
                        try {
                            final InputStreamReader inputStreamReader = new InputStreamReader(
                                    inputStream);
                            final BufferedReader bufferedReader = new BufferedReader(
                                    inputStreamReader);
                            final String line = bufferedReader.readLine();

                            final boolean more;
                            if (line != null) {
                                synchronized (runLock) {
                                    jython.setOut(outputStream);
                                    jython.setErr(outputStream);

                                    if (codeBuilder.length() > 0) {
                                        codeBuilder.append("\n");
                                    }
                                    codeBuilder.append(line);

                                    final String code = codeBuilder.toString();
                                    more = jython.runsource(code, STD_IN);

                                    if (!more) {
                                        codeBuilder.setLength(0);
                                    }
                                }
                            } else {
                                more = false;
                            }

                            final String prompt = more ? CONT_PROMPT
                                    : NEXT_PROMPT;
                            final byte[] promptBytes = prompt.getBytes();
                            outputStream.write(promptBytes);
                            outputStream.flush();
                        } catch (final IOException e) {
                            break;
                        }
                    }

                    inputStream.close();
                    outputStream.close();
                    connection.close();
                } catch (final IOException e) {
                    e.printStackTrace();
                } catch (final PyException e) {
                    if (!e.match(Py.SystemExit)) {
                        throw e;
                    }
                }
            }
        }, "JythonConnection").start();
    }

}
