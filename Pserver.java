import java.lang.*;
import java.io.*;
import java.net.*;
import java.util.Scanner;
import org.teleal.cling.UpnpService;
import org.teleal.cling.UpnpServiceImpl;
import org.teleal.cling.support.model.PortMapping;
import org.teleal.cling.support.igd.PortMappingListener;

public class Pserver implements Runnable
{

	public int port = 1234;
	public String username;
	protected Thread thread = new Thread(this, "Server");

	public Pserver (String username)
	{
		this.username = username;
	}

	public Pserver (String username, int portnum)
	{
		this.username = username;
		port = portnum;
	}

	public void run(
)	{ try{
		PortMapping desiredMapping =
	    new PortMapping(port, InetAddress.getLocalHost().getHostAddress(), PortMapping.Protocol.TCP, "Pchat Port Hole");
		UpnpService upnpService = new UpnpServiceImpl(new PortMappingListener(desiredMapping));
		upnpService.getControlPoint().search();
		Scanner in = new Scanner(System.in);
		//System.out.print("Username? ");
		//String username = in.nextLine() +": ";
		System.out.println("");
		ServerSocket svr = new ServerSocket(port);
		Socket sckt = svr.accept();
		PrintWriter send = new PrintWriter(sckt.getOutputStream(), true);
		System.out.println("Connected...");
		System.out.println("");
		String mssg = "";
		Boolean done = false;
		while(!done)
			{
			mssg = in.nextLine().trim();
			send.print(username + mssg + "\n");
			send.flush();
			if (mssg.equals("/exit"))
			{
				done = true;
			}
			}
		send.close();
	    sckt.close();
	    svr.close();
	 	System.out.println("Disconnected");
	 	}
	catch(Exception e) 
 		{
         System.out.println("Server Error!");
      	}

  	}
  	protected void start()
  	{
  		thread.start();
  	}

  	
}