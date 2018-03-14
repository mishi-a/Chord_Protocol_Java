import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.File;
import java.util.ArrayList;
import java.io.UnsupportedEncodingException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.Scanner;
import javax.xml.bind.DatatypeConverter;
import java.math.BigInteger;
import java.net.InetAddress;
//A chord implementation with m=5 i.e 32 node maxm

class HandleRequest implements Runnable 
{
	public HandleRequest()
	{
		// TODO Auto-generated constructor stub
	}

	public void run()
	{
		ServerSocket server = null;
		int port = node.nodePort;
		try {
			server = new ServerSocket(port);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		while(true)
		{
			Socket client = null;
			InputStream in = null;
			OutputStream out = null;
			try
			{
				client = server.accept();
				System.out.println("****Some one is Requesting something****");
				out = client.getOutputStream();
				in = client.getInputStream();
				byte[] buf = new byte[1024];
				in.read(buf);
				String request = new String(buf);
				request = request.trim();
				
				String[] data = request.split(":");
				System.out.println("**** Requested msg is "+request+"****");
				if(data[0].equals("find_successor"))
				{
					//System.out.println("hi");
					int id = Integer.parseInt(data[1]);
					int po = Integer.parseInt(data[2]);
					int flag = Integer.parseInt(data[3]);
					//System.out.println("find suc "+id);
					String suc = node.find_successor(id,po,flag);
					System.out.println("**** Sending Response ****");
					out.write(suc.getBytes());
					out.close();
				}
				else if(data[0].equals("closest_preceeding"))
				{
					int id = Integer.parseInt(data[1]);
					int cPre = Fingers.closest_preceeding_finger(id);
					String cPreced = ""+cPre;
					System.out.println("**** Sending Response ****");
					out.write(cPreced.getBytes());
					out.close();
				}
				else if(data[0].equals("successor"))
				{
					int suc = node.successorPort;
					int old = node.successorId;
					int flag = Integer.parseInt(data[3]);
					int id = Integer.parseInt(data[2]);
					int p1 = Integer.parseInt(data[1]);
					int n = Chord.SHA1("localhost:"+p1);
					boolean flag1 =false;
					if((id)%32<=(old-1+32)%32)
						flag1 = node.isInterval((id)%32,(old-1+32)%32,n);
					else
					{
						flag1 = node.isInterval((id)%32,31,n);
						if(flag1 == false)
							flag1 = node.isInterval(0,(old-1+32)%32,n);
					}
					System.out.println("flag1 = "+flag1+" "+id+" "+ n+" "+old);
					if(flag1)
					{
						old = n;
						suc = p1;
					}
					if(id == node.successorId)
            		{
            			suc = node.successorPort;
            			old = node.successorId;
            		}
            		if(flag==0)
            			suc = node.successorPort;
				    System.out.println("**** Sending Response ****");
					String p = ""+suc;
					out.write(p.getBytes());
					out.close();
				}
				else if(data[0].equals("predecessor"))
				{
					int pre = node.predecessorPort;
					String p = ""+pre;
					System.out.println("**** Sending Response ****");
					int updPre = Integer.parseInt(data[1]);
					node.predecessorPort = updPre;
					node.predecessorId = Chord.SHA1("localhost:"+updPre);
					if(data.length == 3&& Integer.parseInt(data[2])!=node.nodePort)
						Request.makeRequest(Integer.parseInt(data[2]),"transfer_files:"+0+":"+32);
					out.write(p.getBytes());
					out.close();
				}
				else if(data[0].equals("update"))
				{
					//update finger table
					//System.out.println("node port "+node.successorId+ " "+node.successorPort+" "+node.nodeId);
					//System.out.println("kkkk "+data[1]);
					int s = Integer.parseInt(data[1]);
					int i = Integer.parseInt(data[2]);
					int z = Fingers.table[i][3];
					boolean flag =false;
					int k = 0;
					if((node.nodeId+1)%32<=(z-1+32)%32)
						flag = node.isInterval((node.nodeId+1)%32,(z-1+32)%32,s);
					else
					{
						flag = node.isInterval((node.nodeId+1)%32,31,s);
						if(flag == false)
							flag = node.isInterval(0,(z-1+32)%32,s);
					}
					if(z==(node.nodeId+1)%32)
						flag = false;
					if(flag&&s!=node.nodeId&&data.length==4)
					{
						k = 1;
						if(data.length==4)
						{
							Fingers.table[i][3]=s;
							Fingers.table[i][4] = Integer.parseInt(data[3]);
						}
						if(i==0)
						{
							node.successorId = s;
							node.successorPort = Integer.parseInt(data[3]);
						}
						int prePort = node.predecessorPort;
						if(prePort != node.nodePort&&prePort!=Integer.parseInt(data[3]))
							Request.makeRequest(prePort,"update:"+s+":"+i+":"+data[3]);
					}
					if(s-(z)==0 && data.length==5)
					{
							Fingers.table[i][3]=Chord.SHA1("localhost:"+data[4]);
							Fingers.table[i][4] = Integer.parseInt(data[4]);
							if(i==0)
							{
								node.successorId =Chord.SHA1("localhost:"+data[4]);
								node.successorPort = Integer.parseInt(data[4]);
							}
							int prePort = node.predecessorPort;
							if(prePort != node.nodePort&&prePort!=Integer.parseInt(data[3]))
								Request.makeRequest(prePort,"update:"+s+":"+i+":"+data[3]+":"+data[4]);
					}
				    System.out.println("**** Sending Response ****");
					out.write(" ".getBytes());
					out.close();
				}
				else if(data[0].equals("transfer_files"))
				{
					int start = Integer.parseInt(data[1]);
					int end = Integer.parseInt(data[2]);
					File folder = new File("./nodeFile");
					File[] allFiles = folder.listFiles();
					ArrayList<File> transferFiles = new ArrayList<File>();
					int count = 0;
					for(File file:allFiles)
					{
						String filename = file.getName();
						int key = Chord.SHA1(filename);
						boolean flag =false;
						if(start<=end)
								flag = node.isInterval(start,end,key);
						else
						{
								flag = node.isInterval(start,31,key);
								if(flag == false)
									flag = node.isInterval(0,end,key);
						}
						if(flag)
						{
							transferFiles.add(file);
							count++;
						}
				    }
				
				    //out.write((""+count).getBytes());
					System.out.println("Total file "+count);
					String s ="";
					for(int i = 0;i < count;i++)
					{
							File f = transferFiles.get(i);
							System.out.println("Sent file : "+f.getName());
							//out.write((f.getName()+"\n").getBytes());
							if(i<count-1)
								s = s+f.getName()+":";
							else
								s = s+f.getName();
							f.delete();
					}
					System.out.println("**** Sending Response ****");
					out.write(s.getBytes());
					out.close();
				}
			}
			catch (IOException e)
			{
				// TODO Auto-generated catch block 
				e.printStackTrace();
				return;
			}
			Chord.display();
			
		}
		
	}
	
}

class Fingers
{
	static int table[][];
	Fingers()
	{   
		//Finger table initialized
		table = new int[5][5];
		for(int i=0;i<5;i++)
		{
			table[i][0] = (node.nodeId+(int)Math.pow(2,i))%32;
			table[i][1] = table[i][0];
			table[i][2] = (table[i][1]+(int)Math.pow(2,i))%32;
			table[i][3] = node.nodeId;
			table[i][4] = node.nodePort;
		}

		System.out.println("****Finger table intialized for this node****");
	}
	
	//When joined node knows node i.e id
	//init Finger table
	Fingers(int port)
	{
		table = new int[5][5];
		table[0][0] = (node.nodeId+(int)Math.pow(2,0))%32;
		table[0][1] = table[0][0];
		table[0][2] = (table[0][1]+(int)Math.pow(2,0))%32;
	    String predSucpred = Request.makeRequest(port,"find_successor:"+table[0][0]+":"+node.nodePort+":1");
	    String[] data = predSucpred.split(":");
	    int suc = Integer.parseInt(data[0]);
	    int pre = Integer.parseInt(data[1]);
	    table[0][4] = suc;
	    String k = "localhost:"+suc;
	    int sucId = Chord.SHA1(k);
	    node.successorPort = table[0][4];
	    node.successorId = sucId;
	    node.predecessorId = Chord.SHA1("localhost:"+pre);
	    node.predecessorPort = pre;
	    table[0][3]=sucId;
	    System.out.println("****Found its Successor using Known node i.e "+Chord.SHA1("localhost:"+port)+"****");
	    System.out.println("****It's Successor is "+sucId+"****"); 	
	    int sucPre = Integer.parseInt(Request.makeRequest(suc,"predecessor:"+node.nodePort));
	    for(int i=1;i<5;i++)
	    {
	    	table[i][0] = (node.nodeId+(int)Math.pow(2,i))%32;
	    	table[i][1] = table[i][0];
			table[i][2] = (table[i][1]+(int)Math.pow(2,i))%32;
			int x = table[i-1][3];
			int y = table[i][0];

			//Checking Interval 
			boolean flag =false;
			if((node.nodeId)%32<=(x-1+32)%32)
				flag = node.isInterval((node.nodeId)%32,(x-1+32)%32,y);
			else
			{
				flag = node.isInterval((node.nodeId)%32,31,y);
				if(flag == false)
				flag = node.isInterval(0,(x-1+32)%32,y);
			}
			if(flag)
			{
				table[i][3]=table[i-1][3];
				table[i][4]=table[i-1][4];
			}
			else
			{
                String val =(Request.makeRequest(port,"find_successor:"+table[i][0]+":"+node.nodePort+":1"));
				String[] data1 = val.split(":");
				table[i][3]= Chord.SHA1("localhost:"+data1[0]);
				table[i][4] =Integer.parseInt(data1[0]);
				x = table[i][3];
				y = node.nodeId;
			}
	    }
	    System.out.println("****Finger table intialized for this node****");
	}
	public static int closest_preceeding_finger(int id)
	{
		System.out.println("**** In closest_preceeding_finger ****");
		for(int i=4;i>=0;i--)
		{
			int x = table[i][3];
			boolean flag =false;
			if((node.nodeId+1)%32<=(id-1+32)%32)
				flag = node.isInterval((node.nodeId+1)%32,(id-1+32)%32,x);
			else
			{
				flag = node.isInterval((node.nodeId+1)%32,31,x);
				if(flag == false)
					flag = node.isInterval(0,(id-1+32)%32,x);
			}
			if(flag)
			{
				return table[i][4];
			}
			id%=32;
		}
		return node.nodePort;
	}
	
	//public static void update_finger_table
	
}

class Request
{
	Request()
	{
		
	}
	
	public static String makeRequest(int port,String msg)
	{
		Socket clientsocket = null;
		String ip = "localhost";
		
		InputStream in = null;
		OutputStream out = null;
		
		try
		{
			clientsocket = new Socket(ip,port);
			out = clientsocket.getOutputStream();
			in = clientsocket.getInputStream();
			
			String ask="";
			ask = msg;
			out.write(ask.getBytes());
			byte[] buf = new byte[1024];
			in.read(buf);
			String p = new String(buf);
			p=p.trim();
			System.out.println("****Message Request "+ask+" to node "+Chord.SHA1("localhost:"+port)+"****");
			System.out.println("****Response is "+p+" ****");
			if(msg.contains("transfer_files"))
			{
				String[] name = p.split(":");
				System.out.println("**** Receiving File *****");
				System.out.println("File count "+name.length);
				for(int i=0;i<name.length;i++)
				{
					System.out.println("File received "+name[i]);
					File f = new File("./nodeFile/" +name[i]);
					f.createNewFile();
				}
			}
			//in.close();
			return p;
			
		} 
		catch (Exception e)
		{
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return "";
	}
}

class node
{
	static int predecessorId;
	static int successorId;
    static int predecessorPort;
    static int successorPort;
    static int nodeId;
    static int nodePort;
    static HashMap<Integer, Integer> hmap = new HashMap<Integer, Integer>();
    static boolean isInterval(int start,int end,int key)
    {
    	if(key>=start && key<=end)
    		return true;
    	return false;
    }
	node(int id,int port)
	{
		nodeId = id;
		predecessorId = id;
		successorId = id;
		nodePort = port;
		predecessorPort = port;
		successorPort = port;
	}
	
	public static int find_predecessor(int id)
	{
		int curPredecessor = node.nodeId;
		int curPredecessor1 = node.nodePort;
		int suc = node.successorId;
		//Checking Interval
		boolean flag =false;
		if((curPredecessor+1)%32<=suc)
			flag = node.isInterval((curPredecessor+1)%32,suc,id);
		else
		{
			flag = node.isInterval((curPredecessor+1)%32,31,id);
			if(flag == false)
				flag = node.isInterval(0,suc,id);
		}
	    while(!(flag))
		{
			if(curPredecessor==node.nodeId)
			{
				curPredecessor1 = Fingers.closest_preceeding_finger(id);
				String k = "localhost:"+curPredecessor1;
				curPredecessor = Chord.SHA1(k);
				//System.out.println("huuuuuuu "+curPredecessor+" "+id);
				suc = (curPredecessor==node.nodeId)?node.successorId:Integer.parseInt(Request.makeRequest(curPredecessor1,"successor:1222"+":"+id%32+":0"));
	            //System.out.println(flag+" "+suc+" "+curPredecessor+" "+node.nodeId);

	            if(suc>32)
	            	suc = Chord.SHA1("localhost:"+suc);
	            if((curPredecessor+1)%32<=suc)
					flag = node.isInterval((curPredecessor+1)%32,suc,id);
				else
				{
					flag = node.isInterval((curPredecessor+1)%32,31,id);
					if(flag == false)
						flag = node.isInterval(0,suc,id);
				}
		    }
			else
			{
				curPredecessor1 = Integer.parseInt(Request.makeRequest(curPredecessor1,"closest_preceeding:"+(id)));
				String k = "localhost:"+curPredecessor1;
				curPredecessor = Chord.SHA1(k);
				suc = (curPredecessor==node.nodeId)?node.successorId:Integer.parseInt(Request.makeRequest(curPredecessor1,"successor:1222"+":"+id%32+":0"));
	            if(suc>32)
	            	suc = Chord.SHA1("localhost:"+suc);
	            if((curPredecessor+1)%32<=suc)
					flag = node.isInterval((curPredecessor+1)%32,suc,id);
				else
				{
					flag = node.isInterval((curPredecessor+1)%32,31,id);
					if(flag == false)
					flag = node.isInterval(0,suc,id);
				}
			}
		}

		System.out.println("****Predecessor of node i.e "+id+" is "+Chord.SHA1("localhost:"+curPredecessor1)+"****");
	    return curPredecessor1;
	}
	
	public static String find_successor(int id,int port,int flag)
	{
		int pred = find_predecessor(id);
		System.out.println("****Predecessor is "+Chord.SHA1("localhost:"+pred)+" for node "+id+"****");
		if(pred == node.nodePort)
		{
			int old1 = node.successorPort;
			int old = node.successorId;
			int n = Chord.SHA1("localhost:"+port);
			boolean flag1 =false;
			//Check Interval
			if((id)%32<=(old-1+32)%32)
				flag1 = node.isInterval((id)%32,(old-1+32)%32,n);
			else
			{
				flag1 = node.isInterval((id)%32,31,n);
				if(flag1 == false)
					flag1 = node.isInterval(0,(old-1+32)%32,n);
			}
			if(flag1)
			{
				old = n;
				old1 = port;
			}
            if(id == node.successorId)
            {
            	old1 = node.successorPort;
            	old = node.successorId;
            }
            System.out.println("****Successor Of it's Predecessor i.e"+node.nodeId+" is "+old+"****");
			return old1+":"+pred;
		}
		int predSuc = Integer.parseInt(Request.makeRequest(pred,"successor:"+port+":"+id+":"+flag));
		System.out.println("****Successor Of it's Predecessor i.e"+Chord.SHA1("localhost:"+pred)+" is "+predSuc+"****");
		return predSuc+":"+pred ;
	}
	

}

public class Chord
{
	
	private static int convertToHex(byte[] data)
	{ 
        StringBuffer buf = new StringBuffer();
        int halfbyte = (data[0] >>> 5) & 0x1F;
        System.out.println("in sha "+ halfbyte);
        return halfbyte;
    }
	
	public static int SHA1(String text) 
	{ 
		    MessageDigest md;
		    try
		    {
		    	String sha1 = null;		
        		MessageDigest msdDigest = MessageDigest.getInstance("SHA-1");
        		msdDigest.update(text.getBytes("UTF-8"), 0, text.length());
        		sha1 = DatatypeConverter.printHexBinary(msdDigest.digest());
        		int key = Integer.parseInt(new BigInteger(sha1, 16).toString(2).substring(95, 100), 2);
				return key;
		    }
		    catch(Exception e)
		    {
		    	e.printStackTrace();
		    }
		    return 0;
    } 
	
   public static void display()
   {
	   System.out.println("Enter respective choices");
	   System.out.println("1. own IP address and ID");
	   System.out.println("2.The IP address and ID of the successor and predecessor.");
	   System.out.println("3.The file key IDs it contains");
	   System.out.println("4.Its own finger table");
	   System.out.println("5.To leave network");
   }
   public static void main(String[] args) throws UnknownHostException
   { 
	 Fingers fingerTable;  
	 Scanner in = new Scanner(System.in);  
	 //Giving Port to this host
	 int port = 4068;
	 String inetAddress = "localhost:"+port;
	 
	 //Calling SHA1 for for id of node
	 int id = SHA1(inetAddress);
	 System.out.println("****Node Joined in Network and Id assigned to it is ****"+id);
	 //initializing node
	 node host = new node(id,port);
	 new Thread(new HandleRequest()).start();
	 System.out.println("Enter Port number if you Know someone else enter -1"); 
	 int input = in.nextInt();
	 if(input == -1)
	 {
		 fingerTable = new Fingers();
     }
	 else
	 {
		 //init Finger Table
	 	System.out.println(input+" "+node.nodeId);
		fingerTable = new Fingers(input);
		 
		//update_others
		for(int i=0;i<5;i++)
		{
			 int x = node.nodeId-(int)Math.pow(2,i)+1;
			 if(x<0)
				 x+=32;
		     //System.out.println("heee "+x); 		
			 int p = node.find_predecessor(x);
			 //System.out.println("hhhhh  "+x+" "+p);
			 if(p == node.nodePort)
			 {
			 	break;
			 }
			 System.out.println("heee "+p);
			 Request.makeRequest(p,"update:"+node.nodeId+":"+i+":"+node.nodePort);
			 //break;
		}

		//Transfer File
        int start = node.predecessorId+1;
        int end = node.nodeId;
        File dir = new File("./nodeFile");
		if(!dir.exists())
			dir.mkdir();
		else
		{
			String[] entries = dir.list();
			for(String s: entries)
				new File(dir,s).delete();
		}
		Request.makeRequest(node.successorPort,"transfer_files:"+start+":"+end);
		//int node = Fingers.closest_preceeding_finger(29);
		//System.out.println("heee "+node); 
	 }
	 while(true)
	 {
		 display();
		 int choice = in.nextInt();
		 if(choice == 1)
		 {
		 	 //InetAddress address = InetAddress.getByName("localhost"); 	
			 System.out.println("Host Ip is: " +InetAddress.getLocalHost().getHostAddress()+":"+node.nodePort+" and SHA1 id is "+node.nodeId);
		 }
		 else if(choice == 2)
		 {
			 System.out.println("PREDECESSOR--Ip is " +InetAddress.getLocalHost().getHostAddress()+":"+node.predecessorPort+" SHA1 Id is "+node.predecessorId);
			 System.out.println("SUCCESSOR--Ip is "+ InetAddress.getLocalHost().getHostAddress()+":"+node.successorPort+" SHA1 Id is "+node.successorId);
		 }
		 else if(choice == 3)
		 {
			 		File folder = new File("./nodeFile");
					File[] allFiles = folder.listFiles();
					//ArrayList<File> transferFiles = new ArrayList<File>();
					int count = 0;
					System.out.println("Key and Id it contains are :");
					for(File file:allFiles)
					{
						//System.out.println("hi");
						String filename = file.getName();
						int key = Chord.SHA1(filename);
						System.out.println(filename+" "+key);
				    }
				    System.out.println("Done...............");
		 }
		 else if(choice == 4)
		 {
			 System.out.println("Finger table of node "+node.nodeId);
		     System.out.println("start      startInterval     endInterval     successor");
			 for(int i=0;i<5;i++)
		     {
		    	 System.out.println(Fingers.table[i][0]+"		"+Fingers.table[i][1]+"		"+Fingers.table[i][2]+"		"+Fingers.table[i][3]);
		     }
		 }
		 else if(choice == 5)
		 {
            System.out.println("Node "+node.nodeId+" is about to leave");
            int successorPort = node.successorPort;
            //Request.makeRequest(successorPort,"predecessor:"+node.predecessorPort);
            for(int i=0;i<5;i++)
            {
            	int x = node.nodeId-(int)Math.pow(2,i)+1;
			 	if(x<0)
					 x+=32;
		     	//System.out.println("heee "+x); 		
			 	int p = node.find_predecessor(x);
			 	//System.out.println("hhhhh  "+x+" "+p);
			 	if(p == node.nodePort)
			 	{
			 		break;
			 	}
			 	//System.out.println("heee "+p);
			 	Request.makeRequest(p,"update:"+node.nodeId+":"+i+":"+node.nodePort+":"+successorPort);
            }
            Request.makeRequest(successorPort,"predecessor:"+node.predecessorPort+":"+node.nodePort);
            System.out.println("Well wishes to fellow node from node "+node.nodeId);
            System.exit(0);
		 }
	 }
	 
   }
}