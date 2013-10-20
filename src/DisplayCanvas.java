import java.io.IOException;
import javax.microedition.lcdui.Canvas;
import javax.microedition.lcdui.Command;
import javax.microedition.lcdui.CommandListener;
import javax.microedition.lcdui.Display;
import javax.microedition.lcdui.Displayable;
import javax.microedition.lcdui.Graphics;
import javax.microedition.lcdui.Image;

/**
 * @author Oleg Podsechin
 * @version 1.0
 */
public class DisplayCanvas extends Canvas
        implements CommandListener {
    
    
    private static DisplayCanvas instance = new DisplayCanvas();
    
    private Command scanCommand;
    private Command backCommand;
    
    //*** display variables
    protected byte[] imageBytes;
    protected Image image;
    private Image waitImage;
    
    private DisplayCanvas() {
//        instance = this;
        
        scanCommand = new Command(App.textStrings[App.STRING_SCAN], Command.OK, 1);
        backCommand = new Command(App.textStrings[App.STRING_BACK], Command.BACK, 2);
        
        addCommand(scanCommand);
        addCommand(backCommand);

        setCommandListener(this);

        try {
            waitImage = Image.createImage(getClass().getResourceAsStream("wait.png"));
        } catch (IOException ex) {
            // nothing we can do about it
            waitImage = null;
        }
    }
    
    public static DisplayCanvas getInstance() {
        return instance;
    }
    
    
    void setImage(byte[] pngImage, byte[] pngImageLow) {
        imageBytes = pngImage;
        image = Image.createImage(pngImageLow, 0, pngImageLow.length);
    }
    
    void setImage(int[] intImage, int width, int height) {
        image = Image.createRGBImage(intImage, width, height, false);
    }
    
    void clearImage() {
        image = null;
    }
    
    public void paint(Graphics g) {
        int screenWidth = getWidth(), screenHeight = getHeight();
        
        g.setColor(App.backgroundColor);
        g.fillRect(0,0,screenWidth,screenHeight);
        if(image != null) {
            g.drawImage(image, screenWidth/2,screenHeight/2, Graphics.VCENTER | Graphics.HCENTER);
            if(ConnectionState.currentState > 0) {
                final int barHeight = 10;
                
                g.setColor(0xFF0000);
                g.fillRect(0, screenHeight - barHeight, (screenWidth * ConnectionState.currentState) / 4, barHeight);
                g.setColor(0x000000);
                g.drawRect(0, screenHeight - barHeight, screenWidth, barHeight);
            }

//            g.setColor(0x00FF00FF);
//            g.drawString(CaptureCanvas.getInstance().selectedImageEncoding, 0, 0, Graphics.TOP | Graphics.LEFT);
        } else {
            g.drawImage(waitImage, screenWidth/2,screenHeight/2, Graphics.VCENTER | Graphics.HCENTER);
        }
    }
    
    public void commandAction(Command c, Displayable d) {
        if(c == scanCommand)
            scanImage();
        else if(c == backCommand)
            App.getInstance().displayCanvasBack();
    }
    
    public void keyPressed(int keyCode) {
        int code = getGameAction(keyCode);
        if(code == FIRE || code == KEY_NUM5)
            scanImage();
    }
    
    private void scanImage() {
        //String encoding = System.getProperty("video.snapshot.encodings");
        ConnectionState.sendPacket(ConnectionState.PACKET_LOGIN);
    }
    
}
