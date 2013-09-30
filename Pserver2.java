import java.lang.*;
import java.io.*;
import java.net.*;
import java.util.Scanner;

public class Pserver2 implements Runnable
{
	public String username;
	protected Thread thread = new Thread(this, "Server");

	public Pserver2 (String username)
	{
		this.username = username;
	}


	public void run()
	{
	try
		{
		int port = 1235;
		Scanner in = new Scanner(System.in);
		//System.out.print("Username?");
		//String username = in.nextLine() +": ";		
		System.out.println("");
		ServerSocket svr = new ServerSocket(port);
		Socket sckt = svr.accept();
		PrintWriter send = new PrintWriter(sckt.getOutputStream(), true);
		System.out.println("Connected...");
		System.out.println("");
		String mssg = "";
		while(mssg != "/exit")
			{
			mssg = in.nextLine().trim();
			send.print(username + mssg + "\n");
			send.flush();
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