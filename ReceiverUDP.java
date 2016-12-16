//////////////////////////////////////////////////////////////////////////////////////////////
/*								ENTS 640 Networks and Protocols I							*/
/*			Project:Implementation of reliable data transfer over UDP using RC4 algorithm				*/
/* 					     Authors: Gargi Bhandari and Atharva Deshpande						*/
//////////////////////////////////////////////////////////////////////////////////////////////
package receiverPack;

//import relevant classes
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Random;

public class ReceiverUDP 
{
	
		//Define maximum payload size i.e.30
		private int MAX_PAYLOAD_SIZE=30;
		//Initialize array in which acknowledgement is stored
		private byte[] ackPacket=new byte[9];
		//Initialize receiver arrays
		private byte[] receiveArray=new byte[40];
		private byte[] receiveArrayLast=new byte[30];
		//Initialize the finally received data array
		private byte[] finalReceivedPacket=new byte[500];
		//initialize array to display the received bytes
		private byte[] buffer=new byte[500];
		//Counter for dataa in the last packets
		private int datapInFinalPacket=0;
		//Initialize arrays for calculating integrity check; i.e. header+payload
		private byte[] headerPayload=new byte[ackPacket.length-4];
		//Array for storing integrity
		private byte[] integrity=new byte[4];
		//Counter for number of packets
		private int packetCount=0;
		//Array of received sequence number or previous ACK number
		private byte[] preACKNumber=new byte[4];
		//-----------PUBLIC METHODS---------//
		//Receive packets and send ACK
		public void receiveSendPacket(byte[] seqno) throws IOException
		{
			///Set Packet type to ffh i.e.255 for acknowledgement packet 
			ackPacket[0]=(byte) 0xff;
			//Receive the packet sent by transmitter
			DatagramSocket receiverSocket=new DatagramSocket(3547);
			DatagramPacket receivePacket=new DatagramPacket(receiveArray,receiveArray.length);
			
			receiverSocket.receive(receivePacket);
			int len=receivePacket.getLength();
			byte[] packetArray=new byte[len];
			
			//Transfer the contents of received packet in packetArray
			for(int i=0;i<receivePacket.getLength();i++)
			{
				packetArray[i]=receivePacket.getData()[i];
				
			}
			
			
			//counter to check the correctness of the packet
			int count=0;
			
			//counter for incrementing the sequence number
			int noOfDataBytes=0;

			//check if the packet is correctly received
			//Check if packet type is valid i.e. it is either 55h(85) or aah(-86) and data length
			//is less than maximum payload size
			if(packetArray[0]==85 || packetArray[0]==-86 && packetArray.length<=MAX_PAYLOAD_SIZE)
			{
				byte[] senderHeaderToPayload=new byte[packetArray.length-4];
				for(int i=0;i<packetArray.length-4;i++)
				{
					senderHeaderToPayload[i]=packetArray[i];
				}
				
				//calculating locally generated integrity
				byte[] keyrec2=getkeyStream(senderHeaderToPayload);
				byte[] EncData2=getEncryptedData(senderHeaderToPayload,keyrec2);
				byte[] integrityCheck2=getIntegrity(EncData2);
				
				//check if both locally generated integrity and integrity of received packet are equal
				for(int i=0,j=packetArray.length-4;i<4 && j<packetArray.length;i++,j++)
				{
					if(packetArray[j]==integrityCheck2[i])
					{
						count++;
					}	
				}	
				
				
				//Check if Sequence number received is the same as expected sequence number
				for(int ind=0,sqInd=1;ind<seqno.length && sqInd<5;ind++,sqInd++)
				{
					if(packetArray[sqInd]==seqno[ind]||packetArray[sqInd]==preACKNumber[ind])
						count++;
				}
				//System.out.println("count="+count);
				if(count==8)
				//If count=8, all the above conditions are true
				{	
					
					System.out.printf("\nPacket[%d] received successfully",packetCount);
					packetCount++;
					//Store the data in the final buffer
						for(int datapInPacket=6;datapInPacket<packetArray.length-4;datapInPacket++)
						{
							finalReceivedPacket[datapInFinalPacket]=packetArray[datapInPacket];
							datapInFinalPacket++;
							noOfDataBytes++;
						}	
						
						
						int temp3=1;
						for(int i=0;i<4;i++)
						{	
							//Take the received sequence number from packetArray
							preACKNumber[i]=packetArray[temp3];
							temp3++;
						}
						int ackNumberInt=0;
						//convert sequence number array to integer and add no. of data bytes to form acknowledgement number
						int seqNumberInt=preACKNumber[3] & 0xFF |(preACKNumber[2] & 0xFF) << 8 |    (preACKNumber[1] & 0xFF) << 16 |    (preACKNumber[0] & 0xFF) << 24;
						if(packetArray[0]==85)
						{
							ackNumberInt=(seqNumberInt) +noOfDataBytes+1;
						}
						else if(packetArray[0]==-86)
						{
							ackNumberInt=(seqNumberInt) +noOfDataBytes+1;
						}
				
				
						//convert ack integer to ack byte array of 4 bytes
						byte[] ACKNumber=new byte[4];
						ACKNumber=ByteBuffer.allocate(4).putInt(ackNumberInt).array();
				
				
						//put the ack byte array into ack packet
						for(int i=0,j=1;i<4 && j<5;i++,j++)
						{
							ackPacket[j]=ACKNumber[i];
						}
				
						//Transfer header+payload to forIntegrity array for integrity calculation
						for(int i=0;i<headerPayload.length;i++)
						{
							headerPayload[i]=ackPacket[i];
						}
						//Calculate integrity
						byte[] keyStream=getkeyStream(headerPayload);
						byte[] encryptData=getEncryptedData(headerPayload,keyStream);
						integrity=getIntegrity(encryptData);
				
						//Put the calculated integrity into the packet
						for(int i=0,j=5;i<4 && j<9;i++,j++)
						{
							ackPacket[j]=integrity[i];
						}
				
						//Copy acknowledgement number to seqno array for next sequence number calculation
						for(int ind=0;ind<seqno.length;ind++)
						{
							seqno[ind]=ACKNumber[ind];
						}
				
						//send the packet				
						System.out.println("\nSent Acknowledgement="+Arrays.toString(ackPacket));
						//creating the IP address object for the transmitter
						InetAddress senderAddress = receivePacket.getAddress();
						int senderPort = receivePacket.getPort();
						//creating the UDP ack packet to be sent
						DatagramPacket sendPacket=new DatagramPacket(ackPacket,ackPacket.length,senderAddress,senderPort);
						//sending the packet to the transmitter
						receiverSocket.send(sendPacket);		
				
						//If the packet received is the last packet, print all the elements received
						//until now. If packet is the last packet, type=aah or -86
						if(packetArray[0]==-86)
						{
							for(int i=0;i<500;i++)
							{
								buffer[i]=finalReceivedPacket[i];
							}
							System.out.println("\nCommunication Successful\n\nThe received data bytes are:"+Arrays.toString(buffer));
							//Terminate the system
							System.exit(0);
						}
				}
			}	
			//Close the socket
			receiverSocket.close();
		}//receiveSendPacket()		
			
		//generating the keystream
		public byte[] getkeyStream(byte[] headdata)
		{
			//calculate header plus payload length
			int length=headdata.length;       
		    if(length%4!=0)
		    {
		    	while(length%4!=0)
		    	{
		    		length++;
		    	}
		    }	
			byte[] s=new byte[16];
			int entry=0;
			//Initialize the random key of 16 bytes equal to the one at transmitter
			byte[] K=new byte[]{-112, 22, 115, 62, -48, 71, 105, -35, 118, 41, 32, -29, 47, -70, 36, 111};
			
			byte[] T=new byte[16];
			int keyLength=K.length;
			byte keylen=(byte) keyLength;
			
			for(entry=0;entry<16;entry++)
			{
				s[entry]=(byte) entry;
				T[entry]=(K[entry%keylen]);				
				
			}
			
		    int j=0;
		    
		    for(entry=0;entry<16;++entry)
		    {
				j=(((j+s[entry]+T[entry])%keyLength)& 0xff)/16;
				
				byte temp=s[entry];
				s[entry]=s[j];
				s[j]=temp;
				
			}
		   
		    	
		  //create byte array of header+payload						
		    byte[] rc4Data=new byte[length]; 
		    
		    for(int i=0;i<headdata.length;i++)
		    {
		    	//copy header+payload into rc4Data
		    	rc4Data[i]=headdata[i];		
		    }
		    
		    byte[] keyArray=new byte[length];
		    
		    int i=0,m=0,n=0;
			while(i<length)
			{
				m=(m+1)%keyLength;         
				n=(n+s[m])%keyLength;		
				byte temp=s[m];	  
				s[m]=s[n];
				s[n]=temp;
				int t=(s[m]+s[n])%keyLength;
				byte k=s[t];
				//Transfer the key stream to keyArray
				keyArray[i]=k;				
				i++;
			
			}
			return(keyArray);
		}//getKeyStream()
		
		//generating the encrypted data
		public byte[] getEncryptedData(byte[] rc4Data,byte[] keyArray)
		{
			
			int length=rc4Data.length;			
			if(length%4!=0)
			{
				//Append 0s if length is not a multiple of 4
		    	while(length%4!=0)
		    	{
		    		length++;
		    	}
		    }	
			
			byte[]cipherText=new byte[length];
			byte[] Data=new byte[length];
			for(int i=0;i<rc4Data.length;i++)
			{
				Data[i]=rc4Data[i];
			}
			
			for(int a=0;a<length;a++)
			{
				//EX-OR data with key
				cipherText[a]=(byte) (keyArray[a]^Data[a]);
					
			}
			return(cipherText);
		}//getEncryptedData()
		
		//generate the 4 bytes of integrity
		public byte[] getIntegrity(byte[] cipherText)
		{
			
			int length=cipherText.length;
			if(length%4!=0)
			{
				//Append 0sif length is not a multiple of 4
		    	while(length%4!=0)
		    	{
		    		length++;
		    	}
		    }	
			byte[] C=new byte[4];
			byte[]b1=new byte[length];
			byte[]b2=new byte[length];
			byte[]b3=new byte[length];
			byte[]b4=new byte[length];
			
			
			int ind,a=0;
			for(a=4;a<length;a++)
			{
				//start with zero th element
				b1[0]=cipherText[0];
				if(a%4==0)
				{
					b1[a]=cipherText[a];
					
				}
				else if(a%4==1)
				{
					//start with first element
					b2[1]=cipherText[1];
					b2[a]=cipherText[a];
						
				}
				else if(a%4==2)
				{
					//start with second element
					b3[2]=cipherText[2];
					b3[a]=cipherText[a];
				}
				else if(a%4==3)
				{
					//start with third element
					b4[3]=cipherText[3];
					b4[a]=cipherText[a];
				}
				
			}
				
			//Compress the integrity to 4 bytes
			int count=0,count1=1,count2=2,count3=3;
			int c1=0,c2=0,c3=0,c4=0;
			while(count<length)
			{
				//xor corresponding elements
				c1= (byte)(c1^b1[count]);
				c2= (byte)(c2^b2[count1]);
				c3= (byte)(c3^b3[count2]);
				c4= (byte)(c4^b4[count3]);
				
				count+=4;
				count1+=4;
				count2+=4;
				count3+=4;
			}  
			//put c1,c2,c3,c4 in C array
			C[0]=(byte) c1;
			C[1]=(byte) c2;
			C[2]=(byte) c3;
			C[3]=(byte) c4;
		
			return(C);
	
		}//getIntegrity()		
		
}//class Receiver
