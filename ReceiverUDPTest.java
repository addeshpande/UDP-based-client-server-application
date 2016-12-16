//////////////////////////////////////////////////////////////////////////////////////////////
/*								ENTS 640 Networks and Protocols I							*/
/*			Project:Implementation of reliable data transfer over UDP using RC4 algorithm				*/
/* 					Authors: Gargi Bhandari and Atharva Deshpande							*/
//////////////////////////////////////////////////////////////////////////////////////////////
package receiverPack;

public class ReceiverUDPTest 
{

	public static void main(String[] args) throws Exception 
	{
		
		ReceiverUDP test1=new ReceiverUDP();
		//Sequence number is initialized to the same value as of the transmitter
		byte[] seqno=new byte[]{-27,69,118,24};
						
		//keep receiving the packets until the transmitter informs that the last packet is sent
		
		while(true)
			
		{				
				test1.receiveSendPacket(seqno);
				
		}		
				
	}//main()
}//class ReceiverTest
