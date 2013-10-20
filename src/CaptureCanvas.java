import java.util.Vector;

import javax.microedition.lcdui.Canvas;
import javax.microedition.lcdui.Command;
import javax.microedition.lcdui.CommandListener;
import javax.microedition.lcdui.Display;
import javax.microedition.lcdui.Displayable;
import javax.microedition.lcdui.Graphics;
import javax.microedition.media.Manager;
import javax.microedition.media.MediaException;
import javax.microedition.media.Player;
import javax.microedition.media.control.VideoControl;


/**
 * @author Oleg Podsechin
 * @version 1.0
 */
public class CaptureCanvas extends Canvas implements CommandListener, Runnable {
    
    public final static boolean FAKE_CAPTURE = false;
    
    public final static int[][] PREFERRED_RESOLUTIONS = {
        /*{640, 480}, {320, 240},*/ {160, 120}, {0, 0}
    };
    
    public final static String[] PREFERRED_ENCODINGS = {
        "png", "jpeg"
    };
    
    private static CaptureCanvas instance;
    
    //*** capture variables
    private Command captureCommand;
    private Command exitCommand;
    
    private Player player;
    private VideoControl videoControl;
    
    private boolean active = false;
    private Thread taking = new Thread(this);
    
    public String selectedImageEncoding;
    private int selectedResolution = 0;

    public CaptureCanvas() {
        captureCommand = new Command(App.textStrings[App.STRING_CAPTURE], Command.OK, 1);
        exitCommand = new Command(App.textStrings[App.STRING_EXIT], Command.BACK, 2);
        
        addCommand(captureCommand);
        addCommand(exitCommand);
        
        setCommandListener(this);
        
        createPlayer();
        //selectEncoding();
    }
    
    public static CaptureCanvas getInstance() {
        return instance;
    }
    
    public void selectEncoding() {
        String encodingsString = System.getProperty("video.snapshot.encodings");
        
        // split string
        Vector encodings = new Vector();
        for (int i = 0, j, len = encodingsString.length(); i < len; i = j + 1) {
            j = encodingsString.indexOf(' ', i);
            j = (j == -1) ? len : j;
            encodings.addElement(encodingsString.substring(i, j).toLowerCase());
        }
        // find a match, if any
        for(int i = 0; i < PREFERRED_ENCODINGS.length; i++) {
            for(int j = 0; j < encodings.size(); j++) {
                encodingsString = (String) encodings.elementAt(j);
                if(PREFERRED_ENCODINGS[i].equals(encodingsString)) {
                    selectedImageEncoding = encodingsString;
                    return;
                }
                
            }
        }
        
        // imageFormat may be null, in which case it's omitted from the format string
    }
    
    
    private void discardPlayer() {
        if(player != null){
            player.close();
            player = null;
        }
        videoControl = null;
    }

    public void paint(Graphics g) {
//        g.setColor(App.backgroundColor);
//        g.fillRect(0,0, getWidth(), getHeight());
    }
    
    public void createPlayer() {
        try {
            player=Manager.createPlayer("capture://video");
            player.realize();
            
            videoControl = (VideoControl) player.getControl("VideoControl");
            if(videoControl == null) {
                throw new Exception("control not found");
            } else {
                videoControl.initDisplayMode(VideoControl.USE_DIRECT_VIDEO, this);
                //int x = (getWidth() - videoControl.getDisplayWidth())/2;
                //int y = (getHeight() - videoControl.getDisplayHeight())/2;
                //videoControl.setDisplayLocation(x,y);
                videoControl.setDisplayFullScreen(true);
            }
        } catch(Exception e) {
            discardPlayer();
            App.getInstance().showErrorAlert(App.STRING_ERROR_CAMERA);
        }
    }
    
    public void destroyPlayer() {
        player.deallocate();
    }
    
    synchronized void start() {
        if (!active && player != null) {
            try {
                player.start();
                videoControl.setVisible(true);
                active = true;
            } catch(Exception e) {
                App.getInstance().showErrorAlert(App.STRING_ERROR_START);
            }
        }
    }
    
    synchronized void stop() {
        if(active && player != null) {
            try{
                videoControl.setVisible(false);
                player.stop();
            } catch(MediaException e) {
                App.getInstance().showErrorAlert(App.STRING_ERROR_STOP);
            }
            active = false;
            //destroyPlayer();
        }
        
    }
    
    public void commandAction(Command c, Displayable d) {
        if(c == captureCommand)
            takeSnapshotThread();
        else if(c == exitCommand)
            App.getInstance().cameraCanvasExit();
    }
    
    public void keyPressed(int keyCode) {
        int code = getGameAction(keyCode);
        if(code == FIRE || code == KEY_NUM5)
            takeSnapshotThread();
    }
    
    private synchronized void takeSnapshotThread() {
        if (taking.isAlive())
            return;
        taking = new Thread(this);
        taking.start();
    }
    
    private void takeSnapshot() {
        
        selectEncoding();
        
        //String encodingsString = System.getProperty("video.snapshot.encodings");
        //App.getInstance().showAlert("Info", encodingsString);
        //if(true) return;
        
        if(player != null) {
            
            for(int i = selectedResolution; i < PREFERRED_RESOLUTIONS.length; i ++) {
                try {
                    StringBuffer captureString = new StringBuffer();
                    if(selectedImageEncoding != null)
                        captureString.append("encoding=").append(selectedImageEncoding);
                    
                    if(PREFERRED_RESOLUTIONS[i][0] != 0) {
                        if(captureString.length() != 0)
                            captureString.append("&");
                        captureString.append("width=").append(PREFERRED_RESOLUTIONS[i][0]).append("&height=").append(PREFERRED_RESOLUTIONS[i][1]);
                    }
                    
                    byte[] pngImage;
                    pngImage = videoControl.getSnapshot(captureString.toString()); //"width=640&height=480"
                    DisplayCanvas.getInstance().clearImage();
                    Display.getDisplay(App.getInstance()).setCurrent(DisplayCanvas.getInstance());

                                /*
                                        InputStream in = getClass().getResourceAsStream("/icon2.png");
                                        //System.out.println(in.available());
                                        pngImage = new byte[1756];
                                        in.read(pngImage);
                                        in.close();
                                 */
                    
                    if(pngImage != null) {
                        selectedResolution = i;
                        
                        byte[] pngImageLow = pngImage;
                        if(PREFERRED_RESOLUTIONS[i][0] != 0) {
                            pngImageLow = pngImage;
                            // take a second, lower res image to display
                            //pngImageLow = videoControl.getSnapshot(null);
                        }
                        if(!FAKE_CAPTURE)
                            App.getInstance().cameraCanvasCaptured(pngImage, pngImageLow);
                        else {
                            String info = String.valueOf(pngImage.length) +
                                    ": " + PREFERRED_RESOLUTIONS[i][0] + "x" + PREFERRED_RESOLUTIONS[i][1] + ": " + selectedImageEncoding;
                            App.getInstance().showAlert("Info", info);
                        }
                        return;
                    }
                    
                } catch(Exception e) { //MediaException
                    //System.out.println(e.toString());
                    //App.getInstance().showAlert("Error", e.toString());
                    continue;
                }
            }
        }
        
        App.getInstance().showErrorAlert(App.STRING_ERROR_CAPTURE);
    }

    public void run() {
        takeSnapshot();
    }   
}