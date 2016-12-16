//////////////////////////////////////////////////////////////////////////////////////////////
/*								ENTS 640 Networks and Protocols I							*/
/*			Project:Implementation of reliable data transfer over UDP using RC4 algorithm				*/
/* 					       Authors: Gargi Bhandari and Atharva Deshpande							*/
//////////////////////////////////////////////////////////////////////////////////////////////


package transmitterPack;

//import relevant classes
import java.io.IOException;
import java.io.InterruptedIOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;

import java.util.Arrays;
import java.util.Random;

public class TransmitterUDP 
	{
			//Maximum payoad is of 30
			private int MAX_PAYLOAD_SIZE=30;
			//Total number of bytes to be sent is 500
			private int MAX_DATA_LENGTH=500;
			//first 16 packets of size 30 bytes
			private int T_PACKETS=MAX_DATA_LENGTH/MAX_PAYLOAD_SIZE; 
			//last packet of size D_LEN_LAST_PACKET
			private int D_LEN_LAST_PACKET=MAX_DATA_LENGTH%MAX_PAYLOAD_SIZE; 
			//Received packet is extracted to packet array
			private byte[] packet=new byte[40];
			//Last received packet is extracted in lastPacket array
			private byte[] lastPacket=new byte[30];
			//Array containing 500 random data bytes
			private byte[] random500Bytes=new byte[MAX_DATA_LENGTH];	
			//Array containing maximum data of 30 bytes
			private byte[] data=new byte[30];
			//Length of maximum data
			private byte dataLength=(byte) data.length;			
			//Array containing last 20 data bytes
			private byte[] lastData=new byte[D_LEN_LAST_PACKET];
			//Length of array lastData
			private byte lastDataLength=(byte) lastData.length;
			//Array containing next sequence number
			private byte[] nextSequenceNumber=new byte[4];	
			//Array for received acknowledgement
			byte[] receiveArray=new byte[9];
			//Counter for last packet
			private int counter=0;
			
			//Form array of header plus payload for first 16 packets
			byte[] headerPayload=new byte[packet.length-4];
			//Form array of header plus payload for the last packet
			byte[] headerPayloadLast=new byte[lastPacket.length-4];
			//Integrity is stored in integrity array
			byte[] integrity=new byte[4];
			
			//----------public methods-----------//
			//generating 500 bytes of random data 
			public byte[] generate500Bytes()
			{
				Random r=new Random();
				r.nextBytes(random500Bytes);
				
				return random500Bytes;
			}//generate500Bytes()
		
			//generating a sequence number previously agreed by transmitter ansd receiver 
			public byte[] generateRandomSequenceNumber()
			{	//First generate random sequence number in seqnoR array
				//Random r=new Random();
				//byte[] seqnoR=new byte[4];
				//r.nextBytes(seqnoR);
				//System.out.println("Random seq no.="+Arrays.toString(seqnoR));
				//Copy the random array in seqno
				byte[] seqno=new byte[]{-27,69,118,24};
				return seqno;

			}//generateRandomSequenceNumber()			
			
			//generating the packet
			public byte[] formPacket(byte[] seqno, int packetNumber)
			{	
				//for first 16 packets
				if(packetNumber<T_PACKETS)
				{
					int temp2=1;
				
					//packet type
					packet[0]=0x55;
				
					//insert 30 bytes of data from 500 bytes of randomly generated data in a data array
					for(int noOfDataBytes=0;noOfDataBytes<MAX_PAYLOAD_SIZE;noOfDataBytes++)
					{
						data[noOfDataBytes]=random500Bytes[counter];
						counter++;
					}
				
					//insert the sequence number in the packet
					for(int seq=0;seq<seqno.length;seq++)
					{
						packet[temp2]=seqno[seq];
						temp2++;
					}
				
					//insert length in the packet
					packet[5]=dataLength;

					//insert data in the packet
					int j=6;
					for(int datap=0;datap<data.length;datap++)
					{
						packet[j]=data[datap];
						j++;
					}
				
					//extract header plus payload from the packet to calculate integrity
					for(int i=0;i<headerPayload.length;i++)
					{
						headerPayload[i]=packet[i];
					}
				
					//Calculate integrity
					byte[] keyStream=getkeyStream(headerPayload);
					byte[] encryptData=getEncryptedData(headerPayload,keyStream);
					integrity=getIntegrity(encryptData);

					//insert the calculated integrity into the packet
					for(int i=36,k=0;i<40 && k<integrity.length;i++,k++)
					{
						packet[i]=integrity[k];
					}
				
					return packet;
				}	
					//for the last packet
				else
				{	
				
					int temp2=1;
				
					//insert the packet type
					lastPacket[0]=(byte) 0xaa;
				
					//insert the sequence number in the packet
					for(int seq=0;seq<seqno.length;seq++)
					{
						lastPacket[temp2]=seqno[seq];
						temp2++;
					}
				
					//insert last 20 bytes from 500 bytes
					for(int noOfDataBytes=0;noOfDataBytes<D_LEN_LAST_PACKET;noOfDataBytes++)
					{
						lastData[noOfDataBytes]=random500Bytes[counter];
						counter++;
					}
				
					lastPacket[5]=lastDataLength;
				
					//insert data in the packet
					int j=6;
					for(int datap=0;datap<lastData.length;datap++)
					{
						lastPacket[j]=lastData[datap];
						j++;
					}
					int lastLength=headerPayloadLast.length;
					//append zeros if length is not a multiple of 4
					while(lastLength%4!=0){
						lastLength++;
						
					}
					
					headerPayloadLast=new byte[lastLength];
					//extract header plus payload from the packet to calculate integrity
					for(int i=0;i<lastLength;i++)
					{
						headerPayloadLast[i]=lastPacket[i];
					}
					//key stream generation
					byte[] keyStream=getkeyStream(headerPayloadLast);
					//getting the encrypted data
					byte[] encryptData=getEncryptedData(headerPayloadLast,keyStream);
					//getting the integrity
					integrity=getIntegrity(encryptData);

				
					for(int i=26,k=0;i<30 && k<integrity.length;i++,k++)
					{
						lastPacket[i]=integrity[k];
					}
				
					
					return lastPacket;
				
				}
			}//formPacket()	
					
			
			//send the packet and receive the ACK
			public byte[] sendReceivePacket(byte[] packet) throws IOException
			{
				
				byte[] receiveArray=sendCurrentPacket(packet);
				boolean b=validACK(receiveArray,packet);
				
				if(b==true)
				{
					//If b is true that means transmitter has received valid acknowledgement
					System.out.println("Valid Acknowledgement received");
					for(int temp=1,k=0;temp<5 && k<4;temp++,k++)
					{
						nextSequenceNumber[k]=receiveArray[temp];
					}
					return nextSequenceNumber;
				}
				
				else
				{
					//If b is false i.e.acknowledgement is lost or is invalid, 
					//re-send the packet.
					
					
						receiveArray=sendCurrentPacket(packet);
						boolean b1=validACK(receiveArray,packet);
						//If correct acknowledgement is received,break the while loop
						if(b1==true)
						{
							for(int temp=1,k=0;temp<5 && k<4;temp++,k++)
							{
								nextSequenceNumber[k]=receiveArray[temp];
							}
						}
							
						else
						{
							System.out.println("Communication failure");
							System.exit(0);
						}
					
					
					return nextSequenceNumber;
				}
				
			}	//sendReceivePacket()		
			
			
			public byte[] sendCurrentPacket(byte[] packet) throws IOException
			{
				//creating the IP address object for the receiver
				InetAddress serverip=InetAddress.getLocalHost();
				//creating the UDP packet to be sent
				DatagramPacket sendPacket=new DatagramPacket(packet,packet.length,serverip,3547);
				//creating the UDP transmitter socket 
				DatagramSocket senderSocket=new DatagramSocket();
				//Sending the packet to the receiver
				senderSocket.send(sendPacket);
				System.out.println("Current sent packet"+Arrays.toString(packet));
				//Start the timer
				senderSocket.setSoTimeout(1000);
				
				DatagramPacket receivePacket=new DatagramPacket(receiveArray,receiveArray.length);
				//Receive acknowledgement
				try{
						senderSocket.receive(receivePacket);
												
					}
				catch(InterruptedIOException e)
				{
					//Re-sending the packet
					senderSocket.send(sendPacket);
					System.out.println("Current sent packet"+Arrays.toString(packet));
					//Double the time-out
					senderSocket.setSoTimeout(2000);
					
					
					try{
							//Receive acknowledgement
							senderSocket.receive(receivePacket);
							//If received successfully, reset the time-out value
							senderSocket.setSoTimeout(1000);
							
						}
					catch(InterruptedIOException e1)
					{	
						//Re-send packet
						senderSocket.send(sendPacket);
						System.out.println("Current sent packet"+Arrays.toString(packet));
						//Increase the time-out by 4 times
						senderSocket.setSoTimeout(4000);
					
					
						try{
								//Receive acknowledgement
								senderSocket.receive(receivePacket);
								//If received successfully, reset the time-out value
								senderSocket.setSoTimeout(1000);
								
							}
						catch(InterruptedIOException e2)
						{	
							//Send the packet again
							senderSocket.send(sendPacket);
							System.out.println("Current sent packet"+Arrays.toString(packet));
							//Increase the time-out by 8 times
							senderSocket.setSoTimeout(8000);
						
						
							try{
								//Receive acknowledgement
								senderSocket.receive(receivePacket);
								//If received successfully, reset the time-out value
								senderSocket.setSoTimeout(1000);
								
								}
							catch(InterruptedIOException e3)
							{
								//Send the packet again
								senderSocket.send(sendPacket);
								System.out.println("Current sent packet"+Arrays.toString(packet));
								//Increase the time-out by 16 times
								senderSocket.setSoTimeout(16000);
							
							
								try{
										//Receive acknowledgement
										senderSocket.receive(receivePacket);
										//If received successfully, reset the time-out value
										senderSocket.setSoTimeout(1000);
										
									}
								catch(InterruptedIOException e4)
								{	
									//Notify the user of communication failure
									System.out.println("Communication failure");
									//Terminate the system
									System.exit(0);
								}
							}	
						}	
					}
				}
				senderSocket.close();
				return receiveArray;
			}//sendCurrentPacket()		
			
			//validating the correctness of the packet
			public boolean validACK(byte[] receiveArray,byte[] packet) throws IOException
			{
				
				//calculate header plus payload length
				//subtract 4 i.e. the integrity field
				int lengthRec=receiveArray.length-4;       
			    if(lengthRec%4!=0)
			    {
			    	while(lengthRec%4!=0)
			    	{
			    		lengthRec++;
			    	}
			    }	
			    
				byte[] receiveHead= new byte[lengthRec];
				for(int i=0;i<5;i++)
				{
					receiveHead[i]=receiveArray[i];
				}
				
				//Calculate integrity
				byte[] keyrec=getkeyStream(receiveHead);
				byte[] EncData=getEncryptedData(receiveHead,keyrec);
				byte[] integrityCheck=getIntegrity(EncData);
				
				
			
				//check if the ack is received correctly
				int count=0;
				if(receiveArray[0]==-1)
				{
					count++;
					for(int i=0,j=5;i<4 && j<9;i++,j++)
					{
						if(receiveArray[j]==integrityCheck[i])
						{
							count++;
						}	
					}	
						
							//convert seq number array to integer and add 31 to form ack number
							int checkSeqNumberInt=packet[4] & 0xFF |(packet[3] & 0xFF) << 8 |  (packet[2] & 0xFF) << 16 |   (packet[1] & 0xFF) << 24;
							int seqNumberInt=0;
							if(packet[0]==85)
							{
								 seqNumberInt=(checkSeqNumberInt) +(MAX_PAYLOAD_SIZE+1);
							}
							else if(packet[0]==-86)
							{
								 seqNumberInt=(checkSeqNumberInt)+ (D_LEN_LAST_PACKET+1);
							}
							byte[] sNumberByte=new byte[4];
							sNumberByte=ByteBuffer.allocate(4).putInt(seqNumberInt).array();

							for(int ind=0,ind2=1;ind<sNumberByte.length && ind2<5;ind++,ind2++)
							{
								if(sNumberByte[ind]==receiveArray[ind2])
							
								{
									count++;
								}
							}	
									
							//System.out.println("count="+count);
							if(count==9)
								return true;
							else
								return false;
				}
							
						
						
				else
					return false;				
				
			}//validACK()
			
						
			//generating the keystream
			public byte[] getkeyStream(byte[] headdata)
			{
				//calculate header plus payload length
				int length=headdata.length;       
			    if(length%4!=0)
			    {
			    	//Append 0s if the length is not a multiple of 4
			    	while(length%4!=0)
			    	{
			    		length++;
			    	}
			    }	
			    
				byte[] s=new byte[16];
				int entry=0;
				Random rd=new Random();
				//Generate Random key
				//byte[]KRandom=new byte[16];
				////rd.nextBytes(KRandom);
				////System.out.println("Random key="+Arrays.toString(KRandom));
				
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
			    for(int i=0;i<length;i++)
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
					keyArray[i]=k;					
					i++;
				
				}
				return(keyArray);
			}//getKeyStream()
			
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
				byte[] Data=new byte[length];
				for(int i=0;i<rc4Data.length;i++)
				{
					Data[i]=rc4Data[i];
				}
				byte[]cipherText=new byte[length];
				for(int a=0;a<length;a++)
				{
					//EX-OR key with data
					cipherText[a]=(byte) (keyArray[a]^Data[a]);
						
				}
					
					return(cipherText);
			}//getEncryptedData()
			
			//calculating the 4 bytes of integrity
			public byte[] getIntegrity(byte[] cipherText)
			{
				
				int length=cipherText.length;
				if(length%4!=0)
				{
					//Append 0s if length is not a multiple of 4
			    	while(length%4!=0)
			    	{
			    		length++;
			    	}
			    }	
				//form arrays to store every 4th element
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
				//xor corresponding elements
				while(count<length)
				{			
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
		}//class Transmitter
			
