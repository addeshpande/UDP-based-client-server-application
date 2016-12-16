//////////////////////////////////////////////////////////////////////////////////////////////
/*								ENTS 640 Networks and Protocols I							*/
/*			Project:Implementation of reliable data transfer over UDP using RC4 algorithm				*/
/* 					Authors: Gargi Bhandari and Atharva Deshpande							*/
//////////////////////////////////////////////////////////////////////////////////////////////


package transmitterPack;

//Import relevant classes

import java.util.Arrays;
import java.util.Random;

public class TransmitterUDPTest 
{

	public static void main(String[] args) throws Exception 
		{
		
		 	int MAX_PAYLOAD_SIZE=30;
		 	int MAX_DATA_LENGTH=500;
		 	int T_PACKETS=MAX_DATA_LENGTH/MAX_PAYLOAD_SIZE;
		 	
		
		 	//Create object of class FinalTransmitter
		 	TransmitterUDP test1=new TransmitterUDP(); 
		 
		 	//Initialize initial random sequence number
		 	byte[] sequenceNumber=test1.generateRandomSequenceNumber();
		 	byte[] nextSeqNumber=new byte[4];
		
		 	test1.generate500Bytes();
		 	//Create arrays for packets with appropriate lengths; 40 for first 16 packets
		 	//and 30 for the last one.
		 	byte[] finalPacket=new byte[40];
		 	
		
		 	//call the packet method 17 times for 17 packets
		 	for(int packetNumber=0;packetNumber<=T_PACKETS;packetNumber++)
		 	{
		 		if(packetNumber==0)
		 		{	
		 			//Form the packet,send it and receive acknowledgement
		 			System.out.println("\nPacket Number="+packetNumber);
		 			finalPacket=test1.formPacket(sequenceNumber,packetNumber);
		 			nextSeqNumber=test1.sendReceivePacket(finalPacket);
				
		 		}
		 		else
		 		{
		 			//Form the last packet,send it and receive acknowledgement
		 			System.out.println("\nPacket Number="+packetNumber);
		 			finalPacket=test1.formPacket(nextSeqNumber,packetNumber);
		 			test1.sendReceivePacket(finalPacket);
		 		}
			
		 	}
				
		}//main()
}// class TransmitterTest
