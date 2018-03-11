***********************Project Title*****************************
Chord Protocol : Implementation of chord protocol over collection of nodes in java.Both node join 
and node deletion is implemented. In this node will leave gracefully i.e by notifying his fellow node.Also this is reliable as file is also distributed to fellow node before leaving.

************************Getting Started***************************
There are four folder named 1,2,3,4 representing 4 different nodes and each node contain a folder nodeFile which stores the file of a particular node.Also each folder contain script to generate 100 file which is used later.

***********************PreRequisites*******************************
Java and nothing else

***********************Running the tests**************************
Navigate to each folder and open a terminal in each folder.Each terminal basically represents a node.

use this command :
darkmatter@hp-15:~/Dropbox/Chord/1$ javac Chord.java 
darkmatter@hp-15:~/Dropbox/Chord/1$ java Chord 
****Node Joined in Network and Id assigned to it is ****16
Enter Port number if you Know someone else enter -1

if it's the first node first use :
darkmatter@hp-15:~/Dropbox/Chord/1$ sh fileCreator.sh

it creates 100 file in nodeFile folder of particular node.

To know the port number either look in code or Enter 1 when display menu comes.

darkmatter@hp-15:~/Dropbox/Chord/1$ java Chord 
****Node Joined in Network and Id assigned to it is ****16
Enter Port number if you Know someone else enter -1
-1
****Finger table intialized for this node****
Enter respective choices
1. own IP address and ID
2.The IP address and ID of the successor and predecessor.
3.The file key IDs it contains
4.Its own finger table
5.To leave network


************************************************************************
In order to extend this to more number of node just create another folder with chord.java file and nodeFile folder plus filecreator.sh and change the port in code an try to avoid collision of id's with previous node.

