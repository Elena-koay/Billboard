package ControlPanel;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.*;
import java.net.ConnectException;
import java.net.Socket;
import java.net.UnknownHostException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.swing.plaf.synth.SynthTextAreaUI;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class AccessBillboards extends JFrame implements ActionListener {
    int port;
    String[] tmp;
    String[] user;
    String  SessionToken;
    String Values,username;
    private static String hostName;
    Boolean continues = false;
    Boolean displaylist= false;
    Object[][] row={};
    Object[] column={"Billboard","Creator"};
    DefaultTableModel model = new DefaultTableModel(row,column);
    String bname="",creator="";

    Login log = new Login();


    Container c = getContentPane();
    JTable Table = new JTable(model);

    JScrollPane Pane = new JScrollPane(Table);
    JLabel Listbillboards = new JLabel("List of Billboards");

    JButton Back = new JButton("Back");
    JButton Access = new JButton("List");



    JButton Logout = new JButton("Logout");


    AccessBillboards() {
        setLayoutManager();
        setBounds();
        add();
        addactionEvent();

    }

    public void setLayoutManager() {
        c.setLayout(null);
    }

    public void add() {
        c.add(Listbillboards);
        c.add(Pane);
        c.add(Back);
        c.add(Logout);
        c.add(Access);

    }

    public void setBounds() {
        Listbillboards.setFont(new Font("Arial ", Font.BOLD, 18));
        Listbillboards.setBounds(10,5,200,30);
        Access.setBounds(230,5,75,30);
        Pane.setBounds(10, 50, 770, 500);
        Back.setBounds(10,580,100,30);
        Logout.setBounds(310,5,75,30);
        Table.setRowHeight(40);

    }
    public void addactionEvent(){
        Back.addActionListener(this);
        Access.addActionListener(this);

        Logout.addActionListener(this);
    }
    public void connecttoServer() {

        Login log = new Login();
        String SessionToken = log.getSessionToken();
        Socket s = log.getSocket();
        //Writing to server
        PrintWriter instruction = null;
        try {
            instruction = new PrintWriter(s.getOutputStream(), true);
        } catch (IOException ioException) {
            ioException.printStackTrace();
        }
        instruction.println(SessionToken);
        instruction.flush();
        //Reading the response
        InputStreamReader response = null;
        try {
            response = new InputStreamReader(s.getInputStream());
        } catch (IOException ioException) {
            ioException.printStackTrace();
        }
        BufferedReader buffer = new BufferedReader(response);
        String answer = null;
        while (true) {
            try {
                if (!((answer = buffer.readLine()) != null)) break;
            } catch (IOException ioException) {
                ioException.printStackTrace();
            }
        }
        if (buffer.equals(null)) {
            try {
                buffer.close();
                response.close();
            } catch (IOException ioException) {
                ioException.printStackTrace();
            }
            instruction.close();
        }
        //Checks for invalid token error
        if (answer.contains("Invalid Token")) {
            try {
                s.close();
            } catch (IOException ioException) {
                ioException.printStackTrace();
            }
            Login l = new Login();
            l.setBackground(Color.BLACK);
            l.setForeground(Color.WHITE);
            l.setBounds(10, 10, 370, 600);
            l.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            l.setVisible(true);
            l.setTitle("Billboard Control Panel Login");
        }
        //checks for other errors
        else if (answer.contains("ERR")) {
            JOptionPane.showMessageDialog(this, answer);
        } else {


            Pattern p = Pattern.compile("(?<=\\bbillboard_name:\\b).*?(?=\\b:create_user\\b)");
            Matcher m = p.matcher(answer);

            Pattern p1 = Pattern.compile("(?<=\\b:create_user:\\b).*?(?=\\b:edit_user\\b)");
            Matcher m1 = p1.matcher(answer);

            List<String> names = new ArrayList<String>();
            List<String> creator = new ArrayList<String>();
            while (m.find()) {
                names.add(m.group());

            }
            while (m1.find()) {
                creator.add(m1.group());
            }

            for (int i = 0; i < names.size(); i++) {
                String billboard = names.get(i);
                String create = creator.get(i);
                model.addRow(new Object[]{billboard, create});

            }
        }
    }


    @Override
    public void actionPerformed(ActionEvent e) {
        String buttonString = e.getActionCommand();
        if(buttonString.equals("List")){
            String answer = "";

            Socket socket = null;
            String currentDirectory = System.getProperty("user.dir");
            BufferedReader input = null;
            PrintWriter output = null;
            try (InputStream client_properties = new FileInputStream(currentDirectory+"/client.props")) {
                Properties client_props = new Properties();
                // load a properties file
                client_props.load(client_properties);
                // get the port property value
                port = Integer.parseInt(client_props.getProperty("srv.port"));
                hostName = client_props.getProperty("srv.hostname").toString();
                try {
                    System.out.println("Connecting to Server:"+hostName+" port:"+port);
                    SessionToken = log.getSessionToken();
                    socket = new Socket(hostName, port);
                    output = new PrintWriter(socket.getOutputStream(), true);
                    input = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                    output.println(SessionToken);
                    output.println("billboard:list");
                    while (((answer = input.readLine()) != null) && (!answer.equals("END_MESSAGE"))) {
                        if (answer.equals("ACK")) {
                            displaylist = true;
                        } else if (answer.contains("ERR")) {
                            displaylist = false;
                        }
                    }
                    while (((answer = input.readLine()) != null) && (!answer.equals("END_MESSAGE"))) {

                        if (answer.contains("ERR")) {
                            displaylist = false;
                        } else {
                            displaylist=true;
                            String pattern1 = "billboard_name:";
                            String pattern2 = ":create_user";
                            Pattern p = Pattern.compile(Pattern.quote(pattern1) + "(.*?)" + Pattern.quote(pattern2));
                            Matcher m = p.matcher(answer);
                            while (m.find()) {
                                bname = bname + m.group(1)+",";
                            }
                            String pattern3 = "create_user:";
                            String pattern4 = ":edit_user";
                            Pattern p1 = Pattern.compile(Pattern.quote(pattern3) + "(.*?)" + Pattern.quote(pattern4));
                            Matcher m2 = p1.matcher(answer);
                            while (m2.find()) {
                                creator = creator + m2.group(1)+",";
                            }
                        }

                    }
                    if(!displaylist){
                        JOptionPane.showMessageDialog(this, "ERR: Invalid Permission");

                    }
                    else{
                        System.out.println(bname);
                        tmp = bname.split(",");
                        user = creator.split(",");
                        System.out.println(tmp);
                        for (int i = 0; i < tmp.length; i++) {
                            model.addRow(new Object[]{tmp[i], user[i]});
                        }


                    }
                } catch (UnknownHostException Se) {
                    System.err.println("Unknown host: " + hostName);
                    System.exit(1);
                } catch (ConnectException Se) {
                    System.err.println("Connection refused by host: " + hostName);
                    System.exit(1);
                } catch (IOException Se) {
                    Se.printStackTrace();
                } catch (NullPointerException Se) {
                    System.out.println("NullPointerException thrown!");
                }
                // finally, close the socket and decrement runningThreads
                finally {
                    System.out.println("closing");
                    try {
                        input.close();
                        output.close();
                        socket.close();
                        System.out.flush();
                    } catch (IOException Se) {
                        System.out.println("Couldn't close socket");
                    }
                }
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        }

        if (buttonString.equals("Back")) {
            setVisible(false);
            JFrame listBillboard = new ListBillboard();
            listBillboard.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            listBillboard.setBounds(20, 20, 400, 600);
            listBillboard.setVisible(true);
            listBillboard.setTitle("List Billboards");
        }

        if (buttonString.equals("Logout")) {

            String currentDirectory = System.getProperty("user.dir");
            username = log.getUsername();
            SessionToken = log.getSessionToken();
            Socket s = log.getSocket();
            BufferedReader input = null;
            PrintWriter output = null;
            try (InputStream client_properties = new FileInputStream(currentDirectory + "/client.props")) {
                Properties client_props = new Properties();
                // load a properties file
                client_props.load(client_properties);

                // get the port property value
                port = Integer.parseInt(client_props.getProperty("srv.port"));
                hostName = client_props.getProperty("srv.hostname").toString();

                try {
                    s = new Socket(hostName, port);
                    output = new PrintWriter(s.getOutputStream(), true);
                    input = new BufferedReader(new InputStreamReader(s.getInputStream()));
                    SessionToken = log.getSessionToken();
                    output.println(SessionToken);
                    output.println("user:logout");
                    String answer = "";
                    while (((answer = input.readLine()) != null) && (!answer.contains("END_MESSAGE"))) {

                        if (answer.equals("Sucess: Logged Out")) {
                            continues= true;
                        }
                        else if (answer.contains("ERR")){
                            continues = false;
                        }
                    }
                    while (((answer = input.readLine()) != null) && (!answer.contains("END_MESSAGE"))) {

                        if (answer.contains("ERR")){
                            continues = false;
                        }
                        else{
                            continues = true;
                        }
                    }
                    if (!continues) {
                        if(answer.equals("ERR: Invalid Permission!")){
                            JOptionPane.showMessageDialog(this,"ERR: Invalid Permission!");
                        }
                    } else {
                        JOptionPane.showMessageDialog(this, "Logout Succesfutl");
                        s.close();
                        dispose();
                        Login login = new Login();
                        login.setBackground(Color.BLACK);
                        login.setForeground(Color.WHITE);
                        login.setBounds(10, 10, 370, 600);
                        login.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
                        login.setVisible(true);
                        login.setTitle("Billboard Control Panel Login");

                    }
                } catch (UnknownHostException Se) {
                    System.err.println("Unknown host: " + hostName);
                    System.exit(1);
                } catch (ConnectException Se) {
                    System.err.println("Connection refused by host: " + hostName);
                    System.exit(1);
                } catch (IOException Se) {
                    Se.printStackTrace();
                } catch (NullPointerException Se) {
                    System.out.println("NullPointerException thrown!");
                }
                // finally, close the socket and decrement runningThreads
                finally {
                    System.out.println("closing");
                    try {
                        input.close();
                        output.close();
                        s.close();
                        System.out.flush();
                    } catch (IOException Se) {
                        System.out.println("Couldn't close socket");
                    }
                }
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        }
    }
}