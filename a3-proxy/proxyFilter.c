#include <sys/types.h>
#include <sys/socket.h>
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <netdb.h>
#include <netinet/in.h>
#include "Thread.h"
#define BUFLEN 512
#define REQUEST_BUFLEN 10240
#define BLACKLIST_ENTRY_SIZE 128
#define NUM_THREADS 6
#define GET "GET"
#define HOST "HOST"
#define CACHE_DIRECTORY "cache/"

// entries of blacklisted hosts.
char blackListHost[BLACKLIST_ENTRY_SIZE][BLACKLIST_ENTRY_SIZE];
int lastEntryInBlackList = 0;

/**
* Given a socket, buffer, and total length of string that requires to be sent,
* the method sends the message based on how many elements in the buffer have gone
* through.
**/
void reliableSend(int socket, char* send_buffer, int length) {
    int sent = 0;
    int sending = 0;

    // while the length of string sent is less than the total length of the string
    while (sent < length) {
        // if there is an error sending the message, give an error.
        if ((sending = send(socket, &send_buffer[sent], (length-sent), 0)) < 0) {
            perror("Error in send");
            break;
        }
        // otherwise, increment sent by the amount of string that has been sent.
        sent += sending;
    }
}

/**
  * Given a string, this method converts the string into ASCII hex character.
  * This method will be used for naming cache files.
  **/
char* convertToASC(char* inputstring) {
    // allocate the maximum needed to store the ascii represention:
    char *output = malloc(sizeof(char) * 255);
    char *output_end = output;

    // allocation failed!
    if (!output) 
        exit(EXIT_FAILURE);

    *output_end = '\0';

    // While there is still inputstring left, convert the input string into ASCII characters.
    for (; strlen(output) < 255 && *inputstring; ++inputstring) {
        // check for chars that are not alphabet or number.
        if ((*inputstring >= 'a'&& *inputstring <= 'z') || (*inputstring >= 'A' && *inputstring <= 'Z') || (*inputstring >= '0' && *inputstring <= '9')) {
            output_end += sprintf(output_end, "%c", *inputstring);
        } else {
            if (strlen(output) >= 252) {
                break;
            }
            output_end += sprintf(output_end, "_%u_", *inputstring);

        }
    }
  return output;
}

/**
  * Given a filename and filecontent, this method will create a file with a name that 
  * has been converted into corresponding ASCII of filename given and the filecontent. 
  **/
void writeCacheFile(char* filename, char* filecontent) {
    FILE *fp = fopen(filename, "a+");

    // write strings into the file created.
    if (filecontent != NULL) {
        fputs(filecontent, fp);
    }

    // close the file.
    fclose(fp);
}

/**
  * Given a blacklist file name, the method assigns each of the blacklist hosts to
  * each of the elements of blackListFileLine array. 
  **/
void parseBlacklistFile(char* file_name) {
    // local variables for the method
    char blackListFileLine[BLACKLIST_ENTRY_SIZE];
    size_t len = 0;
    ssize_t read;
    FILE *fp;

    // opening a blacklist file
    fp = fopen(file_name,"r");

    // if the file is not present, then send an error
    if( fp == NULL ) {
        printf("Error while opening the file.\n");
        exit(1);
    }

    // print out the host names contained in the blacklist file.
    printf("The black list hosts in the file %s are :\n", file_name);
    int i = 0;

    // while the blacklist file is read
    while (fgets(blackListFileLine, BLACKLIST_ENTRY_SIZE, fp) != NULL) {
        char *newLineChar;

        // if there is a line break
        if ((newLineChar = strchr(blackListFileLine, '\n')) != NULL)

            // clear the rest of the characters after the line break within the line.
            *newLineChar = '\0';

        // print the blacklist hosts
        printf("%s\n", blackListFileLine);

        // copy the host name string to each element of blackListHost array.
        strcpy(blackListHost[i], blackListFileLine);
        lastEntryInBlackList = i++;
    }
    lastEntryInBlackList++;

    // close the file
    fclose(fp);
}

/**
  * Given the hostURL, this method checks if the hostURL given matches any of the hosts
  * in the blacklist file. If there is a match, returns 1, if not, return 0.
  **/
int isHostBlackListed(char* hostURL) {
    int i;
    // while there are still blacklist hosts present
    for (i = 0; i < lastEntryInBlackList; i++) {
        // if the hostURL matches with a blacklist host
        if (strstr(hostURL, blackListHost[i]) != NULL) {
            printf("%s is blacklisted, comparing with %s\n", hostURL, blackListHost[i]);
            return 1;
        }
    }
    return 0;
}

/**
  * Given an accpeting socket, this method creates a thread containing the accepting socket.
  **/
void *acceptingThread(void * sockfd) {
    int sd = (int) sockfd;

    while(1) {
        struct addrinfo hints;
        struct addrinfo *res;
        int hostSockfd;
        int n;
        int bytes_to_read;
        int clientsd;
        int client_len;
        char *bp;
        char buf[BUFLEN];
        struct sockaddr_in client;

        /* Receive from request from client */
        client_len = sizeof(client);

        clientsd = accept(sd, (struct sockaddr *) &client, &client_len);

        // if there is no accpeting socket, give an error.
        if (clientsd < 0) {
            printf("Can't accept client");
            // go to closeANDrestart to close accepting socket and host socket.
            goto closeANDrestart;
        }

        // creating a buffer for receiving a request from the client.
        char clientRecvBuf[REQUEST_BUFLEN];

        // before the buffer accepts the request from the client, it needs to be cleared so
        // that there will not be any leftover of the previous request in the buffer.
        bzero((char*)clientRecvBuf, REQUEST_BUFLEN);

        int i = 0;
        bp = clientRecvBuf;
        bytes_to_read = REQUEST_BUFLEN;

        // while the buffer is receiving the request from the client
        while ((n = recv(clientsd, bp, bytes_to_read, 0)) > 0) {

            // if there is a line break in the request, then break.
            if (strstr(clientRecvBuf, "\n\n") != NULL || strstr(clientRecvBuf, "\r\n\r\n") != NULL) {
                break;
            }

            // increment the buffer print and decrement bytes_to_read by n.
            bp += n;
            bytes_to_read -= n;
        }

        printf ("Received Request: \n%s\n", clientRecvBuf);

        // Creating variables needed for parsing the request from the client.
        char requestType[16];
        char getURI[1024];
        char httpVersion[16];
        char hostHeader[16];
        char hostURL[256];
        char port[8];

        // Clear the data in the variable.
        bzero(requestType, 16);
        bzero(getURI, 1024);
        bzero(httpVersion, 16);
        bzero(hostHeader, 16);
        bzero(hostURL, 256);
        bzero(port, 8);
        // keep default port number to 80.
        strcpy(port, "80");

        // give a value to each of requestType, getURI, httpVersion.
        sscanf(clientRecvBuf, "%s %s %s\n", requestType, getURI, httpVersion);

        // if requestType is not GET
        if (strstr(requestType, GET) == NULL) {

            // clear the data in the buf
            bzero(buf, BUFLEN);

            // send an error message to the client that the requestType must be GET.
            snprintf(buf, sizeof buf, "%s 405 Method Not Allowed\nOnly GET Request Allowed\nConnection: close\r\n\r\n", httpVersion);
            reliableSend(clientsd, buf, strlen(buf));
            //send(clientsd, buf, strlen(buf), 0);

            // goto closeANDrestart to close the accepting socket and host socket.
            goto closeANDrestart;

        } else {
            printf("REQEUST GET!\n");
            // if HostHeader is not "HOST:",
            char* hostPtr;
            // Check for Host: string exist.
            if ((hostPtr = strstr(clientRecvBuf, "Host: ")) == NULL) {
                // if the Host: doesn't exist return error.
                // clear the data in the buf
                bzero(buf, BUFLEN);

                // send an error message to the client that there is no HOST.
                snprintf(buf, sizeof buf, "%s 400 Bad Request\nThere Is Not Host In Request\nConnection: close\r\n\r\n", httpVersion);
                reliableSend(clientsd, buf, strlen(buf));
                //send(clientsd, buf, strlen(buf), 0);

                // goto closeANDrestart to close the accepting socket and host socket.
                goto closeANDrestart;
            }
            // If the Host does exist. Get the HostURL.
            sscanf(hostPtr, "%s %s\n", hostHeader, hostURL);

            // if there is ":" it mean there is a port number.
            // Then parse the port number out.
            if (strstr(hostURL, ":") != NULL) {
                // variables for hostURL and port
                char * hostURLCopy, * portCopy;

                // hostURLCopy gets the hostURL
                hostURLCopy = strtok(hostURL, ":");

                // portCopy gets the port
                portCopy = strtok(NULL, ":");

                // copy the strings in hostURLCopy and portCopy to hostURL and port respectively.
                strcpy(hostURL, hostURLCopy);
                strcpy(port, portCopy);
            }

            // if the hostURL is blacklisted
            if (isHostBlackListed(hostURL)) {

                // clear the data in buf
                bzero(buf, BUFLEN);

                // send an error to the client that the host has been blacklisted.
                snprintf(buf, sizeof buf, "%s 403 Forbidden\nHost Is Blacklisted\nConnection: close\r\n\r\n", httpVersion);
                reliableSend(clientsd, buf, strlen(buf));
                //send(clientsd, buf, strlen(buf), 0);

                // go to closeANDrestart to close accepting socket and host socket.
                goto closeANDrestart;

            } else {

                // connect to the HOST.
                // setup ai_family and ai_socktype for host socket.
                memset(&hints, 0, sizeof hints );
                hints.ai_family = AF_UNSPEC;
                hints.ai_socktype = SOCK_STREAM;

                // if there is no host socket address info,
                if (getaddrinfo(hostURL, port, &hints, &res) != 0) {

                    // clear the data in buf.
                    bzero(buf, BUFLEN);

                    // send an error to the client that there is no host info.
                    snprintf(buf, sizeof buf, "%s 502 Bad Gateway\nCANNOT GET HOST INFO\nConnection: close\r\n\r\n", httpVersion);
                    reliableSend(clientsd, buf, strlen(buf));
                    //send(clientsd, buf, strlen(buf), 0);

                    // go to closeANDrestart to close accepting socket and host socket.
                    goto closeANDrestart;
                }

                // create a host socket.
                hostSockfd = socket(res->ai_family, res->ai_socktype, res->ai_protocol);

                // if there is no host socket created,
                if (hostSockfd < 0) {

                    // clear the data in buf
                    bzero(buf, BUFLEN);

                    // send an error to the client that socket to host cannot be created.
                    snprintf(buf, sizeof buf, "%s 502 Bad Gateway\nCANNOT CREATE SOCKET TO HOST\nConnection: close\r\n\r\n", httpVersion);
                    reliableSend(clientsd, buf, strlen(buf));
                    //send(clientsd, buf, strlen(buf), 0);

                    // go to closeANDrestart to close accepting socket and host socket.
                    goto closeANDrestart;
                }

                // Create receive timeout. Set it to 30 sec.
                struct timeval tv;
                tv.tv_sec = 30;  /* 30 Secs Timeout */
                tv.tv_usec = 0;  // Not init'ing this can cause strange errors
                
                // Set the timeout
                setsockopt(hostSockfd, SOL_SOCKET, SO_RCVTIMEO, (char *)&tv,sizeof(struct timeval));

                // if the host socket cannot be created,
                if (connect(hostSockfd, res->ai_addr, res->ai_addrlen) < 0) {

                    // clear the data in buf.
                    bzero(buf, BUFLEN);

                    // send an error to the client that host socket cannot be connected.
                    snprintf(buf, sizeof buf, "%s 502 Bad Gateway\nCANNOT CONNECT TO HOST INFO\nConnection: close\r\n\r\n", httpVersion);
                    reliableSend(clientsd, buf, strlen(buf));
                    //send(clientsd, buf, strlen(buf), 0);

                    // go to closeANDrestart to close accepting socket and host socket.
                    goto closeANDrestart;
                }

                // Create a file with a name that has been converted to ASCII
                char *fname = convertToASC(getURI);
                char *hostDirectory = convertToASC(hostURL);


                // Create cache directory.
                mkdir(CACHE_DIRECTORY, 0700);

                // Create string with cache directory and hostDirectory first.
                int size = strlen(fname);
                size += strlen(hostDirectory);
                char fileDirectory[size + 16];
                strcpy(fileDirectory, CACHE_DIRECTORY);
                strcat(fileDirectory, hostDirectory);

                // Create cache/<hostDirectory>.
                mkdir(fileDirectory, 0700);

                // then add the cache filename then the full path is created.
                strcat(fileDirectory, "/");
                strcat(fileDirectory, fname);

                // Try opening the cache file.
                FILE *fp = fopen(fileDirectory, "r");
                printf("CacheFilename: %s\n", fileDirectory);
                // Check if we have cache file.
                if (fp != NULL) {
                    // if we have cached file.
                    printf("Request is in cached file.\n");
                    char cacheRecvBuf[BUFLEN];
                    // while the cache file is read
                    while (fgets(cacheRecvBuf, BUFLEN, fp) != NULL) {
                        // send cached request.
                        reliableSend(clientsd, cacheRecvBuf, strlen(cacheRecvBuf));
                        //send(clientsd, cacheRecvBuf, strlen(cacheRecvBuf), 0);
                    }
                    fclose(fp);
                } else {
                    // if we don't have cached file.
                    printf("Request is not in cached file.\n");
                    // otherwise, send the request to the host via host socket.
                    reliableSend(hostSockfd, clientRecvBuf, strlen(clientRecvBuf));
                    //send(hostSockfd, clientRecvBuf, strlen(clientRecvBuf), 0);
                    printf("sending Request Host:\n%s\n", clientRecvBuf);

                    printf("Recving from host!\n");
                    // create a buffer for receiving response from the host.
                    char* recvBuf[BUFLEN];
                    char* httpReseponeChar;
                    int httpResponseCode;
                    char* httpResepone;
                    // while receiving response from the host, send the host's response to the client.
                    do {
                        bzero((char*)recvBuf, BUFLEN);
                        n = recv(hostSockfd, recvBuf, BUFLEN, 0);
                        if(!(n <= 0)) {
                            reliableSend(clientsd, recvBuf, n);
                            //send(clientsd, recvBuf, n, 0);
                            // If the recvBuf include HTTPversion it mean we can know the http Response code.
                            if (strstr(recvBuf, httpVersion) != NULL) {
                                char recvBufCopy[(n + 1)];
                                // Make a copy of recvBuf so we can use strtok.
                                strcpy(recvBufCopy, recvBuf);
                                httpResepone = strtok(recvBufCopy, "\n");
                                // Get the http Response code.
                                strtok(httpResepone, " ");
                                httpReseponeChar = strtok(NULL, " ");
                                // Save the httpResponse code as integer.
                                httpResponseCode = atoi(httpReseponeChar);
                            }
                            if (httpResponseCode == 200) {
                                writeCacheFile(fileDirectory, recvBuf);
                            }
                        }
                    } while(n > 0);
                    printf("Done Receiving from host!\n");
                }
            }
        }

        // At this point, clientsd (accepting socket) and hostSockfd (host socket) are closed.
        closeANDrestart:
        close(clientsd);
        close(hostSockfd);
    }
}

int main(int argc, char **argv) {

    if (argc < 2) {
        printf("Usage: ./proxyFilter <port_no> (optinal)<blacklistFile>\n");
        exit(1);
    }

    if (argc == 3) {
        parseBlacklistFile(argv[2]);
    }

    printf("Starting proxy server with port: %s\n", argv[1]);

    int sd;
    struct sockaddr_in server;

    // Create listening socket.
    // If the listening socket cannot be created, then send an error.
    if ((sd = socket(AF_INET, SOCK_STREAM, 0)) < 0) {
        printf("Can't create a socket.\n");
        exit(1);
    }

    // Create server sockaddr_in.
    memset((char *)&server, 0, sizeof(struct sockaddr_in));

    // setup for binding.
    server.sin_family = AF_INET;
    server.sin_port = htons(atoi(argv[1]));
    server.sin_addr.s_addr = htonl(INADDR_ANY);

    // If listening socket cannot be bound, send an error.
    if (bind(sd, (struct sockaddr*) &server, sizeof(server)) < 0) {
        printf("Can't bind name to socket.\n");
        exit(1);
    }

    // Make the listening socket listen.
    listen(sd, 50);

    // Create NUM_THREADS to accept on.
    int rc;
    long t;
    void *status;
    pthread_t threads[NUM_THREADS];

    // Create NUM_THREADS number of threads.
    for(t = 0; t < NUM_THREADS; t++){

        // thread created.
        threads[t] = createThread(acceptingThread, (void *) sd);

        // thread running
        rc = runThread((void *) threads[t], NULL);

        // if a thread cannot be run, then send an error.
        if (rc){
            printf("ERROR; return code from pthread_create() is %d\n", rc);
            exit(1);
        }
    }

    // Once the threads are created join the threads.
    for (t = 0; t < NUM_THREADS; t++) {
        rc = joinThread(threads[t], &status);

        // if a thread cannot be joined, then send an error.
        if (rc) {
            printf("ERROR; return code from pthread_join() is %d\n", rc);
            exit(1);
        }
    }

    // close listening socket.
    close(sd);
    return 0;
}
