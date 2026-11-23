package bookbridge.app;
import bookbridge.dao.Database;
import bookbridge.ui.BookBridgeGUI;
import javax.swing.*;
public final class Main {public static void main(String[] args){
 try{Database.initialize();}catch(Exception e){System.err.println("Database setup failed: "+e.getMessage());e.printStackTrace();return;}
 SwingUtilities.invokeLater(()->new BookBridgeGUI().setVisible(true));
}}
