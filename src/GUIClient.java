/*Copyright (C) 2022  Riley Kuttruff
*
*   This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or
*   (at your option) any later version.
*
*   This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General
*   Public License for more details.
*
*   You should have received a copy of the GNU General Public License along with this program.  If not, see <https://www.gnu.org/licenses/>.
*
*/

import java.awt.*;
import java.awt.event.*;

import java.util.*;
import java.util.List;
import java.util.concurrent.*;

import javax.swing.*;
import javax.swing.event.*;
import javax.swing.text.JTextComponent;

//The GUI
/**
 * Implementation of the GUI client.
 *
 *  @author     Riley Kuttruff
 *  @version    1.0
 */
class GUIClient extends JFrame{
    /**Holds session data for this client*/
    private ClientData data;
    /**@hidden*/
    private GUIClient frm;
    
    /**@hidden*/
    private JTextField toField, subField;
    /**@hidden*/
    private JTextArea body;
    /**@hidden*/
    private JButton sendButton, cancelButton;
    
    //When the JOptionPane containing a JPasswordField is displayed, one of the buttons on the JOptionPane is focused by default.
    //I found this code online to set the focus to the JPasswordField (or whatever component into which this listener is installed)
    //in order to allow the user to immediately type thier password in and not have to click the field.
    /**@hidden*/
    static final HierarchyListener PASS_JOP_LISTENER = new HierarchyListener(){
        @Override
        public void hierarchyChanged(HierarchyEvent e){
            if(e.getComponent().isShowing() && (e.getChangeFlags() & HierarchyEvent.SHOWING_CHANGED) != 0)
                SwingUtilities.invokeLater(e.getComponent()::requestFocusInWindow);
        }
    };
    
    /**
     * Constructor
     * <p>
     * Builds client window and displays it, storing session information into the argument object.
     * 
     * @param data {@link ClientData} object to store session data
     */
    private GUIClient(ClientData data){
        super("SMTP Client");
        this.data = data;
        frm = this;
        
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        
        JPanel main = panel();
        main.setLayout(new BoxLayout(main, BoxLayout.Y_AXIS));
        
        toField = new JTextField(buildRecipientString(data.recipients));
        toField.setToolTipText("Separate addresses with a semicolon");
        subField = new JTextField(data.subject);
        
        JLabel toLabel = new JLabel("To: ");
        JLabel subLabel = new JLabel("Subject: ");
        
        JPanel fieldPanel = panel();
        
        GroupLayout layout = new GroupLayout(fieldPanel);
        fieldPanel.setLayout(layout);

        //Mostly copied from javax.swing.GroupLayout javadoc
        layout.setAutoCreateGaps(true);
        layout.setAutoCreateContainerGaps(true);

        GroupLayout.SequentialGroup hGroup = layout.createSequentialGroup();

        hGroup.addGroup(layout.createParallelGroup()
                              .addComponent(toLabel)
                              .addComponent(subLabel)
                       );
        hGroup.addGroup(layout.createParallelGroup()
                              .addComponent(toField)
                              .addComponent(subField)
                       );
                       
        layout.setHorizontalGroup(hGroup);

        GroupLayout.SequentialGroup vGroup = layout.createSequentialGroup();

        vGroup.addGroup(layout.createParallelGroup(GroupLayout.Alignment.BASELINE)
                              .addComponent(toLabel)
                              .addComponent(toField)
                       );
        vGroup.addGroup(layout.createParallelGroup(GroupLayout.Alignment.BASELINE)
                              .addComponent(subLabel)
                              .addComponent(subField)
                       );
                       
        layout.setVerticalGroup(vGroup);
        
        main.add(fieldPanel);
        main.add(new JSeparator(SwingConstants.HORIZONTAL));
        
        body = new JTextArea();
        
        body.setLineWrap(true);
        body.setWrapStyleWord(true);
        body.setTabSize(4);
        
        JScrollPane jsp;
        
        jsp = new JScrollPane(body, 
                              ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED,
                              ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        
        main.add(new Box.Filler(new Dimension(5,5),new Dimension(5,5),new Dimension(5,5)));
        
        jsp.setPreferredSize(new Dimension(590, 500));
        
        main.add(jsp);
                                 
        JPanel buttonPanel = panel(new FlowLayout(SwingConstants.RIGHT, 2, 2));
        
        sendButton = new JButton("Send");
        sendButton.addActionListener((e) -> {
            frm.data.done = true;
            
            frm.data.recipients = toField.getText().split(";");
            frm.data.subject = subField.getText();
            
            frm.data.messageLines.addAll(Arrays.asList(body.getText().split("\n", -1)));
            
            frm.dispose();
        });
        sendButton.setEnabled(false);
        
        cancelButton = new JButton("Cancel");
        cancelButton.addActionListener((e) -> {
            frm.dispose();
        });
        
        buttonPanel.add(sendButton);
        buttonPanel.add(cancelButton);
        
        main.add(buttonPanel);
        
        new AllFilledListener(sendButton, toField, subField);
                                 
        add(main);
        add(new Box.Filler(new Dimension(5,5),new Dimension(5,5),new Dimension(5,5)), BorderLayout.EAST);
        add(new Box.Filler(new Dimension(5,5),new Dimension(5,5),new Dimension(5,5)), BorderLayout.WEST);
        
        setMinimumSize(new Dimension(600,500));
        setLocationRelativeTo(null);
        setVisible(true);
        
        if(data.uName == null){
            String ret;
            
            do{
                ret = JOptionPane.showInputDialog(this,
                                                  "Enter username (Ex: user@gmail.com):",
                                                  "Username",
                                                  JOptionPane.PLAIN_MESSAGE);
                                                  
                if(ret != null)
                    ret = ret.trim();
                else{
                    dispose();
                    return;
                }
            }while(ret.length() == 0);
            
            data.uName = ret;
        }
        
        if(data.pass == null){
            char[] pass;
            int ret;
            
            JPanel panel = panel();
            panel.add(new JLabel("Enter your password: "));
            JPasswordField passField = new JPasswordField(15);
            
            passField.addHierarchyListener(PASS_JOP_LISTENER);
            
            panel.add(passField);
            String[] opts = {"OK", "Cancel"};
            
            do{
                ret = JOptionPane.showOptionDialog(this, panel, "Password",
                                                   JOptionPane.NO_OPTION,
                                                   JOptionPane.PLAIN_MESSAGE,
                                                   null, opts, opts[0]);
                                                   
                if(ret == 1){
                    dispose();
                    return;
                }
                else
                    pass = passField.getPassword();
            }while(pass.length == 0);
            
            data.pass = pass;
        }
    }
    
    //Displays the GUI client and blocks until it closes
    /**
     * Displays the GUI client.
     * <p>
     * Blocks until client exits before returning session data.
     * 
     * @param data {@link ClientData} object to store session data
     * @return data Session data stored in {@link ClientData} object
     */
    public static ClientData getMessage(final ClientData data){
        SwingUtilities.invokeLater(() -> new GUIClient(data));
        
        FutureTask<Thread> future = new FutureTask<>(new Callable<Thread>(){
            public Thread call(){
                return Thread.currentThread();
            }
        });
        
        SwingUtilities.invokeLater(future);
        
        Thread EDT = null;
        
        while(EDT == null){
            try{
                EDT = future.get();
            }
            catch(Exception e){}
        }
        
        
        while(EDT.isAlive()){
            try{
                EDT.join();
            }
            catch(Exception e){}
        }
        
        return data;
    }
    
    //JPanel convenience factory method w/ LayoutManager
    /**@hidden*/
    private static JPanel panel(LayoutManager lm){
        return new JPanel(lm);
    }
    
    //JPanel convenience factory method w/o LayoutManager
    /**@hidden*/
    private static JPanel panel(){
        return new JPanel();
    }
    
    /**@hidden*/
    private static String buildRecipientString(String[] rec){
        if(rec == null || rec.length == 0)
            return "";
        
        StringBuilder sb = new StringBuilder(rec[0]);
        
        for(int i = 1; i < rec.length; i++)
            sb.append(";").append(rec[i]);
        
        return sb.toString();
    }
    
    //Only enable the provided JComponent if ALL JTextComponents has text in them. Used to disable the send button until there's the needed data.
    /**@hidden*/
    private static class AllFilledListener implements DocumentListener{
        JTextComponent[] checklist;
        JComponent toEnable;
        
        public AllFilledListener(JComponent toEnable, JTextComponent... comps){
            if(toEnable == null || comps == null)
                throw new NullPointerException();
            
            this.toEnable = toEnable;
            checklist = comps;
            
            for(JTextComponent jtc : checklist)
                jtc.getDocument().addDocumentListener(this);
        }
        
        private void update(){
            boolean filled = true;
            
            for(JTextComponent jtc : checklist){
                filled = jtc.getDocument().getLength() > 0;
                
                if(!filled)
                    break;
            }
            
            toEnable.setEnabled(filled);
        }
        
        @Override
        public void changedUpdate(DocumentEvent e){
            update();
        }
        
        @Override
        public void insertUpdate(DocumentEvent e){
            update();
        }
        
        @Override
        public void removeUpdate(DocumentEvent e){
            update();
        }
        
    }
    
    /**
     * Container class for important data used by the client.
     * <p>
     * Passes back to {@link SMTPClient} data such as username, recipients, message subject and message 
     * text.
     *
     *  @author     Riley Kuttruff
     *  @version    1.0
     */
    public static class ClientData{
        /**If the data contained is complete and ready to be passed to the SMTP server*/
        private boolean done;
        
        /**Sender's username*/
        public String uName;
        /**User password*/
        public char[] pass;
        /**Recipient's username(s)*/
        public String[] recipients;
        
        /**Message subject*/
        public String subject;
        
        /**List of each line of message text*/
        public List<String> messageLines;
        
        /**
         * Default constructor
         */
        public ClientData(){
            uName = null;
            pass = null;
            recipients = null;
            
            subject = null;
            
            messageLines = new ArrayList<>();
            
            done = false;
        }
        
        /**
         * Returns if the data contained is complete and ready to be passed to the SMTP server
         */
        public boolean isDone(){
            return done;
        }
    }
}