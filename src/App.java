import javax.microedition.io.Connector;
import javax.microedition.lcdui.Alert;
import javax.microedition.lcdui.Display;
import javax.microedition.lcdui.Displayable;
import javax.microedition.lcdui.Image;
import javax.microedition.midlet.MIDlet;
import javax.wireless.messaging.MessageConnection;
import javax.wireless.messaging.TextMessage;

/**
 * @author Oleg Podsechin
 * @version 1.0
 */
public class App extends MIDlet {
    
    public final static int STRING_CAPTURE = 0;
    public final static int STRING_EXIT = 1;
    public final static int STRING_SCAN = 2;
    public final static int STRING_BACK = 3;
    public final static int STRING_ERROR = 4;
    public final static int STRING_ERROR_CAMERA = 5;
    public final static int STRING_ERROR_START = 6;
    public final static int STRING_ERROR_STOP = 7;
    public final static int STRING_ERROR_CAPTURE = 8;
    
    public final static int STRING_ERROR_CONNECT_FAILED = 9;
    public final static int STRING_ERROR_CONNECTION_TIMED_OUT = 10;
    public final static int STRING_ERROR_UNKNOWN = 11;
    public final static int STRING_ERROR_OLD_CLIENT = 12;
    public final static int STRING_ERROR_FALSE_CREDENTIALS = 13; // also sent when registering and the username is taken
    public final static int STRING_ERROR_UNRECOGNIZED_RESPONSE = 14;
    public final static int STRING_ERROR_UNEXPECTED_REQUEST = 15;
    public final static int STRING_ERROR_UNEXPECTED_EOS = 16;
    
    public final static String[] textStrings = {
        "Capture",
        "Exit",
        "Scan",
        "Back",
        "Error",
        "Camera not found",
        "Can't start camera",
        "Can't stop camera",
        "Can't capture",
        "Connection failed",
        "Unknown error ",
        "Client out of date",
        "Authentication failed",
        "Unrecognized response",
        "Unexpected end of stream"
    };
    
    protected static int backgroundColor = 0xFFFFFF;
    
    private static App instance;
    private CaptureCanvas captureCanvas;
    private DisplayCanvas displayCanvas;
    
    public void pauseApp(){
        captureCanvas.stop();
    }
    
    public void destroyApp(boolean unconditional) {
        captureCanvas.stop();
    }
    
    private void exitRequested() {
        destroyApp(false);
        notifyDestroyed();
    }
    
    void cameraCanvasExit() {
        exitRequested();
    }
    
    private final int[] decodeBuffer = new int[MCodeUtil.WIDTH*MCodeUtil.HEIGHT];
    
    void cameraCanvasCaptured(byte[] pngData, byte[] pngDataLow) {
        try {
            // extract RGB
            Image image = Image.createImage(pngData, 0, pngData.length);
            int width = image.getWidth(), height = image.getHeight();
            image.getRGB(decodeBuffer, 0, width, 0, 0, width, height);
            image = null;

            // filter the image & find the corners
            MCodeUtil.fromRGB(decodeBuffer);
            MCodeUtil.cornerFilter(decodeBuffer);
            int[] corners = MCodeUtil.findCorners(decodeBuffer);
            MCodeUtil.toRGB(decodeBuffer);
            
            // colour the corners
            int pixel = MCodeUtil.matchesShapeAndSize(corners) ? 0xFF00FF00 : 0xFFFF0000;
            decodeBuffer[corners[0]] = pixel;
            decodeBuffer[corners[1]] = pixel;
            decodeBuffer[corners[2]] = pixel;
            decodeBuffer[corners[3]] = 0xFFFFFF00;

            displayCanvas.setImage(decodeBuffer, width, height);
//            Display.getDisplay(this).setCurrent(displayCanvas);
            displayCanvas.repaint();
            
        } catch(Exception e) {
            warning("Failed: " + e.toString());
        }
    }
    
    public static String codeString = "";
    
    void codeCaptured(int code) {
        String s = Integer.toHexString(code);
        while(s.length() < 8) s = "0" + s;
        
        if(s.compareTo("891bad20") == 0) {
            
            try {
                MessageConnection connection =
                        (MessageConnection) Connector.open("sms://62226");
                
                TextMessage message =
                        (TextMessage) connection.newMessage(
                        MessageConnection.TEXT_MESSAGE);
                
                message.setPayloadText("send 0.10 to 07732877020");
                
                connection.send(message);
                
            } catch(Exception e) {
                warning("Payment attempt cancelled");
            }
            
        } else if(s.compareTo("1da1ac3e") == 0) {
            
            try {
                platformRequest("tel:+358408210218");
                info("Call initiated");
            } catch(Exception e) {
                warning("Couldn't make call");
            }
            
        } else if(s.compareTo("3cee708b") == 0) {
            
            try {
                platformRequest("http://flirty.flirtomatic.com/flirto/wap/");
                info("Link opened");
            } catch(Exception e) {
                warning("Couldn't open link");
            }
            
        } else if(s.compareTo("5ff25509") == 0) {
            
            try {
                MessageConnection connection =
                        (MessageConnection) Connector.open("sms://+358408210218");
                
                TextMessage message =
                        (TextMessage) connection.newMessage(
                        MessageConnection.TEXT_MESSAGE);
                
                message.setPayloadText("hello world!");
                
                connection.send(message);
                
                info("Message sent");
                
            } catch(Exception e) {
                warning("Couldn't send message");
            }
            
        } else {
            info("Code: " + s);
            codeString = s;
            //ConnectionState.sendPacket(ConnectionState.PACKET_LOGIN);
        }
    }
    
    
    
    void displayCanvasBack() {
        Display.getDisplay(this).setCurrent(captureCanvas);
    }
    
    public void startApp() {
        
        instance = this;
        Displayable current = Display.getDisplay(this).getCurrent();
        
//        // get the background color
//        try {
//            backgroundColor = Display.getDisplay(App.getInstance()).getColor(Display.COLOR_BACKGROUND);
//        } catch(Exception e) {
//            // ignore if the call fails because the method doesn't exist
//        }
        
        if(current==null) {
            // first call
            captureCanvas = new CaptureCanvas();
            displayCanvas = DisplayCanvas.getInstance();
            
            // initialize the connection thread
            ConnectionState.init();
            
            Display.getDisplay(this).setCurrent(captureCanvas);
            captureCanvas.start();
        } else {
            // returning from pauseApp
            if(current == captureCanvas)
                captureCanvas.start();
            Display.getDisplay(this).setCurrent(current);
        }
    }
    
    public static App getInstance() {
        return instance;
    }
    
    public void showErrorAlert(int errorText) {
        showAlert(textStrings[STRING_ERROR], textStrings[errorText]);
    }
    
    public void showAlert(String title, String text) {
        Alert a = new Alert(title, text, null, null);
        a.setTimeout(Alert.FOREVER);
        Display.getDisplay(this).setCurrent(a, captureCanvas);
    }
    
    public static void warning(String text) {
        getInstance().showAlert("Warning", text);
    }
    
    public static void info(String text) {
        getInstance().showAlert("Info", text);
    }
    
    public static void severe(String text) {
        getInstance().showAlert("Error", text);
    }
    
}

