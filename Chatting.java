import java.lang.*;
import java.io.*;
import java.net.*;
import java.util.Scanner;
import org.teleal.cling.*;
import org.teleal.cling.support.model.PortMapping;
import org.teleal.cling.support.igd.PortMappingListener;
import java.net.*;
import java.io.*;

public class Chatting
{
	public static void main (String [] args)
	{

		try{
		URL whatismyip = new URL("http://automation.whatismyip.com/n09230945.asp");
		BufferedReader readerin = new BufferedReader(new InputStreamReader(whatismyip.openStream()));
		String mip = readerin.readLine();

		System.out.println("Your IP Address: " + mip);
		}
	catch(Exception e)
	{
		System.out.println("Could not retreive Extermal IP");
	}
		try{

		Scanner in = new Scanner(System.in);
		Pclient client;

		if (args.length > 1)
		{
			//if (args.length > 0)
			//{
				client = new Pclient(Integer.parseInt(args[0]), args[1]);
			//}
			
		}
		else
		{
			System.out.print("IP to Connect to?");
			String ip = in.nextLine() +": ";
			System.out.println("");
			client = new Pclient(1234, ip);
		}

		System.out.print("Username? ");
		String username = in.nextLine() +": ";
		Pserver server;
		if (args.length > 0)
		{
			server = new Pserver(username, Integer.parseInt(args[0]));
		}
		else
		{
			server = new Pserver(username);
					}
		server.start();
		client.start();
		try
		{
			server.thread.join();
			client.interrupt();
			System.exit(0);
		}
		catch(Exception e)
		{
			System.out.print("Error!");
		}
	}
	catch(Exception e)
	{

	}
}
}