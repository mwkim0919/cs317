
For part A please use branch: master
For part B please use branch: master

ADDITIONAL COMMENTS

After doing part B, we do not all the frames although they are in order.

This is because RTP client playing the frames may take more time to process the previous frame and 
fail to play the next frame on time. Hence, we skip frames that are not on time caused by the client.


Bit#    |  Type      |   Meaning
--------------------------------------
[0-1]   | Version    |  Current(2)
[2]     | Padding    |  <- 0
[3]     | ext.       |    0
[4-7]   | cc         |   0
[8]     | M          |  marker (0)
[9-15]  |payloadType |     26
[16-31] |seq. num.   | 
[32-63] | timestamp  |  
[64-96] | ssrc       |  0

 