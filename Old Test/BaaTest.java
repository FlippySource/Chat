import java.lang.*;
import java.util.*;
import java.io.*;
import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import java.net.*;

public class BaaTest implements Runnable
{
	Thread thread = new Thread(this, "a");
	
	public void run()
	{
		for(int i = 0; i <500; i++)
	{
		System.out.println ("Bye");

	}
	}
	public void start()
	{
		thread.start();
	}
}