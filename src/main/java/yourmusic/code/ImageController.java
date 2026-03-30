package yourmusic.code;

import yourmusic.Controller;

import javax.swing.*;
import javax.swing.filechooser.FileSystemView;
import java.io.File;

public class ImageController {
    public static Icon getIconFile(File path){
        Icon icon = FileSystemView.getFileSystemView().getSystemIcon(path);

        if(icon instanceof ImageIcon){
            java.awt.Image awtImage = ((ImageIcon) icon).getImage();
            return icon;
        }
        return (Icon) Controller.mainImg;
    }



}
